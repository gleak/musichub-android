package com.mediaplayer.android.data

import com.mediaplayer.android.data.dto.SongDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Single source of truth for the user's recently-played songs.
 *
 * Replaces the per-VM [HistoryRepository.recent] fetches that used to
 * live in [com.mediaplayer.android.ui.home.HomeViewModel] and
 * [com.mediaplayer.android.ui.search.SearchViewModel] independently.
 * Now both subscribe to [recents], and the playback service prepends
 * via [markPlayed] the moment a play is recorded — so a track played
 * from the player shows up in Search's "Riprodotti di recente"
 * carousel without re-entering the screen.
 *
 * The cache holds the union of what the UI needs (max [LIMIT] entries);
 * each consumer takes its own slice via `recents.value.take(N)`.
 */
object RecentsCache {

    private val repository = HistoryRepository()
    private val mutex = Mutex()

    private val _recents = MutableStateFlow<List<SongDto>>(emptyList())
    val recents: StateFlow<List<SongDto>> = _recents.asStateFlow()

    /** Force-fetch from the server. Idempotent — safe across consumers. */
    suspend fun refresh() {
        val fresh = runCatching { repository.recent(LIMIT) }.getOrNull() ?: return
        mutex.withLock {
            _recents.value = fresh
        }
    }

    /** Refresh once if the cache is empty (cold-start). */
    suspend fun primeIfEmpty() {
        if (_recents.value.isNotEmpty()) return
        refresh()
    }

    /**
     * Optimistically prepend [song] to the cache so consumers see it
     * before the next server refresh. Dedupes by id (a re-play moves the
     * track to the head rather than duplicating it). Capped at [LIMIT].
     *
     * Caller is responsible for filtering noise — this should fire only
     * for plays that count (i.e. listened past the threshold), not every
     * micro-skip. The playback VM already gates [HistoryRepository.record]
     * the same way, so reusing that gate gives the right behavior.
     */
    fun markPlayed(song: SongDto) {
        if (song.id <= 0L) return
        val current = _recents.value
        val without = current.filterNot { it.id == song.id }
        _recents.value = (listOf(song) + without).take(LIMIT)
    }

    private const val LIMIT = 20
}
