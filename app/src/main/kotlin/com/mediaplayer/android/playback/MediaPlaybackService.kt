package com.mediaplayer.android.playback

import android.content.Intent
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
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.mediaplayer.android.data.Network
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.guava.future

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
        // M13: check the persistent download cache first, then fall back to
        // the streaming cache, then network. setCacheWriteDataSinkFactory(null)
        // ensures playback never writes into the download cache — only
        // MediaDownloadService may do that.
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
            .build()

        mediaSession = MediaLibrarySession.Builder(this, player, LibraryCallback()).build()

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

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> =
            serviceScope.future {
                LibraryResult.ofItem(LibraryTree.root(), params)
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
                val items = LibraryTree.children(parentId)
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
                    val plLeaf = LibraryTree.parsePlaylistLeaf(id)
                    if (plLeaf != null) {
                        val (playlistId, position, _) = plLeaf
                        val expanded = LibraryTree.playlistQueue(playlistId)
                        return@future MediaSession.MediaItemsWithStartPosition(
                            expanded,
                            position,
                            C.TIME_UNSET
                        )
                    }
                    if (id.startsWith("song:")) {
                        val songId = id.removePrefix("song:").toLongOrNull()
                        if (songId != null) {
                            val playable = LibraryTree.playableForSong(songId)
                            return@future MediaSession.MediaItemsWithStartPosition(
                                listOf(playable),
                                0,
                                startPositionMs
                            )
                        }
                    }
                }

                MediaSession.MediaItemsWithStartPosition(mediaItems, startIndex, startPositionMs)
            }
    }
}
