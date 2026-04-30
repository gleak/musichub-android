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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

    val dataSourceFactory = remember {
        // Server may invoke yt-dlp on first request — that can run 30–60s.
        // Network.okHttp's 30s readTimeout would abort; use a fresh client without one.
        val longRead = Network.okHttp.newBuilder()
            .readTimeout(0, TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.SECONDS)
            .build()
        val upstream = OkHttpDataSource.Factory(longRead).apply {
            val token = AuthTokenHolder.idToken
            val anonId = AuthTokenHolder.anonymousId
            val headers = buildMap {
                put("X-Api-Key", Network.API_KEY)
                if (token != null) {
                    put("Authorization", "Bearer $token")
                } else if (anonId != null) {
                    put("X-Anonymous-Id", anonId)
                }
            }
            setDefaultRequestProperties(headers)
        }
        CacheDataSource.Factory()
            .setCache(PlayerCache.get(context))
            .setUpstreamDataSourceFactory(upstream)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    val exoPlayer = remember {
        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(videoUrl))
        ExoPlayer.Builder(context).build().apply {
            setMediaSource(mediaSource)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
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
            },
            modifier = Modifier.fillMaxSize(),
        )
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Close video",
                tint = Color.White,
            )
        }
    }
}
