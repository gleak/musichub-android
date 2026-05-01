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
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.data.dto.PlaylistDto
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.ui.common.AnonymousBanner
import com.mediaplayer.android.ui.common.CenteredSpinner
import com.mediaplayer.android.ui.common.ErrorWithRetry
import com.mediaplayer.android.ui.common.LocalCurrentUser
import com.mediaplayer.android.ui.theme.SpotifyColors
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSongClick: (SongDto) -> Unit = {},
    onPlaylistClick: (PlaylistDto) -> Unit = {},
    onLikedClick: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onShowChangelog: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = viewModel::pullRefresh,
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
                onLikedClick = onLikedClick,
                onSignOut = onSignOut,
                onShowChangelog = onShowChangelog,
            )
        }
    }
}

@Composable
private fun HomeContent(
    recents: List<SongDto>,
    playlists: List<PlaylistDto>,
    onSongClick: (SongDto) -> Unit,
    onPlaylistClick: (PlaylistDto) -> Unit,
    onLikedClick: () -> Unit,
    onSignOut: () -> Unit,
    onShowChangelog: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item(key = "greeting") {
            GreetingHeader(
                onSignOut = onSignOut,
                onShowChangelog = onShowChangelog,
            )
        }

        item(key = "anonymous_banner") {
            AnonymousBanner()
        }

        if (recents.isNotEmpty() || playlists.isNotEmpty()) {
            item(key = "shortcuts") {
                ShortcutGrid(
                    recents = recents,
                    onLikedClick = onLikedClick,
                    onSongClick = onSongClick,
                    onPlaylistClick = onPlaylistClick,
                    playlists = playlists,
                )
            }
        }

        if (recents.isNotEmpty()) {
            item(key = "recent_title") {
                SectionTitle("Recently played")
            }
            item(key = "recent_row") {
                RecentRow(recents = recents, onClick = onSongClick)
            }
        }

        val autoPlaylists = playlists.filter { it.isAuto }
        val userPlaylists = playlists.filter { !it.isAuto }

        if (autoPlaylists.isNotEmpty()) {
            item(key = "made_for_you_title") {
                SectionTitle("Made for you")
            }
            item(key = "made_for_you_row") {
                PlaylistRow(playlists = autoPlaylists, onClick = onPlaylistClick)
            }
        }

        if (userPlaylists.isNotEmpty()) {
            item(key = "playlists_title") {
                SectionTitle("Your playlists")
            }
            item(key = "playlists_row") {
                PlaylistRow(playlists = userPlaylists, onClick = onPlaylistClick)
            }
        }
    }
}

@Composable
private fun GreetingHeader(
    onSignOut: () -> Unit,
    onShowChangelog: () -> Unit,
) {
    val isAnonymous = LocalCurrentUser.current?.user?.anonymous == true
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = greetingForNow(),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
            ) {
                DropdownMenuItem(
                    text = { Text("What's new") },
                    onClick = {
                        menuOpen = false
                        onShowChangelog()
                    },
                )
                DropdownMenuItem(
                    text = { Text(if (isAnonymous) "Sign in" else "Sign out") },
                    onClick = {
                        menuOpen = false
                        onSignOut()
                    },
                )
            }
        }
    }
}

@Composable
private fun ShortcutGrid(
    recents: List<SongDto>,
    playlists: List<PlaylistDto>,
    onLikedClick: () -> Unit,
    onSongClick: (SongDto) -> Unit,
    onPlaylistClick: (PlaylistDto) -> Unit,
) {
    // Up to 6 horizontally-laid wide tiles in a 2-col layout, Spotify style.
    val items = buildList<ShortcutItem> {
        add(ShortcutItem.Liked)
        recents.take(3).forEach { add(ShortcutItem.Song(it)) }
        playlists.take(2).forEach { add(ShortcutItem.Playlist(it)) }
    }.take(6)

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
    val shape = RoundedCornerShape(6.dp)
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
                ShortcutItem.Liked -> "Liked Songs"
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
                                    SpotifyColors.LikedGradientStart,
                                    SpotifyColors.LikedGradientEnd,
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
            is ShortcutItem.Song -> {
                if (item.song.hasCoverArt) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(Network.coverUrl(item.song.id))
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
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
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(6.dp))
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
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (playlist.isAuto) {
                        Brush.linearGradient(
                            listOf(
                                SpotifyColors.LikedGradientStart,
                                SpotifyColors.LikedGradientEnd,
                            )
                        )
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
            text = if (playlist.isAuto) "Made for you"
            else if (playlist.songCount == 1) "1 song"
            else "${playlist.songCount} songs",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun greetingForNow(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
}
