package com.mediaplayer.android

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import com.mediaplayer.android.data.AppVersion
import com.mediaplayer.android.data.ChangelogPreferences
import com.mediaplayer.android.data.ConnectivityObserver
import com.mediaplayer.android.playback.PlaybackViewModel
import com.mediaplayer.android.ui.changelog.ChangelogSheet
import com.mediaplayer.android.ui.albums.AlbumListScreen
import com.mediaplayer.android.ui.albums.AlbumScreen
import com.mediaplayer.android.ui.artists.ArtistListScreen
import com.mediaplayer.android.ui.artists.ArtistScreen
import com.mediaplayer.android.ui.auth.AuthViewModel
import com.mediaplayer.android.ui.auth.LoginScreen
import com.mediaplayer.android.ui.common.CurrentUser
import com.mediaplayer.android.ui.common.LocalCurrentUser
import com.mediaplayer.android.ui.find.FindScreen
import com.mediaplayer.android.ui.home.HomeScreen
import com.mediaplayer.android.ui.liked.LikedScreen
import com.mediaplayer.android.ui.player.MiniPlayer
import com.mediaplayer.android.ui.player.NowPlayingSheet
import com.mediaplayer.android.ui.playlists.PlaylistDetailScreen
import com.mediaplayer.android.ui.playlists.PlaylistsScreen
import com.mediaplayer.android.ui.playlists.SpotifyImportScreen
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
                    AuthGate()
                }
            }
        }
    }
}

@UnstableApi
@Composable
private fun AuthGate() {
    val authVm: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
    val authState by authVm.state.collectAsStateWithLifecycle()

    when (val s = authState) {
        is AuthViewModel.State.SignedIn -> {
            val currentUser = CurrentUser(
                user = s.user,
                onSignIn = authVm::signOut, // upgrading from anon → drop anon state, return to LoginScreen
                onSignOut = authVm::signOut,
            )
            CompositionLocalProvider(LocalCurrentUser provides currentUser) {
                AppScaffold(onSignOut = authVm::signOut)
            }
        }
        else -> LoginScreen(
            state = authState,
            onSignIn = authVm::signIn,
            onContinueAsGuest = authVm::signInAnonymously,
        )
    }
}

private object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val FIND = "find"
    const val LIBRARY = "library"
    const val PLAYLIST_DETAIL = "playlists/{playlistId}"
    const val SPOTIFY_IMPORT = "library/spotify-import"
    const val LIKED = "liked"
    const val ALBUM_LIST = "albums"
    const val ALBUM_DETAIL = "albums/{albumName}?artist={albumArtist}"
    const val ARTIST_LIST = "artists"
    const val ARTIST_DETAIL = "artists/{artistName}"

    fun playlistDetail(id: Long) = "playlists/$id"
    fun albumDetail(name: String, artist: String) =
        "albums/${Uri.encode(name)}?artist=${Uri.encode(artist)}"
    fun artistDetail(name: String) = "artists/${Uri.encode(name)}"

    /**
     * Routes that conceptually live under the "Library" tab. Any sub-route
     * the bottom nav must keep "Library" highlighted while it's open should
     * appear here. Prefix-matched, so `playlists/123` resolves to LIBRARY.
     */
    private val libraryPrefixes = listOf(
        LIBRARY,
        LIKED,
        FIND,
        SPOTIFY_IMPORT,
        "playlists",
        ALBUM_LIST,
        "albums",
        ARTIST_LIST,
        "artists",
    )

    fun belongsToLibrary(currentRoute: String?): Boolean {
        val r = currentRoute ?: return false
        return libraryPrefixes.any { r == it || r.startsWith("$it/") || r.startsWith("$it?") }
    }
}

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit,
)

@UnstableApi
@Composable
private fun AppScaffold(onSignOut: () -> Unit) {
    val playbackVm: PlaybackViewModel = viewModel()
    val currentSong by playbackVm.currentSong.collectAsStateWithLifecycle()
    var sheetOpen by remember { mutableStateOf(false) }
    var changelogOpen by remember { mutableStateOf(false) }

    val navController = rememberNavController()
    val networkAvailable by ConnectivityObserver.networkAvailable.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (ChangelogPreferences.instance.lastSeenVersion() != AppVersion.VERSION) {
            changelogOpen = true
        }
    }

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
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            NavHostBody(
                navController = navController,
                playbackVm = playbackVm,
                onSignOut = onSignOut,
                onShowChangelog = { changelogOpen = true },
            )
            if (!networkAvailable) {
                OfflineBadge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(8.dp),
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

    if (changelogOpen) {
        ChangelogSheet(onDismiss = { changelogOpen = false })
    }
}

@Composable
private fun OfflineBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(36.dp)
            .background(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.CloudOff,
            contentDescription = "No network — playing only downloaded songs",
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(20.dp),
        )
    }
}

@UnstableApi
@Composable
private fun NavHostBody(
    navController: NavHostController,
    playbackVm: PlaybackViewModel,
    onSignOut: () -> Unit,
    onShowChangelog: () -> Unit,
) {
    NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onSongClick = playbackVm::play,
                    onPlaylistClick = { p ->
                        navController.navigate(Routes.playlistDetail(p.id))
                    },
                    onLikedClick = { navController.navigate(Routes.LIKED) },
                    onSignOut = onSignOut,
                    onShowChangelog = onShowChangelog,
                )
            }
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
            composable(Routes.LIBRARY) {
                PlaylistsScreen(
                    onPlaylistClick = { p ->
                        navController.navigate(Routes.playlistDetail(p.id))
                    },
                    onLikedSongsClick = { navController.navigate(Routes.LIKED) },
                    onSpotifyImport = { navController.navigate(Routes.SPOTIFY_IMPORT) },
                    onFindClick = { navController.navigate(Routes.FIND) },
                    onSignOut = onSignOut,
                )
            }
            composable(Routes.SPOTIFY_IMPORT) {
                SpotifyImportScreen(
                    onBack = { navController.popBackStack() },
                    onPlaylistCreated = { playlistId ->
                        navController.navigate(Routes.playlistDetail(playlistId)) {
                            popUpTo(Routes.LIBRARY)
                        }
                    },
                )
            }
            composable(Routes.LIKED) {
                LikedScreen(
                    onBack = { navController.popBackStack() },
                    onPlayFromIndex = { songs, index ->
                        playbackVm.playPlaylist(songs, index)
                    },
                    onShufflePlay = playbackVm::playPlaylistShuffled,
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
                    onShufflePlay = playbackVm::playPlaylistShuffled,
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
            route = Routes.HOME,
            label = "Home",
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
        ),
        BottomDestination(
            route = Routes.SEARCH,
            label = "Search",
            icon = { Icon(Icons.Filled.Search, contentDescription = null) },
        ),
        BottomDestination(
            route = Routes.LIBRARY,
            label = "Your Library",
            icon = {
                Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = null)
            },
        ),
    )

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    NavigationBar(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
    ) {
        destinations.forEach { dest ->
            val selected = currentRoute == dest.route ||
                (dest.route == Routes.LIBRARY && Routes.belongsToLibrary(currentRoute))

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
