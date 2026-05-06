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

    fun isSongDisliked(songId: Long): Boolean = songId in _dislikedSongIds.value

    fun isArtistDisliked(artist: String): Boolean =
        artist.trim().lowercase() in _dislikedArtists.value

    /** One-shot dislike — used by the kebab sheet "Non consigliarmi" entry. */
    fun dislikeSong(songId: Long, displayLabel: String? = null) {
        if (songId <= 0L) return
        if (isSongDisliked(songId)) return
        _dislikedSongIds.value = _dislikedSongIds.value + songId
        resolvedSongIds += songId
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
    }

    /**
     * Resolve disliked-state for [ids] not yet known. Skips the server
     * call when every id has already been resolved. Failures degrade
     * silently — the row just won't show the "Già escluso" affordance
     * until the user revisits with a working connection.
     */
    suspend fun primeSongs(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        val unresolved = resolveMutex.withLock {
            ids.filter { it !in resolvedSongIds && it > 0L }
        }
        if (unresolved.isEmpty()) return
        val resolved = try {
            repository.dislikedSongStatus(unresolved)
        } catch (_: Throwable) {
            return
        }
        resolveMutex.withLock {
            _dislikedSongIds.value =
                (_dislikedSongIds.value - unresolved.toSet()) + resolved
            resolvedSongIds += unresolved
        }
    }

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
