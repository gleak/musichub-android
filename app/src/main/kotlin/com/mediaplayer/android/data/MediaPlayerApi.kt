package com.mediaplayer.android.data

import com.mediaplayer.android.data.dto.AddSongRequest
import com.mediaplayer.android.data.dto.DailyMixRefreshDto
import com.mediaplayer.android.data.dto.AppVersionRequest
import com.mediaplayer.android.data.dto.UserDto
import com.mediaplayer.android.data.dto.LyricLineDto
import com.mediaplayer.android.data.dto.RecordPlayRequest
import com.mediaplayer.android.data.dto.AlbumDetailDto
import com.mediaplayer.android.data.dto.AlbumDto
import com.mediaplayer.android.data.dto.ArtistDetailDto
import com.mediaplayer.android.data.dto.ArtistDto
import com.mediaplayer.android.data.dto.CreatePlaylistRequest
import com.mediaplayer.android.data.dto.CreateRequestBody
import com.mediaplayer.android.data.dto.CutSongRequest
import com.mediaplayer.android.data.dto.GenreSeedRequest
import com.mediaplayer.android.data.dto.PageResponse
import com.mediaplayer.android.data.dto.PlaylistDetailDto
import com.mediaplayer.android.data.dto.PlaylistDto
import com.mediaplayer.android.data.dto.RenamePlaylistRequest
import com.mediaplayer.android.data.dto.ReplaceSongRequest
import com.mediaplayer.android.data.dto.ReplaceSongResponse
import com.mediaplayer.android.data.dto.RequestDto
import com.mediaplayer.android.data.dto.RequestSummaryDto
import com.mediaplayer.android.data.dto.ReorderSongsRequest
import com.mediaplayer.android.data.dto.SelectCandidateBody
import com.mediaplayer.android.data.dto.SetAutoSyncRequest
import com.mediaplayer.android.data.dto.ReinitStatusDto
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.data.dto.SpotifyImportJobIdDto
import com.mediaplayer.android.data.dto.SpotifyImportJobStatusDto
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

    @GET("api/auth/stats")
    suspend fun getStats(): com.mediaplayer.android.data.dto.StatsDto

    /** Phone-side telemetry — reports the installed versionName on cold launch. */
    @POST("api/auth/version")
    suspend fun reportAppVersion(@Body body: AppVersionRequest)

    // ---------- Taste / Onboarding (M14e) ----------

    /** Seeds GENRE rows in user_taste so the recommender's cold-start path has signal. */
    @POST("api/taste/genres")
    suspend fun seedGenres(@Body body: GenreSeedRequest)

    // ---------- Songs (M1/M4) ----------

    @GET("api/songs")
    suspend fun listSongs(
        @Query("q") query: String? = null,
        @Query("genre") genre: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): PageResponse<SongDto>

    /** Re-download a corrupted song from its YouTube source — id stays stable. */
    @POST("api/songs/{id}/redownload")
    suspend fun redownloadSong(@Path("id") id: Long): SongDto

    /**
     * Slice the source song into a new catalog row covering only the given
     * window. Server runs ffmpeg `-c copy`, so the cut snaps to the nearest
     * MP3 frame boundary (~26ms) without re-encoding. Returns the new master.
     */
    @POST("api/songs/{id}/cut")
    suspend fun cutSong(
        @Path("id") id: Long,
        @Body body: CutSongRequest,
    ): SongDto

    /**
     * Report a song as "wrong" (mismatched audio for the title/artist).
     * Backend hard-removes references from playlists/likes/history,
     * deletes the audio/cover/video files from disk, and keeps the row
     * as a tombstone so the YouTube importer refuses to re-download the
     * same content. Global, irreversible.
     */
    @POST("api/songs/{id}/flag-wrong")
    suspend fun flagSongWrong(@Path("id") id: Long)

    /**
     * Kick off async MP4 video download. Backend returns 202 immediately and
     * runs yt-dlp on a virtual thread; clients poll
     * {@link #getDownloadVideoStatus} until DONE/ERROR. Replaces the prior
     * blocking call which would time out client-side and spawn duplicates.
     */
    @POST("api/songs/{id}/download-video")
    suspend fun downloadVideo(@Path("id") id: Long)

    @GET("api/songs/{id}/download-video/status")
    suspend fun getDownloadVideoStatus(@Path("id") id: Long): ReinitStatusDto

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

    /**
     * Async variant — returns a jobId immediately; client polls
     * [getSpotifyImportJobStatus] for progress. Server is 202 Accepted.
     */
    @Multipart
    @POST("api/playlists/import/spotify/async")
    suspend fun importSpotifyPlaylistAsync(
        @Part file: MultipartBody.Part,
        @Part("playlistName") playlistName: RequestBody,
    ): SpotifyImportJobIdDto

    @GET("api/playlists/import/spotify/jobs/{jobId}")
    suspend fun getSpotifyImportJobStatus(
        @Path("jobId") jobId: String,
    ): SpotifyImportJobStatusDto

    // ---------- Playlists (M6) ----------

    @GET("api/playlists")
    suspend fun listPlaylists(
        @Query("kind") kind: String? = null,
    ): List<PlaylistDto>

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

    @PATCH("api/playlists/{id}/auto-sync")
    suspend fun setPlaylistAutoSync(
        @Path("id") id: Long,
        @Body body: SetAutoSyncRequest,
    ): PlaylistDto

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

    /**
     * Bulk swap: replace every reference of [ReplaceSongRequest.oldSongId] with
     * [ReplaceSongRequest.newSongId] across the caller's accessible playlists.
     * Used by the trim editor's "sostituirà l'originale nelle playlist?" CTA.
     */
    @POST("api/playlists/replace-song")
    suspend fun replaceSongInPlaylists(@Body body: ReplaceSongRequest): ReplaceSongResponse

    // ---------- Playlist share (M15a) ----------

    /** Recompute Daily Mix on demand. */
    @POST("api/playlists/auto/daily-mix/refresh")
    suspend fun refreshDailyMix(): DailyMixRefreshDto

    @POST("api/playlists/{id}/share")
    suspend fun createPlaylistShare(
        @Path("id") id: Long,
    ): com.mediaplayer.android.data.dto.ShareLinkDto

    @GET("api/playlists/share/{token}")
    suspend fun previewPlaylistShare(
        @Path("token") token: String,
    ): com.mediaplayer.android.data.dto.SharePreviewDto

    @POST("api/playlists/share/{token}/accept")
    suspend fun acceptPlaylistShare(
        @Path("token") token: String,
    ): PlaylistDetailDto

    @DELETE("api/playlists/{id}/share")
    suspend fun revokePlaylistShares(
        @Path("id") id: Long,
    ): retrofit2.Response<Unit>

    @GET("api/playlists/{id}/members")
    suspend fun listPlaylistMembers(
        @Path("id") id: Long,
    ): List<com.mediaplayer.android.data.dto.PlaylistMemberDto>

    @DELETE("api/playlists/{id}/members/{userId}")
    suspend fun kickPlaylistMember(
        @Path("id") id: Long,
        @Path("userId") userId: Long,
    ): retrofit2.Response<Unit>

    // ---------- Followed artists / Release Radar (M15b) ----------

    @GET("api/follow")
    suspend fun listFollowedArtists(): List<String>

    @POST("api/follow/{artist}")
    suspend fun followArtist(@Path("artist") artist: String)

    @DELETE("api/follow/{artist}")
    suspend fun unfollowArtist(@Path("artist") artist: String)

    @GET("api/follow/status")
    suspend fun followStatus(
        @Query("artists") artists: List<String>,
    ): Set<String>

    // ---------- Self-hosted update channel (M16) ----------

    @GET("api/updates/latest")
    suspend fun latestAppUpdate(): retrofit2.Response<com.mediaplayer.android.data.dto.AppUpdateDto>

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

    // ---------- Dislikes ----------

    @GET("api/dislikes/songs")
    suspend fun getDislikedSongs(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): PageResponse<SongDto>

    @POST("api/dislikes/songs/{songId}")
    suspend fun dislikeSong(@Path("songId") songId: Long)

    @DELETE("api/dislikes/songs/{songId}")
    suspend fun undislikeSong(@Path("songId") songId: Long)

    @GET("api/dislikes/songs/status")
    suspend fun getDislikedSongStatus(@Query("ids") ids: List<Long>): List<Long>

    @GET("api/dislikes/artists")
    suspend fun getDislikedArtists(): List<String>

    @POST("api/dislikes/artists/{artist}")
    suspend fun dislikeArtist(@Path("artist") artist: String)

    @DELETE("api/dislikes/artists/{artist}")
    suspend fun undislikeArtist(@Path("artist") artist: String)

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

    /**
     * On-demand lyric fetch for a single song. The backend no longer
     * auto-downloads lyrics — the user has to ask for them via this call.
     * Returns the imported lines on success; 404 if nothing was found.
     */
    @POST("api/songs/{id}/lyrics/import")
    suspend fun importLyrics(@Path("id") id: Long): List<LyricLineDto>
}
