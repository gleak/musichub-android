package com.mediaplayer.android.data

import com.mediaplayer.android.data.dto.DjChatMessageDto
import com.mediaplayer.android.data.dto.DjChatReplyDto
import com.mediaplayer.android.data.dto.DjErrorBody
import com.mediaplayer.android.data.dto.DjPreferencesDto
import com.mediaplayer.android.data.dto.DjRunDto
import com.mediaplayer.android.data.dto.DjSendMessageRequest
import com.mediaplayer.android.data.dto.DjStatusDto
import com.mediaplayer.android.data.dto.DjTasteProfileDto
import com.mediaplayer.android.data.dto.DjUpdatePreferencesRequest
import com.mediaplayer.android.data.dto.PlaylistDetailDto
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/**
 * Un rifiuto che l'interfaccia deve poter spiegare, non subire.
 *
 * `POST /api/dj/run` risponde 409 quando un giro e' gia' in corso e 429
 * dentro il cooldown, con l'attesa residua sia nell'header `Retry-After`
 * sia nel corpo. La spec e' esplicita: l'interfaccia deve mostrare l'attesa
 * residua. Retrofit consegna entrambi come [HttpException], e il corpo
 * d'errore non lo legge nessuno se non lo si legge di proposito.
 */
data class DjRefusal(val status: Int, val message: String?, val retryAfterSeconds: Long?) {

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /** Null quando non e' un rifiuto HTTP (rete assente, parsing fallito, ...). */
        fun of(t: Throwable): DjRefusal? {
            val http = t as? HttpException ?: return null
            val response = http.response()
            val raw = runCatching { response?.errorBody()?.string() }.getOrNull()
            val body = raw?.takeIf { it.isNotBlank() }
                ?.let { runCatching { json.decodeFromString<DjErrorBody>(it) }.getOrNull() }
            val fromHeader = response?.headers()?.get("Retry-After")?.toLongOrNull()
            return DjRefusal(
                status = http.code(),
                message = body?.error,
                retryAfterSeconds = fromHeader ?: body?.retryAfterSeconds,
            )
        }
    }
}

/**
 * Wrapper sottile su [DjApi], perche' i ViewModel dipendano da codice
 * nostro e i test possano sostituirlo. Stesso schema di
 * [PlaylistRepository].
 *
 * Nessuna cache offline: la sezione DJ e' una conversazione con un servizio
 * che vive sul server. Mostrare messaggi vecchi mentre non si ha rete
 * suggerirebbe che il DJ possa risponderne di nuovi.
 */
class DjRepository(private val injectedApi: DjApi? = null) {

    private val api: DjApi get() = injectedApi ?: Network.djApi

    suspend fun status(): DjStatusDto = api.status()

    suspend fun preferences(): DjPreferencesDto = api.preferences()

    suspend fun updatePreferences(
        cycleEnabled: Boolean? = null,
        slots: Int? = null,
        cadenceDays: Int? = null,
    ): DjPreferencesDto =
        api.updatePreferences(DjUpdatePreferencesRequest(cycleEnabled, slots, cadenceDays))

    suspend fun profile(): DjTasteProfileDto = api.profile()

    suspend fun recentRuns(): List<DjRunDto> = api.recentRuns()

    suspend fun chat(): List<DjChatMessageDto> = api.chat()

    suspend fun sendMessage(message: String): DjChatReplyDto =
        api.sendMessage(DjSendMessageRequest(message))

    suspend fun eraseChat() {
        api.eraseChat()
    }

    suspend fun promotePlaylist(playlistId: Long): PlaylistDetailDto =
        api.promotePlaylist(playlistId)

    /**
     * Fa partire un giro e aspetta l'esito.
     *
     * Apre in modo sincrono (il 202 porta gia' la riga, quindi un secondo
     * tentativo subito dopo la vede) e poi interroga finche' lo stato non e'
     * terminale. [onProgress] riceve ogni risposta, la prima compresa, cosi'
     * la schermata puo' mostrare "sta scegliendo" senza aspettare la fine.
     */
    suspend fun forceRun(onProgress: (DjRunDto) -> Unit = {}): DjRunDto {
        val opened = api.startRun()
        onProgress(opened)
        return DjRunPolling.awaitTerminal(
            runId = opened.id,
            fetch = { api.run(it) },
            onUpdate = onProgress,
        )
    }
}
