package com.mediaplayer.android.ui.common

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mediaplayer.android.ui.theme.CoverShapes

/** Centered spinner used as the standard loading affordance for full-screen states. */
@Composable
fun CenteredSpinner(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.size(32.dp))
    }
}

/** Centered single-line muted message — used for non-error informational empty states. */
@Composable
fun CenteredMessage(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Standard error state with a retry CTA. Use this whenever a screen surfaces a
 * {@code Throwable} from a load — the message comes from {@link friendlyMessage}.
 */
@Composable
fun ErrorWithRetry(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalButton(onClick = onRetry) { Text("Retry") }
        }
    }
}

/**
 * Spotify-style empty state with icon, title, optional subtitle and optional CTA.
 * Replace hand-written empty placeholders that vary in copy and tone.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(36.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(4.dp))
                FilledTonalButton(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

/** Shimmer brush used by the *Shimmer composables — animates a subtle highlight band. */
@Composable
private fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
        ),
        label = "shimmer-translate",
    )
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = MaterialTheme.colorScheme.surfaceContainerHighest
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = androidx.compose.ui.geometry.Offset(translate - 300f, 0f),
        end = androidx.compose.ui.geometry.Offset(translate, 300f),
    )
}

/** Skeleton row matching {@code SongRow}'s layout — used while song lists are loading. */
@Composable
fun SongRowShimmer(modifier: Modifier = Modifier) {
    val brush = shimmerBrush()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CoverShapes.SongRow)
                .background(brush),
        )
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(14.dp)
                    .clip(CoverShapes.Skeleton)
                    .background(brush),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(12.dp)
                    .clip(CoverShapes.Skeleton)
                    .background(brush),
            )
        }
    }
}

/** Vertical column of {@link SongRowShimmer}s — drop-in replacement for a centered spinner. */
@Composable
fun SongListShimmer(rowCount: Int = 8, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(rowCount) { SongRowShimmer() }
    }
}

/** Maps a [Throwable] to user-facing copy. Hides serializer / IO stack traces. */
fun friendlyMessage(t: Throwable?): String = when {
    t == null -> "Something went wrong"
    t is java.io.IOException -> "Couldn't reach the server. Check your connection."
    t.message?.contains("401", ignoreCase = false) == true -> "Sign-in expired. Please sign in again."
    t.message?.contains("403", ignoreCase = false) == true -> "You don't have access to this."
    t.message?.contains("404", ignoreCase = false) == true -> "Not found."
    !t.message.isNullOrBlank() && t.message!!.length < 80 -> t.message!!
    else -> "Something went wrong"
}

/** Tiny dot row used as a "more loading" visual accent. */
@Composable
fun PulsingDot(color: Color = MaterialTheme.colorScheme.primary, size: Int = 6) {
    val transition = rememberInfiniteTransition(label = "dot")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(700, easing = LinearEasing)),
        label = "dot-alpha",
    )
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha)),
    )
}
