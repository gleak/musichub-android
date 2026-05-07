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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
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
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.data.OnboardingPreferences
import com.mediaplayer.android.data.dto.AppVersionRequest
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
import com.mediaplayer.android.ui.common.LocalNowPlaying
import com.mediaplayer.android.ui.common.NowPlayingState
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
import kotlinx.coroutines.launch

@UnstableApi
class MainActivity : ComponentActivity() {
    // Pending share token from a playlist share deep link. Two link
    // shapes feed this: the new `https://<host>/share/<token>` App Link
    // (auto-linkified in chats and verified at install time) and the
    // legacy `mediaplayer://share/<token>` custom-scheme URL still alive
    // in messages sent before the https switch. Held as Compose state so
    // the AppScaffold can pop the import dialog as soon as it lands.
    // `singleTask` launchMode means a deep link tapped while the app is
    // alive routes through onNewIntent — both entry points feed this
    // same state.
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
        // Accept either the legacy custom scheme (`mediaplayer://share/<t>`)
        // or the App Link form (`https://.../share/<t>`). The path layout is
        // identical in both cases — the token is the last segment after a
        // `/share/` (or `share` host for the custom scheme) prefix.
        val matchesLegacy = data.scheme == "mediaplayer" && data.host == "share"
        val matchesAppLink = data.scheme == "https" &&
            data.pathSegments.firstOrNull() == "share"
        if (!matchesLegacy && !matchesAppLink) return
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
        is AuthViewModel.State.Probe -> {
            com.mediaplayer.android.ui.auth.AuthProbeScreen(stage = s.stage)
        }
        is AuthViewModel.State.SignedIn -> {
            // Telemetry: report installed versionName as soon as we have a
            // session — before the onboarding gate, otherwise first-install
            // registrants don't show up in /api/auth/version until they finish
            // (or skip) onboarding. Fire-and-forget; keyed on user id so it
            // re-fires on account switch.
            LaunchedEffect(s.user.id) {
                runCatching {
                    Network.api.reportAppVersion(AppVersionRequest(AppVersion.VERSION))
                }
            }

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
            pickerCancelled = authVm.pickerCancelled,
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
    const val SETTINGS_DISLIKED = "profile/disliked"
    const val SETTINGS_QUEUED_EVENTS = "profile/queued-events"
    const val FIND = "find"
    const val LIBRARY = "library"
    const val PLAYLIST_DETAIL = "playlists/{playlistId}"
    const val SPOTIFY_IMPORT = "library/spotify-import"
    const val LIKED = "liked"
    const val ALBUM_LIST = "albums"
    const val ALBUM_DETAIL = "albums/{albumName}?artist={albumArtist}"
    const val ARTIST_LIST = "artists"
    const val ARTIST_DETAIL = "artists/{artistName}"
    const val GENRE_DETAIL = "genres/{tag}?display={display}"
    const val PLAYLIST_MEMBERS = "playlists/{playlistId}/members?owner={owner}"
    const val TRIM = "trim"

    fun playlistDetail(id: Long) = "playlists/$id"
    fun albumDetail(name: String, artist: String) =
        "albums/${Uri.encode(name)}?artist=${Uri.encode(artist)}"
    fun artistDetail(name: String) = "artists/${Uri.encode(name)}"
    fun genreDetail(tag: String, display: String) =
        "genres/${Uri.encode(tag)}?display=${Uri.encode(display)}"
    fun playlistMembers(id: Long, owner: Boolean) =
        "playlists/$id/members?owner=$owner"

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
        "genres",
    )

    fun belongsToLibrary(currentRoute: String?): Boolean {
        val r = currentRoute ?: return false
        return libraryPrefixes.any { r == it || r.startsWith("$it/") || r.startsWith("$it?") }
    }

