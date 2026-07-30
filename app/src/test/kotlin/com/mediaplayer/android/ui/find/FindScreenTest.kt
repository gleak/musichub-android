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
}
