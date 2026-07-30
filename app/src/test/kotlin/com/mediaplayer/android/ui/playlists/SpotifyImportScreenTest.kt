package com.mediaplayer.android.ui.playlists

import android.app.Application
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.mediaplayer.android.data.dto.SpotifyImportJobIdDto
import com.mediaplayer.android.data.dto.SpotifyImportJobStatusDto
import com.mediaplayer.android.data.dto.SpotifyImportResultDto
import com.mediaplayer.android.ui.ScreenTest
import io.mockk.coEvery
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.robolectric.Shadows.shadowOf
import java.io.ByteArrayInputStream

/**
 * The Spotify import walks a five-step wizard over a file the user picked
 * and a job the backend runs asynchronously. The file is the interesting
 * half: the parser has to cope with what real exporters emit — localised
 * headers, a UTF-8 BOM, quoted fields containing commas — and a file it
 * can't read must land on the error step rather than an empty confirm.
 *
 * The picked file is served through Robolectric's content resolver, so the
 * real `openInputStream` path runs.
 */
class SpotifyImportScreenTest : ScreenTest() {

    private lateinit var store: ViewModelStore

    @After
    fun clearStore() {
        if (::store.isInitialized) store.clear()
    }

    /** Canonical Exportify export: English headers, quoted multi-artist cell. */
    private val exportifyCsv = """
        "Track URI","Track Name","Artist Name(s)","Album Name"
        "spotify:track:1","Breed","Nirvana","Nevermind"
        "spotify:track:2","Under Pressure","Queen, David Bowie","Hot Space"
        "spotify:track:3","Smells Like Teen Spirit","Nirvana","Nevermind"
    """.trimIndent()

    /** Spotify's Italian account export: localised headers, no quoting. */
    private val italianCsv = """
        Titolo,Artista,Album
        Breed,Nirvana,Nevermind
        Lithium,Nirvana,Nevermind
    """.trimIndent()

    private fun pickFile(csv: String, withBom: Boolean = false): Uri {
        val uri = Uri.parse("content://test/playlist.csv")
        val bytes = (if (withBom) "﻿" else "") + csv
        val resolver = ApplicationProvider.getApplicationContext<Application>().contentResolver
        shadowOf(resolver).registerInputStream(uri, ByteArrayInputStream(bytes.toByteArray()))
        return uri
    }

    private fun screen(
        onBack: () -> Unit = {},
        onPlaylistCreated: (Long) -> Unit = {},
    ): SpotifyImportViewModel {
        store = ViewModelStore()
        val vm = ViewModelProvider(
            store,
            ViewModelProvider.AndroidViewModelFactory.getInstance(
                ApplicationProvider.getApplicationContext(),
            ),
        )[SpotifyImportViewModel::class.java]
        setScreen {
            SpotifyImportScreen(
                viewModel = vm,
                onBack = onBack,
                onPlaylistCreated = onPlaylistCreated,
            )
        }
        compose.waitForIdle()
        return vm
    }

    @Test
    fun `the wizard opens on the export instructions`() {
        screen()

        compose.onNodeWithText("Esporta da Exportify").assertIsDisplayed()
    }

    @Test
    fun `a picked export lands on the confirm step`() {
        val vm = screen()

        vm.importFromUri(pickFile(exportifyCsv))

        awaitText("Come la chiamiamo?")
    }

    @Test
    fun `the track count comes from the parsed file`() {
        val vm = screen()

        vm.importFromUri(pickFile(exportifyCsv))

        awaitText("Importa 3 brani")
    }

    /** Localised headers are what Spotify's own Italian export ships. */
    @Test
    fun `an italian export is parsed too`() {
        val vm = screen()

        vm.importFromUri(pickFile(italianCsv))

        awaitText("Importa 2 brani")
    }

    /**
     * Some browsers prefix the file with a UTF-8 BOM. Left in place it
     * survives as a zero-width prefix on the first header cell and column
     * detection fails on row one — the whole file reads as unparseable.
     */
    @Test
    fun `a byte order mark does not break header detection`() {
        val vm = screen()

        vm.importFromUri(pickFile(exportifyCsv, withBom = true))

        awaitText("Importa 3 brani")
    }

    @Test
    fun `a file with no recognisable headers lands on the error step`() {
        val vm = screen()

        vm.importFromUri(pickFile("alpha;beta;gamma\n1;2;3"))

        awaitText("Non riesco a leggere il file")
    }

    @Test
    fun `an unreadable uri lands on the error step`() {
        val vm = screen()

        // Nothing registered for this URI: openInputStream returns null.
        vm.importFromUri(Uri.parse("content://test/missing.csv"))

        awaitText("Non riesco a leggere il file")
    }

    @Test
    fun `the name can be edited before importing`() {
        val vm = screen()
        vm.importFromUri(pickFile(exportifyCsv))
        awaitText("Come la chiamiamo?")

        compose.onNode(hasSetTextAction()).performTextInput("Road Trip")

        // The name shows in the field and again in the summary line.
        compose.onAllNodesWithText("Road Trip", substring = true).onFirst().assertIsDisplayed()
    }

    @Test
    fun `a finished job reports what it imported`() {
        coEvery { api.importSpotifyPlaylistAsync(any(), any()) } returns
            SpotifyImportJobIdDto(jobId = "job-1")
        coEvery { api.getSpotifyImportJobStatus("job-1") } returns SpotifyImportJobStatusDto(
            jobId = "job-1",
            phase = "DONE",
            total = 3,
            current = 3,
            matched = 3,
            approx = 0,
            queued = 0,
            failed = 0,
            result = SpotifyImportResultDto(
                playlistId = 7L,
                playlistName = "Nevermind",
                totalTracks = 3,
                matched = 3,
                approx = 0,
                queued = 0,
                failed = 0,
            ),
        )
        val vm = screen()
        vm.importFromUri(pickFile(exportifyCsv))
        awaitText("Come la chiamiamo?")

        compose.onNodeWithText("Importa 3 brani").performClick()

        awaitText("Importazione completata")
    }

    @Test
    fun `a failed job says so instead of hanging on the progress step`() {
        coEvery { api.importSpotifyPlaylistAsync(any(), any()) } throws
            java.io.IOException("offline")
        val vm = screen()
        vm.importFromUri(pickFile(exportifyCsv))
        awaitText("Come la chiamiamo?")

        compose.onNodeWithText("Importa 3 brani").performClick()

        awaitText("Riprova")
    }

    @Test
    fun `back is wired`() {
        var backed = false

        screen(onBack = { backed = true })
        compose.onNodeWithContentDescription("Indietro").performClick()

        assertEquals(true, backed)
    }
}