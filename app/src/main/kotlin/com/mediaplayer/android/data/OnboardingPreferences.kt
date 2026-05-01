package com.mediaplayer.android.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.mediaplayer.android.MediaPlayerApp
import kotlinx.coroutines.flow.first

private val Context.onboardingDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "onboarding")

/**
 * Local "user skipped the M14e tag picker" flag. Avoids a backend roundtrip
 * for the dismiss case — `getMe().onboardingComplete` only flips when GENRE
 * rows actually land in `user_taste`. Without this, a skip would re-prompt
 * the user on every fresh sign-in.
 */
class OnboardingPreferences private constructor(private val context: Context) {

    suspend fun isDismissed(): Boolean =
        context.onboardingDataStore.data.first()[DISMISSED] == true

    suspend fun markDismissed() {
        context.onboardingDataStore.edit { it[DISMISSED] = true }
    }

    companion object {
        private val DISMISSED = booleanPreferencesKey("dismissed")

        val instance: OnboardingPreferences by lazy {
            OnboardingPreferences(MediaPlayerApp.instance)
        }
    }
}
