package com.mediaplayer.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mediaplayer.android.data.PlayerSettings

/**
 * MusicHub brand palette. Locked dark — gradient/cover-driven hero
 * artwork relies on a black backdrop. Lime accent (#A8E04E) replaces
 * the previous Spotify green per the v0.10 design rebrand.
 */
object MHColors {
    val Lime = Color(0xFFA8E04E)
    val LimePressed = Color(0xFFB8EE5E)
    val LimeDim = Color(0xFF7FB535)

    val Black = Color(0xFF000000)
    val BgTop = Color(0xFF1F1F1F)        // gradient top — screen surface
    val BgBottom = Color(0xFF080808)     // gradient bottom — fades to near-black
    val Surface = Color(0xFF121212)      // legacy alias — kept for callers that still want a flat dark
    val Card = Color(0xFF181818)         // tile / card body
    val CardHover = Color(0xFF222222)    // pressed / hover state
    val CardHigh = Color(0xFF282828)     // raised tiles, mini player
    val CardSelected = Color(0xFF3E3E3E) // selected chip / sheet handle

    val TextHi = Color(0xFFFFFFFF)
    val TextLo = Color(0xFF9A9A9A)       // secondary copy, mono labels
    val TextLo2 = Color(0xFF6A6A6A)      // tertiary / placeholders
    val Divider = Color(0x14FFFFFF)      // 8% white

    // Decorative gradients (auto-playlist covers, Liked songs, etc.).
    // Picked from the MusicHub generative palette.
    val LikedGradientStart = Color(0xFF3A0CA3)  // duotone purple
    val LikedGradientEnd = Color(0xFFF72585)    // duotone magenta

    // Browse-all genre tiles (Search screen). Picked to match the
    // 2×2 colored tiles in the mockup.
    val BrowseAlbumsTile = Color(0xFF3A0CA3)    // indie purple
    val BrowseArtistsTile = Color(0xFFFF4D2E)   // hip-hop orange-red
}

private val MHDark = darkColorScheme(
    primary = MHColors.Lime,
    onPrimary = MHColors.Black,
    primaryContainer = MHColors.Lime,
    onPrimaryContainer = MHColors.Black,
    secondary = MHColors.Lime,
    onSecondary = MHColors.Black,
    secondaryContainer = MHColors.CardHigh,
    onSecondaryContainer = MHColors.TextHi,
    background = MHColors.BgBottom,
    onBackground = MHColors.TextHi,
    surface = MHColors.BgTop,
    onSurface = MHColors.TextHi,
    surfaceVariant = MHColors.Card,
    onSurfaceVariant = MHColors.TextLo,
    surfaceContainer = MHColors.Card,
    surfaceContainerHigh = MHColors.CardHigh,
    surfaceContainerHighest = MHColors.CardSelected,
    outline = MHColors.TextLo,
    outlineVariant = Color(0xFF2A2A2A),
)

/**
 * Experimental light scheme. Lime accent stays for brand consistency,
 * but text + surface roles flip. Mockup is dark-locked, so this is
 * "best-effort"; some screens that hard-code [MHColors.TextHi] /
 * [MHColors.BgTop] will still look dark and need follow-up. The
 * Settings → Tema screen warns the user.
 */
private val MHLight = lightColorScheme(
    primary = MHColors.LimeDim,
    onPrimary = Color(0xFF0A0A0A),
    primaryContainer = MHColors.LimePressed,
    onPrimaryContainer = Color(0xFF0A0A0A),
    secondary = MHColors.LimeDim,
    onSecondary = Color(0xFF0A0A0A),
    secondaryContainer = Color(0xFFE9E9E9),
    onSecondaryContainer = Color(0xFF111111),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF111111),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFF1F1F1),
    onSurfaceVariant = Color(0xFF555555),
    surfaceContainer = Color(0xFFF4F4F4),
    surfaceContainerHigh = Color(0xFFEAEAEA),
    surfaceContainerHighest = Color(0xFFDFDFDF),
    outline = Color(0xFF777777),
    outlineVariant = Color(0xFFD0D0D0),
)

/**
 * Mockup uses Inter for UI/headers and JetBrains Mono for badges,
 * durations and `// SECTION` eyebrows. Without bundled TTFs we map to
 * the closest system fallbacks; replace with `FontFamily(Font(...))`
 * once Inter + JetBrains Mono are added under `res/font/`.
 */
