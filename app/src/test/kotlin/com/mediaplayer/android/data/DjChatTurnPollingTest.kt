package com.mediaplayer.android.data

import com.mediaplayer.android.data.dto.DjTurnDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Il contratto di polling del turno di chat. Gemello di [DjRunPollingTest],
 * e con la stessa asserzione centrale: uno stato sconosciuto conta come
 * terminale.
 */
class DjChatTurnPollingTest {

    private fun turn(status: String, phase: String? = null) =
        DjTurnDto(id = 1L, status = status, phase = phase)

    @Test
    fun `RUNNING non e terminale`() {
        assertFalse(DjChatTurnPolling.isTerminal("RUNNING"))
        assertFalse(DjChatTurnPolling.isTerminal(null))
        assertFalse(DjChatTurnPolling.isTerminal(""))
    }

    @Test
    fun `OK e FAILED sono terminali`() {
        assertTrue(DjChatTurnPolling.isTerminal("OK"))
        assertTrue(DjChatTurnPolling.isTerminal("FAILED"))
    }

    /**
     * Se il backend aggiungesse un quarto esito, un client che aspettasse
     * solo OK e FAILED resterebbe a fare polling per sempre su un turno gia'
     * concluso — cioe' proprio nel caso in cui l'utente ha ottenuto qualcosa.
     */
    @Test
    fun `uno stato sconosciuto conta come terminale`() {
        assertTrue(DjChatTurnPolling.isTerminal("QUALCOSA_DI_NUOVO"))
    }

    @Test
    fun `interroga finche il turno non e concluso e riporta ogni giro`() = runTest {
        val risposte = listOf(
            turn("RUNNING", "PROFILE"),
            turn("RUNNING", "CATALOG"),
            turn("OK"),
        )
        var chiamate = 0
        val fasi = mutableListOf<String?>()

        val esito = DjChatTurnPolling.awaitTerminal(
            turnId = 1L,
            fetch = { risposte[chiamate++] },
            onUpdate = { fasi.add(it.phase) },
            sleep = { },
        )

        assertEquals(3, chiamate)
        // onUpdate anche sul terminale: e' quello che porta la risposta a schermo.
        assertEquals(listOf("PROFILE", "CATALOG", null), fasi)
        assertEquals("OK", esito.status)
    }

    @Test
    fun `si arrende dopo maxAttempts portandosi dietro l ultimo stato`() = runTest {
        var chiamate = 0

        try {
            DjChatTurnPolling.awaitTerminal(
                turnId = 1L,
                maxAttempts = 3,
                fetch = { chiamate++; turn("RUNNING", "CATALOG") },
                sleep = { },
            )
            throw AssertionError("doveva arrendersi")
        } catch (e: DjChatTurnPolling.TimeoutException) {
            assertEquals(3, chiamate)
            assertEquals("CATALOG", e.lastTurn?.phase)
        }
    }
}
