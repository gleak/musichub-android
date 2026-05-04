package com.mediaplayer.android.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.ui.theme.HeroCoverSize
import com.mediaplayer.android.ui.theme.MHColors
import com.mediaplayer.android.playback.PlaybackViewModel
import com.mediaplayer.android.ui.common.SongCover
import com.mediaplayer.android.ui.common.rememberCoverDominantColor
import com.mediaplayer.android.ui.theme.CoverShapes
import com.mediaplayer.android.ui.theme.MediaPlayerSpacing
import java.util.Locale

/**
 * Fullscreen now-playing surface. Replaces the original `ModalBottomSheet`
 * implementation so the cover can participate in a `SharedTransitionLayout`
 * with the MiniPlayer — `ModalBottomSheet` lives in its own Popup window,
 * which sits outside the caller's composition tree and breaks shared-element
 * lookup. Caller (AppScaffold) wraps this in an `AnimatedVisibility` and
 * passes the shared-element scopes down so the cover animates from
 * MiniPlayer position to hero position on open.
 *
 * `sharedTransitionScope` + `animatedVisibilityScope` are nullable so
 * standalone callers (previews, tests) can drop them and fall back to a
 * plain hero render.
 */
@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@UnstableApi
@Composable
fun NowPlayingSheet(
    viewModel: PlaybackViewModel,
    onDismiss: () -> Unit,
    onArtistClick: ((String) -> Unit)? = null,
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null,
) {
    androidx.activity.compose.BackHandler(onBack = onDismiss)
    NowPlayingContent(
        viewModel = viewModel,
        onDismiss = onDismiss,
        onArtistClick = onArtistClick,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
    )
}

/** Plan-locked shared-element key for the MiniPlayer ↔ NowPlayingSheet cover. */
const val NOW_PLAYING_COVER_KEY = "now-playing-cover"

