package com.mediaplayer.android.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.mediaplayer.android.data.AuthTokenHolder
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.playback.PlayerCache
import com.mediaplayer.android.ui.theme.MediaPlayerSpacing
import java.util.concurrent.TimeUnit

@UnstableApi
@Composable
fun VideoPlayerInline(
    song: SongDto,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val videoUrl = Network.videoStreamUrl(song.id)

    // Both the data-source factory and the ExoPlayer are keyed on song.id so
    // a song change inside the inline video session (e.g. user skips via AA
    // or notification) rebuilds the pipeline against the new MediaItem and
    // picks up the current AuthTokenHolder snapshot. Without the key the
    // factory pins the old token forever and the player keeps the old source.
    val dataSourceFactory = remember(song.id) {
        // Server may invoke yt-dlp on first request — that can run 30–60s.
        // Network.okHttp's 30s readTimeout would abort; relax it to a
        // generous-but-finite upper bound rather than the zero (= forever)
        // that the previous version used. A slow trickle stream would
        // otherwise pin ExoPlayer's buffer wait indefinitely with no
        // way for the user to recover other than closing the sheet.
        val longRead = Network.okHttp.newBuilder()
            .readTimeout(120, TimeUnit.SECONDS)
            .callTimeout(180, TimeUnit.SECONDS)
            .build()
        val upstream = OkHttpDataSource.Factory(longRead).apply {
            val token = AuthTokenHolder.idToken
            val headers = buildMap {
                put("X-Api-Key", Network.API_KEY)
                if (token != null) {
                    put("Authorization", "Bearer $token")
                }
            }
            setDefaultRequestProperties(headers)
        }
        CacheDataSource.Factory()
            .setCache(PlayerCache.get(context))
            .setUpstreamDataSourceFactory(upstream)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    val exoPlayer = remember(song.id) {
        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(videoUrl))
        ExoPlayer.Builder(context).build().apply {
            setMediaSource(mediaSource)
            prepare()
            playWhenReady = true
        }
    }

    // Key on exoPlayer so a song change releases the previous instance instead
    // of leaking it until VideoPlayerInline leaves composition entirely.
    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    var fullscreen by remember { mutableStateOf(false) }

    if (!fullscreen) {
        VideoSurface(
            player = exoPlayer,
            fullscreen = false,
            onFullscreenToggle = { fullscreen = true },
            onClose = onClose,
            modifier = modifier,
        )
    } else {
        Dialog(
            onDismissRequest = { fullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            // Hide the system status + navigation bars while the dialog is
            // up so the video actually goes fullscreen — by default a
            // Compose Dialog leaves the system bars visible on top of its
            // own surface, which is what makes the nav buttons stick around
            // over the video. The transient-by-swipe behaviour preserves
            // the standard Android escape hatch.
            val view = LocalView.current
            // Key on `fullscreen` (the gate that opens this dialog) so the
            // hide/restore pair runs once per fullscreen session. Keying
            // on `view` flapped on rotation — Compose creates a new dialog
            // window, the new effect hid bars, then the old onDispose ran
            // and restored them, flashing the system bars onscreen.
            DisposableEffect(fullscreen) {
                val window = (view.parent as? DialogWindowProvider)?.window
                if (window != null) {
                    // Force the dialog window itself to fill the screen.
                    // usePlatformDefaultWidth=false alone leaves the window
                    // sized to its content's measured bounds — on some
                    // devices that ends up smaller than the screen, which
                    // is why pressing PlayerView's fullscreen button left
                    // the video looking stuck at its inline size.
                    window.setLayout(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    WindowCompat.setDecorFitsSystemWindows(window, false)
                    val controller = WindowInsetsControllerCompat(window, view)
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    controller.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
                onDispose {
                    if (window != null) {
                        WindowInsetsControllerCompat(window, view)
                            .show(WindowInsetsCompat.Type.systemBars())
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
            ) {
                VideoSurface(
                    player = exoPlayer,
                    fullscreen = true,
                    onFullscreenToggle = { fullscreen = false },
                    onClose = {
                        fullscreen = false
                        onClose()
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@UnstableApi
@Composable
private fun VideoSurface(
    player: ExoPlayer,
    fullscreen: Boolean,
    onFullscreenToggle: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                    setFullscreenButtonClickListener { onFullscreenToggle() }
                    setFullscreenButtonState(fullscreen)
                }
            },
            update = { view ->
                view.player = player
                view.setFullscreenButtonState(fullscreen)
                // Re-bind the click listener on every update so a stale
                // onFullscreenToggle capture from `factory` can't flip the
                // wrong fullscreen state after a parent recomposition.
                view.setFullscreenButtonClickListener { onFullscreenToggle() }
            },
            modifier = Modifier.fillMaxSize(),
        )
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(MediaPlayerSpacing.Xs),
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Chiudi video",
                tint = Color.White,
            )
        }
    }
}
