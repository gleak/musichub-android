package com.mediaplayer.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.mediaplayer.android.data.OnboardingPreferences
import com.mediaplayer.android.ui.onboarding.OnboardingScreen
import com.mediaplayer.android.ui.onboarding.OnboardingViewModel
import com.mediaplayer.android.playback.PlaybackViewModel
import com.mediaplayer.android.ui.changelog.ChangelogSheet
import com.mediaplayer.android.ui.onboarding.OnboardingSheet
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
import com.mediaplayer.android.ui.playlists.PlaylistShareImporter
import com.mediaplayer.android.ui.playlists.PlaylistsScreen
import com.mediaplayer.android.ui.playlists.SpotifyImportScreen
import com.mediaplayer.android.ui.search.SearchScreen
import com.mediaplayer.android.ui.theme.MediaPlayerTheme
import com.mediaplayer.android.update.AppUpdateChecker
import com.mediaplayer.android.update.AppUpdateDialog
import kotlinx.coroutines.launch

@UnstableApi
class MainActivity : ComponentActivity() {
    // Pending share token from a `mediaplayer://share/<token>` deep link.
    // Held as Compose state so the AppScaffold can pop the import dialog
    // as soon as it lands. `singleTask` launchMode means a deep link
    // tapped while the app is alive routes through onNewIntent — both
    // entry points feed this same state.
    private val pendingShareToken = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeShareIntent(intent)
        setContent {
            MediaPlayerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AuthGate(
                        pendingShareToken = pendingShareToken.value,
                        onShareConsumed = { pendingShareToken.value = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumeShareIntent(intent)
    }

    private fun consumeShareIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != "mediaplayer" || data.host != "share") return
        val token = data.lastPathSegment?.takeIf { it.isNotBlank() } ?: return
        pendingShareToken.value = token
    }
}

@UnstableApi
@Composable
private fun AuthGate(
    pendingShareToken: String?,
    onShareConsumed: () -> Unit,
) {
    val authVm: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
    val authState by authVm.state.collectAsStateWithLifecycle()

    when (val s = authState) {
        is AuthViewModel.State.SignedIn -> {
            // M14e: route fresh sign-ins through the tag picker before AppScaffold
            // so the recommender's cold-start path has GENRE seeds. The local
            // "dismissed" flag lets a user opt out without re-prompting on
            // every cold launch — backend onboardingComplete only flips when
            // GENRE rows actually land in user_taste.
            var localDismissed by remember { mutableStateOf<Boolean?>(null) }
            LaunchedEffect(s.user.id) {
                localDismissed = OnboardingPreferences.instance.isDismissed()
            }
            val needsOnboarding = !s.user.onboardingComplete && localDismissed == false
            when {
                localDismissed == null -> Unit // gate decision pending — render nothing for one frame
                needsOnboarding -> {
                    val onboardingVm = remember {
                        OnboardingViewModel(onResolved = { authVm.refreshMe() })
                    }
                    val saving by onboardingVm.saving.collectAsStateWithLifecycle()
                    val error by onboardingVm.error.collectAsStateWithLifecycle()
                    OnboardingScreen(
                        saving = saving,
                        error = error,
                        onContinue = onboardingVm::submit,
                        onSkip = {
                            onboardingVm.skip()
                            localDismissed = true
                        },
                    )
                }
                else -> {
                    val currentUser = CurrentUser(
                        user = s.user,
                        onSignIn = authVm::signOut, // upgrading from anon → drop anon state, return to LoginScreen
                        onSignOut = authVm::signOut,
                    )
                    CompositionLocalProvider(LocalCurrentUser provides currentUser) {
                        AppScaffold(
                            onSignOut = authVm::signOut,
                            pendingShareToken = pendingShareToken,
                            onShareConsumed = onShareConsumed,
                        )
                    }
                }
            }
        }
        else -> LoginScreen(
            state = authState,
            onSignIn = authVm::signIn,
            onContinueAsGuest = authVm::signInAnonymously,
        )
    }
}

