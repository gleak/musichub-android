package com.mediaplayer.android.ui.find

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import com.mediaplayer.android.data.FindRepository
import com.mediaplayer.android.data.dto.CandidateDto
import com.mediaplayer.android.data.dto.RequestDto
import com.mediaplayer.android.data.dto.RequestStatus
import com.mediaplayer.android.data.dto.RequestSummaryDto
import com.mediaplayer.android.ui.ScreenTest
import io.mockk.coEvery
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

/**
 * "Trova brani" drives a long-running server-side import: search, pick a
 * candidate, unlock, download. The screen polls the request to a terminal
 * status, so what matters is that every stage renders as something the
 * user can act on — and that a failure doesn't leave a spinner forever.
 */
class FindScreenTest : ScreenTest() {

    private fun request(
        status: RequestStatus,
        query: String = "nirvana",
        candidates: List<CandidateDto> = emptyList(),
        error: String? = null,
    ) = RequestDto(
        id = 1L,
        query = query,
        status = status,
        errorMessage = error,
        createdAt = "2026-01-01T00:00:00",
        updatedAt = "2026-01-01T00:00:00",
        candidates = candidates,
    )

    private fun candidate(id: Long, title: String) = CandidateDto(
        id = id,
        videoId = "vid$id",
        title = title,
        channelName = "Channel $id",
        durationSeconds = 200,
    )

    private fun screen(onBack: () -> Unit = {}) {
        val vm = FindViewModel(FindRepository(api))
        setScreen { FindScreen(onBack = onBack, viewModel = vm) }
    }

    private fun searchFor(text: String) {
        compose.onNode(hasSetTextAction()).performTextInput(text)
        compose.onNode(hasSetTextAction()).performImeAction()
    }

    @Test
    fun `the idle screen invites a search`() {
        coEvery { api.listRequests() } returns emptyList()

        screen()

        compose.onNodeWithText("Trova nuovi brani").assertIsDisplayed()
    }

    @Test
    fun `an in-flight request from a previous session is listed`() {
        coEvery { api.listRequests() } returns listOf(
            RequestSummaryDto(
                id = 1L,
                query = "nirvana",
                status = RequestStatus.DOWNLOADING,
                candidateCount = 3,
                createdAt = "2026-01-01T00:00:00",
                updatedAt = "2026-01-01T00:00:00",
            ),
        )

        screen()

        awaitText("nirvana", substring = true)
    }

    @Test
    fun `submitting a query starts a request`() {
        coEvery { api.listRequests() } returns emptyList()
        coEvery { api.createRequest(any()) } returns request(RequestStatus.SEARCHING)
        coEvery { api.getRequest(1L) } returns request(RequestStatus.SEARCHING)

        screen()
        searchFor("nirvana")

        awaitText("nirvana", substring = true)
    }

    @Test
    fun `candidates are offered once the server is awaiting a choice`() {
        coEvery { api.listRequests() } returns emptyList()
        val awaiting = request(
            RequestStatus.AWAITING_SELECTION,
            candidates = listOf(candidate(10L, "Smells Like Teen Spirit")),
        )
        coEvery { api.createRequest(any()) } returns awaiting
        coEvery { api.getRequest(1L) } returns awaiting

        screen()
        searchFor("nirvana")

        awaitText("Smells Like Teen Spirit")
    }

    @Test
    fun `picking a candidate sends the selection`() {
        coEvery { api.listRequests() } returns emptyList()
        val awaiting = request(
            RequestStatus.AWAITING_SELECTION,
            candidates = listOf(candidate(10L, "Smells Like Teen Spirit")),
        )
        coEvery { api.createRequest(any()) } returns awaiting
        coEvery { api.getRequest(1L) } returns awaiting
        coEvery { api.selectCandidate(any(), any()) } returns request(RequestStatus.DOWNLOADING)

        screen()
        searchFor("nirvana")
        awaitText("Smells Like Teen Spirit")
        compose.onNodeWithText("Smells Like Teen Spirit").performClick()

        awaitText("nirvana", substring = true)
    }

    /** A dead backend must surface, not spin. */
    @Test
    fun `a failed request surfaces with a retry`() {
        coEvery { api.listRequests() } returns emptyList()
        coEvery { api.createRequest(any()) } throws IOException("offline")

        screen()
        searchFor("nirvana")

        awaitText("Riprova")
    }

    @Test
    fun `back is wired`() {
        coEvery { api.listRequests() } returns emptyList()
        var backed = false

        screen(onBack = { backed = true })
        compose.onNodeWithContentDescription("Torna a Scopri").performClick()

        assertEquals(true, backed)
    }

    // ---------- the terminal states ----------

    /**
     * The end of a request is the only screen that tells the user whether
     * the track they asked for is now in their library, and the three
     * outcomes need to read differently — one is done, one is half done,
     * one needs another go.
     */
    /**
     * Drive a request all the way to [status]. It is created in flight and
     * reaches its terminal state through the poll, which is the only path a
     * real one takes — a request that comes back already failed from the
     * create call is a different branch (an error, not a terminal screen).
     */
    private fun terminal(status: RequestStatus, query: String = "nirvana") {
        coEvery { api.listRequests() } returns emptyList()
        coEvery { api.createRequest(any()) } returns request(status, query = query)
        coEvery { api.getRequest(1L) } returns request(status, query = query)
        screen()
        searchFor(query)
    }

    @Test
    fun `a finished import says the track is in the library`() {
        terminal(RequestStatus.IMPORTED)

        awaitText("Aggiunto alla libreria")
        compose.onNodeWithText("Apri brano").assertIsDisplayed()
        compose.onNodeWithText("Trova un altro").assertIsDisplayed()
    }

    /** Audio landed, video didn't — done enough to play, not silently. */
    @Test
    fun `a partial import says what is missing`() {
        terminal(RequestStatus.IMPORTED_PARTIAL)

        awaitText("Importato · parzialmente")
        compose.onNodeWithText("Solo audio recuperato · video saltato").assertIsDisplayed()
    }

    /**
     * A search the backend fails outright never becomes a terminal screen —
     * it is an error with the backend's own reason on it, so the user knows
     * whether to retype or to try later.
     */
    @Test
    fun `a search the backend refuses reports its reason`() {
        coEvery { api.listRequests() } returns emptyList()
        coEvery { api.createRequest(any()) } returns
            request(RequestStatus.FAILED, error = "nothing on YouTube for that")

        screen()
        searchFor("zzzz")

        awaitText("nothing on YouTube for that", substring = true)
    }

    @Test
    fun `the terminal screen quotes what was searched for`() {
        terminal(RequestStatus.IMPORTED, query = "nirvana breed")

        awaitText("Aggiunto alla libreria")
        compose.onNodeWithText("\"nirvana breed\"").assertIsDisplayed()
    }

    @Test
    fun `finding another clears the request and returns to the search`() {
        terminal(RequestStatus.IMPORTED)
        awaitText("Aggiunto alla libreria")
        compose.onNodeWithText("Trova un altro").performClick()

        awaitText("Trova nuovi brani")
    }

    @Test
    fun `leaving a terminal screen hands back to the caller`() {
        var backs = 0
        coEvery { api.listRequests() } returns emptyList()
        coEvery { api.createRequest(any()) } returns request(RequestStatus.IMPORTED)
        coEvery { api.getRequest(1L) } returns request(RequestStatus.IMPORTED)
        screen(onBack = { backs++ })
        searchFor("nirvana")
        awaitText("Aggiunto alla libreria")
        compose.onNodeWithText("Trova un altro").performClick()
        compose.waitForIdle()

        assertEquals(1, backs)
    }
}
