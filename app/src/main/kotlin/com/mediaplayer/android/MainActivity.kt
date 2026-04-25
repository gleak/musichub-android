package com.mediaplayer.android

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mediaplayer.android.playback.PlaybackViewModel
import com.mediaplayer.android.ui.albums.AlbumListScreen
import com.mediaplayer.android.ui.albums.AlbumScreen
import com.mediaplayer.android.ui.artists.ArtistListScreen
import com.mediaplayer.android.ui.artists.ArtistScreen
import com.mediaplayer.android.ui.find.FindScreen
import com.mediaplayer.android.ui.liked.LikedScreen
import com.mediaplayer.android.ui.player.MiniPlayer
import com.mediaplayer.android.ui.player.NowPlayingSheet
import com.mediaplayer.android.ui.playlists.PlaylistDetailScreen
import com.mediaplayer.android.ui.playlists.PlaylistsScreen
import com.mediaplayer.android.ui.search.SearchScreen
import com.mediaplayer.android.ui.theme.MediaPlayerTheme

@UnstableApi
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MediaPlayerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppScaffold()
                }
            }
        }
    }
}

private object Routes {
    const val SEARCH = "search"
    const val FIND = "find"
    const val PLAYLISTS = "playlists"
    const val PLAYLIST_DETAIL = "playlists/{playlistId}"
    const val LIKED = "liked"
    const val ALBUM_LIST = "albums"
    const val ALBUM_DETAIL = "albums/{albumName}?artist={albumArtist}"
    const val ARTIST_LIST = "artists"
    const val ARTIST_DETAIL = "artists/{artistName}"

    fun playlistDetail(id: Long) = "playlists/$id"
    fun albumDetail(name: String, artist: String) =
        "albums/${Uri.encode(name)}?artist=${Uri.encode(artist)}"
    fun artistDetail(name: String) = "artists/${Uri.encode(name)}"
}

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit,
)

@UnstableApi
@Composable
private fun AppScaffold() {
    val playbackVm: PlaybackViewModel = viewModel()
    val currentSong by playbackVm.currentSong.collectAsStateWithLifecycle()
    var sheetOpen by remember { mutableStateOf(false) }

    val navController = rememberNavController()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomRegion(
                navController = navController,
                miniPlayerVisible = currentSong != null,
                onExpandMiniPlayer = { sheetOpen = true },
                playbackVm = playbackVm,
            )
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SEARCH,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            composable(Routes.SEARCH) {
                SearchScreen(
                    onSongClick = playbackVm::play,
                    onPlayNext = playbackVm::playNext,
                    onAddToQueue = playbackVm::addToQueue,
                    onAlbumClick = { name, artist ->
                        navController.navigate(Routes.albumDetail(name, artist))
                    },
                    onAlbumListClick = { navController.navigate(Routes.ALBUM_LIST) },
                    onArtistClick = { name ->
                        navController.navigate(Routes.artistDetail(name))
                    },
                    onArtistListClick = { navController.navigate(Routes.ARTIST_LIST) },
                )
            }
            composable(Routes.FIND) {
                FindScreen()
            }
            composable(Routes.PLAYLISTS) {
                PlaylistsScreen(
                    onPlaylistClick = { p ->
                        navController.navigate(Routes.playlistDetail(p.id))
                    },
                    onLikedSongsClick = { navController.navigate(Routes.LIKED) },
                )
            }
            composable(Routes.LIKED) {
                LikedScreen(
                    onBack = { navController.popBackStack() },
                    onPlayFromIndex = { songs, index ->
                        playbackVm.playPlaylist(songs, index)
                    },
                )
            }
            composable(
                route = Routes.PLAYLIST_DETAIL,
                arguments = listOf(navArgument("playlistId") { type = NavType.LongType }),
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("playlistId") ?: return@composable
                PlaylistDetailScreen(
                    playlistId = id,
                    onBack = { navController.popBackStack() },
                    onPlayFromIndex = { songs, index ->
                        playbackVm.playPlaylist(songs, index)
                    },
                )
            }
            composable(Routes.ALBUM_LIST) {
                AlbumListScreen(
                    onBack = { navController.popBackStack() },
                    onAlbumClick = { name, artist ->
                        navController.navigate(Routes.albumDetail(name, artist))
                    },
                )
            }
            composable(
                route = Routes.ALBUM_DETAIL,
                arguments = listOf(
                    navArgument("albumName") { type = NavType.StringType },
                    navArgument("albumArtist") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) { backStackEntry ->
                val name = backStackEntry.arguments?.getString("albumName") ?: return@composable
                val artist = backStackEntry.arguments?.getString("albumArtist") ?: ""
                AlbumScreen(
                    albumName = name,
                    albumArtist = artist,
                    onBack = { navController.popBackStack() },
                    onPlayFromIndex = { songs, index -> playbackVm.playPlaylist(songs, index) },
                    onArtistClick = { artistName ->
                        navController.navigate(Routes.artistDetail(artistName))
                    },
                )
            }
            composable(Routes.ARTIST_LIST) {
                ArtistListScreen(
                    onBack = { navController.popBackStack() },
                    onArtistClick = { name ->
                        navController.navigate(Routes.artistDetail(name))
                    },
                )
            }
            composable(
                route = Routes.ARTIST_DETAIL,
                arguments = listOf(
                    navArgument("artistName") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val name = backStackEntry.arguments?.getString("artistName") ?: return@composable
                ArtistScreen(
                    artistName = name,
                    onBack = { navController.popBackStack() },
                    onPlayFromIndex = { songs, index -> playbackVm.playPlaylist(songs, index) },
                    onAlbumClick = { albumName, albumArtist ->
                        navController.navigate(Routes.albumDetail(albumName, albumArtist))
                    },
                )
            }
        }
    }

    if (sheetOpen) {
        NowPlayingSheet(
            viewModel = playbackVm,
            onDismiss = { sheetOpen = false },
        )
    }
}

@UnstableApi
@Composable
private fun BottomRegion(
    navController: NavHostController,
    miniPlayerVisible: Boolean,
    onExpandMiniPlayer: () -> Unit,
    playbackVm: PlaybackViewModel,
) {
    Column {
        if (miniPlayerVisible) {
            MiniPlayer(
                viewModel = playbackVm,
                onExpand = onExpandMiniPlayer,
            )
        }
        BottomNav(navController = navController)
    }
}

@Composable
private fun BottomNav(navController: NavHostController) {
    val destinations = listOf(
        BottomDestination(
            route = Routes.SEARCH,
            label = "Search",
            icon = { Icon(Icons.Filled.Search, contentDescription = null) },
        ),
        BottomDestination(
            route = Routes.FIND,
            label = "Find",
            icon = { Icon(Icons.Filled.LibraryAdd, contentDescription = null) },
        ),
        BottomDestination(
            route = Routes.PLAYLISTS,
            label = "Playlists",
            icon = {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null)
            },
        ),
    )

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    NavigationBar {
        destinations.forEach { dest ->
            val selected = currentRoute == dest.route ||
                (dest.route == Routes.PLAYLISTS && currentRoute == Routes.PLAYLIST_DETAIL) ||
                (dest.route == Routes.PLAYLISTS && currentRoute == Routes.LIKED)

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(dest.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = dest.icon,
                label = { Text(dest.label) },
            )
        }
    }
}
