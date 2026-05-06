package com.mediaplayer.android.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mediaplayer.android.ui.theme.MHColors

/**
 * 3 vertical bars pulsing in lime — the MusicHub "now playing" indicator
 * per `mh-shared.jsx:282-298`. Suitable next to a track title or in a
 * tab badge to signal active playback. Three independent staggered
 * animations so the bars never sync up.
 */
@Composable
fun MHPlayingBars(
    color: Color = MHColors.Lime,
    height: Dp = 12.dp,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "mh-playing-bars")
    val a by transition.animateFloat(
        initialValue = 0.33f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bar-a",
    )
    val b by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.33f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(120),
        ),
        label = "bar-b",
    )
    val c by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(240),
        ),
        label = "bar-c",
    )

    Box(modifier = modifier.size(width = 12.dp, height = height)) {
        Canvas(Modifier.size(width = 12.dp, height = height)) {
            val barW = 2.dp.toPx()
            val gap = 2.dp.toPx()
            val totalH = size.height
            listOf(a, b, c).forEachIndexed { i, frac ->
                val barH = totalH * frac
                drawRoundRect(
                    color = color,
                    topLeft = Offset(i * (barW + gap), totalH - barH),
                    size = Size(barW, barH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx()),
                )
            }
        }
    }
}
