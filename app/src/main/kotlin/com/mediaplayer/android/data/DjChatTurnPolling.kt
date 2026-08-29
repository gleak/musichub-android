package com.mediaplayer.android.data

import com.mediaplayer.android.data.dto.DjTurnDto
import kotlinx.coroutines.delay

/**
 * Il contratto di polling di un turno di chat, in un posto solo.
 *
 * Gemello di [DjRunPolling], e per la stessa ragione: l'invio e' asincrono
 * perche' un turno puo' durare molto piu' di quanto una richiesta HTTP possa
 * restare aperta. Il server risponde 202 con l'id del turno, e questo e' il
 * modo di scoprire com'e' andata.
 *
 * La differenza rispetto al giro di composizione e' che qui si guarda anche
 * mentre lavora: `onUpdate` riceve ogni risposta, terminale o no, ed e' da li'
 * che lo schermo prende la fase e i secondi trascorsi.
 */
object DjChatTurnPolling {

    /** L'unico stato che significa "non e' finita". */
    const val STATUS_RUNNING = "RUNNING"

    /**
     * Vero quando il turno e' concluso, comunque sia andato.
     *
     * Scritto come "diverso da RUNNING" e non come "OK oppure FAILED" per la
     * stessa ragione di [DjRunPolling.isTerminal]: uno stato che questa
     * versione dell'app non conosce conta come terminale. Se un giorno il
     * backend ne aggiungesse un terzo, fermarsi e mostrare cio' che c'e' e'
     * meno dannoso che restare appesi per sempre su un turno gia' finito.
     */
    fun isTerminal(status: String?): Boolean =
        !status.isNullOrBlank() && !status.equals(STATUS_RUNNING, ignoreCase = true)

    /** Sollevata quando ci si arrende: porta con se' l'ultimo stato visto. */
    class TimeoutException(val lastTurn: DjTurnDto?) :
        RuntimeException("Il DJ non ha concluso il turno entro il tempo di attesa.")

    /**
     * Interroga [fetch] finche' il turno non e' terminale.
     *
     * Backoff 1s, 2s, 4s, poi 5s fissi — lo stesso profilo di [DjRunPolling].
     * Con 200 tentativi copre i 900 secondi di `chat-timeout-seconds` con
     * margine: il freno vero e' quello del server, e arrendersi prima
     * significherebbe abbandonare un turno che sta ancora per rispondere.
     */
    suspend fun awaitTerminal(
        turnId: Long,
        maxAttempts: Int = 200,
        fetch: suspend (Long) -> DjTurnDto,
        onUpdate: (DjTurnDto) -> Unit = {},
        sleep: suspend (Long) -> Unit = { delay(it) },
        delayForAttempt: (Int) -> Long = ::defaultDelay,
    ): DjTurnDto {
        var last: DjTurnDto? = null
        repeat(maxAttempts) { attempt ->
            val turn = fetch(turnId)
            last = turn
            onUpdate(turn)
            if (isTerminal(turn.status)) return turn
            sleep(delayForAttempt(attempt))
        }
        throw TimeoutException(last)
    }

    private fun defaultDelay(attempt: Int): Long = when (attempt) {
        0 -> 1_000L
        1 -> 2_000L
        2 -> 4_000L
        else -> 5_000L
    }
}
