package com.mediaplayer.android.ui.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.mediaplayer.android.data.DownloadRepository
import com.mediaplayer.android.data.PlaylistRepository
import com.mediaplayer.android.data.dto.PlaylistDetailDto
import com.mediaplayer.android.ui.common.friendlyMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PlaylistDetailUiState {
    data object Loading : PlaylistDetailUiState
    data class Success(val playlist: PlaylistDetailDto) : PlaylistDetailUiState
    data class Error(val message: String) : PlaylistDetailUiState
}

/**
 * Owns the [PlaylistDetailDto] for one playlist. The playlist id is
 * passed in explicitly rather than via SavedStateHandle to keep this
 * VM simple to construct from Compose's `viewModel(key = ...)`.
 */
@UnstableApi
class PlaylistDetailViewModel(
    private val playlistId: Long,
    private val repository: PlaylistRepository = PlaylistRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow<PlaylistDetailUiState>(PlaylistDetailUiState.Loading)
    val state: StateFlow<PlaylistDetailUiState> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val downloadedIds: StateFlow<Set<Long>> = DownloadRepository.downloadedIds

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            if (_state.value !is PlaylistDetailUiState.Success) {
                _state.value = PlaylistDetailUiState.Loading
            }
            _state.value = try {
                PlaylistDetailUiState.Success(repository.detail(playlistId))
            } catch (t: Throwable) {
                PlaylistDetailUiState.Error(friendlyMessage(t))
            }
        }
    }

    fun pullRefresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _state.value = try {
                PlaylistDetailUiState.Success(repository.detail(playlistId))
            } catch (t: Throwable) {
                PlaylistDetailUiState.Error(friendlyMessage(t))
            }
            _isRefreshing.value = false
        }
    }

    fun removeSong(songId: Long) {
        viewModelScope.launch {
            try {
                val updated = repository.removeSong(playlistId, songId)
                _state.value = PlaylistDetailUiState.Success(updated)
            } catch (_: Throwable) {
                refresh()
            }
        }
    }

    fun reorderSongs(songIds: List<Long>) {
        viewModelScope.launch {
            try {
                val updated = repository.reorder(playlistId, songIds)
                _state.value = PlaylistDetailUiState.Success(updated)
            } catch (_: Throwable) {
                refresh()
            }
        }
    }

    fun downloadPlaylist() {
        val entries = (state.value as? PlaylistDetailUiState.Success)?.playlist?.songs ?: return
        val missing = entries
            .filterNot { DownloadRepository.isDownloaded(it.song.id) }
            .map { it.song.id to it.song.title }
        DownloadRepository.downloadAllLabeled(missing)
    }

    fun removePlaylistDownloads() {
        val entries = (state.value as? PlaylistDetailUiState.Success)?.playlist?.songs ?: return
        DownloadRepository.removeAll(entries.map { it.song.id })
    }

    fun toggleAutoSync() {
        val current = (state.value as? PlaylistDetailUiState.Success)?.playlist ?: return
        val next = !current.autoSync
        // Optimistic flip so the icon doesn't lag the tap; on failure we
        // refresh() and the server's value reasserts.
        _state.value = PlaylistDetailUiState.Success(current.copy(autoSync = next))
        viewModelScope.launch {
            try {
                repository.setAutoSync(playlistId, next)
                // If the toggle just turned on, kick the runner so the user
                // doesn't have to wait for the next cold launch.
                if (next) {
                    val missing = current.songs
                        .filterNot { DownloadRepository.isDownloaded(it.song.id) }
                        .map { it.song.id to it.song.title }
                    DownloadRepository.downloadAllLabeled(missing)
                }
            } catch (_: Throwable) {
                refresh()
            }
        }
    }
}
