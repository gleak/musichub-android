package com.mediaplayer.android.playback

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.scheduler.Requirements
import com.mediaplayer.android.data.Network
import java.io.File
import java.util.concurrent.Executors

/**
 * Process-scoped singletons for the explicit download pipeline (M13).
 *
 * Download cache lives in [Context.filesDir]/downloads — Android won't
 * reclaim it under storage pressure (unlike cacheDir). [NoOpCacheEvictor]
 * means entries are kept forever until the user explicitly removes them.
 *
 * We intentionally keep [downloadCache] and [PlayerCache] as separate
 * [SimpleCache] instances with separate directories. They share the same
 * "exoplayer_internal.db" file (both use the default [StandaloneDatabaseProvider]),
 * but [CachedContentIndex] uses per-cache UIDs to namespace their tables,
 * so sharing is safe.
 */
@UnstableApi
object DownloadRoot {

    private const val DOWNLOAD_DIR = "downloads"
    private const val DOWNLOAD_THREADS = 3

    @Volatile private var _cache: SimpleCache? = null
    @Volatile private var _manager: DownloadManager? = null

    @Synchronized
    fun getDownloadCache(context: Context): SimpleCache {
        _cache?.let { return it }
        val app = context.applicationContext
        val dir = File(app.filesDir, DOWNLOAD_DIR).apply { mkdirs() }
        val db = StandaloneDatabaseProvider(app)
        return SimpleCache(dir, NoOpCacheEvictor(), db).also { _cache = it }
    }

    @Synchronized
    fun getDownloadManager(context: Context): DownloadManager {
        _manager?.let { return it }
        val app = context.applicationContext
        return DownloadManager(
            app,
            StandaloneDatabaseProvider(app),
            getDownloadCache(app),
            OkHttpDataSource.Factory(Network.okHttp),
            Executors.newFixedThreadPool(DOWNLOAD_THREADS),
        ).also {
            it.requirements = Requirements(Requirements.NETWORK_UNMETERED)
            _manager = it
        }
    }
}
