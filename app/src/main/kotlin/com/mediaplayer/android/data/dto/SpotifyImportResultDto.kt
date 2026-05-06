package com.mediaplayer.android.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class SpotifyImportResultDto(
    val playlistId: Long,
    val playlistName: String,
    val totalTracks: Int,
    val matched: Int,
    val approx: Int = 0,
    val queued: Int,
    val failed: Int,
)

@Serializable
data class SpotifyImportJobIdDto(
    val jobId: String,
)

/**
 * Wire shape of `GET /api/playlists/import/spotify/jobs/{jobId}`.
 *
 * `phase` is one of: PARSING, MATCHING, SAVING, DONE, ERROR.
 * `result` is non-null iff `phase == DONE`; `errorMessage` iff `ERROR`.
 */
@Serializable
data class SpotifyImportJobStatusDto(
    val jobId: String,
    val phase: String,
    val total: Int,
    val current: Int,
    val matched: Int,
    val approx: Int,
    val queued: Int,
    val failed: Int,
    val currentTrack: String? = null,
    val result: SpotifyImportResultDto? = null,
    val errorMessage: String? = null,
)
