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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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

@UnstableApi
@Composable
fun VideoPlayerOverlay(song: SongDto, modifier: Modifier = Modifier, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val videoUrl = Network.videoStreamUrl(song.id)

    val dataSourceFactory = remember {
        val upstream = OkHttpDataSource.Factory(Network.okHttp).apply {
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

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        IconButton(
            onClick = onDismiss,
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
