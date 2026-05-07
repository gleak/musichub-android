package com.mediaplayer.android.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * On-device CRUD for [LocalPlaylist]. Backed by a single DataStore string
 * key holding the JSON-serialized list — the dataset is small (a handful
 * of playlists, each with at most a few hundred track ids), so a Room
 * table would be overkill.
 *
 * All public methods are suspend except [playlists] (the live flow) so
 * callers can chain edits without juggling threads. Writes serialize the
 * whole list and replace the value atomically inside `edit { }` — DataStore
 * collapses concurrent edits, so we never race-write.
 */
class LocalPlaylistStore private constructor(private val store: DataStore<Preferences>) {

    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(LocalPlaylist.serializer())

    val playlists: Flow<List<LocalPlaylist>> = store.data.map { prefs ->
        decode(prefs[KEY])
    }

    suspend fun snapshot(): List<LocalPlaylist> = decode(store.data.first()[KEY])

    suspend fun get(id: String): LocalPlaylist? = snapshot().firstOrNull { it.id == id }

    suspend fun create(name: String, trackIds: List<Long> = emptyList()): LocalPlaylist {
        val now = System.currentTimeMillis()
        val playlist = LocalPlaylist(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifBlank { "Playlist senza nome" },
            trackIds = trackIds.distinct(),
            createdAt = now,
            updatedAt = now,
        )
        mutate { it + playlist }
        return playlist
    }

    suspend fun rename(id: String, name: String) {
        val cleaned = name.trim().ifBlank { return }
        mutate { list ->
            list.map {
                if (it.id == id) it.copy(name = cleaned, updatedAt = System.currentTimeMillis())
                else it
            }
        }
    }

    suspend fun delete(id: String) {
        mutate { list -> list.filterNot { it.id == id } }
    }

    /** Adds [trackIds] to the tail of [id], skipping ids already present. */
    suspend fun addTracks(id: String, trackIds: List<Long>) {
        if (trackIds.isEmpty()) return
        mutate { list ->
            list.map {
                if (it.id != id) it
                else {
                    val have = it.trackIds.toSet()
                    val merged = it.trackIds + trackIds.filter { tid -> tid !in have }
                    it.copy(trackIds = merged, updatedAt = System.currentTimeMillis())
                }
            }
        }
    }

    suspend fun removeTrack(id: String, trackId: Long) {
        mutate { list ->
            list.map {
                if (it.id != id) it
                else it.copy(
                    trackIds = it.trackIds.filterNot { t -> t == trackId },
                    updatedAt = System.currentTimeMillis(),
                )
            }
        }
    }

    suspend fun reorder(id: String, trackIds: List<Long>) {
        mutate { list ->
            list.map {
                if (it.id != id) it
                else {
                    // Trust the caller's order but defensively intersect
                    // with the existing set so a stale reorder request can't
                    // smuggle in foreign ids or drop on-disk tracks.
                    val have = it.trackIds.toSet()
                    val filtered = trackIds.filter { t -> t in have }
                    val missing = it.trackIds.filterNot { t -> t in filtered }
                    it.copy(
                        trackIds = filtered + missing,
                        updatedAt = System.currentTimeMillis(),
                    )
                }
            }
        }
    }

    private suspend fun mutate(transform: (List<LocalPlaylist>) -> List<LocalPlaylist>) {
        store.edit { prefs ->
            val current = decode(prefs[KEY])
            val next = transform(current)
            prefs[KEY] = json.encodeToString(serializer, next)
        }
    }

    private fun decode(raw: String?): List<LocalPlaylist> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString(serializer, raw) }.getOrDefault(emptyList())
    }

    companion object {
        private val KEY = stringPreferencesKey("local_playlists_json")

        @Volatile
        private var INSTANCE: LocalPlaylistStore? = null

        fun instance(context: Context): LocalPlaylistStore =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalPlaylistStore(context.applicationContext.localPlaylistStore).also {
                    INSTANCE = it
                }
            }
    }
}

private val Context.localPlaylistStore: DataStore<Preferences> by preferencesDataStore(
    name = "local_playlists",
)
