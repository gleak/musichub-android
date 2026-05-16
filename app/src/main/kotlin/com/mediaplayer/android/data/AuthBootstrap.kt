package com.mediaplayer.android.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
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

    /**
     * Upper bound on how long we let the silent-auth attempt block before
     * unblocking AA browse callbacks. With the new DataStore-backed token
     * persistence the steady-state read is sub-millisecond, but a stuck
     * disk / first-time Credential Manager hop could in theory hang
     * forever. Five seconds is comfortably above any realistic happy path
     * and tight enough that AA shows its "no library yet" fallback rather
     * than spinning indefinitely.
     */
    private const val SILENT_AUTH_TIMEOUT_MS = 5_000L

    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            try {
                if (AuthTokenHolder.idToken == null) {
                    val token = try {
                        withTimeout(SILENT_AUTH_TIMEOUT_MS) {
                            AuthRepository.instance.tryAutoSignIn()
                        }
                    } catch (_: TimeoutCancellationException) {
                        null
                    }
                    token?.let { AuthTokenHolder.idToken = it }
                }
            } finally {
                ready.complete(Unit)
            }
        }
    }
}
