package com.mediaplayer.android.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mediaplayer.android.data.DislikedSongsCache

/**
 * Handlers for one SongRow's "Non consigliarmi" sheet entries. Both
 * lambdas no-op when the targets are blank/zero so callers can pass
 * them unconditionally.
 *
 * Routes through [DislikedSongsCache] so dislike-state stays consistent
 * across every kebab + the Disliked screen with a single shared
 * StateFlow rather than each row owning its own [DislikedRepository].
 */
class DislikeActions internal constructor(
    private val onDislikeSong: () -> Unit,
    private val onDislikeArtist: () -> Unit,
) {
    fun song(): () -> Unit = onDislikeSong
    fun artist(): () -> Unit = onDislikeArtist
}

@Composable
fun rememberDislikeActions(
    songId: Long,
    artist: String?,
    songLabel: String? = null,
): DislikeActions {
    return remember(songId, artist, songLabel) {
        DislikeActions(
            onDislikeSong = {
                if (songId > 0) DislikedSongsCache.dislikeSong(songId, displayLabel = songLabel)
            },
            onDislikeArtist = {
                val a = artist?.trim().orEmpty()
                if (a.isNotEmpty()) DislikedSongsCache.dislikeArtist(a)
            },
        )
    }
}
