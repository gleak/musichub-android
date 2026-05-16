package com.mediaplayer.android.ui.trim

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.playback.PlaybackViewModel
import com.mediaplayer.android.ui.common.EyebrowText
import com.mediaplayer.android.ui.common.SongCover
import com.mediaplayer.android.ui.theme.LocalMHMono
import com.mediaplayer.android.ui.theme.MHColors
import com.mediaplayer.android.ui.theme.MHGradient
import com.mediaplayer.android.ui.theme.MediaPlayerSpacing
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.random.Random

private val Playhead = Color(0xFFFFC857)

/**
 * Full-screen ringtone-trim editor. Implements `mockup/mh-trim.jsx`:
 * scrub-libero preview card on top, IN/OUT trim card with handles + nudge,
 * fade-in/out pill, result summary, and `Salva` action that POSTs to the
 * backend cut endpoint.
 *
 * Preview audio uses the host [PlaybackViewModel] — the song the user wants
 * to trim is already playing, and reusing the running MediaController keeps
 * the editor in sync with the lockscreen / Auto / mini-player surfaces.
 */
@OptIn(UnstableApi::class)
@Composable
fun TrimScreen(
    song: SongDto,
    playbackVm: PlaybackViewModel,
    onClose: () -> Unit,
    onSaved: (SongDto) -> Unit,
) {
    val totalMs = song.durationMs.coerceAtLeast(1_000L)
    val vm: TrimViewModel = viewModel(
        key = "trim:${song.id}",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                TrimViewModel(sourceSongId = song.id, totalDurationMs = totalMs) as T
        },
    )
    val inMs by vm.inMs.collectAsStateWithLifecycle()
    val outMs by vm.outMs.collectAsStateWithLifecycle()
    val saveState by vm.saveState.collectAsStateWithLifecycle()
    val fadeOn by vm.fadeEnabled.collectAsStateWithLifecycle()
    val abLoopOn by vm.abLoopEnabled.collectAsStateWithLifecycle()
    val zoomTarget by vm.zoomTarget.collectAsStateWithLifecycle()
    val isPlaying by playbackVm.isPlaying.collectAsStateWithLifecycle()
    val playhead by playbackVm.positionMs.collectAsStateWithLifecycle()
    val realPeaks by vm.peaks.collectAsStateWithLifecycle()

    // Synthetic baseline so the editor is usable while WaveformAnalyzer is
    // decoding the audio in the background. Stable per song id so the bars
    // don't shimmer on recomposition. Real peaks replace this once available.
    val syntheticWaveform = remember(song.id) {
        generateWaveform(seed = (song.id and 0x7FFFFFFF).toInt().coerceAtLeast(1), bars = WaveformAnalyzer.BARS)
    }
    val waveform = realPeaks ?: syntheticWaveform

    val statusInsetTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navInsetBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // A/B preview: when enabled, seek back to IN once the playhead crosses OUT.
    // The 200ms window guard avoids a tight loop when OUT is dragged just past
    // the playhead while paused.
    LaunchedEffect(playhead, abLoopOn, outMs, inMs) {
        if (abLoopOn && playhead >= outMs && outMs - inMs > 200L) {
            playbackVm.seekTo(inMs)
        }
    }

    // Saved (cut master is in) → render the "sostituirà l'originale?" toast.
    // Replaced + Failed are terminal — leave it to the user to dismiss. The
    // No path on the Saved toast also calls onSaved directly, so the editor
    // exits there without going through Replacing/Replaced.
    LaunchedEffect(saveState) {
        val s = saveState
        if (s is TrimSaveState.Replaced) onSaved(s.newSong)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MHGradient.heroBg(top = Color(0xFF142A2A))),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = statusInsetTop, bottom = navInsetBottom),
        ) {
            TopBar(
                saving = saveState is TrimSaveState.Saving,
                onClose = onClose,
                onSave = vm::save,
            )
            TrackHeader(song = song, totalMs = totalMs)
            Spacer(Modifier.height(MediaPlayerSpacing.M))

            // === 01 · ASCOLTO ===
            EyebrowText(
                text = "01 · Ascolto · scrub libero",
                color = MHColors.TextLo,
                modifier = Modifier.padding(horizontal = MediaPlayerSpacing.M),
            )
            Spacer(Modifier.height(MediaPlayerSpacing.S))
            PreviewCard(
                waveform = waveform,
                totalMs = totalMs,
                inMs = inMs,
                outMs = outMs,
                playhead = playhead.coerceIn(0L, totalMs),
                isPlaying = isPlaying,
                onScrubTo = playbackVm::seekTo,
                onJumpIn = { playbackVm.seekTo(inMs) },
                onJumpOut = { playbackVm.seekTo((outMs - 200).coerceAtLeast(0L)) },
                onTogglePlay = playbackVm::togglePlayPause,
                onNudgeBack = { playbackVm.seekTo((playhead - 5_000L).coerceIn(0L, totalMs)) },
                onNudgeForward = { playbackVm.seekTo((playhead + 5_000L).coerceIn(0L, totalMs)) },
            )

            Spacer(Modifier.height(MediaPlayerSpacing.M))

            // === 02 · TAGLIO ===
            EyebrowText(
                text = "02 · Taglio · sposta i punti IN / OUT",
                color = MHColors.TextLo,
                modifier = Modifier.padding(horizontal = MediaPlayerSpacing.M),
            )
            Spacer(Modifier.height(MediaPlayerSpacing.S))
            TrimCard(
                waveform = waveform,
                totalMs = totalMs,
                inMs = inMs,
                outMs = outMs,
                fadeEnabled = fadeOn,
                abLoopEnabled = abLoopOn,
                zoomTarget = zoomTarget,
                onSetIn = vm::setIn,
                onSetOut = vm::setOut,
                onNudgeIn = vm::nudgeIn,
                onNudgeOut = vm::nudgeOut,
                onToggleFade = vm::toggleFade,
                onToggleAb = vm::toggleAbLoop,
                onSnapToSilence = { vm.snapToSilence(waveform) },
                onZoomChange = vm::setZoom,
            )

            Spacer(Modifier.height(MediaPlayerSpacing.M))
            ResultCard(totalMs = totalMs, inMs = inMs, outMs = outMs)

            Spacer(Modifier.height(MediaPlayerSpacing.L))
            Spacer(Modifier.weight(1f, fill = true))

            BottomHint()
        }

        when (val s = saveState) {
            is TrimSaveState.Saved -> SavedToast(
                title = s.newSong.title,
                replacing = false,
                onYes = vm::replaceOriginalInPlaylists,
                onNo = { onSaved(s.newSong) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = MediaPlayerSpacing.M)
                    .padding(bottom = navInsetBottom + 36.dp),
            )
            is TrimSaveState.Replacing -> SavedToast(
                title = "Sostituzione…",
                replacing = true,
                onYes = {},
                onNo = {},
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = MediaPlayerSpacing.M)
                    .padding(bottom = navInsetBottom + 36.dp),
            )
            is TrimSaveState.Failed -> ErrorToast(
                message = s.message,
                onDismiss = vm::consumeSaveState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = MediaPlayerSpacing.M)
                    .padding(bottom = navInsetBottom + 36.dp),
            )
            else -> Unit
        }
    }
}

