package com.mediaplayer.android.data

import com.mediaplayer.android.data.dto.PageResponse
import com.mediaplayer.android.data.dto.SongDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit facade over the MediaPlayer backend.
 *
 * Milestone 4 only needs the catalog endpoint. `/cover` and `/stream` are
 * hit by URL (Coil for cover, ExoPlayer for stream) rather than through a
 * Retrofit call, so they don't live here.
 */
interface MediaPlayerApi {

    @GET("api/songs")
    suspend fun listSongs(
        @Query("q") query: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): PageResponse<SongDto>
}
