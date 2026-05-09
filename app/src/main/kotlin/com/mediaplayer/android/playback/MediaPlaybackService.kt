package com.mediaplayer.android.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
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
    }

    private var mediaSession: MediaLibrarySession? = null
    private var resumption: PlaybackResumption? = null
    private var resumptionListener: Player.Listener? = null
    private var prefetch: PrefetchOrchestrator? = null
    private var crossfadeJob: Job? = null

    /**
     * Cached crossfade seconds — kept in sync with [PlayerSettings] via a
     * collect on [serviceScope]. Read on every auto-transition; the prior
     * `runBlocking { crossfadeSecondsNow() }` blocked the player looper on
     * a DataStore read on every track change.
     */
    @Volatile private var crossfadeSecondsCached: Int = 0

    /**
     * Number of currently-attached AA / Automotive controllers. The AA
     * lyrics ticker is disabled while this is 0 so phone-only playback
     * doesn't pay for `replaceMediaItem` per lyric line.
     */
    private var carControllerCount: Int = 0
    private var aaLyricsTicker: AALyricsTicker? = null

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
    @Volatile private var currentLiked: Boolean = false

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
        // Playback writes into the download cache so streamed songs become
        // available offline automatically. DownloadRepository.download() is
        // called after enough of the song is listened to, which causes
        // DownloadManager to find the data already cached and mark it complete.
        val downloadCache = DownloadRoot.getDownloadCache(this)
        val cacheFactory = CacheDataSource.Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(streamCacheFactory)
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

        mediaSession = MediaLibrarySession.Builder(this, player, LibraryCallback())
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
        serviceScope.launch {
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

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                refreshLikeButtonForCurrent(mediaItem)
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    fadeInOnAutoTransition()
                }
            }
        })

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
        val songId = item?.mediaId?.toLongOrNull()
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
        crossfadeJob?.cancel()
        crossfadeJob = serviceScope.launch {
            val totalMs = seconds * 1000L
            val stepMs = 50L
            val steps = (totalMs / stepMs).toInt().coerceAtLeast(1)
            for (i in 0..steps) {
                p.volume = i.toFloat() / steps
                kotlinx.coroutines.delay(stepMs)
            }
            p.volume = 1f
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
            currentLiked = try {
                likedRepository.status(listOf(songId)).contains(songId)
            } catch (_: Exception) {
                false
            }
            com.mediaplayer.android.data.LikedSongsCache.markLiked(songId, currentLiked)
            updateCustomLayout()
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
        ImmutableList.of(buildLikeButton(liked))

    private fun buildLikeButton(liked: Boolean): CommandButton =
        CommandButton.Builder()
            .setSessionCommand(toggleLikeCommand)
            .setDisplayName(if (liked) "Rimuovi mi piace" else "Mi piace")
            .setIconResId(
                if (liked) R.drawable.ic_favorite else R.drawable.ic_favorite_border
            )
            .build()

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

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        // If the user swipes the app away while paused, tear down so the
        // media notification disappears with it. When we're actively
        // playing, keep the service alive — that's the whole point of
        // foreground playback.
        val player = mediaSession?.player
        if (player != null && !player.playWhenReady) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        EqualizerController.release()
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
                    .build()
            } else {
                connectionResult.availableSessionCommands
            }
            // Explicitly grant shuffle + repeat to all connected controllers.
            // Media3's defaults usually include them, but Android Auto only shows
            // its shuffle/repeat overlay buttons when the controller advertises
            // these commands — relying on defaults has bitten us in DHU before.
            val availablePlayerCommands = connectionResult.availablePlayerCommands
                .buildUpon()
                .add(androidx.media3.common.Player.COMMAND_SET_SHUFFLE_MODE)
                .add(androidx.media3.common.Player.COMMAND_SET_REPEAT_MODE)
                .build()
            return MediaSession.ConnectionResult.accept(
                availableSessionCommands,
                availablePlayerCommands,
            )
        }

        override fun onPostConnect(session: MediaSession, controller: MediaSession.ControllerInfo) {
            super.onPostConnect(session, controller)
            if (controller.packageName in CAR_CONTROLLER_PACKAGES) {
                carControllerCount++
                if (carControllerCount == 1) {
                    aaLyricsTicker?.setAaConnected(true)
                    autoResumeForCar(session)
                }
            }
        }

        override fun onDisconnected(session: MediaSession, controller: MediaSession.ControllerInfo) {
            if (controller.packageName in CAR_CONTROLLER_PACKAGES) {
                carControllerCount = (carControllerCount - 1).coerceAtLeast(0)
                if (carControllerCount == 0) {
                    aaLyricsTicker?.setAaConnected(false)
                }
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
                        if (currentLiked) likedRepository.unlike(songId, displayLabel = label)
                        else likedRepository.like(songId, displayLabel = label)
                        currentLiked = !currentLiked
                        com.mediaplayer.android.data.LikedSongsCache.markLiked(songId, currentLiked)
                        withContext(Dispatchers.Main) { updateCustomLayout() }
                        SessionResult(SessionResult.RESULT_SUCCESS)
                    } catch (_: Exception) {
                        SessionResult(SessionError.ERROR_UNKNOWN)
                    }
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
                                session.player.pause()
                            }
                            minutes <= 0 -> sleepTimer.cancel()
                            else -> sleepTimer.set(minutes) { session.player.pause() }
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
                LibraryTree.item(mediaId)?.let { LibraryResult.ofItem(it, /* params = */ null) }
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
                // LibraryTree so the singleton stays player-agnostic.
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

                // Player must be read on its application looper (main).
                val currentItem = withContext(Dispatchers.Main) {
                    session.player.currentMediaItem
                }
                val currentSongId = currentItem?.mediaId?.removePrefix("song:")?.toLongOrNull()

                val items = LibraryTree.children(parentId, currentSongId, page, pageSize)
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
                        val hits = LibraryTree.search(searchQuery, page = 0, pageSize = 50)
                        val playable = hits.mapNotNull { item ->
                            item.mediaId.removePrefix("song:").toLongOrNull()?.let { sid ->
                                LibraryTree.playableForSong(sid)
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
                        return@future MediaSession.MediaItemsWithStartPosition(
                            LibraryTree.playlistQueue(pid), pos, C.TIME_UNSET
                        )
                    }

                    // Album leaf → expand album from chosen position.
                    LibraryTree.parseAlbumLeaf(id)?.let { quad ->
                        return@future MediaSession.MediaItemsWithStartPosition(
                            LibraryTree.albumQueue(quad.a, quad.b), quad.c, C.TIME_UNSET
                        )
                    }

                    // Artist leaf → expand artist's full song list from pos.
                    LibraryTree.parseArtistLeaf(id)?.let { (name, pos, _) ->
                        return@future MediaSession.MediaItemsWithStartPosition(
                            LibraryTree.artistQueue(name), pos, C.TIME_UNSET
                        )
                    }

                    // Genre leaf → expand genre's song list from pos.
                    LibraryTree.parseGenreLeaf(id)?.let { (tag, pos, _) ->
                        return@future MediaSession.MediaItemsWithStartPosition(
                            LibraryTree.genreQueue(tag), pos, C.TIME_UNSET
                        )
                    }

                    // Queue leaf → re-hand the existing player timeline with
                    // the chosen index so AA effectively jumps to that row
                    // without us rebuilding the queue. The MediaItems are
                    // the same instances the player already owns (URIs +
                    // KEY_USER_QUEUED extras preserved).
                    LibraryTree.parseQueueLeaf(id)?.let { (pos, _) ->
                        val current = withContext(Dispatchers.Main) {
                            val p = mediaSession.player
                            (0 until p.mediaItemCount).map { p.getMediaItemAt(it) }
                        }
                        return@future MediaSession.MediaItemsWithStartPosition(
                            current, pos, C.TIME_UNSET
                        )
                    }

                    // Liked leaf → expand liked collection from pos.
                    LibraryTree.parseSimpleLeaf(id, "lk:")?.let { (pos, _) ->
                        return@future MediaSession.MediaItemsWithStartPosition(
                            LibraryTree.likedQueue(), pos, C.TIME_UNSET
                        )
                    }

                    // Recents leaf → expand recents queue from pos.
                    LibraryTree.parseSimpleLeaf(id, "rc:")?.let { (pos, _) ->
                        return@future MediaSession.MediaItemsWithStartPosition(
                            LibraryTree.recentsQueue(), pos, C.TIME_UNSET
                        )
                    }

                    if (id.startsWith("song:")) {
                        val songId = id.removePrefix("song:").toLongOrNull()
                        if (songId != null) {
                            val mediaItem = mediaItems[0].buildUpon()
                                .setUri(Network.streamUrl(songId))
                                .build()
                            return@future MediaSession.MediaItemsWithStartPosition(
                                listOf(mediaItem), 0, startPositionMs
                            )
                        }
                    }
                }

                MediaSession.MediaItemsWithStartPosition(mediaItems, startIndex, startPositionMs)
            }
    }
}
