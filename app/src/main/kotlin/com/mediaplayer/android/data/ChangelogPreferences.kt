package com.mediaplayer.android.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mediaplayer.android.MediaPlayerApp
import kotlinx.coroutines.flow.first

private val Context.changelogDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "changelog")

class ChangelogPreferences private constructor(private val context: Context) {

    /** Null on a fresh install — caller treats that as "show changelog". */
    suspend fun lastSeenVersion(): String? =
        context.changelogDataStore.data.first()[LAST_SEEN_VERSION]

    suspend fun markSeen(version: String) {
        context.changelogDataStore.edit { it[LAST_SEEN_VERSION] = version }
    }

    companion object {
        private val LAST_SEEN_VERSION = stringPreferencesKey("last_seen_version")

        val instance: ChangelogPreferences by lazy {
            ChangelogPreferences(MediaPlayerApp.instance)
        }
    }
}
