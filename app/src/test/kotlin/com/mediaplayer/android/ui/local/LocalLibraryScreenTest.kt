package com.mediaplayer.android.ui.local

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mediaplayer.android.data.local.LocalTrack
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "Sul tuo dispositivo" is the only screen that reads the phone rather than
 * the backend, and the only one that can legitimately render nothing:
 * permission not granted yet, or granted with no music on the device. Those
 * two look identical to a naive implementation and mean opposite things to
 * the user — one needs a prompt, the other needs no action at all.
 *
 * The four tabs all read the same scan through different groupings, so a
 * grouping bug shows up as tracks that exist under Brani and vanish under
 * Cartelle.
 */
class LocalLibraryScreenTest : LocalScreenTest() {

    private fun screen(
        onPlayTrack: (LocalTrack, List<LocalTrack>) -> Unit = { _, _ -> },
        onShufflePlay: (List<LocalTrack>) -> Unit = {},
        onPlayNext: (LocalTrack) -> Unit = {},
        onAddToQueue: (LocalTrack) -> Unit = {},
        onOpenFolder: (String) -> Unit = {},
        onOpenAlbum: (String) -> Unit = {},
        onOpenLiked: () -> Unit = {},
        onOpenPlaylist: (String) -> Unit = {},
        onBack: () -> Unit = {},
    ) {
        setScreen {
            LocalLibraryScreen(
                onBack = onBack,
                onPlayTrack = onPlayTrack,
                onShufflePlay = onShufflePlay,
                onPlayNext = onPlayNext,
                onAddToQueue = onAddToQueue,
                onOpenFolder = onOpenFolder,
                onOpenAlbum = onOpenAlbum,
                onOpenLiked = onOpenLiked,
                onOpenPlaylist = onOpenPlaylist,
            )
        }
    }

    @Test
    fun `the device library is listed once the scan lands`() {
        publishTracks(
            trackRow(id = 1L, title = "Breed"),
            trackRow(id = 2L, title = "Lithium"),
        )

        screen()

        awaitText("Breed")
        compose.onNodeWithText("Lithium").assertIsDisplayed()
    }

    /**
     * Without the permission there is nothing to scan. The screen must ask
     * for it rather than claim the device has no music — those are the same
     * empty list and completely different messages.
     */
    @Test
    fun `a missing permission is a prompt, not an empty library`() {
        revokeAudioPermission()
        publishTracks(trackRow())

        screen()

        awaitText("Accedi ai brani sul dispositivo")
        compose.onNodeWithText("Concedi accesso").assertIsDisplayed()
    }

    @Test
    fun `a granted permission with no music says the device is empty`() {
        publishTracks()

        screen()

        awaitText("Nessun brano trovato")
    }

    @Test
    fun `tapping a track plays it with the rest of the library as its queue`() {
        publishTracks(
            trackRow(id = 1L, title = "Breed"),
            trackRow(id = 2L, title = "Lithium"),
        )
        var played: Pair<LocalTrack, List<LocalTrack>>? = null

        screen(onPlayTrack = { t, queue -> played = t to queue })
        awaitText("Breed")
        compose.onNodeWithText("Breed").performClick()

        assertEquals("Breed", played?.first?.title)
        assertEquals(2, played?.second?.size)
    }

    @Test
    fun `shuffle hands over the whole visible list`() {
        publishTracks(trackRow(id = 1L), trackRow(id = 2L, title = "Lithium"))
        var shuffled: List<LocalTrack>? = null

        screen(onShufflePlay = { shuffled = it })
        awaitText("Breed")
        compose.onNodeWithText("Riproduzione casuale").performClick()

        assertEquals(2, shuffled?.size)
    }

    /**
     * With nothing on the device the empty state replaces the tab content
     * entirely — no action bar, no sort menu, nothing to press.
     */
    @Test
    fun `an empty device offers no controls to press`() {
        publishTracks()

        screen()
        awaitText("Nessun brano trovato")

        compose.onAllNodesWithContentDescription("Ordina").assertCountEquals(0)
        compose.onAllNodesWithText("Riproduzione casuale").assertCountEquals(0)
    }

