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
 * Single source of truth for the user's negative-signal lists: songs and
 * artists they've excluded from recommendations.
 *
 * Each call to "Non consigliarmi questo brano/artista" from a kebab,
 * and each restore from the Disliked screen, routes through this cache
 * so every other UI surface sees the new state instantly. The kebab
 * sheet uses the song-id set to flip its label to "Già escluso" when
 * the user has already disliked the track.
 */
object DislikedSongsCache {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val repository = DislikedRepository()

    private val _dislikedSongIds = MutableStateFlow<Set<Long>>(emptySet())
    val dislikedSongIds: StateFlow<Set<Long>> = _dislikedSongIds.asStateFlow()

    /** Lowercased artist names — backend stores them case-insensitively. */
    private val _dislikedArtists = MutableStateFlow<Set<String>>(emptySet())
    val dislikedArtists: StateFlow<Set<String>> = _dislikedArtists.asStateFlow()

    /** Song ids whose disliked-state has already been resolved server-side. */
    private val resolvedSongIds = mutableSetOf<Long>()
    private val resolveMutex = Mutex()
    private var artistsResolved = false

    /** Ids toggled since the last primeSongs started — held out of the prime
     *  commit so a concurrent optimistic dislike isn't clobbered. */
    private val dirtySongIds = java.util.concurrent.ConcurrentHashMap.newKeySet<Long>()

    fun isSongDisliked(songId: Long): Boolean = songId in _dislikedSongIds.value

    fun isArtistDisliked(artist: String): Boolean =
        artist.trim().lowercase() in _dislikedArtists.value

    /** One-shot dislike — used by the kebab sheet "Non consigliarmi" entry. */
    fun dislikeSong(songId: Long, displayLabel: String? = null) {
        if (songId <= 0L) return
        if (isSongDisliked(songId)) return
        _dislikedSongIds.value = _dislikedSongIds.value + songId
        resolvedSongIds += songId
        dirtySongIds += songId
        scope.launch {
            try {
                repository.dislikeSong(songId, displayLabel = displayLabel)
            } catch (_: Throwable) {
                _dislikedSongIds.value = _dislikedSongIds.value - songId
            }
        }
    }

    /** Restore — used by Disliked screen and any "rimuovi esclusione" path. */
    fun undislikeSong(songId: Long, displayLabel: String? = null) {
        if (songId <= 0L) return
        if (!isSongDisliked(songId)) return
        _dislikedSongIds.value = _dislikedSongIds.value - songId
        resolvedSongIds += songId
        dirtySongIds += songId
        scope.launch {
            try {
                repository.undislikeSong(songId, displayLabel = displayLabel)
            } catch (_: Throwable) {
                _dislikedSongIds.value = _dislikedSongIds.value + songId
            }
        }
    }

    fun dislikeArtist(artist: String) {
        val normalized = artist.trim()
        if (normalized.isEmpty()) return
        val key = normalized.lowercase()
        if (key in _dislikedArtists.value) return
        _dislikedArtists.value = _dislikedArtists.value + key
        scope.launch {
            try {
                repository.dislikeArtist(normalized)
            } catch (_: Throwable) {
                _dislikedArtists.value = _dislikedArtists.value - key
            }
        }
    }

    fun undislikeArtist(artist: String) {
        val normalized = artist.trim()
        if (normalized.isEmpty()) return
        val key = normalized.lowercase()
        if (key !in _dislikedArtists.value) return
        _dislikedArtists.value = _dislikedArtists.value - key
        scope.launch {
            try {
                repository.undislikeArtist(normalized)
            } catch (_: Throwable) {
                _dislikedArtists.value = _dislikedArtists.value + key
            }
        }
    }

    /** Local-only mark — used when a screen has confirmed state from the server. */
    fun markSongDisliked(songId: Long, disliked: Boolean) {
        if (songId <= 0L) return
        _dislikedSongIds.value =
            if (disliked) _dislikedSongIds.value + songId
            else _dislikedSongIds.value - songId
        resolvedSongIds += songId
        dirtySongIds += songId
    }

    /** Reset on sign-out so the next user starts with a clean negative-signal
     *  set (both songs and artists), re-resolved from their own account. */
    fun clear() {
        _dislikedSongIds.value = emptySet()
        _dislikedArtists.value = emptySet()
        dirtySongIds.clear()
        scope.launch {
            resolveMutex.withLock {
                resolvedSongIds.clear()
                artistsResolved = false
            }
        }
    }

    /**
     * Resolve disliked-state for [ids] not yet known. Skips the server
     * call when every id has already been resolved. Failures degrade
     * silently — the row just won't show the "Già escluso" affordance
     * until the user revisits with a working connection.
     *
     * Batched at [STATUS_CHUNK_SIZE] (same reasoning as
     * [LikedSongsCache.prime] — Tomcat's `maxParameterCount=1000` rejects
     * single requests with too many `ids=` params).
     */
    suspend fun primeSongs(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        val unresolved = resolveMutex.withLock {
            ids.filter { it !in resolvedSongIds && it > 0L }
        }
        if (unresolved.isEmpty()) return
        unresolved.forEach { dirtySongIds.remove(it) }
        val merged = mutableSetOf<Long>()
        for (chunk in unresolved.chunked(STATUS_CHUNK_SIZE)) {
            val resolved = try {
                repository.dislikedSongStatus(chunk)
            } catch (_: Throwable) {
                return
            }
            merged += resolved
        }
        resolveMutex.withLock {
            // Hold out ids toggled while the status call was in flight so an
            // optimistic dislike isn't reverted by a stale snapshot.
            val apply = unresolved.filterNot { it in dirtySongIds }.toSet()
            val add = merged.filter { it in apply }
            _dislikedSongIds.value = (_dislikedSongIds.value - apply) + add
            resolvedSongIds += apply
        }
    }

    private const val STATUS_CHUNK_SIZE = 500

    /**
     * Resolve the disliked-artist set once per process. The list is
     * compact (one row per artist string) so a single fetch covers every
     * "is artist X disliked" lookup the kebab will ever need.
     */
    suspend fun primeArtists() {
        if (artistsResolved) return
        val fresh = try {
            repository.dislikedArtists()
        } catch (_: Throwable) {
            return
        }
        resolveMutex.withLock {
            _dislikedArtists.value = fresh.mapTo(hashSetOf()) { it.trim().lowercase() }
            artistsResolved = true
        }
    }
}
