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

@Serializable
data class DjChatMessageDto(
    val role: String,
    val content: String,
    /** Vero quando il DJ ha rifiutato di rispondere perche' fuori tema. */
    val refused: Boolean = false,
    val createdAt: String,
)

@Serializable
data class DjChatReplyDto(
    val reply: String,
    val offTopic: Boolean = false,
)

@Serializable
data class DjSendMessageRequest(val message: String)

/** Il corpo che i controller del DJ mandano con 409, 429 e 503. */
@Serializable
data class DjErrorBody(
    val error: String? = null,
    val retryAfterSeconds: Long? = null,
)
