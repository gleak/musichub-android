package com.mediaplayer.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import android.widget.Toast
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.data.dto.PlaylistDto
import com.mediaplayer.android.ui.common.AppUpdateBannerHost
import com.mediaplayer.android.update.AppUpdateChecker
import kotlinx.coroutines.launch
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.ui.common.CenteredSpinner
import com.mediaplayer.android.ui.common.ErrorWithRetry
import com.mediaplayer.android.ui.common.LocalCurrentUser
import com.mediaplayer.android.ui.common.MHLogo
import com.mediaplayer.android.ui.common.SectionHeader
import com.mediaplayer.android.ui.common.SongCover
import com.mediaplayer.android.ui.theme.CoverShapes
import com.mediaplayer.android.ui.theme.MediaPlayerSpacing
import com.mediaplayer.android.ui.theme.MHColors
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSongClick: (SongDto) -> Unit = {},
    onPlaylistClick: (PlaylistDto) -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    onLikedClick: () -> Unit = {},
    onFindClick: () -> Unit = {},
    onSpotifyImport: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onShowChangelog: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onResumeFlush: suspend () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    // Silent refresh on resume so songs played in this session land in the
    // recents row (and therefore in the Musica/Artisti filter views) when
    // the user returns to Home from the player or another tab. We flush
    // the in-flight play first so the backend has the new record before we
    // re-fetch /recent.
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    onResumeFlush()
                    viewModel.resume()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                onResumeFlush()
                viewModel.pullRefresh()
            }
        },
        modifier = modifier.fillMaxSize(),
    ) {
        when (val s = state) {
            HomeUiState.Loading -> CenteredSpinner()
            is HomeUiState.Error -> ErrorWithRetry(s.message, viewModel::refresh)
            is HomeUiState.Success -> HomeContent(
                recents = s.recents,
                playlists = s.playlists,
                onSongClick = onSongClick,
                onPlaylistClick = onPlaylistClick,
                onArtistClick = onArtistClick,
                onLikedClick = onLikedClick,
                onFindClick = onFindClick,
                onSpotifyImport = onSpotifyImport,
                onSignOut = onSignOut,
                onShowChangelog = onShowChangelog,
                onProfileClick = onProfileClick,
            )
        }
    }
}

private enum class HomeFilter(val label: String) {
    All("Tutto"),
    Music("Musica"),
    Playlists("Playlist"),
    Artists("Artisti"),
}

