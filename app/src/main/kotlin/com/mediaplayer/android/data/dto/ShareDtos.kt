package com.mediaplayer.android.data.dto

import kotlinx.serialization.Serializable

/** Mirror of `ShareLinkDto` — what the owner gets when they mint a link. */
@Serializable
data class ShareLinkDto(
    val token: String,
    val url: String,
)

/** Mirror of `SharePreviewDto` — recipient's pre-import preview. */
@Serializable
data class SharePreviewDto(
    val token: String,
    val playlistName: String,
    val songCount: Int,
    val ownerName: String,
    val coverSongId: Long? = null,
)
