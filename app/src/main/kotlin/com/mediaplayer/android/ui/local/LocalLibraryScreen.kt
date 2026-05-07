package com.mediaplayer.android.ui.local

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mediaplayer.android.data.local.LocalLibraryRepository
import com.mediaplayer.android.data.local.LocalLikedStore
import com.mediaplayer.android.data.local.LocalTrack
import com.mediaplayer.android.ui.common.CenteredSpinner
import com.mediaplayer.android.ui.common.EmptyState
import com.mediaplayer.android.ui.common.PillChip
import com.mediaplayer.android.ui.theme.MHColors
import com.mediaplayer.android.ui.theme.MediaPlayerSpacing
import kotlinx.coroutines.launch

/**
 * Top-level "Sul tuo dispositivo" screen. Three tabs (Brani / Cartelle /
 * Album) sit under the header; permission gate and empty state are handled
 * inline. Backed by [LocalLibraryViewModel].
 */
@Composable
fun LocalLibraryScreen(
    onBack: () -> Unit,
    onPlayTrack: (LocalTrack, List<LocalTrack>) -> Unit,
    onShufflePlay: (List<LocalTrack>) -> Unit,
    onPlayNext: (LocalTrack) -> Unit,
    onAddToQueue: (LocalTrack) -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenLiked: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: LocalLibraryViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tab by viewModel.tab.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val likedStore = remember { LocalLikedStore.instance(context) }
    val likedIds by likedStore.liked.collectAsStateWithLifecycle(initialValue = emptySet())
    val coroutineScope = rememberCoroutineScope()

    val safLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { treeUri ->
        if (treeUri != null) {
            val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            runCatching {
                context.contentResolver.takePersistableUriPermission(treeUri, flags)
            }
            coroutineScope.launch {
                com.mediaplayer.android.data.local.LocalFolderPrefs.instance(context)
                    .add(treeUri.toString())
                viewModel.refresh()
            }
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {
            // MIUI/Xiaomi can report granted=false even when the user actually
            // granted (the OS gate races the result callback). Always re-bootstrap
            // and let hasPermission() be the source of truth.
            viewModel.bootstrap()
        },
    )

    // Re-evaluate on every resume — the user may have flipped the permission in
    // system settings, or MIUI may have propagated the grant after our launcher
    // callback already returned.
    LifecycleResumeEffect(Unit) {
        viewModel.bootstrap()
        onPauseOrDispose { }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Header(onBack = onBack)
        Spacer(Modifier.height(MediaPlayerSpacing.S))

        when (val s = state) {
            LocalLibraryViewModel.State.PermissionRequired -> {
                EmptyState(
                    icon = Icons.Filled.LibraryMusic,
                    title = "Accedi ai brani sul dispositivo",
                    subtitle = "Concedi il permesso di leggere i file audio per " +
                        "vedere musica, cartelle e album salvati sul telefono.",
                    actionLabel = "Concedi accesso",
                    onAction = {
                        val perm = LocalLibraryRepository.requiredPermission()
                        permLauncher.launch(perm)
                    },
                )
            }
            LocalLibraryViewModel.State.Loading -> CenteredSpinner()
            is LocalLibraryViewModel.State.Ready -> {
                if (s.tracks.isEmpty()) {
                    EmptyState(
                        icon = Icons.Filled.LibraryMusic,
                        title = "Nessun brano trovato",
                        subtitle = "Non ci sono file audio sul dispositivo, " +
                            "oppure sono troppo brevi (<30s).",
                    )
                    return@Column
                }

                TabBar(tab = tab, onTab = viewModel::selectTab)
                Spacer(Modifier.height(MediaPlayerSpacing.S))

                when (tab) {
                    LocalLibraryViewModel.Tab.Tracks -> TracksTab(
                        tracks = viewModel.sortedTracks(),
                        sort = sort,
                        likedIds = likedIds,
                        onSortChange = viewModel::selectSort,
                        onPlay = onPlayTrack,
                        onShuffle = onShufflePlay,
                        onPlayNext = onPlayNext,
                        onAddToQueue = onAddToQueue,
                        onToggleLike = { t ->
                            coroutineScope.launch {
                                likedStore.setLiked(t.id, t.id !in likedIds)
                            }
                        },
                        onOpenLiked = onOpenLiked,
                        likedCount = likedIds.size,
                    )
                    LocalLibraryViewModel.Tab.Folders -> FoldersTab(
                        groups = viewModel.foldersGrouped(),
                        onOpenFolder = onOpenFolder,
                        onAddSafFolder = { safLauncher.launch(null) },
                    )
                    LocalLibraryViewModel.Tab.Albums -> AlbumsTab(
                        groups = viewModel.albumsGrouped(),
                        onOpenAlbum = onOpenAlbum,
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MediaPlayerSpacing.S, vertical = MediaPlayerSpacing.S),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Indietro",
            )
        }
        Spacer(Modifier.width(MediaPlayerSpacing.Xs))
        Text(
            text = "Sul tuo dispositivo",
            style = MaterialTheme.typography.titleLarge,
            color = MHColors.TextHi,
        )
    }
}

@Composable
private fun TabBar(
    tab: LocalLibraryViewModel.Tab,
    onTab: (LocalLibraryViewModel.Tab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MediaPlayerSpacing.M),
        horizontalArrangement = Arrangement.spacedBy(MediaPlayerSpacing.S),
    ) {
        PillChip(
            label = "Brani",
            selected = tab == LocalLibraryViewModel.Tab.Tracks,
            onClick = { onTab(LocalLibraryViewModel.Tab.Tracks) },
        )
        PillChip(
            label = "Cartelle",
            selected = tab == LocalLibraryViewModel.Tab.Folders,
            onClick = { onTab(LocalLibraryViewModel.Tab.Folders) },
        )
        PillChip(
            label = "Album",
            selected = tab == LocalLibraryViewModel.Tab.Albums,
            onClick = { onTab(LocalLibraryViewModel.Tab.Albums) },
        )
    }
}

@Composable
private fun TracksTab(
    tracks: List<LocalTrack>,
    sort: LocalLibraryViewModel.SortBy,
    likedIds: Set<Long>,
    onSortChange: (LocalLibraryViewModel.SortBy) -> Unit,
    onPlay: (LocalTrack, List<LocalTrack>) -> Unit,
    onShuffle: (List<LocalTrack>) -> Unit,
    onPlayNext: (LocalTrack) -> Unit,
    onAddToQueue: (LocalTrack) -> Unit,
    onToggleLike: (LocalTrack) -> Unit,
    onOpenLiked: () -> Unit,
    likedCount: Int,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ActionBar(
            count = tracks.size,
            sort = sort,
            onSortChange = onSortChange,
            onShuffle = { onShuffle(tracks) },
        )
        Spacer(Modifier.height(MediaPlayerSpacing.S))
        var menuFor by remember { mutableStateOf<LocalTrack?>(null) }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            if (likedCount > 0) {
                item(key = "liked_local_tile") {
                    LikedShortcut(count = likedCount, onClick = onOpenLiked)
                    Spacer(Modifier.height(MediaPlayerSpacing.S))
                }
            }
            items(tracks, key = { it.id }) { t ->
                Box {
                    LocalTrackRow(
                        track = t,
                        onClick = { onPlay(t, tracks) },
                        onMore = { menuFor = t },
                        liked = t.id in likedIds,
                        onToggleLike = { onToggleLike(t) },
                    )
                    if (menuFor?.id == t.id) {
                        DropdownMenu(
                            expanded = true,
                            onDismissRequest = { menuFor = null },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Riproduci") },
                                onClick = {
                                    menuFor = null
                                    onPlay(t, tracks)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Riproduci dopo") },
                                onClick = {
                                    menuFor = null
                                    onPlayNext(t)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Aggiungi alla coda") },
                                onClick = {
                                    menuFor = null
                                    onAddToQueue(t)
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(if (t.id in likedIds) "Togli dai preferiti" else "Aggiungi ai preferiti")
                                },
                                onClick = {
                                    menuFor = null
                                    onToggleLike(t)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LikedShortcut(count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MediaPlayerSpacing.M)
            .clip(RoundedCornerShape(12.dp))
            .background(MHColors.Card)
            .clickable(onClick = onClick)
            .padding(MediaPlayerSpacing.M),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MHColors.LikedGradientEnd),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(MediaPlayerSpacing.M))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Brani che ti piacciono",
                style = MaterialTheme.typography.titleSmall,
                color = MHColors.TextHi,
                maxLines = 1,
            )
            Text(
                text = "$count locali",
                style = MaterialTheme.typography.bodySmall,
                color = MHColors.TextLo,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ActionBar(
    count: Int,
    sort: LocalLibraryViewModel.SortBy,
    onSortChange: (LocalLibraryViewModel.SortBy) -> Unit,
    onShuffle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MediaPlayerSpacing.M),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MediaPlayerSpacing.S),
    ) {
        FilledTonalButton(
            onClick = onShuffle,
            enabled = count > 0,
        ) {
            Icon(
                imageVector = Icons.Filled.Shuffle,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(MediaPlayerSpacing.Xs))
            Text("Riproduzione casuale")
        }
        Spacer(Modifier.weight(1f))
        SortMenu(sort = sort, onSortChange = onSortChange)
    }
}

@Composable
private fun SortMenu(
    sort: LocalLibraryViewModel.SortBy,
    onSortChange: (LocalLibraryViewModel.SortBy) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.Sort, contentDescription = "Ordina")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            sortLabel.forEach { (key, label) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = label,
                            color = if (sort == key) MHColors.Lime
                                    else MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    onClick = {
                        open = false
                        onSortChange(key)
                    },
                )
            }
        }
    }
}

