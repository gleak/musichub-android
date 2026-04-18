package com.mediaplayer.android

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.mediaplayer.android.data.Network

/**
 * Application class — the only job right now is to hand Coil the same
 * OkHttpClient the API uses, so both share one connection pool, one cache,
 * and one set of interceptors.
 */
class MediaPlayerApp : Application(), SingletonImageLoader.Factory {
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { Network.okHttp }))
            }
            .build()
}
