package com.mediaplayer.android.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Persists, per `appWidgetId`, the four auto-playlist tile slots the user
 * picked when placing a [QuickLaunchWidget]. Stored as a comma-separated
 * list of [QuickLaunchKind] names (e.g. `DISCOVER_DAILY,ON_REPEAT,LIKED,
 * RELEASE_RADAR`). Survives reboots; cleared on widget removal.
 */
object QuickLaunchPrefs {

    private val Context.store: DataStore<Preferences> by preferencesDataStore("widget_quick_launch")

    private fun key(appWidgetId: Int) = stringPreferencesKey("tiles_$appWidgetId")

    fun tilesFlow(context: Context, appWidgetId: Int): Flow<List<QuickLaunchKind>> =
        context.store.data.map { decode(it[key(appWidgetId)]) }

    suspend fun setTiles(context: Context, appWidgetId: Int, tiles: List<QuickLaunchKind>) {
        context.store.edit { it[key(appWidgetId)] = tiles.joinToString(",") { k -> k.name } }
    }

    suspend fun getTiles(context: Context, appWidgetId: Int): List<QuickLaunchKind> {
        val raw = context.store.data.first()[key(appWidgetId)]
        return decode(raw)
    }

    suspend fun remove(context: Context, appWidgetId: Int) {
        context.store.edit { it.remove(key(appWidgetId)) }
    }

    private fun decode(raw: String?): List<QuickLaunchKind> {
        if (raw.isNullOrBlank()) return DEFAULT_TILES
        return raw.split(',').mapNotNull { name ->
            runCatching { QuickLaunchKind.valueOf(name.trim()) }.getOrNull()
        }.ifEmpty { DEFAULT_TILES }
    }

    val DEFAULT_TILES: List<QuickLaunchKind> = listOf(
        QuickLaunchKind.DISCOVER_DAILY,
        QuickLaunchKind.ON_REPEAT,
        QuickLaunchKind.LIKED,
        QuickLaunchKind.RELEASE_RADAR,
    )
}

/**
 * Quick-launch tile targets. The first three map directly to backend
 * `AutoPlaylistKind` values (looked up via `listPlaylists(kind=…)` and
 * launched via `playPlaylist`). [LIKED] is special-cased: routes to the
 * Liked Songs screen which has its own playback path.
 */
enum class QuickLaunchKind(val label: String, val backendKind: String?) {
    DISCOVER_DAILY("Discover Daily", "DISCOVER_DAILY"),
    ON_REPEAT("In rotazione", "ON_REPEAT"),
    LIKED("Mi piace", null),
    RELEASE_RADAR("Nuove uscite", "RELEASE_RADAR"),
    DAILY_MIX_1("Daily Mix 1", "DAILY_MIX_1"),
    DAILY_MIX_2("Daily Mix 2", "DAILY_MIX_2"),
    DAILY_MIX_3("Daily Mix 3", "DAILY_MIX_3"),
    TIME_CAPSULE("Time capsule", "TIME_CAPSULE"),
}
