package com.mediaplayer.android.widget

import androidx.media3.common.util.UnstableApi
import com.mediaplayer.android.data.LikedRepository
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.playback.PlaybackViewModel

/**
 * Resolves a [QuickLaunchKind] to a concrete playback action.
 *
 * For auto-playlist kinds we ask the backend for the user's playlist of
 * that kind (every signed-in user gets exactly one row per kind), then
 * fetch its songs and hand them to [PlaybackViewModel.playPlaylist]. For
 * [QuickLaunchKind.LIKED] we pull the first page of liked songs (sized
 * generously so the user gets a reasonable session) and shuffle-play.
 *
 * Errors are swallowed silently — widget taps must not surface dialogs
 * since the caller may be on the Now-Playing surface or a settings page.
 * A failed launch falls through to "open the app" which is the same
 * outcome as not having tapped the tile at all.
 */
@UnstableApi
object WidgetTargetLauncher {

    private val likedRepo by lazy { LikedRepository() }

    suspend fun launch(kind: QuickLaunchKind, playbackVm: PlaybackViewModel) {
        runCatching {
            when (kind) {
                QuickLaunchKind.LIKED -> launchLiked(playbackVm)
                else -> launchAutoPlaylist(kind, playbackVm)
            }
        }
    }

    private suspend fun launchLiked(playbackVm: PlaybackViewModel) {
        val page = likedRepo.likedSongs(page = 0, size = 200)
        val songs = page.items
        if (songs.isNotEmpty()) playbackVm.playPlaylistShuffled(songs)
    }

    private suspend fun launchAutoPlaylist(kind: QuickLaunchKind, playbackVm: PlaybackViewModel) {
        val backendKind = kind.backendKind ?: return
        val matches = Network.api.listPlaylists(kind = backendKind)
        val target = matches.firstOrNull() ?: return
        val detail = Network.api.getPlaylist(target.id)
        val songs = detail.songs.map { it.song }
        if (songs.isNotEmpty()) playbackVm.playPlaylist(songs, 0)
    }
}
