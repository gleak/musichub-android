package com.mediaplayer.android.playback

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
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

    /**
     * Off-main scope for `MediaLibrarySession.Callback` work (browse tree
     * fetches, search). Bridged to Media3's `ListenableFuture` API via
     * `kotlinx-coroutines-guava`'s `future { ... }` builder.
     */
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        val httpFactory = OkHttpDataSource.Factory(Network.okHttp)
        val dataSourceFactory = DefaultDataSource.Factory(this, httpFactory)
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
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
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
                    ?: LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
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
                    LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
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
                        val queue = LibraryTree.playlistQueue(playlistId)
                        return@future MediaSession.MediaItemsWithStartPosition(
                            queue,
                            position,
                            startPositionMs,
                        )
                    }
                    if (id.startsWith("song:")) {
                        val sid = id.removePrefix("song:").toLongOrNull()
                        if (sid != null) {
                            return@future MediaSession.MediaItemsWithStartPosition(
                                listOf(LibraryTree.playableForSong(sid)),
                                /* startIndex = */ 0,
                                startPositionMs,
                            )
                        }
                    }
                }
                // Multi-item or unknown scheme — return as-is. The player
                // will still try, but unresolved items without URIs will
                // fail fast rather than silently no-op.
                MediaSession.MediaItemsWithStartPosition(
                    mediaItems,
                    startIndex,
                    startPositionMs,
                )
            }
    }
}
