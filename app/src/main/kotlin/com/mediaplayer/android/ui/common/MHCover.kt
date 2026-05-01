package com.mediaplayer.android.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediaplayer.android.ui.theme.InterFamily
import com.mediaplayer.android.ui.theme.MHColors
import kotlin.math.abs

/**
 * Generative cover renderer ported from the MusicHub mockup
 * (`mh-shared.jsx` `COVER_KINDS`). 11 styles, deterministic palette
 * per id so the same album always paints the same cover even when no
 * artwork is available.
 */
@Immutable
enum class MHCoverKind { Arc, Grid, Moon, Triangles, Wave, Dot, Stripes, Blob, Type, Duotone, Artist }

@Immutable
data class MHCoverPalette(
    val a: Color,
    val b: Color,
    val label: String? = null,
)

/** Pick a deterministic kind/palette from a stable id. */
fun mhCoverFor(id: Long): Pair<MHCoverKind, MHCoverPalette> {
    val kinds = MHCoverKind.values()
    val kind = kinds[(abs(id) % kinds.size).toInt()]
    val palette = MH_PALETTES[(abs(id * 31) % MH_PALETTES.size).toInt()]
    return kind to palette
}

private val MH_PALETTES = listOf(
    MHCoverPalette(Color(0xFFFF6B5B), Color(0xFF3A1F8A)),
    MHCoverPalette(Color(0xFF0E1F3A), MHColors.Lime),
    MHCoverPalette(Color(0xFFE8DCC4), Color(0xFF1A1A1A)),
    MHCoverPalette(Color(0xFF1A1A1A), Color(0xFFFF4D2E)),
    MHCoverPalette(Color(0xFF5C2D8C), Color(0xFFF0A6B0)),
    MHCoverPalette(Color(0xFF0B3D2E), MHColors.Lime),
    MHCoverPalette(Color(0xFFFFC857), Color(0xFF1A1A1A)),
    MHCoverPalette(Color(0xFF1E3A8A), Color(0xFF06B6D4)),
    MHCoverPalette(Color(0xFFE8E2D5), Color(0xFF222222), label = "echo"),
    MHCoverPalette(Color(0xFF3A0CA3), Color(0xFFF72585)),
    MHCoverPalette(Color(0xFF2A1E12), Color(0xFFE8DCC4)),
)

@Composable
fun MHCover(
    kind: MHCoverKind,
    palette: MHCoverPalette,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(MHColors.Card),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            when (kind) {
                MHCoverKind.Arc -> drawArc(palette)
                MHCoverKind.Grid -> drawGrid(palette)
                MHCoverKind.Moon -> drawMoon(palette)
                MHCoverKind.Triangles -> drawTriangles(palette)
                MHCoverKind.Wave -> drawWave(palette)
                MHCoverKind.Dot -> drawDot(palette)
                MHCoverKind.Stripes -> drawStripes(palette)
                MHCoverKind.Blob -> drawBlob(palette)
                MHCoverKind.Type -> drawType(palette)
                MHCoverKind.Duotone -> drawDuotone(palette)
                MHCoverKind.Artist -> drawArtist(palette)
            }
        }
        if (kind == MHCoverKind.Type && palette.label != null) {
            Text(
                text = palette.label,
                style = TextStyle(
                    fontFamily = InterFamily,
                    fontSize = 28.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.b,
                ),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp),
            )
        }
    }
}

private fun DrawScope.drawArc(p: MHCoverPalette) {
    drawRect(Brush.linearGradient(0f to p.a, 1f to p.b))
    val rings = listOf(0.55f, 0.42f, 0.28f)
    rings.forEachIndexed { i, r ->
        drawCircle(
            color = Color.White.copy(alpha = 0.22f - i * 0.06f),
            radius = size.minDimension * r,
            center = Offset(size.width * 0.2f, size.height * 0.8f),
            style = Stroke(width = 1f),
        )
    }
}

private fun DrawScope.drawGrid(p: MHCoverPalette) {
    drawRect(p.a)
    val on = setOf(0, 2, 4, 5, 6, 8)
    val cellSize = size.minDimension / 3.3f
    val pad = cellSize * 0.18f
    for (i in 0 until 9) {
        val cx = (i % 3) * (cellSize + pad) + pad
        val cy = (i / 3) * (cellSize + pad) + pad
        drawRect(
            color = if (i in on) p.b else Color.White.copy(alpha = 0.08f),
            topLeft = Offset(cx, cy),
            size = Size(cellSize * 0.7f, cellSize * 0.7f),
        )
    }
}

