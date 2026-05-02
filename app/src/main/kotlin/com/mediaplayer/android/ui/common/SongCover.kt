package com.mediaplayer.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.ui.theme.CoverShapes

/**
 * Shared song-cover composable. Replaces the per-screen `CoverArt` /
 * `Cover` / inline `Box(...) { AsyncImage(...) }` / `Icon(MusicNote)`
 * patterns that drifted across SongRow, MiniPlayer, HomeScreen,
 * SearchScreen and AddSongsToPlaylistSheet.
 *
 * Behaviour:
 *  - When [songId] resolves to a song with cover art, fetches via Coil
 *    against [Network.coverUrl].
 *  - Otherwise paints the surfaceVariant box with a `MusicNote` glyph.
 *  - [contentDescription] is null by default — pass a string only for
 *    cover-as-identity surfaces (carousel tiles where the title text
 *    can ellipsis); leave null for row-with-text covers since the
 *    surrounding title/artist Text is what TalkBack reads.
 */
@Composable
fun SongCover(
    song: SongDto,
    size: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = CoverShapes.SongRow,
    contentDescription: String? = null,
) {
    SongCover(
        songId = song.id,
        hasCoverArt = song.hasCoverArt,
        size = size,
        modifier = modifier,
        shape = shape,
        contentDescription = contentDescription,
    )
}

@Composable
fun SongCover(
    songId: Long,
    hasCoverArt: Boolean,
    size: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = CoverShapes.SongRow,
    contentDescription: String? = null,
) {
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val request = remember(songId, sizePx) {
        ImageRequest.Builder(context)
            .data(Network.coverUrl(songId))
            .size(Size(sizePx, sizePx))
            .crossfade(true)
            .build()
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (hasCoverArt) {
            AsyncImage(
                model = request,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
