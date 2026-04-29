package com.mediaplayer.android.data.dto

import kotlinx.serialization.Serializable

/**
 * Mirrors the backend's `SongDto`. The server never emits `filePath`; streaming
 * happens through `/api/songs/{id}/stream`, covers through
 * `/api/songs/{id}/cover`.
 */
@Serializable
data class SongDto(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationMs: Long,
    val hasCoverArt: Boolean,
    val hasVideo: Boolean = false,
)
