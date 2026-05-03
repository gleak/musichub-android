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
    /**
     * True when the current user already owns the playlist or has previously
     * accepted the share. Lets the client skip the "Add to library" CTA and
     * navigate straight to the existing playlist instead of producing a
     * duplicate. Defaulted so older backends still parse.
     */
    val alreadyAccessible: Boolean = false,
)
