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
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.mediaplayer.android.BuildConfig
import com.mediaplayer.android.MediaPlayerApp
import kotlinx.coroutines.flow.first

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth")

/**
 * Three-way outcome of a silent-auth attempt. Lets the caller distinguish
 * "user never signed in on this device" (legit login prompt) from "we tried
 * and the Google flow failed" (LoginScreen should surface a banner with the
 * underlying cause).
 */
sealed interface SilentAuthOutcome {
    data class Success(val token: String) : SilentAuthOutcome
    /** No remembered account on the device — show the regular login CTA, no banner. */
    data object NoRemembered : SilentAuthOutcome
    /** Credential manager attempt failed (no network, Google rejected, etc.). */
    data class Failed(val throwable: Throwable) : SilentAuthOutcome
}

class AuthRepository private constructor(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)

    /**
     * Attempts silent re-authentication using previously authorized accounts.
     * Returns the Google ID token on success, null if no silent path is available.
     *
     * Note: legacy callers want a String? — this preserves the old contract.
     * For the richer outcome (distinguishing "no account" from "fail"), call
     * [silentAuth] directly.
     */
    suspend fun tryAutoSignIn(): String? =
        (silentAuth() as? SilentAuthOutcome.Success)?.token

    /**
     * Silent-auth with full outcome. Used by the UI to decide whether to show
     * the login screen plainly or with an error banner ([SilentAuthOutcome.Failed]).
     *
     * Persistence model: the Google ID token retrieved on the first sign-in
     * is stashed in DataStore and reused forever. The backend's
     * {@code GoogleAuthFilter} falls back to a local payload decode when
     * Google's tokeninfo endpoint rejects an expired token, so the same
     * `sub`/`email` resolve indefinitely — no further Credential Manager
     * (i.e. no further Google round-trip) calls are needed.
     */
    suspend fun silentAuth(): SilentAuthOutcome {
        val stored = loadStoredToken()
        if (stored != null) return SilentAuthOutcome.Success(stored)
        if (!hasEverSignedIn()) return SilentAuthOutcome.NoRemembered
        // Migration path: a user who signed in on an older build (where the
        // token wasn't persisted) still has hasEverSignedIn=true but no
        // stored token yet. Pull one fresh time from Google so they don't
        // get bounced back to the login screen, then persist it for good.
        return try {
            val option = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .setAutoSelectEnabled(true)
                .build()
            val request = GetCredentialRequest(listOf(option))
            val result = credentialManager.getCredential(context, request)
            val token = extractToken(result)
                ?: return SilentAuthOutcome.Failed(
                    IllegalStateException("No Google ID token in credential response")
                )
            storeToken(token)
            SilentAuthOutcome.Success(token)
        } catch (e: Exception) {
            SilentAuthOutcome.Failed(e)
        }
    }

    /**
     * Shows the Google account picker and returns the ID token on success.
     * Requires an Activity context so the picker UI can be anchored.
     *
     * The token is persisted to DataStore here so every subsequent app
     * launch can short-circuit straight to [silentAuth] without ever
     * re-invoking the Credential Manager.
     */
    suspend fun signIn(activityContext: Context): String {
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .build()
        val request = GetCredentialRequest(listOf(option))
        val result = credentialManager.getCredential(activityContext, request)
        val token = extractToken(result) ?: error("No Google ID token in credential response")
        storeToken(token)
        markSignedIn()
        return token
    }

    suspend fun signOut() {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
        context.authDataStore.edit {
            it.remove(HAS_SIGNED_IN)
            it.remove(ID_TOKEN)
        }
    }

    private suspend fun hasEverSignedIn(): Boolean =
        context.authDataStore.data.first()[HAS_SIGNED_IN] == true

    private suspend fun markSignedIn() {
        context.authDataStore.edit { it[HAS_SIGNED_IN] = true }
    }

    /**
     * Reads the persisted token. Two formats can land here:
     *  - the modern AES-GCM ciphertext written by [storeToken];
     *  - a plaintext JWT written by builds that pre-date Keystore wrapping
     *    (a single-conversation transient where the same key was used).
     *
     * Tried in that order. If we hit a legacy plaintext, transparently
     * re-encrypt it in place so the next read takes the encrypted path
     * and the plaintext copy disappears from disk.
     */
    private suspend fun loadStoredToken(): String? {
        val blob = context.authDataStore.data.first()[ID_TOKEN]?.takeIf { it.isNotBlank() }
            ?: return null
        TokenCipher.decrypt(blob)?.let { return it }
        if (looksLikeJwt(blob)) {
            // One-time silent upgrade from the brief plaintext window.
            storeToken(blob)
            return blob
        }
        // Garbage — drop so the silent flow re-issues from scratch.
        context.authDataStore.edit { it.remove(ID_TOKEN) }
        return null
    }

    private suspend fun storeToken(token: String) {
        val encrypted = TokenCipher.encrypt(token)
        context.authDataStore.edit { it[ID_TOKEN] = encrypted }
    }

    private fun looksLikeJwt(s: String): Boolean {
        // Header.Payload.Signature, each base64url. Coarse shape check is
        // enough — we only need to distinguish "old plaintext JWT" from
        // "random garbage" so we don't accidentally re-encrypt junk.
        val parts = s.split('.')
        return parts.size == 3 && parts.all { it.isNotEmpty() }
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
        /**
         * Persistent Google ID token, AES-GCM-sealed by [TokenCipher] before
         * landing here. Once written it stays put until the user explicitly
         * signs out — even after the token's `exp` window passes, since
         * the backend falls back to a payload-only decode to identify the
         * user. Encryption guarantees that a stolen DataStore file alone
         * yields no usable Bearer; only the on-device Keystore can unwrap.
         */
        private val ID_TOKEN = stringPreferencesKey("id_token")

        val instance: AuthRepository by lazy {
            AuthRepository(MediaPlayerApp.instance)
        }
    }
}
