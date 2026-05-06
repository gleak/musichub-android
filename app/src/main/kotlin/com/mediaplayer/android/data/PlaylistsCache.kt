package com.mediaplayer.android.data

import com.mediaplayer.android.data.dto.PlaylistDetailDto
import com.mediaplayer.android.data.dto.PlaylistDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Single source of truth for the user's playlists.
 *
 * Every screen that lists or mutates playlists (Home, Playlists tab,
 * AddToPlaylist kebab sheet, PlaylistDetail, share importer) reads from
 * [playlists] / [details] and routes mutations through the methods below
 * so an action performed on one surface is reflected on every other.
 *
 * Mutations are optimistic: cache state flips first, then the
 * [PlaylistRepository] call runs. On failure we revert and rethrow so
 * callers can show an error.
 */
object PlaylistsCache {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val repository = PlaylistRepository()

    private val _playlists = MutableStateFlow<List<PlaylistDto>>(emptyList())
    val playlists: StateFlow<List<PlaylistDto>> = _playlists.asStateFlow()

    private val _details = MutableStateFlow<Map<Long, PlaylistDetailDto>>(emptyMap())
    val details: StateFlow<Map<Long, PlaylistDetailDto>> = _details.asStateFlow()

    /** Last-load error, cleared on the next successful refresh. */
    private val _listError = MutableStateFlow<Throwable?>(null)
    val listError: StateFlow<Throwable?> = _listError.asStateFlow()

    /**
     * True while [refresh] is in flight on first load — surfaces a loading
     * placeholder. Subsequent refreshes (pull-to-refresh) keep showing
     * stale data, which is what the screens want.
     */
    private val _initialLoading = MutableStateFlow(true)
    val initialLoading: StateFlow<Boolean> = _initialLoading.asStateFlow()

    private val mutex = Mutex()

    /** Force-fetch the list. Idempotent — safe to call from multiple screens. */
    suspend fun refresh() {
        try {
            val fresh = repository.list()
            mutex.withLock {
                _playlists.value = fresh
                // Drop cached details for playlists that no longer exist
                // (deleted server-side, or a session change).
                val keepIds = fresh.mapTo(hashSetOf()) { it.id }
                _details.value = _details.value.filterKeys { it in keepIds }
                _listError.value = null
            }
        } catch (t: Throwable) {
            _listError.value = t
            throw t
        } finally {
            _initialLoading.value = false
        }
    }

    /**
     * Refresh the list only when the cache is empty. Avoids hammering the
     * backend when several screens that all need the list are visible.
     */
    suspend fun primeIfEmpty() {
        if (_playlists.value.isNotEmpty()) {
            _initialLoading.value = false
            return
        }
        runCatching { refresh() }
    }

    /** Force-fetch one playlist's detail (used by PlaylistDetailScreen). */
    suspend fun refreshDetail(id: Long): PlaylistDetailDto {
        val fresh = repository.detail(id)
        mutex.withLock {
            putDetail(fresh)
        }
        return fresh
    }

    suspend fun create(name: String): PlaylistDto {
        val created = repository.create(name)
        mutex.withLock {
            // Append optimistically; refresh() would also pick it up but
            // would cost an extra round-trip.
            if (_playlists.value.none { it.id == created.id }) {
                _playlists.value = _playlists.value + created
            }
        }
        return created
    }

    suspend fun delete(id: Long) {
        val before = _playlists.value
        val beforeDetails = _details.value
        // Optimistic local removal so the row drops immediately.
        mutex.withLock {
            _playlists.value = before.filterNot { it.id == id }
            _details.value = beforeDetails - id
        }
        try {
            repository.delete(id)
        } catch (t: Throwable) {
            mutex.withLock {
                _playlists.value = before
                _details.value = beforeDetails
            }
            throw t
        }
    }

