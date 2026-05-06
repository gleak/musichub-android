package com.mediaplayer.android.data.dto

import kotlinx.serialization.Serializable

/** Body for POST /api/playlists/replace-song — swap song refs across user's playlists. */
@Serializable
data class ReplaceSongRequest(val oldSongId: Long, val newSongId: Long)

@Serializable
data class ReplaceSongResponse(val updated: Int)
