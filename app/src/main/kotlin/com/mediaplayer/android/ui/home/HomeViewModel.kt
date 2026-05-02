package com.mediaplayer.android.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediaplayer.android.data.HistoryRepository
import com.mediaplayer.android.data.PlaylistRepository
import com.mediaplayer.android.data.dto.PlaylistDto
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.ui.common.friendlyMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val recents: List<SongDto>,
        val playlists: List<PlaylistDto>,
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(
    private val history: HistoryRepository = HistoryRepository(),
    private val playlists: PlaylistRepository = PlaylistRepository(),
) : ViewModel() {
    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init { load() }

    fun refresh() = load()

    /** Silent reload — no spinner. Used on screen resume so returning from
     *  the player surfaces newly-played tracks in the recents row. */
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
        viewModelScope.launch {
            _state.value = HomeUiState.Loading
            loadOnce()
        }
    }

    private suspend fun loadOnce() {
        _state.value = try {
            val recents = runCatching { history.recent(8) }.getOrDefault(emptyList())
            val pls = runCatching { playlists.list() }.getOrDefault(emptyList())
            HomeUiState.Success(recents, pls)
        } catch (t: Throwable) {
            HomeUiState.Error(friendlyMessage(t))
        }
    }
}