@Composable
private fun TopBar(
    saving: Boolean,
    onClose: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MediaPlayerSpacing.M, vertical = MediaPlayerSpacing.S),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Chiudi",
                tint = MHColors.TextHi,
            )
        }
        EyebrowText(text = "Modalità · Taglio", color = MHColors.Lime)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(if (saving) MHColors.LimeDim else MHColors.Lime)
                .clickable(enabled = !saving, onClick = onSave)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (saving) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF0A0A0A),
                    )
                    Text(
                        text = "Salvataggio…",
                        color = Color(0xFF0A0A0A),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            } else {
                Text(
                    text = "Salva",
                    color = Color(0xFF0A0A0A),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun TrackHeader(song: SongDto, totalMs: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MediaPlayerSpacing.M, vertical = MediaPlayerSpacing.S),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SongCover(song = song, size = 60.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = MHColors.TextHi,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
            )
            val subtitle = listOfNotNull(song.artist.takeIf { it.isNotBlank() }, song.album).joinToString(" · ")
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    color = MHColors.TextLo,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                text = "${formatTime(totalMs)} · originale",
                color = MHColors.TextLo2,
                style = LocalMHMono.current.duration,
            )
        }
    }
}

@Composable
private fun PreviewCard(
    waveform: FloatArray,
    totalMs: Long,
    inMs: Long,
    outMs: Long,
    playhead: Long,
    isPlaying: Boolean,
    onScrubTo: (Long) -> Unit,
    onJumpIn: () -> Unit,
    onJumpOut: () -> Unit,
    onTogglePlay: () -> Unit,
    onNudgeBack: () -> Unit,
    onNudgeForward: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = MediaPlayerSpacing.M)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MHColors.Card)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        WaveformWithPlayhead(
            waveform = waveform,
            totalMs = totalMs,
            inMs = inMs,
            outMs = outMs,
            playhead = playhead,
            onScrubTo = onScrubTo,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "00:00.0",
                color = MHColors.TextLo,
                style = LocalMHMono.current.duration,
            )
            PlayheadChip(timeMs = playhead)
            Text(
                text = formatTime(totalMs),
                color = MHColors.TextLo,
                style = LocalMHMono.current.duration,
            )
        }
        Spacer(Modifier.height(12.dp))
        TransportRow(
            isPlaying = isPlaying,
            onJumpIn = onJumpIn,
            onJumpOut = onJumpOut,
            onTogglePlay = onTogglePlay,
            onNudgeBack = onNudgeBack,
            onNudgeForward = onNudgeForward,
        )
    }
}

