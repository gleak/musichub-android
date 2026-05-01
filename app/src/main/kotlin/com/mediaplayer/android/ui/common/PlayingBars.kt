package com.mediaplayer.android.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mediaplayer.android.ui.theme.MHColors

/**
 * 3-bar animated indicator drawn on a 14×14 canvas. Bars pulse out
 * of phase to evoke an EQ meter — used inline next to the currently
 * playing song title in lists.
 */
@Composable
fun PlayingBars(
    color: Color = MHColors.Lime,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "playing-bars")
    val a by transition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = LinearEasing), RepeatMode.Reverse,
        ), label = "a",
    )
    val b by transition.animateFloat(
        initialValue = 1f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            tween(800, easing = LinearEasing), RepeatMode.Reverse,
        ), label = "b",
    )
    val c by transition.animateFloat(
        initialValue = 0.5f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            tween(1100, easing = LinearEasing), RepeatMode.Reverse,
        ), label = "c",
    )

    Box(modifier = modifier.size(width = 14.dp, height = 12.dp)) {
        Canvas(Modifier.size(width = 14.dp, height = 12.dp)) {
            val barW = size.width / 5f
            val gap = barW / 2
            listOf(a, b, c).forEachIndexed { i, factor ->
                val h = size.height * factor
                drawRect(
                    color = color,
                    topLeft = Offset(i * (barW + gap), size.height - h),
                    size = Size(barW, h),
                )
            }
        }
    }
}
