package com.mediaplayer.android.data.dto

import kotlinx.serialization.Serializable

/**
 * Mirror of `com.mediaplayer.backend.song.dto.PageResponse`. Flat, lean
 * pagination envelope — the backend deliberately doesn't emit Spring's
 * internal `PageImpl` shape.
 */
@Serializable
data class PageResponse<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalItems: Long,
    val totalPages: Int,
)
