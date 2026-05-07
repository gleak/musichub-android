package com.mediaplayer.android.ui.search

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mediaplayer.android.R
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.ui.common.CenteredMessage
import com.mediaplayer.android.ui.common.ErrorWithRetry
import com.mediaplayer.android.ui.common.EyebrowText
import com.mediaplayer.android.ui.common.MHCover
import com.mediaplayer.android.ui.common.MHCoverKind
import com.mediaplayer.android.ui.common.MHCoverPalette
import com.mediaplayer.android.ui.common.SongCover
import com.mediaplayer.android.ui.common.SongListShimmer
import com.mediaplayer.android.ui.playlists.AddToPlaylistSheet
import com.mediaplayer.android.ui.theme.CoverShapes
import com.mediaplayer.android.ui.theme.LocalMHMono
import com.mediaplayer.android.ui.theme.MHColors

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel(),
    onSongClick: (SongDto) -> Unit = {},
    onPlayNext: ((SongDto) -> Unit)? = null,
    onAddToQueue: ((SongDto) -> Unit)? = null,
    onAlbumClick: (name: String, artist: String) -> Unit = { _, _ -> },
    onAlbumListClick: () -> Unit = {},
    onArtistClick: (name: String) -> Unit = {},
    onArtistListClick: () -> Unit = {},
    onGenreOpen: (display: String, tag: String) -> Unit = { _, _ -> },
    onProfileClick: () -> Unit = {},
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val downloadedIds by viewModel.downloadedIds.collectAsStateWithLifecycle()
    val recentSongs by viewModel.recentSongs.collectAsStateWithLifecycle()
    val recentQueries by viewModel.recentQueries.collectAsStateWithLifecycle()
    val activeGenre by viewModel.activeGenre.collectAsStateWithLifecycle()

    val kebab = com.mediaplayer.android.ui.common.rememberSongKebab()
    val snackbar = remember { SnackbarHostState() }
    var lastAdded by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(lastAdded) {
        val msg = lastAdded ?: return@LaunchedEffect
        snackbar.showSnackbar(msg)
        lastAdded = null
    }

    val context = LocalContext.current
    val voicePromptText = stringResource(R.string.search_voice_prompt)
    val voiceUnavailableText = stringResource(R.string.search_voice_unavailable)
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.takeIf { it.isNotBlank() }
        if (spoken != null) viewModel.commitQuery(spoken)
    }

    val onMicClick: () -> Unit = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, voicePromptText)
        }
        try {
            voiceLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, voiceUnavailableText, Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Cerca",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onProfileClick) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Profilo",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        SearchField(
            query = query,
            onQueryChange = viewModel::onQueryChange,
            onSubmit = { viewModel.commitQuery(query) },
            onClear = viewModel::clearQuery,
            onMic = onMicClick,
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when (val s = state) {
                SearchUiState.Idle -> IdleContent(
                    recents = recentQueries,
                    onRecentClick = viewModel::commitQuery,
                    onRecentRemove = viewModel::removeRecent,
                    onClearRecents = viewModel::clearRecents,
                    // Tile taps open the dedicated GenreDetailScreen now
                    // (`mh-library.jsx:156-216`); the inline filter pill stays
                    // available when entered from elsewhere via selectGenre.
                    onGenreClick = { display, tag -> onGenreOpen(display, tag) },
                )
                SearchUiState.Loading -> Column {
                    activeGenre?.let { GenreFilterPill(name = it, onClear = viewModel::clearGenre) }
                    SongListShimmer()
                }
                is SearchUiState.Success -> {
                    LaunchedEffect(s.songs) {
                        com.mediaplayer.android.data.LikedSongsCache.prime(s.songs.map { it.id })
                    }
                    if (s.songs.isEmpty()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            activeGenre?.let { GenreFilterPill(name = it, onClear = viewModel::clearGenre) }
                            CenteredMessage(stringResource(R.string.search_no_matches))
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            activeGenre?.let { name ->
                                item {
                                    GenreFilterPill(name = name, onClear = viewModel::clearGenre)
                                }
                            }
                            if (query.isEmpty() && recentSongs.isNotEmpty()) {
                                item {
                                    RecentlyPlayedCarousel(
                                        songs = recentSongs,
                                        onSongClick = onSongClick,
                                    )
                                }
                            }
                            items(items = s.songs, key = { it.id }) { song ->
                                SongRow(
                                    song = song,
                                    onClick = { onSongClick(song) },
                                    isDownloaded = song.id in downloadedIds,
                                    onArtistClick = onArtistClick,
                                    onMore = { kebab.open(song) },
                                )
                            }
                        }
                    }
                }
                is SearchUiState.Error -> ErrorWithRetry(
                    message = stringResource(R.string.search_error) + "\n" + s.message,
                    onRetry = viewModel::retry,
                )
            }

            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    com.mediaplayer.android.ui.common.SongKebabSheet(
        state = kebab,
        onPlayNext = onPlayNext?.let { cb -> { song -> cb(song) } },
        onAddToQueue = onAddToQueue?.let { cb -> { song -> cb(song) } },
        onDownload = { song -> viewModel.toggleDownload(song.id, song.title) },
        onFlagged = { viewModel.retry() },
        onAdded = { playlistName, _ -> lastAdded = "Added to $playlistName" },
    )
}

