package com.mediaplayer.android.data.dto

import kotlinx.serialization.Serializable

/** Body for `POST /api/playlists`. */
@Serializable
data class CreatePlaylistRequest(val name: String)

/** Body for `PATCH /api/playlists/{id}`. */
@Serializable
data class RenamePlaylistRequest(val name: String)

/** Body for `POST /api/playlists/{id}/songs`. */
@Serializable
data class AddSongRequest(val songId: Long)

/**
 * Body for `PUT /api/playlists/{id}/songs`. Replaces the ordered
 * contents of the playlist; used for reorder and bulk edit.
 */
@Serializable
data class ReorderSongsRequest(val songIds: List<Long>)
