package com.mediaplayer.android.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Cover-art and surface corner radii. Replaces the 4/6/8/10/12dp literals
 * that previously lived per-screen.
 *
 *  - [Skeleton]   — shimmer placeholder bars (~12–14dp tall)
 *  - [SongRow]    — small list row covers / playlist tile inner cover (~48–180dp art)
 *  - [Tile]       — grid tile container / browse tiles (~120dp+)
 *  - [MiniPlayer] — mini-player chip cover (alias of SongRow radius — kept for semantic clarity)
 *  - [Card]       — promo cards / onboarding pick tiles
 *  - [Banner]     — full-width inline banners (anonymous, etc.)
 */
object CoverShapes {
    val Skeleton = RoundedCornerShape(4.dp)
    val SongRow = RoundedCornerShape(6.dp)
    val Tile = RoundedCornerShape(8.dp)
    val MiniPlayer = RoundedCornerShape(6.dp)
    val Card = RoundedCornerShape(10.dp)
    val Banner = RoundedCornerShape(12.dp)
}

/**
 * Hero cover sizing rules. Two distinct shapes:
 *
 *  - [DetailFraction] / [DetailMax] — used by `SpotifyHero` on detail
 *    screens (album, artist, playlist). Cover sits inline with metadata
 *    so it stays smaller than the screen width.
 *  - [NowPlayingFraction] / [NowPlayingMax] — used by `NowPlayingSheet`.
 *    Cover IS the screen, so we lean into the full width minus a margin.
 *
 * Magic numbers were chosen visually to match Spotify on a 360–411dp
 * width range; lifted here so future tweaks happen in one place.
 */
object HeroCoverSize {
    const val DetailFraction = 0.6f
    val DetailMax = 280.dp

    const val NowPlayingFraction = 0.92f
    val NowPlayingMax = 360.dp
}
