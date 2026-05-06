package com.mediaplayer.android.ui.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.mediaplayer.android.data.DownloadRepository
import com.mediaplayer.android.data.PlaylistsCache
import com.mediaplayer.android.data.dto.PlaylistDetailDto
import com.mediaplayer.android.ui.common.friendlyMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PlaylistDetailUiState {
    data object Loading : PlaylistDetailUiState
    data class Success(val playlist: PlaylistDetailDto) : PlaylistDetailUiState
    data class Error(val message: String) : PlaylistDetailUiState
}

/**
 * Owns the [PlaylistDetailDto] for one playlist. State is derived from
 * [PlaylistsCache] so a song added from a kebab elsewhere or a rename
 * triggered from another screen lands here without a refetch.
 *
 * The playlist id is passed in explicitly rather than via SavedStateHandle
 * to keep this VM simple to construct from Compose's `viewModel(key = ...)`.
 */
@UnstableApi
class PlaylistDetailViewModel(
    private val playlistId: Long,
) : ViewModel() {

    private val _loadError = MutableStateFlow<Throwable?>(null)

    val state: StateFlow<PlaylistDetailUiState> = combine(
        PlaylistsCache.details.map { it[playlistId] },
        _loadError,
    ) { detail, error ->
        when {
            detail != null -> PlaylistDetailUiState.Success(detail)
            error != null -> PlaylistDetailUiState.Error(friendlyMessage(error))
            else -> PlaylistDetailUiState.Loading
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = PlaylistDetailUiState.Loading,
    )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val downloadedIds: StateFlow<Set<Long>> = DownloadRepository.downloadedIds

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            try {
                PlaylistsCache.refreshDetail(playlistId)
                _loadError.value = null
            } catch (t: Throwable) {
                _loadError.value = t
            }
        }
    }

    fun pullRefresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                PlaylistsCache.refreshDetail(playlistId)
                _loadError.value = null
            } catch (t: Throwable) {
                _loadError.value = t
            }
            _isRefreshing.value = false
        }
    }

    fun removeSong(songId: Long) {
        viewModelScope.launch {
            runCatching { PlaylistsCache.removeSong(playlistId, songId) }
                .onFailure { refresh() }
        }
    }

    fun reorderSongs(songIds: List<Long>) {
        viewModelScope.launch {
            runCatching { PlaylistsCache.reorder(playlistId, songIds) }
                .onFailure { refresh() }
        }
    }

    fun downloadPlaylist() {
        val entries = PlaylistsCache.details.value[playlistId]?.songs ?: return
        val missing = entries
            .filterNot { DownloadRepository.isDownloaded(it.song.id) }
            .map { it.song.id to it.song.title }
        DownloadRepository.downloadAllLabeled(missing)
    }

    fun removePlaylistDownloads() {
        val entries = PlaylistsCache.details.value[playlistId]?.songs ?: return
        DownloadRepository.removeAll(entries.map { it.song.id })
    }

    /**
     * Owner = cascade delete; member = leave the share. Backend is the same
     * `DELETE /api/playlists/{id}` endpoint — server inspects ownership and
     * either drops the row entirely or only the membership.
     */
    fun deleteOrLeave(onDone: (success: Boolean, isMember: Boolean) -> Unit) {
        val current = PlaylistsCache.details.value[playlistId]
        val isMember = current?.isOwner == false
        viewModelScope.launch {
            val ok = runCatching { PlaylistsCache.delete(playlistId) }.isSuccess
            onDone(ok, isMember)
        }
    }

    fun toggleAutoSync() {
        val current = PlaylistsCache.details.value[playlistId] ?: return
        val next = !current.autoSync
        viewModelScope.launch {
            try {
                PlaylistsCache.setAutoSync(playlistId, next)
                if (next) {
                    val missing = current.songs
                        .filterNot { DownloadRepository.isDownloaded(it.song.id) }
                        .map { it.song.id to it.song.title }
                    DownloadRepository.downloadAllLabeled(missing)
                }
            } catch (_: Throwable) {
                // Cache rolls itself back; just refresh detail to catch
                // anything else server-side that diverged.
                refresh()
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
