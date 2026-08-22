package com.mediaplayer.android.data

import com.mediaplayer.android.data.dto.DjRunDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Il contratto di polling del DJ, per intero e in un posto solo.
 *
 * `POST /api/dj/run` risponde 202 con la riga appena aperta perche' un giro
 * puo' durare fino a 300 secondi mentre OkHttp taglia a 30: l'esito si
 * scopre interrogando `GET /api/dj/runs/{id}`. Gli stati terminali sono TRE
 * — OK, FAILED, PARTIAL — e un client che ne aspettasse due resterebbe a
 * fare polling per sempre su un giro che ha scritto playlist davvero.
 *
 * `runTest` usa tempo virtuale, quindi i `delay` del backoff non fanno
 * durare il test quanto un giro vero.
 */
class DjRunPollingTest {

    private fun run(status: String, written: Int = 0) = DjRunDto(
        id = 7L,
        startedAt = "2026-08-22T10:00:00Z",
        finishedAt = if (status == "RUNNING") null else "2026-08-22T10:02:00Z",
        status = status,
        model = "gemini-2.5-flash",
        playlistsWritten = written,
    )

    @Test
    fun `every status other than RUNNING is terminal`() {
        assertTrue(DjRunPolling.isTerminal("OK"))
        assertTrue(DjRunPolling.isTerminal("FAILED"))
        assertTrue(DjRunPolling.isTerminal("PARTIAL"))
    }

    @Test
    fun `RUNNING is the only reason to keep asking`() {
        assertFalse(DjRunPolling.isTerminal("RUNNING"))
        assertFalse(DjRunPolling.isTerminal("running"))
    }

    @Test
    fun `a missing status is not terminal and an unknown one is`() {
        assertFalse(DjRunPolling.isTerminal(null))
        assertFalse(DjRunPolling.isTerminal(""))
        // Se il backend introducesse un quarto esito, restare appesi sarebbe
        // il fallimento peggiore: ci si ferma e si mostra quello che c'e'.
        assertTrue(DjRunPolling.isTerminal("CANCELLED"))
    }

    @Test
    fun `polling stops at the first terminal answer`() = runTest {
        val answers = mutableListOf(run("RUNNING"), run("RUNNING"), run("OK", written = 2))
        var calls = 0

        val result = DjRunPolling.awaitTerminal(7L, fetch = {
            calls++
            answers.removeAt(0)
        })

        assertEquals("OK", result.status)
        assertEquals(2, result.playlistsWritten)
        assertEquals(3, calls)
    }

    @Test
    fun `polling stops on PARTIAL, which a two-status client would hang on`() = runTest {
        val answers = mutableListOf(run("RUNNING"), run("PARTIAL", written = 3))

        val result = DjRunPolling.awaitTerminal(7L, fetch = { answers.removeAt(0) })

        assertEquals("PARTIAL", result.status)
        assertEquals(3, result.playlistsWritten)
    }

    @Test
    fun `every answer is reported so the screen can show progress`() = runTest {
        val seen = mutableListOf<String>()
        val answers = mutableListOf(run("RUNNING"), run("FAILED"))

        DjRunPolling.awaitTerminal(7L,
            fetch = { answers.removeAt(0) },
            onUpdate = { seen += it.status })

        assertEquals(listOf("RUNNING", "FAILED"), seen)
    }

    @Test(expected = DjRunPolling.TimeoutException::class)
    fun `polling gives up instead of asking for ever`() = runTest {
        DjRunPolling.awaitTerminal(7L, maxAttempts = 4, fetch = { run("RUNNING") })
    }

    @Test
    fun `giving up hands back the last thing it saw`() = runTest {
        val error = runCatching {
            DjRunPolling.awaitTerminal(7L, maxAttempts = 2, fetch = { run("RUNNING") })
        }.exceptionOrNull() as DjRunPolling.TimeoutException

        assertEquals("RUNNING", error.lastRun?.status)
    }
}
