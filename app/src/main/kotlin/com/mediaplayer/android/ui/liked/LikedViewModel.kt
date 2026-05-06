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
    data class Success(
        val songs: List<SongDto>,
        val totalItems: Long,
        val loadingMore: Boolean = false,
        val endReached: Boolean = false,
    ) : LikedUiState
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

    private var nextPage: Int = 0

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = LikedUiState.Loading
            loadFirstPage()
        }
    }

    fun pullRefresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadFirstPage()
            _isRefreshing.value = false
        }
    }

    private suspend fun loadFirstPage() {
        nextPage = 0
        _state.value = try {
            val page = repository.likedSongs(page = 0, size = PAGE_SIZE)
            nextPage = 1
            LikedUiState.Success(
                songs = page.items,
                totalItems = page.totalItems,
                endReached = page.items.size >= page.totalItems || page.items.isEmpty(),
            )
        } catch (t: Throwable) {
            LikedUiState.Error(friendlyMessage(t))
        }
    }

    /**
     * Append the next page. No-op when already loading, when the end has
     * been reached, or when the screen isn't in a Success state — guards
     * the LazyColumn's "near the end" trigger from spamming the backend
     * during fast flings.
     */
    fun loadMore() {
        val current = _state.value as? LikedUiState.Success ?: return
        if (current.loadingMore || current.endReached) return
        _state.value = current.copy(loadingMore = true)
        val pageToLoad = nextPage
        viewModelScope.launch {
            try {
                val page = repository.likedSongs(page = pageToLoad, size = PAGE_SIZE)
                val merged = current.songs + page.items
                nextPage = pageToLoad + 1
                _state.value = LikedUiState.Success(
                    songs = merged,
                    totalItems = page.totalItems,
                    loadingMore = false,
                    endReached = merged.size.toLong() >= page.totalItems || page.items.isEmpty(),
                )
            } catch (_: Throwable) {
                // Silent: keep the partial list; the user can scroll up + pull
                // to refresh if they want a retry. Surfacing this as a hard
                // error would erase their existing list.
                _state.value = current.copy(loadingMore = false)
            }
        }
    }

    fun unlike(songId: Long) {
        val current = (_state.value as? LikedUiState.Success) ?: return
        val target = current.songs.firstOrNull { it.id == songId }
        val label = target?.let { labelOf(it.title, it.artist) }
        // Optimistic: drop locally and decrement totalItems so the header
        // counter and the row both update before the server round-trip.
        _state.value = current.copy(
            songs = current.songs.filterNot { it.id == songId },
            totalItems = (current.totalItems - 1).coerceAtLeast(0),
        )
        viewModelScope.launch {
            try {
                repository.unlike(songId, displayLabel = label)
            } catch (_: Throwable) {
                refresh()
            }
        }
    }

    private fun labelOf(title: String?, artist: String?): String? {
        val t = title?.takeIf { it.isNotBlank() }
        val a = artist?.takeIf { it.isNotBlank() }
        return when {
            t != null && a != null -> "$t — $a"
            t != null -> t
            a != null -> a
            else -> null
        }
    }

    private companion object {
        const val PAGE_SIZE = 30
    }
}
