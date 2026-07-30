package com.mediaplayer.android.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceBitmapLoader
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.CacheBitmapLoader
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.Executors
import com.mediaplayer.android.MainActivity
import com.mediaplayer.android.R
import com.mediaplayer.android.data.AuthBootstrap
import com.mediaplayer.android.data.AuthTokenHolder
import com.mediaplayer.android.data.LikedRepository
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.widget.NowPlayingSnapshot
import com.mediaplayer.android.widget.WidgetState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.launch

/**
 * Bound foreground service that owns the ExoPlayer instance.
 *
 * Playback lives here — not in a ViewModel — so the OS keeps audio alive
 * when the Activity is destroyed and we get lock-screen / notification
 * media controls out of the box. The UI layer talks to this via a
 * [androidx.media3.session.MediaController] built in [PlayerConnection].
 *
 * Subclasses [MediaLibraryService] so Android Auto can discover the app
 * and request a browse tree via [MediaLibrarySession.Callback]. Phone-side
 * behaviour is unchanged — a `MediaController` handshake still lands on
 * the same session.
 *
 * The player is configured to pipe audio through [Network.okHttp] so every
 * byte (song stream, cover art, catalog calls) shares one connection pool
 * and one cache.
 */
@UnstableApi
class MediaPlaybackService : MediaLibraryService() {

    companion object {
        const val ACTION_TOGGLE_LIKE = "com.mediaplayer.android.TOGGLE_LIKE"
        /**
         * Custom session command for toggling shuffle from any controller (AA
         * or phone). No args. Flips the shared shuffle pref; the service's
         * shuffle-pref collector then reorders the whole source pool via
         * [EndlessQueueController]. Bound to a CommandButton in the AA custom
         * layout — shuffle is app-level (the native `Player.shuffleModeEnabled`
         * is kept off), so this custom control is the single working shuffle
         * entry point on the car surface.
         */
        const val ACTION_TOGGLE_SHUFFLE = "com.mediaplayer.android.TOGGLE_SHUFFLE"
        /**
         * Custom session command for cycling repeat mode (OFF → ALL → ONE → OFF).
         * Mirrors the phone NowPlayingSheet's [PlaybackViewModel.cycleRepeat]
         * order. Bound to an AA CommandButton for the same reason as
         * [ACTION_TOGGLE_SHUFFLE].
         */
        const val ACTION_CYCLE_REPEAT = "com.mediaplayer.android.CYCLE_REPEAT"
        /**
         * Custom session command: drop a song from the endless-queue source
         * pool so it's never re-appended on a refill/wrap. Sent by the phone
         * VM after "brano sbagliato" (flagWrong) — the timeline items are
         * removed there, but only the service owns [EndlessQueueController]'s
         * [sourceItems]. Bundle key {@code "songId"} (Long).
         */
        const val ACTION_DROP_FROM_SOURCE = "com.mediaplayer.android.DROP_FROM_SOURCE"
        /** Bundle key for [ACTION_DROP_FROM_SOURCE]: the song id to drop. */
        const val EXTRA_DROP_SONG_ID = "songId"
        /**
         * Custom session command for setting / cancelling the sleep timer.
         * Args bundle key: {@code "minutes"} (Int). 0 cancels an active timer.
         * Authoritative timer state lives on this service so controllers on
         * Android Auto and the phone share one timer instance.
         */
        const val ACTION_SLEEP_TIMER = "com.mediaplayer.android.SLEEP_TIMER"
        /** Bundle key on session extras: Boolean. True when a sleep timer is armed. */
        const val EXTRA_SLEEP_ACTIVE = "sleep_active"
        /**
         * Bundle key on session extras: Long. Remaining ms while the sleep
         * timer is armed; 0 when no timer is active OR when the timer is
         * armed in end-of-track mode (which has no countdown). Updated at
         * minute boundaries — controllers reading this value should ceil
         * to minutes.
         */
        const val EXTRA_SLEEP_REMAINING_MS = "sleep_remaining_ms"
        /**
         * Bundle key on session extras: Boolean. True when sleep timer is
         * armed in `Fine traccia` (end-of-track) mode — pause fires on the
         * next AUTO/REPEAT track transition, no countdown.
         */
        const val EXTRA_SLEEP_END_OF_TRACK = "sleep_end_of_track"
        /** Bundle key on session extras: Boolean. True when current song is liked. */
        const val EXTRA_LIKED = "liked"

        /**
         * Controllers we accept on the {@link MediaLibrarySession}. Anything not
         * in this set (or our own package) is rejected in {@code onConnect} so
         * an arbitrary app on the device can't subscribe to our session and
         * issue custom commands.
         */
        private val ALLOWED_CONTROLLER_PACKAGES = setOf(
            "com.google.android.projection.gearhead", // Android Auto
            "com.google.android.car.media",          // Automotive OS media center
            "com.google.android.googlequicksearchbox", // Assistant
            "com.android.bluetooth",                  // BT car/headphone media controls
            "com.android.systemui",                   // System lockscreen / notification controls
            "android",                                // System "android" package (lockscreen, etc.)
        )

        /** AA / Automotive packages — narrower than [ALLOWED_CONTROLLER_PACKAGES],
         *  used to gate the [AALyricsTicker] so phone-only sessions don't pay
         *  for an AA-card refresh that nothing renders. */
        private val CAR_CONTROLLER_PACKAGES = setOf(
            "com.google.android.projection.gearhead",
            "com.google.android.car.media",
        )

        /** Car head units plus Bluetooth car-kit media controls — the presence
         *  of any of these at grace-pause fire time means the user is almost
         *  certainly still connected, so a stale projection `false` shouldn't
         *  pause playback. See [hasConnectedCarController]. */
        private val CAR_OR_BT_CONTROLLER_PACKAGES = CAR_CONTROLLER_PACKAGES + "com.android.bluetooth"

        /** Tag for car projection / controller diagnostics. Grep this in
         *  logcat after a drive: pause decisions log here with the projection
         *  state that drove them. */
        private const val CAR_LIFECYCLE_TAG = "PlaybackCarLifecycle"

        /**
         * Grace window before the projection-dropped pause fires. Projection
         * recovering within this window (wireless AA re-handshake) cancels
         * it. Long enough to ride out a blip, short enough that genuinely
         * leaving the car still pauses promptly.
         */
        private const val CAR_DISCONNECT_PAUSE_DELAY_MS = 6_000L

        /** Tag for "why did playback stop" diagnostics: every playWhenReady
         *  flip (with reason), suppression change, player error and sleep
         *  expiry logs here. One logcat grep now disambiguates focus loss /
         *  becoming-noisy / rogue controller / network error / sleep timer. */
        private const val STOP_DIAG_TAG = "PlaybackStopDiag"

        /**
         * Errors worth retrying in place: transient transport failures where
         * the bytes are fine and the link will likely come back (cellular
         * dead zone mid-drive, server blip behind Caddy). Corruption-like
         * codes are deliberately excluded — those go through the ViewModel's
         * refreshLocalDownload path instead.
         */
        private val RETRYABLE_STREAM_ERROR_CODES = setOf(
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_TIMEOUT,
        )

        /** Backoff ladder for [RETRYABLE_STREAM_ERROR_CODES]; the attempt
         *  counter resets once the player reaches STATE_READY again. */
        private val STREAM_RETRY_DELAYS_MS = longArrayOf(2_000L, 5_000L, 15_000L, 30_000L)
    }

    private var mediaSession: MediaLibrarySession? = null
    private var resumption: PlaybackResumption? = null
    private var resumptionListener: Player.Listener? = null
    private var prefetch: PrefetchOrchestrator? = null
    private var endlessQueue: EndlessQueueController? = null
    private var phantomHealer: PhantomSkipHealer? = null
    private var crossfadeJob: Job? = null

    /**
     * Monotonic id of the newest crossfade ramp. A rapid double-skip cancels
     * the old fade job, but its `finally` can still run AFTER the new fade
     * has begun — an unconditional volume reset there jumped audio to 1.0
     * mid-ramp. The finally now resets only while it still owns the newest
     * generation. Main-thread only.
     */
    private var crossfadeGeneration: Int = 0

    /** In-flight delayed retry after a transient stream error. */
    private var streamRetryJob: Job? = null

