package com.mediaplayer.android.ui.common

import com.mediaplayer.android.ui.theme.CoverShapes
import com.mediaplayer.android.ui.theme.HeroCoverSize
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.util.LruCache
import androidx.palette.graphics.Palette
import coil3.asDrawable
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.crossfade
import coil3.size.Size
import coil3.imageLoader

private val DefaultBackdrop = Color(0xFF3E3E3E)

/**
 * Process-wide memo of cover-art → dominant Color. Hero header is
 * re-composed on every detail-screen open; without a cache, each
 * navigation triggers a fresh Coil fetch + Palette generation on the
 * same handful of cover URLs. 64 entries covers a typical session.
 *
 * `Color.Unspecified` is stored as a sentinel for "computed but no
 * swatch found" so we don't redo the work on the next visit.
 */
private val paletteCache = LruCache<String, Color>(64)

private fun cacheKey(model: Any): String = model.toString()

/**
 * Re-usable hero header for detail screens (Liked / Playlist / Album / Artist),
 * matching Spotify: square cover art at top, gradient fade from cover-derived
 * dominant color → app background, then title + meta, then Play/Shuffle buttons.
 */
@Composable
fun SpotifyHero(
    title: String,
    subtitle: String,
    coverModel: Any?,
    fallbackGradient: Pair<Color, Color>? = null,
    coverShape: CoverShape = CoverShape.Square,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    playEnabled: Boolean = true,
    isPlaying: Boolean = false,
    eyebrow: String? = null,
    subtitleStyle: SubtitleStyle = SubtitleStyle.Default,
    extraActions: @Composable () -> Unit = {},
) {
    var dominant by remember(coverModel) {
        mutableStateOf(fallbackGradient?.first ?: DefaultBackdrop)
    }

    if (coverModel != null) {
        val ctx = LocalContext.current
        LaunchedEffect(coverModel) {
            dominant = extractDominantColor(ctx, coverModel) ?: dominant
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val artSize: Dp = (maxWidth * HeroCoverSize.DetailFraction).coerceAtMost(HeroCoverSize.DetailMax)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            0f to dominant,
                            0.65f to dominant.copy(alpha = 0.55f),
                            1f to MaterialTheme.colorScheme.background,
                        )
                    )
                    .padding(top = 24.dp, bottom = 16.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    HeroCover(
                        model = coverModel,
                        shape = coverShape,
                        size = artSize,
                        fallbackGradient = fallbackGradient,
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (eyebrow != null) {
                val mono = com.mediaplayer.android.ui.theme.LocalMHMono.current
                Text(
                    text = "// ${eyebrow.uppercase()}",
                    style = mono.eyebrow.copy(color = com.mediaplayer.android.ui.theme.MHColors.Lime),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val subStyle = when (subtitleStyle) {
                SubtitleStyle.Mono -> com.mediaplayer.android.ui.theme.LocalMHMono.current.caption
                    .copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                SubtitleStyle.Default -> MaterialTheme.typography.bodyMedium
                    .copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = subtitle,
                style = subStyle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                extraActions()
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onShuffle, enabled = playEnabled) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Casuale",
                        tint = if (playEnabled) MaterialTheme.colorScheme.onSurface
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Spotify-style scale-on-press feedback. Tracks the
                // press interaction directly rather than the toggleable
                // selection state — single tap should give a brief 0.92→1
                // springback so the button feels alive instead of static.
                val playInteraction = remember { MutableInteractionSource() }
                val pressed by playInteraction.collectIsPressedAsState()
                val playScale by animateFloatAsState(
                    targetValue = if (pressed) 0.92f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    label = "play-press-scale",
                )
                FilledIconButton(
                    onClick = onPlay,
                    enabled = playEnabled,
                    interactionSource = playInteraction,
                    modifier = Modifier.size(56.dp).scale(playScale),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pausa" else "Riproduci",
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
    }
}

enum class CoverShape { Square, Circle }
enum class SubtitleStyle { Default, Mono }

@Composable
private fun HeroCover(
    model: Any?,
    shape: CoverShape,
    size: Dp,
    fallbackGradient: Pair<Color, Color>?,
) {
    val composeShape = if (shape == CoverShape.Circle) CircleShape else CoverShapes.SongRow
    val placeholderBrush = if (fallbackGradient != null)
        Brush.linearGradient(listOf(fallbackGradient.first, fallbackGradient.second))
    else
        Brush.linearGradient(listOf(DefaultBackdrop, MaterialTheme.colorScheme.surface))

    Box(
        modifier = Modifier.size(size).clip(composeShape).background(placeholderBrush),
        contentAlignment = Alignment.Center,
    ) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            )
        }
    }
}

@Composable
fun rememberCoverDominantColor(model: Any?, fallback: Color): Color {
    var color by remember(model) { mutableStateOf(fallback) }
    if (model != null) {
        val ctx = LocalContext.current
        LaunchedEffect(model) {
            color = extractDominantColor(ctx, model) ?: fallback
        }
    }
    return color
}

internal suspend fun extractDominantColor(
    context: android.content.Context,
    model: Any,
): Color? {
    val key = cacheKey(model)
    paletteCache.get(key)?.let { cached ->
        return if (cached == Color.Unspecified) null else cached
    }
    val computed: Color? = try {
        val request = ImageRequest.Builder(context)
            .data(model)
            .size(Size(128, 128))
            .crossfade(false)
            .build()
        val result = context.imageLoader.execute(request)
        if (result !is SuccessResult) null
        else {
            val bmp = (result.image.asDrawable(context.resources) as? BitmapDrawable)?.bitmap
            if (bmp == null) null
            else {
                val palette = Palette.from(bmp).clearFilters().generate()
                val swatch = palette.darkVibrantSwatch
                    ?: palette.darkMutedSwatch
                    ?: palette.vibrantSwatch
                    ?: palette.dominantSwatch
                if (swatch == null) null else Color(swatch.rgb)
            }
        }
    } catch (_: Throwable) {
        null
    }
    paletteCache.put(key, computed ?: Color.Unspecified)
    return computed
}
