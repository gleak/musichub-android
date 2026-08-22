package com.mediaplayer.android.data

import com.mediaplayer.android.data.dto.DjChatMessageDto
import com.mediaplayer.android.data.dto.DjChatReplyDto
import com.mediaplayer.android.data.dto.DjPreferencesDto
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
 * Le rotte del DJ, separate da [MediaPlayerApi] per una ragione tecnica e
 * non estetica: Retrofit lega un'interfaccia a un client, e queste chiamate
 * devono passare da [Network.djApi], che ha un read timeout piu' lungo
 * dei 30 secondi del client generale. Una risposta di chat puo' impiegarne
 * fino a 60 (`dj.chat.timeout-seconds`).
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

    @POST("api/dj/chat")
    suspend fun sendMessage(@Body body: DjSendMessageRequest): DjChatReplyDto

    /** Cancella conversazione, profilo e storico del profilo. 204. */
    @DELETE("api/dj/chat")
    suspend fun eraseChat(): Response<Unit>

    /** Converte uno slot DJ in playlist dell'utente, sul posto. */
    @POST("api/dj/playlists/{id}/promote")
    suspend fun promotePlaylist(@Path("id") id: Long): PlaylistDetailDto
}
