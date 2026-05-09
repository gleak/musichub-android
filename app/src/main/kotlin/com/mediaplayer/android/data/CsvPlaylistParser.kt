package com.mediaplayer.android.data

import com.mediaplayer.android.data.SpotifyImportTrack

/**
 * Thrown when the CSV doesn't look like an Exportify export. The message is
 * surfaced as-is in the import error screen, so it should describe *why* the
 * file was rejected (empty, wrong headers, no rows) rather than the generic
 * "no songs found" we used to show.
 */
class CsvPlaylistParseException(message: String) : Exception(message)

/**
 * Parses Exportify CSV files (https://exportify.net).
 *
 * Exportify header (English, as of 2024):
 *   Spotify ID, Artist IDs, Track Name, Album Name, Artist Name(s), Released, …
 *
 * Italian Exportify (and other Italian-localised CSV exports) translate the
 * column names — most users actually receive the localised file because the
 * Spotify web UI is in Italian. We match both languages by checking the
 * trimmed/lowercased header against an alias list per column.
 *
 * Column indices are discovered from the header row so the parser survives
 * minor column order changes in future Exportify versions. When the file
 * fails validation we throw [CsvPlaylistParseException] with a user-readable
 * Italian message instead of returning an empty list — silently swallowing
 * the failure was the root cause of the "import error with no hint" bug.
 */
object CsvPlaylistParser {

    /**
     * Track-name column synonyms. English first (canonical Exportify), then
     * the Italian variants the localised CSV ships with — exact matches only,
     * so unrelated columns like "Track URI" or "Track ID" don't collide.
     */
    private val TRACK_HEADER_ALIASES = listOf(
        // English
        "track name", "track title", "title",
        // Italian — Exportify locale + common manual exports
        "titolo", "titolo brano", "titolo del brano",
        "nome traccia", "nome del brano", "nome brano", "brano",
    )

    /**
     * Artist-name column synonyms. The English Exportify export uses
     * "Artist Name(s)" (parens included) for collaborations; the Italian one
     * typically drops the parens. "Artist IDs" / "Artist Genres" deliberately
     * do NOT match — they are present in every Exportify CSV and must not
     * shadow the real artist-name column.
     */
    private val ARTIST_HEADER_ALIASES = listOf(
        // English
        "artist name(s)", "artist name", "artists", "artist",
        // Italian
        "artista", "artisti",
        "nome artista", "nome artisti",
        "nome dell'artista", "nome degli artisti",
    )

    fun parse(lines: List<String>): List<SpotifyImportTrack> {
        if (lines.isEmpty()) {
            throw CsvPlaylistParseException("Il file è vuoto.")
        }
        if (lines.size < 2) {
            throw CsvPlaylistParseException(
                "Il file contiene solo l'intestazione, nessun brano da importare."
            )
        }

        val header = parseCsvLine(lines[0])
        val trackCol = findHeaderColumn(header, TRACK_HEADER_ALIASES)
        val artistCol = findHeaderColumn(header, ARTIST_HEADER_ALIASES)

        if (trackCol < 0 || artistCol < 0) {
            val preview = header.take(6).joinToString(", ") { it.trim().ifBlank { "·" } }
            val missing = buildList {
                if (trackCol < 0) add("titolo del brano (es. \"Track Name\" / \"Titolo\" / \"Nome traccia\")")
                if (artistCol < 0) add("nome dell'artista (es. \"Artist Name(s)\" / \"Artista\" / \"Nome artista\")")
            }.joinToString(" e ")
            throw CsvPlaylistParseException(
                "Intestazione del CSV non riconosciuta: manca la colonna $missing. " +
                    "Trovate: $preview… — assicurati di esportare la playlist da Exportify (https://exportify.net)."
            )
        }

        val rows = lines.drop(1).filter { it.isNotBlank() }
        if (rows.isEmpty()) {
            throw CsvPlaylistParseException(
                "Il CSV non contiene righe di brani sotto l'intestazione."
            )
        }

        val tracks = rows.mapNotNull { line ->
            val cols = parseCsvLine(line)
            val title = cols.getOrNull(trackCol)?.trim().orEmpty()
            val artist = cols.getOrNull(artistCol)?.trim().orEmpty()
            if (title.isEmpty()) null
            else SpotifyImportTrack(title = title, artist = artist)
        }

        if (tracks.isEmpty()) {
            throw CsvPlaylistParseException(
                "Nessun titolo valido nelle ${rows.size} righe del CSV: " +
                    "la colonna del titolo (${header[trackCol].trim()}) è vuota in tutte le righe."
            )
        }

        return tracks
    }

    /**
     * Returns the index of the first header cell whose normalised value
     * (trim + lowercase) matches an entry in [aliases], or -1 if none match.
     * Exact equality only — substring matching would let "Artist IDs" hijack
     * the artist-name column.
     */
    private fun findHeaderColumn(header: List<String>, aliases: List<String>): Int {
        val normalised = header.map { it.trim().lowercase() }
        return normalised.indexOfFirst { it in aliases }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    result += current.toString()
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        result += current.toString()
        return result
    }
}
