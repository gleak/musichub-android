package com.mediaplayer.android.data.dto

import kotlinx.serialization.Serializable

/**
 * Full playlist payload returned by `GET /api/playlists/{id}`.
 * The [songs] list is ordered — index 0 plays first.
 */
@Serializable
data class PlaylistDetailDto(
    val id: Long,
    val name: String,
    val createdAt: String,
    val updatedAt: String,
    val songs: List<PlaylistSongEntryDto>,
)
