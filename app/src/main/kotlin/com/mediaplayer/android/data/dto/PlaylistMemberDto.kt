package com.mediaplayer.android.data.dto

import kotlinx.serialization.Serializable

/**
 * Mirror of backend `PlaylistMemberDto` — one row in the Manage Members
 * screen. Owner is surfaced as the first row with [owner] = true.
 */
@Serializable
data class PlaylistMemberDto(
    val userId: Long,
    val name: String,
    val joinedAt: String,
    val owner: Boolean,
)
