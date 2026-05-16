package com.mediaplayer.android.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.mediaplayer.android.data.DownloadRepository
import com.mediaplayer.android.data.LikedSongsCache
import com.mediaplayer.android.data.RecentsCache
import com.mediaplayer.android.data.SearchHistoryStore
import com.mediaplayer.android.data.SongRepository
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.ui.common.friendlyMessage
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
@UnstableApi
@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val repository: SongRepository = SongRepository(),
    private val searchHistoryStore: SearchHistoryStore = SearchHistoryStore.instance,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** Display name shown in the genre filter pill (e.g. "Hip-hop"). */
    private val _activeGenre = MutableStateFlow<String?>(null)
    val activeGenre: StateFlow<String?> = _activeGenre.asStateFlow()

    /**
     * Backend tag to filter on (e.g. "hip-hop") — distinct from the display
     * name because Last.fm tags are English while UI labels are Italian.
     * Null when no genre is active.
     */
    private val _activeGenreTag = MutableStateFlow<String?>(null)

    val downloadedIds: StateFlow<Set<Long>> = DownloadRepository.downloadedIds

    /**
     * Recently-played carousel on the idle search screen. Reads from
     * the shared [RecentsCache] so a track played from the player
     * appears here immediately, no per-VM fetch.
     */
    val recentSongs: StateFlow<List<SongDto>> = RecentsCache.recents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val recentQueries: StateFlow<List<String>> = searchHistoryStore.recent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    /**
     * Retry trigger combined into the state pipeline. Bumping this re-runs
     * the current query without typing into the field — `combine` makes the
     * downstream flow re-emit even when the query string itself hasn't
     * changed (which `distinctUntilChanged` would otherwise swallow).
     */
    private val _retryTick = MutableStateFlow(0)

    val state: StateFlow<SearchUiState> = combine(
        _query.debounce(DEBOUNCE_MS).distinctUntilChanged(),
        _activeGenreTag,
        _retryTick,
    ) { q, tag, _ -> q to tag }
        .flatMapLatest { (q, tag) ->
            flow {
                if (q.isBlank() && tag.isNullOrBlank()) {
                    emit(SearchUiState.Idle)
                } else {
                    emit(SearchUiState.Loading)
                    emit(fetch(q, tag))
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = SearchUiState.Idle,
        )

    fun retry() {
        // Guard against rapid double-taps on Riprova firing two parallel
        // fetches. flatMapLatest already cancels the prior request, but
        // the second tap still costs a network round-trip and an extra
        // Loading flicker — bail when we're already mid-load.
        if (state.value is SearchUiState.Loading) return
        _retryTick.value++
    }

    init {
        // Cold-start the cache when nothing else has populated it yet.
        viewModelScope.launch { RecentsCache.primeIfEmpty() }
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun clearQuery() {
        _query.value = ""
    }

    /** User-triggered commit (Enter / mic / history reuse). Persists query. */
    fun commitQuery(text: String) {
        val q = text.trim()
        if (q.isEmpty()) return
        _query.value = q
        viewModelScope.launch { searchHistoryStore.add(q) }
    }

    /**
     * Activate a genre filter. [displayName] populates the pill, [tagQuery]
     * is the actual `song_tags.tag` value sent to the backend. The free-text
     * query is left untouched — typing in the field then narrows within the
     * genre rather than replacing it.
     */
    fun selectGenre(displayName: String, tagQuery: String) {
        _activeGenre.value = displayName
        _activeGenreTag.value = tagQuery
    }

    fun clearGenre() {
        _activeGenre.value = null
        _activeGenreTag.value = null
    }

    fun removeRecent(query: String) {
        viewModelScope.launch { searchHistoryStore.remove(query) }
    }

    fun clearRecents() {
        viewModelScope.launch { searchHistoryStore.clear() }
    }

    fun toggleDownload(songId: Long, label: String? = null) {
        if (DownloadRepository.isDownloaded(songId)) DownloadRepository.remove(songId)
        else DownloadRepository.download(songId, label)
    }

    private suspend fun fetch(query: String, genre: String?): SearchUiState = try {
        val page = repository.listSongs(query = query, genre = genre)
        // Prime the shared liked-state cache so heart icons render in the
        // correct state on first paint (the screen's per-list LaunchedEffect
        // also primes — this just hits one batch sooner).
        LikedSongsCache.prime(page.items.map { it.id })
        SearchUiState.Success(page.items)
    } catch (t: Throwable) {
        SearchUiState.Error(friendlyMessage(t))
    }

    private companion object {
        const val DEBOUNCE_MS = 300L
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
