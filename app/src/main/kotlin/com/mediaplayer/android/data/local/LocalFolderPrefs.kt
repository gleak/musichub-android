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
 * Persistable URI list for SAF-pinned folders. Stores the raw tree URI
 * strings the user picked via `ACTION_OPEN_DOCUMENT_TREE`. Persistable
 * permission is granted at pick time by the activity, so re-reading the
 * URI here is enough to rebuild a DocumentFile tree on the next scan.
 */
class LocalFolderPrefs private constructor(private val store: DataStore<Preferences>) {

    val uris: Flow<Set<String>> = store.data.map { it[KEY] ?: emptySet() }

    suspend fun snapshot(): Set<String> = store.data.first()[KEY] ?: emptySet()

    suspend fun add(uri: String) {
        store.edit { prefs ->
            prefs[KEY] = (prefs[KEY] ?: emptySet()) + uri
        }
    }

    suspend fun remove(uri: String) {
        store.edit { prefs ->
            prefs[KEY] = (prefs[KEY] ?: emptySet()) - uri
        }
    }

    companion object {
        private val KEY = stringSetPreferencesKey("local_saf_uris")

        @Volatile
        private var INSTANCE: LocalFolderPrefs? = null

        fun instance(context: Context): LocalFolderPrefs =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalFolderPrefs(context.applicationContext.localFolderPrefsStore).also {
                    INSTANCE = it
                }
            }
    }
}

private val Context.localFolderPrefsStore: DataStore<Preferences> by preferencesDataStore(
    name = "local_folder_prefs",
)
