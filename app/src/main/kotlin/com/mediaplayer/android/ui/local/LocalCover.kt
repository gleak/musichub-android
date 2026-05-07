package com.mediaplayer.android.ui.local

import android.media.MediaMetadataRetriever
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
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
import com.mediaplayer.android.data.local.LocalTrack
import com.mediaplayer.android.ui.theme.CoverShapes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Local-track cover with an embedded-ID3 fallback. The standard
 * `albumart` provider URI returns null on plenty of devices (notably Xiaomi
 * MIUI, anything where the user dropped audio in via SAF without rebuilding
 * the album thumbnail cache). This composable falls back to the bytes
 * embedded inside the audio container itself, decoded via
 * MediaMetadataRetriever on the IO dispatcher.
 *
 * Results are memoised in a process-wide cache so scrolling a long list
 * doesn't reopen every file. A null cache entry means "tried, no embedded
 * art" — those rows render the MusicNote glyph without retrying.
 */
@Composable
fun LocalCover(
    track: LocalTrack,
    size: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = CoverShapes.SongRow,
    contentDescription: String? = null,
) {
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { size.roundToPx() }

    val embedded by produceState<ByteArray?>(initialValue = embeddedCache[track.id], track.id) {
        if (track.albumArtUri != null) return@produceState
        if (embeddedCache.containsKey(track.id)) {
            value = embeddedCache[track.id]
            return@produceState
        }
        val bytes = withContext(Dispatchers.IO) {
            val mmr = MediaMetadataRetriever()
            try {
                mmr.setDataSource(context, track.uri)
                mmr.embeddedPicture
            } catch (_: Throwable) {
                null
            } finally {
                runCatching { mmr.release() }
            }
        }
        embeddedCache[track.id] = bytes
        value = bytes
    }

    val data = track.albumArtUri ?: embedded
    val request = remember(data, sizePx) {
        ImageRequest.Builder(context)
            .data(data)
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
        if (data != null) {
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

private val embeddedCache: ConcurrentHashMap<Long, ByteArray?> = ConcurrentHashMap()
