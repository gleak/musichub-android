package com.mediaplayer.android.data.dto

import kotlinx.serialization.Serializable

/**
 * DTO della sezione DJ. Rispecchiano i record dei controller del backend:
 * DjRunController (che serializza direttamente l'entita' DjRun),
 * DjPreferencesController, DjSectionController e DjChatController.
 *
 * Ogni campo che non e' strettamente necessario ha un default, come il resto
 * dei DTO di questo package: `Json` di Network ha `ignoreUnknownKeys = true`,
 * quindi un backend piu' vecchio o piu' nuovo non fa esplodere il parsing.
 */

/**
 * Una passata del DJ. [status] vale RUNNING, OK, FAILED o PARTIAL: i
 * terminali sono TRE. Usare [com.mediaplayer.android.data.DjRunPolling.isTerminal],
 * mai un confronto scritto a mano.
 */
@Serializable
data class DjRunDto(
    val id: Long,
    val userId: Long = 0L,
    val startedAt: String,
    val finishedAt: String? = null,
    val status: String,
    val model: String? = null,
    val playlistsWritten: Int = 0,
    val playlistsRejected: Int = 0,
    val error: String? = null,
    /**
     * La playlist che il giro ha creato, quando ne ha creata una nuova.
     *
     * Nullo per il ciclo settimanale, che riscrive slot gia' esistenti invece
     * di crearne. Valorizzato dai giri nati dalla chat: e' l'unico modo che
     * l'app ha per portare la persona sulla playlist che ha appena chiesto,
     * invece di dirle "fatto" e lasciarla cercare.
     */
    val createdPlaylistId: Long? = null,
)

@Serializable
data class DjPreferencesDto(
    val cycleEnabled: Boolean = true,
    val slots: Int = 4,
    val cadenceDays: Int = 7,
    /** Falso quando i valori sopra sono i default globali e non una scelta. */
    val explicit: Boolean = false,
    val minSlots: Int = 1,
    val maxSlots: Int = 8,
    val minCadenceDays: Int = 1,
    val maxCadenceDays: Int = 30,
    /** Gia' risolti sul default globale quando la persona non ha scelto: mai un segnaposto. */
    val playlistMinSize: Int = 15,
    val playlistMaxSize: Int = 28,
    val minPlaylistSize: Int = 5,
    val maxPlaylistSize: Int = 50,
)

/**
 * Aggiornamento parziale: i campi nulli non vengono serializzati affatto
 * (`Json` di Network ha `explicitNulls = false`), quindi il server conserva
 * quelli non mandati.
 */
@Serializable
data class DjUpdatePreferencesRequest(
    val cycleEnabled: Boolean? = null,
    val slots: Int? = null,
    val cadenceDays: Int? = null,
    val playlistMinSize: Int? = null,
    val playlistMaxSize: Int? = null,
)

@Serializable
data class DjStatusDto(
    val agentAvailable: Boolean = false,
    val apiKeyConfigured: Boolean = false,
    val cycleEnabled: Boolean = false,
    val chatEnabled: Boolean = false,
    val runInProgress: Boolean = false,
    val cooldownSeconds: Long = 0L,
)

/** Cio' che il DJ crede del gusto di chi ascolta. Campi stabili, testo libero. */
@Serializable
data class DjTasteProfile(
    val moods: List<String> = emptyList(),
    val contexts: List<String> = emptyList(),
    val loves: List<String> = emptyList(),
    val avoids: List<String> = emptyList(),
    val artists: List<String> = emptyList(),
    val constraints: List<String> = emptyList(),
    val openQuestions: List<String> = emptyList(),
    val notes: String = "",
)

@Serializable
data class DjTasteProfileDto(
    val hasProfile: Boolean = false,
    val version: Int = 0,
    val sourceMessageCount: Int = 0,
    val updatedAt: String? = null,
    val profile: DjTasteProfile = DjTasteProfile(),
)

/**
 * Un turno della conversazione.
 *
 * [playlistName] e [playlistBrief] sono valorizzati solo sui turni in cui il
 * DJ e la persona hanno definito una scaletta concreta — quasi mai, ed e' il
 * caso normale. Quando ci sono, sotto il messaggio compare il pulsante che la
 * fa comporre davvero: in chat il DJ non ha il catalogo in mano, quindi puo'
 * decidere che playlist fare ma non farla.
 *
 * [id] serve proprio a quello: il pulsante si riferisce a QUEL messaggio, non
 * all'ultimo. La conversazione intanto va avanti.
 */
@Serializable
data class DjChatMessageDto(
    val role: String,
    val content: String,
    /** Vero quando il DJ ha rifiutato di rispondere perche' fuori tema. */
    val refused: Boolean = false,
    val createdAt: String,
    // In coda e non in testa: le chiamate posizionali che gia' esistono
    // (i test di schermata) continuano a dire cio' che intendevano.
    val id: Long? = null,
    val playlistName: String? = null,
    val playlistBrief: String? = null,
) {
    /** Vero quando da questo turno si puo' far comporre una playlist. */
    val hasPlaylistIntent: Boolean
        get() = id != null && !playlistName.isNullOrBlank()
}

/**
 * L'esito di un invio: il turno e' aperto, il DJ ci sta lavorando.
 *
 * La risposta NON e' qui. Arriva da [DjTurnDto], interrogando il turno: e'
 * il punto dell'intero cambio, perche' cosi' il testo esiste anche quando il
 * DJ ci mette piu' di quanto una connessione HTTP possa restare aperta.
 *
 * [userMessageId] permette di mostrare subito il messaggio appena scritto
 * senza ricaricare tutta la conversazione.
 */
@Serializable
data class DjTurnAcceptedDto(
    val turnId: Long,
    val userMessageId: Long? = null,
)

/**
 * Lo stato di un turno di chat.
 *
 * [status] e' RUNNING finche' c'e' da aspettare; ogni altro valore significa
 * "e' finita", compresi quelli che questa versione dell'app non conosce —
 * vedi [DjChatTurnPolling.isTerminal].
 *
 * [phase] dice a che punto e' il DJ mentre lavora ed e' null a turno
 * concluso. Il server manda un'etichetta (`CATALOG`), non una frase: il testo
 * italiano vive in [com.mediaplayer.android.ui.dj.phaseText], perche' e' nel
 * client che abita la lingua dell'interfaccia.
 */
@Serializable
data class DjTurnDto(
    val id: Long,
    val status: String,
    val phase: String? = null,
    val elapsedSeconds: Long = 0,
    /** L'id della riga del DJ: e' quello a cui si riferisce il pulsante della scaletta. */
    val messageId: Long? = null,
    val reply: String? = null,
    val offTopic: Boolean = false,
    val playlistName: String? = null,
    val playlistBrief: String? = null,
    /** Valorizzato solo quando [status] e' FAILED. */
    val error: String? = null,
)

@Serializable
data class DjSendMessageRequest(val message: String)

/** Il corpo che i controller del DJ mandano con 409, 429 e 503. */
@Serializable
data class DjErrorBody(
    val error: String? = null,
    val retryAfterSeconds: Long? = null,
)
