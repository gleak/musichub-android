package com.mediaplayer.android.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

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

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun clearQuery() {
        _query.value = ""
    }

    private suspend fun fetch(query: String): SearchUiState = try {
        val page = repository.listSongs(query = query)
        SearchUiState.Success(page.items)
    } catch (t: Throwable) {
        SearchUiState.Error(t.message ?: "Unknown error")
    }

    private companion object {
        const val DEBOUNCE_MS = 300L
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
