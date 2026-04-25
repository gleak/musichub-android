package com.mediaplayer.android.data

import com.mediaplayer.android.data.dto.AddSongRequest
import com.mediaplayer.android.data.dto.LyricLineDto
import com.mediaplayer.android.data.dto.RecordPlayRequest
import com.mediaplayer.android.data.dto.AlbumDetailDto
import com.mediaplayer.android.data.dto.AlbumDto
import com.mediaplayer.android.data.dto.ArtistDetailDto
import com.mediaplayer.android.data.dto.ArtistDto
import com.mediaplayer.android.data.dto.CreatePlaylistRequest
import com.mediaplayer.android.data.dto.CreateRequestBody
import com.mediaplayer.android.data.dto.PageResponse
import com.mediaplayer.android.data.dto.PlaylistDetailDto
import com.mediaplayer.android.data.dto.PlaylistDto
import com.mediaplayer.android.data.dto.RenamePlaylistRequest
import com.mediaplayer.android.data.dto.RequestDto
import com.mediaplayer.android.data.dto.RequestSummaryDto
import com.mediaplayer.android.data.dto.ReorderSongsRequest
import com.mediaplayer.android.data.dto.SelectCandidateBody
import com.mediaplayer.android.data.dto.SongDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit facade over the MediaPlayer backend.
 *
 * `/cover` and `/stream` are hit by URL (Coil for cover, ExoPlayer for
 * stream) rather than through a Retrofit call, so they don't live here.
 */
interface MediaPlayerApi {

    // ---------- Songs (M1/M4) ----------

    @GET("api/songs")
    suspend fun listSongs(
        @Query("q") query: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): PageResponse<SongDto>

    // ---------- Playlists (M6) ----------

    @GET("api/playlists")
    suspend fun listPlaylists(): List<PlaylistDto>

    @POST("api/playlists")
    suspend fun createPlaylist(@Body body: CreatePlaylistRequest): PlaylistDto

    @GET("api/playlists/{id}")
    suspend fun getPlaylist(@Path("id") id: Long): PlaylistDetailDto

    @PATCH("api/playlists/{id}")
    suspend fun renamePlaylist(
        @Path("id") id: Long,
        @Body body: RenamePlaylistRequest,
    ): PlaylistDto

    @DELETE("api/playlists/{id}")
    suspend fun deletePlaylist(@Path("id") id: Long)

    @POST("api/playlists/{id}/songs")
    suspend fun addSongToPlaylist(
        @Path("id") id: Long,
        @Body body: AddSongRequest,
    ): PlaylistDetailDto

    @DELETE("api/playlists/{id}/songs/{songId}")
    suspend fun removeSongFromPlaylist(
        @Path("id") id: Long,
        @Path("songId") songId: Long,
    ): PlaylistDetailDto

    @PUT("api/playlists/{id}/songs")
    suspend fun reorderPlaylistSongs(
        @Path("id") id: Long,
        @Body body: ReorderSongsRequest,
    ): PlaylistDetailDto

    // ---------- Find new music (M9) ----------

    /** Kicks off a Prowlarr search; server blocks briefly while the indexer answers. */
    @POST("api/requests")
    suspend fun createRequest(@Body body: CreateRequestBody): RequestDto

    @GET("api/requests")
    suspend fun listRequests(): List<RequestSummaryDto>

    @GET("api/requests/{id}")
    suspend fun getRequest(@Path("id") id: Long): RequestDto

    /** Hands off to the orchestrator — response is the request in UNLOCKING state. */
    @POST("api/requests/{id}/select")
    suspend fun selectCandidate(
        @Path("id") id: Long,
        @Body body: SelectCandidateBody,
    ): RequestDto

    @DELETE("api/requests/{id}")
    suspend fun deleteRequest(@Path("id") id: Long)

    // ---------- Liked Songs (M11a) ----------

    @GET("api/liked")
    suspend fun getLikedSongs(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): PageResponse<SongDto>

    @POST("api/liked/{songId}")
    suspend fun likeSong(@Path("songId") songId: Long)

    @DELETE("api/liked/{songId}")
    suspend fun unlikeSong(@Path("songId") songId: Long)

    @GET("api/liked/status")
    suspend fun getLikedStatus(@Query("ids") ids: List<Long>): List<Long>

    // ---------- Albums + Artists (M11b) ----------

    @GET("api/albums")
    suspend fun listAlbums(
        @Query("q") query: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): PageResponse<AlbumDto>

    @GET("api/albums/{name}")
    suspend fun getAlbum(
        @Path("name") name: String,
        @Query("artist") artist: String? = null,
    ): AlbumDetailDto

    @GET("api/artists")
    suspend fun listArtists(
        @Query("q") query: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): PageResponse<ArtistDto>

    @GET("api/artists/{name}")
    suspend fun getArtist(@Path("name") name: String): ArtistDetailDto

    // ---------- History (M11c) ----------

    @POST("api/history")
    suspend fun recordPlay(@Body body: RecordPlayRequest)

    @GET("api/history")
    suspend fun recentSongs(@Query("limit") limit: Int = 20): List<SongDto>

    // ---------- Lyrics (M12) ----------

    @GET("api/songs/{id}/lyrics")
    suspend fun getLyrics(@Path("id") id: Long): List<LyricLineDto>
}
