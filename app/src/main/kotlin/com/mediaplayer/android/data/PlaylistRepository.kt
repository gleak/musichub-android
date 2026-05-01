package com.mediaplayer.android.data

import com.mediaplayer.android.data.dto.AddSongRequest
import com.mediaplayer.android.data.dto.CreatePlaylistRequest
import com.mediaplayer.android.data.dto.PlaylistDetailDto
import com.mediaplayer.android.data.dto.PlaylistDto
import com.mediaplayer.android.data.dto.RenamePlaylistRequest
import com.mediaplayer.android.data.dto.ReorderSongsRequest
import com.mediaplayer.android.data.dto.ShareLinkDto
import com.mediaplayer.android.data.dto.SharePreviewDto

/**
 * Thin wrapper over [MediaPlayerApi]'s playlist endpoints. Exists so
 * ViewModels depend on code we own and so tests can substitute a fake.
 */
class PlaylistRepository(
    private val api: MediaPlayerApi = Network.api,
) {

    /**
     * Returns auto-playlists (Discover Daily / On Repeat) first, then the
     * user's hand-curated playlists. Backend's `GET /api/playlists` defaults
     * to `kind=USER`, so we hit it twice and merge — auto rows would be
     * invisible otherwise.
     */
    suspend fun list(): List<PlaylistDto> {
        val auto = runCatching { api.listPlaylists(kind = "auto") }.getOrDefault(emptyList())
        val user = api.listPlaylists()
        return auto + user
    }

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

    suspend fun createShare(playlistId: Long): ShareLinkDto =
        api.createPlaylistShare(playlistId)

    suspend fun previewShare(token: String): SharePreviewDto =
        api.previewPlaylistShare(token)

    suspend fun acceptShare(token: String): PlaylistDetailDto =
        api.acceptPlaylistShare(token)
}
