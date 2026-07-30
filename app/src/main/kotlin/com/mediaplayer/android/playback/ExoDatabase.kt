package com.mediaplayer.android.playback

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider

/**
 * One process-wide [DatabaseProvider] over `exoplayer_internal.db`, shared by
 * every Media3 component that needs it (the player cache, the download cache,
 * and the DownloadManager). Media3 namespaces each cache's rows by per-cache
 * UID so a single provider is safe — and opening one `SQLiteOpenHelper` on the
 * file instead of three avoids the lock-contention edge of multiple helpers on
 * the same DB.
 */
@UnstableApi
object ExoDatabase {
    @Volatile private var provider: DatabaseProvider? = null

    @Synchronized
    fun get(context: Context): DatabaseProvider {
        provider?.let { return it }
        return StandaloneDatabaseProvider(context.applicationContext).also { provider = it }
    }
}