@Composable
private fun WaveformWithPlayhead(
    waveform: FloatArray,
    totalMs: Long,
    inMs: Long,
    outMs: Long,
    playhead: Long,
    onScrubTo: (Long) -> Unit,
) {
    val height = 72.dp
    var sizePx by remember { mutableStateOf(IntSize.Zero) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .onSizeChanged { sizePx = it }
            .pointerInput(totalMs) {
                detectDragGestures(
                    onDragStart = { off: Offset -> emitScrub(off.x, sizePx.width, totalMs, onScrubTo) },
                    onDragEnd = {},
                    onDrag = { change: PointerInputChange, _: Offset ->
                        emitScrub(change.position.x, sizePx.width, totalMs, onScrubTo)
                        change.consume()
                    },
                )
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val barCount = waveform.size
            val barW = w / barCount
            val gap = barW * 0.18f
            val inFrac = (inMs.toFloat() / totalMs).coerceIn(0f, 1f)
            val outFrac = (outMs.toFloat() / totalMs).coerceIn(0f, 1f)
            for (i in 0 until barCount) {
                val v = waveform[i]
                val barH = (v * h).coerceAtLeast(2f)
                val x = i * barW
                val frac = (i + 0.5f) / barCount
                val inMask = frac in inFrac..outFrac
                val color = if (inMask) MHColors.TextHi else Color(0x2EFFFFFF)
                drawRect(
                    color = color,
                    topLeft = Offset(x + gap / 2, (h - barH) / 2),
                    size = androidx.compose.ui.geometry.Size(barW - gap, barH),
                )
            }
            // Dashed in/out region overlay
            val regionLeft = w * inFrac
            val regionRight = w * outFrac
            val regionTop = -4f
            val regionBottom = h + 4f
            drawRect(
                color = Color(0x0FA8E04E),
                topLeft = Offset(regionLeft, regionTop),
                size = androidx.compose.ui.geometry.Size(regionRight - regionLeft, regionBottom - regionTop),
            )
            val dashed = Stroke(
                width = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
            )
            drawLine(
                color = Color(0x66A8E04E),
                start = Offset(regionLeft, regionTop),
                end = Offset(regionLeft, regionBottom),
                strokeWidth = dashed.width,
                pathEffect = dashed.pathEffect,
            )
            drawLine(
                color = Color(0x66A8E04E),
                start = Offset(regionRight, regionTop),
                end = Offset(regionRight, regionBottom),
                strokeWidth = dashed.width,
                pathEffect = dashed.pathEffect,
            )
            // Playhead
            val headFrac = (playhead.toFloat() / totalMs).coerceIn(0f, 1f)
            val headX = w * headFrac
            drawRect(
                color = Playhead,
                topLeft = Offset(headX - 1f, -8f),
                size = androidx.compose.ui.geometry.Size(2f, h + 16f),
            )
            drawRect(
                color = Playhead,
                topLeft = Offset(headX - 7f, -14f),
                size = androidx.compose.ui.geometry.Size(14f, 10f),
            )
        }
    }
}

