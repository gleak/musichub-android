package com.mediaplayer.android.data

class FollowRepository(
    private val api: MediaPlayerApi = Network.api,
) {
    suspend fun list(): List<String> = api.listFollowedArtists()

    suspend fun follow(artist: String) = api.followArtist(artist)

    suspend fun unfollow(artist: String) = api.unfollowArtist(artist)

    /** Returns the lowercased subset of [artists] the current user follows. */
    suspend fun status(artists: List<String>): Set<String> =
        if (artists.isEmpty()) emptySet() else api.followStatus(artists)
}
