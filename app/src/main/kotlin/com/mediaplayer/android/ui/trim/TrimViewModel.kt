package com.mediaplayer.android.ui.trim

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.data.dto.CutSongRequest
import com.mediaplayer.android.data.dto.ReplaceSongRequest
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.ui.common.friendlyMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * State for the full-screen trim editor. Owns the IN / OUT cursors, the
 * trim card pills (fade / snap-to-silence / A/B preview), the long-press
 * ×8 zoom state, and the two-step Save → "replace in playlists?" flow.
 *
 * The screen drives playback through the host's [com.mediaplayer.android.playback.PlaybackViewModel] —
 * scrubbing, jump-to-IN/OUT, and ±5s nudges all map onto the existing
 * MediaController so we don't run a second ExoPlayer instance just for the
 * editor preview.
 */
sealed interface TrimSaveState {
    data object Idle : TrimSaveState
    data object Saving : TrimSaveState
    /** Backend cut master is in. Screen pops the "replace in playlists?" toast. */
    data class Saved(val newSong: SongDto) : TrimSaveState
    /** User picked Sì in the saved toast — bulk swap is in flight. */
    data object Replacing : TrimSaveState
    /** Bulk swap finished; [updated] is the rows touched. */
    data class Replaced(val newSong: SongDto, val updated: Int) : TrimSaveState
    data class Failed(val message: String) : TrimSaveState
}

/** Which handle (if any) is currently in long-press ×8 zoom mode. */
enum class ZoomTarget { NONE, IN, OUT }

