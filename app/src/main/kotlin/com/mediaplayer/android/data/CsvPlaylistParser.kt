package com.mediaplayer.android.data

import com.mediaplayer.android.data.SpotifyImportTrack

/**
 * Parses Exportify CSV files (https://exportify.net).
 *
 * Exportify header (as of 2024):
 *   Spotify ID, Artist IDs, Track Name, Album Name, Artist Name(s), Released, …
 *
 * Column indices are discovered from the header row so the parser survives
 * minor column order changes in future Exportify versions.
 */
object CsvPlaylistParser {

    fun parse(lines: List<String>): List<SpotifyImportTrack> {
        if (lines.size < 2) return emptyList()

        val header = parseCsvLine(lines[0])
        val trackCol = header.indexOfFirst { it.trim().equals("Track Name", ignoreCase = true) }
        val artistCol = header.indexOfFirst { it.trim().equals("Artist Name(s)", ignoreCase = true) }

        if (trackCol < 0 || artistCol < 0) return emptyList()

        return lines.drop(1)
            .mapNotNull { line ->
                val cols = parseCsvLine(line)
                val title = cols.getOrNull(trackCol)?.trim().orEmpty()
                val artist = cols.getOrNull(artistCol)?.trim().orEmpty()
                if (title.isEmpty()) null
                else SpotifyImportTrack(title = title, artist = artist)
            }
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