    @Test
    fun `sorting by artist reorders the list`() {
        publishTracks(
            trackRow(id = 1L, title = "Aaa", artist = "Zappa"),
            trackRow(id = 2L, title = "Zzz", artist = "Abba"),
        )

        screen()
        awaitText("Aaa")
        // Default sort is by title, so Aaa sits above Zzz.
        assertTrue(topOf("Aaa") < topOf("Zzz"))

        compose.onNodeWithContentDescription("Ordina").performClick()
        compose.onNodeWithText("Artista").performClick()
        compose.waitForIdle()

        // Zzz is by Abba, so sorting by artist lifts it above Aaa.
        assertTrue(topOf("Zzz") < topOf("Aaa"))
    }

    private fun topOf(text: String): Float =
        compose.onNodeWithText(text).fetchSemanticsNode().positionInRoot.y

    @Test
    fun `the folders tab groups tracks by their directory`() {
        publishTracks(
            trackRow(id = 1L, title = "Breed", folder = "Nirvana"),
            trackRow(id = 2L, title = "Lithium", folder = "Nirvana"),
            trackRow(id = 3L, title = "Innuendo", folder = "Queen"),
        )

        screen()
        awaitText("Breed")
        compose.onNodeWithText("Cartelle").performClick()

        awaitText("Nirvana")
        compose.onNodeWithText("Queen").assertIsDisplayed()
    }

    @Test
    fun `opening a folder reports the path, not the display name`() {
        publishTracks(trackRow(id = 1L, folder = "Nirvana"))
        var opened: String? = null

        screen(onOpenFolder = { opened = it })
        awaitText("Breed")
        compose.onNodeWithText("Cartelle").performClick()
        awaitText("Nirvana")
        compose.onNodeWithText("Nirvana").performClick()

        assertEquals("Music/Nirvana", opened)
    }

    /**
     * A folder can be turned into a local playlist in one step. The name is
     * taken from the folder, and every track in it comes along.
     */
    @Test
    fun `a folder can be saved as a local playlist`() {
        publishTracks(
            trackRow(id = 1L, title = "Breed", folder = "Nirvana"),
            trackRow(id = 2L, title = "Lithium", folder = "Nirvana"),
        )

        screen()
        awaitText("Breed")
        compose.onNodeWithText("Cartelle").performClick()
        awaitText("Nirvana")
        compose.onNodeWithContentDescription("Altre opzioni").performClick()
        compose.onNodeWithText("Crea playlist da cartella").performClick()

        awaitPlaylists { it.size == 1 }
        val saved = runBlocking { localPlaylists().snapshot() }.single()
        assertEquals("Nirvana", saved.name)
        assertEquals(listOf(1L, 2L), saved.trackIds)
    }

    @Test
    fun `the albums tab lists each album once with its artist`() {
        publishTracks(
            trackRow(id = 1L, title = "Breed", album = "Nevermind"),
            trackRow(id = 2L, title = "Lithium", album = "Nevermind"),
            trackRow(id = 3L, title = "Innuendo", album = "Innuendo", artist = "Queen"),
        )

        screen()
        awaitText("Breed")
        compose.onNodeWithText("Album").performClick()

        awaitText("Nevermind")
        compose.onAllNodesWithText("Nevermind").assertCountEquals(1)
    }

    /** Tracks with no album tag would otherwise group under an empty tile. */
    @Test
    fun `tracks without an album are left out of the albums tab`() {
        publishTracks(
            trackRow(id = 1L, title = "Breed", album = "Nevermind"),
            trackRow(id = 2L, title = "Untagged", album = "<unknown>"),
        )

        screen()
        awaitText("Breed")
        compose.onNodeWithText("Album").performClick()

        awaitText("Nevermind")
        compose.onAllNodesWithText("Untagged").assertCountEquals(0)
    }

    @Test
    fun `the playlists tab starts empty and offers to create one`() {
        publishTracks(trackRow())

        screen()
        awaitText("Breed")
        compose.onNodeWithText("Playlist").performClick()

        awaitText("Nessuna playlist locale", substring = true)
        openDialog("Crea nuova playlist")

        compose.onNodeWithText("Nuova playlist").assertIsDisplayed()
    }

    /** A nameless playlist would render as a blank row nobody can identify. */
    @Test
    fun `a playlist cannot be created without a name`() {
        publishTracks(trackRow())

        screen()
        awaitText("Breed")
        compose.onNodeWithText("Playlist").performClick()
        awaitText("Crea nuova playlist")
        openDialog("Crea nuova playlist")

        compose.onNodeWithText("Crea").assertIsNotEnabled()
    }