// ---------- Search field ----------

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
    onMic: () -> Unit,
) {
    val focus = LocalFocusManager.current
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(10.dp)),
        placeholder = { Text(stringResource(R.string.search_hint)) },
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.content_desc_clear_query),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onMic) {
                    Icon(
                        Icons.Filled.Mic,
                        contentDescription = stringResource(R.string.search_voice_prompt),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {
            onSubmit()
            focus.clearFocus()
        }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White.copy(alpha = 0.08f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
            disabledContainerColor = Color.White.copy(alpha = 0.08f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
    )
}

// ---------- Idle content (recents + genres) ----------

@Composable
private fun IdleContent(
    recents: List<String>,
    onRecentClick: (String) -> Unit,
    onRecentRemove: (String) -> Unit,
    onClearRecents: () -> Unit,
    onGenreClick: (displayName: String, tagQuery: String) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (recents.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EyebrowText(text = stringResource(R.string.search_section_recents))
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.search_recents_clear),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable(onClick = onClearRecents),
                    )
                }
            }
            items(items = recents, key = { it }) { q ->
                RecentRow(
                    query = q,
                    onClick = { onRecentClick(q) },
                    onRemove = { onRecentRemove(q) },
                )
            }
        }
        item {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 6.dp)) {
                EyebrowText(text = stringResource(R.string.search_section_browse))
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.search_browse_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        item {
            GenreGrid(onGenreClick = onGenreClick)
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun RecentRow(
    query: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = query,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(16.dp),
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

// ---------- Genre tiles ----------

private data class Genre(
    val name: String,
    /** English Last.fm tag matched against `song_tags.tag` server-side. */
    val tagQuery: String,
    val tile: Color,
    val kind: MHCoverKind,
    val palette: MHCoverPalette,
)

private val GENRES: List<Genre> = listOf(
    Genre("Indie", "indie", Color(0xFF3A0CA3), MHCoverKind.Arc,
        MHCoverPalette(Color(0xFF3A0CA3), Color(0xFFF72585))),
    Genre("Elettronica", "electronic", Color(0xFF06B6D4), MHCoverKind.Wave,
        MHCoverPalette(Color(0xFF1E3A8A), Color(0xFF06B6D4))),
    Genre("Hip-hop", "hip-hop", Color(0xFFFF4D2E), MHCoverKind.Triangles,
        MHCoverPalette(Color(0xFF1A1A1A), Color(0xFFFF4D2E))),
    Genre("Jazz", "jazz", Color(0xFFFFC857), MHCoverKind.Stripes,
        MHCoverPalette(Color(0xFFFFC857), Color(0xFF1A1A1A))),
    Genre("Classica", "classical", Color(0xFFE8DCC4), MHCoverKind.Moon,
        MHCoverPalette(Color(0xFFE8DCC4), Color(0xFF1A1A1A))),
    Genre("Ambient", "ambient", Color(0xFF0B3D2E), MHCoverKind.Dot,
        MHCoverPalette(Color(0xFF0B3D2E), MHColors.Lime)),
    Genre("Rock", "rock", Color(0xFF5C2D8C), MHCoverKind.Grid,
        MHCoverPalette(Color(0xFF5C2D8C), Color(0xFFF0A6B0))),
    Genre("Pop", "pop", Color(0xFFF0A6B0), MHCoverKind.Duotone,
        MHCoverPalette(Color(0xFFF0A6B0), Color(0xFF3A0CA3))),
    Genre("Metal", "metal", Color(0xFF1A1A1A), MHCoverKind.Type,
        MHCoverPalette(Color(0xFF1A1A1A), Color(0xFFA8201A))),
    Genre("Punk", "punk", Color(0xFF0A0A0A), MHCoverKind.Triangles,
        MHCoverPalette(Color(0xFF0A0A0A), Color(0xFFFF1F8F))),
    Genre("R&B", "r&b", Color(0xFF7A4F8F), MHCoverKind.Blob,
        MHCoverPalette(Color(0xFF3A0E5C), Color(0xFFD4A857))),
    Genre("Folk", "folk", Color(0xFF6B5238), MHCoverKind.Artist,
        MHCoverPalette(Color(0xFF6B5238), Color(0xFF3D5C30))),
    Genre("Country", "country", Color(0xFFC28B5A), MHCoverKind.Stripes,
        MHCoverPalette(Color(0xFFC28B5A), Color(0xFF1F5C5C))),
    Genre("Blues", "blues", Color(0xFF1F4FA8), MHCoverKind.Wave,
        MHCoverPalette(Color(0xFF1F4FA8), Color(0xFF0A1A33))),
    Genre("Reggae", "reggae", Color(0xFF1B7A3A), MHCoverKind.Stripes,
        MHCoverPalette(Color(0xFF1B7A3A), Color(0xFFFFC42E))),
    Genre("Latina", "latin", Color(0xFFC71585), MHCoverKind.Arc,
        MHCoverPalette(Color(0xFFFF4D2E), Color(0xFFC71585))),
    Genre("Lo-fi", "lo-fi", Color(0xFF7B5C9C), MHCoverKind.Dot,
        MHCoverPalette(Color(0xFFE6A0B5), Color(0xFF7B5C9C))),
    Genre("Cantautorato", "singer-songwriter", Color(0xFF7A4A2E), MHCoverKind.Artist,
        MHCoverPalette(Color(0xFFE8DCC4), Color(0xFF7A4A2E))),
)

@Composable
private fun GenreGrid(onGenreClick: (displayName: String, tagQuery: String) -> Unit) {
    // Inline grid (no nested scroll) — split into rows of 2 manually.
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        GENRES.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                pair.forEach { g ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(90.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(g.tile)
                            .clickable { onGenreClick(g.name, g.tagQuery) }
                            .padding(12.dp),
                    ) {
                        Text(
                            text = g.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            modifier = Modifier.align(Alignment.TopStart),
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(60.dp)
                                .rotate(20f),
                        ) {
                            MHCover(
                                kind = g.kind,
                                palette = g.palette,
                                cornerRadius = 6.dp,
                            )
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun GenreFilterPill(name: String, onClear: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(CircleShape)
            .background(MHColors.Lime)
            .clickable(onClick = onClear)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = MHColors.Black,
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = stringResource(R.string.search_genre_filter_clear),
            tint = MHColors.Black,
            modifier = Modifier.size(14.dp),
        )
    }
}

// ---------- Recent songs carousel (kept) ----------

@Composable
private fun RecentlyPlayedCarousel(
    songs: List<SongDto>,
    onSongClick: (SongDto) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp)) {
        EyebrowText(
            text = "Recenti",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items = songs, key = { it.id }) { song ->
                RecentSongCard(song = song, onClick = { onSongClick(song) })
            }
        }
    }
}

@Composable
private fun RecentSongCard(song: SongDto, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(96.dp)
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SongCover(
            song = song,
            size = 80.dp,
            shape = CoverShapes.Tile,
            contentDescription = "${song.title}, ${song.artist}",
        )
        Text(
            text = song.title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
