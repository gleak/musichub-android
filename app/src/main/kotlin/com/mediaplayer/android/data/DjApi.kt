package com.mediaplayer.android.data

import com.mediaplayer.android.data.dto.DjChatMessageDto
import com.mediaplayer.android.data.dto.DjPreferencesDto
import com.mediaplayer.android.data.dto.DjTurnAcceptedDto
import com.mediaplayer.android.data.dto.DjTurnDto
import com.mediaplayer.android.data.dto.DjRunDto
import com.mediaplayer.android.data.dto.DjSendMessageRequest
import com.mediaplayer.android.data.dto.DjStatusDto
import com.mediaplayer.android.data.dto.DjTasteProfileDto
import com.mediaplayer.android.data.dto.DjUpdatePreferencesRequest
import com.mediaplayer.android.data.dto.PlaylistDetailDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Le rotte del DJ, separate da [MediaPlayerApi] solo per tenerle insieme.
 *
 * Fino alla chat asincrona questa interfaccia aveva un client HTTP tutto suo
 * con un read timeout di 120 secondi, perche' `POST api/dj/chat` restava
 * aperta finche' il DJ non aveva finito di pensare. Non serve piu': nessuna
 * chiamata del DJ e' lunga: sia il giro di composizione sia il turno di chat
 * rispondono 202 e si seguono col polling. Un client speciale in meno da
 * ricordarsi di tenere allineato quando cambia l'autenticazione.
 */
interface DjApi {

    /** Se c'e' un agente, se il ciclo e la chat sono accesi, se un giro e' aperto. */
    @GET("api/dj/status")
    suspend fun status(): DjStatusDto

    @GET("api/dj/preferences")
    suspend fun preferences(): DjPreferencesDto

    @PUT("api/dj/preferences")
    suspend fun updatePreferences(@Body body: DjUpdatePreferencesRequest): DjPreferencesDto

    @GET("api/dj/profile")
    suspend fun profile(): DjTasteProfileDto

    /** Gli ultimi cinque giri, dal piu' recente. */
    @GET("api/dj/runs")
    suspend fun recentRuns(): List<DjRunDto>

    /** Polling dell'esito di un giro aperto. Terminale = status != RUNNING. */
    @GET("api/dj/runs/{id}")
    suspend fun run(@Path("id") id: Long): DjRunDto

    /**
     * 202 con la riga appena aperta (status RUNNING), non con l'esito.
     * 409 se un giro e' gia' in corso, 429 dentro il cooldown con
     * `Retry-After`. Vedi [DjRefusal].
     */
    @POST("api/dj/run")
    suspend fun startRun(): DjRunDto

    @GET("api/dj/chat")
    suspend fun chat(@Query("limit") limit: Int = 200): List<DjChatMessageDto>

    /**
     * 202 con l'id del turno appena aperto, non con la risposta: quella si
     * aspetta con [chatTurn]. 409 se un turno e' gia' in volo, 429 col tetto
     * giornaliero, 503 a chat spenta. Vedi [DjRefusal].
     */
    @POST("api/dj/chat")
    suspend fun sendMessage(@Body body: DjSendMessageRequest): DjTurnAcceptedDto

    /** Polling del turno. Terminale = status != RUNNING. */
    @GET("api/dj/chat/turns/{id}")
    suspend fun chatTurn(@Path("id") id: Long): DjTurnDto

    /**
     * L'ultimo turno, in corso o concluso. Riaggancia il polling quando si
     * riapre lo schermo mentre il DJ sta ancora rispondendo. 404 se questo
     * utente non ha mai scritto al DJ.
     */
    @GET("api/dj/chat/turns/latest")
    suspend fun latestChatTurn(): DjTurnDto

    /**
     * Fa comporre davvero la playlist concordata in quel turno di chat.
     *
     * Non manda nessun testo: il briefing con cui si compone vive sulla riga
     * del messaggio lato server. Se lo mandasse il client, chiunque potrebbe
     * far comporre al DJ qualunque cosa chiamando l'endpoint da solo.
     *
     * Risponde 202 con la riga appena aperta (status RUNNING), non con
     * l'esito: e' un giro da minuti, e si segue con [run]. 404 se quel
     * messaggio non esiste, non e' tuo, o non portava nessuna proposta;
     * 409/429 con le stesse regole di [startRun].
     */
    @POST("api/dj/chat/messages/{messageId}/playlist")
    suspend fun composePlaylistFromChat(@Path("messageId") messageId: Long): DjRunDto

    /** Cancella conversazione, profilo e storico del profilo. 204. */
    @DELETE("api/dj/chat")
    suspend fun eraseChat(): Response<Unit>

    /** Converte uno slot DJ in playlist dell'utente, sul posto. */
    @POST("api/dj/playlists/{id}/promote")
    suspend fun promotePlaylist(@Path("id") id: Long): PlaylistDetailDto
}
