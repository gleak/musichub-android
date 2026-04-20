package com.mediaplayer.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
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

/**
 * Nav destinations. Kept as plain constants because the graph is small
 * enough that a sealed hierarchy would be ceremony for ceremony's sake.
 */
private object Routes {
    const val SEARCH = "search"
    const val PLAYLISTS = "playlists"
    const val PLAYLIST_DETAIL = "playlists/{playlistId}"
    fun playlistDetail(id: Long) = "playlists/$id"
}

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit,
)

@UnstableApi
@Composable
private fun AppScaffold() {
    // Single activity-scoped PlaybackViewModel so search, playlists,
    // the mini-player, and Now Playing sheet all observe one source of truth.
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
                )
            }
            composable(Routes.PLAYLISTS) {
                PlaylistsScreen(
                    onPlaylistClick = { p ->
                        navController.navigate(Routes.playlistDetail(p.id))
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
        }
    }

    if (sheetOpen) {
        NowPlayingSheet(
            viewModel = playbackVm,
            onDismiss = { sheetOpen = false },
        )
    }
}

/**
 * Bottom region: mini-player (if a track is loaded) stacked above the
 * NavigationBar. Rendered as the Scaffold's `bottomBar` slot so content
 * is padded above it automatically.
 */
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
                // Treat the detail route as "still inside Playlists" so the
                // tab stays lit while drilling in.
                (dest.route == Routes.PLAYLISTS && currentRoute == Routes.PLAYLIST_DETAIL)

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
