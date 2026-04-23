package com.mediaplayer.android.playback

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * Process-wide disk cache for audio bytes (M10).
 *
 * SimpleCache is explicit that **only one instance per cache directory
 * per process** may exist — it takes a file lock on its database at
 * open time, and a second instance will blow up with `IllegalStateException:
 * Cache folder already locked`. We singleton it here so the
 * [MediaPlaybackService] can be torn down and recreated within the same
 * process (a pause → swipe-away → replay cycle) without re-entering the
 * `new SimpleCache(...)` path.
 *
 * The cache lives under `Context.cacheDir/audio-cache` so Android will
 * reclaim space when the device is low on storage; no explicit wipe on
 * upgrade is needed. 1 GiB hard cap via
 * [LeastRecentlyUsedCacheEvictor]. At ~3-4 MB per typical track that's
 * ~250 lossy tracks warm, or about 25 FLAC albums.
 *
 * We deliberately never call [SimpleCache.release]: the cache is tied to
 * the process lifetime and Android's process kill reclaims file
 * descriptors. Releasing it mid-process (e.g. on service destroy) would
 * prevent a subsequent service instantiation from reopening it without
 * a full restart.
 */
@UnstableApi
object PlayerCache {
    private const val CACHE_DIR_NAME = "audio-cache"
    private const val MAX_BYTES = 1_024L * 1_024L * 1_024L // 1 GiB

    @Volatile
    private var instance: SimpleCache? = null

    @Synchronized
    fun get(context: Context): SimpleCache {
        instance?.let { return it }
        val appContext = context.applicationContext
        val dir = File(appContext.cacheDir, CACHE_DIR_NAME).apply { mkdirs() }
        val evictor = LeastRecentlyUsedCacheEvictor(MAX_BYTES)
        val db = StandaloneDatabaseProvider(appContext)
        return SimpleCache(dir, evictor, db).also { instance = it }
    }
}
