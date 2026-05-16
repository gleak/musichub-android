package com.mediaplayer.android.ui.playlists

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.playback.PlaybackViewModel
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mediaplayer.android.data.dto.PlaylistDetailDto
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.ui.common.CenteredMessage
import com.mediaplayer.android.ui.common.EmptyState
import com.mediaplayer.android.ui.common.ErrorWithRetry
import com.mediaplayer.android.ui.common.SongListShimmer
import com.mediaplayer.android.ui.common.SpotifyHero
import com.mediaplayer.android.ui.search.SongRow
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.ui.graphics.Color
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    onBack: () -> Unit,
    onPlayFromIndex: (List<SongDto>, Int) -> Unit,
    onShufflePlay: (List<SongDto>) -> Unit,
    onOpenMembers: (playlistId: Long, isOwner: Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val viewModel: PlaylistDetailViewModel = viewModel(
        key = "playlist-$playlistId",
        factory = viewModelFactory {
            initializer { PlaylistDetailViewModel(playlistId) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val downloadedIds by viewModel.downloadedIds.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val kebab = com.mediaplayer.android.ui.common.rememberSongKebab()
    var addSongsOpen by remember { mutableStateOf(false) }
    var shareOpen by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    var snackMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(snackMessage) {
        val msg = snackMessage ?: return@LaunchedEffect
        snackbar.showSnackbar(msg)
        snackMessage = null
    }

    val successState = state as? PlaylistDetailUiState.Success

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    if (successState != null) {
                        IconButton(
                            onClick = viewModel::pullRefresh,
                            enabled = !isRefreshing,
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.Refresh, contentDescription = "Aggiorna")
                            }
                        }
                        // Only the owner can mint share links — members of a
                        // shared playlist must ask the owner to invite a third
                        // user. Hiding the icon for non-owners avoids the user
                        // getting a 404 when they try.
                        if (successState.playlist.isOwner) {
                            IconButton(onClick = { shareOpen = true }) {
                                Icon(Icons.Filled.Share, contentDescription = "Condividi playlist")
                            }
                        }
                        if (!successState.playlist.isAuto) {
                            IconButton(onClick = { addSongsOpen = true }) {
                                Icon(Icons.Filled.Add, contentDescription = "Aggiungi brani")
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                PlaylistDetailUiState.Loading -> SongListShimmer()
                is PlaylistDetailUiState.Error -> ErrorWithRetry(
                    message = "Couldn't load playlist.\n${s.message}",
                    onRetry = viewModel::refresh,
                )
                is PlaylistDetailUiState.Success -> {
                    val songIds = s.playlist.songs.map { it.song.id }
                    val downloadedCount = songIds.count { it in downloadedIds }
                    PlaylistDetailBody(
                        playlist = s.playlist,
                        downloadedCount = downloadedCount,
                        downloadedIds = downloadedIds,
                        playlistSongIds = songIds.toSet(),
                        onPlayFromIndex = onPlayFromIndex,
                        onShufflePlay = onShufflePlay,
                        onRemoveSong = { songId ->
                            viewModel.removeSong(songId)
                            snackMessage = "Removed from playlist"
                        },
                        onReorderSongs = viewModel::reorderSongs,
                        onLongPressSong = { kebab.open(it) },
                        onDownload = {
                            viewModel.downloadPlaylist()
                            val cm = context.getSystemService(ConnectivityManager::class.java)
                            val caps = cm.getNetworkCapabilities(cm.activeNetwork)
                            val onWifi = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == true
                            if (!onWifi) snackMessage = "Download queued — will start on Wi-Fi"
                        },
                        onRemoveDownloads = viewModel::removePlaylistDownloads,
                        onToggleAutoSync = viewModel::toggleAutoSync,
                        onManageMembers = {
                            onOpenMembers(playlistId, s.playlist.isOwner)
                        },
                        onLeavePlaylist = {
                            viewModel.deleteOrLeave { ok, _ ->
                                if (ok) onBack() else snackMessage = "Impossibile rimuovere la playlist"
                            }
                        },
                    )
                }
            }
        }
    }

    com.mediaplayer.android.ui.common.SongKebabSheet(
        state = kebab,
        onFlagged = { viewModel.refresh() },
        onAdded = { playlistName, _ -> snackMessage = "Added to $playlistName" },
    )

    if (addSongsOpen && successState != null) {
        AddSongsToPlaylistSheet(
            playlistId = playlistId,
            playlistName = successState.playlist.name,
            existingSongIds = successState.playlist.songs.map { it.song.id }.toSet(),
            onDismiss = { addSongsOpen = false },
            onSongAdded = viewModel::refresh,
        )
    }

    if (shareOpen && successState != null && successState.playlist.isOwner) {
        PlaylistShareSheet(
            playlistId = playlistId,
            playlistName = successState.playlist.name,
            memberCount = successState.playlist.memberCount,
            onDismiss = { shareOpen = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistDetailBody(
    playlist: PlaylistDetailDto,
    downloadedCount: Int,
    downloadedIds: Set<Long>,
    playlistSongIds: Set<Long>,
    onPlayFromIndex: (List<SongDto>, Int) -> Unit,
    onShufflePlay: (List<SongDto>) -> Unit,
    onRemoveSong: (Long) -> Unit,
    onReorderSongs: (List<Long>) -> Unit,
    onLongPressSong: (SongDto) -> Unit,
    onDownload: () -> Unit,
    onRemoveDownloads: () -> Unit,
    onToggleAutoSync: () -> Unit,
    onManageMembers: () -> Unit,
    onLeavePlaylist: () -> Unit,
) {
    val playbackVm: PlaybackViewModel = viewModel()
    val playerIsPlaying by playbackVm.isPlaying.collectAsStateWithLifecycle()
    val playerCurrentSong by playbackVm.currentSong.collectAsStateWithLifecycle()
    // "Playing this playlist" = player is in a play state AND its current
    // track belongs to this playlist. Drives the hero's play→pause toggle
    // so a tap visibly switches to the pause icon instead of relooping
    // playback from track 0.
    val playingFromHere = playerIsPlaying && (playerCurrentSong?.id in playlistSongIds)
    val lazyListState = rememberLazyListState()
    // `entries` holds PlaylistSongEntryDto so each row carries its
    // playlist_songs.id — that's the stable per-occurrence key the
    // LazyColumn needs for `Modifier.animateItem()` to fire on reorder
    // (song.id alone collides on duplicate songs).
    var entries by remember { mutableStateOf(playlist.songs) }
    val songsForPlayback: List<SongDto> = entries.map { it.song }
    // Key the prime + sync effects on a stable id-list hash. The previous
    // List<*> key triggered on every cache refresh because each refresh
    // produces a fresh list instance — re-priming hundreds of liked rows
    // on every position tick was wasted work, and the sync effect kept
    // resetting `entries` whenever the cache emitted, even if nothing had
    // actually changed.
    val playlistSongIdsHash = remember(playlist.songs) {
        playlist.songs.fold(0) { acc, e -> acc * 31 + e.playlistSongId.hashCode() }
    }
    LaunchedEffect(playlistSongIdsHash) {
        com.mediaplayer.android.data.LikedSongsCache.prime(songsForPlayback.map { it.id })
    }

    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        // Subtract 1 to account for the header item at LazyColumn index 0.
        val fromIndex = from.index - 1
        val toIndex = to.index - 1
        if (fromIndex !in entries.indices || toIndex !in entries.indices) return@rememberReorderableLazyListState
        entries = entries.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
    }

    // Sync from server only when no drag is in flight. Without the guard a
    // mid-drag refresh would yank the local list out from under the gesture.
    LaunchedEffect(playlistSongIdsHash) {
        if (!reorderState.isAnyItemDragging) entries = playlist.songs
    }

    LaunchedEffect(reorderState) {
        var wasEverDragging = false
        snapshotFlow { reorderState.isAnyItemDragging }.collect { dragging ->
            if (dragging) {
                wasEverDragging = true
            } else if (wasEverDragging) {
                onReorderSongs(entries.map { it.song.id })
                wasEverDragging = false
            }
        }
    }

    LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize()) {
        item(key = "header") {
            val total = playlist.songs.size
            val allDownloaded = total > 0 && downloadedCount == total
            val totalDurationMs = playlist.songs.sumOf { it.song.durationMs }
            val durationLabel = formatPlaylistDuration(totalDurationMs)
            val subtitleStr = buildString {
                if (playlist.isAuto) {
                    append(com.mediaplayer.android.ui.common.familyOf(playlist.kind).label)
                    append(" · ")
                    append(pluralizeSongsDetail(total))
                    if (durationLabel.isNotEmpty()) append(" · $durationLabel")
                    // Surface the actual regeneration timestamp directly in
                    // the hero so every auto-playlist tile shows it without
                    // requiring the user to scroll past the cards. Falls
                    // through silently when the field is null (shell created
                    // by the bootstrapper but not yet refreshed).
                    val refreshed = com.mediaplayer.android.ui.common.formatRefreshedAt(
                        playlist.lastRefreshedAt
                    )
                    if (refreshed != null) append(" · Rigenerata $refreshed")
                } else {
                    append("Playlist · ")
                    append(pluralizeSongsDetail(total))
                    if (durationLabel.isNotEmpty()) append(" · $durationLabel")
                    if (total > 0) {
                        append(
                            when {
                                allDownloaded -> " · Tutti scaricati"
                                downloadedCount > 0 -> " · $downloadedCount/$total scaricati"
                                else -> " · 0/$total scaricati"
                            }
                        )
                    }
                    if (!playlist.isOwner && !playlist.ownerName.isNullOrBlank()) {
                        append(" · Condivisa da ${playlist.ownerName}")
                    } else if (playlist.isOwner && playlist.memberCount > 0) {
                        append(
                            if (playlist.memberCount == 1) " · Condivisa con 1 persona"
                            else " · Condivisa con ${playlist.memberCount} persone"
                        )
                    }
                }
            }
            // Auto-playlists get a stylized collage hero (mirrors the tile in
            // PlaylistsScreen / ForYou). User playlists adopt the first
            // song-with-cover as the hero artwork — the same convention
            // PlaylistDto.coverSongId uses on the list view.
            val heroCoverSongId = if (playlist.isAuto) null
                else playlist.songs.firstOrNull { it.song.hasCoverArt }?.song?.id
            val heroEyebrow = when {
                playlist.isAuto -> "PER TE · GENERATA"
                playlist.isShared -> "PLAYLIST · COLLABORATIVA"
                else -> null
            }
            val autoCoverIds: List<Long> = if (playlist.isAuto) {
                playlist.songs
                    .asSequence()
                    .map { it.song }
                    .filter { it.hasCoverArt }
                    .map { it.id }
                    .distinct()
                    .take(4)
                    .toList()
            } else emptyList()
            SpotifyHero(
                title = playlist.name,
                subtitle = subtitleStr,
                coverModel = heroCoverSongId?.let { Network.coverUrl(it) },
                eyebrow = heroEyebrow,
                onPlay = {
                    if (playingFromHere) playbackVm.togglePlayPause()
                    else if (entries.isNotEmpty()) onPlayFromIndex(songsForPlayback, 0)
                },
                onShuffle = { onShufflePlay(songsForPlayback) },
                playEnabled = entries.isNotEmpty(),
                isPlaying = playingFromHere,
                customCover = if (playlist.isAuto) {
                    { size ->
                        com.mediaplayer.android.ui.common.CollageCover(
                            kind = playlist.kind,
                            badge = com.mediaplayer.android.ui.common.badgeFor(playlist.kind),
                            songIds = autoCoverIds,
                            modifier = androidx.compose.ui.Modifier.size(size),
                        )
                    }
                } else null,
                extraActions = {
                    if (playlist.songs.isNotEmpty()) {
                        IconButton(
                            onClick = if (allDownloaded) onRemoveDownloads else onDownload,
                        ) {
                            Icon(
                                imageVector = if (allDownloaded) Icons.Filled.CloudDone else Icons.Filled.CloudDownload,
                                contentDescription = if (allDownloaded) "Rimuovi scaricati" else "Scarica",
                                tint = if (allDownloaded) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                },
            )
        }

        if (playlist.isAuto) {
            item(key = "auto_meta") {
                AutoPlaylistMetaStrip(
                    family = com.mediaplayer.android.ui.common.familyOf(playlist.kind),
                    songCount = playlist.songs.size,
                    lastRefreshedAt = playlist.lastRefreshedAt,
                )
            }
        } else {
            if (playlist.isShared) {
                item(key = "members_strip") {
                    com.mediaplayer.android.ui.common.MembersStripCard(
                        isOwner = playlist.isOwner,
                        ownerName = playlist.ownerName,
                        memberCount = playlist.memberCount,
                        onManage = onManageMembers,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
            // Auto-sync is a per-user preference for collaborative playlists
            // since v0.13.0 — owner persists to playlists.auto_sync, members
            // persist to playlist_members.auto_sync. Each user's toggle is
            // independent: my "always download" doesn't surprise your phone
            // into doing the same.
            item(key = "auto_sync") {
                AutoSyncCard(
                    enabled = playlist.autoSync,
                    onToggle = onToggleAutoSync,
                )
            }
            // Non-owner members get an inline ghost CTA per the mockup
            // (`mh-library.jsx:303-309`). Same destination as the long-press
            // dialog on the library landing — keeps two entry points in sync.
            if (!playlist.isOwner && playlist.isShared) {
                item(key = "leave_cta") {
                    LeavePlaylistButton(
                        playlistName = playlist.name,
                        ownerName = playlist.ownerName,
                        onConfirm = onLeavePlaylist,
                    )
                }
            }
        }

        if (entries.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    title = "Nessun brano",
                    subtitle = "Aggiungi brani dalla ricerca o tocca +.",
                )
            }
        } else {
            itemsIndexed(
                items = entries,
                key = { _, entry -> entry.playlistSongId },
            ) { idx, entry ->
                val song = entry.song
                ReorderableItem(reorderState, key = entry.playlistSongId) {
                    // Canonical Material3 pattern: don't fire side effects from
                    // confirmValueChange (it can be invoked multiple times during
                    // a single gesture, and ignored values get re-tried). Observe
                    // currentValue settling instead. `removed` guards re-entry
                    // because the row stays composed until server response trims
                    // `entries`.
                    var removed by remember(entry.playlistSongId) { mutableStateOf(false) }
                    val dismissState = rememberSwipeToDismissBoxState()
                    LaunchedEffect(dismissState.currentValue) {
                        if (!removed && dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                            removed = true
                            // Drop the row locally first so the LazyColumn unmounts
                            // it instead of leaving the SwipeToDismissBox stuck in
                            // its dismissed (background-only) state while the server
                            // round-trip is in flight. On API failure the VM's
                            // refresh() path re-syncs and the row reappears.
                            entries = entries.filterNot { it.playlistSongId == entry.playlistSongId }
                            onRemoveSong(song.id)
                        }
                    }
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        // Auto-playlists are server-managed; swipe-remove and
                        // reorder both no-op upstream, so suppressing the
                        // affordances avoids the misleading "I removed it but
                        // it's still here" feel.
                        enableDismissFromEndToStart = !playlist.isAuto,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .padding(end = 16.dp),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Rimuovi",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        },
                    ) {
                        // Contributor pill only when collaborative AND the
                        // adder isn't the playlist owner — owner attribution
                        // would clutter every row on a single-author playlist.
                        val contributorTag = entry.addedByName
                            ?.takeIf { playlist.isShared && entry.addedByUserId != playlist.ownerId }
                        SongRow(
                            song = song,
                            isDownloaded = song.id in downloadedIds,
                            onClick = { onPlayFromIndex(songsForPlayback, idx) },
                            onMore = { onLongPressSong(song) },
                            contributorTag = contributorTag,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface),
                            rowGestureModifier = if (!playlist.isAuto)
                                Modifier.longPressDraggableHandle()
                            else Modifier,
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}


private fun pluralizeSongsDetail(count: Int): String =
    if (count == 1) "1 brano" else "$count brani"

private fun formatPlaylistDuration(totalMs: Long): String {
    if (totalMs <= 0L) return ""
    val totalMinutes = (totalMs / 60_000L).toInt()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours <= 0 -> "${minutes} min"
        minutes == 0 -> "${hours} h"
        else -> "${hours} h ${minutes} min"
    }
}

@Composable
private fun AutoSyncCard(
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(com.mediaplayer.android.ui.theme.MHColors.Card)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.CloudDownload,
            contentDescription = null,
            tint = if (enabled) com.mediaplayer.android.ui.theme.MHColors.Lime
                   else com.mediaplayer.android.ui.theme.MHColors.TextLo,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Sincronizzazione automatica",
                style = MaterialTheme.typography.bodyMedium,
                color = com.mediaplayer.android.ui.theme.MHColors.TextHi,
            )
            Text(
                text = "Scarica i nuovi brani all'apertura dell'app. Disattivata per impostazione predefinita.",
                style = MaterialTheme.typography.bodySmall,
                color = com.mediaplayer.android.ui.theme.MHColors.TextLo,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = enabled,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = com.mediaplayer.android.ui.theme.MHColors.TextHi,
                checkedTrackColor = com.mediaplayer.android.ui.theme.MHColors.Lime,
                uncheckedThumbColor = com.mediaplayer.android.ui.theme.MHColors.TextLo,
                uncheckedTrackColor = com.mediaplayer.android.ui.theme.MHColors.Card,
            ),
        )
    }
}

@Composable
private fun AutoPlaylistMetaStrip(
    family: com.mediaplayer.android.ui.common.AutoPlaylistFamily,
    songCount: Int,
    lastRefreshedAt: String?,
) {
    val mono = com.mediaplayer.android.ui.theme.LocalMHMono.current
    val refreshLabel = com.mediaplayer.android.ui.common.formatRefreshedAt(lastRefreshedAt)
    // Card stays terse — "Oggi" / "Ieri" / "10 mag" — so it fits the
    // half-width MetaCard without character-by-character wrapping. The
    // full timestamp ("oggi alle 04:03") goes in the descriptive line
    // below, which has the full row width and word-wraps cleanly.
    val cardLabel = com.mediaplayer.android.ui.common.formatRefreshedAtShort(lastRefreshedAt)
        ?: "In attesa"
    val scheduleLabel =
        if (family == com.mediaplayer.android.ui.common.AutoPlaylistFamily.Radar)
            "ogni lunedì alle 05:45"
        else
            "ogni notte alle 04:00"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetaCard(
                label = "AGGIORNATA",
                value = cardLabel,
                modifier = Modifier.weight(1f),
            )
            MetaCard(
                label = "BRANI",
                value = "$songCount",
                modifier = Modifier.weight(1f),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(com.mediaplayer.android.ui.theme.MHColors.Lime.copy(alpha = 0.06f))
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    tint = com.mediaplayer.android.ui.theme.MHColors.Lime,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (refreshLabel != null)
                            "Ultima rigenerazione: $refreshLabel"
                        else
                            "Rigenerazione non ancora eseguita",
                        style = MaterialTheme.typography.bodySmall,
                        color = com.mediaplayer.android.ui.theme.MHColors.TextHi,
                    )
                    Text(
                        text = "Aggiornata automaticamente — ${family.label} · $scheduleLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = com.mediaplayer.android.ui.theme.MHColors.TextLo,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val mono = com.mediaplayer.android.ui.theme.LocalMHMono.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            style = mono.eyebrow.copy(color = com.mediaplayer.android.ui.theme.MHColors.Lime),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = com.mediaplayer.android.ui.theme.MHColors.TextHi,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
private fun LeavePlaylistButton(
    playlistName: String,
    ownerName: String?,
    onConfirm: () -> Unit,
) {
    var confirm by remember { mutableStateOf(false) }
    OutlinedButton(
        onClick = { confirm = true },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Text("Rimuovi dalla libreria")
    }
    if (confirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Rimuovi dalla libreria?") },
            text = {
                Text(
                    "\"$playlistName\" sparirà dalla tua libreria. " +
                        "Continuerà ad esistere per ${ownerName ?: "il proprietario"} " +
                        "e gli altri membri.",
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    confirm = false
                    onConfirm()
                }) { Text("Rimuovi") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { confirm = false }) { Text("Annulla") }
            },
        )
    }
}
