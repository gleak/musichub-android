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
    /**
     * Sharing fields — populated by collaborative-playlist backends (≥0.13.0).
     * Defaulted so older backends that don't emit these still parse: in that
     * case [isOwner] = true matches the legacy "every playlist you can see is
     * yours" assumption, and the shared-by indicator stays hidden.
     */
    val ownerId: Long? = null,
    val ownerName: String? = null,
    val isOwner: Boolean = true,
    val memberCount: Int = 0,
) {
    val isAuto: Boolean get() = kind != "USER"
    /** True when the playlist is shared (owner has at least one member, or current user is a member). */
    val isShared: Boolean get() = !isOwner || memberCount > 0
}
