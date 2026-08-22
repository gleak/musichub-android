package com.mediaplayer.android.data

import com.mediaplayer.android.data.dto.DjRunDto
import kotlinx.coroutines.delay

/**
 * Il contratto di polling di un giro del DJ, in un posto solo.
 *
 * Il trigger e' asincrono perche' un giro puo' durare fino a 300 secondi
 * (`dj.agent.budget-seconds`) mentre OkHttp taglia a 30: il server risponde
 * 202 con la riga appena aperta e questo e' il modo di scoprire come e'
 * andata.
 */
object DjRunPolling {

    /** L'unico stato che significa "non e' finita". */
    const val STATUS_RUNNING = "RUNNING"

    /**
     * Vero quando il giro e' concluso, comunque sia andato.
     *
     * Scritto come "diverso da RUNNING" e non come "OK oppure FAILED"
     * perche' gli esiti terminali sono TRE: PARTIAL significa che le
     * playlist sono state scritte e committate e un passo successivo e'
     * fallito. Un client che aspettasse i primi due resterebbe a fare
     * polling per sempre proprio nel caso in cui l'utente ha ottenuto
     * qualcosa.
     */
    fun isTerminal(status: String?): Boolean =
        !status.isNullOrBlank() && !status.equals(STATUS_RUNNING, ignoreCase = true)

    /** Sollevata quando ci si arrende: porta con se' l'ultimo stato visto. */
    class TimeoutException(val lastRun: DjRunDto?) :
        RuntimeException("Il giro del DJ non si e' concluso entro il tempo di attesa.")

    /**
     * Interroga [fetch] finche' la run non e' terminale.
     *
     * Backoff 1s, 2s, 4s, poi 5s fissi — lo stesso profilo che
     * `SpotifyImportViewModel.pollProgress` usa gia' per l'import. Con 120
     * tentativi copre abbondantemente i 300 secondi di budget dell'agente.
     */
    suspend fun awaitTerminal(
        runId: Long,
        maxAttempts: Int = 120,
        fetch: suspend (Long) -> DjRunDto,
        onUpdate: (DjRunDto) -> Unit = {},
    ): DjRunDto {
        var last: DjRunDto? = null
        var backoff = 1_000L
        repeat(maxAttempts) {
            val current = fetch(runId)
            last = current
            onUpdate(current)
            if (isTerminal(current.status)) return current
            delay(backoff)
            backoff = (backoff * 2).coerceAtMost(5_000L)
        }
        throw TimeoutException(last)
    }
}
