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
