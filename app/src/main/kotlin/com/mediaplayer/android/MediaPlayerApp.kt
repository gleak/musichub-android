package com.mediaplayer.android

import android.app.Application
import androidx.media3.common.util.UnstableApi
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.playback.PlayerConnection

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
        PlayerConnection.connect(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { Network.okHttp }))
            }
            .build()
}
