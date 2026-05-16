package com.mediaplayer.android.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.sp
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
    onTrim: (() -> Unit)? = null,
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null,
) {
    androidx.activity.compose.BackHandler(onBack = onDismiss)
    NowPlayingContent(
        viewModel = viewModel,
        onDismiss = onDismiss,
        onArtistClick = onArtistClick,
        onTrim = onTrim,
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
    onTrim: (() -> Unit)? = null,
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

    val dislike = com.mediaplayer.android.ui.common.rememberDislikeActions(current.id, current.artist)

    var scrubValue by remember { mutableStateOf<Float?>(null) }
    var showQueue by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    var showSleepMenu by remember { mutableStateOf(false) }
    var showVideo by remember { mutableStateOf(false) }
    var pausedForVideo by remember { mutableStateOf(false) }

    // Pause/resume must be decided synchronously with the toggle, not in a
    // LaunchedEffect that re-reads `isPlaying`: the collected State can lag
    // the underlying StateFlow by one frame and a same-frame play/pause
    // race could either leave audio silent after closing the video, or
    // force-resume audio the user explicitly paused.
    val openVideo: () -> Unit = {
        if (!showVideo) {
            if (viewModel.isPlaying.value) {
                viewModel.pause()
                pausedForVideo = true
            }
            showVideo = true
        }
    }
    val closeVideo: () -> Unit = {
        if (showVideo) {
            if (pausedForVideo) {
                viewModel.play()
                pausedForVideo = false
            }
            showVideo = false
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
                        contentDescription = "Comprimi",
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
                IconButton(onClick = { showSleepMenu = true }) {
                    Icon(
                        imageVector = Icons.Filled.Bedtime,
                        contentDescription = "Timer di sospensione",
                        tint = if (sleepActive) MaterialTheme.colorScheme.primary else MHColors.OnHero,
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
                            onClose = closeVideo,
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
                com.mediaplayer.android.ui.common.LikeButton(
                    songId = current.id,
                    variant = com.mediaplayer.android.ui.common.LikeButtonVariant.Hero,
                    // Route through the service so AA + notification heart
                    // stay in sync. The service mirrors back into the cache.
                    onToggleOverride = { viewModel.toggleCurrentLike() },
                )
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
                    modifier = Modifier.semantics { contentDescription = "Posizione di riproduzione" },
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
                            contentDescription = "Casuale",
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
                            contentDescription = "Precedente",
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
                            contentDescription = if (isPlaying) "Pausa" else "Riproduci",
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
                            contentDescription = "Successivo",
                            tint = MHColors.OnHero,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                    IconButton(onClick = viewModel::cycleRepeat) {
                        Icon(
                            imageVector = if (repeatMode == Player.REPEAT_MODE_ONE)
                                Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                            contentDescription = "Ripeti",
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
                        IconButton(onClick = { if (showVideo) closeVideo() else openVideo() }) {
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
                        if (onTrim != null) {
                            DropdownMenuItem(
                                text = { Text("Taglia traccia…") },
                                onClick = {
                                    overflowOpen = false
                                    onTrim()
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Non consigliarmi questo brano") },
                            onClick = {
                                overflowOpen = false
                                dislike.song().invoke()
                            },
                        )
                        if (current.artist.isNotBlank()) {
                            DropdownMenuItem(
                                text = { Text("Non consigliarmi questo artista") },
                                onClick = {
                                    overflowOpen = false
                                    dislike.artist().invoke()
                                },
                            )
                        }
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

    if (showSleepMenu) {
        SleepTimerSheet(
            viewModel = viewModel,
            onDismiss = { showSleepMenu = false },
        )
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
        com.mediaplayer.android.ui.common.FlagWrongConfirmDialog(
            songId = current.id,
            songTitle = current.title,
            songArtist = current.artist,
            hasCoverArt = current.hasCoverArt,
            onConfirm = {
                confirmFlagWrong = false
                viewModel.flagWrong(current.id)
            },
            onDismiss = { confirmFlagWrong = false },
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

/**
 * Bottom sheet replacing the legacy `SleepTimerMenu` dropdown. Mirrors
 * `mockup/mh-player-sheets.jsx:171-206`:
 *
 * - eyebrow `// TIMER`, title `Timer di sospensione`
 * - active hero card (gradient lime alpha bg + lime border) with mono
 *   countdown `mm:ss`, scheduled-end hint `L'audio si fermerà alle hh:mm`,
 *   and an `Annulla` pill. End-of-track variant swaps the countdown for
 *   `Fine traccia` and the hint for "Si fermerà alla fine del brano corrente".
 * - 3×2 preset grid (5/10/15/30/45/60 min) — chip number is mono 22sp,
 *   `MIN` sub-label is mono 10sp.
 * - full-width outlined `Fine traccia` end-of-track CTA (lime border alpha 0.3).
 *
 * Service routes preset / end-of-track taps through `SleepTimer.set` /
 * `setEndOfTrack`, both of which cancel any currently-armed timer before
 * arming the new mode — so the sheet does not need to cancel first.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SleepTimerSheet(
    viewModel: PlaybackViewModel,
    onDismiss: () -> Unit,
) {
    val sleepActive by viewModel.sleepTimerActive.collectAsStateWithLifecycle()
    val remainingMs by viewModel.sleepTimerRemainingMs.collectAsStateWithLifecycle()
    val endOfTrack by viewModel.sleepTimerEndOfTrack.collectAsStateWithLifecycle()

    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    )
    val mono = com.mediaplayer.android.ui.theme.LocalMHMono.current
    val accent = MaterialTheme.colorScheme.primary

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF161616),
        scrimColor = Color.Black.copy(alpha = 0.5f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(
                        color = MHColors.TextLo2,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp),
                    ),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = "// TIMER",
                style = mono.eyebrow,
                color = accent,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Timer di sospensione",
                style = MaterialTheme.typography.headlineSmall,
                color = MHColors.TextHi,
            )
            Spacer(Modifier.height(20.dp))

            if (sleepActive) {
                ActiveTimerCard(
                    endOfTrack = endOfTrack,
                    remainingMs = remainingMs,
                    onCancel = { viewModel.cancelSleepTimer() },
                    accent = accent,
                    monoEyebrow = mono.eyebrow,
                )
                Spacer(Modifier.height(22.dp))
            }

            val presets = listOf(5, 10, 15, 30, 45, 60)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { minutes ->
                            SleepPresetChip(
                                minutes = minutes,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.setSleepTimer(minutes) },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            EndOfTrackButton(
                accent = accent,
                onClick = { viewModel.setEndOfTrackSleepTimer() },
            )
        }
    }
}

@Composable
private fun ActiveTimerCard(
    endOfTrack: Boolean,
    remainingMs: Long,
    onCancel: () -> Unit,
    accent: Color,
    monoEyebrow: androidx.compose.ui.text.TextStyle,
) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    0f to accent.copy(alpha = 0.12f),
                    1f to accent.copy(alpha = 0.04f),
                ),
                shape = shape,
            )
            .thinBorder(accent.copy(alpha = 0.25f), shape)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "// ATTIVO",
                style = monoEyebrow,
                color = accent,
            )
            Spacer(Modifier.height(4.dp))
            if (endOfTrack) {
                Text(
                    text = "Fine traccia",
                    style = MaterialTheme.typography.headlineMedium,
                    color = accent,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Si fermerà alla fine del brano corrente",
                    style = MaterialTheme.typography.bodySmall,
                    color = MHColors.TextLo,
                )
            } else {
                // Service updates `remainingMs` only at minute boundaries (it
                // sleeps until the next whole-minute roll-over). The hero card
                // wants a live `mm:ss` so we tick locally between snapshots —
                // each new snapshot resets the wall-clock baseline.
                val targetEndAt = remember(remainingMs) {
                    System.currentTimeMillis() + remainingMs.coerceAtLeast(0L)
                }
                var liveMs by remember(remainingMs) {
                    mutableStateOf(remainingMs.coerceAtLeast(0L))
                }
                LaunchedEffect(remainingMs) {
                    while (true) {
                        val left = targetEndAt - System.currentTimeMillis()
                        liveMs = left.coerceAtLeast(0L)
                        if (left <= 0L) break
                        kotlinx.coroutines.delay(1000L)
                    }
                }
                Text(
                    text = formatRemaining(liveMs),
                    fontFamily = com.mediaplayer.android.ui.theme.MonoFamily,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                    fontSize = 38.sp,
                    color = accent,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "L'audio si fermerà alle ${formatClockAt(targetEndAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MHColors.TextLo,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .background(
                    color = Color.White.copy(alpha = 0.08f),
                    shape = androidx.compose.foundation.shape.CircleShape,
                )
                .clickable(onClick = onCancel)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text(
                text = "Annulla",
                style = MaterialTheme.typography.labelLarge,
                color = MHColors.TextHi,
            )
        }
    }
}

@Composable
private fun SleepPresetChip(
    minutes: Int,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val mono = com.mediaplayer.android.ui.theme.LocalMHMono.current
    Column(
        modifier = modifier
            .background(
                color = MHColors.Card,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = minutes.toString(),
            fontFamily = com.mediaplayer.android.ui.theme.MonoFamily,
            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
            fontSize = 22.sp,
            color = MHColors.TextHi,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "MIN",
            style = mono.badge,
            color = MHColors.TextLo,
        )
    }
}

@Composable
private fun EndOfTrackButton(
    accent: Color,
    onClick: () -> Unit,
) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = accent.copy(alpha = 0.06f),
                shape = shape,
            )
            .thinBorder(accent.copy(alpha = 0.3f), shape)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Fine traccia",
            style = MaterialTheme.typography.titleSmall,
            color = accent,
        )
    }
}

/** Convenience to apply a 1dp solid border with a given shape. */
private fun Modifier.thinBorder(
    color: Color,
    shape: androidx.compose.ui.graphics.Shape,
): Modifier = this.border(width = 1.dp, color = color, shape = shape)

private fun formatRemaining(ms: Long): String {
    val safe = ms.coerceAtLeast(0L)
    val totalSeconds = (safe + 999L) / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

private fun formatClockAt(epochMs: Long): String {
    val fmt = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())
    return fmt.format(java.util.Date(epochMs))
}

private fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}
