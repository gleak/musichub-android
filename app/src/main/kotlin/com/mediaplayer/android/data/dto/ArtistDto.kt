package com.mediaplayer.android.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ArtistDto(
    val name: String,
    val albumCount: Int,
    val songCount: Int,
)

@Serializable
data class ArtistDetailDto(
    val name: String,
    val albums: List<AlbumDto>,
    val songs: List<SongDto>,
)
