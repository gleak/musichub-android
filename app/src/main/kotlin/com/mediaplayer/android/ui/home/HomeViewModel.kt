package com.mediaplayer.android.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediaplayer.android.data.PlaylistsCache
import com.mediaplayer.android.data.RecentsCache
import com.mediaplayer.android.data.dto.PlaylistDto
import com.mediaplayer.android.data.dto.SongDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val recents: List<SongDto>,
        val playlists: List<PlaylistDto>,
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

/**
 * Home is a thin combiner over the shared caches — playlists come from
 * [PlaylistsCache], recents from [RecentsCache]. So a song the user just
 * played, a playlist freshly created from another tab, or an accepted
 * share all surface here without per-VM fetching.
 */
class HomeViewModel : ViewModel() {

    private val _recentsLoaded = MutableStateFlow(false)

    val state: StateFlow<HomeUiState> = combine(
        RecentsCache.recents,
        _recentsLoaded,
        PlaylistsCache.playlists,
        PlaylistsCache.initialLoading,
    ) { recents, recentsLoaded, playlists, playlistsLoading ->
        when {
            !recentsLoaded || (playlistsLoading && playlists.isEmpty()) -> HomeUiState.Loading
            // Cap the recents row at 8 to match the original Home limit;
            // RecentsCache holds the broader pool used by Search too.
            else -> HomeUiState.Success(recents.take(HOME_RECENTS_LIMIT), playlists)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = HomeUiState.Loading,
    )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init { load() }

    fun refresh() = load()

    /** Silent reload — no spinner. Returns to a freshly-played track. */
    fun resume() {
        viewModelScope.launch { loadOnce() }
    }

    fun pullRefresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadOnce()
            _isRefreshing.value = false
        }
    }

    private fun load() {
        viewModelScope.launch { loadOnce() }
    }

    private suspend fun loadOnce() {
        runCatching { RecentsCache.refresh() }
        _recentsLoaded.value = true
        runCatching { PlaylistsCache.refresh() }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val HOME_RECENTS_LIMIT = 8
    }
}
