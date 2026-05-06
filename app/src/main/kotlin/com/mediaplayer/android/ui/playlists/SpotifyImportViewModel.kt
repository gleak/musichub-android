package com.mediaplayer.android.ui.playlists

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mediaplayer.android.data.CsvPlaylistParser
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.data.SpotifyImportTrack
import com.mediaplayer.android.data.dto.SpotifyImportJobStatusDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

sealed interface SpotifyImportUiState {
    data object Idle : SpotifyImportUiState
    data object FetchingPlaylist : SpotifyImportUiState
    data class Confirming(
        val playlistName: String,
        val tracks: List<SpotifyImportTrack>,
        val uri: Uri,
    ) : SpotifyImportUiState
    data class Importing(
        val total: Int,
        val current: Int,
        val matched: Int,
        val approx: Int,
        val failed: Int,
        val currentTrack: String?,
    ) : SpotifyImportUiState
    data class Done(
        val playlistId: Long,
        val playlistName: String,
        val matched: Int,
        val approx: Int,
        val queued: Int,
        val failed: Int,
    ) : SpotifyImportUiState
    data class Error(val message: String) : SpotifyImportUiState
}

class SpotifyImportViewModel(application: Application) : AndroidViewModel(application) {

    private val api = Network.api

    private val _state = MutableStateFlow<SpotifyImportUiState>(SpotifyImportUiState.Idle)
    val state: StateFlow<SpotifyImportUiState> = _state.asStateFlow()

    private var pollJob: Job? = null

    fun importFromUri(uri: Uri) {
        _state.value = SpotifyImportUiState.FetchingPlaylist
        viewModelScope.launch {
            _state.value = try {
                val (name, tracks) = withContext(Dispatchers.IO) {
                    val resolver = getApplication<Application>().contentResolver
                    val lines = resolver.openInputStream(uri)
                        ?.bufferedReader()?.readLines()
                        ?: throw Exception("Impossibile leggere il file.")
                    val tracks = CsvPlaylistParser.parse(lines)
                    val name = resolveFilename(uri)
                    name to tracks
                }
                if (tracks.isEmpty()) {
                    SpotifyImportUiState.Error("Nessun brano trovato. Assicurati che sia un file CSV di Exportify.")
                } else {
                    SpotifyImportUiState.Confirming(playlistName = name, tracks = tracks, uri = uri)
                }
            } catch (t: Throwable) {
                SpotifyImportUiState.Error(t.message ?: "Lettura del file non riuscita.")
            }
        }
    }

    fun reset() {
        pollJob?.cancel()
        pollJob = null
        _state.value = SpotifyImportUiState.Idle
    }

    fun startImport(playlistName: String) {
        val confirming = _state.value as? SpotifyImportUiState.Confirming ?: return
        val name = playlistName.trim().ifEmpty { confirming.playlistName }
        val uri = confirming.uri

        _state.value = SpotifyImportUiState.Importing(
            total = confirming.tracks.size,
            current = 0,
            matched = 0,
            approx = 0,
            failed = 0,
            currentTrack = null,
        )

        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            try {
                val jobId = withContext(Dispatchers.IO) {
                    val resolver = getApplication<Application>().contentResolver
                    val tmpFile = File.createTempFile(
                        "spotify_import", ".csv",
                        getApplication<Application>().cacheDir,
                    )
                    try {
                        resolver.openInputStream(uri)?.use { input ->
                            tmpFile.outputStream().use { output -> input.copyTo(output) }
                        } ?: throw Exception("Impossibile leggere il file.")

                        val filePart = MultipartBody.Part.createFormData(
                            "file",
                            tmpFile.name,
                            tmpFile.asRequestBody("text/csv".toMediaType()),
                        )
                        val namePart = name.toRequestBody("text/plain".toMediaType())
                        api.importSpotifyPlaylistAsync(filePart, namePart).jobId
                    } finally {
                        tmpFile.delete()
                    }
                }
                pollProgress(jobId)
            } catch (t: Throwable) {
                _state.value = SpotifyImportUiState.Error(
                    t.message ?: "Importazione non riuscita."
                )
            }
        }
    }

    private suspend fun pollProgress(jobId: String) {
        var backoff = 500L
        while (true) {
            val status: SpotifyImportJobStatusDto = api.getSpotifyImportJobStatus(jobId)
            when (status.phase) {
                "DONE" -> {
                    val r = status.result
                    _state.value = if (r != null) SpotifyImportUiState.Done(
                        playlistId = r.playlistId,
                        playlistName = r.playlistName,
                        matched = r.matched,
                        approx = r.approx,
                        queued = r.queued,
                        failed = r.failed,
                    ) else SpotifyImportUiState.Error("Nessun risultato dal server.")
                    return
                }
                "ERROR" -> {
                    _state.value = SpotifyImportUiState.Error(
                        status.errorMessage ?: "Importazione non riuscita."
                    )
                    return
                }
                else -> {
                    _state.value = SpotifyImportUiState.Importing(
                        total = status.total.coerceAtLeast(1),
                        current = status.current,
                        matched = status.matched,
                        approx = status.approx,
                        failed = status.failed,
                        currentTrack = status.currentTrack,
                    )
                }
            }
            delay(backoff)
            backoff = (backoff * 2).coerceAtMost(2_000L)
        }
    }

    private fun resolveFilename(uri: Uri): String {
        val cursor = getApplication<Application>().contentResolver
            .query(uri, null, null, null, null)
        return cursor?.use {
            val col = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst() && col >= 0) it.getString(col) else null
        }?.removeSuffix(".csv") ?: "Playlist importata"
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }
}
