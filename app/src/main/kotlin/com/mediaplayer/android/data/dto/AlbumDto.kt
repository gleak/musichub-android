package com.mediaplayer.android.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class AlbumDto(
    val name: String,
    val artist: String,
    val songCount: Int,
    val totalDurationMs: Long,
    val year: Int? = null,
    val coverSongId: Long? = null,
)

@Serializable
data class AlbumDetailDto(
    val name: String,
    val artist: String,
    val songs: List<SongDto>,
)
