package com.mediaplayer.android.ui.find

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediaplayer.android.data.FindRepository
import com.mediaplayer.android.data.dto.CandidateDto
import com.mediaplayer.android.data.dto.RequestDto
import com.mediaplayer.android.data.dto.RequestStatus
import com.mediaplayer.android.data.dto.RequestSummaryDto
import com.mediaplayer.android.ui.common.friendlyMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface FindUiState {
    data object Idle : FindUiState
    data object Searching : FindUiState
    data class Error(val message: String) : FindUiState
    data class Tracking(val request: RequestDto) : FindUiState
}

class FindViewModel(
    private val repository: FindRepository = FindRepository(),
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _state = MutableStateFlow<FindUiState>(FindUiState.Idle)
    val state: StateFlow<FindUiState> = _state.asStateFlow()

    private val _activeRequests = MutableStateFlow<List<RequestSummaryDto>>(emptyList())
    val activeRequests: StateFlow<List<RequestSummaryDto>> = _activeRequests.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var pollJob: Job? = null
    private var requestsJob: Job? = null

    /**
     * The request currently being polled to terminal status. Held outside
     * the job so [resume] can restart the poll after a pause without losing
     * track of which request the screen was watching.
     */
    private var trackedRequestId: Long? = null

    /**
     * Whether the screen is currently in the foreground. The Find screen
     * calls [resume] on STARTED and [pause] on STOPPED; both poll loops
     * are gated on this so a backgrounded screen doesn't keep waking the
     * CPU and hitting the backend.
     */
    private var screenActive: Boolean = false

    init {
        // Don't start polling until the screen actually becomes visible.
    }

    /**
     * Resume polling. Called by FindScreen on lifecycle STARTED. If a
     * request was being tracked when we paused, restart its poll;
     * otherwise restart the active-requests tracker.
     */
    fun resume() {
        if (screenActive) return
        screenActive = true
        val tracked = trackedRequestId
        if (tracked != null && _state.value is FindUiState.Tracking) {
            startTrackingPoll(tracked)
        } else {
            startRequestsTracking()
        }
    }

    /**
     * Pause polling. Called by FindScreen on lifecycle STOPPED. Cancels
     * both jobs but preserves [trackedRequestId] so [resume] can pick up
     * where we left off.
     */
    fun pause() {
        if (!screenActive) return
        screenActive = false
        pollJob?.cancel()
        pollJob = null
        requestsJob?.cancel()
        requestsJob = null
    }

    fun onQueryChange(q: String) {
        _query.value = q
    }

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
                _state.value = FindUiState.Error(friendlyMessage(t))
            }
        }
    }

    fun select(candidate: CandidateDto) {
        val current = (_state.value as? FindUiState.Tracking) ?: return
        val requestId = current.request.id
        trackedRequestId = requestId
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            try {
                val updated = repository.select(requestId, candidate.id)
                _state.value = FindUiState.Tracking(updated)
            } catch (t: Throwable) {
                _state.value = FindUiState.Error(friendlyMessage(t))
                return@launch
            }
            runTrackingLoop(requestId)
        }
    }

    fun reset() {
        pollJob?.cancel()
        pollJob = null
        trackedRequestId = null
        _state.value = FindUiState.Idle
        startRequestsTracking()
    }

    fun refreshActiveRequests() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                _activeRequests.value = repository.list().filter { !it.status.isTerminal }
            } catch (_: Throwable) {}
            _isRefreshing.value = false
        }
        startRequestsTracking()
    }

    private fun startTrackingPoll(requestId: Long) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch { runTrackingLoop(requestId) }
    }

    private suspend fun runTrackingLoop(requestId: Long) {
        try {
            var backoff = POLL_MS
            while (true) {
                delay(backoff)
                val fresh = repository.detail(requestId)
                _state.value = FindUiState.Tracking(fresh)
                if (fresh.status.isTerminal) {
                    delay(TERMINAL_LINGER_MS)
                    _state.value = FindUiState.Idle
                    trackedRequestId = null
                    startRequestsTracking()
                    break
                }
                backoff = (backoff * 2).coerceAtMost(MAX_POLL_MS)
            }
        } catch (t: Throwable) {
            _state.value = FindUiState.Error(friendlyMessage(t))
        }
    }

    private fun startRequestsTracking() {
        if (!screenActive) return
        requestsJob?.cancel()
        requestsJob = viewModelScope.launch {
            try {
                var backoff = POLL_MS
                while (true) {
                    val fresh = repository.list().filter { !it.status.isTerminal }
                    _activeRequests.value = fresh
                    if (fresh.isEmpty()) break
                    delay(backoff)
                    backoff = (backoff * 2).coerceAtMost(MAX_POLL_MS)
                }
            } catch (_: Throwable) {
                // Best-effort — silently stop polling on network error.
            }
        }
    }

    override fun onCleared() {
        pollJob?.cancel()
        requestsJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val POLL_MS = 2_000L
        const val MAX_POLL_MS = 10_000L
        const val TERMINAL_LINGER_MS = 2_000L
    }
}
