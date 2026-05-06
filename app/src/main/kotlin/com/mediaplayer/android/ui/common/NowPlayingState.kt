package com.mediaplayer.android.ui.common

import androidx.compose.runtime.compositionLocalOf

/**
 * Lightweight broadcast of the currently-playing track to every list screen.
 *
 * Threaded via CompositionLocal so leaf composables (`SongRow`,
 * `PlaylistListRow`) can highlight the active row + render `MHPlayingBars`
 * without each caller having to plumb the playback ViewModel through.
 *
 * Set once in `MainActivity` from `PlaybackViewModel.currentSong` +
 * `PlaybackViewModel.isPlaying`. Defaults are no-op (no current track,
 * not playing) so unmounted previews and tests don't need to bind it.
 */
data class NowPlayingState(
    val currentSongId: Long? = null,
    val isPlaying: Boolean = false,
)

val LocalNowPlaying = compositionLocalOf { NowPlayingState() }