private fun DrawScope.drawMoon(p: MHCoverPalette) {
    drawRect(p.a)
    drawCircle(p.b, radius = size.minDimension * 0.32f, center = Offset(size.width * 0.5f, size.height * 0.6f))
    drawCircle(p.a, radius = size.minDimension * 0.28f, center = Offset(size.width * 0.62f, size.height * 0.52f))
}

private fun DrawScope.drawTriangles(p: MHCoverPalette) {
    drawRect(p.a)
    val path = Path().apply {
        moveTo(size.width * 0.1f, size.height * 0.8f)
        lineTo(size.width * 0.5f, size.height * 0.2f)
        lineTo(size.width * 0.9f, size.height * 0.8f)
        close()
    }
    drawPath(path, p.b)
    val shadow = Path().apply {
        moveTo(size.width * 0.5f, size.height * 0.2f)
        lineTo(size.width * 0.9f, size.height * 0.8f)
        lineTo(size.width * 0.5f, size.height * 0.8f)
        close()
    }
    drawPath(shadow, Color.Black.copy(alpha = 0.25f))
}

private fun DrawScope.drawWave(p: MHCoverPalette) {
    drawRect(Brush.verticalGradient(0f to p.a, 1f to p.b))
    listOf(0.30f, 0.45f, 0.60f, 0.75f).forEach { y ->
        val path = Path().apply {
            moveTo(0f, size.height * y)
            quadraticBezierTo(
                size.width * 0.25f, size.height * y - size.minDimension * 0.08f,
                size.width * 0.5f, size.height * y,
            )
            quadraticBezierTo(
                size.width * 0.75f, size.height * y + size.minDimension * 0.08f,
                size.width, size.height * y,
            )
        }
        drawPath(path, Color.White.copy(alpha = 0.5f), style = Stroke(width = 0.8f))
    }
}

private fun DrawScope.drawDot(p: MHCoverPalette) {
    drawRect(p.a)
    drawCircle(p.b, radius = size.minDimension * 0.18f, center = Offset(size.width * 0.68f, size.height * 0.32f))
}

private fun DrawScope.drawStripes(p: MHCoverPalette) {
    drawRect(p.a)
    val barH = size.height * 0.06f
    listOf(0f, 0.2f, 0.4f, 0.6f, 0.8f).forEach { y ->
        drawRect(
            color = p.b.copy(alpha = 0.85f),
            topLeft = Offset(0f, size.height * y),
            size = Size(size.width, barH),
        )
    }
    drawCircle(p.b, radius = size.minDimension * 0.14f, center = Offset(size.width * 0.78f, size.height * 0.22f))
}

private fun DrawScope.drawBlob(p: MHCoverPalette) {
    drawRect(p.a)
    drawOval(
        color = p.b.copy(alpha = 0.85f),
        topLeft = Offset(size.width * 0.12f, size.height * 0.27f),
        size = Size(size.width * 0.76f, size.height * 0.56f),
    )
    drawOval(
        color = Color.White.copy(alpha = 0.25f),
        topLeft = Offset(size.width * 0.2f, size.height * 0.34f),
        size = Size(size.width * 0.4f, size.height * 0.28f),
    )
}

private fun DrawScope.drawType(p: MHCoverPalette) {
    drawRect(p.a)
    drawLine(
        color = p.b,
        start = Offset(size.width * 0.06f, size.height * 0.68f),
        end = Offset(size.width * 0.94f, size.height * 0.68f),
        strokeWidth = 0.6f,
    )
}

private fun DrawScope.drawDuotone(p: MHCoverPalette) {
    drawRect(p.a, topLeft = Offset.Zero, size = Size(size.width, size.height * 0.5f))
    drawRect(p.b, topLeft = Offset(0f, size.height * 0.5f), size = Size(size.width, size.height * 0.5f))
    drawCircle(p.a, radius = size.minDimension * 0.22f, center = Offset(size.width * 0.5f, size.height * 0.5f))
}

private fun DrawScope.drawArtist(p: MHCoverPalette) {
    drawRect(p.a)
    drawCircle(p.b, radius = size.minDimension * 0.20f, center = Offset(size.width * 0.5f, size.height * 0.42f))
    drawOval(
        color = p.b,
        topLeft = Offset(size.width * 0.15f, size.height * 0.7f),
        size = Size(size.width * 0.7f, size.height * 0.44f),
    )
}