    suspend fun rename(id: Long, name: String): PlaylistDto {
        val renamed = repository.rename(id, name)
        mutex.withLock {
            _playlists.value = _playlists.value.map {
                if (it.id == id) it.copy(name = renamed.name, updatedAt = renamed.updatedAt) else it
            }
            _details.value[id]?.let { d ->
                putDetail(d.copy(name = renamed.name, updatedAt = renamed.updatedAt))
            }
        }
        return renamed
    }

    suspend fun setAutoSync(id: Long, enabled: Boolean): PlaylistDto {
        // Optimistic flip first so the toggle UI doesn't lag the tap.
        val before = _playlists.value
        val beforeDetail = _details.value[id]
        mutex.withLock {
            _playlists.value = before.map {
                if (it.id == id) it.copy(autoSync = enabled) else it
            }
            beforeDetail?.let { putDetail(it.copy(autoSync = enabled)) }
        }
        try {
            val updated = repository.setAutoSync(id, enabled)
            mutex.withLock {
                _playlists.value = _playlists.value.map {
                    if (it.id == id) updated else it
                }
            }
            return updated
        } catch (t: Throwable) {
            mutex.withLock {
                _playlists.value = before
                if (beforeDetail != null) putDetail(beforeDetail) else dropDetail(id)
            }
            throw t
        }
    }

    suspend fun addSong(playlistId: Long, songId: Long): PlaylistDetailDto {
        val updated = repository.addSong(playlistId, songId)
        mutex.withLock {
            putDetail(updated)
            bumpListCount(playlistId, updated.songs.size)
        }
        return updated
    }

    suspend fun removeSong(playlistId: Long, songId: Long): PlaylistDetailDto {
        val updated = repository.removeSong(playlistId, songId)
        mutex.withLock {
            putDetail(updated)
            bumpListCount(playlistId, updated.songs.size)
        }
        return updated
    }

    suspend fun reorder(playlistId: Long, songIds: List<Long>): PlaylistDetailDto {
        val updated = repository.reorder(playlistId, songIds)
        mutex.withLock {
            putDetail(updated)
        }
        return updated
    }

    suspend fun acceptShare(token: String): PlaylistDetailDto {
        val accepted = repository.acceptShare(token)
        mutex.withLock {
            putDetail(accepted)
            // Synthesize a list-row entry from the detail so Home/Playlists
            // pick it up without waiting for the next refresh.
            val tile = PlaylistDto(
                id = accepted.id,
                name = accepted.name,
                songCount = accepted.songs.size,
                createdAt = accepted.createdAt,
                updatedAt = accepted.updatedAt,
                kind = accepted.kind,
                lastRefreshedAt = accepted.lastRefreshedAt,
                autoSync = accepted.autoSync,
                ownerId = accepted.ownerId,
                ownerName = accepted.ownerName,
                isOwner = accepted.isOwner,
                memberCount = accepted.memberCount,
            )
            if (_playlists.value.none { it.id == accepted.id }) {
                _playlists.value = _playlists.value + tile
            } else {
                _playlists.value = _playlists.value.map { if (it.id == tile.id) tile else it }
            }
        }
        return accepted
    }

    /**
     * The set of playlist ids currently known to contain [songId]. Backed
     * by the cached [details] map only — playlists whose detail hasn't
     * been loaded yet are silently absent. Used by the kebab sheet to
     * show "already in playlist X" checkmarks.
     */
    fun playlistIdsContaining(songId: Long): Set<Long> {
        val out = mutableSetOf<Long>()
        for ((pid, detail) in _details.value) {
            if (detail.songs.any { it.song.id == songId }) out += pid
        }
        return out
    }

    /** Caller must hold [mutex]. */
    private fun putDetail(detail: PlaylistDetailDto) {
        _details.value = _details.value + (detail.id to detail)
    }

    /** Caller must hold [mutex]. */
    private fun dropDetail(id: Long) {
        _details.value = _details.value - id
    }

    /** Caller must hold [mutex]. */
    private fun bumpListCount(id: Long, count: Int) {
        _playlists.value = _playlists.value.map {
            if (it.id == id) it.copy(songCount = count) else it
        }
    }
}
