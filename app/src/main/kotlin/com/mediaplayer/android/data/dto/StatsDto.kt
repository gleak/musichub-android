package com.mediaplayer.android.data.dto

import kotlinx.serialization.Serializable

/**
 * Counts shown on the Profile header. Songs and artists are catalog-wide
 * (the catalog is shared across users); playlists are scoped to the
 * requesting user. Mirrors backend `StatsDto` record.
 */
@Serializable
data class StatsDto(
    val songs: Long,
    val playlists: Long,
    val artists: Long,
)
