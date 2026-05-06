package com.mediaplayer.android.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediaplayer.android.ui.theme.MHColors

/**
 * MusicHub brand monogram tile — lime square with the equalizer-bars
 * SVG mark per `mh-auth.jsx:14-31` and `mh-shared.jsx`. Used on
 * LoginScreen and the OnboardingSheet hero. The 8 vertical bars
 * differ in height to suggest a frozen audio waveform.
 */
@Composable
fun MHMonogramTile(
    size: Dp = 92.dp,
    cornerRadius: Dp = 22.dp,
    shadowElevation: Dp = 24.dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .shadow(
                elevation = shadowElevation,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = MHColors.Lime.copy(alpha = 0.45f),
                spotColor = MHColors.Lime.copy(alpha = 0.45f),
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.verticalGradient(
                    0f to MHColors.Lime,
                    1f to MHColors.LimeDim,
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size * 0.61f)) {
            val barColor = Color(0xFF0A0A0A)
            val canvasSize = this.size
            val s = canvasSize.minDimension / 108f
            // 8 equalizer bars centered around y=54 in a 108×108 viewBox.
            // (x, height, width) tuples per mh-auth.jsx:23-28.
            val bars = listOf(
                Triple(24f, 14f, 5f),
                Triple(32f, 30f, 5f),
                Triple(40f, 22f, 5f),
                Triple(48f, 54f, 6f),
                Triple(57f, 40f, 5f),
                Triple(65f, 62f, 6f),
                Triple(74f, 28f, 5f),
                Triple(82f, 18f, 5f),
            )
            bars.forEach { (x, h, w) ->
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(((x - w / 2f) * s), ((54f - h / 2f) * s)),
                    size = Size(w * s, h * s),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * s / 2f),
                )
            }
        }
    }
}

/**
 * Brand mark in horizontal layout: monogram tile + "MusicHub" wordmark.
 * Used in headers / top bars where the rebrand calls for a fixed brand
 * lockup. Per `mh-shared.jsx:301-317` the default tile is 28dp.
 */
@Composable
fun MHLogo(
    tileSize: Dp = 28.dp,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MHMonogramTile(size = tileSize, cornerRadius = (tileSize.value * 0.25f).dp, shadowElevation = 0.dp)
        Spacer(Modifier.width(0.dp))
        Text(
            text = "MusicHub",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.6).sp,
                fontSize = 18.sp,
            ),
            color = MHColors.TextHi,
        )
    }
}
