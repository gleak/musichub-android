package com.mediaplayer.android.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mediaplayer.android.MediaPlayerApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.searchHistoryDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "search_history")

/**
 * Most-recent-first list of past search queries. Capped at [MAX] so the
 * UI can render the bottom of the page without paging. Stored as a single
 * `\n`-delimited string so order is preserved (a stringSet would lose it).
 */
class SearchHistoryStore private constructor(private val context: Context) {

    val recent: Flow<List<String>> = context.searchHistoryDataStore.data
        .map { it[KEY]?.split('\n')?.filter { s -> s.isNotBlank() } ?: emptyList() }

    suspend fun add(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        context.searchHistoryDataStore.edit { prefs ->
            val cur = prefs[KEY]?.split('\n')?.filter { it.isNotBlank() } ?: emptyList()
            val deduped = (listOf(q) + cur.filter { !it.equals(q, ignoreCase = true) }).take(MAX)
            prefs[KEY] = deduped.joinToString("\n")
        }
    }

    suspend fun remove(query: String) {
        context.searchHistoryDataStore.edit { prefs ->
            val cur = prefs[KEY]?.split('\n')?.filter { it.isNotBlank() } ?: return@edit
            prefs[KEY] = cur.filter { !it.equals(query, ignoreCase = true) }.joinToString("\n")
        }
    }

    suspend fun clear() {
        context.searchHistoryDataStore.edit { it.remove(KEY) }
    }

    companion object {
        private const val MAX = 5
        private val KEY = stringPreferencesKey("recent_queries")

        val instance: SearchHistoryStore by lazy { SearchHistoryStore(MediaPlayerApp.instance) }
    }
}
