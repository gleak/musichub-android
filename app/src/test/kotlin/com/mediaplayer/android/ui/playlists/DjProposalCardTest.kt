package com.mediaplayer.android.ui.playlists

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mediaplayer.android.data.dto.PlaylistDetailDto
import com.mediaplayer.android.ui.ScreenTest
import io.mockk.coEvery
import io.mockk.coVerify
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * Il patto col DJ, scritto dove serve: sulla playlist che sta per essere
 * riscritta. Senza, la prima sovrascrittura si legge come una perdita di
 * dati — l'utente torna sulla proposta di ieri e ci trova altro.
 */
class DjProposalCardTest : ScreenTest() {

    private fun promoted() = PlaylistDetailDto(
        id = 5L,
        name = "Cantautori di Casa",
        createdAt = "2026-08-01T00:00:00Z",
        updatedAt = "2026-08-22T00:00:00Z",
        songs = emptyList(),
        kind = "USER",
    )

    private fun card() {
        setScreen { DjProposalCard(playlistId = 5L) }
        compose.waitForIdle()
    }

    @Test
    fun `it states the deal before anything is overwritten`() {
        card()

        compose.onNodeWithText("si rinnova da sola", substring = true).assertIsDisplayed()
    }

    @Test
    fun `promoting goes through the DJ endpoint`() {
        coEvery { djApi.promotePlaylist(5L) } returns promoted()

        card()
        compose.onNodeWithText("Promuovi").performClick()
        compose.waitForIdle()

        coVerify { djApi.promotePlaylist(5L) }
    }

    @Test
    fun `a successful promotion says so instead of leaving the button there`() {
        coEvery { djApi.promotePlaylist(5L) } returns promoted()

        card()
        compose.onNodeWithText("Promuovi").performClick()

        awaitText("Adesso e", substring = true)
    }

    @Test
    fun `a refusal is explained with the server's own words`() {
        val raw = okhttp3.Response.Builder()
            .code(400).message("error").protocol(Protocol.HTTP_1_1)
            .request(Request.Builder().url("http://localhost/api/dj/playlists/5/promote").build())
            .build()
        coEvery { djApi.promotePlaylist(5L) } throws HttpException(Response.error<Any>(
            """{"error":"Solo una proposta del DJ puo' essere promossa"}"""
                .toResponseBody("application/json".toMediaType()),
            raw))

        card()
        compose.onNodeWithText("Promuovi").performClick()

        awaitText("Solo una proposta del DJ", substring = true)
    }
}
