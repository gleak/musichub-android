package com.mediaplayer.android.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.data.OnboardingPreferences
import com.mediaplayer.android.data.dto.GenreSeedRequest
import com.mediaplayer.android.ui.common.friendlyMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Owns the M14e tag-picker submission. AuthGate calls [onSeeded] / [onDismissed]
 * to refresh its `SignedIn(user)` state once the picker resolves — both the
 * server-side commit and the local skip flag flow back through the same hook.
 */
class OnboardingViewModel(
    private val onResolved: suspend () -> Unit,
) : ViewModel() {

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    @Suppress("TooGenericExceptionCaught")
    fun submit(genres: List<String>) {
        // compareAndSet keeps the guard atomic with the flip — a fast
        // double-tap on Continua used to slip past `if (_saving.value)`
        // before either call wrote true and fired two seedGenres POSTs.
        if (!_saving.compareAndSet(false, true)) return
        _error.value = null
        viewModelScope.launch {
            try {
                Network.api.seedGenres(GenreSeedRequest(genres))
                onResolved()
            } catch (t: Throwable) {
                _error.value = friendlyMessage(t)
            } finally {
                _saving.value = false
            }
        }
    }

    fun skip() {
        if (!_saving.compareAndSet(false, true)) return
        viewModelScope.launch {
            // try/finally so a DataStore IOException from markDismissed or
            // a future-suspending onResolved can't leave _saving stuck at
            // true — the screen would otherwise stay dimmed forever and
            // the user couldn't even retry the skip.
            try {
                OnboardingPreferences.instance.markDismissed()
                onResolved()
            } catch (t: Throwable) {
                _error.value = friendlyMessage(t)
            } finally {
                _saving.value = false
            }
        }
    }
}
