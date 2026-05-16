package com.mediaplayer.android.ui.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddToPhotos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.ThumbDown
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mediaplayer.android.data.LikedSongsCache
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mediaplayer.android.data.DislikedSongsCache
import com.mediaplayer.android.data.PlaylistsCache
import com.mediaplayer.android.data.dto.PlaylistDto
import com.mediaplayer.android.ui.common.EmptyState
import com.mediaplayer.android.ui.common.friendlyMessage
import com.mediaplayer.android.ui.theme.CoverShapes
import kotlinx.coroutines.launch

/**
 * "Add <song> to playlist" bottom sheet.
 *
 * Triggered from a long-press on a [SongRow][com.mediaplayer.android.ui.search.SongRow].
 * Loads the user's playlists on open so the list is always fresh — a
 * new playlist created from Playlists tab should be immediately pickable.
 *
 * UI contract:
 *  - Tap a playlist row -> add song, close sheet.
 *  - Tap "+ New playlist" row -> inline create dialog, then add song.
 *
 * Self-contained in terms of data: owns a [PlaylistRepository] directly
 * rather than piping through a ViewModel. The sheet is short-lived and
 * has no state worth preserving across config changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    songTitle: String,
    songId: Long,
    songArtist: String = "",
    songHasCoverArt: Boolean = true,
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
    onDislikeSong: (() -> Unit)? = null,
    onDislikeArtist: (() -> Unit)? = null,
    onFlagWrong: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onAdded: (playlistName: String) -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val cachedPlaylists by PlaylistsCache.playlists.collectAsStateWithLifecycle()
    val cacheLoading by PlaylistsCache.initialLoading.collectAsStateWithLifecycle()
    val cacheError by PlaylistsCache.listError.collectAsStateWithLifecycle()
    val cachedDetails by PlaylistsCache.details.collectAsStateWithLifecycle()
    val playlists = remember(cachedPlaylists) { cachedPlaylists.filterNot { it.isAuto } }
    val loading = cacheLoading && playlists.isEmpty()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    // Reset errorMessage when cacheError resolves so a successful retry no
    // longer leaves the stale error toast on screen.
    LaunchedEffect(cacheError, playlists.isEmpty()) {
        errorMessage = cacheError
            ?.takeIf { playlists.isEmpty() }
            ?.let(::friendlyMessage)
    }
    var createOpen by remember { mutableStateOf(false) }
    var flagConfirmOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { PlaylistsCache.primeIfEmpty() }

    // Set of playlist ids the song already belongs to. Backed by whatever
    // detail entries have been loaded — primary source is the kebab being
    // opened on a playlist's detail screen. We don't fan out N detail
    // requests just to populate checkmarks; a missing checkmark is a
    // false negative, never a false positive.
    val membershipIds = remember(cachedDetails, songId) {
        cachedDetails.values
            .filter { detail -> detail.songs.any { it.song.id == songId } }
            .mapTo(hashSetOf()) { it.id }
    }

    fun addTo(playlist: PlaylistDto) {
        scope.launch {
            try {
                PlaylistsCache.addSong(playlist.id, songId)
                onAdded(playlist.name)
                onDismiss()
            } catch (t: Throwable) {
                errorMessage = "Couldn't add: ${t.message ?: "unknown error"}"
            }
        }
    }

    var query by remember { mutableStateOf("") }
    val mono = com.mediaplayer.android.ui.theme.LocalMHMono.current
    val accent = MaterialTheme.colorScheme.primary
    val filteredPlaylists = remember(playlists, query) {
        if (query.isBlank()) playlists
        else playlists.filter { it.name.contains(query, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header — eyebrow + title (mockup `mh-player-sheets.jsx:318-358`).
            Text(
                text = "// AGGIUNGI A",
                style = mono.eyebrow,
                color = accent,
                modifier = Modifier.padding(start = 24.dp, top = 8.dp),
            )
            Spacer(Modifier.size(2.dp))
            Text(
                text = "Le mie playlist",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Text(
                text = songTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )

            // Auxiliary track-actions block. Like row is always present so
            // every kebab surface (artist/album/playlist/queue/liked/search)
            // can like-toggle the song without a separate plumbing per
            // screen. The other rows still gate on caller-supplied callbacks.
            LaunchedEffect(songId) { LikedSongsCache.prime(listOf(songId)) }
            val likedIds by LikedSongsCache.likedIds.collectAsStateWithLifecycle()
            val isLiked = songId in likedIds
            Spacer(Modifier.size(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            QueueActionRow(
                label = if (isLiked) "Rimuovi dai preferiti" else "Aggiungi ai preferiti",
                icon = {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isLiked) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurface,
                    )
                },
                onClick = {
                    val label = listOfNotNull(
                        songTitle.takeIf { it.isNotBlank() },
                        songArtist.takeIf { it.isNotBlank() },
                    ).joinToString(" — ").ifBlank { null }
                    LikedSongsCache.toggle(songId, label)
                    onDismiss()
                },
            )
            val anyAux = onPlayNext != null || onAddToQueue != null ||
                onDownload != null || onDislikeSong != null ||
                onDislikeArtist != null || onFlagWrong != null
            if (anyAux) {
                onPlayNext?.let {
                    QueueActionRow(
                        label = "Riproduci dopo",
                        icon = { Icon(Icons.Filled.SkipNext, contentDescription = null) },
                        onClick = { it(); onDismiss() },
                    )
                }
                onAddToQueue?.let {
                    QueueActionRow(
                        label = "Aggiungi alla coda",
                        icon = { Icon(Icons.Filled.AddToPhotos, contentDescription = null) },
                        onClick = { it(); onDismiss() },
                    )
                }
                onDownload?.let {
                    QueueActionRow(
                        label = "Scarica",
                        icon = { Icon(Icons.Filled.FileDownload, contentDescription = null) },
                        onClick = { it(); onDismiss() },
                    )
                }
                onDislikeSong?.let {
                    val dislikedSongs by DislikedSongsCache.dislikedSongIds.collectAsStateWithLifecycle()
                    LaunchedEffect(songId) { DislikedSongsCache.primeSongs(listOf(songId)) }
                    val songAlreadyExcluded = songId in dislikedSongs
                    QueueActionRow(
                        // Switch the label + tint when the song is already
                        // excluded so the user knows the action is a no-op
                        // rather than letting them re-click into the void.
                        label = if (songAlreadyExcluded) "Brano già escluso"
                                else "Non consigliarmi questo brano",
                        icon = {
                            Icon(
                                Icons.Filled.ThumbDown,
                                contentDescription = null,
                                tint = if (songAlreadyExcluded) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        onClick = {
                            if (!songAlreadyExcluded) it()
                            onDismiss()
                        },
                    )
                }
                onDislikeArtist?.let {
                    val dislikedArtists by DislikedSongsCache.dislikedArtists.collectAsStateWithLifecycle()
                    LaunchedEffect(Unit) { DislikedSongsCache.primeArtists() }
                    val artistAlreadyExcluded =
                        songArtist.trim().lowercase() in dislikedArtists
                    QueueActionRow(
                        label = if (artistAlreadyExcluded) "Artista già escluso"
                                else "Non consigliarmi questo artista",
                        icon = {
                            Icon(
                                Icons.Filled.PersonOff,
                                contentDescription = null,
                                tint = if (artistAlreadyExcluded) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        onClick = {
                            if (!artistAlreadyExcluded) it()
                            onDismiss()
                        },
                    )
                }
                onFlagWrong?.let { _ ->
                    QueueActionRow(
                        label = "Segnala brano sbagliato",
                        icon = {
                            Icon(
                                Icons.Filled.ReportProblem,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = { flagConfirmOpen = true },
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Search-by-playlist-name field.
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Cerca playlist…") },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                    )
                },
                trailingIcon = if (query.isNotEmpty()) {
                    @Composable {
                        androidx.compose.material3.IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancella")
                        }
                    }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Scrollable list area — capped so the sticky CTA is always reachable.
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
                    errorMessage != null && playlists.isEmpty() -> Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    filteredPlaylists.isEmpty() && playlists.isEmpty() -> EmptyState(
                        icon = Icons.AutoMirrored.Filled.QueueMusic,
                        title = "Nessuna playlist",
                        subtitle = "Crea la tua prima playlist per iniziare.",
                    )
                    filteredPlaylists.isEmpty() -> Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Nessuna playlist per “$query”.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    else -> LazyColumn {
                        items(items = filteredPlaylists, key = { it.id }) { p ->
                            PlaylistPickerRow(
                                playlist = p,
                                alreadyContainsSong = p.id in membershipIds,
                                onClick = { addTo(p) },
                            )
                        }
                    }
                }
            }

            if (errorMessage != null && playlists.isNotEmpty()) {
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                )
            }

            // Sticky bottom CTA — outlined dashed lime per mockup.
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(
                        width = 1.dp,
                        color = accent.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .clickable { createOpen = true }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = accent,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Crea nuova playlist",
                        style = MaterialTheme.typography.labelLarge,
                        color = accent,
                    )
                }
            }
        }
    }

    if (flagConfirmOpen && onFlagWrong != null) {
        com.mediaplayer.android.ui.common.FlagWrongConfirmDialog(
            songId = songId,
            songTitle = songTitle,
            songArtist = songArtist,
            hasCoverArt = songHasCoverArt,
            onConfirm = {
                flagConfirmOpen = false
                onFlagWrong()
                onDismiss()
            },
            onDismiss = { flagConfirmOpen = false },
        )
    }

    if (createOpen) {
        CreateAndAddDialog(
            onDismiss = { createOpen = false },
            onConfirm = { name ->
                scope.launch {
                    try {
                        val created = PlaylistsCache.create(name)
                        PlaylistsCache.addSong(created.id, songId)
                        onAdded(created.name)
                        createOpen = false
                        onDismiss()
                    } catch (t: Throwable) {
                        errorMessage = "Couldn't create: ${t.message ?: "unknown error"}"
                        createOpen = false
                    }
                }
            },
        )
    }
}

@Composable
private fun PlaylistPickerRow(
    playlist: PlaylistDto,
    alreadyContainsSong: Boolean,
    onClick: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Real cover when the playlist has a backing song; auto-playlists keep
        // their gradient palette; user playlists with no coverSongId fall back
        // to the queue-music icon.
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CoverShapes.SongRow)
                .background(
                    if (playlist.isAuto) {
                        com.mediaplayer.android.ui.common.autoPlaylistGradient(playlist.kind)
                    } else {
                        androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.surfaceVariant)
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            val coverId = playlist.coverSongId
            if (coverId != null && !playlist.isAuto) {
                coil3.compose.AsyncImage(
                    model = com.mediaplayer.android.data.Network.coverUrl(coverId),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = null,
                    tint = if (playlist.isAuto) androidx.compose.ui.graphics.Color.White
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (playlist.songCount == 1) "1 brano" else "${playlist.songCount} brani",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        if (alreadyContainsSong) {
            // Filled lime circle with check — the song is already in this
            // playlist. Tapping still re-adds (backend dedupes), but the
            // affordance tells the user nothing useful will happen.
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(accent, shape = androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Già in questa playlist",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp),
                )
            }
        } else {
            // Empty circle — empty radio dot to signal a tappable destination.
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .border(
                        width = 1.5.dp,
                        color = accent.copy(alpha = 0.6f),
                        shape = androidx.compose.foundation.shape.CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun QueueActionRow(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CoverShapes.SongRow)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun CreateAndAddDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuova playlist") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                placeholder = { Text("Nome playlist") },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.trim()) },
                enabled = text.trim().isNotEmpty(),
            ) { Text("Crea e aggiungi") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        },
    )
}
