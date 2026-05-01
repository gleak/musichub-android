package com.mediaplayer.android.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediaplayer.android.ui.theme.LocalMHMono
import com.mediaplayer.android.ui.theme.MHColors
import com.mediaplayer.android.ui.theme.MonoFamily

/**
 * Auto-playlist family taxonomy. Mirror of the backend
 * `AutoPlaylistKind` enum + the mockup `coverKind` strings. Each
 * family has a dedicated cover renderer so that, glance-by-glance,
 * the user can tell which playlist they're looking at.
 */
enum class AutoPlaylistFamily(val label: String) {
    Rotation("In rotazione"),
    Daily("Mix giornaliero"),
    Releases("Nuove uscite"),
    Capsule("Time capsule"),
    Radar("Radar"),
    Mood("Mood del momento"),
    Next("In poi"),
}

/** Map a backend `kind` string (e.g. `ON_REPEAT`) to a family. */
fun familyOf(kind: String?): AutoPlaylistFamily = when (kind?.uppercase()) {
    "ON_REPEAT", "ROTATION" -> AutoPlaylistFamily.Rotation
    "DAILY_MIX_1", "DAILY_MIX_2", "DAILY_MIX_3",
    "DAILY_MIX_4", "DAILY_MIX_5", "DAILY_MIX_6",
    "DISCOVER_DAILY" -> AutoPlaylistFamily.Daily
    "RELEASE_RADAR", "RELEASES" -> AutoPlaylistFamily.Releases
    "TIME_CAPSULE", "CAPSULE" -> AutoPlaylistFamily.Capsule
    "RADAR" -> AutoPlaylistFamily.Radar
    "MOOD" -> AutoPlaylistFamily.Mood
    "NEXT", "CONTINUE" -> AutoPlaylistFamily.Next
    else -> AutoPlaylistFamily.Daily
}

/** Pick the badge label that appears on the cover. */
fun badgeFor(kind: String?): String = when (kind?.uppercase()) {
    "DAILY_MIX_1" -> "01"
    "DAILY_MIX_2" -> "02"
    "DAILY_MIX_3" -> "03"
    "DAILY_MIX_4" -> "04"
    "DAILY_MIX_5" -> "05"
    "DAILY_MIX_6" -> "06"
    "RELEASE_RADAR", "RELEASES" -> "VEN"
    "ON_REPEAT", "ROTATION" -> "S" + ((java.time.LocalDate.now().dayOfYear / 7) + 1).toString()
    "TIME_CAPSULE", "CAPSULE" -> java.time.LocalDate.now().minusYears(3).year.toString()
    "RADAR" -> "LUN"
    "MOOD" -> "NOW"
    "NEXT", "CONTINUE" -> "→"
    else -> ""
}

@Composable
fun GeneratedCover(
    family: AutoPlaylistFamily,
    badge: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp,
) {
    val (paletteA, paletteB) = paletteFor(family)
    val mono = LocalMHMono.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(MHColors.Card),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            when (family) {
                AutoPlaylistFamily.Rotation -> drawRotation(paletteA, paletteB)
                AutoPlaylistFamily.Daily -> drawDaily(paletteA, paletteB)
                AutoPlaylistFamily.Releases -> drawReleases(paletteA, paletteB)
                AutoPlaylistFamily.Capsule -> drawCapsule(paletteA, paletteB)
                AutoPlaylistFamily.Radar -> drawRadar(paletteA, paletteB)
                AutoPlaylistFamily.Mood -> drawMood(paletteA, paletteB)
                AutoPlaylistFamily.Next -> drawNext(paletteA, paletteB)
            }
        }
        // Eyebrow label top-left
        Text(
            text = "// ${familyEyebrow(family)}",
            style = mono.eyebrow.copy(color = Color.White.copy(alpha = 0.85f)),
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        )
        // Center mega-badge for daily mix + radar + capsule
        if (family == AutoPlaylistFamily.Daily ||
            family == AutoPlaylistFamily.Capsule ||
            family == AutoPlaylistFamily.Releases
        ) {
            Text(
                text = badge,
                style = TextStyle(
                    fontFamily = MonoFamily,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-2).sp,
                ),
                modifier = Modifier.align(Alignment.Center),
            )
        }
        // Subtitle bottom-left
        if (subtitle != null) {
            Text(
                text = subtitle,
                color = Color.White,
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                maxLines = 2,
            )
        }
    }
}

