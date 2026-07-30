package com.mediaplayer.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Spotify import parser is where a user's playlist file meets the app, and
 * the failure mode it was rewritten to fix was silence: a rejected file used to
 * come back as an empty list, so the import screen said "no songs found" no
 * matter what was actually wrong. These tests pin both the parsing and the
 * fact that each rejection explains itself.
 */
class CsvPlaylistParserTest {

    @Test
    fun `parses the canonical English Exportify header`() {
        val tracks = CsvPlaylistParser.parse(
            listOf(
                "Spotify ID,Artist IDs,Track Name,Album Name,Artist Name(s),Released",
                "abc,xyz,Bohemian Rhapsody,A Night at the Opera,Queen,1975",
            )
        )

        assertEquals(1, tracks.size)
        assertEquals("Bohemian Rhapsody", tracks[0].title)
        assertEquals("Queen", tracks[0].artist)
    }

    /** Most users receive the Italian export, because the Spotify web UI is Italian. */
    @Test
    fun `parses the Italian localised header`() {
        val tracks = CsvPlaylistParser.parse(
            listOf(
                "ID,Titolo,Album,Artista,Data di uscita",
                "1,Vivere,Anime salve,Fabrizio De André,1996",
            )
        )

        assertEquals("Vivere", tracks[0].title)
        assertEquals("Fabrizio De André", tracks[0].artist)
    }

    @Test
    fun `accepts every documented header synonym`() {
        val titleAliases = listOf("Track Name", "Titolo", "Nome della traccia", "Brano")
        val artistAliases = listOf("Artist Name(s)", "Artista", "Nome dell'artista", "Artisti")

        for (title in titleAliases) {
            for (artist in artistAliases) {
                val tracks = CsvPlaylistParser.parse(listOf("$title,$artist", "T,A"))
                assertEquals("$title / $artist", "T", tracks[0].title)
                assertEquals("$title / $artist", "A", tracks[0].artist)
            }
        }
    }

    /**
     * Every Exportify CSV carries "Artist IDs" and "Artist Genres". Matching
     * headers by substring would let those shadow the real artist column and
     * import a playlist full of hex ids.
     */
    @Test
    fun `id and genre columns do not shadow the artist column`() {
        val tracks = CsvPlaylistParser.parse(
            listOf(
                "Artist IDs,Artist Genres,Track Name,Artist Name(s)",
                "4pSFT,rock,Song,Real Artist",
            )
        )

        assertEquals("Real Artist", tracks[0].artist)
    }

    @Test
    fun `commas inside quotes stay part of the cell`() {
        val tracks = CsvPlaylistParser.parse(
            listOf(
                "Track Name,Artist Name(s)",
                "\"Ballad of a Thin Man\",\"Dylan, Bob\"",
            )
        )

        assertEquals("Ballad of a Thin Man", tracks[0].title)
        assertEquals("Dylan, Bob", tracks[0].artist)
    }

    @Test
    fun `header matching ignores case and surrounding spaces`() {
        val tracks = CsvPlaylistParser.parse(listOf("  TRACK NAME , artista ", "T,A"))

        assertEquals("T", tracks[0].title)
        assertEquals("A", tracks[0].artist)
    }

    @Test
    fun `rows without a title are skipped rather than imported blank`() {
        val tracks = CsvPlaylistParser.parse(
            listOf(
                "Track Name,Artist Name(s)",
                "Kept,Artist",
                ",Orphan artist",
                "Also kept,Another",
            )
        )

        assertEquals(listOf("Kept", "Also kept"), tracks.map { it.title })
    }

    @Test
    fun `blank rows are ignored`() {
        val tracks = CsvPlaylistParser.parse(
            listOf("Track Name,Artist Name(s)", "", "Song,Artist", ",")
        )

        assertEquals(1, tracks.size)
    }

    // --- rejections, each of which has to explain itself ------------------

    @Test
    fun `an empty file is rejected`() {
        val e = assertThrows(CsvPlaylistParseException::class.java) {
            CsvPlaylistParser.parse(emptyList())
        }
        assertTrue(e.message!!.contains("vuoto"))
    }

    @Test
    fun `a header with no rows is rejected`() {
        val e = assertThrows(CsvPlaylistParseException::class.java) {
            CsvPlaylistParser.parse(listOf("Track Name,Artist Name(s)"))
        }
        assertTrue(e.message!!.contains("intestazione"))
    }

    /** The message must name the missing column, not just say the file is bad. */
    @Test
    fun `an unrecognised header names the missing columns`() {
        val e = assertThrows(CsvPlaylistParseException::class.java) {
            CsvPlaylistParser.parse(listOf("Colonna A,Colonna B", "1,2"))
        }

        assertTrue(e.message!!.contains("titolo del brano"))
        assertTrue(e.message!!.contains("nome dell'artista"))
        assertTrue("should preview what it did find", e.message!!.contains("Colonna A"))
    }

    @Test
    fun `a missing artist column alone is reported alone`() {
        val e = assertThrows(CsvPlaylistParseException::class.java) {
            CsvPlaylistParser.parse(listOf("Track Name,Qualcosa", "T,X"))
        }

        assertTrue(e.message!!.contains("nome dell'artista"))
        assertTrue("title column was present", !e.message!!.contains("titolo del brano"))
    }

    @Test
    fun `rows that exist but are all title-less are rejected`() {
        val e = assertThrows(CsvPlaylistParseException::class.java) {
            CsvPlaylistParser.parse(listOf("Track Name,Artist Name(s)", ",A", ",B"))
        }

        assertTrue(e.message!!.contains("Nessun titolo valido"))
    }

    @Test
    fun `parseRows is equivalent to parse for already tokenised input`() {
        val rows = listOf(
            listOf("Track Name", "Artist Name(s)"),
            listOf("Song", "Artist"),
        )

        assertEquals(CsvPlaylistParser.parse(listOf("Track Name,Artist Name(s)", "Song,Artist")),
            CsvPlaylistParser.parseRows(rows))
    }
}
