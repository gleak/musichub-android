package com.mediaplayer.android.data

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.mediaplayer.android.BuildConfig
import com.mediaplayer.android.MediaPlayerApp
import kotlinx.coroutines.flow.first

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth")

class AuthRepository private constructor(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)

    /**
     * Attempts silent re-authentication using previously authorized accounts.
     * Returns the Google ID token on success, null if no silent path is available.
     */
    suspend fun tryAutoSignIn(): String? {
        if (!hasEverSignedIn()) return null
        return try {
            val option = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .setAutoSelectEnabled(true)
                .build()
            val request = GetCredentialRequest(listOf(option))
            val result = credentialManager.getCredential(context, request)
            extractToken(result)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Shows the Google account picker and returns the ID token on success.
     * Requires an Activity context so the picker UI can be anchored.
     */
    suspend fun signIn(activityContext: Context): String {
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .build()
        val request = GetCredentialRequest(listOf(option))
        val result = credentialManager.getCredential(activityContext, request)
        val token = extractToken(result) ?: error("No Google ID token in credential response")
        markSignedIn()
        return token
    }

    suspend fun signOut() {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
        context.authDataStore.edit { it.remove(HAS_SIGNED_IN) }
    }

    private suspend fun hasEverSignedIn(): Boolean =
        context.authDataStore.data.first()[HAS_SIGNED_IN] == true

    private suspend fun markSignedIn() {
        context.authDataStore.edit { it[HAS_SIGNED_IN] = true }
    }

    private fun extractToken(result: GetCredentialResponse): String? {
        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return GoogleIdTokenCredential.createFrom(credential.data).idToken
        }
        return null
    }

    companion object {
        private val HAS_SIGNED_IN = booleanPreferencesKey("has_signed_in")

        val instance: AuthRepository by lazy {
            AuthRepository(MediaPlayerApp.instance)
        }
    }
}
