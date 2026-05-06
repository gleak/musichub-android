package com.mediaplayer.android.ui.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediaplayer.android.data.PlaylistsCache
import com.mediaplayer.android.data.dto.PlaylistDto
import com.mediaplayer.android.ui.common.friendlyMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PlaylistsUiState {
    data object Loading : PlaylistsUiState
    data class Success(val playlists: List<PlaylistDto>) : PlaylistsUiState
    data class Error(val message: String) : PlaylistsUiState
}

/**
 * Drives the playlists list screen. State is derived from
 * [PlaylistsCache] so creates/deletes performed elsewhere (Home tile,
 * kebab sheet, share importer) are reflected here without a refetch.
 */
class PlaylistsViewModel : ViewModel() {

    val state: StateFlow<PlaylistsUiState> = combine(
        PlaylistsCache.playlists,
        PlaylistsCache.initialLoading,
        PlaylistsCache.listError,
    ) { items, loading, error ->
        when {
            loading && items.isEmpty() -> PlaylistsUiState.Loading
            error != null && items.isEmpty() -> PlaylistsUiState.Error(friendlyMessage(error))
            else -> PlaylistsUiState.Success(items)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = PlaylistsUiState.Loading,
    )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        viewModelScope.launch { PlaylistsCache.primeIfEmpty() }
    }

    fun refresh() {
        viewModelScope.launch { runCatching { PlaylistsCache.refresh() } }
    }

    fun pullRefresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            runCatching { PlaylistsCache.refresh() }
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
            onResult(runCatching { PlaylistsCache.create(trimmed) })
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { runCatching { PlaylistsCache.delete(id) } }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
