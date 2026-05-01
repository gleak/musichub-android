package com.mediaplayer.android.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Cover-art corner radii used across rows, tiles, mini-player, hero. Replaces
 * the 4/6/8/10dp values that previously lived as literals on each screen.
 *
 *  - [SongRow]      — small list row covers (~48dp art)
 *  - [Tile]         — grid tile / playlist row / hero (~80–280dp art)
 *  - [MiniPlayer]   — mini-player chip cover
 *  - [Card]         — promo cards / shortcut grid items
 */
object CoverShapes {
    val SongRow = RoundedCornerShape(6.dp)
    val Tile = RoundedCornerShape(8.dp)
    val MiniPlayer = RoundedCornerShape(6.dp)
    val Card = RoundedCornerShape(10.dp)
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
