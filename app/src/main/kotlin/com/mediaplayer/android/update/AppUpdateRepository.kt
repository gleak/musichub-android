package com.mediaplayer.android.update

import com.mediaplayer.android.data.MediaPlayerApi
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.data.dto.AppUpdateDto

class AppUpdateRepository(
    private val injectedApi: MediaPlayerApi? = null,
) {
    /** Resolved per call so the client can be swapped after construction. */
    private val api: MediaPlayerApi get() = injectedApi ?: Network.api

    /**
     * Returns the published manifest if the server has one, or null when
     * the channel is disabled (server returns 204 No Content).
     */
    suspend fun latest(): AppUpdateDto? {
        val resp = api.latestAppUpdate()
        if (!resp.isSuccessful) return null
        return resp.body()
    }
}
