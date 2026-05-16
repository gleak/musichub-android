package com.mediaplayer.android.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Thin singleton that manages the asynchronous lifecycle of a
 * [MediaController] bound to [MediaPlaybackService].
 *
 * - The controller is obtained asynchronously via [MediaController.Builder].
 * - Exposes `controller` as a `StateFlow<MediaController?>`; consumers can
 *   null-gate against it.
 * - `connect()` is idempotent — safe to call from multiple entry points.
 * - `release()` is called on app shutdown by [com.mediaplayer.android.MediaPlayerApp].
 */
@UnstableApi
object PlayerConnection {

    private val _controller = MutableStateFlow<MediaController?>(null)
    val controller: StateFlow<MediaController?> = _controller.asStateFlow()

    /**
     * Most recent session-level extras bundle published by [MediaPlaybackService]
     * via {@link androidx.media3.session.MediaSession#setSessionExtras}. Used to
     * surface service-owned state (e.g. sleep timer active) to the UI.
     */
    private val _sessionExtras = MutableStateFlow(Bundle.EMPTY)
    val sessionExtras: StateFlow<Bundle> = _sessionExtras.asStateFlow()

    /**
     * Most recent bind failure, or null when the controller is bound or
     * still being bound. UI surfaces this so a tap on Play that would
     * otherwise no-op (because the MediaController never came up) instead
     * shows the user *why* nothing happened.
     */
    private val _bindError = MutableStateFlow<Throwable?>(null)
    val bindError: StateFlow<Throwable?> = _bindError.asStateFlow()

    @Volatile
    private var inFlight: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null

    private val controllerListener = object : MediaController.Listener {
        override fun onExtrasChanged(controller: MediaController, extras: Bundle) {
            _sessionExtras.value = extras
        }
    }

    fun connect(context: Context) {
        if (_controller.value != null || inFlight != null) return

        val token = SessionToken(
            context.applicationContext,
            ComponentName(context.applicationContext, MediaPlaybackService::class.java),
        )
        val future = MediaController.Builder(context.applicationContext, token)
            .setListener(controllerListener)
            .buildAsync()
        inFlight = future
        future.addListener(
            {
                try {
                    val c = future.get()
                    _controller.value = c
                    _sessionExtras.value = c.sessionExtras
                    _bindError.value = null
                } catch (t: Throwable) {
                    // Bind failure — expose so PlaybackViewModel can surface
                    // a dialog instead of silently swallowing every play tap.
                    t.printStackTrace()
                    _bindError.value = t
                } finally {
                    inFlight = null
                }
            },
            MoreExecutors.directExecutor(),
        )
    }

    /** Retry binding after a previous failure. Resets [bindError]. */
    fun retry(context: Context) {
        _bindError.value = null
        connect(context)
    }

    fun release() {
        _controller.value?.release()
        _controller.value = null
        inFlight?.cancel(true)
        inFlight = null
    }
}
