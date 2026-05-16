package com.mediaplayer.android.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.playback.PlaybackViewModel
import com.mediaplayer.android.playback.QueueEntry
import com.mediaplayer.android.ui.common.EmptyState
import com.mediaplayer.android.ui.playlists.AddToPlaylistSheet
import com.mediaplayer.android.ui.search.SongRow
import com.mediaplayer.android.ui.theme.LocalMHMono

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun QueueSheet(
    viewModel: PlaybackViewModel,
    onDismiss: () -> Unit,
) {
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val kebab = com.mediaplayer.android.ui.common.rememberSongKebab()

    // Spotify-style split: hide everything before the current item,
    // surface the user queue first, then the rest of the source.
    val current = queue.firstOrNull { it.isCurrent }
    val ahead = queue.filter { it.index > (current?.index ?: -1) }
    val userAhead = ahead.filter { it.userQueued }
    val sourceAhead = ahead.filter { !it.userQueued }
    // Source-of-queue label: every source row shares an album, so use the
    // first source row's album (falls back to artist) for the eyebrow.
    val sourceLabel = sourceAhead.firstOrNull()?.song?.album?.takeIf { it.isNotBlank() }
        ?: sourceAhead.firstOrNull()?.song?.artist
    val mono = LocalMHMono.current
    val accent = MaterialTheme.colorScheme.primary

    // Hoisted out of the argument list — rememberModalBottomSheetState
    // relies on positional remembering, and inlining it makes the slot
    // fragile against any future composition-hierarchy shift above this
    // call site.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Eyebrow + title + header chips.
            Text(
                text = "// CODA",
                style = mono.eyebrow,
                color = accent,
                modifier = Modifier.padding(start = 24.dp, top = 4.dp),
            )
            Spacer(Modifier.size(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "In riproduzione",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                HeaderChip(
                    icon = Icons.Filled.Shuffle,
                    label = "Shuffle",
                    selected = shuffleEnabled,
                    onClick = { viewModel.toggleShuffle() },
                )
                Spacer(Modifier.width(8.dp))
                HeaderChip(
                    icon = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne
                    else Icons.Filled.Repeat,
                    label = "Repeat",
                    selected = repeatMode != Player.REPEAT_MODE_OFF,
                    onClick = { viewModel.cycleRepeat() },
                )
                Spacer(Modifier.width(8.dp))
                HeaderChip(
                    icon = Icons.Filled.MoreHoriz,
                    label = "More",
                    selected = false,
                    onClick = onDismiss,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Box(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
            ) {
                if (queue.isEmpty()) {
                    EmptyState(
                        icon = Icons.AutoMirrored.Filled.QueueMusic,
                        title = "Coda vuota",
                        subtitle = "Tocca un brano per iniziare la riproduzione.",
                    )
                } else {
                    LazyColumn {
                        if (current != null) {
                            item(key = "section-now") {
                                QueueSectionHeader(
                                    main = "// IN RIPRODUZIONE",
                                    sub = null,
                                    accent = accent,
                                )
                            }
                            item(key = "now-${current.index}-${current.song.id}") {
                                QueueRow(
                                    entry = current,
                                    onClick = { viewModel.skipToQueueItem(current.index) },
                                    onMore = { kebab.open(current.song) },
                                    onRemove = null,
                                    highlight = true,
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                        if (userAhead.isNotEmpty()) {
                            item(key = "section-user") {
                                QueueSectionHeader(
                                    main = "// IN CODA · UTENTE · ${userAhead.size}",
                                    sub = null,
                                    accent = accent,
                                )
                            }
                            items(items = userAhead, key = { "uq-${it.index}-${it.song.id}" }) { entry ->
                                QueueRow(
                                    entry = entry,
                                    onClick = { viewModel.skipToQueueItem(entry.index) },
                                    onMore = { kebab.open(entry.song) },
                                    onRemove = { viewModel.removeFromQueue(entry.index) },
                                    highlight = false,
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                        if (sourceAhead.isNotEmpty()) {
                            item(key = "section-source") {
                                QueueSectionHeader(
                                    main = "// SUCCESSIVI",
                                    sub = sourceLabel?.let { "DA “${it}”" },
                                    accent = accent,
                                )
                            }
                            items(items = sourceAhead, key = { "src-${it.index}-${it.song.id}" }) { entry ->
                                QueueRow(
                                    entry = entry,
                                    onClick = { viewModel.skipToQueueItem(entry.index) },
                                    onMore = { kebab.open(entry.song) },
                                    onRemove = { viewModel.removeFromQueue(entry.index) },
                                    highlight = false,
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }

            // Sticky bottom — Cancella coda CTA. Only visible when there's
            // anything to clear (current + ≥1 follow-up).
            if (ahead.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(
                            width = 1.dp,
                            color = Color(0xFFFF4D2E).copy(alpha = 0.5f),
                            shape = RoundedCornerShape(999.dp),
                        )
                        .clickable { viewModel.clearQueue() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Cancella coda",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFFF4D2E),
                    )
                }
            }
        }
    }

    com.mediaplayer.android.ui.common.SongKebabSheet(
        state = kebab,
        // Queue's flag-wrong both reports the song AND prunes flagged
        // entries from the active timeline — bypass the standard
        // SongRepository-only flow.
        flagWrongOverride = { song -> viewModel.flagWrong(song.id) },
    )
}

@Composable
private fun HeaderChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(
                color = if (selected) accent.copy(alpha = 0.16f)
                else Color.White.copy(alpha = 0.06f),
                shape = CircleShape,
            )
            .border(
                width = 1.dp,
                color = if (selected) accent.copy(alpha = 0.6f)
                else Color.White.copy(alpha = 0.08f),
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun QueueSectionHeader(main: String, sub: String?, accent: Color) {
    val mono = LocalMHMono.current
    Row(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = main,
            style = mono.eyebrow,
            color = accent,
        )
        if (!sub.isNullOrBlank()) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = "· $sub",
                style = mono.eyebrow,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QueueRow(
    entry: QueueEntry,
    onClick: () -> Unit,
    onMore: () -> Unit,
    onRemove: (() -> Unit)?,
    highlight: Boolean,
) {
    Row(
        modifier = if (highlight)
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
        else Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SongRow(
            song = entry.song,
            onClick = onClick,
            onMore = onMore,
            // Queue rows already crowd the trailing area with a remove-from-queue
            // affordance — drop the heart here and route like via the kebab.
            showLike = false,
            modifier = Modifier.weight(1f),
        )
        if (onRemove != null) {
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Rimuovi dalla coda",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