class TrimViewModel(
    private val sourceSongId: Long,
    private val totalDurationMs: Long,
) : ViewModel() {

    private val _inMs = MutableStateFlow(DEFAULT_IN_MARGIN_MS.coerceAtMost(totalDurationMs / 4))
    val inMs: StateFlow<Long> = _inMs.asStateFlow()

    private val _outMs = MutableStateFlow(
        (totalDurationMs - DEFAULT_OUT_MARGIN_MS).coerceAtLeast(_inMs.value + MIN_WINDOW_MS)
    )
    val outMs: StateFlow<Long> = _outMs.asStateFlow()

    private val _fadeEnabled = MutableStateFlow(true)
    val fadeEnabled: StateFlow<Boolean> = _fadeEnabled.asStateFlow()

    /** A/B preview: when true, the screen seeks back to IN whenever playhead reaches OUT. */
    private val _abLoopEnabled = MutableStateFlow(false)
    val abLoopEnabled: StateFlow<Boolean> = _abLoopEnabled.asStateFlow()

    private val _zoomTarget = MutableStateFlow(ZoomTarget.NONE)
    val zoomTarget: StateFlow<ZoomTarget> = _zoomTarget.asStateFlow()

    private val _saveState = MutableStateFlow<TrimSaveState>(TrimSaveState.Idle)
    val saveState: StateFlow<TrimSaveState> = _saveState.asStateFlow()

    /**
     * Real PCM-derived peaks. Null until [WaveformAnalyzer] returns; the
     * screen falls back to its synthetic curve in the meantime so the editor
     * is usable while decode runs. Flips once with the real array.
     */
    private val _peaks = MutableStateFlow<FloatArray?>(null)
    val peaks: StateFlow<FloatArray?> = _peaks.asStateFlow()

    init {
        viewModelScope.launch {
            val real = withContext(Dispatchers.IO) {
                WaveformAnalyzer.analyze(sourceSongId)
            }
            if (real != null) _peaks.value = real
        }
    }

    fun setIn(ms: Long) {
        val clamped = ms.coerceIn(0L, _outMs.value - MIN_WINDOW_MS)
        _inMs.value = clamped
    }

    fun setOut(ms: Long) {
        val clamped = ms.coerceIn(_inMs.value + MIN_WINDOW_MS, totalDurationMs)
        _outMs.value = clamped
    }

    fun nudgeIn(deltaMs: Long) = setIn(_inMs.value + deltaMs)
    fun nudgeOut(deltaMs: Long) = setOut(_outMs.value + deltaMs)

    fun toggleFade() { _fadeEnabled.value = !_fadeEnabled.value }
    fun toggleAbLoop() { _abLoopEnabled.value = !_abLoopEnabled.value }

    fun setZoom(target: ZoomTarget) { _zoomTarget.value = target }

    /**
     * Snap IN and OUT to the nearest local minimum in [waveform]. Heuristic
     * silence detector — the waveform is the same one the canvas paints, so
     * the pill jumps are visually consistent with what the user sees. Picks
     * the closest valley within ±[SNAP_RADIUS_MS] of each handle.
     */
    fun snapToSilence(waveform: FloatArray) {
        if (waveform.isEmpty() || totalDurationMs <= 0L) return
        val newIn = nearestValleyMs(waveform, _inMs.value)
        val newOut = nearestValleyMs(waveform, _outMs.value)
        // Apply with the existing clamps so the order constraint holds.
        if (newIn < _outMs.value - MIN_WINDOW_MS) setIn(newIn)
        if (newOut > _inMs.value + MIN_WINDOW_MS) setOut(newOut)
    }

    private fun nearestValleyMs(waveform: FloatArray, anchorMs: Long): Long {
        val anchorBar = ((anchorMs.toFloat() / totalDurationMs) * waveform.size).toInt()
            .coerceIn(0, waveform.size - 1)
        val radiusBars = ((SNAP_RADIUS_MS.toFloat() / totalDurationMs) * waveform.size)
            .toInt().coerceAtLeast(1)
        val from = (anchorBar - radiusBars).coerceAtLeast(0)
        val to = (anchorBar + radiusBars).coerceAtMost(waveform.size - 1)
        var bestIdx = anchorBar
        var bestVal = waveform[anchorBar]
        for (i in from..to) {
            // Local minima win; ties resolved toward the anchor for stability.
            if (waveform[i] < bestVal ||
                (waveform[i] == bestVal && kotlin.math.abs(i - anchorBar) < kotlin.math.abs(bestIdx - anchorBar))) {
                bestVal = waveform[i]
                bestIdx = i
            }
        }
        return ((bestIdx + 0.5f) / waveform.size * totalDurationMs).toLong()
    }

    fun consumeSaveState() {
        _saveState.value = TrimSaveState.Idle
    }

    fun save() {
        if (_saveState.value is TrimSaveState.Saving) return
        val inMs = _inMs.value
        val outMs = _outMs.value
        if (outMs - inMs < MIN_WINDOW_MS) {
            _saveState.value = TrimSaveState.Failed("Finestra troppo corta")
            return
        }
        _saveState.value = TrimSaveState.Saving
        viewModelScope.launch {
            try {
                val newSong = Network.api.cutSong(
                    id = sourceSongId,
                    body = CutSongRequest(
                        inMs = inMs,
                        outMs = outMs,
                        fadeEnabled = _fadeEnabled.value,
                    ),
                )
                _saveState.value = TrimSaveState.Saved(newSong)
            } catch (t: Throwable) {
                _saveState.value = TrimSaveState.Failed(friendlyMessage(t))
            }
        }
    }

    /**
     * User answered "Sì" to "sostituirà l'originale nelle playlist?". Fires
     * the bulk swap on the backend; on success transitions to [TrimSaveState.Replaced]
     * which the screen turns into a final toast.
     */
    fun replaceOriginalInPlaylists() {
        val saved = _saveState.value as? TrimSaveState.Saved ?: return
        _saveState.value = TrimSaveState.Replacing
        viewModelScope.launch {
            try {
                val resp = Network.api.replaceSongInPlaylists(
                    ReplaceSongRequest(oldSongId = sourceSongId, newSongId = saved.newSong.id),
                )
                _saveState.value = TrimSaveState.Replaced(saved.newSong, resp.updated)
            } catch (t: Throwable) {
                _saveState.value = TrimSaveState.Failed(friendlyMessage(t))
            }
        }
    }

    private companion object {
        // Default trim picks a sensible 90 % window so the user lands inside
        // an editable region instead of having to drag both handles inward.
        const val DEFAULT_IN_MARGIN_MS = 5_000L
        const val DEFAULT_OUT_MARGIN_MS = 5_000L
        const val MIN_WINDOW_MS = 1_000L
        // Snap-to-silence search radius. Wide enough to land on a real valley
        // even with a coarse 96-bar waveform; narrow enough that a button tap
        // doesn't yank the handle into a different musical phrase.
        const val SNAP_RADIUS_MS = 4_000L
    }
}
