package com.mediaplayer.android.ui.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediaplayer.android.data.PlaylistRepository
import com.mediaplayer.android.data.dto.PlaylistDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PlaylistsUiState {
    data object Loading : PlaylistsUiState
    data class Success(val playlists: List<PlaylistDto>) : PlaylistsUiState
    data class Error(val message: String) : PlaylistsUiState
}

/**
 * Drives the playlists list screen. The UX is simple:
 *  - Fetch once on construction, expose as a StateFlow.
 *  - Create mutates the list in-place on success (no refetch — the API
 *    returns the new [PlaylistDto] with songCount=0).
 *  - Delete is optimistic: remove locally, refetch on failure.
 */
class PlaylistsViewModel(
    private val repository: PlaylistRepository = PlaylistRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow<PlaylistsUiState>(PlaylistsUiState.Loading)
    val state: StateFlow<PlaylistsUiState> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = PlaylistsUiState.Loading
            _state.value = try {
                PlaylistsUiState.Success(repository.list())
            } catch (t: Throwable) {
                PlaylistsUiState.Error(t.message ?: "Unknown error")
            }
        }
    }

    fun pullRefresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _state.value = try {
                PlaylistsUiState.Success(repository.list())
            } catch (t: Throwable) {
                PlaylistsUiState.Error(t.message ?: "Unknown error")
            }
            _isRefreshing.value = false
        }
    }

    fun create(name: String, onResult: (Result<PlaylistDto>) -> Unit = {}) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            onResult(Result.failure(IllegalArgumentException("Name cannot be blank")))
            return
        }
        viewModelScope.launch {
            try {
                val created = repository.create(trimmed)
                val current = (_state.value as? PlaylistsUiState.Success)?.playlists.orEmpty()
                _state.value = PlaylistsUiState.Success(current + created)
                onResult(Result.success(created))
            } catch (t: Throwable) {
                onResult(Result.failure(t))
            }
        }
    }

    fun delete(id: Long) {
        val before = (_state.value as? PlaylistsUiState.Success)?.playlists ?: return
        _state.value = PlaylistsUiState.Success(before.filterNot { it.id == id })
        viewModelScope.launch {
            try {
                repository.delete(id)
            } catch (_: Throwable) {
                // Roll back by refetching — keeps list honest if the
                // delete failed on the server.
                refresh()
            }
        }
    }
}