private fun emitScrub(xPx: Float, widthPx: Int, totalMs: Long, onScrubTo: (Long) -> Unit) {
    if (widthPx <= 0) return
    val frac = (xPx / widthPx).coerceIn(0f, 1f)
    onScrubTo((frac * totalMs).toLong())
}

@Composable
private fun PlayheadChip(timeMs: Long) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0x1FFFC857))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Playhead),
        )
        Text(
            text = formatTime(timeMs),
            color = Playhead,
            style = LocalMHMono.current.duration,
        )
    }
}

@Composable
private fun TransportRow(
    isPlaying: Boolean,
    onJumpIn: () -> Unit,
    onJumpOut: () -> Unit,
    onTogglePlay: () -> Unit,
    onNudgeBack: () -> Unit,
    onNudgeForward: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TransportSmallButton(label = "−5s", onClick = onNudgeBack) {
            Text("−5", color = MHColors.TextHi, style = LocalMHMono.current.duration)
        }
        TransportSmallButton(label = "Vai a IN", onClick = onJumpIn) {
            Text("⇤", color = MHColors.Lime, fontSize = 18.sp)
        }
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (isPlaying) Playhead else MHColors.Lime)
                .clickable(onClick = onTogglePlay),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pausa" else "Riproduci",
                tint = Color(0xFF0A0A0A),
                modifier = Modifier.size(24.dp),
            )
        }
        TransportSmallButton(label = "Vai a OUT", onClick = onJumpOut) {
            Text("⇥", color = MHColors.Lime, fontSize = 18.sp)
        }
        TransportSmallButton(label = "+5s", onClick = onNudgeForward) {
            Text("+5", color = MHColors.TextHi, style = LocalMHMono.current.duration)
        }
    }
}

@Composable
private fun TransportSmallButton(
    label: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0x10FFFFFF))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { content() }
        Text(
            text = label,
            color = MHColors.TextLo2,
            style = LocalMHMono.current.eyebrow.copy(letterSpacing = 0.5.sp, fontSize = 9.sp),
        )
    }
}

@Composable
private fun TrimCard(
    waveform: FloatArray,
    totalMs: Long,
    inMs: Long,
    outMs: Long,
    fadeEnabled: Boolean,
    abLoopEnabled: Boolean,
    zoomTarget: ZoomTarget,
    onSetIn: (Long) -> Unit,
    onSetOut: (Long) -> Unit,
    onNudgeIn: (Long) -> Unit,
    onNudgeOut: (Long) -> Unit,
    onToggleFade: () -> Unit,
    onToggleAb: () -> Unit,
    onSnapToSilence: () -> Unit,
    onZoomChange: (ZoomTarget) -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = MediaPlayerSpacing.M)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MHColors.Card)
            .border(1.dp, Color(0x2EA8E04E), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        TrimTrack(
            waveform = waveform,
            totalMs = totalMs,
            inMs = inMs,
            outMs = outMs,
            zoomTarget = zoomTarget,
            onSetIn = onSetIn,
            onSetOut = onSetOut,
            onZoomChange = onZoomChange,
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NudgeBox(
                label = "IN",
                timeMs = inMs,
                onNudge = onNudgeIn,
                modifier = Modifier.weight(1f),
            )
            NudgeBox(
                label = "OUT",
                timeMs = outMs,
                onNudge = onNudgeOut,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        // Three quick-action pills — match `mh-trim.jsx:150-154` order:
        // silence-snap, fade in/out, A/B preview.
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuickPill(label = "Aggancia al silenzio", icon = "≣", active = false, onClick = onSnapToSilence)
            QuickPill(label = "Fade in/out", icon = "△", active = fadeEnabled, onClick = onToggleFade)
            QuickPill(label = "Anteprima A/B", icon = "↻", active = abLoopEnabled, onClick = onToggleAb)
        }
    }
}

