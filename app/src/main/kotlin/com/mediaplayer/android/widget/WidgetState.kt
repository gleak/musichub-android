package com.mediaplayer.android.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.glance.appwidget.updateAll
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-process snapshot of playback state surfaced to home-screen widgets.
 *
 * Written by [com.mediaplayer.android.playback.MediaPlaybackService] every
 * time the current item or play state flips, read by the Glance widgets
 * during render. Cover bitmap is loaded asynchronously and stored here so
 * the widget side never needs to do its own image I/O at render time
 * (Glance's render pass is short — pre-decoded bitmap = instant repaint).
 */
@UnstableApi
data class NowPlayingSnapshot(
    val songId: Long? = null,
    val title: String = "",
    val artist: String = "",
    val isPlaying: Boolean = false,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val coverUri: String? = null,
    val cover: Bitmap? = null,
)

@UnstableApi
object WidgetState {
    private val _now = MutableStateFlow(NowPlayingSnapshot())
    val now: StateFlow<NowPlayingSnapshot> = _now.asStateFlow()

    fun update(snapshot: NowPlayingSnapshot) {
        _now.value = snapshot
    }

    fun clear() {
        _now.value = NowPlayingSnapshot()
    }

    suspend fun refreshNowPlayingWidgets(context: Context) {
        NowPlayingWidget().updateAll(context)
    }
}
