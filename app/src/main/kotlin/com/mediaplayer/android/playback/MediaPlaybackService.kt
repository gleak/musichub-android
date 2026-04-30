package com.mediaplayer.android.playback

import android.app.PendingIntent
import android.content.Intent
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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
    }

    private var mediaSession: MediaLibrarySession? = null
    private var resumption: PlaybackResumption? = null
    private var resumptionListener: Player.Listener? = null
    private var prefetch: PrefetchOrchestrator? = null

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
            .build()

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = android.app.PendingIntent.getActivity(
            this,
            0,
            intent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaLibrarySession.Builder(this, player, LibraryCallback())
            .setSessionActivity(pendingIntent)
            .setCustomLayout(ImmutableList.of(buildLikeButton(liked = false)))
            .build()

        // M13: bind the hardware Equalizer to this player's audio session.
        // audioSessionId is allocated at build time; 0 means unsupported.
        EqualizerController.init(this, player.audioSessionId)

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

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaSession?.notifyChildrenChanged(LibraryTree.ROOT_ID, 10, null)
                mediaSession?.notifyChildrenChanged(LibraryTree.ALL_SONGS_ID, 100, null)
                refreshLikeButtonForCurrent(mediaItem)
            }
        })
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
        mediaSession?.setCustomLayout(
            ImmutableList.of(buildLikeButton(currentLiked))
        )
    }

    private fun buildLikeButton(liked: Boolean): CommandButton =
        CommandButton.Builder()
            .setSessionCommand(toggleLikeCommand)
            .setDisplayName(if (liked) "Unlike" else "Like")
            .setIconResId(
                if (liked) R.drawable.ic_favorite else R.drawable.ic_favorite_border
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
            val connectionResult = super.onConnect(session, controller)
            val availableSessionCommands = connectionResult.availableSessionCommands
                .buildUpon()
                .add(toggleLikeCommand)
                .build()
            return MediaSession.ConnectionResult.accept(
                availableSessionCommands,
                connectionResult.availablePlayerCommands
            )
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> = serviceScope.future {
            if (customCommand.customAction != ACTION_TOGGLE_LIKE) {
                return@future SessionResult(SessionError.ERROR_NOT_SUPPORTED)
            }
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

                val items = LibraryTree.children(parentId, currentSongId)
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
                // Spec: we signal readiness here; the controller then calls
                // onGetSearchResult to pull the actual hits. We notify first
                // so paging hints line up with what we'll return.
                val hits = LibraryTree.search(query)
                session.notifySearchResultChanged(browser, query, hits.size, params)
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
                LibraryResult.ofItemList(ImmutableList.copyOf(LibraryTree.search(query)), params)
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
