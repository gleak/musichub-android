package com.mediaplayer.android.data.dto

import kotlinx.serialization.Serializable

/**
 * Summary of a playlist returned by `GET /api/playlists`. Matches the
 * backend's [`PlaylistDto`] record — name, size, timestamps, no songs.
 *
 * [createdAt] / [updatedAt] come back from the backend as ISO-8601
 * Instant strings; we keep them as strings here because neither the
 * list nor detail screens need to parse them — sorting is server-side.
 */
@Serializable
data class PlaylistDto(
    val id: Long,
    val name: String,
    val songCount: Int,
    val createdAt: String,
    val updatedAt: String,
    val coverSongId: Long? = null,
    val kind: String = "USER",
    val lastRefreshedAt: String? = null,
    /**
     * Up to four track ids backing a 2×2 cover collage on auto-playlist
     * tiles. Defaulted to empty so the field is forward-compatible with
     * older backend builds that don't emit it.
     */
    val coverSongIds: List<Long> = emptyList(),
    val autoSync: Boolean = false,
) {
    val isAuto: Boolean get() = kind != "USER"
}
