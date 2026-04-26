package com.mediaplayer.android.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mirrors the backend's `RequestStatus` enum. Serialized as the enum
 * name (Spring's default Jackson config + `@Enumerated(STRING)` on the
 * entity), so the kotlinx.serialization defaults line up without a
 * custom serializer.
 */
@Serializable
enum class RequestStatus {
    SEARCHING,
    AWAITING_SELECTION,
    UNLOCKING,
    DOWNLOADING,
    IMPORTED,
    IMPORTED_PARTIAL,
    FAILED,
    CANCELED;

    /** True when the orchestrator will make no further transitions. */
    val isTerminal: Boolean
        get() = this == IMPORTED ||
            this == IMPORTED_PARTIAL ||
            this == FAILED ||
            this == CANCELED
}

/** Mirrors `CandidateDto` — one YouTube video search result. */
@Serializable
data class CandidateDto(
    val id: Long,
    val videoId: String,
    val title: String,
    val channelName: String? = null,
    val thumbnailUrl: String? = null,
    val durationSeconds: Int? = null,
    val viewCount: Long? = null,
)

/**
 * Full request payload (`GET /api/requests/{id}`, `POST /api/requests`,
 * `POST /api/requests/{id}/select`). `createdAt` / `updatedAt` are
 * ISO-8601 strings from the backend's `Instant` serializer — kept as
 * String because nothing in the UI parses them today.
 */
@Serializable
data class RequestDto(
    val id: Long,
    val query: String,
    val status: RequestStatus,
    val selectedCandidateId: Long? = null,
    val errorMessage: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val candidates: List<CandidateDto> = emptyList(),
)

/** Lean row for `GET /api/requests` — omits the candidate array. */
@Serializable
data class RequestSummaryDto(
    val id: Long,
    val query: String,
    val status: RequestStatus,
    val candidateCount: Int,
    val createdAt: String,
    val updatedAt: String,
)

/** Body for `POST /api/requests`. */
@Serializable
data class CreateRequestBody(
    @SerialName("query") val query: String,
)

/** Body for `POST /api/requests/{id}/select`. */
@Serializable
data class SelectCandidateBody(
    @SerialName("candidateId") val candidateId: Long,
)
