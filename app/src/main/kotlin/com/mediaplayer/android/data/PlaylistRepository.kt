package com.mediaplayer.android.data

import com.mediaplayer.android.data.dto.AddSongRequest
import com.mediaplayer.android.data.dto.CreatePlaylistRequest
import com.mediaplayer.android.data.dto.PlaylistDetailDto
import com.mediaplayer.android.data.dto.PlaylistDto
import com.mediaplayer.android.data.dto.RenamePlaylistRequest
import com.mediaplayer.android.data.dto.ReorderSongsRequest

/**
 * Thin wrapper over [MediaPlayerApi]'s playlist endpoints. Exists so
 * ViewModels depend on code we own and so tests can substitute a fake.
 */
class PlaylistRepository(
    private val api: MediaPlayerApi = Network.api,
) {

    suspend fun list(): List<PlaylistDto> = api.listPlaylists()

    suspend fun detail(id: Long): PlaylistDetailDto = api.getPlaylist(id)

    suspend fun create(name: String): PlaylistDto =
        api.createPlaylist(CreatePlaylistRequest(name.trim()))

    suspend fun rename(id: Long, name: String): PlaylistDto =
        api.renamePlaylist(id, RenamePlaylistRequest(name.trim()))

    suspend fun delete(id: Long) {
        api.deletePlaylist(id)
    }

    suspend fun addSong(playlistId: Long, songId: Long): PlaylistDetailDto =
        api.addSongToPlaylist(playlistId, AddSongRequest(songId))

    suspend fun removeSong(playlistId: Long, songId: Long): PlaylistDetailDto =
        api.removeSongFromPlaylist(playlistId, songId)

    suspend fun reorder(playlistId: Long, songIds: List<Long>): PlaylistDetailDto =
        api.reorderPlaylistSongs(playlistId, ReorderSongsRequest(songIds))
}