@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@UnstableApi
@Composable
private fun NowPlayingContent(
    viewModel: PlaybackViewModel,
    onDismiss: () -> Unit,
    onArtistClick: ((String) -> Unit)? = null,
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null,
) {
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
    val haptics = LocalHapticFeedback.current
    val redownloading by viewModel.redownloading.collectAsStateWithLifecycle()
    val redownloadError by viewModel.redownloadError.collectAsStateWithLifecycle()
    val alarmExport by viewModel.alarmExportState.collectAsStateWithLifecycle()
    val videoDownloading by viewModel.videoDownloading.collectAsStateWithLifecycle()
    val videoDownloadError by viewModel.videoDownloadError.collectAsStateWithLifecycle()
    val videoReinitializing by viewModel.videoReinitializing.collectAsStateWithLifecycle()
    val videoReinitializeError by viewModel.videoReinitializeError.collectAsStateWithLifecycle()
    var confirmRedownload by remember { mutableStateOf(false) }
    var confirmMarkBroken by remember { mutableStateOf(false) }
    var confirmFlagWrong by remember { mutableStateOf(false) }
    var overflowOpen by remember { mutableStateOf(false) }

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
                // Cover-derived gradient. The dominant colour bleeds in at the
                // top so the hero artwork blends into the backdrop; we then
                // fade through a midtone so transport controls (white icons +
                // text) keep contrast even when the cover is light-dominant
                // (white classical sleeves, pastel covers). See the second
                // black overlay below the gradient for the same reason.
                Brush.verticalGradient(
                    0f to dominant,
                    0.55f to dominant.copy(alpha = 0.55f),
                    1f to MaterialTheme.colorScheme.background,
                )
            ),
    ) {
        // Darken overlay for the bottom half. Light-dominant covers
        // (white classical sleeves, pastel pop) bleach out the white
        // transport icons + slider time labels below; this gradient pulls
        // them back to readable contrast without dimming the hero art.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.45f to Color.Transparent,
                        1f to MHColors.HeroScrim,
                    )
                ),
        )
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
                        tint = MHColors.OnHero,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "IN RIPRODUZIONE DA",
                        style = MaterialTheme.typography.labelSmall,
                        color = MHColors.OnHeroMuted,
                    )
                    Text(
                        text = current.album ?: current.artist,
                        style = MaterialTheme.typography.titleSmall,
                        color = MHColors.OnHero,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box {
                    IconButton(onClick = { showSleepMenu = true }) {
                        Icon(
                            imageVector = Icons.Filled.Bedtime,
                            contentDescription = "Sleep timer",
                            tint = if (sleepActive) MaterialTheme.colorScheme.primary else MHColors.OnHero,
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

            Spacer(Modifier.height(MediaPlayerSpacing.L))

            BoxWithConstraints {
                val artSize = (maxWidth * HeroCoverSize.NowPlayingFraction).coerceAtMost(HeroCoverSize.NowPlayingMax)
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
                    // Hero cover. When the caller wired up SharedTransitionLayout
                    // scopes, the cover animates from the MiniPlayer's small
                    // tile position into this hero size on open via sharedBounds.
                    // Without the scopes (preview/test), falls back to a plain
                    // sized Cover with no entrance animation.
                    val heroModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                        with(sharedTransitionScope) {
                            Modifier.sharedBounds(
                                sharedContentState = rememberSharedContentState(NOW_PLAYING_COVER_KEY),
                                animatedVisibilityScope = animatedVisibilityScope,
                            )
                        }
                    } else Modifier
                    Box(
                        modifier = Modifier.size(artSize),
                        contentAlignment = Alignment.Center,
                    ) {
                        SongCover(
                            song = current,
                            size = artSize,
                            shape = CoverShapes.MiniPlayer,
                            modifier = heroModifier,
                        )
                    }
                }
            }

            Spacer(Modifier.height(MediaPlayerSpacing.Xl + MediaPlayerSpacing.Xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = current.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MHColors.OnHero,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = current.artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MHColors.OnHeroMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = if (onArtistClick != null) {
                            Modifier.clickable {
                                onDismiss()
                                onArtistClick(current.artist)
                            }
                        } else Modifier,
                    )
                }
                IconButton(onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.toggleCurrentLike()
                }) {
                    Icon(
                        imageVector = if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (liked) "Rimuovi mi piace" else "Mi piace",
                        tint = if (liked) MaterialTheme.colorScheme.primary else MHColors.OnHero,
                    )
                }
            }

            Spacer(Modifier.height(MediaPlayerSpacing.M))

            // Audio progress + transport are hidden while the inline video is
            // showing — the video plays in its own ExoPlayer (audio is paused
            // for the duration), so the audio slider/transport would be
            // disconnected from what the user sees and just confuse them.
            // The PlayerView's own controller exposes scrub/play/pause for
            // the video stream.
            if (!showVideo) {
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
                        thumbColor = MHColors.OnHero,
                        activeTrackColor = MHColors.OnHero,
                        inactiveTrackColor = MHColors.OnHeroTrack,
                    ),
                    modifier = Modifier.semantics { contentDescription = "Playback position" },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = formatMs(sliderValue.toLong()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MHColors.OnHeroMuted,
                    )
                    Text(
                        text = formatMs(duration.coerceAtLeast(0)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MHColors.OnHeroMuted,
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
                                   else MHColors.OnHeroMuted,
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
                            tint = MHColors.OnHero,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                    FilledIconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.togglePlayPause()
                        },
                        modifier = Modifier.size(72.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MHColors.OnHero,
                            contentColor = Color.Black,
                        ),
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
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
                            tint = MHColors.OnHero,
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
                            else MHColors.OnHeroMuted,
                        )
                    }
                }

                Spacer(Modifier.height(MediaPlayerSpacing.M))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { showLyrics = !showLyrics }) {
                    Icon(
                        imageVector = Icons.Filled.TextSnippet,
                        contentDescription = "Testo",
                        tint = MHColors.OnHeroMuted,
                    )
                }
                if (current.hasVideo) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showVideo = !showVideo }) {
                            Icon(
                                imageVector = if (showVideo) Icons.Filled.MusicNote
                                              else Icons.Filled.VideoLibrary,
                                contentDescription = if (showVideo) "Torna all'audio"
                                                     else "Guarda il video",
                                tint = if (showVideo) MaterialTheme.colorScheme.primary
                                       else MHColors.OnHeroMuted,
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
                                    color = MHColors.OnHero,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Tune,
                                    contentDescription = "Reinizializza il video per la ricerca rapida",
                                    tint = MHColors.OnHeroDim,
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
                                color = MHColors.OnHero,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.VideoFile,
                                contentDescription = "Scarica il video",
                                tint = MHColors.OnHeroDim,
                            )
                        }
                    }
                }
                IconButton(onClick = { showEqualizer = true }) {
                    Icon(
                        imageVector = Icons.Filled.Equalizer,
                        contentDescription = "Equalizzatore",
                        tint = MHColors.OnHeroMuted,
                    )
                }
                IconButton(onClick = { showQueue = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = "Coda",
                        tint = MHColors.OnHeroMuted,
                    )
                }
                Box {
                    IconButton(onClick = { overflowOpen = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Altro",
                            tint = MHColors.OnHeroMuted,
                        )
                    }
                    DropdownMenu(
                        expanded = overflowOpen,
                        onDismissRequest = { overflowOpen = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (redownloading) "Riscaricamento…" else "Riscarica dalla sorgente") },
                            enabled = !redownloading,
                            onClick = {
                                overflowOpen = false
                                confirmRedownload = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Aggiorna copia locale") },
                            enabled = !redownloading,
                            onClick = {
                                overflowOpen = false
                                confirmMarkBroken = true
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (alarmExport is PlaybackViewModel.AlarmExportState.Exporting)
                                        "Salvataggio come suoneria…"
                                    else "Salva come suoneria sveglia"
                                )
                            },
                            enabled = alarmExport !is PlaybackViewModel.AlarmExportState.Exporting,
                            onClick = {
                                overflowOpen = false
                                viewModel.saveCurrentAsAlarmSound()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Segnala brano sbagliato") },
                            onClick = {
                                overflowOpen = false
                                confirmFlagWrong = true
                            },
                        )
                    }
                }
            }

            if (showLyrics) {
                LyricsView(
                    songId = current.id,
                    positionMs = position,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            Spacer(Modifier.height(MediaPlayerSpacing.L))
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
            title = { Text("Riscaricare dalla sorgente?") },
            text = {
                Text(
                    "Elimina audio e copertina di \"${current.title}\" e li riscarica " +
                        "dalla sorgente originale. Utile se il file è danneggiato o la " +
                        "copertina è sbagliata."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmRedownload = false
                    viewModel.redownloadCurrent()
                }) { Text("Riscarica") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRedownload = false }) { Text("Annulla") }
            },
        )
    }

    if (confirmFlagWrong) {
        AlertDialog(
            onDismissRequest = { confirmFlagWrong = false },
            title = { Text("Segnalare brano sbagliato?") },
            text = {
                Text(
                    "“${current.title}” verrà rimosso dalle tue playlist, dai mi piace e dalla " +
                        "cronologia, e il file sarà eliminato dal server. L'azione è definitiva."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmFlagWrong = false
                    viewModel.flagWrong(current.id)
                }) { Text("Segnala") }
            },
            dismissButton = {
                TextButton(onClick = { confirmFlagWrong = false }) { Text("Annulla") }
            },
        )
    }

    if (confirmMarkBroken) {
        AlertDialog(
            onDismissRequest = { confirmMarkBroken = false },
            title = { Text("Aggiornare la copia locale?") },
            text = {
                Text(
                    "Cancella la cache locale, la copia offline e la copertina di " +
                        "\"${current.title}\" e scarica byte freschi dal server. Usalo quando " +
                        "il file sul telefono è danneggiato ma quello sul server è integro."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmMarkBroken = false
                    viewModel.refreshLocalDownload()
                }) { Text("Aggiorna") }
            },
            dismissButton = {
                TextButton(onClick = { confirmMarkBroken = false }) { Text("Annulla") }
            },
        )
    }

    redownloadError?.let { msg ->
        AlertDialog(
            onDismissRequest = viewModel::consumeRedownloadError,
            title = { Text("Riscaricamento non riuscito") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = viewModel::consumeRedownloadError) { Text("OK") }
            },
        )
    }

    videoDownloadError?.let { msg ->
        AlertDialog(
            onDismissRequest = viewModel::consumeVideoDownloadError,
            title = { Text("Download del video non riuscito") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = viewModel::consumeVideoDownloadError) { Text("OK") }
            },
        )
    }

    videoReinitializeError?.let { msg ->
        AlertDialog(
            onDismissRequest = viewModel::consumeVideoReinitializeError,
            title = { Text("Reinizializzazione del video non riuscita") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = viewModel::consumeVideoReinitializeError) { Text("OK") }
            },
        )
    }

    when (val s = alarmExport) {
        is PlaybackViewModel.AlarmExportState.Success -> AlertDialog(
            onDismissRequest = viewModel::consumeAlarmExportState,
            title = { Text("Salvato come suoneria sveglia") },
            text = {
                Text(
                    "\"${s.title}\" è ora disponibile nell'app Orologio — apri una sveglia, " +
                        "tocca la riga del suono e selezionalo dalla lista."
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::consumeAlarmExportState) { Text("OK") }
            },
        )
        is PlaybackViewModel.AlarmExportState.Failure -> AlertDialog(
            onDismissRequest = viewModel::consumeAlarmExportState,
            title = { Text("Impossibile salvare la suoneria") },
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
                text = { Text("Annulla timer") },
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