    /** Index into [STREAM_RETRY_DELAYS_MS]; reset on STATE_READY. */
    private var streamRetryAttempt: Int = 0

    /**
     * Cached crossfade seconds — kept in sync with [PlayerSettings] via a
     * collect on [serviceScope]. Read on every auto-transition; the prior
     * `runBlocking { crossfadeSecondsNow() }` blocked the player looper on
     * a DataStore read on every track change.
     */
    @Volatile private var crossfadeSecondsCached: Int = 0

    private var aaLyricsTicker: AALyricsTicker? = null

    /**
     * Authoritative "is the phone projecting to a car" signal, fed by
     * [androidx.car.app.connection.CarConnection]'s LiveData (observed on
     * the main thread, so no lock is needed).
     *
     * This REPLACES the old `carControllerCount` heuristic that counted
     * gearhead MediaController connects/disconnects. That heuristic was
     * structurally broken (Media3 1.10.0 semantics, confirmed in library
     * source):
     *  - gearhead is a LEGACY controller: it has no real disconnect signal.
     *    Media3 synthesizes `onDisconnected` after a fixed 5-minute
     *    inactivity timeout — so passively listening in the car (no button
     *    presses) "disconnected" the controller mid-drive and paused music.
     *  - `onDisconnected` can fire for controllers that never reached
     *    `onPostConnect` (connection aborted mid-handshake), driving the
     *    count to 0 while the real controller was still attached.
     *  - gearhead also connects controllers with NO car attached (background
     *    media scans, AA app opens, post-boot init) — which is why the pause
     *    also fired while listening on Bluetooth HEADPHONES.
     */
    private var carProjectionActive: Boolean = false
    private var carConnection: androidx.car.app.connection.CarConnection? = null
    private val carConnectionObserver = androidx.lifecycle.Observer<Int> { type ->
        onCarConnectionTypeChanged(type)
    }

    /**
     * Pending "user left the vehicle" pause, scheduled when car projection
     * drops and cancelled if projection comes back within
     * [CAR_DISCONNECT_PAUSE_DELAY_MS] (wireless AA can blip briefly). A
     * genuine exit (projection stays down) still pauses after the grace
     * window.
     */
    private var carDisconnectPauseJob: Job? = null

    /**
     * Off-main scope for `MediaLibrarySession.Callback` work (browse tree
     * fetches, search). Bridged to Media3's `ListenableFuture` API via
     * `kotlinx-coroutines-guava`'s `future { ... }` builder.
     */
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // --- "Like current song" custom command (mirrors phone heart button)
    //
    // Exposed as a CommandButton so Android Auto / Wear / lockscreen all
    // get a quick toggle for the currently playing track. Icon flips
    // between filled and outline based on the cached liked state, which
    // is refreshed every time the player transitions to a new media item.
    private val likedRepository = LikedRepository()
    private val toggleLikeCommand =
        SessionCommand(ACTION_TOGGLE_LIKE, Bundle.EMPTY)
    private val toggleShuffleCommand =
        SessionCommand(ACTION_TOGGLE_SHUFFLE, Bundle.EMPTY)
    private val cycleRepeatCommand =
        SessionCommand(ACTION_CYCLE_REPEAT, Bundle.EMPTY)
    private val dropFromSourceCommand =
        SessionCommand(ACTION_DROP_FROM_SOURCE, Bundle.EMPTY)
    @Volatile private var currentLiked: Boolean = false
    /** Mirrors `player.shuffleModeEnabled` so the AA custom-layout button can
     *  pick the right icon without touching the player off the main thread. */
    @Volatile private var currentShuffle: Boolean = false
    /** Mirrors `player.repeatMode` for the same reason as [currentShuffle]. */
    @Volatile private var currentRepeatMode: Int = Player.REPEAT_MODE_OFF

    // --- Sleep timer (service-owned, single source of truth across phone + AA)
    //
    // The service owns the timer so a timer set in the car keeps ticking when
    // the activity is destroyed, and a timer set on the phone is reflected
    // back in AA's now-playing card. Controllers send {@link #ACTION_SLEEP_TIMER}
    // with a "minutes" int (0 = cancel); state is published via session extras.
    // Main-dispatched scope so the timer's expiry callback (which calls
    // `player.pause()`) runs on the main thread — Player must be accessed
    // on its application looper. Job parented to serviceScope so service
    // teardown still cancels in-flight timers.
    private val mainScope = CoroutineScope(serviceScope.coroutineContext + Dispatchers.Main)
    private val sleepTimer = SleepTimer(mainScope)
    private val sleepTimerCommand =
        SessionCommand(ACTION_SLEEP_TIMER, Bundle.EMPTY)
    /** Default minutes used when a controller invokes the sleep command without args. */
    private val defaultSleepMinutes = 30

