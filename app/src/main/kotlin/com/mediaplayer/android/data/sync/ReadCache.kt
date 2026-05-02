package com.mediaplayer.android.data.sync

import android.content.ContentValues
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * Single-table key-value cache for backend read responses. Lives in
 * the same SQLite file as the write-queue ([SyncDb]) so an offline
 * cold start opens one file and gets both: the rows it owes the
 * server and the rows the server most recently sent it.
 *
 * # Design choice — KV vs per-entity tables
 * A typed table per entity (cached_playlists, cached_songs, ...) gives
 * better introspection and per-row indexes, but every new cached
 * endpoint costs a schema migration + a DAO. We almost always read
 * the full payload back as one DTO blob, so the per-row indexes are
 * unused. KV keeps the schema tiny and adding a cache point becomes
 * a one-line wrap inside a repository method.
 *
 * # Stale-while-revalidate pattern
 * Repositories are expected to:
 *  - Try the network first.
 *  - On success, [putJson] write-through and return the fresh value.
 *  - On `IOException`, fall back to [getJson] / [getOrNull] so the
 *    UI shows the last-seen state instead of an empty list.
 *
 * The cache is best-effort — a missing or stale entry never blocks the
 * UI. There is no TTL eviction; entries are overwritten on the next
 * online refresh and the working set stays small (one row per logical
 * screen / list).
 */
object ReadCache {

    /** Key namespaces — keep here so they don't drift across repos. */
    object Keys {
        const val PLAYLISTS_ALL = "playlists:all"
        const val LIKED_PAGE0 = "liked:page0"
        const val LIKED_STATUS = "liked:status"
        const val HISTORY_RECENT = "history:recent"
        const val FOLLOW_LIST = "follow:list"
        fun playlistDetail(id: Long) = "playlist:detail:$id"
    }

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private val writeLock = Mutex()
    private lateinit var db: SyncDb

    /**
     * Lazy init — [EventQueue.init] already opens the DB at app start,
     * but ReadCache may be touched first during a screen test. Reusing
     * the same SyncDb instance avoids two open file handles.
     */
    fun init(context: Context) {
        if (this::db.isInitialized) return
        db = SyncDb(context.applicationContext)
    }

    suspend fun <T> putJson(key: String, value: T, serializer: KSerializer<T>) {
        val encoded = json.encodeToString(serializer, value)
        withContext(Dispatchers.IO) {
            writeLock.withLock {
                val cv = ContentValues().apply {
                    put("key", key)
                    put("json", encoded)
                    put("fetched_at", System.currentTimeMillis())
                }
                db.writableDatabase.insertWithOnConflict(
                    CACHE_TABLE,
                    null,
                    cv,
                    android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE,
                )
            }
        }
    }

    suspend fun <T> getOrNull(key: String, serializer: KSerializer<T>): T? {
        val raw = getJson(key) ?: return null
        return runCatching { json.decodeFromString(serializer, raw) }.getOrNull()
    }

    private suspend fun getJson(key: String): String? = withContext(Dispatchers.IO) {
        db.readableDatabase.rawQuery(
            "SELECT json FROM $CACHE_TABLE WHERE key = ?",
            arrayOf(key),
        ).use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }

    suspend fun delete(key: String) = withContext(Dispatchers.IO) {
        writeLock.withLock {
            db.writableDatabase.delete(CACHE_TABLE, "key = ?", arrayOf(key))
        }
    }
}
