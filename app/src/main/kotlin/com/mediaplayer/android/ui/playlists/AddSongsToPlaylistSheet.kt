package com.mediaplayer.android.ui.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.data.PlaylistRepository
import com.mediaplayer.android.data.SongRepository
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.ui.common.EmptyState
import com.mediaplayer.android.ui.common.friendlyMessage
import com.mediaplayer.android.ui.theme.CoverShapes
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Concurrent fan-out cap for the per-song add path. Bounded so we don't
// open 50+ sockets against the backend at once, but high enough that the
// happy path completes in ~ceil(N/8) RTTs instead of N sequential RTTs.
private const val CONCURRENT_ADD_BATCH = 8

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSongsToPlaylistSheet(
    playlistId: Long,
    playlistName: String = "",
    existingSongIds: Set<Long>,
    playlistRepository: PlaylistRepository = remember { PlaylistRepository() },
    songRepository: SongRepository = remember { SongRepository() },
    onDismiss: () -> Unit,
    onSongAdded: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var songs by remember { mutableStateOf<List<SongDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var committing by remember { mutableStateOf(false) }
    val addedIds = remember { mutableStateSetOf<Long>().also { it.addAll(existingSongIds) } }
    val selectedIds = remember { mutableStateSetOf<Long>() }
    val mono = com.mediaplayer.android.ui.theme.LocalMHMono.current
    val accent = MaterialTheme.colorScheme.primary

    LaunchedEffect(query) {
        // Show the spinner immediately so the debounce window doesn't look
        // like a frozen UI; the previous version waited the full 300 ms
        // before flipping `loading`.
        loading = true
        delay(300)
        val trimmed = query.trim()
        // Single-character queries fan out the full catalog and rarely
        // mean what the user wanted. Treat them as "still typing".
        if (trimmed.isNotEmpty() && trimmed.length < 2) {
            songs = emptyList()
            loading = false
            return@LaunchedEffect
        }
        errorMessage = null
        try {
            songs = songRepository.listSongs(
                query = trimmed.takeIf { it.isNotEmpty() },
                size = 50,
            ).items
        } catch (t: Throwable) {
            errorMessage = friendlyMessage(t)
        } finally {
            loading = false
        }
    }

    fun commitSelection() {
        if (committing || selectedIds.isEmpty()) return
        committing = true
        scope.launch {
            try {
                // Backend has no bulk endpoint — fan out N requests with a
                // bounded concurrency window via async/awaitAll so the user
                // doesn't pay N sequential RTTs (adding 50 songs used to
                // take 50 round-trips, ~25s on a slow connection).
                // Failures stay in `selectedIds` so a retry tap re-attempts
                // only the failed ids; successes move into `addedIds`.
                val toAdd = selectedIds.toList()
                var successCount = 0
                var failureCount = 0
                var lastError: String? = null
                toAdd.chunked(CONCURRENT_ADD_BATCH).forEach { chunk ->
                    val results = chunk.map { songId ->
                        async {
                            songId to runCatching {
                                playlistRepository.addSong(playlistId, songId)
                            }
                        }
                    }.awaitAll()
                    for ((songId, result) in results) {
                        result.onSuccess {
                            addedIds.add(songId)
                            selectedIds.remove(songId)
                            successCount++
                        }.onFailure {
                            failureCount++
                            lastError = friendlyMessage(it)
                        }
                    }
                }
                // Build a discriminating message instead of overwriting with
                // only the last failure — the old branch dismissed on full
                // success but stayed open with a single error otherwise,
                // dropping all context on how many succeeded.
                errorMessage = when {
                    failureCount == 0 -> null
                    successCount == 0 -> lastError
                    else -> "$successCount aggiunti, $failureCount non riusciti."
                }
                if (failureCount == 0) {
                    onSongAdded()
                    onDismiss()
                } else {
                    onSongAdded()
                }
            } finally {
                committing = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Eyebrow includes the target playlist name when available.
            val eyebrowLabel = if (playlistName.isNotBlank()) {
                "// AGGIUNGI A · ${playlistName.uppercase()}"
            } else "// AGGIUNGI A"
            Text(
                text = eyebrowLabel,
                style = mono.eyebrow,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 24.dp, top = 8.dp),
            )
            Spacer(Modifier.size(2.dp))
            Text(
                text = "Aggiungi brani",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Cerca brani") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancella")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Scrollable picker — capped so the sticky CTA stays reachable.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
            ) {
                when {
                    loading -> Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    }
                    errorMessage != null -> Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    songs.isEmpty() -> EmptyState(
                        icon = Icons.Filled.MusicNote,
                        title = if (query.isBlank()) "La tua libreria è vuota" else "Nessun brano per “$query”",
                        subtitle = if (query.isBlank()) "Aggiungi brani al catalogo per vederli qui." else null,
                    )
                    else -> LazyColumn {
                        items(items = songs, key = { it.id }) { song ->
                            SongPickerRow(
                                song = song,
                                isAdded = song.id in addedIds,
                                isSelected = song.id in selectedIds,
                                onToggle = {
                                    if (song.id in addedIds) return@SongPickerRow
                                    if (song.id in selectedIds) selectedIds.remove(song.id)
                                    else selectedIds.add(song.id)
                                },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }

            // Sticky bottom CTA — disabled while no selection or while committing.
            val ctaEnabled = selectedIds.isNotEmpty() && !committing
            val ctaModifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(
                    color = if (ctaEnabled) accent else accent.copy(alpha = 0.25f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                )
                .let { if (ctaEnabled) it.clickable { commitSelection() } else it }
                .padding(vertical = 14.dp)
            Box(
                modifier = ctaModifier,
                contentAlignment = Alignment.Center,
            ) {
                val n = selectedIds.size
                val label = when {
                    committing -> "Aggiungo…"
                    n == 0 -> "Seleziona almeno un brano"
                    n == 1 -> "Aggiungi 1 brano"
                    else -> "Aggiungi $n brani"
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = androidx.compose.ui.graphics.Color(0xFF0A0A0A),
                )
            }
        }
    }
}

@Composable
private fun SongPickerRow(
    song: SongDto,
    isAdded: Boolean,
    isSelected: Boolean,
    onToggle: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isAdded, onClick = onToggle)
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Leading checkbox — empty / accent-filled / "Già aggiunto" check.
        Box(
            modifier = Modifier
                .size(22.dp)
                .border(
                    width = 1.5.dp,
                    color = when {
                        isAdded -> accent.copy(alpha = 0.4f)
                        isSelected -> accent
                        else -> MaterialTheme.colorScheme.outline
                    },
                    shape = RoundedCornerShape(4.dp),
                )
                .background(
                    color = if (isSelected || isAdded) accent.copy(alpha = 0.15f)
                    else androidx.compose.ui.graphics.Color.Transparent,
                    shape = RoundedCornerShape(4.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isAdded || isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CoverShapes.SongRow)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (song.hasCoverArt) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(Network.coverUrl(song.id))
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = formatDurationMs(song.durationMs),
            style = com.mediaplayer.android.ui.theme.LocalMHMono.current.duration,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatDurationMs(ms: Long): String {
    if (ms <= 0L) return "—"
    val totalSeconds = ms / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return java.lang.String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
}
