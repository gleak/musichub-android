package com.mediaplayer.android.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing scale used throughout the app. Replaces the ad-hoc 4/6/8/10/12/16/24
 * literals scattered across screens — every screen now uses one of these.
 *
 * Pick a value by intent, not by visual size:
 *  - [Xs] for tight inline gaps (icon + label, sub-text rows)
 *  - [S]  for grouped controls (between buttons in a row)
 *  - [M]  for content padding (default screen edge padding)
 *  - [L]  for section breaks (between cards, between header and body)
 *  - [Xl] for hero spacing (between hero art and section titles)
 */
object MediaPlayerSpacing {
    val Xs = 4.dp
    val S = 8.dp
    val M = 16.dp
    val L = 24.dp
    val Xl = 32.dp
}
