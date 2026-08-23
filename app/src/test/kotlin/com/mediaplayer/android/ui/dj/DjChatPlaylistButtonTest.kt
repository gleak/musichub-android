package com.mediaplayer.android.ui.dj

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mediaplayer.android.data.dto.DjChatMessageDto
import com.mediaplayer.android.data.dto.DjPreferencesDto
import com.mediaplayer.android.data.dto.DjRunDto
import com.mediaplayer.android.data.dto.DjStatusDto
import com.mediaplayer.android.data.dto.DjTasteProfileDto
import com.mediaplayer.android.ui.ScreenTest
import io.mockk.coEvery
import io.mockk.coVerify
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Il ponte fra il parlare e il fare, visto dalla schermata.
 *
 * <p>In chat il DJ non ha il catalogo in mano — la sua superficie di strumenti
 * e' il solo profilo dell'ascoltatore — quindi puo' concordare CHE playlist
 * fare ma non farla. Il pulsante "Crea questa playlist" apre il giro che la
 * compone davvero, partendo dal briefing che il DJ ha scritto su quel turno.
 *
 * <p>Le due proprieta' che questi test tengono ferme sono entrambe cose che
 * fallirebbero in silenzio: un pulsante che compare su ogni messaggio (e
 * spende un giro da minuti su una chiacchierata qualunque), e un pulsante che
 * manda l'id sbagliato quando la conversazione contiene piu' di una proposta —
 * comporrebbe cio' di cui si e' parlato prima, senza che si veda quale.
 */
class DjChatPlaylistButtonTest : ScreenTest() {

    private fun stub(messages: List<DjChatMessageDto>, runInProgress: Boolean = false) {
        coEvery { djApi.status() } returns DjStatusDto(
            agentAvailable = true, apiKeyConfigured = true, cycleEnabled = true,
            chatEnabled = true, runInProgress = runInProgress, cooldownSeconds = 0L,
        )
        coEvery { djApi.chat(any()) } returns messages
        coEvery { djApi.profile() } returns DjTasteProfileDto()
        coEvery { djApi.preferences() } returns DjPreferencesDto()
        coEvery { djApi.recentRuns() } returns emptyList()
    }

    private fun screen(onOpenPlaylist: (Long) -> Unit = {}) {
        setScreen { DjScreen(onOpenPlaylist = onOpenPlaylist, viewModel = DjViewModel(clock = { 0L })) }
        compose.waitForIdle()
    }

    private fun djTurn(
        content: String,
        at: String,
        id: Long? = null,
        name: String? = null,
        brief: String? = null,
    ) = DjChatMessageDto(
        role = "DJ", content = content, refused = false, createdAt = at,
        id = id, playlistName = name, playlistBrief = brief,
    )

    private fun finishedRun(playlistId: Long?, error: String? = null) = DjRunDto(
        id = 9L,
        startedAt = "2026-08-23T10:00:00Z",
        finishedAt = "2026-08-23T10:03:00Z",
        status = if (playlistId != null) "OK" else "FAILED",
        playlistsWritten = if (playlistId != null) 1 else 0,
        error = error,
        createdPlaylistId = playlistId,
    )

    @Test
    fun `an ordinary turn offers no button`() {
        stub(listOf(djTurn("Che genere ti va?", "2026-08-23T09:00:00Z", id = 1L)))

        screen()

        compose.onAllNodes(hasText("Crea questa playlist")).assertCountEquals(0)
    }

    @Test
    fun `a turn that agreed on a playlist offers it, with what was agreed`() {
        stub(listOf(djTurn(
            "Te la preparo.", "2026-08-23T09:00:00Z",
            id = 42L, name = "Corsa serale", brief = "ritmi fra 150 e 170 bpm, niente ballate",
        )))

        screen()

        compose.onNodeWithText("Crea questa playlist").assertIsDisplayed()
        // Il nome e il briefing sono cio' su cui la persona sta decidendo: un
        // pulsante nudo la costringerebbe a rileggere la conversazione per
        // capire che cosa sta per far comporre.
        compose.onNodeWithText("Corsa serale").assertIsDisplayed()
        compose.onNodeWithText("ritmi fra 150 e 170 bpm, niente ballate").assertIsDisplayed()
    }