@Composable
private fun TrimTrack(
    waveform: FloatArray,
    totalMs: Long,
    inMs: Long,
    outMs: Long,
    zoomTarget: ZoomTarget,
    onSetIn: (Long) -> Unit,
    onSetOut: (Long) -> Unit,
    onZoomChange: (ZoomTarget) -> Unit,
) {
    val density = LocalDensity.current
    var sizePx by remember { mutableStateOf(IntSize.Zero) }
    var activeHandle by remember { mutableStateOf<HandleSide?>(null) }

    // ZOOM_FACTOR ×8 — when a handle is in zoom mode, the visible window
    // shrinks to 1/8 of the timeline, centered on that handle. Drag math
    // maps the local x back to a timeline ms within the windowed range.
    val zoomFactor = 8f
    val isZoomed = zoomTarget != ZoomTarget.NONE
    val anchorMs = when (zoomTarget) {
        ZoomTarget.IN -> inMs
        ZoomTarget.OUT -> outMs
        ZoomTarget.NONE -> 0L
    }
    val visibleSpan = if (isZoomed) (totalMs / zoomFactor).toLong().coerceAtLeast(500L) else totalMs
    val visibleStart = if (isZoomed) {
        (anchorMs - visibleSpan / 2).coerceIn(0L, (totalMs - visibleSpan).coerceAtLeast(0L))
    } else 0L
    val visibleEnd = visibleStart + visibleSpan

    // Map a timeline ms to a [0..1] fraction of the rendered track.
    fun toFrac(ms: Long): Float =
        ((ms - visibleStart).toFloat() / visibleSpan).coerceIn(0f, 1f)
    // Inverse: x in [0..1] within the rendered track → timeline ms.
    fun fromFrac(frac: Float): Long =
        (visibleStart + (frac.coerceIn(0f, 1f) * visibleSpan)).toLong()

    // Closest-handle picker shared between the long-press detector (zoom toggle)
    // and the drag detector (continuous handle move). Stays in sync because both
    // gesture pipelines key off the same down position.
    val pickHandle: (Float) -> HandleSide = { xPx ->
        val w = sizePx.width.takeIf { it > 0 } ?: 1
        val frac = (xPx / w).coerceIn(0f, 1f)
        val targetMs = fromFrac(frac)
        val toIn = kotlin.math.abs(targetMs - inMs)
        val toOut = kotlin.math.abs(targetMs - outMs)
        // Strict `<` so an exact tie (touch lands on the midpoint between IN
        // and OUT, or both handles overlap during a transient drag) resolves
        // to OUT instead of always to IN. The previous `<=` made the OUT
        // handle ungrabbable at the midpoint.
        if (toIn < toOut) HandleSide.IN else HandleSide.OUT
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .onSizeChanged { sizePx = it }
            // Long-press toggles ×8 zoom on the closest handle. Plain tap
            // outside zoom mode is a no-op; tap while zoomed releases zoom.
            .pointerInput(totalMs, isZoomed, visibleStart, inMs, outMs) {
                detectTapGestures(
                    onLongPress = { off ->
                        val side = pickHandle(off.x)
                        onZoomChange(if (side == HandleSide.IN) ZoomTarget.IN else ZoomTarget.OUT)
                    },
                    onTap = {
                        if (isZoomed) onZoomChange(ZoomTarget.NONE)
                    },
                )
            }
            // Drag picks the closest handle on press-down and stays with it
            // for the rest of the gesture so handles don't hop.
            .pointerInput(totalMs, isZoomed, visibleStart) {
                detectDragGestures(
                    onDragStart = { off ->
                        val side = pickHandle(off.x)
                        activeHandle = side
                        val w = sizePx.width.takeIf { it > 0 } ?: return@detectDragGestures
                        val frac = (off.x / w).coerceIn(0f, 1f)
                        val ms = fromFrac(frac)
                        when (side) {
                            HandleSide.IN -> onSetIn(ms)
                            HandleSide.OUT -> onSetOut(ms)
                        }
                    },
                    onDrag = { change, _ ->
                        val w = sizePx.width.takeIf { it > 0 } ?: return@detectDragGestures
                        val frac = (change.position.x / w).coerceIn(0f, 1f)
                        val ms = fromFrac(frac)
                        when (activeHandle) {
                            HandleSide.IN -> onSetIn(ms)
                            HandleSide.OUT -> onSetOut(ms)
                            null -> Unit
                        }
                        change.consume()
                    },
                    onDragEnd = { activeHandle = null },
                    onDragCancel = { activeHandle = null },
                )
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val midY = h / 2
            val barCount = waveform.size
            val gap = (w / barCount) * 0.18f
            // Visible band of the waveform — when zoomed, only the bars whose
            // timeline midpoint lands inside [visibleStart, visibleEnd] render,
            // each stretched to 1/zoomFactor of the row. Otherwise full span.
            val firstBar = ((visibleStart.toFloat() / totalMs) * barCount).toInt()
                .coerceIn(0, barCount - 1)
            val lastBar = ((visibleEnd.toFloat() / totalMs) * barCount).toInt()
                .coerceIn(firstBar, barCount - 1)
            val visibleCount = (lastBar - firstBar + 1).coerceAtLeast(1)
            val barW = w / visibleCount
            val inFrac = toFrac(inMs)
            val outFrac = toFrac(outMs)
            for (i in 0 until visibleCount) {
                val srcIdx = firstBar + i
                val tFrac = (i + 0.5f) / visibleCount
                val inside = tFrac in inFrac..outFrac
                val color = if (inside) Color(0x73A8E04E) else Color(0x12FFFFFF)
                val barH = (waveform[srcIdx] * h * 0.55f).coerceAtLeast(2f)
                drawRect(
                    color = color,
                    topLeft = Offset(i * barW + gap / 2, midY - barH / 2),
                    size = androidx.compose.ui.geometry.Size(barW - gap, barH),
                )
            }
            // Background hairline + active 8px lime bar
            drawRect(
                color = Color(0x14FFFFFF),
                topLeft = Offset(0f, midY - 1f),
                size = androidx.compose.ui.geometry.Size(w, 2f),
            )
            drawRect(
                color = MHColors.Lime,
                topLeft = Offset(w * inFrac, midY - 4f),
                size = androidx.compose.ui.geometry.Size(w * (outFrac - inFrac), 8f),
            )
            // Handles — vertical glow line + 24x36 lime body. The zoomed
            // handle gets a brighter highlight.
            val bodyW = with(density) { 24.dp.toPx() }
            val bodyH = with(density) { 36.dp.toPx() }
            for (handle in HandleSide.entries) {
                val ms = if (handle == HandleSide.IN) inMs else outMs
                if (ms < visibleStart || ms > visibleEnd) continue
                val frac = if (handle == HandleSide.IN) inFrac else outFrac
                val cx = w * frac
                val isFocused = (handle == HandleSide.IN && zoomTarget == ZoomTarget.IN) ||
                    (handle == HandleSide.OUT && zoomTarget == ZoomTarget.OUT)
                drawRect(
                    color = if (isFocused) Color(0xFFFFD96B) else Color(0xB3A8E04E),
                    topLeft = Offset(cx - 1f, midY - bodyH / 2 - 6f),
                    size = androidx.compose.ui.geometry.Size(2f, bodyH + 12f),
                )
                drawRect(
                    color = if (isFocused) Color(0xFFFFC857) else MHColors.Lime,
                    topLeft = Offset(cx - bodyW / 2, midY - bodyH / 2),
                    size = androidx.compose.ui.geometry.Size(bodyW, bodyH),
                )
            }
        }
        // ZOOM ×8 badge while focused — gives the user an obvious affordance
        // and a tap target to release zoom.
        if (isZoomed) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0A0A0A))
                    .border(1.dp, Color(0xFFFFC857), RoundedCornerShape(6.dp))
                    .clickable { onZoomChange(ZoomTarget.NONE) }
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    text = "ZOOM ×8 · ${if (zoomTarget == ZoomTarget.IN) "IN" else "OUT"}",
                    color = Color(0xFFFFC857),
                    style = LocalMHMono.current.eyebrow.copy(fontSize = 9.sp),
                )
            }
        }
    }
}

