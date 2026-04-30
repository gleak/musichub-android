package com.mediaplayer.android.data

import com.mediaplayer.android.data.dto.AddSongRequest
import com.mediaplayer.android.data.dto.UserDto
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
import com.mediaplayer.android.data.dto.ReinitStatusDto
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.data.dto.SpotifyImportResultDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit facade over the MediaPlayer backend.
 *
 * `/cover` and `/stream` are hit by URL (Coil for cover, ExoPlayer for
 * stream) rather than through a Retrofit call, so they don't live here.
 */
interface MediaPlayerApi {

    // ---------- Auth ----------

    @GET("api/auth/me")
    suspend fun getMe(): UserDto

    // ---------- Songs (M1/M4) ----------

    @GET("api/songs")
    suspend fun listSongs(
        @Query("q") query: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): PageResponse<SongDto>

    /** Re-download a corrupted song from its YouTube source — id stays stable. */
    @POST("api/songs/{id}/redownload")
    suspend fun redownloadSong(@Path("id") id: Long): SongDto

    /** Download the MP4 video for a YouTube-sourced song that has no video yet. */
    @POST("api/songs/{id}/download-video")
    suspend fun downloadVideo(@Path("id") id: Long): SongDto

    /** Re-mux existing MP4 with -movflags faststart for instant seek support. Starts async job, returns 202. */
    @POST("api/songs/{id}/video/reinitialize")
    suspend fun reinitializeVideo(@Path("id") id: Long)

    /** Poll status of ongoing reinitialize job. Returns {status: RUNNING|DONE|ERROR, error: String}. */
    @GET("api/songs/{id}/video/reinitialize/status")
    suspend fun getReinitializeStatus(@Path("id") id: Long): ReinitStatusDto

    // ---------- Spotify import ----------

    @Multipart
    @POST("api/playlists/import/spotify")
    suspend fun importSpotifyPlaylist(
        @Part file: MultipartBody.Part,
        @Part("playlistName") playlistName: RequestBody,
    ): SpotifyImportResultDto

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