    /**
     * Il briefing non parte da qui: viaggia solo l'id del turno. E' cio' che
     * impedisce che chiunque faccia comporre al DJ qualunque cosa chiamando
     * l'endpoint per conto suo.
     */
    @Test
    fun `pressing composes from that message, not the latest`() {
        stub(listOf(
            djTurn("Prima idea.", "2026-08-23T09:00:00Z",
                id = 10L, name = "Vecchia idea", brief = "roba di ieri"),
            djTurn("Meglio questa.", "2026-08-23T09:05:00Z",
                id = 11L, name = "Idea nuova", brief = "roba di oggi"),
        ))
        coEvery { djApi.composePlaylistFromChat(any()) } returns finishedRun(playlistId = 55L)
        coEvery { djApi.run(any()) } returns finishedRun(playlistId = 55L)

        screen()
        // Il primo dei due pulsanti: quello del turno piu' vecchio.
        compose.onAllNodes(hasText("Crea questa playlist"))[0].performClick()
        compose.waitForIdle()

        coVerify { djApi.composePlaylistFromChat(10L) }
    }

    @Test
    fun `when it is ready the screen offers to open it`() {
        stub(listOf(djTurn(
            "Te la preparo.", "2026-08-23T09:00:00Z",
            id = 42L, name = "Corsa serale", brief = "ritmi alti",
        )))
        coEvery { djApi.composePlaylistFromChat(42L) } returns finishedRun(playlistId = 55L)
        coEvery { djApi.run(any()) } returns finishedRun(playlistId = 55L)

        var opened: Long? = null
        screen(onOpenPlaylist = { opened = it })

        compose.onNodeWithText("Crea questa playlist").performClick()
        awaitText("Aprila")
        compose.onNodeWithText("Aprila").performClick()
        compose.waitForIdle()

        // Dire "fatto" e lasciare la persona a cercarla in libreria rende
        // inutile la funzione.
        assertEquals(55L, opened)
    }

    /**
     * Un giro terminale non e' per forza un giro riuscito: la scaletta puo'
     * essere stata scartata dalla validazione, e allora non c'e' nessuna
     * playlist da aprire. Dire "pronta" li' sarebbe una bugia verificabile in
     * due secondi dall'utente.
     */
    @Test
    fun `a run that produced nothing says so instead of offering an empty link`() {
        stub(listOf(djTurn(
            "Te la preparo.", "2026-08-23T09:00:00Z",
            id = 42L, name = "Impossibile", brief = "roba che non abbiamo",
        )))
        val failed = finishedRun(playlistId = null, error = "nessuna traccia utilizzabile")
        coEvery { djApi.composePlaylistFromChat(42L) } returns failed
        coEvery { djApi.run(any()) } returns failed

        screen()
        compose.onNodeWithText("Crea questa playlist").performClick()

        awaitText("nessuna traccia utilizzabile", substring = true)
        compose.onAllNodes(hasText("Aprila")).assertCountEquals(0)
    }

    /**
     * Comporre e "genera adesso" sono lo stesso giro sul server: un solo
     * thread, una sola guardia. Lasciare il pulsante attivo mentre un giro e'
     * gia' in corso prometterebbe un 409.
     */
    @Test
    fun `the button is disabled while another run is already going`() {
        stub(
            listOf(djTurn(
                "Te la preparo.", "2026-08-23T09:00:00Z",
                id = 42L, name = "Corsa serale", brief = "ritmi alti",
            )),
            runInProgress = true,
        )

        screen()

        compose.onNodeWithText("Crea questa playlist").assertIsNotEnabled()
    }
}
