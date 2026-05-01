package com.mediaplayer.android.ui.foryou

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.data.dto.PlaylistDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ForYouUiState {
    data object Loading : ForYouUiState
    data class Error(val message: String) : ForYouUiState
    data class Ready(val autoPlaylists: List<PlaylistDto>) : ForYouUiState
}

class ForYouViewModel : ViewModel() {
    private val _state = MutableStateFlow<ForYouUiState>(ForYouUiState.Loading)
    val state: StateFlow<ForYouUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = ForYouUiState.Loading
            runCatching { Network.api.listPlaylists(kind = "auto") }
                .onSuccess { _state.value = ForYouUiState.Ready(it) }
                .onFailure { _state.value = ForYouUiState.Error(it.message ?: "Unknown error") }
        }
    }
}
