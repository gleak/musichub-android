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
    val kind: String = "USER",
    val lastRefreshedAt: String? = null,
    val autoSync: Boolean = false,
    /**
     * Sharing fields — see [PlaylistDto] for forward-compat rationale.
     * The detail screen uses these to: (a) show "Shared by X" sub-header,
     * (b) hide the auto-sync card for non-owners, (c) flip the destructive
     * action label between "Delete playlist" (owner) and "Remove from
     * library" (member).
     */
    val ownerId: Long? = null,
    val ownerName: String? = null,
    val isOwner: Boolean = true,
    val memberCount: Int = 0,
) {
    val isAuto: Boolean get() = kind != "USER"
    val isShared: Boolean get() = !isOwner || memberCount > 0
}
