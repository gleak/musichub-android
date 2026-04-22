package com.mediaplayer.android.ui.find

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediaplayer.android.data.FindRepository
import com.mediaplayer.android.data.dto.CandidateDto
import com.mediaplayer.android.data.dto.RequestDto
import com.mediaplayer.android.data.dto.RequestStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the Find-new-music tab. One screen has two logical phases:
 *
 * 1. **Compose a query** → submit → backend runs Prowlarr synchronously.
 * 2. **Candidate picker** → tap a row → backend flips to UNLOCKING →
 *    the view model polls `/api/requests/{id}` every [POLL_MS] until
 *    the status is terminal.
 *
 * Both phases live on the same screen so the user never leaves the tab
 * they opened. The picker and the "watching" states are distinguished
 * by [FindUiState] and inspected by the composable.
 */
sealed interface FindUiState {
    /** Idle landing state — empty query, nothing searched yet. */
    data object Idle : FindUiState

    /** Prowlarr search in flight. */
    data object Searching : FindUiState

    /** Prowlarr search failed or backend returned FAILED. */
    data class Error(val message: String) : FindUiState

    /**
     * Request is open on the backend. Covers every non-terminal live
     * state plus the terminal ones so the UI can render a completion
     * message in the same screen.
     */
    data class Tracking(val request: RequestDto) : FindUiState
}

class FindViewModel(
    private val repository: FindRepository = FindRepository(),
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _state = MutableStateFlow<FindUiState>(FindUiState.Idle)
    val state: StateFlow<FindUiState> = _state.asStateFlow()

    /** Polling job for the post-select phase. Cancelled on reset / new search. */
    private var pollJob: Job? = null

    fun onQueryChange(q: String) {
        _query.value = q
    }

    /** Fire-and-forget search. Safe to call multiple times; each call cancels a stale poll. */
    fun submit() {
        val q = _query.value.trim()
        if (q.isEmpty()) return
        pollJob?.cancel()
        _state.value = FindUiState.Searching
        viewModelScope.launch {
            try {
                val created = repository.create(q)
                when (created.status) {
                    RequestStatus.FAILED ->
                        _state.value = FindUiState.Error(
                            created.errorMessage ?: "Search failed on the backend."
                        )
                    else -> _state.value = FindUiState.Tracking(created)
                }
            } catch (t: Throwable) {
                _state.value = FindUiState.Error(t.message ?: "Unknown error")
            }
        }
    }

    /**
     * User tapped a candidate row. Transition backend to UNLOCKING and
     * start polling until the orchestrator reaches a terminal state.
     */
    fun select(candidate: CandidateDto) {
        val current = (_state.value as? FindUiState.Tracking) ?: return
        val requestId = current.request.id
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            try {
                val updated = repository.select(requestId, candidate.id)
                _state.value = FindUiState.Tracking(updated)
                // Poll until terminal. `updated.status` is already UNLOCKING.
                while (true) {
                    delay(POLL_MS)
                    val fresh = repository.detail(requestId)
                    _state.value = FindUiState.Tracking(fresh)
                    if (fresh.status.isTerminal) break
                }
            } catch (t: Throwable) {
                _state.value = FindUiState.Error(t.message ?: "Unknown error")
            }
        }
    }

    /** Drop the current request (if any) and return to the query screen. */
    fun reset() {
        pollJob?.cancel()
        pollJob = null
        _query.value = ""
        _state.value = FindUiState.Idle
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val POLL_MS = 2_000L
    }
}
