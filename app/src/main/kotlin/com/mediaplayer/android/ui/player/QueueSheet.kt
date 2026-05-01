package com.mediaplayer.android.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.playback.PlaybackViewModel
import com.mediaplayer.android.playback.QueueEntry
import com.mediaplayer.android.ui.common.EmptyState
import com.mediaplayer.android.ui.playlists.AddToPlaylistSheet
import com.mediaplayer.android.ui.search.SongRow

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun QueueSheet(
    viewModel: PlaybackViewModel,
    onDismiss: () -> Unit,
) {
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    var sheetSong by remember { mutableStateOf<SongDto?>(null) }

    // Spotify-style split: hide everything before the current item,
    // surface the user queue first ("Next in queue"), then the rest of
    // the source ("Next up"). The two sub-lists are derived purely from
    // the QueueEntry flags so the UI stays in sync with whatever the
    // service is publishing.
    val current = queue.firstOrNull { it.isCurrent }
    val ahead = queue.filter { it.index > (current?.index ?: -1) }
    val userAhead = ahead.filter { it.userQueued }
    val sourceAhead = ahead.filter { !it.userQueued }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Text(
                text = "Up next",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            if (queue.isEmpty()) {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    title = "Queue is empty",
                    subtitle = "Tap a song to start playback.",
                )
            } else {
                LazyColumn {
                    if (current != null) {
                        item(key = "section-now") {
                            QueueSectionHeader("Now playing")
                        }
                        item(key = "now-${current.index}-${current.song.id}") {
                            QueueRow(
                                entry = current,
                                onClick = { viewModel.skipToQueueItem(current.index) },
                                onMore = { sheetSong = current.song },
                                onRemove = null,
                                highlight = true,
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                    if (userAhead.isNotEmpty()) {
                        item(key = "section-user") {
                            QueueSectionHeader("Next in queue")
                        }
                        items(items = userAhead, key = { "uq-${it.index}-${it.song.id}" }) { entry ->
                            QueueRow(
                                entry = entry,
                                onClick = { viewModel.skipToQueueItem(entry.index) },
                                onMore = { sheetSong = entry.song },
                                onRemove = { viewModel.removeFromQueue(entry.index) },
                                highlight = false,
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                    if (sourceAhead.isNotEmpty()) {
                        item(key = "section-source") {
                            QueueSectionHeader("Next up")
                        }
                        items(items = sourceAhead, key = { "src-${it.index}-${it.song.id}" }) { entry ->
                            QueueRow(
                                entry = entry,
                                onClick = { viewModel.skipToQueueItem(entry.index) },
                                onMore = { sheetSong = entry.song },
                                onRemove = { viewModel.removeFromQueue(entry.index) },
                                highlight = false,
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }

    sheetSong?.let { song ->
        AddToPlaylistSheet(
            songTitle = song.title,
            songId = song.id,
            onDismiss = { sheetSong = null },
        )
    }
}

@Composable
private fun QueueSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
    )
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
            modifier = Modifier.weight(1f),
        )
        if (onRemove != null) {
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Remove from queue",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
