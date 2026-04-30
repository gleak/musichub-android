package com.mediaplayer.android.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.playback.PlaybackViewModel
import com.mediaplayer.android.ui.common.rememberCoverDominantColor
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun NowPlayingSheet(
    viewModel: PlaybackViewModel,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null,
        modifier = Modifier.fillMaxSize(),
    ) {
        NowPlayingContent(viewModel = viewModel, onDismiss = onDismiss)
    }
}

@UnstableApi
@Composable
private fun NowPlayingContent(viewModel: PlaybackViewModel, onDismiss: () -> Unit) {
    val song by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val position by viewModel.positionMs.collectAsStateWithLifecycle()
    val duration by viewModel.durationMs.collectAsStateWithLifecycle()
    val hasNext by viewModel.hasNext.collectAsStateWithLifecycle()
    val hasPrevious by viewModel.hasPrevious.collectAsStateWithLifecycle()
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val sleepActive by viewModel.sleepTimerActive.collectAsStateWithLifecycle()
    val liked by viewModel.currentLiked.collectAsStateWithLifecycle()
    val redownloading by viewModel.redownloading.collectAsStateWithLifecycle()
    val redownloadError by viewModel.redownloadError.collectAsStateWithLifecycle()
    val alarmExport by viewModel.alarmExportState.collectAsStateWithLifecycle()
    val videoDownloading by viewModel.videoDownloading.collectAsStateWithLifecycle()
    val videoDownloadError by viewModel.videoDownloadError.collectAsStateWithLifecycle()
    val videoReinitializing by viewModel.videoReinitializing.collectAsStateWithLifecycle()
    val videoReinitializeError by viewModel.videoReinitializeError.collectAsStateWithLifecycle()
    var confirmRedownload by remember { mutableStateOf(false) }
    var confirmMarkBroken by remember { mutableStateOf(false) }

    val current = song ?: run {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    var scrubValue by remember { mutableStateOf<Float?>(null) }
    var showQueue by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    var showSleepMenu by remember { mutableStateOf(false) }
    var showVideo by remember { mutableStateOf(false) }
    var pausedForVideo by remember { mutableStateOf(false) }

    LaunchedEffect(showVideo) {
        if (showVideo) {
            if (isPlaying) {
                viewModel.pause()
                pausedForVideo = true
            }
        } else if (pausedForVideo) {
            viewModel.play()
            pausedForVideo = false
        }
    }

    val coverModel = if (current.hasCoverArt) Network.coverUrl(current.id) else null
    val dominant = rememberCoverDominantColor(
        model = coverModel,
        fallback = MaterialTheme.colorScheme.surfaceContainerHighest,
    )

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to dominant,
                    0.55f to dominant.copy(alpha = 0.55f),
                    1f to MaterialTheme.colorScheme.background,
                )
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top bar: collapse + meta
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = Color.White,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "PLAYING FROM",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                    Text(
                        text = current.album ?: current.artist,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box {
                    IconButton(onClick = { showSleepMenu = true }) {
                        Icon(
                            imageVector = Icons.Filled.Bedtime,
                            contentDescription = "Sleep timer",
                            tint = if (sleepActive) MaterialTheme.colorScheme.primary else Color.White,
                        )
                    }
                    SleepTimerMenu(
                        expanded = showSleepMenu,
                        sleepActive = sleepActive,
                        onDismiss = { showSleepMenu = false },
                        onSelect = { minutes ->
                            showSleepMenu = false
                            if (minutes == 0) viewModel.cancelSleepTimer()
                            else viewModel.setSleepTimer(minutes)
                        },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            BoxWithConstraints {
                val artSize = (maxWidth * 0.92f).coerceAtMost(360.dp)
                if (showVideo) {
                    val videoWidth = maxWidth * 0.96f
                    Box(
                        modifier = Modifier
                            .width(videoWidth)
                            .height(videoWidth * 9f / 16f),
                        contentAlignment = Alignment.Center,
                    ) {
                        VideoPlayerInline(
                            song = current,
                            onClose = { showVideo = false },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.size(artSize),
                        contentAlignment = Alignment.Center,
                    ) {
                        Cover(song = current, size = artSize)
                    }
                }
            }

            Spacer(Modifier.height(36.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = current.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = current.artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = viewModel::toggleCurrentLike) {
                    Icon(
                        imageVector = if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (liked) "Unlike" else "Like",
                        tint = if (liked) MaterialTheme.colorScheme.primary else Color.White,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            val sliderMax = duration.takeIf { it > 0 }?.toFloat() ?: 1f
            val sliderValue = scrubValue ?: position.toFloat().coerceIn(0f, sliderMax)
            Slider(
                value = sliderValue,
                onValueChange = { scrubValue = it },
                onValueChangeFinished = {
                    scrubValue?.let { viewModel.seekTo(it.toLong()) }
                    scrubValue = null
                },
                valueRange = 0f..sliderMax,
                enabled = duration > 0,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatMs(sliderValue.toLong()),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                )
                Text(
                    text = formatMs(duration.coerceAtLeast(0)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = viewModel::toggleShuffle) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffleEnabled) MaterialTheme.colorScheme.primary
                               else Color.White.copy(alpha = 0.85f),
                    )
                }
                IconButton(
                    onClick = viewModel::skipPrevious,
                    enabled = hasPrevious,
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp),
                    )
                }
                FilledIconButton(
                    onClick = viewModel::togglePlayPause,
                    modifier = Modifier.size(72.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black,
                    ),
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                    )
                }
                IconButton(
                    onClick = viewModel::skipNext,
                    enabled = hasNext,
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp),
                    )
                }
                IconButton(onClick = viewModel::cycleRepeat) {
                    Icon(
                        imageVector = if (repeatMode == Player.REPEAT_MODE_ONE)
                            Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        contentDescription = "Repeat",
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF)
                            MaterialTheme.colorScheme.primary
                        else Color.White.copy(alpha = 0.85f),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { showLyrics = !showLyrics }) {
                    Icon(
                        imageVector = Icons.Filled.TextSnippet,
                        contentDescription = "Lyrics",
                        tint = Color.White.copy(alpha = 0.85f),
                    )
                }
                if (current.hasVideo) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showVideo = true }) {
                            Icon(
                                imageVector = Icons.Filled.VideoLibrary,
                                contentDescription = "Watch video",
                                tint = Color.White.copy(alpha = 0.85f),
                            )
                        }
                        IconButton(
                            onClick = viewModel::reinitializeVideoForCurrent,
                            enabled = !videoReinitializing,
                        ) {
                            if (videoReinitializing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Tune,
                                    contentDescription = "Reinitialize video for fast seeking",
                                    tint = Color.White.copy(alpha = 0.45f),
                                )
                            }
                        }
                    }
                } else {
                    IconButton(
                        onClick = viewModel::downloadVideoForCurrent,
                        enabled = !videoDownloading,
                    ) {
                        if (videoDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.VideoFile,
                                contentDescription = "Download video",
                                tint = Color.White.copy(alpha = 0.45f),
                            )
                        }
                    }
                }
                IconButton(onClick = { showEqualizer = true }) {
                    Icon(
                        imageVector = Icons.Filled.Equalizer,
                        contentDescription = "Equalizer",
                        tint = Color.White.copy(alpha = 0.85f),
                    )
                }
                IconButton(
                    onClick = { confirmRedownload = true },
                    enabled = !redownloading,
                ) {
                    if (redownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Re-download song",
                            tint = Color.White.copy(alpha = 0.85f),
                        )
                    }
                }
                IconButton(
                    onClick = { confirmMarkBroken = true },
                    enabled = !redownloading,
                ) {
                    Icon(
                        imageVector = Icons.Filled.ReportProblem,
                        contentDescription = "Re-download song to device",
                        tint = Color.White.copy(alpha = 0.85f),
                    )
                }
                IconButton(
                    onClick = { viewModel.saveCurrentAsAlarmSound() },
                    enabled = alarmExport !is PlaybackViewModel.AlarmExportState.Exporting,
                ) {
                    if (alarmExport is PlaybackViewModel.AlarmExportState.Exporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Alarm,
                            contentDescription = "Save as alarm sound",
                            tint = Color.White.copy(alpha = 0.85f),
                        )
                    }
                }
                IconButton(onClick = { showQueue = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = "Queue",
                        tint = Color.White.copy(alpha = 0.85f),
                    )
                }
            }

            if (showLyrics) {
                LyricsView(
                    songId = current.id,
                    positionMs = position,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showQueue) {
        QueueSheet(viewModel = viewModel, onDismiss = { showQueue = false })
    }

    if (showEqualizer) {
        EqualizerSheet(onDismiss = { showEqualizer = false })
    }

    if (confirmRedownload) {
        AlertDialog(
            onDismissRequest = { confirmRedownload = false },
            title = { Text("Re-download song?") },
            text = {
                Text(
                    "Delete the current audio and cover for \"${current.title}\" and " +
                        "fetch them again from the original source. Useful when the file " +
                        "is corrupted or the cover is wrong."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmRedownload = false
                    viewModel.redownloadCurrent()
                }) { Text("Re-download") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRedownload = false }) { Text("Cancel") }
            },
        )
    }

    if (confirmMarkBroken) {
        AlertDialog(
            onDismissRequest = { confirmMarkBroken = false },
            title = { Text("Re-download song to device?") },
            text = {
                Text(
                    "Drop the local cache, offline copy and cover for \"${current.title}\" " +
                        "and fetch fresh bytes from the server. Use this when the file on " +
                        "your phone is corrupted but the server copy is fine."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmMarkBroken = false
                    viewModel.refreshLocalDownload()
                }) { Text("Re-download") }
            },
            dismissButton = {
                TextButton(onClick = { confirmMarkBroken = false }) { Text("Cancel") }
            },
        )
    }

    redownloadError?.let { msg ->
        AlertDialog(
            onDismissRequest = viewModel::consumeRedownloadError,
            title = { Text("Re-download failed") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = viewModel::consumeRedownloadError) { Text("OK") }
            },
        )
    }

    videoDownloadError?.let { msg ->
        AlertDialog(
            onDismissRequest = viewModel::consumeVideoDownloadError,
            title = { Text("Video download failed") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = viewModel::consumeVideoDownloadError) { Text("OK") }
            },
        )
    }

    videoReinitializeError?.let { msg ->
        AlertDialog(
            onDismissRequest = viewModel::consumeVideoReinitializeError,
            title = { Text("Video reinitialize failed") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = viewModel::consumeVideoReinitializeError) { Text("OK") }
            },
        )
    }

    when (val s = alarmExport) {
        is PlaybackViewModel.AlarmExportState.Success -> AlertDialog(
            onDismissRequest = viewModel::consumeAlarmExportState,
            title = { Text("Saved as alarm sound") },
            text = {
                Text(
                    "\"${s.title}\" is now available in your Clock app — open an alarm, " +
                        "tap the sound row and pick it from the list."
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::consumeAlarmExportState) { Text("OK") }
            },
        )
        is PlaybackViewModel.AlarmExportState.Failure -> AlertDialog(
            onDismissRequest = viewModel::consumeAlarmExportState,
            title = { Text("Couldn't save alarm sound") },
            text = { Text(s.message) },
            confirmButton = {
                TextButton(onClick = viewModel::consumeAlarmExportState) { Text("OK") }
            },
        )
        else -> Unit
    }
}

@Composable
private fun SleepTimerMenu(
    expanded: Boolean,
    sleepActive: Boolean,
    onDismiss: () -> Unit,
    onSelect: (minutes: Int) -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        if (sleepActive) {
            DropdownMenuItem(
                text = { Text("Cancel timer") },
                onClick = { onSelect(0) },
            )
        }
        listOf(15, 30, 60).forEach { min ->
            DropdownMenuItem(
                text = { Text("$min minutes") },
                onClick = { onSelect(min) },
            )
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}