    @Test
    fun `cancelling the create dialog leaves no playlist behind`() {
        publishTracks(trackRow())

        screen()
        awaitText("Breed")
        compose.onNodeWithText("Playlist").performClick()
        awaitText("Crea nuova playlist")
        openDialog("Crea nuova playlist")
        compose.onNodeWithText("Annulla").performClick()
        pumpFrames()
        compose.mainClock.autoAdvance = true

        assertTrue(runBlocking { localPlaylists().snapshot() }.isEmpty())
    }

    @Test
    fun `an existing local playlist is listed with its track count`() {
        runBlocking { localPlaylists().create("Corsa", listOf(1L, 2L)) }
        publishTracks(trackRow())

        screen()
        awaitText("Breed")
        compose.onNodeWithText("Playlist").performClick()

        awaitText("Corsa")
        compose.onNodeWithText("2 brani", substring = true).assertIsDisplayed()
    }

    @Test
    fun `opening a local playlist reports its id`() {
        val created = runBlocking { localPlaylists().create("Corsa", listOf(1L)) }
        publishTracks(trackRow())
        var opened: String? = null

        screen(onOpenPlaylist = { opened = it })
        awaitText("Breed")
        compose.onNodeWithText("Playlist").performClick()
        awaitText("Corsa")
        compose.onNodeWithText("Corsa").performClick()

        assertEquals(created.id, opened)
    }

    /**
     * The liked shortcut only earns its place at the top of the list once
     * something has been liked.
     */
    @Test
    fun `the liked shortcut appears only when something is liked`() {
        publishTracks(trackRow(id = 1L))

        screen()
        awaitText("Breed")

        compose.onAllNodesWithText("Brani che ti piacciono").assertCountEquals(0)
    }

    @Test
    fun `liking a track from the row menu brings up the liked shortcut`() {
        publishTracks(trackRow(id = 1L, title = "Breed"))

        screen()
        awaitText("Breed")
        compose.onNodeWithContentDescription("Altre opzioni").performClick()
        compose.onNodeWithText("Aggiungi ai preferiti").performClick()

        awaitText("Brani che ti piacciono")
        assertEquals(setOf(1L), runBlocking { localLikes().liked.first() })
    }

    @Test
    fun `the row menu can queue a track next`() {
        publishTracks(trackRow(id = 1L, title = "Breed"))
        var next: LocalTrack? = null

        screen(onPlayNext = { next = it })
        awaitText("Breed")
        compose.onNodeWithContentDescription("Altre opzioni").performClick()
        compose.onNodeWithText("Riproduci dopo").performClick()

        assertEquals(1L, next?.id)
    }

    @Test
    fun `the row menu can append a track to the queue`() {
        publishTracks(trackRow(id = 1L, title = "Breed"))
        var queued: LocalTrack? = null

        screen(onAddToQueue = { queued = it })
        awaitText("Breed")
        compose.onNodeWithContentDescription("Altre opzioni").performClick()
        compose.onNodeWithText("Aggiungi alla coda").performClick()

        assertEquals(1L, queued?.id)
    }

    @Test
    fun `adding a track to a local playlist goes through the picker sheet`() {
        runBlocking { localPlaylists().create("Corsa") }
        publishTracks(trackRow(id = 7L, title = "Breed"))

        screen()
        awaitText("Breed")
        compose.onNodeWithContentDescription("Altre opzioni").performClick()
        compose.onNodeWithText("Aggiungi a playlist locale").performClick()
        awaitText("Aggiungi a playlist locale")
        compose.onNodeWithText("Corsa").performClick()

        awaitPlaylists { it.single().trackIds == listOf(7L) }
    }

    @Test
    fun `back is wired to the caller`() {
        publishTracks(trackRow())
        var backs = 0

        screen(onBack = { backs++ })
        awaitText("Breed")
        compose.onNodeWithContentDescription("Indietro").performClick()

        assertEquals(1, backs)
    }

    @Test
    fun `the liked shortcut opens the liked screen`() {
        runBlocking { localLikes().setLiked(1L, true) }
        publishTracks(trackRow(id = 1L))
        var opened = 0

        screen(onOpenLiked = { opened++ })
        awaitText("Brani che ti piacciono")
        compose.onNodeWithText("Brani che ti piacciono").performClick()

        assertEquals(1, opened)
    }
}