private fun familyEyebrow(f: AutoPlaylistFamily): String = when (f) {
    AutoPlaylistFamily.Rotation -> "ROTATION"
    AutoPlaylistFamily.Daily -> "MIX"
    AutoPlaylistFamily.Releases -> "DROP"
    AutoPlaylistFamily.Capsule -> "CAPSULE"
    AutoPlaylistFamily.Radar -> "RADAR"
    AutoPlaylistFamily.Mood -> "MOOD"
    AutoPlaylistFamily.Next -> "NEXT"
}

private fun paletteFor(f: AutoPlaylistFamily): Pair<Color, Color> = when (f) {
    AutoPlaylistFamily.Rotation -> Color(0xFF1A1A1A) to MHColors.Lime
    AutoPlaylistFamily.Daily -> Color(0xFF1E3A8A) to Color(0xFF06B6D4)
    AutoPlaylistFamily.Releases -> MHColors.Lime to Color(0xFF0A0A0A)
    AutoPlaylistFamily.Capsule -> Color(0xFFF0A6B0) to Color(0xFF3A1F8A)
    AutoPlaylistFamily.Radar -> Color(0xFF06B6D4) to Color(0xFF1E3A8A)
    AutoPlaylistFamily.Mood -> Color(0xFF3A0CA3) to Color(0xFFF72585)
    AutoPlaylistFamily.Next -> Color(0xFFFFC857) to Color(0xFF3A1F8A)
}

private fun DrawScope.drawRotation(a: Color, b: Color) {
    drawRect(a)
    listOf(0.85f, 0.6f, 0.35f).forEach { r ->
        drawCircle(
            color = b,
            radius = size.minDimension * 0.45f * r,
            center = Offset(size.width / 2, size.height / 2),
            style = Stroke(width = 2f),
        )
    }
}

private fun DrawScope.drawDaily(a: Color, b: Color) {
    drawRect(Brush.linearGradient(0f to a, 1f to b))
}

private fun DrawScope.drawReleases(a: Color, b: Color) {
    drawRect(a)
    drawRect(
        color = b.copy(alpha = 0.4f),
        topLeft = Offset(0f, size.height * 0.7f),
        size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.3f),
    )
}

private fun DrawScope.drawCapsule(a: Color, b: Color) {
    drawRect(Brush.verticalGradient(0f to a, 1f to b))
    // polaroid frame in center
    val frameW = size.width * 0.7f
    val frameH = size.height * 0.65f
    val left = (size.width - frameW) / 2
    val top = (size.height - frameH) / 2
    drawRect(
        color = Color.White,
        topLeft = Offset(left, top),
        size = androidx.compose.ui.geometry.Size(frameW, frameH),
    )
    drawRect(
        color = a,
        topLeft = Offset(left + 6f, top + 6f),
        size = androidx.compose.ui.geometry.Size(frameW - 12f, frameH - 30f),
    )
}

private fun DrawScope.drawRadar(a: Color, b: Color) {
    drawRect(Brush.radialGradient(
        0f to a, 1f to b,
        center = Offset(size.width / 2, size.height / 2),
        radius = size.minDimension * 0.6f,
    ))
    // dot grid
    val cols = 5
    val rows = 5
    val cellW = size.width / (cols + 1f)
    val cellH = size.height / (rows + 1f)
    for (r in 1..rows) for (c in 1..cols) {
        drawCircle(
            color = Color.White.copy(alpha = 0.4f),
            radius = 1.5f,
            center = Offset(c * cellW, r * cellH),
        )
    }
}

private fun DrawScope.drawMood(a: Color, b: Color) {
    drawRect(Brush.linearGradient(0f to a, 1f to b))
}

private fun DrawScope.drawNext(a: Color, b: Color) {
    drawRect(Brush.linearGradient(0f to a, 1f to b))
}
