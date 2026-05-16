package com.mediaplayer.android.data.sync

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.mediaplayer.android.data.AuthBootstrap
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
internal const val DB_VERSION = 3
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
        if (old < 3) {
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN display_label TEXT")
        }
    }

    private fun createPendingEvents(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                type TEXT NOT NULL,
                payload TEXT NOT NULL,
                dedupe_key TEXT,
                display_label TEXT,
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

    /**
     * Live per-type breakdown of unsent events. Empty until [init] runs.
     * Backs the `Eventi in coda` diagnostic screen — the user wants to
     * see WHAT is queued, not just how many things.
     */
    private val _breakdown = MutableStateFlow<Map<EventType, Int>>(emptyMap())
    val breakdown: StateFlow<Map<EventType, Int>> = _breakdown.asStateFlow()

    /**
     * Live row-level snapshot for the diagnostic screen. Each entry is
     * one outstanding queue row — id, type and the literal payload (song
     * id as a string, or an artist name, or the JSON of a play record).
     * Capped at 50 rows so an unbounded offline queue can't bloat the
     * UI; the count remains accurate via [pending].
     */
    data class UiRow(
        val id: Long,
        val type: EventType,
        val payload: String,
        val displayLabel: String?,
    )

    private val _rows = MutableStateFlow<List<UiRow>>(emptyList())
    val rows: StateFlow<List<UiRow>> = _rows.asStateFlow()

    /**
     * Earliest `next_attempt_at` in the future, or null if no row is
     * waiting on a backoff. The UI counts down to this so users know
     * the next retry is imminent. Set to `0L` here only as a placeholder —
     * actual values come from SQLite.
     */
    private val _nextRetryAt = MutableStateFlow<Long?>(null)
    val nextRetryAt: StateFlow<Long?> = _nextRetryAt.asStateFlow()

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

    suspend fun enqueuePlay(req: RecordPlayRequest, displayLabel: String? = null) =
        insert(EventType.PLAY, json.encodeToString(req), dedupeKey = null,
            displayLabel = displayLabel)

    suspend fun enqueueLike(songId: Long, displayLabel: String? = null) =
        insert(EventType.LIKE, songId.toString(),
            dedupeKey = "LIKE:$songId", replaceKey = "UNLIKE:$songId",
            displayLabel = displayLabel)

    suspend fun enqueueUnlike(songId: Long, displayLabel: String? = null) =
        insert(EventType.UNLIKE, songId.toString(),
            dedupeKey = "UNLIKE:$songId", replaceKey = "LIKE:$songId",
            displayLabel = displayLabel)

    suspend fun enqueueFollow(artist: String) =
        insert(EventType.FOLLOW, artist,
            dedupeKey = "FOLLOW:$artist", replaceKey = "UNFOLLOW:$artist",
            displayLabel = artist)

    suspend fun enqueueUnfollow(artist: String) =
        insert(EventType.UNFOLLOW, artist,
            dedupeKey = "UNFOLLOW:$artist", replaceKey = "FOLLOW:$artist",
            displayLabel = artist)

    suspend fun enqueueDislikeSong(songId: Long, displayLabel: String? = null) =
        insert(EventType.DISLIKE_SONG, songId.toString(),
            dedupeKey = "DISLIKE_SONG:$songId", replaceKey = "UNDISLIKE_SONG:$songId",
            displayLabel = displayLabel)

    suspend fun enqueueUndislikeSong(songId: Long, displayLabel: String? = null) =
        insert(EventType.UNDISLIKE_SONG, songId.toString(),
            dedupeKey = "UNDISLIKE_SONG:$songId", replaceKey = "DISLIKE_SONG:$songId",
            displayLabel = displayLabel)

    suspend fun enqueueDislikeArtist(artist: String) =
        insert(EventType.DISLIKE_ARTIST, artist,
            dedupeKey = "DISLIKE_ARTIST:$artist", replaceKey = "UNDISLIKE_ARTIST:$artist",
            displayLabel = artist)

    suspend fun enqueueUndislikeArtist(artist: String) =
        insert(EventType.UNDISLIKE_ARTIST, artist,
            dedupeKey = "UNDISLIKE_ARTIST:$artist", replaceKey = "DISLIKE_ARTIST:$artist",
            displayLabel = artist)

    private suspend fun insert(
        type: EventType,
        payload: String,
        dedupeKey: String?,
        replaceKey: String? = null,
        displayLabel: String? = null,
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
                        put("display_label", displayLabel)
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

    /**
     * Signal that a row contains a payload the dispatcher can't even
     * attempt — malformed JSON, non-numeric song id, etc. Bumping
     * exponential backoff would just jam the row forever, so the drainer
     * treats poison pills as "delete and move on".
     */
    private class PoisonPillException(message: String) : Throwable(message)

    private suspend fun drainerLoop(api: MediaPlayerApi) {
        // Don't start draining until the silent-auth attempt has resolved.
        // Without this gate, the first batch can race AuthBootstrap and fire
        // with `AuthTokenHolder.idToken == null` — the request 401s and the
        // row enters backoff for a problem that has nothing to do with the
        // network being bad. After ready completes the token is either set
        // or genuinely absent; either way the dispatch outcome is meaningful.
        AuthBootstrap.ready.await()
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
                Log.w(TAG, "dispatch ${row.type} id=${row.id} payload=${row.payload}")
                val outcome = runCatching { dispatch(api, row) }
                val exc = outcome.exceptionOrNull()
                when {
                    outcome.isSuccess -> {
                        Log.w(TAG, "dispatch ${row.type} id=${row.id} OK")
                        delete(row.id)
                    }
                    exc is PoisonPillException -> {
                        Log.w(TAG, "poison-pill ${row.type} id=${row.id}: ${exc.message}")
                        delete(row.id)
                    }
                    else -> {
                        val attempts = row.attempts + 1
                        val backoff = min(MAX_BACKOFF_MS, 1_000L * (1L shl min(attempts, 8)))
                        bumpFailure(row.id, attempts, exc?.message, backoff)
                        Log.w(TAG, "dispatch ${row.type} id=${row.id} FAILED: ${exc?.message}")
                        // Network just proved bad — bail out, the interceptor
                        // already flipped ConnectivityObserver, outer loop
                        // re-blocks until it flips back.
                        break
                    }
                }
            }
        }
    }

    private suspend fun dispatch(api: MediaPlayerApi, row: Row) {
        val songId: () -> Long = {
            row.payload.toLongOrNull()
                ?: throw PoisonPillException("non-numeric song id: ${row.payload}")
        }
        when (row.type) {
            EventType.PLAY -> api.recordPlay(json.decodeFromString(row.payload))
            EventType.LIKE -> api.likeSong(songId())
            EventType.UNLIKE -> api.unlikeSong(songId())
            EventType.FOLLOW -> api.followArtist(row.payload)
            EventType.UNFOLLOW -> api.unfollowArtist(row.payload)
            EventType.DISLIKE_SONG -> api.dislikeSong(songId())
            EventType.UNDISLIKE_SONG -> api.undislikeSong(songId())
            EventType.DISLIKE_ARTIST -> api.dislikeArtist(row.payload)
            EventType.UNDISLIKE_ARTIST -> api.undislikeArtist(row.payload)
        }
    }

    private suspend fun readBatch(): List<Row> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val out = ArrayList<Row>(BATCH_SIZE)
        val unknownIds = ArrayList<Long>()
        db.readableDatabase.rawQuery(
            "SELECT id, type, payload, attempts FROM $TABLE " +
                "WHERE next_attempt_at <= ? ORDER BY id LIMIT ?",
            arrayOf(now.toString(), BATCH_SIZE.toString()),
        ).use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                // EventType.valueOf throws on a row written by a newer build
                // (downgrade, backup-restore). One unknown row used to crash
                // readBatch and kill the drainer coroutine for the rest of
                // the process — collect them and prune outside the cursor
                // loop instead so a single bad row never stops drainage.
                val type = runCatching { EventType.valueOf(c.getString(1)) }.getOrNull()
                if (type == null) {
                    unknownIds.add(id)
                    continue
                }
                out.add(
                    Row(
                        id = id,
                        type = type,
                        payload = c.getString(2),
                        attempts = c.getInt(3),
                    )
                )
            }
        }
        if (unknownIds.isNotEmpty()) {
            writeLock.withLock {
                val w = db.writableDatabase
                for (id in unknownIds) {
                    Log.w(TAG, "pruning unknown-type queue row id=$id")
                    w.delete(TABLE, "id = ?", arrayOf(id.toString()))
                }
            }
            _pending.value = queryPendingCount()
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
        _breakdown.value = queryBreakdown()
        _rows.value = queryRows()
        _nextRetryAt.value = queryNextRetryAt()
    }

    private suspend fun queryBreakdown(): Map<EventType, Int> = withContext(Dispatchers.IO) {
        val out = LinkedHashMap<EventType, Int>()
        db.readableDatabase.rawQuery(
            "SELECT type, COUNT(*) FROM $TABLE GROUP BY type", null,
        ).use { c ->
            while (c.moveToNext()) {
                val type = runCatching { EventType.valueOf(c.getString(0)) }.getOrNull() ?: continue
                out[type] = c.getInt(1)
            }
        }
        out
    }

    private suspend fun queryRows(): List<UiRow> = withContext(Dispatchers.IO) {
        val out = ArrayList<UiRow>(50)
        db.readableDatabase.rawQuery(
            "SELECT id, type, payload, display_label FROM $TABLE ORDER BY id LIMIT 50",
            null,
        ).use { c ->
            while (c.moveToNext()) {
                val type = runCatching { EventType.valueOf(c.getString(1)) }.getOrNull() ?: continue
                out.add(
                    UiRow(
                        id = c.getLong(0),
                        type = type,
                        payload = c.getString(2),
                        displayLabel = if (c.isNull(3)) null else c.getString(3),
                    )
                )
            }
        }
        out
    }

    /**
     * Smallest `next_attempt_at` strictly in the future across all rows.
     * Returns `null` when no row is currently in backoff (so all eligible
     * rows would dispatch immediately on next network).
     */
    private suspend fun queryNextRetryAt(): Long? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        db.readableDatabase.rawQuery(
            "SELECT MIN(next_attempt_at) FROM $TABLE WHERE next_attempt_at > ?",
            arrayOf(now.toString()),
        ).use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else null
        }
    }
}
