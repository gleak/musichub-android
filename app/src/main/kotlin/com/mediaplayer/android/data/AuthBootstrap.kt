package com.mediaplayer.android.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
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
     *
     * Private on purpose: awaiting it directly is unbounded, and every
     * caller sits on a path (AA browse callbacks, the offline queue
     * drainer) where hanging is worse than proceeding token-less. Go
     * through [awaitReady].
     */
    private val ready: CompletableDeferred<Unit> = CompletableDeferred()

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

    /**
     * Upper bound on the gate itself, one second above the silent-auth
     * budget so a normal attempt is never cut short. This is the backstop
     * for the case the inner timeout can't cover: [start] never running at
     * all, because something threw in `MediaPlayerApp.onCreate` ahead of
     * it. The deferred would then stay uncompleted for the life of the
     * process and every awaiter would block forever — a blank Android Auto
     * library with no spinner and no error, and an offline queue that never
     * drains. Bounded waiting turns that into a token-less request, which
     * fails visibly and recovers on the next call.
     */
    private const val GATE_TIMEOUT_MS = 6_000L

    /**
     * Wait for silent auth to settle before making an authenticated call.
     *
     * Returns true when the attempt finished (whether or not it produced a
     * token) and false when the gate timed out, in which case the caller
     * should proceed anyway rather than stall.
     *
     * Arms [start] first: the gate is only meaningful if something is
     * driving it, and making the first awaiter do that removes the
     * ordering dependency on `MediaPlayerApp.onCreate` entirely.
     */
    suspend fun awaitReady(): Boolean {
        start()
        return withTimeoutOrNull(GATE_TIMEOUT_MS) { ready.await() } != null
    }

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
