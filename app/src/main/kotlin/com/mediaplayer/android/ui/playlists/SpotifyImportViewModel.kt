package com.mediaplayer.android.ui.playlists

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mediaplayer.android.data.CsvPlaylistParseException
import com.mediaplayer.android.data.CsvPlaylistParser
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.data.SpotifyImportTrack
import com.mediaplayer.android.data.XlsxRowReader
import com.mediaplayer.android.data.dto.SpotifyImportJobStatusDto
import java.io.ByteArrayInputStream
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
                    // Hard 10 MB cap on the raw read so a misnamed huge file
                    // (or a zip-bomb XLSX that expands to multiple GB on
                    // decompression) can't OOM the app. A real Exportify
                    // / Spotify CSV for a 10k-track playlist is well under
                    // 2 MB; this leaves comfortable headroom.
                    val bytes = resolver.openInputStream(uri)?.use { stream ->
                        readCapped(stream, IMPORT_FILE_CAP_BYTES)
                    } ?: throw Exception("Impossibile aprire il file selezionato (il provider non ha restituito uno stream).")
                    val tracks = if (looksLikeXlsx(bytes)) {
                        // Spotify's Italian "Account info" download and several
                        // browser-side exporters now ship .xlsx instead of .csv;
                        // route those through the lightweight in-process parser
                        // so the user doesn't have to convert by hand.
                        val rows = XlsxRowReader.read(ByteArrayInputStream(bytes))
                        CsvPlaylistParser.parseRows(rows)
                    } else {
                        // Strip the UTF-8 BOM (`EF BB BF`) — some browsers
                        // emit it and it would otherwise survive as a
                        // zero-width prefix on the first header cell,
                        // breaking column detection on the first row.
                        val text = bytes.toString(Charsets.UTF_8).removePrefix("﻿")
                        CsvPlaylistParser.parse(text.split('\n').map { it.trimEnd('\r') })
                    }
                    val name = resolveFilename(uri)
                    name to tracks
                }
                SpotifyImportUiState.Confirming(playlistName = name, tracks = tracks, uri = uri)
            } catch (t: Throwable) {
                SpotifyImportUiState.Error(describeError(t, fallback = "Lettura del file non riuscita."))
            }
        }
    }

    /**
     * Detects an XLSX file by its ZIP magic bytes (`PK\x03\x04`). Cheap byte
     * sniff — no need to trust the URI's MIME type or extension, since the
     * Android file picker will hand back `application/octet-stream` for
     * `.xlsx` on plenty of devices and the user could rename a `.csv` to
     * something else anyway.
     */
    private fun looksLikeXlsx(bytes: ByteArray): Boolean =
        bytes.size >= 4 &&
            bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() &&
            bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()

    fun reset() {
        pollJob?.cancel()
        pollJob = null
        _state.value = SpotifyImportUiState.Idle
    }

    fun startImport(playlistName: String) {
        val confirming = _state.value as? SpotifyImportUiState.Confirming ?: return
        val name = playlistName.trim().ifEmpty { confirming.playlistName }
        val tracks = confirming.tracks

        _state.value = SpotifyImportUiState.Importing(
            total = tracks.size,
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
                    val tmpFile = File.createTempFile(
                        "spotify_import", ".csv",
                        getApplication<Application>().cacheDir,
                    )
                    try {
                        // Always upload a normalized CSV with English headers
                        // synthesized from the rows we parsed locally — keeps
                        // the backend importer (which only knows the canonical
                        // Exportify schema) untouched while letting us accept
                        // Italian CSVs and XLSX workbooks on the client side.
                        writeNormalizedCsv(tmpFile, tracks)

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
                    describeError(t, fallback = "Importazione non riuscita.")
                )
            }
        }
    }

    /**
     * Writes a minimal Exportify-compatible CSV into [target] containing only
     * the two columns the backend matcher actually needs: `Track Name` and
     * `Artist Name(s)`. Values with commas / quotes / newlines are wrapped in
     * double-quotes with internal `"` doubled (RFC 4180).
     */
    private fun writeNormalizedCsv(target: File, tracks: List<SpotifyImportTrack>) {
        target.bufferedWriter(Charsets.UTF_8).use { w ->
            w.write("Track Name,Artist Name(s)")
            w.write("\r\n")
            for (t in tracks) {
                w.write(csvEscape(t.title))
                w.write(",")
                w.write(csvEscape(t.artist))
                w.write("\r\n")
            }
        }
    }

    private fun csvEscape(value: String): String {
        val needsQuote = value.any { it == '"' || it == ',' || it == '\n' || it == '\r' }
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuote) "\"$escaped\"" else escaped
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
                    ) else SpotifyImportUiState.Error(
                        "Il server ha segnato il job come completato ma non ha restituito alcun risultato (job $jobId)."
                    )
                    return
                }
                "ERROR" -> {
                    val serverMsg = status.errorMessage?.takeIf { it.isNotBlank() }
                    _state.value = SpotifyImportUiState.Error(
                        if (serverMsg != null) "Errore dal server: $serverMsg"
                        else "Il server ha interrotto l'importazione senza fornire una causa (job $jobId)."
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

    /**
     * Renders a [Throwable] into a user-readable Italian sentence that
     * actually says *what* went wrong, instead of falling back to a generic
     * "Importazione non riuscita.". The previous code surfaced [Throwable.message]
     * directly — for [retrofit2.HttpException] that's just `"HTTP 500 Internal
     * Server Error"`, for [java.net.UnknownHostException] often null, leaving
     * the user staring at the fallback string with no hint.
     *
     * Priority order:
     *  - [CsvPlaylistParseException] — already user-readable, surface as-is.
     *  - HTTP errors — include status code + a body excerpt (truncated to
     *    280 chars) so backend-reported reasons reach the screen.
     *  - Network-level exceptions — explicit translation per type so the
     *    user knows whether to retry or check connectivity.
     *  - Anything else — class simple name + message, never just the message
     *    alone, so cryptic SDK strings ("Source closed") still come with a
     *    type marker for triage.
     */
    private fun describeError(t: Throwable, fallback: String): String {
        return when (t) {
            is CsvPlaylistParseException -> t.message ?: fallback
            is XlsxRowReader.XlsxReadException -> t.message ?: fallback
            is retrofit2.HttpException -> {
                val code = t.code()
                val body = try {
                    t.response()?.errorBody()?.string()?.trim()
                } catch (_: Throwable) {
                    null
                }
                val excerpt = body?.takeIf { it.isNotBlank() }?.let {
                    if (it.length > 280) it.substring(0, 277) + "…" else it
                }
                buildString {
                    append("Il server ha risposto con un errore HTTP ").append(code)
                    if (!excerpt.isNullOrBlank()) {
                        append(": ").append(excerpt)
                    } else {
                        append(" senza dettagli aggiuntivi.")
                    }
                }
            }
            is java.net.SocketTimeoutException ->
                "Il server non ha risposto in tempo (timeout). Verifica la rete e riprova."
            is java.net.UnknownHostException ->
                "Impossibile raggiungere il server (host non risolto): controlla la connessione."
            is java.net.ConnectException ->
                "Connessione al server rifiutata: ${t.message ?: "il backend potrebbe essere offline"}."
            is javax.net.ssl.SSLException ->
                "Errore di canale sicuro (SSL): ${t.message ?: "handshake non riuscito"}."
            is java.io.IOException ->
                "Errore di rete: ${t.message ?: t.javaClass.simpleName}"
            is kotlinx.serialization.SerializationException ->
                "Risposta non valida dal server: ${t.message ?: "JSON malformato"}."
            else -> {
                val msg = t.message?.takeIf { it.isNotBlank() }
                if (msg != null) "${t.javaClass.simpleName}: $msg"
                else "${t.javaClass.simpleName} ($fallback)"
            }
        }
    }

    private fun resolveFilename(uri: Uri): String {
        val cursor = getApplication<Application>().contentResolver
            .query(uri, null, null, null, null)
        val raw = cursor?.use {
            val col = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst() && col >= 0) it.getString(col) else null
        } ?: return "Playlist importata"
        // Strip the extension *and* sanitize: a malicious or surprising
        // file picker can return paths, control chars or zero-width chars
        // that would otherwise go straight into the multipart `name` field
        // we send to the backend.
        val withoutExt = raw
            .removeSuffix(".csv")
            .removeSuffix(".CSV")
            .removeSuffix(".xlsx")
            .removeSuffix(".XLSX")
        return withoutExt
            .replace(Regex("[\\x00-\\x1F\\x7F]"), "")
            .trim()
            .take(80)
            .ifBlank { "Playlist importata" }
    }

    /**
     * Streams up to [cap] bytes from [stream] and throws if the source has
     * more data left. Drops the 1-byte sentinel check overhead by reading
     * straight into a pre-sized buffer and snapshotting only the used range.
     */
    private fun readCapped(stream: java.io.InputStream, cap: Int): ByteArray {
        val buf = ByteArray(cap)
        var read = 0
        while (read < cap) {
            val n = stream.read(buf, read, cap - read)
            if (n <= 0) break
            read += n
        }
        if (read >= cap && stream.read() != -1) {
            throw Exception("File troppo grande (oltre ${cap / 1_000_000} MB) — rifiutato.")
        }
        return buf.copyOf(read)
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val IMPORT_FILE_CAP_BYTES = 10 * 1024 * 1024
    }
}
