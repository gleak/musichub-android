package com.mediaplayer.android.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Service-owned sleep timer. Two arming modes:
 *
 *  - [set]            — countdown N minutes, fire `onExpire`.
 *  - [setEndOfTrack]  — listen for the next auto/repeat track transition,
 *                       fire `onExpire`. Mirrors the mockup `Fine traccia`
 *                       chip in `mockup/mh-auto-extra.jsx` (audit D15).
 *
 * Either mode flips [isActive]; only the minute mode publishes a
 * non-zero [remainingMs]. End-of-track mode advertises itself via
 * [endOfTrackActive] so the AA cancel chip can label itself
 * `Annulla · fine traccia` instead of `Annulla · N min`.
 */
class SleepTimer(private val scope: CoroutineScope) {

    private var job: Job? = null

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    /**
     * Remaining time in ms while a minute-mode timer is armed, else 0.
     * Updated at minute boundaries so the AA custom-layout countdown
     * label can show "Annulla · N min" without thrashing
     * `setCustomLayout` once per second.
     */
    private val _remainingMs = MutableStateFlow(0L)
    val remainingMs: StateFlow<Long> = _remainingMs.asStateFlow()

    /** True only while the end-of-track mode is armed (no countdown). */
    private val _endOfTrackActive = MutableStateFlow(false)
    val endOfTrackActive: StateFlow<Boolean> = _endOfTrackActive.asStateFlow()

    private var endOfTrackListener: Player.Listener? = null
    private var endOfTrackPlayer: Player? = null

    fun set(minutes: Int, onExpire: () -> Unit) {
        cancel()
        if (minutes <= 0) return
        val totalMs = minutes * 60_000L
        val endAtMs = System.currentTimeMillis() + totalMs
        _isActive.value = true
        _remainingMs.value = totalMs
        job = scope.launch {
            while (true) {
                val left = endAtMs - System.currentTimeMillis()
                if (left <= 0L) {
                    _isActive.value = false
                    _remainingMs.value = 0L
                    onExpire()
                    return@launch
                }
                _remainingMs.value = left
                // Sleep until the next whole-minute boundary so the displayed
                // ceil-minutes label flips exactly when the value rolls over.
                val partial = left % 60_000L
                val nextStep = if (partial == 0L) 60_000L else partial
                delay(nextStep.coerceAtMost(left))
            }
        }
    }

    /**
     * Arm the "stop at end of current track" mode. Fires `onExpire` on
     * the next AUTO or REPEAT transition (player advanced on its own).
     * User-driven skips (next/prev tap) do NOT fire — `Fine traccia`
     * means "the song you started", not "the next user-issued track".
     */
    fun setEndOfTrack(player: Player, onExpire: () -> Unit) {
        cancel()
        _isActive.value = true
        _endOfTrackActive.value = true
        _remainingMs.value = 0L
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                    reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT
                ) {
                    player.removeListener(this)
                    if (endOfTrackListener === this) {
                        endOfTrackListener = null
                        endOfTrackPlayer = null
                    }
                    _isActive.value = false
                    _endOfTrackActive.value = false
                    onExpire()
                }
            }
        }
        endOfTrackListener = listener
        endOfTrackPlayer = player
        player.addListener(listener)
    }

    fun cancel() {
        job?.cancel()
        job = null
        endOfTrackListener?.let { l -> endOfTrackPlayer?.removeListener(l) }
        endOfTrackListener = null
        endOfTrackPlayer = null
        _isActive.value = false
        _endOfTrackActive.value = false
        _remainingMs.value = 0L
    }
}
