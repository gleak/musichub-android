package com.mediaplayer.android.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Google's official multi-color "G" logo, used on the LoginScreen
 * sign-in button. Geometry mirrors the SVG in `mh-auth.jsx:67-72`
 * (48×48 viewBox) — quadrant arcs in red/yellow/green/blue with the
 * inner cut-out forming the "G" notch. Sized in dp via [size]; the
 * canvas scales the 48-unit viewBox uniformly.
 */
@Composable
fun GoogleGIcon(size: Dp = 20.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val s = this.size.minDimension / 48f
        val cx = 24f * s
        val cy = 24f * s
        val outerR = 19f * s
        val innerR = 11f * s

        val red = Color(0xFFEB4335)
        val yellow = Color(0xFFFBBC05)
        val green = Color(0xFF34A853)
        val blue = Color(0xFF4285F4)

        // Donut cut: outer disc minus inner disc per quadrant, with the
        // bottom-right "G" mouth carved by clipping the blue arc.
        fun quadrant(color: Color, startAngle: Float, sweep: Float) {
            val outer = Path().apply {
                arcTo(
                    rect = Rect(
                        offset = Offset(cx - outerR, cy - outerR),
                        size = Size(outerR * 2, outerR * 2),
                    ),
                    startAngleDegrees = startAngle,
                    sweepAngleDegrees = sweep,
                    forceMoveTo = true,
                )
                lineTo(cx, cy)
                close()
            }
            val inner = Path().apply {
                addOval(
                    Rect(
                        offset = Offset(cx - innerR, cy - innerR),
                        size = Size(innerR * 2, innerR * 2),
                    ),
                )
            }
            val clipped = Path.combine(PathOperation.Difference, outer, inner)
            drawPath(clipped, color)
        }

        // Yellow: bottom-left quadrant (90°→180°).
        quadrant(yellow, 135f, 90f)
        // Red: top-left (180°→270°).
        quadrant(red, 225f, 90f)
        // Green: bottom-right (0°→90°).
        quadrant(green, 45f, 90f)
        // Blue: top-right (270°→360°), with the "G" notch carved out.
        val bluePath = Path().apply {
            arcTo(
                rect = Rect(
                    offset = Offset(cx - outerR, cy - outerR),
                    size = Size(outerR * 2, outerR * 2),
                ),
                startAngleDegrees = -45f,
                sweepAngleDegrees = 90f,
                forceMoveTo = true,
            )
            lineTo(cx, cy)
            close()
        }
        val blueInner = Path().apply {
            addOval(
                Rect(
                    offset = Offset(cx - innerR, cy - innerR),
                    size = Size(innerR * 2, innerR * 2),
                ),
            )
        }
        // Carve the horizontal "mouth" — a rectangle covering the right
        // half of the inner area, so the blue arc terminates at a flat
        // edge (the bar of the G).
        val notch = Path().apply {
            addRect(
                Rect(
                    offset = Offset(cx, cy - 4f * s),
                    size = Size(outerR, 8f * s),
                ),
            )
        }
        val blueWithoutInner = Path.combine(PathOperation.Difference, bluePath, blueInner)
        val blueFinal = Path.combine(PathOperation.Difference, blueWithoutInner, notch)
        drawPath(blueFinal, blue)
        // The G's horizontal bar (a thin blue rectangle filling the notch
        // cutout, but only the inside half — the inner-disc edge already
        // hides the rest).
        drawRect(
            color = blue,
            topLeft = Offset(cx, cy - 2f * s),
            size = Size(outerR * 0.7f, 4f * s),
        )
    }
}
