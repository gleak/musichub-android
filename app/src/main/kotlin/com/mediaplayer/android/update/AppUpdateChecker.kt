package com.mediaplayer.android.update

import android.content.Context
import com.mediaplayer.android.BuildConfig
import com.mediaplayer.android.data.dto.AppUpdateDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

/**
 * Polls the backend's update manifest on launch and exposes the result
 * as a StateFlow the UI can hang an in-app dialog off of. "Later" sets
 * a per-version skip flag in SharedPreferences so the prompt doesn't
 * harass the user every cold-start of the same update — they'll see
 * it again next time a NEWER version drops.
 */
object AppUpdateChecker {

    private const val PREFS = "app_update"
    private const val KEY_DISMISSED_VERSION_CODE = "dismissed_version_code"
    private const val KEY_LAST_CHECK_AT = "last_check_at"

    /** Don't poll more than once per 6h on cold launches. */
    private val MIN_CHECK_INTERVAL_MS = TimeUnit.HOURS.toMillis(6)

    private val _state = MutableStateFlow<AppUpdateDto?>(null)
    val state: StateFlow<AppUpdateDto?> = _state.asStateFlow()

    sealed interface ManualResult {
        /** A new version is available; the dialog state has been refreshed. */
        data object Updated : ManualResult
        /** Local build is at or above the published version. */
        data object UpToDate : ManualResult
        data class Error(val message: String) : ManualResult
    }

    suspend fun check(
        context: Context,
        repository: AppUpdateRepository = AppUpdateRepository(),
    ) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val lastCheck = prefs.getLong(KEY_LAST_CHECK_AT, 0L)
        if (now - lastCheck < MIN_CHECK_INTERVAL_MS && _state.value != null) return

        val manifest = runCatching { repository.latest() }.getOrNull() ?: return
        prefs.edit().putLong(KEY_LAST_CHECK_AT, now).apply()

        // Server is newer than us?
        if (manifest.versionCode <= BuildConfig.VERSION_CODE) {
            _state.value = null
            return
        }
        // User dismissed THIS version? Don't re-prompt unless newer.
        val dismissed = prefs.getInt(KEY_DISMISSED_VERSION_CODE, 0)
        if (!manifest.required && dismissed >= manifest.versionCode) {
            _state.value = null
            return
        }
        _state.value = manifest
    }

    /**
     * Manual "check for updates" entry. Bypasses the 6h rate-limit and
     * the per-version dismissal flag — the user explicitly asked, so
     * they get a clear yes/no answer regardless of prior state.
     */
    suspend fun forceCheck(
        context: Context,
        repository: AppUpdateRepository = AppUpdateRepository(),
    ): ManualResult {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val manifest = try {
            repository.latest()
        } catch (t: Throwable) {
            return ManualResult.Error(t.message ?: "Couldn't reach the update server")
        }
        prefs.edit().putLong(KEY_LAST_CHECK_AT, System.currentTimeMillis()).apply()
        if (manifest == null || manifest.versionCode <= BuildConfig.VERSION_CODE) {
            _state.value = null
            return ManualResult.UpToDate
        }
        _state.value = manifest
        return ManualResult.Updated
    }

    fun dismiss(context: Context, manifest: AppUpdateDto) {
        if (manifest.required) return // can't dismiss a forced update
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_DISMISSED_VERSION_CODE, manifest.versionCode)
            .apply()
        _state.value = null
    }

    fun consume() {
        _state.value = null
    }
}
