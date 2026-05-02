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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

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

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Resolve the persistent anonymous device id off the main thread; the
        // OkHttp interceptor awaits this Deferred on its dispatcher thread for
        // the very first cold-start request and skips the await once resolved.
        // Replaces a runBlocking call that was blocking onCreate on DataStore IO.
        AuthTokenHolder.anonymousIdDeferred = applicationScope.async {
            AuthRepository.instance.anonymousId().also { AuthTokenHolder.anonymousId = it }
        }
        ConnectivityObserver.init()
        PlayerConnection.connect(this)
        DownloadRepository.init()
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { Network.okHttp }))
            }
            // 22% heap for in-memory cover cache — bumped from 15% after
            // adding sized ImageRequests + remember() to SongCover so the
            // working set is small per-cover but accumulates across more
            // distinct covers as the user scrolls multiple shelves.
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.22)
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