    /**
     * Profile + nested settings render as a full-screen overlay above the
     * bottom nav, so they're not a tab — entered only via the avatar button
     * on Home and exited via the back chevron / system back.
     */
    fun isProfileOverlay(currentRoute: String?): Boolean {
        val r = currentRoute ?: return false
        return r == PROFILE || r.startsWith("profile/")
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
    val isPlaying by playbackVm.isPlaying.collectAsStateWithLifecycle()
    var sheetOpen by remember { mutableStateOf(false) }
    var changelogOpen by remember { mutableStateOf(false) }
    var onboardingOpen by remember { mutableStateOf(false) }

    val navController = rememberNavController()
    val networkAvailable by ConnectivityObserver.networkAvailable.collectAsStateWithLifecycle()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val hideBottomRegion = Routes.isProfileOverlay(currentRoute)

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

    // Manual "Controlla aggiornamenti" handler — bypasses 6h rate-limit
    // and per-version dismissal. Updated → pop back to Home (where the
    // banner lives) and bump attentionTick so the banner bounces; the
    // toast becomes redundant when the user lands on the banner itself.
    // Up-to-date / error stay as toasts since there's no banner to draw.
    val updateScope = rememberCoroutineScope()
    val onCheckUpdates: () -> Unit = {
        updateScope.launch {
            when (val result = AppUpdateChecker.forceCheck(ctx)) {
                AppUpdateChecker.ManualResult.Updated -> {
                    navController.navigate(Routes.HOME) {
                        // Drop everything we navigated through to reach
                        // the "Controlla" button so back from Home goes
                        // to the system home rather than back into
                        // Profile/settings.
                        popUpTo(Routes.HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                    AppUpdateChecker.requestAttention()
                }
                AppUpdateChecker.ManualResult.UpToDate ->
                    android.widget.Toast.makeText(
                        ctx, "App già aggiornata", android.widget.Toast.LENGTH_SHORT,
                    ).show()
                is AppUpdateChecker.ManualResult.Error ->
                    android.widget.Toast.makeText(
                        ctx, result.message, android.widget.Toast.LENGTH_LONG,
                    ).show()
            }
        }
    }

    // Wrap the whole Scaffold + the NowPlaying overlay in one
    // `SharedTransitionLayout` so the MiniPlayer cover and the
    // NowPlayingSheet hero cover share an animation surface. The cover
    // physically slides + scales between the two positions on open/close —
    // see `NOW_PLAYING_COVER_KEY`.
    androidx.compose.animation.SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        // Broadcast current track + playing state to every list screen so
        // SongRow can light up the active row + render MHPlayingBars
        // (mockup `mh-screens.jsx:91-95`).
        CompositionLocalProvider(
            LocalNowPlaying provides NowPlayingState(
                currentSongId = currentSong?.id,
                isPlaying = isPlaying,
            ),
        ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    // Profile + settings render as a full-screen overlay so the
                    // bottom nav (and the mini player riding above it) drop out
                    // entirely while they're open.
                    if (!hideBottomRegion) {
                        val miniVisible = currentSong != null && !sheetOpen
                        BottomRegion(
                            navController = navController,
                            miniPlayerVisible = miniVisible,
                            onExpandMiniPlayer = { sheetOpen = true },
                            playbackVm = playbackVm,
                            sharedTransitionScope = this@SharedTransitionLayout,
                        )
                    }
                },
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    NavHostBody(
                        navController = navController,
                        playbackVm = playbackVm,
                        onSignOut = onSignOut,
                        onShowChangelog = { changelogOpen = true },
                        onCheckUpdates = onCheckUpdates,
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
                    onTrim = {
                        sheetOpen = false
                        navController.navigate(Routes.TRIM)
                    },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this,
                )
            }
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

    // Non-required updates render as a banner inside HomeScreen via
    // `AppUpdateBannerHost`. Required updates render as a full-screen
    // blocking overlay here so the user cannot scroll past them.
    if (pendingUpdate?.required == true) {
        com.mediaplayer.android.ui.common.AppUpdateRequiredOverlay()
    }

    val playbackError by playbackVm.playbackError.collectAsStateWithLifecycle()
    playbackError?.let { info ->
        PlaybackErrorDialog(
            info = info,
            onDismiss = playbackVm::dismissPlaybackError,
            onRetry = playbackVm::retryCurrent,
            onRedownload = {
                playbackVm.dismissPlaybackError()
                playbackVm.redownloadCurrent()
            },
        )
    }
}

/**
 * Custom playback-error dialog matching mockup `mh-player-sheets.jsx:209-235`:
 * red triangle + `// ERRORE PLAYBACK` eyebrow, specific reason as title,
 * mono `CODE | …` pill with the error code name, three-button footer
 * (Chiudi / Riprova / Riscarica — Riscarica is the accent CTA).
 */
@Composable
private fun PlaybackErrorDialog(
    info: com.mediaplayer.android.playback.PlaybackErrorInfo,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onRedownload: () -> Unit,
) {
    val mono = com.mediaplayer.android.ui.theme.LocalMHMono.current
    val danger = Color(0xFFFF7A7A)
    val cardShape = RoundedCornerShape(16.dp)
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .background(color = Color(0xFF1A1A1A), shape = cardShape)
                .border(
                    width = 1.dp,
                    color = Color(0xFFE14848).copy(alpha = 0.25f),
                    shape = cardShape,
                )
                .padding(22.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                color = Color(0xFFE14848).copy(alpha = 0.15f),
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ReportProblem,
                            contentDescription = null,
                            tint = danger,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    Spacer(Modifier.size(10.dp))
                    Text(
                        text = "// ERRORE PLAYBACK",
                        style = mono.eyebrow,
                        color = danger,
                    )
                }
                Spacer(Modifier.size(14.dp))
                Text(
                    text = info.reason,
                    style = MaterialTheme.typography.titleMedium,
                    color = com.mediaplayer.android.ui.theme.MHColors.TextHi,
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = info.recoveryHint ?: "Brano: ${info.songTitle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = com.mediaplayer.android.ui.theme.MHColors.TextLo,
                )
                Spacer(Modifier.size(14.dp))
                Row(
                    modifier = Modifier
                        .background(color = Color(0xFF0A0A0A), shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "CODE",
                        style = mono.eyebrow,
                        color = com.mediaplayer.android.ui.theme.MHColors.TextLo2,
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = info.errorCodeName,
                        style = mono.duration,
                        color = com.mediaplayer.android.ui.theme.MHColors.TextLo,
                    )
                }
                Spacer(Modifier.size(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DialogPillButton(
                        label = "Chiudi",
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss,
                    )
                    DialogPillButton(
                        label = "Riprova",
                        modifier = Modifier.weight(1f),
                        onClick = onRetry,
                    )
                    DialogPillButton(
                        label = "Riscarica",
                        modifier = Modifier.weight(1.2f),
                        accent = true,
                        onClick = onRedownload,
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogPillButton(
    label: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    onClick: () -> Unit,
) {
    val accentColor = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .background(
                color = if (accent) accentColor else Color.White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (accent) Color(0xFF0A0A0A)
            else com.mediaplayer.android.ui.theme.MHColors.TextHi,
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
            contentDescription = "Nessuna rete — riproduzione solo dei brani scaricati",
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
    onCheckUpdates: () -> Unit,
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
                    onArtistClick = { name ->
                        navController.navigate(Routes.artistDetail(name))
                    },
                    onLikedClick = { navController.navigate(Routes.LIKED) },
                    onFindClick = { navController.navigate(Routes.FIND) },
                    onSpotifyImport = { navController.navigate(Routes.SPOTIFY_IMPORT) },
                    onSignOut = onSignOut,
                    onShowChangelog = onShowChangelog,
                    onProfileClick = { navController.navigate(Routes.PROFILE) },
                    onResumeFlush = { playbackVm.flushPlayHistoryAwait() },
                )
            }
            composable(Routes.FOR_YOU) {
                com.mediaplayer.android.ui.foryou.ForYouScreen(
                    onPlaylistClick = { p ->
                        navController.navigate(Routes.playlistDetail(p.id))
                    },
                    onProfileClick = { navController.navigate(Routes.PROFILE) },
                )
            }
            composable(Routes.PROFILE) {
                com.mediaplayer.android.ui.profile.ProfileScreen(
                    onBack = { navController.popBackStack() },
                    onShowChangelog = onShowChangelog,
                    onCheckUpdates = onCheckUpdates,
                    onSignOut = onSignOut,
                    onOpenSetting = { route -> navController.navigate(route) },
                    // Pop Profile off the stack on the way out so back from
                    // the destination returns to Home, not to the Profile
                    // overlay we just left.
                    onSongsClick = {
                        navController.navigate(Routes.LIKED) {
                            popUpTo(Routes.PROFILE) { inclusive = true }
                        }
                    },
                    onPlaylistsClick = {
                        navController.navigate(Routes.LIBRARY) {
                            popUpTo(Routes.PROFILE) { inclusive = true }
                        }
                    },
                    onArtistsClick = {
                        navController.navigate(Routes.ARTIST_LIST) {
                            popUpTo(Routes.PROFILE) { inclusive = true }
                        }
                    },
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
            composable(Routes.SETTINGS_DISLIKED) {
                com.mediaplayer.android.ui.profile.settings.DislikedScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.SETTINGS_QUEUED_EVENTS) {
                com.mediaplayer.android.ui.profile.settings.QueuedEventsScreen(
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
                    onGenreOpen = { display, tag ->
                        navController.navigate(Routes.genreDetail(tag, display))
                    },
                    onProfileClick = { navController.navigate(Routes.PROFILE) },
                )
            }
            composable(Routes.FIND) {
                FindScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.LIBRARY) {
                PlaylistsScreen(
                    onPlaylistClick = { p ->
                        navController.navigate(Routes.playlistDetail(p.id))
                    },
                    onLikedSongsClick = { navController.navigate(Routes.LIKED) },
                    onSpotifyImport = { navController.navigate(Routes.SPOTIFY_IMPORT) },
                    onFindClick = { navController.navigate(Routes.FIND) },
                    onProfileClick = { navController.navigate(Routes.PROFILE) },
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
                    onOpenMembers = { pid, isOwner ->
                        navController.navigate(Routes.playlistMembers(pid, isOwner))
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
            composable(
                route = Routes.PLAYLIST_MEMBERS,
                arguments = listOf(
                    navArgument("playlistId") { type = NavType.LongType },
                    navArgument("owner") {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                ),
            ) { backStackEntry ->
                val pid = backStackEntry.arguments?.getLong("playlistId") ?: return@composable
                val owner = backStackEntry.arguments?.getBoolean("owner") ?: false
                com.mediaplayer.android.ui.playlists.MembersScreen(
                    playlistId = pid,
                    isOwnerView = owner,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.TRIM) {
                val current = playbackVm.currentSong.collectAsStateWithLifecycle().value
                if (current == null) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                } else {
                    com.mediaplayer.android.ui.trim.TrimScreen(
                        song = current,
                        playbackVm = playbackVm,
                        onClose = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() },
                    )
                }
            }
            composable(
                route = Routes.GENRE_DETAIL,
                arguments = listOf(
                    navArgument("tag") { type = NavType.StringType },
                    navArgument("display") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) { backStackEntry ->
                val tag = backStackEntry.arguments?.getString("tag") ?: return@composable
                val display = backStackEntry.arguments?.getString("display").orEmpty()
                    .ifEmpty { tag.replaceFirstChar { it.uppercase() } }
                com.mediaplayer.android.ui.genre.GenreDetailScreen(
                    tag = tag,
                    displayName = display,
                    onBack = { navController.popBackStack() },
                    onSongClick = playbackVm::play,
                    onPlayAll = { songs -> playbackVm.playPlaylist(songs, 0) },
                    onShufflePlay = playbackVm::playPlaylistShuffled,
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
                    // Every bottom-nav tap lands at the tab's root, even if
                    // already "in" that tab via a sub-route (Library has 4
                    // children — Album / Artisti / Liked / Playlist detail).
                    // popUpTo(HOME, inclusive=false) drops every entry above
                    // start; launchSingleTop avoids re-pushing the root if
                    // it's already on top. saveState/restoreState are off
                    // on purpose: the user explicitly asked for "always go
                    // to the root", not "remember where I was".
                    navController.navigate(dest.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                },
                icon = dest.icon,
                label = { Text(dest.label) },
            )
        }
    }
}
