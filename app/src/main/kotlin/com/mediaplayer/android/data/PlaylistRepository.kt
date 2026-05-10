package com.mediaplayer.android.data

import com.mediaplayer.android.data.dto.AddSongRequest
import com.mediaplayer.android.data.dto.CreatePlaylistRequest
import com.mediaplayer.android.data.dto.PlaylistDetailDto
import com.mediaplayer.android.data.dto.PlaylistDto
import com.mediaplayer.android.data.dto.RenamePlaylistRequest
import com.mediaplayer.android.data.dto.ReorderSongsRequest
import com.mediaplayer.android.data.dto.SetAutoSyncRequest
import com.mediaplayer.android.data.dto.ShareLinkDto
import com.mediaplayer.android.data.dto.SharePreviewDto
import com.mediaplayer.android.data.sync.ReadCache
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.serializer
import java.io.IOException

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
        return try {
            val auto = runCatching { api.listPlaylists(kind = "auto") }.getOrDefault(emptyList())
            val user = api.listPlaylists()
            val combined = auto + user
            ReadCache.putJson(ReadCache.Keys.PLAYLISTS_ALL, combined, ListSerializer(serializer()))
            combined
        } catch (_: IOException) {
            // Offline — show whatever the cache has, empty if user has
            // never opened this screen online.
            ReadCache.getOrNull(ReadCache.Keys.PLAYLISTS_ALL, ListSerializer(serializer<PlaylistDto>())) ?: emptyList()
        }
    }

    suspend fun detail(id: Long): PlaylistDetailDto {
        return try {
            val fresh = api.getPlaylist(id)
            ReadCache.putJson(ReadCache.Keys.playlistDetail(id), fresh, serializer())
            fresh
        } catch (e: IOException) {
            ReadCache.getOrNull(ReadCache.Keys.playlistDetail(id), serializer<PlaylistDetailDto>()) ?: throw e
        }
    }

    suspend fun create(name: String): PlaylistDto =
        api.createPlaylist(CreatePlaylistRequest(name.trim()))

    suspend fun rename(id: Long, name: String): PlaylistDto =
        api.renamePlaylist(id, RenamePlaylistRequest(name.trim()))

    suspend fun delete(id: Long) {
        api.deletePlaylist(id)
    }

    suspend fun setAutoSync(id: Long, enabled: Boolean): PlaylistDto =
        api.setPlaylistAutoSync(id, SetAutoSyncRequest(enabled))

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

    /** Owner-only: revoke every active share token for the playlist. */
    suspend fun revokeShares(playlistId: Long) {
        api.revokePlaylistShares(playlistId)
    }

    suspend fun listMembers(playlistId: Long): List<com.mediaplayer.android.data.dto.PlaylistMemberDto> =
        api.listPlaylistMembers(playlistId)

    suspend fun kickMember(playlistId: Long, userId: Long) {
        api.kickPlaylistMember(playlistId, userId)
    }

    /** Triggers a manual Daily Mix recompute. Returns the number of refreshed entries. */
    suspend fun refreshDailyMix(): Int = api.refreshDailyMix().refreshed

    /**
     * Recompute every auto-playlist family for the current user (Discover Daily,
     * On Repeat, Daily Mix, Mood, Release Radar, Time Capsule, Up Next, Radar).
     * Returns the total refreshed entries across all families.
     */
    suspend fun refreshAllAutoPlaylists(): Int = api.refreshAllAutoPlaylists().refreshed
}
