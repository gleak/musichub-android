package com.mediaplayer.android

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.media3.common.util.UnstableApi
import com.mediaplayer.android.data.AuthRepository
import com.mediaplayer.android.data.SilentAuthOutcome
import com.mediaplayer.android.data.dto.UserDto
import com.mediaplayer.android.ui.ScreenTest
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * The front door's decision: splash, login, or the app itself.
 *
 * Every path to a session runs through Credential Manager, which needs Play
 * Services no JVM test has, so the repository is stood in for. What is
 * being checked is the gate's own judgement — in particular the two cases
 * that look alike and aren't: a server that says no (sign out, ask again)
 * and a server that can't be reached (carry on with the remembered
 * identity, because the local library still plays).
 */
@UnstableApi
class AuthGateTest : ScreenTest() {

    private lateinit var authRepository: AuthRepository

    private fun user(
        id: Long = 1L,
        name: String = "Antonio",
        onboardingComplete: Boolean = true,
    ) = UserDto(
        id = id,
        email = "gleak87@gmail.com",
        name = name,
        onboardingComplete = onboardingComplete,
    )

    @Before
    fun standInForCredentialManager() {
        authRepository = mockk(relaxed = true)
        AuthRepository.overrideForTest = authRepository
        // The "picker was skipped" flag is left at its default: nothing else
        // in the suite writes it, and there is no way to clear it once set.
    }

    @After
    fun dropTheStandIn() {
        AuthRepository.overrideForTest = null
    }

    /** A device with a remembered account and a reachable backend. */
    private fun signedIn(user: UserDto = user()) {
        coEvery { authRepository.silentAuth() } returns SilentAuthOutcome.Success("id-token")
        coEvery { api.getMe() } returns user
        // Home reads these on arrival.
        coEvery { api.recentSongs(any()) } returns emptyList()
        coEvery { api.listPlaylists(any()) } returns emptyList()
    }

    private fun gate() {
        setScreen {
            AuthGate(
                pendingShareToken = null,
                onShareConsumed = {},
            )
        }
        compose.waitForIdle()
    }

    @Test
    fun `a remembered account lands straight in the app`() {
        signedIn()

        gate()

        awaitText("Cerca")
        compose.onAllNodesWithText("Per ascoltare la tua libreria, accedi con Google.")
            .assertCountEquals(0)
    }

    @Test
    fun `a device that has never signed in gets the login screen`() {
        coEvery { authRepository.silentAuth() } returns SilentAuthOutcome.NoRemembered

        gate()

        awaitText("Per ascoltare la tua libreria, accedi con Google.")
    }

    /**
     * A remembered account whose silent refresh failed is not the same as no
     * account: the user is told why they are back at the login screen. The
     * usual cause is a car or a weak signal killing the round trip.
     */
    @Test
    fun `a failed silent refresh explains itself`() {
        coEvery { authRepository.silentAuth() } returns
            SilentAuthOutcome.Failed(IOException("Unable to resolve host"))

        gate()

        awaitText("Accesso non riuscito")
        compose.onNodeWithText("auth/network-error", substring = true).assertIsDisplayed()
    }

    /**
     * Token in hand, backend unreachable, and a cached identity on disk:
     * start signed in. Everything downloaded still plays, and the stored
     * Bearer authenticates the rest as soon as the network returns.
     */
    @Test
    fun `an unreachable backend falls back to the cached identity`() {
        coEvery { authRepository.silentAuth() } returns SilentAuthOutcome.Success("id-token")
        coEvery { api.getMe() } throws IOException("Unable to resolve host")
        coEvery { authRepository.loadCachedUser() } returns user()
        coEvery { api.recentSongs(any()) } returns emptyList()
        coEvery { api.listPlaylists(any()) } returns emptyList()

        gate()

        awaitText("Cerca")
    }

    /**
     * A rejected token is the opposite case and must not use the cache —
     * that would keep a revoked session alive on the device.
     */
    @Test
    fun `a rejected token goes back to the login screen even with a cache`() {
        coEvery { authRepository.silentAuth() } returns SilentAuthOutcome.Success("stale-token")
        coEvery { api.getMe() } throws IllegalStateException("HTTP 401 Unauthorized")
        coEvery { authRepository.loadCachedUser() } returns user()

        gate()

        awaitText("Accesso non riuscito", timeoutMs = 10_000)
        compose.onNodeWithText("auth/server-rejected", substring = true).assertIsDisplayed()
    }

    @Test
    fun `no cached identity and no backend also ends at the login screen`() {
        coEvery { authRepository.silentAuth() } returns SilentAuthOutcome.Success("id-token")
        coEvery { api.getMe() } throws IOException("Unable to resolve host")
        coEvery { authRepository.loadCachedUser() } returns null

        gate()

        awaitText("Accesso non riuscito", timeoutMs = 10_000)
    }

    /**
     * A fresh account has no taste signal, so the recommender would start
     * cold. The gate routes it through the tag picker before the app.
     */
    @Test
    fun `a fresh account is asked what it listens to first`() {
        signedIn(user(onboardingComplete = false))

        gate()

        awaitText("Cosa ascolti?")
    }

    @Test
    fun `an account that already has taste data skips the picker`() {
        signedIn(user(onboardingComplete = true))

        gate()

        awaitText("Cerca")
        compose.onAllNodesWithText("Cosa ascolti?").assertCountEquals(0)
    }
}