@Composable
private fun HomeContent(
    recents: List<SongDto>,
    playlists: List<PlaylistDto>,
    onSongClick: (SongDto) -> Unit,
    onPlaylistClick: (PlaylistDto) -> Unit,
    onArtistClick: (String) -> Unit,
    onLikedClick: () -> Unit,
    onFindClick: () -> Unit,
    onSpotifyImport: () -> Unit,
    onSignOut: () -> Unit,
    onShowChangelog: () -> Unit,
    onProfileClick: () -> Unit,
) {
    var filter by remember { mutableStateOf(HomeFilter.All) }

    val artists = remember(recents) {
        recents.map { it.artist }.filter { it.isNotBlank() }.distinct()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = MediaPlayerSpacing.M, bottom = MediaPlayerSpacing.L),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item(key = "greeting") {
            // Mockup `mh-screens.jsx:43-45` adds a "· N nuove uscite per te" tail
            // when there are new releases. We use Release Radar's songCount as
            // the count: the auto-playlist literally is "tracks released since
            // your last visit by artists you follow", so its size is the most
            // honest number to show.
            val newReleases = playlists
                .firstOrNull { it.kind.equals("RELEASE_RADAR", ignoreCase = true) }
                ?.songCount ?: 0
            GreetingHeader(
                newReleaseCount = newReleases,
                onSignOut = onSignOut,
                onShowChangelog = onShowChangelog,
                onProfileClick = onProfileClick,
            )
        }

        item(key = "filter_chips") {
            HomeFilterChips(selected = filter, onSelect = { filter = it })
        }

        item(key = "app_update_banner") {
            AppUpdateBannerHost()
        }

        val autoPlaylists = playlists.filter { it.isAuto }
        val userPlaylists = playlists.filter { !it.isAuto }

        when (filter) {
            HomeFilter.All -> {
                if (recents.isNotEmpty() || playlists.isNotEmpty()) {
                    item(key = "shortcuts") {
                        ShortcutGrid(
                            recents = recents,
                            playlists = playlists,
                            includeLiked = true,
                            onLikedClick = onLikedClick,
                            onSongClick = onSongClick,
                            onPlaylistClick = onPlaylistClick,
                        )
                    }
                } else {
                    // Cold-start (zero recents AND zero playlists). Default Home
                    // is otherwise empty under the greeting — surface CTAs that
                    // move the user toward content instead of leaving them at a wall.
                    item(key = "cold_start") {
                        ColdStartCtas(
                            onFindClick = onFindClick,
                            onSpotifyImport = onSpotifyImport,
                        )
                    }
                }

                if (recents.isNotEmpty()) {
                    item(key = "recent_title") {
                        SectionHeader(eyebrow = "Cronologia", title = "Riprodotti di recente")
                    }
                    item(key = "recent_row") {
                        RecentRow(recents = recents, onClick = onSongClick)
                    }
                }

                if (autoPlaylists.isNotEmpty()) {
                    item(key = "made_for_you_title") {
                        SectionHeader(eyebrow = "Generata per te", title = "Le tue playlist di oggi")
                    }
                    item(key = "made_for_you_row") {
                        PlaylistRow(playlists = autoPlaylists, onClick = onPlaylistClick)
                    }
                }

                if (userPlaylists.isNotEmpty()) {
                    item(key = "playlists_title") {
                        SectionHeader(eyebrow = "Libreria", title = "Le tue playlist")
                    }
                    item(key = "playlists_row") {
                        PlaylistRow(playlists = userPlaylists, onClick = onPlaylistClick)
                    }
                }
            }

            HomeFilter.Music -> {
                item(key = "liked_row") {
                    SingleColumnTile(
                        title = "Brani che mi piacciono",
                        subtitle = "I brani con il cuore",
                        icon = Icons.Filled.Favorite,
                        gradientStart = MHColors.LikedGradientStart,
                        gradientEnd = MHColors.LikedGradientEnd,
                        iconTint = Color.White,
                        onClick = onLikedClick,
                    )
                }
                if (recents.isEmpty()) {
                    item(key = "music_empty") {
                        EmptyFilterHint("Nessun brano recente — riproduci qualcosa per riempire questa lista.")
                    }
                } else {
                    item(key = "music_title") {
                        SectionHeader(eyebrow = "Cronologia", title = "Brani recenti")
                    }
                    items(items = recents, key = { "song_${it.id}" }) { song ->
                        com.mediaplayer.android.ui.search.SongRow(
                            song = song,
                            onClick = { onSongClick(song) },
                            onArtistClick = onArtistClick,
                        )
                    }
                }
            }

            HomeFilter.Playlists -> {
                if (playlists.isEmpty()) {
                    item(key = "pl_empty") {
                        EmptyFilterHint("Nessuna playlist — creane una o importala.")
                    }
                } else {
                    if (autoPlaylists.isNotEmpty()) {
                        item(key = "pl_auto_title") {
                            SectionHeader(eyebrow = "Generata per te", title = "Le tue playlist di oggi")
                        }
                        items(items = autoPlaylists, key = { "auto_${it.id}" }) { p ->
                            PlaylistListRow(playlist = p, onClick = { onPlaylistClick(p) })
                        }
                    }
                    if (userPlaylists.isNotEmpty()) {
                        item(key = "pl_user_title") {
                            SectionHeader(eyebrow = "Libreria", title = "Le tue playlist")
                        }
                        items(items = userPlaylists, key = { "user_${it.id}" }) { p ->
                            PlaylistListRow(playlist = p, onClick = { onPlaylistClick(p) })
                        }
                    }
                }
            }

            HomeFilter.Artists -> {
                if (artists.isEmpty()) {
                    item(key = "artists_empty") {
                        EmptyFilterHint("Nessun artista — riproduci qualcosa per popolare questa lista.")
                    }
                } else {
                    item(key = "artists_title") {
                        SectionHeader(eyebrow = "Dai tuoi ascolti", title = "Artisti")
                    }
                    items(items = artists, key = { "artist_$it" }) { name ->
                        ArtistListRow(name = name, onClick = { onArtistClick(name) })
                    }
                }
            }
        }

        item(key = "feed_end") {
            Text(
                text = "— FINE FEED —",
                style = com.mediaplayer.android.ui.theme.LocalMHMono.current.duration.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    letterSpacing = 1.sp,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HomeFilterChips(
    selected: HomeFilter,
    onSelect: (HomeFilter) -> Unit,
) {
    val chips = HomeFilter.values().toList()
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(chips) { c ->
            com.mediaplayer.android.ui.common.PillChip(
                label = c.label,
                selected = c == selected,
                onClick = { onSelect(c) },
            )
        }
    }
}

@Composable
private fun EmptyFilterHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    )
}

