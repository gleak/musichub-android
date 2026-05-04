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
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
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
import com.mediaplayer.android.MainActivity
import com.mediaplayer.android.R
import com.mediaplayer.android.data.LikedRepository
import com.mediaplayer.android.data.Network
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
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
    private val sleepTimer = SleepTimer(serviceScope)
    private val sleepTimerCommand =
        SessionCommand(ACTION_SLEEP_TIMER, Bundle.EMPTY)
    /** Default minutes used when AA presses the Sleep button without args. */
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

        mediaSession = MediaLibrarySession.Builder(this, player, LibraryCallback())
            .setSessionActivity(pendingIntent)
            .setCustomLayout(buildCustomLayout(liked = false, sleepActive = false))
            .build()

        // Mirror service-owned sleep timer state to controllers (phone VM + AA)
        // via session extras + a refresh of the custom layout.
        serviceScope.launch {
            sleepTimer.isActive.collectLatest { active ->
                mediaSession?.let { session ->
                    session.setSessionExtras(buildSessionExtras(currentLiked, active))
                    session.setCustomLayout(buildCustomLayout(currentLiked, active))
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
        aaLyricsTicker = AALyricsTicker(player, serviceScope).also { it.install() }

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                refreshLikeButtonForCurrent(mediaItem)
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    fadeInOnAutoTransition()
                }
            }
        })
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
            updateCustomLayout()
        }
    }

    private fun updateCustomLayout() {
        val sleepActive = sleepTimer.isActive.value
        mediaSession?.let { session ->
            session.setCustomLayout(buildCustomLayout(currentLiked, sleepActive))
            session.setSessionExtras(buildSessionExtras(currentLiked, sleepActive))
        }
    }

    private fun buildSessionExtras(liked: Boolean, sleepActive: Boolean): Bundle =
        Bundle().apply {
            putBoolean(EXTRA_LIKED, liked)
            putBoolean(EXTRA_SLEEP_ACTIVE, sleepActive)
        }

    private fun buildCustomLayout(liked: Boolean, sleepActive: Boolean): ImmutableList<CommandButton> =
        ImmutableList.of(
            buildLikeButton(liked),
            buildSleepButton(sleepActive),
        )

    private fun buildLikeButton(liked: Boolean): CommandButton =
        CommandButton.Builder()
            .setSessionCommand(toggleLikeCommand)
            .setDisplayName(if (liked) "Rimuovi mi piace" else "Mi piace")
            .setIconResId(
                if (liked) R.drawable.ic_favorite else R.drawable.ic_favorite_border
            )
            .build()

    private fun buildSleepButton(active: Boolean): CommandButton =
        CommandButton.Builder()
            .setSessionCommand(sleepTimerCommand)
            .setDisplayName(if (active) "Annulla timer" else "Sospendi tra ${defaultSleepMinutes}m")
            .setIconResId(
                if (active) R.drawable.ic_bedtime else R.drawable.ic_bedtime_off
            )
            .build()

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
            // Gate by package: only ourselves and known media controllers (Android
            // Auto, Assistant, system media controls, BT) get the session.
            // Reject everything else so other apps can't subscribe and abuse
            // our custom session commands.
            val pkg = controller.packageName
            val ours = pkg == applicationContext.packageName
            if (!ours && pkg !in ALLOWED_CONTROLLER_PACKAGES) {
                return MediaSession.ConnectionResult.reject()
            }
            val connectionResult = super.onConnect(session, controller)
            val availableSessionCommands = connectionResult.availableSessionCommands
                .buildUpon()
                .add(toggleLikeCommand)
                .add(sleepTimerCommand)
                .build()
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
                    val songId = songIdOf(session.player.currentMediaItem)
                        ?: return@future SessionResult(SessionError.ERROR_INVALID_STATE)
                    try {
                        if (currentLiked) likedRepository.unlike(songId)
                        else likedRepository.like(songId)
                        currentLiked = !currentLiked
                        updateCustomLayout()
                        SessionResult(SessionResult.RESULT_SUCCESS)
                    } catch (_: Exception) {
                        SessionResult(SessionError.ERROR_UNKNOWN)
                    }
                }
                ACTION_SLEEP_TIMER -> {
                    // 0 cancels; absent / negative defaults to AA's quick-set value.
                    val minutes = if (args.containsKey("minutes")) {
                        args.getInt("minutes")
                    } else {
                        defaultSleepMinutes
                    }
                    if (minutes <= 0 || sleepTimer.isActive.value) {
                        sleepTimer.cancel()
                    } else {
                        sleepTimer.set(minutes) { session.player.pause() }
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
                val currentItem = session.player.currentMediaItem
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
