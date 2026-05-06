package com.mediaplayer.android.data.dto

import kotlinx.serialization.Serializable

/**
 * One row in [PlaylistDetailDto.songs]. The backend includes the
 * `playlist_songs` surrogate id so the Android client can use it as a
 * stable LazyColumn key — playlists allow the same song twice, so
 * `song.id` alone isn't unique. Stable per-occurrence keys are what
 * make `Modifier.animateItem()` animate reorders instead of remounting.
 */
@Serializable
data class PlaylistSongEntryDto(
    val playlistSongId: Long,
    val song: SongDto,
    /**
     * Contributor — user who added this song to the playlist. Null when
     * unknown (legacy rows or auto-playlist server-side inserts). The UI
     * shows a per-track contributor pill on collaborative playlists when
     * this is set AND differs from the playlist owner.
     */
    val addedByUserId: Long? = null,
    val addedByName: String? = null,
)
