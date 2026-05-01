package com.mediaplayer.android.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mediaplayer.android.data.AuthRepository
import com.mediaplayer.android.data.AuthTokenHolder
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.data.dto.UserDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    sealed interface State {
        data object Loading : State
        data object NotSignedIn : State
        data class SignedIn(val user: UserDto) : State
        data class Error(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val token = authRepository.tryAutoSignIn()
            if (token != null) {
                AuthTokenHolder.idToken = token
                try {
                    _state.value = State.SignedIn(Network.api.getMe())
                } catch (_: Exception) {
                    // Server rejected the token — drop it and let the user pick again
                    // (sign in or continue as guest) rather than entering a half-signed-in state.
                    AuthTokenHolder.idToken = null
                    _state.value = State.NotSignedIn
                }
            } else {
                _state.value = State.NotSignedIn
            }
        }
    }

    fun signIn(context: Context) {
        viewModelScope.launch {
            _state.value = State.Loading
            try {
                val token = authRepository.signIn(context)
                AuthTokenHolder.idToken = token
                val user = Network.api.getMe()
                _state.value = State.SignedIn(user)
            } catch (e: Exception) {
                val msg = e.message ?: ""
                if (msg.contains("cancel", ignoreCase = true) ||
                    msg.contains("Cancel", ignoreCase = true)
                ) {
                    _state.value = State.NotSignedIn
                } else {
                    _state.value = State.Error(msg.ifEmpty { "Sign-in failed" })
                }
            }
        }
    }

    /**
     * Enters the app as an anonymous guest. No Google sign-in dialog — the network
     * layer's persistent {@code X-Anonymous-Id} header carries the device identity,
     * and the backend creates (or returns) the corresponding anonymous user row.
     */
    fun signInAnonymously() {
        viewModelScope.launch {
            _state.value = State.Loading
            AuthTokenHolder.idToken = null
            try {
                val user = Network.api.getMe()
                _state.value = State.SignedIn(user)
            } catch (e: Exception) {
                _state.value = State.Error(
                    e.message?.takeIf { it.isNotBlank() } ?: "Couldn't reach the server"
                )
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
