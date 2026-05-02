package com.mediaplayer.android

import android.app.Application
import androidx.media3.common.util.UnstableApi
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.mediaplayer.android.data.AuthRepository
import com.mediaplayer.android.data.AuthTokenHolder
import com.mediaplayer.android.data.ConnectivityObserver
import com.mediaplayer.android.data.DownloadRepository
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.playback.PlayerConnection
import kotlinx.coroutines.runBlocking

/**
 * Application class — single place where app-wide singletons get wired:
 *  - Coil is handed the same [Network.okHttp] so image + API + stream
 *    traffic all share one connection pool and one cache.
 *  - [PlayerConnection] starts binding to [MediaPlaybackService] so the
 *    MediaController is ready by the time the first row is tapped.
 */
@UnstableApi
class MediaPlayerApp : Application(), SingletonImageLoader.Factory {

    companion object {
        lateinit var instance: MediaPlayerApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Resolve the persistent anonymous device id before any network call so
        // unauthenticated requests carry X-Anonymous-Id instead of the dev API key.
        AuthTokenHolder.anonymousId = runBlocking { AuthRepository.instance.anonymousId() }
        ConnectivityObserver.init()
        PlayerConnection.connect(this)
        DownloadRepository.init()
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { Network.okHttp }))
            }
            // 15% heap for in-memory cover cache — Coil's default 25%
            // crowds the rest of the app on smaller devices and the gain
            // for the 25-cover-or-so working set we actually scroll past
            // is negligible.
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.15)
                    .build()
            }
            // Explicit disk cache scoped to a dedicated dir + 50 MB cap so
            // cover thumbs survive process death without bloating internal
            // storage on long-time installs. Cover bodies are small
            // (typically 30-80 KB at 300×300) so 50 MB is ~700 covers.
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            .build()
}
