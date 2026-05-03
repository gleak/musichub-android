package com.mediaplayer.android.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.mediaplayer.android.data.SongRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Handler for the "Report wrong song" SongRow kebab entry.
 *
 * Fires the global flag-wrong API call (which deletes the file/cover
 * server-side and hard-removes references from playlists/likes/history)
 * then invokes [onFlagged] on the main thread so the calling screen can
 * drop the song from its local list state.
 */
@Composable
fun rememberFlagWrongAction(
    songId: Long,
    onFlagged: (Long) -> Unit = {},
): () -> Unit {
    val scope = rememberCoroutineScope()
    val repo = remember { SongRepository() }
    return remember(songId, onFlagged) {
        {
            if (songId > 0) {
                scope.launch {
                    runCatching { withContext(Dispatchers.IO) { repo.flagWrong(songId) } }
                    onFlagged(songId)
                }
            }
        }
    }
}
