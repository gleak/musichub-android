package com.mediaplayer.android.widget

import android.content.ComponentName
import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.action.ActionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.mediaplayer.android.playback.MediaPlaybackService
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Builds a transient [MediaController] bound to [MediaPlaybackService],
 * runs [block], then releases. Each widget tap opens its own short-lived
 * connection so the widget never holds a controller between presses —
 * keeps idle CPU/battery cost at zero between interactions.
 *
 * Returns silently if the service binding fails (e.g. service not running
 * because no playback has started yet). Widget UI already gates transport
 * buttons on `WidgetState`, so the user can't reach this path with a stale
 * state in practice.
 */
@UnstableApi
private suspend fun withController(
    context: Context,
    block: (MediaController) -> Unit,
) {
    val token = SessionToken(
        context.applicationContext,
        ComponentName(context.applicationContext, MediaPlaybackService::class.java),
    )
    val controller = suspendCancellableCoroutine<MediaController?> { cont ->
        val future = MediaController.Builder(context.applicationContext, token).buildAsync()
        future.addListener(
            {
                val c = runCatching { future.get() }.getOrNull()
                if (cont.isActive) cont.resume(c)
            },
            MoreExecutors.directExecutor(),
        )
        cont.invokeOnCancellation { future.cancel(true) }
    } ?: return
    try {
        block(controller)
    } finally {
        controller.release()
    }
}

@UnstableApi
class PlayPauseAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        withController(context) { c ->
            if (c.isPlaying) c.pause() else c.play()
        }
    }
}

@UnstableApi
class NextAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        withController(context) { c ->
            if (c.hasNextMediaItem()) c.seekToNextMediaItem()
        }
    }
}

@UnstableApi
class PreviousAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        withController(context) { c -> c.seekToPrevious() }
    }
}
