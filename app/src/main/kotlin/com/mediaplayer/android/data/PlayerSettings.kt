package com.mediaplayer.android.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mediaplayer.android.MediaPlayerApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.playerSettingsDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "player_settings")

/**
 * App-wide playback / download preferences. Backs the Settings sub-pages
 * (Crossfade, Download offline, Notifiche). All values are observable
 * via Flow so the playback service can react without forcing screens to
 * push imperatively.
 */
class PlayerSettings private constructor(private val context: Context) {

    val crossfadeSeconds: Flow<Int> = context.playerSettingsDataStore.data
        .map { it[CROSSFADE_SECONDS] ?: 0 }

    val downloadWifiOnly: Flow<Boolean> = context.playerSettingsDataStore.data
        .map { it[DOWNLOAD_WIFI_ONLY] ?: true }

    // Default OFF — auto-downloading every played song surprised users who
    // saw a foreground "Sto scaricando…" notification without having opted
    // in. Per-playlist autoSync is the explicit, visible alternative.
    val downloadAuto: Flow<Boolean> = context.playerSettingsDataStore.data
        .map { it[DOWNLOAD_AUTO] ?: false }

    /** "dark" | "light" | "system". Defaults to "dark" — the design assumes dark. */
    val theme: Flow<String> = context.playerSettingsDataStore.data
        .map { it[THEME] ?: "dark" }

    suspend fun setTheme(value: String) {
        context.playerSettingsDataStore.edit { it[THEME] = value }
    }

    /**
     * One-shot dismissal of the Xiaomi/MIUI restrictions banner. When the
     * user taps the X on the warning, this flag flips to true and the
     * banner never appears again — even if the underlying restrictions
     * are still in place. The action buttons inside the banner do NOT
     * flip this flag, only the explicit dismiss does.
     */
    val miuiWarningDismissed: Flow<Boolean> = context.playerSettingsDataStore.data
        .map { it[MIUI_WARNING_DISMISSED] ?: false }

    suspend fun setMiuiWarningDismissed(value: Boolean) {
        context.playerSettingsDataStore.edit { it[MIUI_WARNING_DISMISSED] = value }
    }

    suspend fun setCrossfadeSeconds(seconds: Int) {
        context.playerSettingsDataStore.edit { it[CROSSFADE_SECONDS] = seconds.coerceIn(0, 12) }
    }

    suspend fun setDownloadWifiOnly(value: Boolean) {
        context.playerSettingsDataStore.edit { it[DOWNLOAD_WIFI_ONLY] = value }
    }

    suspend fun setDownloadAuto(value: Boolean) {
        context.playerSettingsDataStore.edit { it[DOWNLOAD_AUTO] = value }
    }

    /** Synchronous snapshot for service-side reads that can't suspend. */
    suspend fun crossfadeSecondsNow(): Int = crossfadeSeconds.first()
    suspend fun downloadWifiOnlyNow(): Boolean = downloadWifiOnly.first()
    suspend fun downloadAutoNow(): Boolean = downloadAuto.first()

    companion object {
        private val CROSSFADE_SECONDS = intPreferencesKey("crossfade_seconds")
        private val DOWNLOAD_WIFI_ONLY = booleanPreferencesKey("download_wifi_only")
        private val DOWNLOAD_AUTO = booleanPreferencesKey("download_auto")
        private val THEME = stringPreferencesKey("theme")
        private val MIUI_WARNING_DISMISSED = booleanPreferencesKey("miui_warning_dismissed")

        val instance: PlayerSettings by lazy {
            PlayerSettings(MediaPlayerApp.instance)
        }
    }
}
