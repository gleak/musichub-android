package com.mediaplayer.android.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class SpotifyImportResultDto(
    val playlistId: Long,
    val playlistName: String,
    val totalTracks: Int,
    val matched: Int,
    val queued: Int,
    val failed: Int,
)