@Composable
private fun ColdStartCtas(
    onFindClick: () -> Unit,
    onSpotifyImport: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Iniziamo",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "La tua libreria è vuota — scegli come riempirla.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ColdStartTile(
                title = "Scopri musica",
                subtitle = "Cerca su YouTube e importa",
                icon = Icons.Filled.Search,
                onClick = onFindClick,
                modifier = Modifier.weight(1f),
            )
            ColdStartTile(
                title = "Importa Spotify",
                subtitle = "Porta una playlist qui",
                icon = Icons.Filled.LibraryMusic,
                onClick = onSpotifyImport,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ColdStartTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(CoverShapes.Tile)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GreetingHeader(
    newReleaseCount: Int,
    onSignOut: () -> Unit,
    onShowChangelog: () -> Unit,
    onProfileClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        // Brand lockup pinned to the top of the surface per mockup
        // `mh-screens.jsx:35-40`. Sits above the greeting so the
        // headline still anchors the screen.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        ) {
            MHLogo(tileSize = 22.dp, modifier = Modifier.weight(1f))
            IconButton(onClick = onProfileClick, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Profilo",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = greetingForNow(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
        val tail = when {
            newReleaseCount <= 0 -> ""
            newReleaseCount == 1 -> " · 1 nuova uscita per te"
            else -> " · $newReleaseCount nuove uscite per te"
        }
        Text(
            text = currentDateLabel() + tail,
            style = com.mediaplayer.android.ui.theme.LocalMHMono.current.caption,
            color = com.mediaplayer.android.ui.theme.MHColors.TextLo,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

private fun currentDateLabel(): String {
    val cal = Calendar.getInstance()
    val days = listOf("Dom", "Lun", "Mar", "Mer", "Gio", "Ven", "Sab")
    val months = listOf(
        "Gen", "Feb", "Mar", "Apr", "Mag", "Giu",
        "Lug", "Ago", "Set", "Ott", "Nov", "Dic",
    )
    val day = days[cal.get(Calendar.DAY_OF_WEEK) - 1]
    val month = months[cal.get(Calendar.MONTH)]
    val date = cal.get(Calendar.DAY_OF_MONTH)
    return "$day $date $month"
}

@Composable
private fun ShortcutGrid(
    recents: List<SongDto>,
    playlists: List<PlaylistDto>,
    includeLiked: Boolean,
    onLikedClick: () -> Unit,
    onSongClick: (SongDto) -> Unit,
    onPlaylistClick: (PlaylistDto) -> Unit,
) {
    // Up to 6 horizontally-laid wide tiles in a 2-col layout, Spotify style.
    val items = buildList<ShortcutItem> {
        if (includeLiked) add(ShortcutItem.Liked)
        val songSlots = if (playlists.isEmpty()) 5 else 3
        val plSlots = if (recents.isEmpty()) 5 else 2
        recents.take(songSlots).forEach { add(ShortcutItem.Song(it)) }
        playlists.take(plSlots).forEach { add(ShortcutItem.Playlist(it)) }
    }.take(6)
    if (items.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowItems.forEach { item ->
                    ShortcutTile(
                        item = item,
                        modifier = Modifier.weight(1f),
                        onLikedClick = onLikedClick,
                        onSongClick = onSongClick,
                        onPlaylistClick = onPlaylistClick,
                    )
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private sealed interface ShortcutItem {
    data object Liked : ShortcutItem
    data class Song(val song: SongDto) : ShortcutItem
    data class Playlist(val playlist: PlaylistDto) : ShortcutItem
}

@Composable
private fun ShortcutTile(
    item: ShortcutItem,
    onLikedClick: () -> Unit,
    onSongClick: (SongDto) -> Unit,
    onPlaylistClick: (PlaylistDto) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = CoverShapes.SongRow
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable {
                when (item) {
                    ShortcutItem.Liked -> onLikedClick()
                    is ShortcutItem.Song -> onSongClick(item.song)
                    is ShortcutItem.Playlist -> onPlaylistClick(item.playlist)
                }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShortcutIcon(item = item)
        Text(
            text = when (item) {
                ShortcutItem.Liked -> "Brani che mi piacciono"
                is ShortcutItem.Song -> item.song.title
                is ShortcutItem.Playlist -> item.playlist.name
            },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = 8.dp).weight(1f),
        )
    }
}

@Composable
private fun ShortcutIcon(item: ShortcutItem) {
    Box(
        modifier = Modifier.size(56.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (item) {
            ShortcutItem.Liked -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MHColors.LikedGradientStart,
                                    MHColors.LikedGradientEnd,
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            is ShortcutItem.Song -> SongCover(
                song = item.song,
                size = 56.dp,
                shape = RoundedCornerShape(0.dp),
                contentDescription = "${item.song.title}, ${item.song.artist}",
            )
            is ShortcutItem.Playlist -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}


@Composable
private fun RecentRow(recents: List<SongDto>, onClick: (SongDto) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = recents, key = { it.id }) { song ->
            SongCardSquare(song = song, onClick = { onClick(song) })
        }
    }
}

@Composable
private fun SongCardSquare(song: SongDto, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SongCover(
            song = song,
            size = 140.dp,
            shape = CoverShapes.MiniPlayer,
            contentDescription = "${song.title}, ${song.artist}",
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = song.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
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
}

@Composable
private fun PlaylistRow(playlists: List<PlaylistDto>, onClick: (PlaylistDto) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = playlists, key = { it.id }) { p ->
            PlaylistCardSquare(playlist = p, onClick = { onClick(p) })
        }
    }
}

@Composable
private fun PlaylistCardSquare(playlist: PlaylistDto, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(CoverShapes.SongRow)
                .background(
                    if (playlist.isAuto) {
                        com.mediaplayer.android.ui.common.autoPlaylistGradient(playlist.kind)
                    } else {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surfaceVariant,
                            )
                        )
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (playlist.isAuto) Icons.Filled.AutoAwesome
                else Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = null,
                tint = if (playlist.isAuto) Color.White
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp),
            )
        }
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = if (playlist.isAuto) "Generata per te"
            else if (playlist.songCount == 1) "1 brano"
            else "${playlist.songCount} brani",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ArtistRow(artists: List<String>, onClick: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = artists, key = { it }) { name ->
            ArtistCardCircle(name = name, onClick = { onClick(name) })
        }
    }
}

@Composable
private fun ArtistCardCircle(name: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                        )
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.take(1).uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SingleColumnTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradientStart: Color,
    gradientEnd: Color,
    iconTint: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(CoverShapes.SongRow)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(Brush.linearGradient(listOf(gradientStart, gradientEnd))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PlaylistListRow(playlist: PlaylistDto, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CoverShapes.SongRow)
                .background(
                    if (playlist.isAuto) {
                        com.mediaplayer.android.ui.common.autoPlaylistGradient(playlist.kind)
                    } else {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surfaceVariant,
                            )
                        )
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (playlist.isAuto) Icons.Filled.AutoAwesome
                else Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = null,
                tint = if (playlist.isAuto) Color.White
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (playlist.isAuto) "Per te · ${playlist.songCount} brani"
                else "Playlist · ${playlist.songCount} brani",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ArtistListRow(name: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                        )
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun greetingForNow(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Buongiorno"
        in 12..17 -> "Buon pomeriggio"
        else -> "Buonasera"
    }
}
