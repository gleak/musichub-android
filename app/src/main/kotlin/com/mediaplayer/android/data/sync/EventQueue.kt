package com.mediaplayer.android.data.sync

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.mediaplayer.android.data.ConnectivityObserver
import com.mediaplayer.android.data.MediaPlayerApi
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.data.dto.RecordPlayRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.min

/**
 * Persistent write-queue for fire-and-forget backend mutations
 * (play history, like/unlike, follow/unfollow). Rows live in a
 * dedicated SQLite file so they survive process death and cold
 * starts; a single background coroutine drains them whenever
 * [ConnectivityObserver.networkAvailable] reports the network is up.
 *
 * # Why a queue, not a direct call
 * Phones lose signal mid-song and lock screens for hours. A direct
 * `api.recordPlay(...)` from a ViewModel either silently swallows
 * the IOException (we lose the play count) or surfaces an error the
 * user can do nothing about. Buffering on disk decouples user
 * action from network availability — the user just keeps listening.
 *
 * # Toggle dedupe
 * Like/unlike (and follow/unfollow) are toggles. If the user taps
 * heart-on, heart-off, heart-on while offline we want to send one
 * LIKE, not three rows. [insert] does this in two passes inside a
 * transaction: delete the inverse row by `dedupe_key` (so a fresh
 * UNLIKE cancels a still-pending LIKE), then delete any duplicate of
 * the same key, then insert. PLAY events are never deduped — each
 * play is a distinct datum the recommender wants.
 *
 * # Backoff
 * On failure a row gets `attempts++` and `next_attempt_at` bumped by
 * `1s * 2^attempts`, capped at 5 minutes. A failed dispatch breaks
 * the inner loop so we don't hammer the network — the OkHttp
 * interceptor will flip [ConnectivityObserver] to offline anyway,
 * and the outer `first { it }` re-blocks until it flips back.
 */

internal const val DB_NAME = "sync.db"
internal const val DB_VERSION = 2
internal const val TABLE = "pending_events"
internal const val CACHE_TABLE = "cached_kv"

