package com.mediaplayer.android.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Local-only "liked" set. Backend likes are keyed by positive song ids and
 * stored server-side in `liked_songs`; on-device tracks have no row there
 * so we keep a parallel set keyed by MediaStore `_ID` (always positive).
 *
 * Persisted as a `Set<String>` of base-10 ids in DataStore Preferences —
 * lighter than a Room table and the queries needed (membership + add /
 * remove) are O(1) on a HashSet anyway.
 */
class LocalLikedStore private constructor(private val store: DataStore<Preferences>) {

    val liked: Flow<Set<Long>> = store.data.map { prefs ->
        prefs[KEY]?.mapNotNullTo(HashSet()) { it.toLongOrNull() } ?: emptySet()
    }

    suspend fun isLiked(localId: Long): Boolean {
        val raw = store.data.first()[KEY] ?: return false
        return raw.contains(localId.toString())
    }

    suspend fun setLiked(localId: Long, liked: Boolean) {
        store.edit { prefs ->
            val current = prefs[KEY].orEmpty().toMutableSet()
            if (liked) current.add(localId.toString()) else current.remove(localId.toString())
            prefs[KEY] = current
        }
    }

    companion object {
        private val KEY = stringSetPreferencesKey("local_liked_ids")

        @Volatile
        private var INSTANCE: LocalLikedStore? = null

        fun instance(context: Context): LocalLikedStore =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalLikedStore(context.applicationContext.localLikedDataStore).also {
                    INSTANCE = it
                }
            }
    }
}

private val Context.localLikedDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "local_liked",
)
