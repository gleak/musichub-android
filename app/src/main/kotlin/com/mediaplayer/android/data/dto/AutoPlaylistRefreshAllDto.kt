package com.mediaplayer.android.data.dto

import kotlinx.serialization.Serializable

/**
 * Result of `POST /api/playlists/auto/refresh-all`. The backend fans out
 * the request across every auto-playlist family (Discover Daily, On Repeat,
 * the six Daily Mix slots, the six Mood slots, Release Radar, Time Capsule,
 * Up Next, Radar) and returns both a flat total and a per-family breakdown
 * — the Profile / Download Offline screens render the total inline and use
 * the breakdown for diagnostics if a family silently fails.
 */
@Serializable
data class AutoPlaylistRefreshAllDto(
    val userId: Long,
    val refreshed: Int,
    val byFamily: Map<String, Int> = emptyMap(),
)