// `internal` (was `private`) so future tests in the same module can assert on
// route shapes / deep-link patterns without copying the constants.
internal object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val FOR_YOU = "for-you"
    const val PROFILE = "profile"
    const val SETTINGS_CROSSFADE = "profile/crossfade"
    const val SETTINGS_DOWNLOAD = "profile/download"
    const val SETTINGS_THEME = "profile/theme"
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
private fun AppScaffold(
    onSignOut: () -> Unit,
    pendingShareToken: String? = null,
    onShareConsumed: () -> Unit = {},
) {
    val playbackVm: PlaybackViewModel = viewModel()
    val currentSong by playbackVm.currentSong.collectAsStateWithLifecycle()
    var sheetOpen by remember { mutableStateOf(false) }
    var changelogOpen by remember { mutableStateOf(false) }
    var onboardingOpen by remember { mutableStateOf(false) }

    val navController = rememberNavController()
    val networkAvailable by ConnectivityObserver.networkAvailable.collectAsStateWithLifecycle()

    // Android 13+ silent-notifications gap: declare in manifest is not enough,
    // the runtime permission must be requested. Trigger on first playback so the
    // ask lands at a moment the user understands ("we want to show a media
    // notification while music plays") instead of cold on app start.
    val ctx = LocalContext.current
    var notifPermAsked by remember { mutableStateOf(false) }
    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* user choice persists in OS — nothing to do */ },
    )
    LaunchedEffect(currentSong) {
        if (currentSong != null && !notifPermAsked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            notifPermAsked = true
        }
    }

    LaunchedEffect(Unit) {
        // Distinguish first-launch (null) from upgrade (different version):
        // brand-new users see the welcome sheet, returning users see what's new.
        val seen = ChangelogPreferences.instance.lastSeenVersion()
        when {
            seen == null -> onboardingOpen = true
            seen != AppVersion.VERSION -> changelogOpen = true
        }
    }

    // Self-hosted update channel — checks the backend manifest on cold
    // launch (rate-limited to once per 6h via SharedPreferences).
    val pendingUpdate by AppUpdateChecker.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { AppUpdateChecker.check(ctx) }

    // Wrap the whole Scaffold + the NowPlaying overlay in one
    // `SharedTransitionLayout` so the MiniPlayer cover and the
    // NowPlayingSheet hero cover share an animation surface. The cover
    // physically slides + scales between the two positions on open/close —
    // see `NOW_PLAYING_COVER_KEY`.
    androidx.compose.animation.SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    // Mini-player rides inside an AnimatedVisibility so it can
                    // (a) hand a sharedBounds modifier off to its cover and
                    // (b) hide cleanly while the NowPlayingSheet takes over.
                    val miniVisible = currentSong != null && !sheetOpen
                    BottomRegion(
                        navController = navController,
                        miniPlayerVisible = miniVisible,
                        onExpandMiniPlayer = { sheetOpen = true },
                        playbackVm = playbackVm,
                        sharedTransitionScope = this@SharedTransitionLayout,
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

            androidx.compose.animation.AnimatedVisibility(
                visible = sheetOpen,
                enter = androidx.compose.animation.slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
                    ),
                ) + androidx.compose.animation.fadeIn(
                    animationSpec = androidx.compose.animation.core.tween(220),
                ),
                exit = androidx.compose.animation.slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = androidx.compose.animation.core.tween(280),
                ) + androidx.compose.animation.fadeOut(
                    animationSpec = androidx.compose.animation.core.tween(180),
                ),
            ) {
                NowPlayingSheet(
                    viewModel = playbackVm,
                    onDismiss = { sheetOpen = false },
                    onArtistClick = { name ->
                        navController.navigate(Routes.artistDetail(name))
                    },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this,
                )
            }
        }
    }

    if (changelogOpen) {
        ChangelogSheet(onDismiss = { changelogOpen = false })
    }

    if (onboardingOpen) {
        val scope = rememberCoroutineScope()
        OnboardingSheet(onDismiss = {
            onboardingOpen = false
            // Mark current version as seen so the changelog sheet doesn't immediately
            // pop after dismissing the welcome sheet.
            scope.launch {
                ChangelogPreferences.instance.markSeen(AppVersion.VERSION)
            }
        })
    }

    // Incoming share-link dialog. The token gets routed up from the
    // activity's onCreate / onNewIntent and we pop the import preview
    // here so it overlays whatever screen the user happens to be on.
    if (pendingShareToken != null) {
        PlaylistShareImporter(
            token = pendingShareToken,
            onDismiss = onShareConsumed,
            onImported = { playlistId, _ ->
                onShareConsumed()
                navController.navigate(Routes.playlistDetail(playlistId))
            },
        )
    }

    pendingUpdate?.let { manifest ->
        AppUpdateDialog(
            manifest = manifest,
            onDismiss = { AppUpdateChecker.dismiss(ctx, manifest) },
        )
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
                    onFindClick = { navController.navigate(Routes.FIND) },
                    onSpotifyImport = { navController.navigate(Routes.SPOTIFY_IMPORT) },
                    onSignOut = onSignOut,
                    onShowChangelog = onShowChangelog,
                    onProfileClick = { navController.navigate(Routes.PROFILE) },
                )
            }
            composable(Routes.FOR_YOU) {
                com.mediaplayer.android.ui.foryou.ForYouScreen(
                    onPlaylistClick = { p ->
                        navController.navigate(Routes.playlistDetail(p.id))
                    },
                )
            }
            composable(Routes.PROFILE) {
                com.mediaplayer.android.ui.profile.ProfileScreen(
                    onShowChangelog = onShowChangelog,
                    onCheckUpdates = {
                        // TODO wire AppUpdateChecker.forceCheck via VM in Phase J
                    },
                    onSignOut = onSignOut,
                    onOpenSetting = { route -> navController.navigate(route) },
                )
            }
            composable(Routes.SETTINGS_CROSSFADE) {
                com.mediaplayer.android.ui.profile.settings.CrossfadeScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.SETTINGS_DOWNLOAD) {
                com.mediaplayer.android.ui.profile.settings.DownloadOfflineScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.SETTINGS_THEME) {
                com.mediaplayer.android.ui.profile.settings.ThemeScreen(
                    onBack = { navController.popBackStack() },
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

@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@UnstableApi
@Composable
private fun BottomRegion(
    navController: NavHostController,
    miniPlayerVisible: Boolean,
    onExpandMiniPlayer: () -> Unit,
    playbackVm: PlaybackViewModel,
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope,
) {
    Column {
        // AnimatedVisibility wraps MiniPlayer so its cover gets an
        // `AnimatedVisibilityScope` to feed into `Modifier.sharedBounds(...)`,
        // and so the mini fades out smoothly as the NowPlayingSheet rises in.
        androidx.compose.animation.AnimatedVisibility(
            visible = miniPlayerVisible,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut(),
        ) {
            with(sharedTransitionScope) {
                MiniPlayer(
                    viewModel = playbackVm,
                    onExpand = onExpandMiniPlayer,
                    coverModifier = Modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(
                            com.mediaplayer.android.ui.player.NOW_PLAYING_COVER_KEY
                        ),
                        animatedVisibilityScope = this@AnimatedVisibility,
                    ),
                )
            }
        }
        BottomNav(navController = navController)
    }
}

@Composable
private fun BottomNav(navController: NavHostController) {
    val destinations = listOf(
        // Pass the label as cd so TalkBack always reads the destination
        // even when the NavigationBarItem label is hidden (showLabels=false
        // states or animation transitions). Material3 reads the item label
        // by default — this is belt-and-suspenders for safety.
        BottomDestination(
            route = Routes.HOME,
            label = "Home",
            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
        ),
        BottomDestination(
            route = Routes.SEARCH,
            label = "Cerca",
            icon = { Icon(Icons.Filled.Search, contentDescription = "Cerca") },
        ),
        BottomDestination(
            route = Routes.FOR_YOU,
            label = "Per te",
            icon = {
                Icon(Icons.Filled.AutoAwesome, contentDescription = "Per te")
            },
        ),
        BottomDestination(
            route = Routes.LIBRARY,
            label = "Libreria",
            icon = {
                Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = "Libreria")
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
