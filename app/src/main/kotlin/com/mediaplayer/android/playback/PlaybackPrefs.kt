package com.mediaplayer.android.playback

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.media3.common.Player
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val PLAYBACK_DATASTORE = "playback"

/**
 * Backing DataStore for playback session prefs. Declared once for the whole
 * process (the `preferencesDataStore` delegate must be a single instance per
 * file name) and reached only through [PlaybackPrefs]. [SharedPreferencesMigration]
 * folds in the legacy `playback` SharedPreferences on first access.
 */
private val Context.playbackDataStore: DataStore<Preferences> by preferencesDataStore(
    name = PLAYBACK_DATASTORE,
    produceMigrations = { ctx ->
        listOf(SharedPreferencesMigration(ctx, PLAYBACK_DATASTORE))
    },
)

/**
 * Single source of truth for the playback session prefs (shuffle, repeat),
 * shared by [PlaybackViewModel] (phone UI) and [MediaPlaybackService] (the
 * service-owned shuffle + endless-queue engine).
 *
 * Shuffle in particular is app-level: the native `Player.shuffleModeEnabled`
 * is kept off and the actual ordering is done by [EndlessQueueController].
 * Routing every shuffle toggle through this one pref means an Android Auto
 * button press and a phone tap drive identical behaviour, and it keeps working
 * headless (screen off / no Activity) where no ViewModel is alive.
 */
object PlaybackPrefs {
    val SHUFFLE_KEY = booleanPreferencesKey("shuffle")
    val REPEAT_KEY = intPreferencesKey("repeat")

    private fun store(context: Context): DataStore<Preferences> =
        context.applicationContext.playbackDataStore

    fun dataStore(context: Context): DataStore<Preferences> = store(context)

    fun shuffleFlow(context: Context): Flow<Boolean> =
        store(context).data.map { it[SHUFFLE_KEY] ?: false }

    fun repeatFlow(context: Context): Flow<Int> =
        store(context).data.map { it[REPEAT_KEY] ?: Player.REPEAT_MODE_OFF }

    suspend fun currentShuffle(context: Context): Boolean =
        store(context).data.first()[SHUFFLE_KEY] ?: false

    suspend fun setShuffle(context: Context, enabled: Boolean) {
        store(context).edit { it[SHUFFLE_KEY] = enabled }
    }

    suspend fun setRepeat(context: Context, mode: Int) {
        store(context).edit { it[REPEAT_KEY] = mode }
    }
}
