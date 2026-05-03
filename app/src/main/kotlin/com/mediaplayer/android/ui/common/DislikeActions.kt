package com.mediaplayer.android.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.mediaplayer.android.data.DislikedRepository
import kotlinx.coroutines.launch

/**
 * Handlers wired around a fresh [DislikedRepository] for one SongRow's
 * "Don't recommend" sheet entries. Both lambdas no-op when the targets
 * are blank/zero so callers can pass them unconditionally.
 */
class DislikeActions internal constructor(
    private val onDislikeSong: () -> Unit,
    private val onDislikeArtist: () -> Unit,
) {
    fun song(): () -> Unit = onDislikeSong
    fun artist(): () -> Unit = onDislikeArtist
}

@Composable
fun rememberDislikeActions(songId: Long, artist: String?): DislikeActions {
    val scope = rememberCoroutineScope()
    val repo = remember { DislikedRepository() }
    return remember(songId, artist) {
        DislikeActions(
            onDislikeSong = {
                if (songId > 0) scope.launch { repo.dislikeSong(songId) }
            },
            onDislikeArtist = {
                val a = artist?.trim().orEmpty()
                if (a.isNotEmpty()) scope.launch { repo.dislikeArtist(a) }
            },
        )
    }
}