internal class SyncDb(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        createPendingEvents(db)
        createCacheKv(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) {
        // Additive only — never drop pending_events, those are user data.
        if (old < 2) createCacheKv(db)
    }

    private fun createPendingEvents(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                type TEXT NOT NULL,
                payload TEXT NOT NULL,
                dedupe_key TEXT,
                created_at INTEGER NOT NULL,
                attempts INTEGER NOT NULL DEFAULT 0,
                next_attempt_at INTEGER NOT NULL DEFAULT 0,
                last_error TEXT
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_${TABLE}_next ON $TABLE(next_attempt_at)")
        db.execSQL("CREATE INDEX idx_${TABLE}_dedupe ON $TABLE(dedupe_key)")
    }

    private fun createCacheKv(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $CACHE_TABLE (
                key TEXT PRIMARY KEY,
                json TEXT NOT NULL,
                fetched_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

enum class EventType {
    PLAY, LIKE, UNLIKE, FOLLOW, UNFOLLOW,
    DISLIKE_SONG, UNDISLIKE_SONG, DISLIKE_ARTIST, UNDISLIKE_ARTIST,
}

private data class Row(
    val id: Long,
    val type: EventType,
    val payload: String,
    val attempts: Int,
)

object EventQueue {
    private const val TAG = "EventQueue"
    private const val BATCH_SIZE = 50
    private const val MAX_BACKOFF_MS = 5 * 60_000L
    private const val IDLE_TICK_MS = 30_000L

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private lateinit var db: SyncDb
    private val writeLock = Mutex()
    private val wakeup = Channel<Unit>(Channel.CONFLATED)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var started = false

    /**
     * Live count of unsent events. Updated after every insert / dispatch /
     * backoff-bump. Profile screen collects this so the user sees the
     * queue draining in real time when the network comes back.
     */
    private val _pending = MutableStateFlow(0)
    val pending: StateFlow<Int> = _pending.asStateFlow()

    fun init(context: Context) {
        if (this::db.isInitialized) return
        db = SyncDb(context.applicationContext)
        scope.launch { refreshPendingCount() }
    }

    fun start(api: MediaPlayerApi = Network.api) {
        if (started) return
        started = true
        scope.launch { drainerLoop(api) }
    }

    suspend fun enqueuePlay(req: RecordPlayRequest) =
        insert(EventType.PLAY, json.encodeToString(req), dedupeKey = null)

    suspend fun enqueueLike(songId: Long) =
        insert(EventType.LIKE, songId.toString(),
            dedupeKey = "LIKE:$songId", replaceKey = "UNLIKE:$songId")

    suspend fun enqueueUnlike(songId: Long) =
        insert(EventType.UNLIKE, songId.toString(),
            dedupeKey = "UNLIKE:$songId", replaceKey = "LIKE:$songId")

    suspend fun enqueueFollow(artist: String) =
        insert(EventType.FOLLOW, artist,
            dedupeKey = "FOLLOW:$artist", replaceKey = "UNFOLLOW:$artist")

    suspend fun enqueueUnfollow(artist: String) =
        insert(EventType.UNFOLLOW, artist,
            dedupeKey = "UNFOLLOW:$artist", replaceKey = "FOLLOW:$artist")

    suspend fun enqueueDislikeSong(songId: Long) =
        insert(EventType.DISLIKE_SONG, songId.toString(),
            dedupeKey = "DISLIKE_SONG:$songId", replaceKey = "UNDISLIKE_SONG:$songId")

    suspend fun enqueueUndislikeSong(songId: Long) =
        insert(EventType.UNDISLIKE_SONG, songId.toString(),
            dedupeKey = "UNDISLIKE_SONG:$songId", replaceKey = "DISLIKE_SONG:$songId")

    suspend fun enqueueDislikeArtist(artist: String) =
        insert(EventType.DISLIKE_ARTIST, artist,
            dedupeKey = "DISLIKE_ARTIST:$artist", replaceKey = "UNDISLIKE_ARTIST:$artist")

    suspend fun enqueueUndislikeArtist(artist: String) =
        insert(EventType.UNDISLIKE_ARTIST, artist,
            dedupeKey = "UNDISLIKE_ARTIST:$artist", replaceKey = "DISLIKE_ARTIST:$artist")

    private suspend fun insert(
        type: EventType,
        payload: String,
        dedupeKey: String?,
        replaceKey: String? = null,
    ) {
        withContext(Dispatchers.IO) {
            writeLock.withLock {
                val w = db.writableDatabase
                w.beginTransaction()
                try {
                    if (replaceKey != null) {
                        w.delete(TABLE, "dedupe_key = ?", arrayOf(replaceKey))
                    }
                    if (dedupeKey != null) {
                        w.delete(TABLE, "dedupe_key = ?", arrayOf(dedupeKey))
                    }
                    val cv = ContentValues().apply {
                        put("type", type.name)
                        put("payload", payload)
                        put("dedupe_key", dedupeKey)
                        put("created_at", System.currentTimeMillis())
                        put("next_attempt_at", 0L)
                    }
                    w.insertOrThrow(TABLE, null, cv)
                    w.setTransactionSuccessful()
                } finally {
                    w.endTransaction()
                }
            }
        }
        refreshPendingCount()
        wakeup.trySend(Unit)
    }

    private suspend fun drainerLoop(api: MediaPlayerApi) {
        while (true) {
            // Block until the OkHttp interceptor has reported a successful
            // call (or ConnectivityManager flips us back online). Same
            // signal the offline-badge in the UI uses.
            ConnectivityObserver.networkAvailable.first { it }
            val rows = readBatch()
            if (rows.isEmpty()) {
                // Idle: wake on next enqueue, or re-check after IDLE_TICK_MS
                // in case a backoff window just elapsed.
                withTimeoutOrNull(IDLE_TICK_MS) { wakeup.receive() }
                continue
            }
            for (row in rows) {
                val outcome = runCatching { dispatch(api, row) }
                if (outcome.isSuccess) {
                    delete(row.id)
                } else {
                    val attempts = row.attempts + 1
                    val backoff = min(MAX_BACKOFF_MS, 1_000L * (1L shl min(attempts, 8)))
                    bumpFailure(row.id, attempts, outcome.exceptionOrNull()?.message, backoff)
                    Log.d(TAG, "dispatch ${row.type} id=${row.id} failed: ${outcome.exceptionOrNull()?.message}")
                    // Network just proved bad — bail out, the interceptor
                    // already flipped ConnectivityObserver, outer loop
                    // re-blocks until it flips back.
                    break
                }
            }
        }
    }

    private suspend fun dispatch(api: MediaPlayerApi, row: Row) {
        when (row.type) {
            EventType.PLAY -> api.recordPlay(json.decodeFromString(row.payload))
            EventType.LIKE -> api.likeSong(row.payload.toLong())
            EventType.UNLIKE -> api.unlikeSong(row.payload.toLong())
            EventType.FOLLOW -> api.followArtist(row.payload)
            EventType.UNFOLLOW -> api.unfollowArtist(row.payload)
            EventType.DISLIKE_SONG -> api.dislikeSong(row.payload.toLong())
            EventType.UNDISLIKE_SONG -> api.undislikeSong(row.payload.toLong())
            EventType.DISLIKE_ARTIST -> api.dislikeArtist(row.payload)
            EventType.UNDISLIKE_ARTIST -> api.undislikeArtist(row.payload)
        }
    }

    private suspend fun readBatch(): List<Row> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val out = ArrayList<Row>(BATCH_SIZE)
        db.readableDatabase.rawQuery(
            "SELECT id, type, payload, attempts FROM $TABLE " +
                "WHERE next_attempt_at <= ? ORDER BY id LIMIT ?",
            arrayOf(now.toString(), BATCH_SIZE.toString()),
        ).use { c ->
            while (c.moveToNext()) {
                out.add(
                    Row(
                        id = c.getLong(0),
                        type = EventType.valueOf(c.getString(1)),
                        payload = c.getString(2),
                        attempts = c.getInt(3),
                    )
                )
            }
        }
        out
    }

    private suspend fun delete(id: Long) {
        withContext(Dispatchers.IO) {
            writeLock.withLock {
                db.writableDatabase.delete(TABLE, "id = ?", arrayOf(id.toString()))
            }
        }
        refreshPendingCount()
    }

    private suspend fun bumpFailure(
        id: Long,
        attempts: Int,
        error: String?,
        backoffMs: Long,
    ) = withContext(Dispatchers.IO) {
        writeLock.withLock {
            val cv = ContentValues().apply {
                put("attempts", attempts)
                put("next_attempt_at", System.currentTimeMillis() + backoffMs)
                put("last_error", error?.take(500))
            }
            db.writableDatabase.update(TABLE, cv, "id = ?", arrayOf(id.toString()))
        }
    }

    /** One-shot count — prefer collecting [pending] for live updates. */
    suspend fun pendingCount(): Int = queryPendingCount()

    private suspend fun queryPendingCount(): Int = withContext(Dispatchers.IO) {
        db.readableDatabase.rawQuery("SELECT COUNT(*) FROM $TABLE", null).use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    private suspend fun refreshPendingCount() {
        _pending.value = queryPendingCount()
    }
}
