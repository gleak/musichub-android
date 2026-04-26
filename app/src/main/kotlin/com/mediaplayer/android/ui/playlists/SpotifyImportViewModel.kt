package com.mediaplayer.android.ui.playlists

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mediaplayer.android.data.CsvPlaylistParser
import com.mediaplayer.android.data.FindRepository
import com.mediaplayer.android.data.PlaylistRepository
import com.mediaplayer.android.data.SongRepository
import com.mediaplayer.android.data.SpotifyImportTrack
import com.mediaplayer.android.data.dto.RequestStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface SpotifyImportUiState {
    data object Idle : SpotifyImportUiState
    data object FetchingPlaylist : SpotifyImportUiState
    data class Confirming(
        val playlistName: String,
        val tracks: List<SpotifyImportTrack>,
    ) : SpotifyImportUiState
    data class Importing(
        val total: Int,
        val completed: Int,
        val imported: Int,
        val failed: Int,
        val currentTrack: String,
    ) : SpotifyImportUiState
    data class Done(
        val playlistId: Long,
        val playlistName: String,
        val imported: Int,
        val failed: Int,
    ) : SpotifyImportUiState
    data class Error(val message: String) : SpotifyImportUiState
}

class SpotifyImportViewModel(application: Application) : AndroidViewModel(application) {

    private val findRepository = FindRepository()
    private val playlistRepository = PlaylistRepository()
    private val songRepository = SongRepository()

    private val _state = MutableStateFlow<SpotifyImportUiState>(SpotifyImportUiState.Idle)
    val state: StateFlow<SpotifyImportUiState> = _state.asStateFlow()

    fun importFromUri(uri: Uri) {
        _state.value = SpotifyImportUiState.FetchingPlaylist
        viewModelScope.launch {
            _state.value = try {
                val (name, tracks) = withContext(Dispatchers.IO) {
                    val resolver = getApplication<Application>().contentResolver
                    val lines = resolver.openInputStream(uri)
                        ?.bufferedReader()?.readLines()
                        ?: throw Exception("Could not read file.")
                    val tracks = CsvPlaylistParser.parse(lines)
                    val name = resolveFilename(uri)
                    name to tracks
                }
                if (tracks.isEmpty()) {
                    SpotifyImportUiState.Error(
                        "No tracks found. Make sure this is an Exportify CSV file."
                    )
                } else {
                    SpotifyImportUiState.Confirming(playlistName = name, tracks = tracks)
                }
            } catch (t: Throwable) {
                SpotifyImportUiState.Error(t.message ?: "Failed to read file.")
            }
        }
    }

    fun reset() {
        _state.value = SpotifyImportUiState.Idle
    }

    fun startImport(playlistName: String) {
        val confirming = _state.value as? SpotifyImportUiState.Confirming ?: return
        val tracks = confirming.tracks
        val name = playlistName.trim().ifEmpty { confirming.playlistName }

        viewModelScope.launch {
            val playlist = try {
                playlistRepository.create(name)
            } catch (t: Throwable) {
                _state.value = SpotifyImportUiState.Error("Failed to create playlist: ${t.message}")
                return@launch
            }

            var imported = 0
            var failed = 0

            for (track in tracks) {
                _state.value = SpotifyImportUiState.Importing(
                    total = tracks.size,
                    completed = imported + failed,
                    imported = imported,
                    failed = failed,
                    currentTrack = trackLabel(track),
                )

                val songId = downloadAndLocate(track)
                if (songId != null) {
                    try {
                        playlistRepository.addSong(playlist.id, songId)
                        imported++
                    } catch (_: Throwable) {
                        failed++
                    }
                } else {
                    failed++
                }
            }

            _state.value = SpotifyImportUiState.Done(
                playlistId = playlist.id,
                playlistName = name,
                imported = imported,
                failed = failed,
            )
        }
    }

    private suspend fun downloadAndLocate(track: SpotifyImportTrack): Long? {
        return try {
            var request = findRepository.create(trackLabel(track))
            var polls = 0
            var selected = false

            while (!request.status.isTerminal && polls < MAX_POLLS) {
                delay(POLL_MS)
                polls++
                request = findRepository.detail(request.id)

                if (request.status == RequestStatus.AWAITING_SELECTION && !selected) {
                    val candidate = request.candidates.firstOrNull() ?: continue
                    request = findRepository.select(request.id, candidate.id)
                    selected = true
                }
            }

            if (request.status == RequestStatus.IMPORTED ||
                request.status == RequestStatus.IMPORTED_PARTIAL
            ) {
                locateSong(track)
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private suspend fun locateSong(track: SpotifyImportTrack): Long? {
        return try {
            val results = songRepository.listSongs("${track.artist} ${track.title}", size = 5)
            results.items.firstOrNull { it.title.equals(track.title, ignoreCase = true) }?.id
                ?: results.items.firstOrNull()?.id
        } catch (_: Throwable) {
            null
        }
    }

    private fun resolveFilename(uri: Uri): String {
        val cursor = getApplication<Application>().contentResolver
            .query(uri, null, null, null, null)
        return cursor?.use {
            val col = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst() && col >= 0) it.getString(col) else null
        }?.removeSuffix(".csv") ?: "Imported Playlist"
    }

    private fun trackLabel(track: SpotifyImportTrack): String =
        "${track.artist} ${track.title}".trim()

    private companion object {
        const val POLL_MS = 2_000L
        const val MAX_POLLS = 90
    }
}