private val sortLabel: List<Pair<LocalLibraryViewModel.SortBy, String>> = listOf(
    LocalLibraryViewModel.SortBy.Title to "Titolo",
    LocalLibraryViewModel.SortBy.Artist to "Artista",
    LocalLibraryViewModel.SortBy.Album to "Album",
    LocalLibraryViewModel.SortBy.DateAdded to "Aggiunti di recente",
    LocalLibraryViewModel.SortBy.Duration to "Durata",
)

@Composable
private fun FoldersTab(
    groups: List<Pair<String, List<LocalTrack>>>,
    onOpenFolder: (String) -> Unit,
    onAddSafFolder: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = MediaPlayerSpacing.M,
            vertical = MediaPlayerSpacing.S,
        ),
        verticalArrangement = Arrangement.spacedBy(MediaPlayerSpacing.S),
    ) {
        item(key = "saf_add") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MHColors.CardHigh)
                    .clickable(onClick = onAddSafFolder)
                    .padding(MediaPlayerSpacing.M),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MHColors.Lime),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = Color(0xFF0A0A0A),
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(MediaPlayerSpacing.M))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Aggiungi cartella personalizzata",
                        style = MaterialTheme.typography.titleSmall,
                        color = MHColors.TextHi,
                        maxLines = 1,
                    )
                    Text(
                        text = "Includi una cartella che il sistema non indicizza (SD card, scaricati, ecc.)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MHColors.TextLo,
                        maxLines = 2,
                    )
                }
            }
        }
        items(groups, key = { it.first }) { (path, items) ->
            FolderTile(
                path = path,
                count = items.size,
                onClick = { onOpenFolder(path) },
            )
        }
    }
}