val InterFamily: FontFamily = FontFamily.SansSerif
val MonoFamily: FontFamily = FontFamily.Monospace

private val MHType = Typography(
    displayLarge = TextStyle(fontFamily = InterFamily, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp),
    headlineLarge = TextStyle(fontFamily = InterFamily, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.8).sp),
    headlineMedium = TextStyle(fontFamily = InterFamily, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.6).sp),
    headlineSmall = TextStyle(fontFamily = InterFamily, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontFamily = InterFamily, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
    titleMedium = TextStyle(fontFamily = InterFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontFamily = InterFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontFamily = InterFamily, fontSize = 16.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontFamily = InterFamily, fontSize = 14.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontFamily = InterFamily, fontSize = 12.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontFamily = InterFamily, fontSize = 14.sp, fontWeight = FontWeight.Bold),
    labelMedium = TextStyle(fontFamily = InterFamily, fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontFamily = InterFamily, fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
)

/**
 * Mono text styles — eyebrow `// SECTION` labels, badges, durations.
 * Not part of Material3's Typography scale, so accessed via a
 * CompositionLocal.
 */
data class MHMonoTextStyles(
    val eyebrow: TextStyle,        // `// PER TE` lime, 10sp, letterSpacing 1.5
    val badge: TextStyle,          // `NEW`, `VEN`, `S18` — 9–10sp accent
    val duration: TextStyle,       // `3:42` — 11sp lo
    val caption: TextStyle,        // `Ven 1 Mag · 3 nuove uscite` — 12.5sp lo
    val statValue: TextStyle,      // `247` stat tile big number — 22sp accent
)

val DefaultMHMono = MHMonoTextStyles(
    eyebrow = TextStyle(fontFamily = MonoFamily, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp),
    badge = TextStyle(fontFamily = MonoFamily, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
    duration = TextStyle(fontFamily = MonoFamily, fontSize = 11.sp, fontWeight = FontWeight.Normal),
    caption = TextStyle(fontFamily = MonoFamily, fontSize = 12.5.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.2.sp),
    statValue = TextStyle(fontFamily = MonoFamily, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.4).sp),
)

val LocalMHMono = staticCompositionLocalOf { DefaultMHMono }

/**
 * Standard MusicHub vertical gradient: surface → background. Theme-reactive
 * — reads [MaterialTheme.colorScheme] so dark mode paints the original
 * `BG_TOP → BG_BOTTOM` ladder while light mode paints near-white surfaces.
 *
 * Apply via `Modifier.background(MHGradient.screenBg())` from any
 * `@Composable` body (the function is composable now — older non-composable
 * callers will fail to compile and must be moved into a Composable scope).
 */
object MHGradient {
    @Composable
    fun screenBg(): Brush {
        val scheme = MaterialTheme.colorScheme
        return Brush.verticalGradient(
            0f to scheme.surface,
            1f to scheme.background,
        )
    }

    /** Per-screen gradient with a custom hero tint at the top. */
    @Composable
    fun heroBg(top: Color): Brush {
        val bottom = MaterialTheme.colorScheme.background
        return Brush.verticalGradient(
            0f to top,
            0.5f to bottom,
            1f to bottom,
        )
    }
}

/**
 * Theme-reactive accessors for MusicHub semantic tokens. Use these
 * inside @Composable scopes instead of the static [MHColors] members
 * when you want the value to respond to the user's theme picker.
 *
 * Static [MHColors] members stay available for non-composable contexts
 * (canvas drawScope, drawables) — they always return the dark-theme value.
 */
object MHTheme {
    val textHi: Color
        @Composable get() = MaterialTheme.colorScheme.onSurface

    val textLo: Color
        @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

    val card: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainer

    val cardHigh: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh

    val divider: Color
        @Composable get() = MaterialTheme.colorScheme.outlineVariant

    /** Lime accent — same in both themes. */
    val accent: Color get() = MHColors.Lime
}

@Composable
fun MediaPlayerTheme(content: @Composable () -> Unit) {
    val themeMode by PlayerSettings.instance.theme.collectAsStateWithLifecycle(initialValue = "dark")
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        "light" -> false
        "system" -> systemDark
        else -> true // "dark" + any unknown value
    }
    val scheme = if (useDark) MHDark else MHLight
    MaterialTheme(colorScheme = scheme, typography = MHType, content = content)
}
