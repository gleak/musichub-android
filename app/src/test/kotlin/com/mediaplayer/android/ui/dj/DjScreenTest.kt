package com.mediaplayer.android.ui.dj

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.mediaplayer.android.data.dto.DjChatMessageDto
import com.mediaplayer.android.data.dto.DjChatReplyDto
import com.mediaplayer.android.data.dto.DjPreferencesDto
import com.mediaplayer.android.data.dto.DjStatusDto
import com.mediaplayer.android.data.dto.DjTasteProfile
import com.mediaplayer.android.data.dto.DjTasteProfileDto
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
 * La sezione DJ e' il posto dove l'utente modella la propria esperienza del
 * DJ, non una superficie d'ascolto: quello che conta qui e' che la
 * conversazione sia leggibile, che il profilo sia visibile in chiaro, e che
 * un rifiuto del server arrivi all'utente con la sua spiegazione invece che
 * come un pulsante che ha smesso di funzionare.
 */
class DjScreenTest : ScreenTest() {

    private fun stubEverything(
        status: DjStatusDto = DjStatusDto(
            agentAvailable = true, apiKeyConfigured = true, cycleEnabled = true,
            chatEnabled = true, runInProgress = false, cooldownSeconds = 0L,
        ),
        messages: List<DjChatMessageDto> = emptyList(),
        profile: DjTasteProfileDto = DjTasteProfileDto(),
    ) {
        coEvery { djApi.status() } returns status
        coEvery { djApi.chat(any()) } returns messages
        coEvery { djApi.profile() } returns profile
        coEvery { djApi.preferences() } returns DjPreferencesDto()
        coEvery { djApi.recentRuns() } returns emptyList()
    }

    private fun screen() {
        setScreen { DjScreen(viewModel = DjViewModel()) }
        compose.waitForIdle()
    }

    private fun httpError(code: Int, body: String): HttpException {
        val raw = okhttp3.Response.Builder()
            .code(code).message("error").protocol(Protocol.HTTP_1_1)
            .request(Request.Builder().url("http://localhost/api/dj/chat").build())
            .build()
        return HttpException(
            Response.error<Any>(body.toResponseBody("application/json".toMediaType()), raw))
    }

    @Test
    fun `the conversation is shown in order`() {
        stubEverything(messages = listOf(
            DjChatMessageDto("USER", "stasera voglio roba lenta", false, "2026-08-22T09:00:00Z"),
            DjChatMessageDto("DJ", "Ti metto qualcosa di caldo.", false, "2026-08-22T09:00:05Z"),
        ))

        screen()

        compose.onNodeWithText("stasera voglio roba lenta").assertIsDisplayed()
        compose.onNodeWithText("Ti metto qualcosa di caldo.").assertIsDisplayed()
    }

    @Test
    fun `a refused turn is marked as off topic`() {
        // Il flag `refused` esiste per rendere osservabile il confine
        // tematico del DJ. Se non si vede, non serve a niente.
        stubEverything(messages = listOf(
            DjChatMessageDto("DJ", "Di questo non parlo.", true, "2026-08-22T09:00:05Z"),
        ))

        screen()

        compose.onNodeWithText("Fuori tema").assertIsDisplayed()
    }

    @Test
    fun `sending posts what was typed`() {
        stubEverything()
        coEvery { djApi.sendMessage(any()) } returns DjChatReplyDto("Ok.", offTopic = false)

        screen()
        compose.onNodeWithText("Scrivi al DJ…").performTextInput("per correre voglio roba che spinge")
        compose.onNodeWithContentDescription("Invia").performClick()
        compose.waitForIdle()

        coVerify {
            djApi.sendMessage(match { it.message == "per correre voglio roba che spinge" })
        }
    }

    @Test
    fun `the daily cap is explained in the server's own words`() {
        stubEverything()
        coEvery { djApi.sendMessage(any()) } throws httpError(
            429, """{"error":"Hai raggiunto il limite di messaggi di oggi. Riprova domani."}""")

        screen()
        compose.onNodeWithText("Scrivi al DJ…").performTextInput("ancora uno")
        compose.onNodeWithContentDescription("Invia").performClick()

        // "HTTP 429" non e' una risposta a chi ha appena scritto un messaggio.
        awaitText("Hai raggiunto il limite di messaggi di oggi. Riprova domani.")
    }

    @Test
    fun `a switched-off chat says so instead of failing on send`() {
        stubEverything(status = DjStatusDto(
            agentAvailable = true, apiKeyConfigured = true, cycleEnabled = true,
            chatEnabled = false, runInProgress = false, cooldownSeconds = 0L,
        ))

        screen()

        awaitText("La chat col DJ", substring = true)
        compose.onNodeWithContentDescription("Invia").assertIsNotEnabled()
    }

    @Test
    fun `the profile is shown field by field`() {
        stubEverything(profile = DjTasteProfileDto(
            hasProfile = true, version = 3, sourceMessageCount = 12,
            updatedAt = "2026-08-22T08:00:00Z",
            profile = DjTasteProfile(
                moods = listOf("malinconico la sera"),
                contexts = listOf("corsa: roba che spinge"),
                loves = listOf("voci calde"),
                avoids = listOf("archi invadenti"),
                openQuestions = listOf("che musica ascolti in auto?"),
                notes = "parla di momenti, non di generi",
            ),
        ))

        screen()

        compose.onNodeWithText("voci calde").assertIsDisplayed()
        compose.onNodeWithText("archi invadenti").assertIsDisplayed()
        compose.onNodeWithText("che musica ascolti in auto?").assertIsDisplayed()
    }

    @Test
    fun `an empty profile says so rather than showing empty lists`() {
        stubEverything(profile = DjTasteProfileDto(hasProfile = false))

        screen()

        awaitText("Il DJ non sa ancora niente di te", substring = true)
    }

    @Test
    fun `erasing asks first and only then deletes`() {
        stubEverything(profile = DjTasteProfileDto(hasProfile = true, version = 1))
        coEvery { djApi.eraseChat() } returns Response.success(null)

        screen()
        compose.onNodeWithText("Cancella conversazione e profilo").performClick()
        compose.waitForIdle()

        // Una sola pressione non deve poter cancellare quello che una persona
        // ha raccontato di se' nell'arco di settimane.
        coVerify(exactly = 0) { djApi.eraseChat() }

        compose.onNodeWithText("Sì, cancella tutto").performClick()
        compose.waitForIdle()

        coVerify(exactly = 1) { djApi.eraseChat() }
    }

    @Test
    fun `a server without an agent says so instead of offering a dead screen`() {
        stubEverything(status = DjStatusDto(
            agentAvailable = false, apiKeyConfigured = false, cycleEnabled = false,
            chatEnabled = false, runInProgress = false, cooldownSeconds = 0L,
        ))

        screen()

        awaitText("Il DJ non e", substring = true)
    }
}
