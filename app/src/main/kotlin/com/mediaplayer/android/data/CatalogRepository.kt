package com.mediaplayer.android.data

class CatalogRepository(private val injectedApi: MediaPlayerApi? = null) {
    /** Resolved per call so the client can be swapped after construction. */
    private val api: MediaPlayerApi get() = injectedApi ?: Network.api

    suspend fun listAlbums(query: String? = null, page: Int = 0, size: Int = 20) =
        api.listAlbums(query, page, size)

    suspend fun getAlbum(name: String, artist: String? = null) =
        api.getAlbum(name, artist)

    suspend fun listArtists(query: String? = null, page: Int = 0, size: Int = 20) =
        api.listArtists(query, page, size)

    suspend fun getArtist(name: String) =
        api.getArtist(name)
}