@Composable
private fun FolderTile(path: String, count: Int, onClick: () -> Unit) {
    val name = path.substringAfterLast('/').ifBlank { path }.ifBlank { "(radice)" }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MHColors.Card)
            .clickable(onClick = onClick)
            .padding(MediaPlayerSpacing.M),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MHColors.CardHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.LibraryMusic,
                contentDescription = null,
                tint = MHColors.Lime,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(MediaPlayerSpacing.M))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                color = MHColors.TextHi,
                maxLines = 1,
            )
            if (path != name && path.isNotBlank()) {
                Text(
                    text = path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MHColors.TextLo2,
                    maxLines = 1,
                )
            }
        }
        Text(
            text = "$count",
            style = MaterialTheme.typography.bodySmall,
            color = MHColors.TextLo,
        )
    }
}

@Composable
private fun AlbumsTab(
    groups: List<Pair<String, List<LocalTrack>>>,
    onOpenAlbum: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = MediaPlayerSpacing.M,
            vertical = MediaPlayerSpacing.S,
        ),
        verticalArrangement = Arrangement.spacedBy(MediaPlayerSpacing.S),
    ) {
        items(groups, key = { it.first }) { (album, items) ->
            AlbumTile(
                album = album,
                artist = items.firstOrNull()?.artist.orEmpty(),
                count = items.size,
                cover = items.firstOrNull(),
                onClick = { onOpenAlbum(album) },
            )
        }
    }
}

@Composable
private fun AlbumTile(
    album: String,
    artist: String,
    count: Int,
    cover: LocalTrack?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MHColors.Card)
            .clickable(onClick = onClick)
            .padding(MediaPlayerSpacing.M),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (cover != null) {
            LocalCover(track = cover, size = 48.dp)
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MHColors.CardHigh),
            )
        }
        Spacer(Modifier.width(MediaPlayerSpacing.M))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album,
                style = MaterialTheme.typography.titleSmall,
                color = MHColors.TextHi,
                maxLines = 1,
            )
            Text(
                text = artist.ifBlank { "Sconosciuto" },
                style = MaterialTheme.typography.bodySmall,
                color = MHColors.TextLo,
                maxLines = 1,
            )
        }
        Text(
            text = "$count",
            style = MaterialTheme.typography.bodySmall,
            color = MHColors.TextLo,
        )
    }
}
