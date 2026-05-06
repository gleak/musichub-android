package com.mediaplayer.android.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.common.util.UnstableApi
import com.mediaplayer.android.data.AuthRepository
import com.mediaplayer.android.data.AuthTokenHolder
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.data.PlaylistAutoSyncRunner
import com.mediaplayer.android.data.dto.UserDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    /**
     * Auth state model. Distinguishes the *initial probe* (silent token
     * refresh on app launch — full-screen splash) from credential exchange
     * (white pill spinner over LoginScreen) per `mockup/mh-auth-states.jsx`.
     */
    sealed interface State {
        /** Initial probe — show [AuthProbeScreen]. [stage] feeds the diagnostic line. */
        data class Probe(val stage: ProbeStage) : State

        /** Google credential exchange in flight — keep LoginScreen, swap CTA to spinner. */
        data object SigningIn : State

        data object NotSignedIn : State
        data class SignedIn(val user: UserDto) : State
        data class Error(val message: String, val code: String) : State
    }

    enum class ProbeStage { Token, Me, RejectedSilent }

    /**
     * Maps a raw exception from the Google credential flow or `/api/auth/me`
     * to one of the four mockup-defined error categories. The category drives
     * the mono code shown inside `LoginErrorPanel` (see `mockup/mh-auth.jsx`
     * and `mockup/mh-auth-states.jsx`). Pure string matching — no HTTP /
     * `GetCredentialException` types pulled in to keep the VM untyped.
     */
    private fun classifyAuthError(e: Throwable): String {
        val msg = (e.message ?: "").lowercase()
        return when {
            msg.contains("timeout") ||
                msg.contains("unable to resolve host") ||
                msg.contains("failed to connect") ||
                msg.contains("network") ||
                msg.contains("unreachable") -> "auth/network-error"
            msg.contains("401") || msg.contains("unauthorized") ||
                msg.contains("403") || msg.contains("forbidden") -> "auth/server-rejected"
            msg.contains("credential") || msg.contains("google") ||
                msg.contains("no_credential") || msg.contains("nocredential") -> "auth/google-rejected"
            else -> "auth/unknown"
        }
    }

    private val _state = MutableStateFlow<State>(State.Probe(ProbeStage.Token))
    val state: StateFlow<State> = _state.asStateFlow()

    /**
     * One-shot signal emitted when the user dismisses the Google account
     * picker without choosing an account. LoginScreen collects this to flash
     * the `auth/picker-cancel` soft toast — distinct from a hard `Error`.
     */
    private val _pickerCancelled = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val pickerCancelled: SharedFlow<Unit> = _pickerCancelled.asSharedFlow()

    /**
     * One-shot guard: kick `PlaylistAutoSyncRunner` the first time we reach
     * `SignedIn` in this process. The runner used to fire from
     * `MediaPlayerApp.onCreate`, but the token wasn't set yet so it produced
     * 401s on `/api/playlists`. Tying it to the auth state makes sure the
     * Bearer header is in place before the cold-launch sync hits the wire.
     */
    private var autoSyncTriggered = false

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun triggerAutoSyncOnce() {
        if (autoSyncTriggered) return
        autoSyncTriggered = true
        PlaylistAutoSyncRunner.run()
    }

    init {
        viewModelScope.launch {
            val token = authRepository.tryAutoSignIn()
            if (token != null) {
                AuthTokenHolder.idToken = token
                _state.value = State.Probe(ProbeStage.Me)
                try {
                    _state.value = State.SignedIn(Network.api.getMe())
                    triggerAutoSyncOnce()
                } catch (_: Exception) {
                    // Server rejected the token — flash the rejected-silent
                    // probe stage briefly so the user sees what happened, then
                    // drop back to the picker.
                    AuthTokenHolder.idToken = null
                    _state.value = State.Probe(ProbeStage.RejectedSilent)
                    delay(900)
                    _state.value = State.NotSignedIn
                }
            } else {
                _state.value = State.NotSignedIn
            }
        }
    }

    fun signIn(context: Context) {
        viewModelScope.launch {
            _state.value = State.SigningIn
            try {
                val token = authRepository.signIn(context)
                AuthTokenHolder.idToken = token
                val user = Network.api.getMe()
                _state.value = State.SignedIn(user)
                triggerAutoSyncOnce()
            } catch (e: Exception) {
                val msg = e.message ?: ""
                if (msg.contains("cancel", ignoreCase = true)) {
                    _pickerCancelled.tryEmit(Unit)
                    _state.value = State.NotSignedIn
                } else {
                    _state.value = State.Error(
                        message = msg.ifEmpty { "Accesso non riuscito" },
                        code = classifyAuthError(e),
                    )
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            AuthTokenHolder.idToken = null
            _state.value = State.NotSignedIn
        }
    }

    /**
     * Re-fetches `/api/auth/me` and replaces the current `SignedIn` user
     * in place. Called after onboarding flips `onboardingComplete=true`
     * server-side so the gate routes the user into AppScaffold.
     */
    fun refreshMe() {
        viewModelScope.launch {
            try {
                val user = Network.api.getMe()
                _state.value = State.SignedIn(user)
            } catch (_: Exception) { /* keep current state on transient failure */ }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { AuthViewModel(AuthRepository.instance) }
        }
    }
}
