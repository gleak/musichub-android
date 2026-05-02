package com.mediaplayer.android.data

import com.mediaplayer.android.data.sync.EventQueue
import com.mediaplayer.android.data.sync.ReadCache
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import java.io.IOException

class FollowRepository(
    private val api: MediaPlayerApi = Network.api,
) {
    suspend fun list(): List<String> {
        return try {
            val fresh = api.listFollowedArtists()
            ReadCache.putJson(ReadCache.Keys.FOLLOW_LIST, fresh, ListSerializer(String.serializer()))
            fresh
        } catch (_: IOException) {
            ReadCache.getOrNull(ReadCache.Keys.FOLLOW_LIST, ListSerializer(String.serializer())) ?: emptyList()
        }
    }

    /** Queued — toggles dedupe per artist name (follow then unfollow cancels). */
    suspend fun follow(artist: String) = EventQueue.enqueueFollow(artist)

    suspend fun unfollow(artist: String) = EventQueue.enqueueUnfollow(artist)

    /** Returns the lowercased subset of [artists] the current user follows. */
    suspend fun status(artists: List<String>): Set<String> =
        if (artists.isEmpty()) emptySet() else api.followStatus(artists)
}
