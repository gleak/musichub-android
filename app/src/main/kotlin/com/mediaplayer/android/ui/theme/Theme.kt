package com.mediaplayer.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Spotify-inspired brand palette. Locked dark — Spotify is dark-only and
// the gradient/cover-driven hero artwork relies on a black backdrop.
object SpotifyColors {
    val Green = Color(0xFF1DB954)
    val GreenPressed = Color(0xFF1ED760)
    val Black = Color(0xFF000000)
    val Surface = Color(0xFF121212)        // app background
    val SurfaceContainer = Color(0xFF181818) // cards/tiles
    val SurfaceContainerHigh = Color(0xFF282828) // raised tiles, mini player
    val SurfaceContainerHighest = Color(0xFF3E3E3E) // chip / selected
    val OnSurface = Color(0xFFFFFFFF)
    val OnSurfaceVariant = Color(0xFFB3B3B3) // Spotify subtitle gray
    val OutlineVariant = Color(0xFF2A2A2A)
    val LikedGradientStart = Color(0xFF4F1AAF)
    val LikedGradientEnd = Color(0xFF8A8FFF)

    // "Browse all" tiles — Spotify's pink/purple accent pair.
    val BrowseAlbumsTile = Color(0xFFE8115B)
    val BrowseArtistsTile = Color(0xFF8400E7)
}

private val SpotifyDark = darkColorScheme(
    primary = SpotifyColors.Green,
    onPrimary = SpotifyColors.Black,
    primaryContainer = SpotifyColors.Green,
    onPrimaryContainer = SpotifyColors.Black,
    secondary = SpotifyColors.Green,
    onSecondary = SpotifyColors.Black,
    secondaryContainer = SpotifyColors.SurfaceContainerHigh,
    onSecondaryContainer = SpotifyColors.OnSurface,
    background = SpotifyColors.Black,
    onBackground = SpotifyColors.OnSurface,
    surface = SpotifyColors.Surface,
    onSurface = SpotifyColors.OnSurface,
    surfaceVariant = SpotifyColors.SurfaceContainer,
    onSurfaceVariant = SpotifyColors.OnSurfaceVariant,
    surfaceContainer = SpotifyColors.SurfaceContainer,
    surfaceContainerHigh = SpotifyColors.SurfaceContainerHigh,
    surfaceContainerHighest = SpotifyColors.SurfaceContainerHighest,
    outline = SpotifyColors.OnSurfaceVariant,
    outlineVariant = SpotifyColors.OutlineVariant,
)

private val SpotifyType = Typography(
    displayLarge = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.ExtraBold),
    headlineLarge = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.ExtraBold),
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.ExtraBold),
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
)

@Composable
fun MediaPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SpotifyDark, typography = SpotifyType, content = content)
}
