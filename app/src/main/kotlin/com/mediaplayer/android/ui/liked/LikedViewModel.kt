package com.mediaplayer.android.ui.liked

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.mediaplayer.android.data.DownloadRepository
import com.mediaplayer.android.data.LikedRepository
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.ui.common.friendlyMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LikedUiState {
    data object Loading : LikedUiState
    data class Success(val songs: List<SongDto>) : LikedUiState
    data class Error(val message: String) : LikedUiState
}

@UnstableApi
class LikedViewModel(
    private val repository: LikedRepository = LikedRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow<LikedUiState>(LikedUiState.Loading)
    val state: StateFlow<LikedUiState> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val downloadedIds: StateFlow<Set<Long>> = DownloadRepository.downloadedIds

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = LikedUiState.Loading
            _state.value = try {
                LikedUiState.Success(repository.likedSongs().items)
            } catch (t: Throwable) {
                LikedUiState.Error(friendlyMessage(t))
            }
        }
    }

    fun pullRefresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _state.value = try {
                LikedUiState.Success(repository.likedSongs().items)
            } catch (t: Throwable) {
                LikedUiState.Error(friendlyMessage(t))
            }
            _isRefreshing.value = false
        }
    }

    fun unlike(songId: Long) {
        val current = (_state.value as? LikedUiState.Success)?.songs ?: return
        _state.value = LikedUiState.Success(current.filterNot { it.id == songId })
        viewModelScope.launch {
            try {
                repository.unlike(songId)
            } catch (_: Throwable) {
                refresh()
            }
        }
    }
}
