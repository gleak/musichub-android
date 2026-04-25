package com.mediaplayer.android.data

class CatalogRepository(private val api: MediaPlayerApi = Network.api) {
    suspend fun listAlbums(query: String? = null, page: Int = 0, size: Int = 20) =
        api.listAlbums(query, page, size)

    suspend fun getAlbum(name: String, artist: String? = null) =
        api.getAlbum(name, artist)

    suspend fun listArtists(query: String? = null, page: Int = 0, size: Int = 20) =
        api.listArtists(query, page, size)

    suspend fun getArtist(name: String) =
        api.getArtist(name)
}