private enum class HandleSide { IN, OUT }

@Composable
private fun NudgeBox(
    label: String,
    timeMs: Long,
    onNudge: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0A0A0A))
            .border(1.dp, Color(0x10FFFFFF), RoundedCornerShape(10.dp))
            .padding(12.dp),
    ) {
        EyebrowText(text = label, color = MHColors.Lime)
        Spacer(Modifier.height(6.dp))
        Text(
            text = formatTime(timeMs),
            color = MHColors.TextHi,
            style = LocalMHMono.current.statValue,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            NudgeButton("−1s", Modifier.weight(1f)) { onNudge(-1_000L) }
            NudgeButton("−.1", Modifier.weight(1f)) { onNudge(-100L) }
            NudgeButton("+.1", Modifier.weight(1f)) { onNudge(100L) }
            NudgeButton("+1s", Modifier.weight(1f)) { onNudge(1_000L) }
        }
    }
}

@Composable
private fun NudgeButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x10FFFFFF))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = MHColors.TextHi,
            style = LocalMHMono.current.duration.copy(fontSize = 10.sp),
        )
    }
}

@Composable
private fun QuickPill(label: String, icon: String, active: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) Color(0x26A8E04E) else Color(0x10FFFFFF))
            .border(
                width = if (active) 1.dp else 0.dp,
                color = if (active) Color(0x66A8E04E) else Color.Transparent,
                shape = RoundedCornerShape(999.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = icon,
            color = if (active) MHColors.Lime else MHColors.TextHi,
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            text = label,
            color = if (active) MHColors.Lime else MHColors.TextHi,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun ResultCard(totalMs: Long, inMs: Long, outMs: Long) {
    val window = (outMs - inMs).coerceAtLeast(0L)
    val cut = (totalMs - window).coerceAtLeast(0L)
    Column(
        modifier = Modifier
            .padding(horizontal = MediaPlayerSpacing.M)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x0FA8E04E))
            .border(1.dp, Color(0x2EA8E04E), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        EyebrowText(text = "Risultato", color = MHColors.Lime)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = formatTime(window),
                color = MHColors.TextHi,
                style = LocalMHMono.current.duration.copy(fontSize = 14.sp),
            )
            Text(
                text = " · tagliato ${formatTime(cut)} dall'originale",
                color = MHColors.TextLo,
                style = LocalMHMono.current.duration,
            )
        }
    }
}

