package com.mediaplayer.android.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Brush
import com.mediaplayer.android.ui.theme.CoverShapes
import com.mediaplayer.android.ui.theme.MediaPlayerSpacing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.playback.PlaybackViewModel
import com.mediaplayer.android.ui.common.SongCover

/**
 * Compact, always-visible bar pinned above the main content whenever a
 * song is loaded. Click the bar → expand the Now Playing sheet.
 */
@UnstableApi
@Composable
fun MiniPlayer(
    viewModel: PlaybackViewModel,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Optional modifier applied to the cover Box only. AppScaffold injects a
     * `Modifier.sharedBounds(...)` here so the cover participates in the
     * MiniPlayer ↔ NowPlayingSheet shared-element transition. Empty by
     * default keeps standalone use (preview, tests) trivial.
     */
    coverModifier: Modifier = Modifier,
) {
    val song by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val position by viewModel.positionMs.collectAsStateWithLifecycle()
    val duration by viewModel.durationMs.collectAsStateWithLifecycle()
    val liked by viewModel.currentLiked.collectAsStateWithLifecycle()

    val current = song ?: return  // mini-player hidden until a track loads
    val haptics = LocalHapticFeedback.current

    val dismissState = rememberSwipeToDismissBoxState(
        // Re-key per track so dismissing one song doesn't pre-arm the bar
        // for the next track that gets queued up.
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd ||
                value == SwipeToDismissBoxValue.EndToStart
            ) {
                viewModel.dismissPlayback()
                true
            } else false
        },
    )
    LaunchedEffect(current.id) { dismissState.reset() }

    val cardShape = CoverShapes.Card
    val accent = MaterialTheme.colorScheme.primary
    val swipeProgress = dismissState.progress
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            // Mockup `mh-player-sheets.jsx:269-315` shows a fade trail with a
            // "Rilascia per fermare" hint behind the dragged card. We approximate
            // by tinting the trail with the accent + showing the hint once the
            // user has dragged past ~25% — kept subtle so it doesn't compete
            // with the foreground card.
            val target = dismissState.targetValue
            val active = target == SwipeToDismissBoxValue.StartToEnd ||
                target == SwipeToDismissBoxValue.EndToStart
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = MediaPlayerSpacing.S, vertical = MediaPlayerSpacing.Xs)
                    .clip(cardShape)
                    .background(
                        Brush.horizontalGradient(
                            0f to accent.copy(alpha = if (active) 0.10f else 0.04f),
                            1f to accent.copy(alpha = 0.02f),
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (active && swipeProgress > 0.25f) {
                    Text(
                        text = "Rilascia per fermare",
                        style = MaterialTheme.typography.labelMedium,
                        color = accent,
                    )
                }
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MediaPlayerSpacing.S, vertical = MediaPlayerSpacing.Xs),
    ) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            // Brand gradient outline (mockup uses a 1px lime→transparent
            // frame to lift the mini-player off the screen background).
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    0f to accent.copy(alpha = 0.45f),
                    1f to accent.copy(alpha = 0.05f),
                ),
                shape = cardShape,
            )
            .clickable { onExpand() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MediaPlayerSpacing.S, vertical = MediaPlayerSpacing.S),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SongCover(
                song = current,
                size = 44.dp,
                shape = CoverShapes.MiniPlayer,
                modifier = coverModifier,
            )
            Spacer(Modifier.width(MediaPlayerSpacing.Xs + MediaPlayerSpacing.S))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = current.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // Artist · Album line per mockup. Album is dropped silently
                // when the song has none (unknown / missing metadata).
                val subtitle = current.album?.takeIf { it.isNotBlank() }
                    ?.let { "${current.artist} · $it" }
                    ?: current.artist
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.toggleCurrentLike()
            }) {
                Icon(
                    imageVector = if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (liked) "Rimuovi mi piace" else "Mi piace",
                    tint = if (liked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledIconButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.togglePlayPause()
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = accent,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pausa" else "Riproduci",
                )
            }
        }
        LinearProgressIndicator(
            progress = { if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
            color = MaterialTheme.colorScheme.onSurface,
            // surfaceContainerHighest (0xFF3E3E3E) reads as nearly the same
            // grey as the mini-player's surfaceContainerHigh background
            // (0xFF282828) — the inactive track all but vanished. A flat
            // white-with-alpha gives consistent contrast against any
            // background colour the mini-player ever sits on.
            trackColor = Color.White.copy(alpha = 0.18f),
        )
    }
    }
}

/**
 * Kept as a thin alias so NowPlayingSheet (which imports `Cover` from this
 * package) doesn't need to know about the new shared composable. New code
 * should call [SongCover] directly.
 */
@Composable
internal fun Cover(song: SongDto, size: androidx.compose.ui.unit.Dp) {
    SongCover(song = song, size = size, shape = CoverShapes.MiniPlayer)
}
