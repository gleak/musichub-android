package com.mediaplayer.android.ui.local

import android.media.MediaMetadataRetriever
import android.util.LruCache
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

    val embedded by produceState<ByteArray?>(
        initialValue = embeddedCache.get(track.id)?.takeIf { it !== NO_EMBEDDED_ART },
        track.id,
    ) {
        if (track.albumArtUri != null) return@produceState
        val cached = embeddedCache.get(track.id)
        if (cached != null) {
            value = if (cached === NO_EMBEDDED_ART) null else cached
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
        embeddedCache.put(track.id, bytes ?: NO_EMBEDDED_ART)
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

// Sentinel for "tried, no embedded art" so we can cache the negative result
// without leaking memory by holding genuinely-null values in an LruCache.
private val NO_EMBEDDED_ART = ByteArray(0)

// Bounded by total bytes (~16 MB) rather than entry count: embedded pictures
// vary wildly in size (tiny placeholders vs full-quality JPEG), so per-entry
// limits would either evict too aggressively or pin hundreds of MB on a large
// local library. Thread-safe; access from compose threads + IO dispatcher.
private val embeddedCache: LruCache<Long, ByteArray> =
    object : LruCache<Long, ByteArray>(16 * 1024 * 1024) {
        override fun sizeOf(key: Long, value: ByteArray): Int =
            value.size.coerceAtLeast(1)
    }
