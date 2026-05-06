package com.mediaplayer.android.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.ui.playlists.AddToPlaylistSheet

/**
 * Holds the "currently-open" song for a screen's kebab sheet.
 *
 * Replaces the per-screen `var sheetSong by remember ...` + manual
 * wiring of [rememberDislikeActions] / [rememberFlagWrongAction] +
 * [AddToPlaylistSheet] that used to repeat across every list screen.
 *
 * Caller owns one [SongKebabState] per screen and renders one
 * [SongKebabSheet]. Tapping a SongRow's kebab calls [open]; the sheet
 * auto-dismisses after any action.
 */
class SongKebabState internal constructor() {
    var current by mutableStateOf<SongDto?>(null)
        private set

    fun open(song: SongDto) {
        current = song
    }

    fun close() {
        current = null
    }
}

@Composable
fun rememberSongKebab(): SongKebabState = remember { SongKebabState() }

/**
 * Renders the actual [AddToPlaylistSheet] when a song is open in [state].
 * No-op until [SongKebabState.open] is called.
 *
 * Per-screen variations (which optional rows to show, what to do after a
 * playlist add or a flag-wrong report) come in as parameters here so the
 * sheet body itself stays uniform across every list screen.
 *
 * @param onPlayNext  When non-null, the kebab shows the "Riproduci dopo" row.
 * @param onAddToQueue  When non-null, shows the "Aggiungi alla coda" row.
 * @param onDownload  When non-null, shows the "Scarica" row.
 * @param onFlagged  Invoked on the main thread after a successful
 *  flag-wrong report. Use it to refresh the screen's local list. Ignored
 *  when [flagWrongOverride] is provided.
 * @param flagWrongOverride  Replaces the standard [rememberFlagWrongAction]
 *  flow with a caller-supplied lambda. The QueueSheet uses this to route
 *  through `PlaybackViewModel.flagWrong`, which both reports the song
 *  and prunes flagged entries from the active media-session timeline.
 * @param onAdded  Invoked after the user picks a playlist. The kebab
 *  state closes automatically afterwards — caller doesn't need to call
 *  [SongKebabState.close].
 */
@Composable
fun SongKebabSheet(
    state: SongKebabState,
    onPlayNext: ((SongDto) -> Unit)? = null,
    onAddToQueue: ((SongDto) -> Unit)? = null,
    onDownload: ((SongDto) -> Unit)? = null,
    onFlagged: () -> Unit = {},
    flagWrongOverride: ((SongDto) -> Unit)? = null,
    onAdded: (playlistName: String, song: SongDto) -> Unit = { _, _ -> },
) {
    val song = state.current ?: return
    val dislike = rememberDislikeActions(song.id, song.artist)
    val standardFlagWrong = rememberFlagWrongAction(
        songId = song.id,
        onFlagged = { onFlagged() },
    )
    val flagWrong: () -> Unit = if (flagWrongOverride != null) {
        { flagWrongOverride(song) }
    } else {
        standardFlagWrong
    }
    AddToPlaylistSheet(
        songTitle = song.title,
        songId = song.id,
        songArtist = song.artist,
        songHasCoverArt = song.hasCoverArt,
        onPlayNext = onPlayNext?.let { cb -> { cb(song); state.close() } },
        onAddToQueue = onAddToQueue?.let { cb -> { cb(song); state.close() } },
        onDownload = onDownload?.let { cb -> { cb(song); state.close() } },
        onDislikeSong = dislike.song(),
        onDislikeArtist = dislike.artist(),
        onFlagWrong = flagWrong,
        onDismiss = { state.close() },
        onAdded = { name -> onAdded(name, song); state.close() },
    )
}
