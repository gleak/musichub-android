package com.mediaplayer.android.ui.dj

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.mediaplayer.android.data.dto.DjChatMessageDto
import com.mediaplayer.android.data.dto.DjChatReplyDto
import com.mediaplayer.android.data.dto.DjPreferencesDto
import com.mediaplayer.android.data.dto.DjRunDto
import com.mediaplayer.android.data.dto.DjStatusDto
import com.mediaplayer.android.data.dto.DjTasteProfile
import com.mediaplayer.android.data.dto.DjTasteProfileDto
import com.mediaplayer.android.ui.ScreenTest
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.CompletableDeferred
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
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

    /**
     * L'orologio di default e' fisso: la maggior parte dei test non deve
     * ragionare sul tempo, e un `System.currentTimeMillis()` vero sarebbe
     * comunque non deterministico. I test sul countdown passano il proprio
     * orologio finto e usano il [DjViewModel] restituito per farlo scorrere
     * senza un `delay()` reale.
     */
    private fun screen(clock: () -> Long = { 0L }): DjViewModel {
        val viewModel = DjViewModel(clock = clock)
        setScreen { DjScreen(viewModel = viewModel) }
        compose.waitForIdle()
        return viewModel
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
    fun `a second tap while sending sends nothing`() {
        // Vera moneta spesa in token: senza la guardia in DjViewModel.send,
        // due tap ravvicinati mandano due messaggi (e due chiamate al
        // modello) invece di uno.
        stubEverything()
        val pending = CompletableDeferred<DjChatReplyDto>()
        coEvery { djApi.sendMessage(any()) } coAnswers { pending.await() }

        val viewModel = screen()
        viewModel.send("uno")
        viewModel.send("due")
        compose.waitForIdle()

        coVerify(exactly = 1) { djApi.sendMessage(any()) }
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
    fun `the chat cooldown counts down and re-enables Invia`() {
        stubEverything()
        coEvery { djApi.sendMessage(any()) } throws httpError(
            429, """{"error":"Troppi messaggi, rallenta.","retryAfterSeconds":90}""")

        var now = 0L
        val viewModel = screen(clock = { now })
        compose.onNodeWithText("Scrivi al DJ…").performTextInput("uno")
        compose.onNodeWithContentDescription("Invia").performClick()
        compose.waitForIdle()

        awaitText("1 min 30 s", substring = true)
        compose.onNodeWithText("Scrivi al DJ…").performTextInput("due")
        compose.onNodeWithContentDescription("Invia").assertIsNotEnabled()

        // Il `Retry-After` letto e buttato via era il bug: il pulsante
        // restava disabilitato (o abilitato) per sempre, mai coerente con
        // l'orologio. Si sposta l'orologio finto e si chiama lo stesso giro
        // che il ticker reale farebbe da solo ogni secondo.
        now += 90_000L
        viewModel.tick()
        compose.waitForIdle()

        compose.onNodeWithContentDescription("Invia").assertIsEnabled()
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

        // Task 10 ha inserito generazione forzata, preferenze e cronologia
        // fra il composer e il profilo: la LazyColumn e' piu' lunga di uno
        // schermo, quindi il profilo va scrollato in vista prima di
        // verificarlo.
        compose.onNodeWithText("voci calde").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("archi invadenti").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("che musica ascolti in auto?").performScrollTo().assertIsDisplayed()
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
        // Sotto lo schermo: Task 10 ha allungato la LazyColumn prima del
        // profilo, quindi il blocco di cancellazione non e' piu' nella
        // prima schermata.
        compose.onNodeWithText("Cancella conversazione e profilo").performScrollTo().performClick()
        compose.waitForIdle()

        // Una sola pressione non deve poter cancellare quello che una persona
        // ha raccontato di se' nell'arco di settimane.
        coVerify(exactly = 0) { djApi.eraseChat() }

        compose.onNodeWithText("Sì, cancella tutto").performScrollTo().performClick()
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

    private fun run(status: String, written: Int = 0, error: String? = null) = DjRunDto(
        id = 7L,
        startedAt = "2026-08-22T10:00:00Z",
        finishedAt = if (status == "RUNNING") null else "2026-08-22T10:02:00Z",
        status = status,
        model = "gemini-2.5-flash",
        playlistsWritten = written,
        error = error,
    )

    @Test
    fun `default preferences are labelled as defaults`() {
        stubEverything()
        coEvery { djApi.preferences() } returns DjPreferencesDto(explicit = false)

        screen()

        // Senza questa riga l'app direbbe che l'utente ha scelto quattro
        // proposte ogni sette giorni, e non e' vero: non ha scelto niente.
        awaitText("Non le hai ancora impostate", substring = true)
    }

    @Test
    fun `a globally switched-off cycle is explained even with the user's own toggle on`() {
        // status.cycleEnabled e' `dj.enabled`, l'interruttore del cron — non
        // quello per-utente. Spento com'e' in produzione, un utente col
        // proprio interruttore acceso deve saperlo: altrimenti aspetta
        // proposte che il ciclo non generera' mai da solo.
        stubEverything(status = DjStatusDto(
            agentAvailable = true, apiKeyConfigured = true, cycleEnabled = false,
            chatEnabled = true, runInProgress = false, cooldownSeconds = 0L,
        ))
        coEvery { djApi.preferences() } returns DjPreferencesDto(cycleEnabled = true, explicit = true)

        screen()

        awaitText("Il ciclo automatico e", substring = true)
    }

    @Test
    fun `the user's own toggle switched off needs no global-cycle notice`() {
        stubEverything(status = DjStatusDto(
            agentAvailable = true, apiKeyConfigured = true, cycleEnabled = false,
            chatEnabled = true, runInProgress = false, cooldownSeconds = 0L,
        ))
        coEvery { djApi.preferences() } returns DjPreferencesDto(cycleEnabled = false, explicit = true)

        screen()

        compose.onAllNodes(hasText("Il ciclo automatico e", substring = true))
            .assertCountEquals(0)
    }

    @Test
    fun `turning the cycle off sends only that field`() {
        stubEverything()
        coEvery { djApi.preferences() } returns DjPreferencesDto(cycleEnabled = true, explicit = true)
        coEvery { djApi.updatePreferences(any()) } returns
            DjPreferencesDto(cycleEnabled = false, explicit = true)

        screen()
        awaitText("Il DJ propone da solo")
        // Il click va sullo Switch, non sull'etichetta: SettingsToggleRow non
        // rende cliccabile l'intera riga (solo lo Switch lo e'), quindi il
        // testo da solo non intercetta il tocco.
        compose.onNode(isToggleable()).performScrollTo().performClick()
        compose.waitForIdle()

        coVerify {
            djApi.updatePreferences(match {
                it.cycleEnabled == false && it.slots == null && it.cadenceDays == null
            })
        }
    }

    @Test
    fun `stepping the number of proposals saves it`() {
        stubEverything()
        coEvery { djApi.preferences() } returns DjPreferencesDto(slots = 4, explicit = true)
        coEvery { djApi.updatePreferences(any()) } returns
            DjPreferencesDto(slots = 5, explicit = true)

        screen()
        compose.onNodeWithContentDescription("Aumenta Proposte").performClick()
        compose.waitForIdle()

        coVerify { djApi.updatePreferences(match { it.slots == 5 }) }
    }

    @Test
    fun `the bounds come from the server, not from the app`() {
        // maxSlots = 8 e' anche il default del DTO: uno stepper con `max`
        // scritto a mano nella schermata (`= 8`) passerebbe questo test
        // esattamente allo stesso modo. Un tetto che il server non manda
        // mai in produzione (6, non 8) e' l'unico modo che il test dipenda
        // davvero dal numero che arriva dalla rete.
        stubEverything()
        coEvery { djApi.preferences() } returns
            DjPreferencesDto(slots = 6, maxSlots = 6, explicit = true)

        screen()

        compose.onNodeWithContentDescription("Aumenta Proposte").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Diminuisci Proposte").assertIsEnabled()
    }

    @Test
    fun `forcing a run polls until it is terminal and reports a PARTIAL honestly`() {
        stubEverything()
        coEvery { djApi.startRun() } returns run("RUNNING")
        coEvery { djApi.run(7L) } returns run("PARTIAL", written = 2, error = "queueWanted")

        screen()
        compose.onNodeWithText("Genera adesso").performClick()

        // Se la schermata aspettasse "OK oppure FAILED" resterebbe qui per
        // sempre, su un giro che ha scritto due playlist davvero.
        awaitText("2 playlist", substring = true)
        // "2 playlist" da solo lo direbbe anche un giro OK: l'etichetta
        // "Riuscito in parte" e' cio' che distingue davvero un PARTIAL.
        awaitText("Riuscito in parte", substring = true)
    }

    @Test
    fun `PARTIAL gets its own label, never the one OK uses`() {
        assertEquals("Riuscito in parte", runStatusLabel("PARTIAL"))
    }

    @Test
    fun `the cooldown refusal shows how long is left`() {
        stubEverything()
        coEvery { djApi.startRun() } throws httpError(
            429,
            """{"error":"Troppo presto per un altro giro forzato.","retryAfterSeconds":240}""")

        screen()
        compose.onNodeWithText("Genera adesso").performClick()

        awaitText("4 min", substring = true)
    }

    @Test
    fun `the cooldown counts down and re-enables the button`() {
        // Il bug: `refusedWaitSeconds` veniva scritto una volta al rifiuto e
        // mai piu' toccato, quindi "4 min" restava scritto per sempre e il
        // pulsante restava disabilitato ben oltre la fine vera dell'attesa.
        stubEverything()
        coEvery { djApi.startRun() } throws httpError(
            429, """{"error":"Troppo presto.","retryAfterSeconds":90}""")

        var now = 0L
        val viewModel = screen(clock = { now })
        compose.onNodeWithText("Genera adesso").performClick()
        compose.waitForIdle()

        awaitText("1 min 30 s", substring = true)
        compose.onNodeWithText("Genera adesso").assertIsNotEnabled()

        // Stesso principio del test sul tetto della chat: si sposta
        // l'orologio finto invece di aspettare novanta secondi veri, e si
        // chiama lo stesso giro che il ticker reale farebbe da solo.
        now += 90_000L
        viewModel.tick()
        compose.waitForIdle()

        compose.onNodeWithText("Genera adesso").assertIsEnabled()
    }

    @Test
    fun `a run already in progress is explained, not swallowed`() {
        stubEverything()
        coEvery { djApi.startRun() } throws httpError(
            409, """{"error":"Un giro e' gia' in corso per questo utente."}""")

        screen()
        compose.onNodeWithText("Genera adesso").performClick()

        awaitText("gia' in corso", substring = true)
    }

    @Test
    fun `a 409 disables the button without a manual refresh`() {
        // Il bug mirror: un 409 non toccava affatto lo stato locale, quindi
        // `canForce` restava vero e il pulsante invitava un secondo tap che
        // poteva solo ottenere un altro 409 identico.
        coEvery { djApi.status() } returnsMany listOf(
            DjStatusDto(agentAvailable = true, apiKeyConfigured = true, cycleEnabled = true,
                chatEnabled = true, runInProgress = false, cooldownSeconds = 0L),
            DjStatusDto(agentAvailable = true, apiKeyConfigured = true, cycleEnabled = true,
                chatEnabled = true, runInProgress = true, cooldownSeconds = 0L),
        )
        coEvery { djApi.chat(any()) } returns emptyList()
        coEvery { djApi.profile() } returns DjTasteProfileDto()
        coEvery { djApi.preferences() } returns DjPreferencesDto()
        coEvery { djApi.recentRuns() } returns emptyList()
        coEvery { djApi.startRun() } throws httpError(
            409, """{"error":"Un giro e' gia' in corso per questo utente."}""")

        screen()
        compose.onNodeWithText("Genera adesso").assertIsEnabled()
        compose.onNodeWithText("Genera adesso").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Genera adesso").assertIsNotEnabled()
    }

    @Test
    fun `a second tap while forcing sends nothing`() {
        // Anche questa e' moneta vera: senza la guardia in
        // DjViewModel.forceRun, due tap ravvicinati aprono due giri.
        stubEverything()
        val pending = CompletableDeferred<DjRunDto>()
        coEvery { djApi.startRun() } coAnswers { pending.await() }

        val viewModel = screen()
        viewModel.forceRun()
        viewModel.forceRun()
        compose.waitForIdle()

        coVerify(exactly = 1) { djApi.startRun() }
    }

    @Test
    fun `the button is already disabled when the server reports an open run`() {
        stubEverything(status = DjStatusDto(
            agentAvailable = true, apiKeyConfigured = true, cycleEnabled = true,
            chatEnabled = true, runInProgress = true, cooldownSeconds = 0L,
        ))

        screen()

        compose.onNodeWithText("Genera adesso").assertIsNotEnabled()
    }

    @Test
    fun `the recent runs are listed with their outcome`() {
        stubEverything()
        coEvery { djApi.recentRuns() } returns listOf(
            run("OK", written = 2),
            run("FAILED", error = "budget esaurito"),
        )

        screen()

        awaitText("2 playlist scritte")
        compose.onNodeWithText("Non riuscito").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `the run history formats the date instead of printing the raw instant`() {
        stubEverything()
        coEvery { djApi.recentRuns() } returns listOf(run("OK", written = 1))

        screen()

        awaitText("1 playlist scritta")
        // "2026-08-22T10:00:00Z" con una T e una Z non e' per un essere
        // umano: deve sparire, non solo comparire un formato migliore
        // altrove.
        compose.onAllNodes(hasText("2026-08-22T10:00:00Z", substring = true)).assertCountEquals(0)
    }

    @Test
    fun `an empty history says so instead of showing an empty box`() {
        stubEverything()
        coEvery { djApi.recentRuns() } returns emptyList()

        screen()

        awaitText("Il DJ non ha ancora fatto nessun giro", substring = true)
    }
}
