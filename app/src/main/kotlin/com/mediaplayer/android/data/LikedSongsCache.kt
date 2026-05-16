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

    /** Optimistic toggle. Returns the new liked state (true = now liked). */
    fun toggle(songId: Long, displayLabel: String? = null): Boolean {
        val wasLiked = songId in _likedIds.value
        _likedIds.value = if (wasLiked) _likedIds.value - songId else _likedIds.value + songId
        resolvedIds += songId
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
            ids.filter { it !in resolvedIds }
        }
        if (unresolved.isEmpty()) return
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
            _likedIds.value = (_likedIds.value - unresolved.toSet()) + liked
            resolvedIds += unresolved
        }
    }

    /** Below Tomcat's default `maxParameterCount=1000` with comfortable headroom. */
    private const val STATUS_CHUNK_SIZE = 500

    fun isLiked(songId: Long): Boolean = songId in _likedIds.value
}
