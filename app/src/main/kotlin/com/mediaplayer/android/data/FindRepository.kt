package com.mediaplayer.android.data

import com.mediaplayer.android.data.dto.CreateRequestBody
import com.mediaplayer.android.data.dto.RequestDto
import com.mediaplayer.android.data.dto.RequestSummaryDto
import com.mediaplayer.android.data.dto.SelectCandidateBody

/**
 * Thin wrapper over [MediaPlayerApi]'s `/api/requests` endpoints. Keeps
 * the Retrofit interface out of the ViewModel so tests can swap in a
 * fake, matching the pattern used by [SongRepository] and
 * [PlaylistRepository].
 */
class FindRepository(
    private val api: MediaPlayerApi = Network.api,
) {

    /** Search Prowlarr via the backend and persist the request row. */
    suspend fun create(query: String): RequestDto =
        api.createRequest(CreateRequestBody(query.trim()))

    suspend fun list(): List<RequestSummaryDto> = api.listRequests()

    suspend fun detail(id: Long): RequestDto = api.getRequest(id)

    /**
     * Pick a candidate. Backend flips the request to UNLOCKING and the
     * scheduled orchestrator takes over; clients poll [detail] to watch
     * the state machine.
     */
    suspend fun select(requestId: Long, candidateId: Long): RequestDto =
        api.selectCandidate(requestId, SelectCandidateBody(candidateId))

    suspend fun delete(id: Long) {
        api.deleteRequest(id)
    }
}