@Composable
private fun BottomHint() {
    Text(
        text = "TIENI PREMUTO UN MARCATORE PER ZOOM ×8",
        color = MHColors.TextLo2,
        style = LocalMHMono.current.eyebrow.copy(letterSpacing = 0.5.sp),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MediaPlayerSpacing.M, vertical = 12.dp),
    )
}

@Composable
private fun SavedToast(
    title: String,
    replacing: Boolean,
    onYes: () -> Unit,
    onNo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF181818))
            .border(1.dp, Color(0x4DA8E04E), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MHColors.Lime),
            contentAlignment = Alignment.Center,
        ) {
            if (replacing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF0A0A0A),
                )
            } else {
                Text("✓", color = Color(0xFF0A0A0A), style = MaterialTheme.typography.labelLarge)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Salvato come · $title",
                color = MHColors.TextHi,
                style = MaterialTheme.typography.titleSmall,
            )
            if (replacing) {
                Text(
                    text = "Sostituzione nelle playlist…",
                    color = MHColors.TextLo,
                    style = LocalMHMono.current.duration,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Versione locale · sostituirà l'originale nelle playlist?",
                        color = MHColors.TextLo,
                        style = LocalMHMono.current.duration,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text(
                        text = "Sì",
                        color = MHColors.Lime,
                        style = LocalMHMono.current.duration,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(onClick = onYes)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                    Text(
                        text = "/",
                        color = MHColors.TextLo,
                        style = LocalMHMono.current.duration,
                    )
                    Text(
                        text = "No",
                        color = MHColors.Lime,
                        style = LocalMHMono.current.duration,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(onClick = onNo)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorToast(message: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF2A1414))
            .border(1.dp, Color(0x66E04E4E), RoundedCornerShape(14.dp))
            .clickable(onClick = onDismiss)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Salvataggio non riuscito",
            color = Color(0xFFFF8A8A),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = message,
            color = MHColors.TextLo,
            style = LocalMHMono.current.duration,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalDeci = (ms / 100).coerceAtLeast(0L)
    val minutes = totalDeci / 600
    val seconds = (totalDeci % 600) / 10
    val deci = totalDeci % 10
    return "%02d:%02d.%d".format(minutes, seconds, deci)
}

private fun generateWaveform(seed: Int, bars: Int): FloatArray {
    // Mulberry32-ish so the bars look like real audio without an FFT pass.
    val rng = Random(seed)
    val out = FloatArray(bars)
    for (i in 0 until bars) {
        val t = i.toFloat() / bars
        val env = (t * 4f).coerceAtMost(1f) * (1f - (t - 0.85f).coerceAtLeast(0f) * 6f).coerceAtLeast(0f)
        val noise = rng.nextFloat()
        val sin = 0.4f + 0.55f * kotlin.math.abs(kotlin.math.sin(t * Math.PI.toFloat() * 7f))
        out[i] = (env * (0.55f * sin + 0.45f * noise)).coerceAtLeast(0.05f)
    }
    return out
}
