package com.mediaplayer.android.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediaplayer.android.data.HistoryRepository
import com.mediaplayer.android.data.LikedRepository
import com.mediaplayer.android.data.SongRepository
import com.mediaplayer.android.data.dto.SongDto
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(val songs: List<SongDto>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

/**
 * Emits UI state for the search screen.
 *
 * - The query is debounced 300ms so we don't hammer the backend on every
 *   keystroke.
 * - `flatMapLatest` cancels an in-flight request when the query changes —
 *   avoids races where a slower earlier response overwrites a fresher one.
 * - Empty query returns the full catalog (same as the web `curl` flow), so
 *   the landing screen shows something useful.
 */
@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val repository: SongRepository = SongRepository(),
    private val likedRepository: LikedRepository = LikedRepository(),
    private val historyRepository: HistoryRepository = HistoryRepository(),
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _likedIds = MutableStateFlow<Set<Long>>(emptySet())
    val likedIds: StateFlow<Set<Long>> = _likedIds.asStateFlow()

    private val _recentSongs = MutableStateFlow<List<SongDto>>(emptyList())
    val recentSongs: StateFlow<List<SongDto>> = _recentSongs.asStateFlow()

    val state: StateFlow<SearchUiState> = _query
        .debounce(DEBOUNCE_MS)
        .distinctUntilChanged()
        .flatMapLatest { q ->
            flow {
                emit(SearchUiState.Loading)
                emit(fetch(q))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = SearchUiState.Idle,
        )

    init {
        viewModelScope.launch {
            try { _recentSongs.value = historyRepository.recent(20) } catch (_: Throwable) {}
        }
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun clearQuery() {
        _query.value = ""
    }

    fun toggleLike(songId: Long) {
        val isLiked = songId in _likedIds.value
        _likedIds.value = if (isLiked) _likedIds.value - songId else _likedIds.value + songId
        viewModelScope.launch {
            try {
                if (isLiked) likedRepository.unlike(songId) else likedRepository.like(songId)
            } catch (_: Throwable) {
                _likedIds.value = if (isLiked) _likedIds.value + songId else _likedIds.value - songId
            }
        }
    }

    private suspend fun fetch(query: String): SearchUiState = try {
        val page = repository.listSongs(query = query)
        val ids = page.items.map { it.id }
        _likedIds.value = if (ids.isEmpty()) emptySet() else likedRepository.status(ids)
        SearchUiState.Success(page.items)
    } catch (t: Throwable) {
        SearchUiState.Error(t.message ?: "Unknown error")
    }

    private companion object {
        const val DEBOUNCE_MS = 300L
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
