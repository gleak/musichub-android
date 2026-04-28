package com.mediaplayer.android.ui.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediaplayer.android.data.PlaylistRepository
import com.mediaplayer.android.data.dto.PlaylistDetailDto
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
class PlaylistDetailViewModel(
    private val playlistId: Long,
    private val repository: PlaylistRepository = PlaylistRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow<PlaylistDetailUiState>(PlaylistDetailUiState.Loading)
    val state: StateFlow<PlaylistDetailUiState> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = PlaylistDetailUiState.Loading
            _state.value = try {
                PlaylistDetailUiState.Success(repository.detail(playlistId))
            } catch (t: Throwable) {
                PlaylistDetailUiState.Error(t.message ?: "Unknown error")
            }
        }
    }

    fun pullRefresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _state.value = try {
                PlaylistDetailUiState.Success(repository.detail(playlistId))
            } catch (t: Throwable) {
                PlaylistDetailUiState.Error(t.message ?: "Unknown error")
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
}
