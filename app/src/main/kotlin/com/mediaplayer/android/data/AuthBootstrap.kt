package com.mediaplayer.android.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Process-wide silent-auth gate.
 *
 * Android Auto can cold-start the [com.mediaplayer.android.playback.MediaPlaybackService]
 * without ever launching `MainActivity`. The phone-side auth UI (where
 * `AuthTokenHolder.idToken` is normally populated by `AuthViewModel.init`)
 * never runs in that path, so browse-tree calls fired off `serviceScope.future`
 * hit the backend without a Bearer token and get 401-ed — AA shows an empty
 * library on the head unit.
 *
 * This object kicks the same `tryAutoSignIn` call AuthViewModel uses, but
 * from `MediaPlayerApp.onCreate`, so the token is loaded for any process
 * entry point. Browse callbacks in [com.mediaplayer.android.playback.LibraryTree]
 * `await()` [ready] before hitting the network.
 *
 * Safe to call from both AA cold-start and phone cold-start: [start] is
 * idempotent (AtomicBoolean guard), and AuthViewModel's own `tryAutoSignIn`
 * stays in place for the UI-state machine — both paths writing the same
 * token to [AuthTokenHolder] is benign.
 */
object AuthBootstrap {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val started = AtomicBoolean(false)

    /**
     * Completes once the silent auth attempt has finished — token is set
     * if the user is already linked to the device, or left null if there
     * is no remembered account. Either way the deferred completes so
     * awaiters don't hang forever; the network call will simply 401 and
     * the UI will surface NotSignedIn the next time AuthViewModel runs.
     */
    val ready: CompletableDeferred<Unit> = CompletableDeferred()

    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            try {
                if (AuthTokenHolder.idToken == null) {
                    AuthRepository.instance.tryAutoSignIn()?.let { token ->
                        AuthTokenHolder.idToken = token
                    }
                }
            } finally {
                ready.complete(Unit)
            }
        }
    }
}
