package com.mediaplayer.android.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Single source of truth for the set of song ids the user has liked.
 *
 * Every UI surface that shows a heart (lists, player, mini-player, kebab
 * sheets) reads from [likedIds] and routes mutations through [toggle] so a
 * like performed anywhere is reflected everywhere instantly.
 *
 * Mutations are optimistic: the in-memory set flips first, then the
 * [LikedRepository] call queues a sync event. On failure we revert the
 * local flip so the UI reflects the last known good state.
 */
object LikedSongsCache {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val repository = LikedRepository()

    private val _likedIds = MutableStateFlow<Set<Long>>(emptySet())
    val likedIds: StateFlow<Set<Long>> = _likedIds.asStateFlow()

    /** Ids whose liked-state has already been resolved against the server. */
    private val resolvedIds = mutableSetOf<Long>()
    private val resolveMutex = Mutex()

    /**
     * Ids the user toggled (or a service mirror marked) since the last prime
     * started. A concurrent optimistic like must not be clobbered by the
     * server snapshot from a [prime] that was already in flight when the tap
     * happened — those ids are held out of the prime commit.
     */
    private val dirtyIds = java.util.concurrent.ConcurrentHashMap.newKeySet<Long>()

    /** Optimistic toggle. Returns the new liked state (true = now liked). */
    fun toggle(songId: Long, displayLabel: String? = null): Boolean {
        // Local tracks use negative ids; their likes live in LocalLikedStore,
        // not the backend. Never route them through the server like/unlike.
        if (songId <= 0L) return songId in _likedIds.value
        val wasLiked = songId in _likedIds.value
        _likedIds.value = if (wasLiked) _likedIds.value - songId else _likedIds.value + songId
        resolvedIds += songId
        dirtyIds += songId
        scope.launch {
            try {
                if (wasLiked) repository.unlike(songId, displayLabel = displayLabel)
                else repository.like(songId, displayLabel = displayLabel)
            } catch (_: Throwable) {
                _likedIds.value = if (wasLiked) _likedIds.value + songId else _likedIds.value - songId
            }
        }
        return !wasLiked
    }

    /** Local-only mark (e.g. mirrored from a service-side toggle). */
    fun markLiked(songId: Long, liked: Boolean) {
        _likedIds.value = if (liked) _likedIds.value + songId else _likedIds.value - songId
        resolvedIds += songId
        dirtyIds += songId
    }

    /** Reset to empty on sign-out so user A's hearts never show for user B.
     *  Clears resolvedIds too so B's real state gets re-fetched from scratch. */
    fun clear() {
        _likedIds.value = emptySet()
        dirtyIds.clear()
        scope.launch { resolveMutex.withLock { resolvedIds.clear() } }
    }

    /**
     * Resolve liked-state for [ids] not yet known. Skips the server call when
     * every id is already resolved. Failures degrade silently — the heart
     * just stays empty until the user either toggles it or revisits the
     * screen with a working connection.
     *
     * Batched at [STATUS_CHUNK_SIZE] so a tap on a large playlist doesn't
     * fan out into a single `GET /api/liked/status?ids=...` carrying every
     * song id — Tomcat caps parameter counts at 1000 and rejects the request
     * with a 500. Chunks are merged into the final set so the UI sees
     * one coherent update.
     */
    suspend fun prime(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        val unresolved = resolveMutex.withLock {
            // Skip local (negative) ids — a `GET /api/liked/status?ids=-N`
            // would 404/pollute; local hearts come from LocalLikedStore.
            ids.filter { it > 0L && it !in resolvedIds }
        }
        if (unresolved.isEmpty()) return
        // Only mid-flight toggles (after this point) should win over the
        // server snapshot — clear any stale dirty marks for the ids we're
        // about to resolve.
        unresolved.forEach { dirtyIds.remove(it) }
        val liked = mutableSetOf<Long>()
        for (chunk in unresolved.chunked(STATUS_CHUNK_SIZE)) {
            val resolved = try {
                repository.status(chunk)
            } catch (_: Throwable) {
                return
            }
            liked += resolved
        }
        resolveMutex.withLock {
            // Hold out ids the user toggled while status() was in flight — an
            // optimistic like must not be reverted by a snapshot that predates
            // it. Those stay unresolved so a later prime settles them.
            val apply = unresolved.filterNot { it in dirtyIds }.toSet()
            val add = liked.filter { it in apply }
            _likedIds.value = (_likedIds.value - apply) + add
            resolvedIds += apply
        }
    }

    /** Below Tomcat's default `maxParameterCount=1000` with comfortable headroom. */
    private const val STATUS_CHUNK_SIZE = 500

    fun isLiked(songId: Long): Boolean = songId in _likedIds.value
}