    override fun onCreate() {
        super.onCreate()

        val httpFactory = OkHttpDataSource.Factory(Network.okHttp)
        // M10: wrap the HTTP data source in a disk-backed CacheDataSource so
        // repeat plays / seeks within cached windows avoid the network. The
        // cache singleton is process-scoped — see [PlayerCache] for why we
        // never release it. FLAG_IGNORE_CACHE_ON_ERROR means a corrupt cache
        // entry falls through to upstream instead of hard-failing playback.
        val streamCache = PlayerCache.get(this)
        val streamCacheFactory = CacheDataSource.Factory()
            .setCache(streamCache)
            .setUpstreamDataSourceFactory(httpFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        // Read offline copies from the download cache, but do NOT write to it
        // from the streaming pipeline: its evictor is a NoOpCacheEvictor
        // (DownloadRoot) so every streamed byte would persist forever in
        // filesDir/downloads with no cap — internal storage grew unbounded.
        // The inner streamCache (PlayerCache, 1 GiB LRU) still absorbs
        // repeat-play/seek caching; explicit offline saves go through
        // DownloadManager, which remains the sole writer of the download cache.
        // (setCacheWriteDataSinkFactory(null) makes this layer read-only.)
        val downloadCache = DownloadRoot.getDownloadCache(this)
        val cacheFactory = CacheDataSource.Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(streamCacheFactory)
            .setCacheWriteDataSinkFactory(null)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        val dataSourceFactory = DefaultDataSource.Factory(this, cacheFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(dataSourceFactory)

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            // Treat the stream as music so the system honours ducking /
            // becoming-noisy / audio-focus loss the right way.
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            // Keep CPU + Wi-Fi awake while streaming network audio so the
            // OS doesn't doze the buffer mid-playback. NETWORK wake mode
            // also covers local cached files (superset of WAKE_MODE_LOCAL).
            // Manifest already declares WAKE_LOCK.
            .setWakeMode(C.WAKE_MODE_NETWORK)
            // PREV means "previous track", always. Media3's 3s default made
            // seekToPrevious() restart the current song whenever the user was
            // more than 3s in — so from a head unit, PREV "did nothing" mid-song.
            // Zeroing the threshold makes PREV deterministically go back a track.
            .setMaxSeekToPreviousPositionMs(0)
            .build()

        // Pin a fresh audio-session id BEFORE the equalizer attaches.
        // ExoPlayer otherwise allocates the id lazily on first AudioTrack
        // creation, so `player.audioSessionId` is 0 here and the equalizer
        // would silently no-op for the lifetime of the service. Generating
        // and assigning explicitly guarantees a stable id the Equalizer
        // hardware effect can bind to from the first frame onwards.
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val audioSessionId = audioManager.generateAudioSessionId()
        if (audioSessionId != AudioManager.ERROR) {
            player.audioSessionId = audioSessionId
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = android.app.PendingIntent.getActivity(
            this,
            0,
            intent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        // System media surfaces (notification, lockscreen, Android Auto, BT/AVRCP)
        // resolve `MediaMetadata.artworkUri` through Media3's BitmapLoader.
        // Wrap an OkHttp-backed factory in DefaultDataSource so the loader
        // can resolve BOTH schemes the app emits:
        //  - `content://com.mediaplayer.android.covers/{id}` for AA browse
        //    tiles (handled in-process by [CoverContentProvider] via
        //    ContentDataSource — auth headers are injected when the
        //    provider's openFile() hits the backend),
        //  - `https://backend/api/songs/{id}/cover` for now-playing /
        //    resumption snapshots / phone-side controllers (falls through
        //    to OkHttpDataSource — auth interceptor on Network.okHttp
        //    rides along).
        val bitmapLoader = CacheBitmapLoader(
            DataSourceBitmapLoader(
                MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor()),
                DefaultDataSource.Factory(this, OkHttpDataSource.Factory(Network.okHttp)),
            )
        )

        // Endless queue lives on the player itself (not the Activity-scoped
        // ViewModel) so it keeps refilling for head-unit / headless skips. Wrap
        // the player so external NEXT commands refill the tail before advancing.
        // Internal listeners (resumption, prefetch, lyrics, widget, engine) stay
        // on the inner `player`; the session drives the forwarding wrapper.
        endlessQueue = EndlessQueueController(player).also { it.install() }
        // Truncated-download self-heal, also service-owned so it fires headless
        // (Android Auto / screen off) where the VM isn't around — that's exactly
        // where the car "skips a song by itself".
        phantomHealer = PhantomSkipHealer(this, player).also { it.install() }
        val sessionPlayer = EndlessForwardingPlayer(player, endlessQueue!!)

        mediaSession = MediaLibrarySession.Builder(this, sessionPlayer, LibraryCallback())
            .setBitmapLoader(bitmapLoader)
            .setSessionActivity(pendingIntent)
            .setCustomLayout(
                buildCustomLayout(
                    liked = false,
                    sleepActive = false,
                    sleepRemainingMs = 0L,
                    sleepEndOfTrack = false,
                )
            )
            .build()

        // Mirror service-owned sleep timer state to controllers (phone VM + AA)
        // via session extras + a refresh of the custom layout. Combine
        // isActive + remainingMs + endOfTrackActive so the AA chip flips
        // from preset chips → live `Annulla · N min` countdown (or
        // `Annulla · fine traccia`) the moment the timer is armed, and
        // ticks at minute boundaries until expiration / cancel.
        //
        // mainScope (not serviceScope/IO) because setSessionExtras +
        // setCustomLayout publish through the MediaSession and must run
        // on the session's application thread.
        mainScope.launch {
            combine(
                sleepTimer.isActive,
                sleepTimer.remainingMs,
                sleepTimer.endOfTrackActive,
            ) { active, remaining, eot ->
                Triple(active, remaining, eot)
            }.collectLatest { (active, remaining, eot) ->
                mediaSession?.let { session ->
                    session.setSessionExtras(
                        buildSessionExtras(currentLiked, active, remaining, eot)
                    )
                    session.setCustomLayout(
                        buildCustomLayout(currentLiked, active, remaining, eot)
                    )
                }
            }
        }

        // Cache crossfade seconds so the auto-transition listener doesn't
        // have to runBlocking-read DataStore on every track change.
        serviceScope.launch {
            com.mediaplayer.android.data.PlayerSettings.instance
                .crossfadeSeconds
                .collectLatest { crossfadeSecondsCached = it }
        }

        // M13: bind the hardware Equalizer to the audio session we pinned
        // above. Using the locally-generated id (rather than re-reading
        // `player.audioSessionId`) protects against drivers that report
        // 0 until the first AudioTrack is built.
        EqualizerController.init(this, audioSessionId)

        // Checkpoint the queue + position so Android Auto can show a
        // "resume" chip on cold car connect. See onPlaybackResumption.
        resumption = PlaybackResumption(this).also {
            resumptionListener = it.install(player)
        }

        // M10: warm the disk cache with the prev/next tracks around
        // whatever is currently playing, gated on unmetered network.
        prefetch = PrefetchOrchestrator(this, streamCache, httpFactory).also {
            it.install(player)
        }

        // Drive the AA now-playing card with the current synced lyric line.
        // Phone has its own LyricsSheet; AA can't show a scrolling list, so
        // we surface lyrics one line at a time via MediaMetadata.description.
        // Main scope: tickOnce/applyDescription read player.currentPosition
        // and call player.replaceMediaItem — both must be on the application
        // looper. Network I/O inside the ticker still hops to Dispatchers.IO
        // explicitly via withContext.
        aaLyricsTicker = AALyricsTicker(player, mainScope).also { it.install() }

        // Watch real car projection state. observeForever is safe here:
        // onCreate runs on the main thread and the observer is removed in
        // onDestroy. The first emission (NOT_CONNECTED when no car) matches
        // the initial [carProjectionActive] = false, so nothing fires until
        // an actual transition.
        carConnection = androidx.car.app.connection.CarConnection(this).also {
            it.type.observeForever(carConnectionObserver)
        }

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                refreshLikeButtonForCurrent(mediaItem)
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    fadeInOnAutoTransition()
                }
            }
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                // App owns shuffle (see [EndlessQueueController]); the AA button
                // state is driven by the shuffle pref, not this native flag.
                // Never let native ExoPlayer shuffle order actually take effect
                // — a head unit / AVRCP control that flips the standard command
                // would otherwise fight our timeline reordering. Force it off.
                if (shuffleModeEnabled) mediaSession?.player?.shuffleModeEnabled = false
            }
            override fun onRepeatModeChanged(repeatMode: Int) {
                currentRepeatMode = repeatMode
                // Persist it. Repeat shares its store and key with the phone,
                // but the service never wrote it, so a mode set on the head
                // unit was lost on service restart and silently reverted to
                // the last phone-set value. Shuffle already round-trips this
                // way; repeat was a half-finished mirror of it.
                serviceScope.launch {
                    runCatching { PlaybackPrefs.setRepeat(applicationContext, repeatMode) }
                }
                updateCustomLayout()
            }
        })

        // Stop diagnostics + transient-error self-heal. Logs WHY playback
        // state changed (the missing fact in every "music stopped by itself"
        // report) and retries in place after a transient transport error —
        // without this, a cellular dead zone mid-drive left the player in
        // STATE_IDLE forever, and with the Activity gone (AA / screen off)
        // nobody was around to even show an error.
        player.addListener(object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                Log.i(
                    STOP_DIAG_TAG,
                    "playWhenReady=$playWhenReady reason=${playWhenReadyReason(reason)}",
                )
            }
            override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {
                Log.i(
                    STOP_DIAG_TAG,
                    "suppression=${suppressionReason(playbackSuppressionReason)}",
                )
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY && streamRetryAttempt != 0) {
                    Log.i(STOP_DIAG_TAG, "stream recovered, retry ladder reset")
                    streamRetryAttempt = 0
                    streamRetryJob?.cancel()
                    streamRetryJob = null
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                Log.w(STOP_DIAG_TAG, "playerError ${error.errorCodeName}: ${error.message}")
                maybeRetryAfterStreamError(error)
            }
        })
        // Any explicit transport action (NEXT / PREV / seek, or a user-driven
        // play/pause) proves the user is present and interacting — so cancel a
        // pending "left the vehicle" grace pause. Fixes the race where a brief
        // wireless-AA/BT projection blip armed the 6s pause, the user pressed
        // NEXT on the wheel (which advances via the session, NOT via
        // CarConnection), and 6s later the still-stale projection flag paused
        // playback, undoing the skip the user just made.
        player.addListener(object : Player.Listener {
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                if (reason == Player.DISCONTINUITY_REASON_SEEK ||
                    reason == Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT
                ) {
                    cancelCarDisconnectPause()
                }
            }
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) {
                    cancelCarDisconnectPause()
                }
            }
        })

        // Repeat is native, so seed it from the player. Shuffle is app-level
        // and seeded/kept in sync by the shuffle-pref collector below.
        currentRepeatMode = player.repeatMode

        // Shuffle is service-owned. The shared shuffle pref is the single
        // source of truth: a toggle from Android Auto (ACTION_TOGGLE_SHUFFLE)
        // or the phone both write it, and this collector performs the actual
        // reorder over the full source pool on the app looper, then refreshes
        // the AA custom-layout button. DataStore replays the current value on
        // collect, so this also seeds [currentShuffle] on service start.
        // Seed repeat once from the shared pref so a headless car start comes
        // up in the mode the user last chose, wherever they chose it. One-shot
        // rather than a collector: the phone controller writes the same key,
        // and two writers on one player would fight.
        serviceScope.launch {
            val saved = runCatching {
                PlaybackPrefs.repeatFlow(applicationContext).first()
            }.getOrNull() ?: return@launch
            withContext(Dispatchers.Main) {
                mediaSession?.player?.repeatMode = saved
            }
        }

        serviceScope.launch {
            PlaybackPrefs.shuffleFlow(applicationContext).collectLatest { enabled ->
                withContext(Dispatchers.Main) {
                    currentShuffle = enabled
                    endlessQueue?.applyShuffle(enabled)
                    updateCustomLayout()
                }
            }
        }

        // Mirror player state into [WidgetState] so the Now-Playing home-screen
        // widget can repaint without holding its own MediaController. Updates
        // fire on track change, play state change, and timeline change so the
        // widget's hasNext/hasPrevious gating stays accurate.
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                pushWidgetState()
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                pushWidgetState()
            }
            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                pushWidgetState()
            }
            override fun onPlaybackStateChanged(state: Int) {
                pushWidgetState()
            }
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                pushWidgetState()
            }
            override fun onRepeatModeChanged(repeatMode: Int) {
                pushWidgetState()
            }
        })
        // Seed the widget once at service start so a freshly bound widget
        // doesn't render an empty placeholder until the first transition.
        pushWidgetState()
    }

    /**
     * Snapshot the current player state into [WidgetState] (synchronously)
     * and kick off an async cover decode via Coil that re-pushes once the
     * bitmap is ready. Called on every relevant Player.Listener event.
     *
     * Loading the cover off-thread keeps the listener callback non-blocking;
     * the widget repaints twice per track change (text first, then cover)
     * which is identical to how the system notification fills its art.
     */
    private fun pushWidgetState() {
        val player = mediaSession?.player ?: return
        val item = player.currentMediaItem
        val md = item?.mediaMetadata
        // AA / library / resumption items use the "song:{id}" mediaId form;
        // stripping the prefix keeps the widget alive (its transport buttons
        // gate on a non-null songId) for car-initiated playback too.
        val songId = item?.mediaId?.removePrefix("song:")?.toLongOrNull()
        val artUri = md?.artworkUri?.toString()
        val previous = WidgetState.now.value
        val keepCover = previous.songId == songId && previous.coverUri == artUri
        val snapshot = NowPlayingSnapshot(
            songId = songId,
            title = md?.title?.toString().orEmpty(),
            artist = md?.artist?.toString().orEmpty(),
            isPlaying = player.isPlaying,
            hasNext = player.hasNextMediaItem(),
            hasPrevious = player.hasPreviousMediaItem(),
            coverUri = artUri,
            cover = if (keepCover) previous.cover else null,
            // Shuffle is app-level; the native flag is pinned off, so read the
            // mirrored app-level state instead or the widget icon is always off.
            shuffleEnabled = currentShuffle,
            repeatMode = player.repeatMode,
        )
        WidgetState.update(snapshot)
        if (!keepCover && artUri != null) loadCoverForWidget(artUri, songId)
    }

    private var coverLoadJob: Job? = null
    /**
     * Fetches the cover bytes for the home-screen widget and decodes to a
     * software Bitmap with [android.graphics.BitmapFactory]. Software
     * config is mandatory for AppWidgets — the RemoteViews IPC channel
     * rejects hardware bitmaps and the widget would silently render blank.
     *
     * Handles both schemes the player emits as `MediaMetadata.artworkUri`:
     *  - `content://` (AA browse-tile origin) → ContentResolver, which
     *    routes back to [CoverContentProvider] in-process,
     *  - `https://` (phone-side / resumption origin) → shared OkHttp
     *    client so the auth interceptor injects backend headers.
     */
    private fun loadCoverForWidget(uri: String, expectedSongId: Long?) {
        coverLoadJob?.cancel()
        coverLoadJob = serviceScope.launch {
            val bitmap = runCatching {
                val parsed = android.net.Uri.parse(uri)
                val bytes: ByteArray? = when (parsed.scheme) {
                    "content", "android.resource", "file" ->
                        contentResolver.openInputStream(parsed)?.use { it.readBytes() }
                    else -> {
                        val response = Network.okHttp.newCall(
                            okhttp3.Request.Builder().url(uri).build()
                        ).execute()
                        response.use { r ->
                            if (!r.isSuccessful) null else r.body?.bytes()
                        }
                    }
                }
                bytes?.let { android.graphics.BitmapFactory.decodeByteArray(it, 0, it.size) }
            }.getOrNull()
            withContext(Dispatchers.Main) {
                val cur = WidgetState.now.value
                if (cur.songId == expectedSongId && cur.coverUri == uri) {
                    WidgetState.update(cur.copy(cover = bitmap))
                }
            }
        }
    }

    /**
     * Crossfade approximation: when ExoPlayer auto-advances to the next
     * track, ramp [Player.volume] from 0 → 1 over the user-configured
     * duration. With Media3's default gapless playback this gives a
     * perceptible smooth entrance without needing two players. Skipped
     * when the user picked 0 sec.
     *
     * Reads the player from [mediaSession] since this is invoked from
     * the listener body where the local `player` is no longer in scope.
     */
    private fun fadeInOnAutoTransition() {
        val p = mediaSession?.player ?: return
        val seconds = crossfadeSecondsCached
        if (seconds <= 0) {
            p.volume = 1f
            return
        }
        // Cancel any in-flight ramp from a previous auto-transition. The
        // job's finally resets volume so a user-skip mid-fade doesn't
        // strand the next track at < 1.0.
        val gen = ++crossfadeGeneration
        crossfadeJob?.cancel()
        // Player.volume must be written on the application looper (= main).
        // serviceScope is bound to Dispatchers.IO and will throw
        // IllegalStateException("Player is accessed on the wrong thread")
        // under Media3's strict-mode build or on a future version bump.
        crossfadeJob = mainScope.launch {
            try {
                val totalMs = seconds * 1000L
                val stepMs = 50L
                val steps = (totalMs / stepMs).toInt().coerceAtLeast(1)
                for (i in 0..steps) {
                    p.volume = i.toFloat() / steps
                    kotlinx.coroutines.delay(stepMs)
                }
                p.volume = 1f
            } finally {
                // Reset to full on any exit path (cancellation, exception) so
                // a cancelled fade doesn't leave the next track silent — but
                // only while this is still the newest ramp: a superseded
                // job's finally otherwise fires mid-way through its
                // replacement and jumps the volume to 1.0.
                if (gen == crossfadeGeneration) p.volume = 1f
            }
        }
    }

    /** Resolves the song id from a `song:{id}` mediaId, or null otherwise. */
    private fun songIdOf(mediaItem: MediaItem?): Long? =
        mediaItem?.mediaId?.removePrefix("song:")?.toLongOrNull()

    /**
     * Pull liked status for the new track and rebuild the custom layout
     * so the heart icon reflects reality. Network call lives off the
     * main thread; failures degrade silently to "not liked".
     */
    private fun refreshLikeButtonForCurrent(mediaItem: MediaItem?) {
        val songId = songIdOf(mediaItem) ?: run {
            currentLiked = false
            updateCustomLayout()
            return
        }
        serviceScope.launch {
            val liked = try {
                likedRepository.status(listOf(songId)).contains(songId)
            } catch (_: Exception) {
                false
            }
            com.mediaplayer.android.data.LikedSongsCache.markLiked(songId, liked)
            // setCustomLayout / setSessionExtras must run on the session's
            // application thread (serviceScope is Dispatchers.IO); publishing
            // from IO violates the Media3 contract and races the main-thread
            // sleep-timer collector. Hop to Main, and re-check the track is
            // still current so a skip mid-fetch doesn't paint a stale heart.
            withContext(Dispatchers.Main) {
                if (songIdOf(mediaSession?.player?.currentMediaItem) == songId) {
                    currentLiked = liked
                    updateCustomLayout()
                }
            }
        }
    }

    private fun updateCustomLayout() {
        val sleepActive = sleepTimer.isActive.value
        val remainingMs = sleepTimer.remainingMs.value
        val endOfTrack = sleepTimer.endOfTrackActive.value
        mediaSession?.let { session ->
            session.setCustomLayout(
                buildCustomLayout(currentLiked, sleepActive, remainingMs, endOfTrack)
            )
            session.setSessionExtras(
                buildSessionExtras(currentLiked, sleepActive, remainingMs, endOfTrack)
            )
        }
    }

    private fun buildSessionExtras(
        liked: Boolean,
        sleepActive: Boolean,
        sleepRemainingMs: Long,
        sleepEndOfTrack: Boolean,
    ): Bundle = Bundle().apply {
        putBoolean(EXTRA_LIKED, liked)
        putBoolean(EXTRA_SLEEP_ACTIVE, sleepActive)
        putLong(EXTRA_SLEEP_REMAINING_MS, sleepRemainingMs)
        putBoolean(EXTRA_SLEEP_END_OF_TRACK, sleepEndOfTrack)
    }

    /**
     * AA / lockscreen custom layout. Sleep-timer chips were removed from this
     * surface — driver-distraction concern: 4 chips on the AA card consumed
     * the entire button budget and pushed the like button off the visible
     * row on small heads. Sleep timer is still reachable from the phone
     * NowPlayingSheet; the [ACTION_SLEEP_TIMER] command stays registered so
     * the phone VM's existing send path keeps working unchanged.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun buildCustomLayout(
        liked: Boolean,
        sleepActive: Boolean,
        sleepRemainingMs: Long,
        sleepEndOfTrack: Boolean,
    ): ImmutableList<CommandButton> =
        ImmutableList.of(
            buildLikeButton(liked),
            buildShuffleButton(currentShuffle),
            buildRepeatButton(currentRepeatMode),
        )

    private fun buildLikeButton(liked: Boolean): CommandButton =
        CommandButton.Builder()
            .setSessionCommand(toggleLikeCommand)
            .setDisplayName(if (liked) "Rimuovi mi piace" else "Mi piace")
            .setIconResId(
                if (liked) R.drawable.ic_favorite else R.drawable.ic_favorite_border
            )
            .build()

    private fun buildShuffleButton(enabled: Boolean): CommandButton =
        CommandButton.Builder()
            .setSessionCommand(toggleShuffleCommand)
            .setDisplayName(if (enabled) "Casuale attivo" else "Casuale")
            .setIconResId(if (enabled) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle)
            .build()

    private fun buildRepeatButton(mode: Int): CommandButton {
        val (label, icon) = when (mode) {
            Player.REPEAT_MODE_ONE -> "Ripeti brano" to R.drawable.ic_repeat_one_on
            Player.REPEAT_MODE_ALL -> "Ripeti tutto" to R.drawable.ic_repeat_on
            else -> "Ripeti" to R.drawable.ic_repeat
        }
        return CommandButton.Builder()
            .setSessionCommand(cycleRepeatCommand)
            .setDisplayName(label)
            .setIconResId(icon)
            .build()
    }

    /**
     * Auto-resume playback when the first car controller (Android Auto /
     * Automotive) attaches. Three states matter:
     *  - already playing → no-op (some heads connect mid-session).
     *  - queue loaded but paused → just `prepare()` (if idle) + `play()`.
     *  - cold start, queue empty → seed from the saved [PlaybackResumption]
     *    snapshot (same data the resume chip uses), prepare, play.
     *
     * Hops via [mainScope] so we can `await` [AuthBootstrap.ready] before
     * touching the player — otherwise a cold-process AA connect can fire
     * the stream request before the silent sign-in coroutine has set the
     * Bearer token, and the backend rejects the audio fetch with 401.
     * Player methods must run on the application looper, hence
     * [Dispatchers.Main] inside [mainScope].
     */
    private fun autoResumeForCar(session: MediaSession) {
        mainScope.launch {
            AuthBootstrap.ready.await()
            val p = session.player
            if (p.isPlaying) return@launch
            if (p.mediaItemCount > 0) {
                if (p.playbackState == Player.STATE_IDLE) p.prepare()
                p.play()
                return@launch
            }
            val snapshot = resumption?.load() ?: return@launch
            p.setMediaItems(snapshot.items, snapshot.startIndex, snapshot.startPositionMs)
            p.prepare()
            p.play()
        }
    }

    /**
     * Reacts to [androidx.car.app.connection.CarConnection] LiveData updates
     * (main thread). PROJECTION = Android Auto on a head unit; NATIVE would
     * be Automotive OS. Anything else means "not in a car" — including all
     * the gearhead controller churn that used to fake car exits.
     */
    private fun onCarConnectionTypeChanged(type: Int) {
        val inCar =
            type != androidx.car.app.connection.CarConnection.CONNECTION_TYPE_NOT_CONNECTED
        Log.i(CAR_LIFECYCLE_TAG, "car connection type=$type inCar=$inCar (was $carProjectionActive)")
        if (inCar == carProjectionActive) return
        carProjectionActive = inCar
        val session = mediaSession ?: return
        if (inCar) {
            // Entered the car (or projection blip recovered) — abort any
            // pending leave-vehicle pause and resume where we left off.
            cancelCarDisconnectPause()
            aaLyricsTicker?.setAaConnected(true)
            autoResumeForCar(session)
        } else {
            aaLyricsTicker?.setAaConnected(false)
            scheduleCarDisconnectPause(session)
        }
    }

    /**
     * Schedule the "user left the vehicle" pause after car projection drops,
     * deferred by [CAR_DISCONNECT_PAUSE_DELAY_MS] so a transient projection
     * blip (wireless AA re-handshake) that recovers within the window — and
     * cancels this via [cancelCarDisconnectPause] — doesn't stop playback
     * mid-drive. Pause (not stop) keeps the queue/position intact so the
     * next car connect resumes exactly where it left off.
     *
     * mainScope so the delayed `player.pause()` runs on the application
     * looper; the job is parented to serviceScope so teardown cancels it.
     */
    private fun scheduleCarDisconnectPause(session: MediaSession) {
        carDisconnectPauseJob?.cancel()
        carDisconnectPauseJob = mainScope.launch {
            kotlinx.coroutines.delay(CAR_DISCONNECT_PAUSE_DELAY_MS)
            // Re-check at fire time: a projection recovery should have
            // cancelled this job, but guard against an interleaved cancel +
            // reschedule. Both this job and the observer run on main, so a
            // plain read is safe. Also keep playing if a car controller is
            // still connected — the CarConnection LiveData lags real reconnect
            // on wireless AA/BT, so a stale `false` here would otherwise pause
            // mid-drive right after a projection blip.
            when {
                carProjectionActive ->
                    Log.i(CAR_LIFECYCLE_TAG, "grace elapsed but projection is back, keep playing")
                hasConnectedCarController(session) ->
                    Log.i(CAR_LIFECYCLE_TAG, "grace elapsed but a car controller is still connected, keep playing")
                else -> {
                    Log.i(CAR_LIFECYCLE_TAG, "grace elapsed, pausing (user left vehicle)")
                    session.player.pause()
                }
            }
        }
    }

    /** Cancel a pending [scheduleCarDisconnectPause] — projection came back,
     *  so the drop was a transient blip, not the user leaving. */
    private fun cancelCarDisconnectPause() {
        carDisconnectPauseJob?.cancel()
        carDisconnectPauseJob = null
    }

    /**
     * True if a head-unit / car-kit controller (Android Auto, Automotive, or a
     * Bluetooth car controller) is still connected to the session. Used as a
     * second opinion at grace-pause fire time because the [CarConnection]
     * projection LiveData lags real reconnect on wireless AA/BT — if a car
     * controller is present, a projection `false` is almost certainly a stale
     * blip, so we keep playing rather than pause mid-drive.
     */
    private fun hasConnectedCarController(session: MediaSession): Boolean =
        session.connectedControllers.any { it.packageName in CAR_OR_BT_CONTROLLER_PACKAGES }

    private fun playWhenReadyReason(reason: Int): String = when (reason) {
        Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST -> "USER_REQUEST"
        Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS -> "AUDIO_FOCUS_LOSS"
        Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY -> "AUDIO_BECOMING_NOISY"
        Player.PLAY_WHEN_READY_CHANGE_REASON_REMOTE -> "REMOTE"
        Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM -> "END_OF_MEDIA_ITEM"
        Player.PLAY_WHEN_READY_CHANGE_REASON_SUPPRESSED_TOO_LONG -> "SUPPRESSED_TOO_LONG"
        else -> "UNKNOWN($reason)"
    }

    private fun suppressionReason(reason: Int): String = when (reason) {
        Player.PLAYBACK_SUPPRESSION_REASON_NONE -> "NONE"
        Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS ->
            "TRANSIENT_AUDIO_FOCUS_LOSS"
        else -> "OTHER($reason)"
    }

    /**
     * Transient transport error → schedule prepare()+play() with backoff.
     * After a fatal error the player sits in STATE_IDLE at the current
     * position; prepare() resumes from exactly there. Only fires while the
     * user still wants playback (playWhenReady) and gives up once the
     * ladder is exhausted so a genuinely dead server doesn't loop forever.
     * Runs on the player listener thread (= main), as does the reset in
     * onPlaybackStateChanged, so the attempt counter needs no lock.
     */
    private fun maybeRetryAfterStreamError(error: PlaybackException) {
        if (error.errorCode !in RETRYABLE_STREAM_ERROR_CODES) return
        val p = mediaSession?.player ?: return
        if (!p.playWhenReady) return
        if (streamRetryAttempt >= STREAM_RETRY_DELAYS_MS.size) {
            Log.w(STOP_DIAG_TAG, "retry ladder exhausted, staying idle")
            return
        }
        val delayMs = STREAM_RETRY_DELAYS_MS[streamRetryAttempt]
        streamRetryAttempt++
        streamRetryJob?.cancel()
        streamRetryJob = mainScope.launch {
            kotlinx.coroutines.delay(delayMs)
            // Re-read intent after the wait. The ladder goes up to 30s, and a
            // user who paused during it has said what they want — resuming
            // anyway is the "music started on its own" complaint. Checking only
            // at schedule time was not enough: nothing cancels this job on
            // pause, because the counter reset keys off STATE_READY, which
            // never arrives while the player sits idle after the error.
            if (!p.playWhenReady) {
                Log.i(STOP_DIAG_TAG, "retry abandoned: paused during the wait")
                return@launch
            }
            Log.i(
                STOP_DIAG_TAG,
                "retrying stream (attempt $streamRetryAttempt after ${delayMs}ms)",
            )
            p.prepare()
            p.play()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        // If the user swipes the app away while paused, tear down so the
        // media notification disappears with it. When we're actively
        // playing, keep the service alive — that's the whole point of
        // foreground playback.
        //
        // onTaskRemoved is invoked by ActivityManager on a binder thread,
        // but Player is single-thread-confined to its application looper
        // (= main). Post the read+stopSelf back to main so we don't trip
        // Media3's wrong-thread guard.
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            val player = mediaSession?.player
            if (player != null && !player.playWhenReady) {
                stopSelf()
            }
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        EqualizerController.release()
        // Stop watching projection state before tearing the session down so
        // a late LiveData emission can't touch a released player.
        carConnection?.type?.removeObserver(carConnectionObserver)
        carConnection = null
        // Drop the lyrics listener before serviceScope is cancelled so the
        // ticker stops cleanly without a stray replaceMediaItem on a
        // half-released player.
        aaLyricsTicker?.uninstall()
        aaLyricsTicker = null
        serviceScope.cancel()
        // Release the prefetch orchestrator *before* tearing down the
        // player — it holds a Player.Listener and a NetworkCallback and
        // needs the player alive to unhook cleanly.
        prefetch?.release()
        prefetch = null
        // Unhook the endless-queue + phantom-heal listeners before release.
        endlessQueue?.release()
        endlessQueue = null
        phantomHealer?.release()
        phantomHealer = null
        mediaSession?.run {
            resumptionListener?.let { player.removeListener(it) }
            player.release()
            release()
            mediaSession = null
        }
        resumption = null
        resumptionListener = null
        super.onDestroy()
    }

    /**
     * Android Auto browse tree + playback entrypoints.
     *
     * All heavy lifting (HTTP calls, mediaId parsing) lives in
     * [LibraryTree]; this inner class only bridges the suspend API to
     * Media3's `ListenableFuture` contract and handles queue expansion
     * for playlist taps.
     */
    private inner class LibraryCallback : MediaLibrarySession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            // Accept ALL controllers so standard transport commands (play /
            // pause / skipNext / skipPrev) reach the player regardless of which
            // package routes them. Steering-wheel keys on some OEMs (Xiaomi /
            // MIUI in particular) arrive via a non-Google Bluetooth stack
            // package not on our allowlist — rejecting those silently dropped
            // every wheel press. Custom session commands (toggle like, sleep
            // timer) remain gated below to known media surfaces only.
            val pkg = controller.packageName
            val trusted = pkg == applicationContext.packageName ||
                pkg in ALLOWED_CONTROLLER_PACKAGES
            val connectionResult = super.onConnect(session, controller)
            val availableSessionCommands = if (trusted) {
                connectionResult.availableSessionCommands.buildUpon()
                    .add(toggleLikeCommand)
                    .add(sleepTimerCommand)
                    .add(toggleShuffleCommand)
                    .add(cycleRepeatCommand)
                    .add(dropFromSourceCommand)
                    .build()
            } else {
                connectionResult.availableSessionCommands
            }
            // Repeat is native, so grant COMMAND_SET_REPEAT_MODE — Android Auto
            // only shows its repeat overlay when the controller advertises it
            // (relying on defaults has bitten us in DHU before). Shuffle is
            // app-level and exposed through our own [toggleShuffleCommand]
            // custom button, so we deliberately do NOT grant the native
            // COMMAND_SET_SHUFFLE_MODE to anyone: that would surface a second,
            // dead shuffle control that fights our timeline reordering.
            //
            // Repeat stays native for our OWN controllers (phone VM sets
            // c.repeatMode directly, widget too), but for Android Auto we drop
            // COMMAND_SET_REPEAT_MODE so its native repeat overlay disappears
            // and only our custom cycle-repeat button shows — no double control.
            val ownController = pkg == applicationContext.packageName
            val availablePlayerCommands = connectionResult.availablePlayerCommands
                .buildUpon()
                .remove(androidx.media3.common.Player.COMMAND_SET_SHUFFLE_MODE)
                .apply {
                    if (ownController) add(androidx.media3.common.Player.COMMAND_SET_REPEAT_MODE)
                    else remove(androidx.media3.common.Player.COMMAND_SET_REPEAT_MODE)
                }
                .build()
            return MediaSession.ConnectionResult.accept(
                availableSessionCommands,
                availablePlayerCommands,
            )
        }

        // Car controller connects/disconnects are logged for diagnostics but
        // deliberately drive NO behavior. gearhead's legacy controllers
        // "disconnect" on a 5-minute inactivity timeout and connect during
        // carless background scans — acting on these events is what caused
        // the phantom pauses (in the car AND on Bluetooth headphones).
        // Car entry/exit is handled by [onCarConnectionTypeChanged].

        override fun onPostConnect(session: MediaSession, controller: MediaSession.ControllerInfo) {
            super.onPostConnect(session, controller)
            if (controller.packageName in CAR_CONTROLLER_PACKAGES) {
                Log.i(
                    CAR_LIFECYCLE_TAG,
                    "controller connect pkg=${controller.packageName}" +
                        " (no-op, projection=$carProjectionActive)",
                )
            }
        }

        override fun onDisconnected(session: MediaSession, controller: MediaSession.ControllerInfo) {
            if (controller.packageName in CAR_CONTROLLER_PACKAGES) {
                Log.i(
                    CAR_LIFECYCLE_TAG,
                    "controller disconnect pkg=${controller.packageName}" +
                        " (no-op, projection=$carProjectionActive)",
                )
            }
            super.onDisconnected(session, controller)
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> = serviceScope.future {
            when (customCommand.customAction) {
                ACTION_TOGGLE_LIKE -> {
                    // Player + session must be touched on the main thread —
                    // serviceScope runs on Dispatchers.IO, so a direct read
                    // throws IllegalStateException ("Player is accessed on
                    // the wrong thread") and aborts the toggle silently.
                    val item = withContext(Dispatchers.Main) {
                        session.player.currentMediaItem
                    }
                    val songId = songIdOf(item)
                        ?: return@future SessionResult(SessionError.ERROR_INVALID_STATE)
                    val title = item?.mediaMetadata?.title?.toString()
                    val artist = item?.mediaMetadata?.artist?.toString()
                    val label = listOfNotNull(
                        title?.takeIf { it.isNotBlank() },
                        artist?.takeIf { it.isNotBlank() },
                    ).joinToString(" — ").ifBlank { null }
                    try {
                        val nowLiked = !currentLiked
                        if (nowLiked) likedRepository.like(songId, displayLabel = label)
                        else likedRepository.unlike(songId, displayLabel = label)
                        com.mediaplayer.android.data.LikedSongsCache.markLiked(songId, nowLiked)
                        withContext(Dispatchers.Main) {
                            // Only flip the shared heart state if the liked
                            // track is still the current one — a skip during
                            // the network call otherwise painted song A's
                            // liked state onto song B's card.
                            if (songIdOf(session.player.currentMediaItem) == songId) {
                                currentLiked = nowLiked
                                updateCustomLayout()
                            }
                        }
                        SessionResult(SessionResult.RESULT_SUCCESS)
                    } catch (_: Exception) {
                        SessionResult(SessionError.ERROR_UNKNOWN)
                    }
                }
                ACTION_TOGGLE_SHUFFLE -> {
                    // Shuffle is app-level and owned by [EndlessQueueController].
                    // Flip the shared shuffle pref; the service's shuffle-pref
                    // collector reorders the whole source pool on the app looper
                    // and refreshes the AA button. Same path a phone toggle
                    // takes, so behaviour is identical and works headless.
                    val now = PlaybackPrefs.currentShuffle(applicationContext)
                    PlaybackPrefs.setShuffle(applicationContext, !now)
                    SessionResult(SessionResult.RESULT_SUCCESS)
                }
                ACTION_CYCLE_REPEAT -> {
                    withContext(Dispatchers.Main) {
                        val p = session.player
                        p.repeatMode = when (p.repeatMode) {
                            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                            else -> Player.REPEAT_MODE_OFF
                        }
                    }
                    SessionResult(SessionResult.RESULT_SUCCESS)
                }
                ACTION_DROP_FROM_SOURCE -> {
                    val songId = args.getLong(EXTRA_DROP_SONG_ID, 0L)
                        .takeIf { it != 0L }
                        ?: customCommand.customExtras.getLong(EXTRA_DROP_SONG_ID, 0L)
                    if (songId != 0L) {
                        withContext(Dispatchers.Main) { endlessQueue?.dropFromSource(songId) }
                    }
                    SessionResult(SessionResult.RESULT_SUCCESS)
                }
                ACTION_SLEEP_TIMER -> {
                    // Phone VM sends the minute count via `args`; AA preset
                    // chips bake their value into the button's SessionCommand
                    // `customExtras`. Inspect both keys (`minutes`, `end_of_track`)
                    // across both bundles so either entry point routes to the
                    // same logic.
                    //
                    // The AA cancel chip carries neither key — that's the
                    // "raw cancel" intent. Phone sheet preset/end-of-track
                    // taps while a timer is armed must REPLACE the timer
                    // (`SleepTimer.set` / `setEndOfTrack` already cancel the
                    // current job before re-arming).
                    val sources = listOf(args, customCommand.customExtras)
                    val hasMinutes = sources.any { it.containsKey("minutes") }
                    val hasEndOfTrack = sources.any { it.containsKey("end_of_track") }
                    val endOfTrack = sources.any { it.getBoolean("end_of_track", false) }
                    val minutesSource = sources.firstOrNull { it.containsKey("minutes") }
                    val minutes = minutesSource?.getInt("minutes") ?: defaultSleepMinutes
                    // SleepTimer.{set,setEndOfTrack,cancel} touch
                    // Player.addListener/removeListener synchronously, so the
                    // whole branch must run on the main thread. serviceScope
                    // is Dispatchers.IO — without this hop, Player throws
                    // "accessed on the wrong thread".
                    withContext(Dispatchers.Main) {
                        when {
                            !hasMinutes && !hasEndOfTrack -> sleepTimer.cancel()
                            endOfTrack -> sleepTimer.setEndOfTrack(session.player) {
                                Log.i(STOP_DIAG_TAG, "sleep timer (end of track) fired, pausing")
                                session.player.pause()
                            }
                            minutes <= 0 -> sleepTimer.cancel()
                            else -> sleepTimer.set(minutes) {
                                Log.i(STOP_DIAG_TAG, "sleep timer (${minutes}m) fired, pausing")
                                session.player.pause()
                            }
                        }
                    }
                    SessionResult(SessionResult.RESULT_SUCCESS)
                }
                else -> SessionResult(SessionError.ERROR_NOT_SUPPORTED)
            }
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> =
            serviceScope.future {
                // Hand AA a LibraryParams whose extras advertise content-style
                // support so per-folder GRID/LIST hints (set on each section's
                // MediaMetadata in LibraryTree) are honoured by the AA UI.
                val rootParams = LibraryParams.Builder()
                    .setExtras(LibraryTree.rootExtras())
                    .build()
                LibraryResult.ofItem(LibraryTree.root(), rootParams)
            }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> =
            serviceScope.future {
                AuthBootstrap.ready.await()
                // LibraryTree.item may hit the backend (playlist details,
                // etc.). A network failure shouldn't propagate as a failed
                // future — AA surfaces those as generic crashes that
                // poison the resume chip / deep-link path for the rest
                // of the session. Map any throw to ERROR_IO instead.
                val item = try {
                    LibraryTree.item(mediaId)
                } catch (_: Exception) {
                    return@future LibraryResult.ofError(SessionError.ERROR_IO)
                }
                item?.let { LibraryResult.ofItem(it, /* params = */ null) }
                    ?: LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
            }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> =
            serviceScope.future {
                // Wait for silent auth before hitting the backend. On AA
                // cold-start MainActivity never runs, so AuthBootstrap (kicked
                // from MediaPlayerApp.onCreate) is what actually populates
                // AuthTokenHolder. Without this gate the very first browse
                // call hits the wire token-less and the backend returns 401,
                // leaving the AA library blank for the whole session.
                AuthBootstrap.ready.await()
                // Custom queue folder: snapshot the player's timeline on the
                // application main thread (Player is single-thread-confined)
                // and render via LibraryTree. Done here instead of inside
                // LibraryTree so the singleton stays player-agnostic. Queue
                // browsing is local-only — bypass the auth gate so the user
                // can still review what's playing offline.
                if (parentId == LibraryTree.QUEUE_ID) {
                    val (timeline, currentIndex) = withContext(Dispatchers.Main) {
                        val p = session.player
                        val items = (0 until p.mediaItemCount).map { p.getMediaItemAt(it) }
                        items to p.currentMediaItemIndex
                    }
                    return@future LibraryResult.ofItemList(
                        ImmutableList.copyOf(LibraryTree.queueChildren(timeline, currentIndex)),
                        params,
                    )
                }

                // If silent sign-in didn't yield a token, every backend call
                // below would 401 and AA would render a blank panel with no
                // indication of what's wrong. Surface a single info row so
                // the driver knows to open the app on the phone.
                if (AuthTokenHolder.idToken == null) {
                    return@future LibraryResult.ofItemList(
                        ImmutableList.of(
                            LibraryTree.infoItem("Apri MusicHub sul telefono per accedere")
                        ),
                        params,
                    )
                }

                // Player must be read on its application looper (main).
                val currentItem = withContext(Dispatchers.Main) {
                    session.player.currentMediaItem
                }
                val currentSongId = currentItem?.mediaId?.removePrefix("song:")?.toLongOrNull()

                val items = try {
                    LibraryTree.children(parentId, currentSongId, page, pageSize)
                } catch (e: Exception) {
                    // Backend unreachable / 401 / timeout. Don't let the future
                    // fail — AA renders a generic error and the user has no
                    // clue what happened. A single info row is far clearer.
                    return@future LibraryResult.ofItemList(
                        ImmutableList.of(
                            LibraryTree.infoItem("Server irraggiungibile, riprova")
                        ),
                        params,
                    )
                }
                if (items == null) {
                    LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
                } else {
                    LibraryResult.ofItemList(ImmutableList.copyOf(items), params)
                }
            }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<Void>> =
            serviceScope.future {
                AuthBootstrap.ready.await()
                // Probe the first page to get an item count for AA's UI.
                // The actual paged hits are fetched lazily in onGetSearchResult.
                val firstPage = LibraryTree.search(query, page = 0, pageSize = 50)
                session.notifySearchResultChanged(browser, query, firstPage.size, params)
                LibraryResult.ofVoid()
            }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> =
            serviceScope.future {
                AuthBootstrap.ready.await()
                val hits = LibraryTree.search(query, page, pageSize)
                LibraryResult.ofItemList(ImmutableList.copyOf(hits), params)
            }

        /**
         * Called when the controller sets the queue (e.g. AA tap on a leaf).
         * Two things happen here:
         *
         * 1. Queue expansion. A tap on a `pl:{pid}:{pos}:{sid}` leaf inside
         *    a playlist should enqueue the whole playlist starting at that
         *    position — matches [PlaybackViewModel.playPlaylist] on phone.
         *    A tap on a `song:{id}` leaf (under all-songs or search) stays
         *    a single-item queue.
         * 2. Stream URI attachment. Browse-side MediaItems carry metadata
         *    but no URI (they're not meant to be played as-is); we resolve
         *    each id into a playable MediaItem via [LibraryTree].
         */
        /**
         * Called by Android Auto on cold car connect to populate the
         * "resume where you left off" chip. We hand back the last queue
         * persisted by [PlaybackResumption]; when nothing has been saved
         * yet, returning an immediate-failed future makes AA simply
         * omit the chip rather than show an empty placeholder.
         */
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val snapshot = resumption?.load()
                ?: return Futures.immediateFailedFuture(
                    UnsupportedOperationException("No saved playback state")
                )
            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(
                    snapshot.items,
                    snapshot.startIndex,
                    snapshot.startPositionMs,
                )
            )
        }

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> =
            serviceScope.future {
                AuthBootstrap.ready.await()
                // Voice-search path: AA / Google Assistant deliver
                // "Hey Google, play X on MediaPlayer" as a single MediaItem
                // whose only payload is RequestMetadata.searchQuery. Resolve
                // the query into a song queue so playback starts immediately
                // instead of failing with "no media to play".
                if (mediaItems.size == 1) {
                    val searchQuery = mediaItems[0].requestMetadata.searchQuery
                    if (!searchQuery.isNullOrBlank() &&
                        mediaItems[0].mediaId.isEmpty()
                    ) {
                        val hits = runCatching {
                            LibraryTree.search(searchQuery, page = 0, pageSize = 50)
                        }.getOrDefault(emptyList())
                        val playable = hits.mapNotNull { item ->
                            item.mediaId.removePrefix("song:").toLongOrNull()?.let { sid ->
                                runCatching { LibraryTree.playableForSong(sid) }.getOrNull()
                            }
                        }
                        if (playable.isNotEmpty()) {
                            return@future MediaSession.MediaItemsWithStartPosition(
                                playable, 0, C.TIME_UNSET
                            )
                        }
                        // Fall through to the default handling below if the
                        // search produced nothing; AA will surface "no results".
                    }
                }

                if (mediaItems.size == 1) {
                    val id = mediaItems[0].mediaId

                    // Playlist leaf → expand the whole playlist starting at pos.
                    LibraryTree.parsePlaylistLeaf(id)?.let { (pid, pos, _) ->
                        val q = runCatching { LibraryTree.playlistQueue(pid) }
                            .getOrDefault(emptyList())
                        if (q.isNotEmpty()) return@future MediaSession.MediaItemsWithStartPosition(
                            q, pos.coerceAtMost(q.lastIndex.coerceAtLeast(0)), C.TIME_UNSET
                        )
                    }

                    // Album leaf → expand album from chosen position.
                    LibraryTree.parseAlbumLeaf(id)?.let { quad ->
                        val q = runCatching { LibraryTree.albumQueue(quad.a, quad.b) }
                            .getOrDefault(emptyList())
                        if (q.isNotEmpty()) return@future MediaSession.MediaItemsWithStartPosition(
                            q, quad.c.coerceAtMost(q.lastIndex.coerceAtLeast(0)), C.TIME_UNSET
                        )
                    }

                    // Artist leaf → expand artist's full song list from pos.
                    LibraryTree.parseArtistLeaf(id)?.let { (name, pos, _) ->
                        val q = runCatching { LibraryTree.artistQueue(name) }
                            .getOrDefault(emptyList())
                        if (q.isNotEmpty()) return@future MediaSession.MediaItemsWithStartPosition(
                            q, pos.coerceAtMost(q.lastIndex.coerceAtLeast(0)), C.TIME_UNSET
                        )
                    }

                    // Genre leaf → expand genre's song list from pos.
                    LibraryTree.parseGenreLeaf(id)?.let { (tag, pos, _) ->
                        val q = runCatching { LibraryTree.genreQueue(tag) }
                            .getOrDefault(emptyList())
                        if (q.isNotEmpty()) return@future MediaSession.MediaItemsWithStartPosition(
                            q, pos.coerceAtMost(q.lastIndex.coerceAtLeast(0)), C.TIME_UNSET
                        )
                    }

                    // Queue leaf → re-hand the existing player timeline with
                    // the chosen index so AA effectively jumps to that row
                    // without us rebuilding the queue. The MediaItems are
                    // the same instances the player already owns (URIs +
                    // KEY_USER_QUEUED extras preserved).
                    LibraryTree.parseQueueLeaf(id)?.let { (pos, sid) ->
                        val current = withContext(Dispatchers.Main) {
                            val p = mediaSession.player
                            (0 until p.mediaItemCount).map { p.getMediaItemAt(it) }
                        }
                        // Identity first, position second. The leaf's pos was
                        // baked when the Coda folder was browsed and the endless
                        // engine prunes and refills underneath, so by the time
                        // the driver taps a row that index can point at a
                        // different song. The leaf also carries the song id, so
                        // honour what was actually tapped and keep pos only as a
                        // fallback — coerced, since an out-of-range start index
                        // throws IllegalSeekPositionException.
                        val bySongId = current.indexOfFirst {
                            it.mediaId.removePrefix("song:").toLongOrNull() == sid
                        }
                        val startIdx = if (bySongId >= 0) {
                            bySongId
                        } else {
                            pos.coerceIn(0, (current.size - 1).coerceAtLeast(0))
                        }
                        return@future MediaSession.MediaItemsWithStartPosition(
                            current, startIdx, C.TIME_UNSET
                        )
                    }

                    // Liked leaf → expand liked collection from pos.
                    LibraryTree.parseSimpleLeaf(id, "lk:")?.let { (pos, _) ->
                        val q = runCatching { LibraryTree.likedQueue() }
                            .getOrDefault(emptyList())
                        if (q.isNotEmpty()) return@future MediaSession.MediaItemsWithStartPosition(
                            q, pos.coerceAtMost(q.lastIndex.coerceAtLeast(0)), C.TIME_UNSET
                        )
                    }

                    // Recents leaf → expand recents queue from pos.
                    LibraryTree.parseSimpleLeaf(id, "rc:")?.let { (pos, _) ->
                        val q = runCatching { LibraryTree.recentsQueue() }
                            .getOrDefault(emptyList())
                        if (q.isNotEmpty()) return@future MediaSession.MediaItemsWithStartPosition(
                            q, pos.coerceAtMost(q.lastIndex.coerceAtLeast(0)), C.TIME_UNSET
                        )
                    }

                    // Bare song leaf (search result, voice fallback) — the only
                    // leaf with no browsing context of its own. Play it first,
                    // then back it with a catalog page: a one-item timeline has
                    // no next item and the endless engine won't refill from a
                    // pool of one, so skip would stay dead until the user
                    // started playback again from somewhere else.
                    if (id.startsWith("song:")) {
                        val songId = id.removePrefix("song:").toLongOrNull()
                        if (songId != null) {
                            val head = mediaItems[0].buildUpon()
                                .setUri(Network.streamUrl(songId))
                                .build()
                            // Offline or backend down: fall back to the bare
                            // item rather than failing the tap outright.
                            val pool = runCatching { LibraryTree.allSongsQueue() }
                                .getOrDefault(emptyList())
                                .filterNot { it.mediaId == id }
                            return@future MediaSession.MediaItemsWithStartPosition(
                                listOf(head) + pool, 0, startPositionMs
                            )
                        }
                    }
                }

                MediaSession.MediaItemsWithStartPosition(mediaItems, startIndex, startPositionMs)
            }
    }
}
