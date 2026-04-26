package com.mediaplayer.android.data

import com.mediaplayer.android.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Single entry point for every network-facing dependency. Intentionally kept
 * as a plain `object` — no DI framework yet; M6 can introduce Hilt if the
 * graph ever grows enough to warrant it.
 */
object Network {

    val baseUrl: String = BuildConfig.BASE_URL.ensureTrailingSlash()

    /** JSON configured to survive the occasional backend shape tweak. */
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    private const val API_KEY = "cf3ea1ea-f12a-4557-b926-1ac32a5ac4e2"

    val okHttp: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("X-Api-Key", API_KEY)
                    .build()
            )
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            // Debug builds: dump request/response bodies; release: silent.
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        })
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttp)
        .addConverterFactory(
            json.asConverterFactory("application/json".toMediaType())
        )
        .build()

    val api: MediaPlayerApi = retrofit.create(MediaPlayerApi::class.java)

    /** Backend URL for song cover art, used by Coil. */
    fun coverUrl(songId: Long): String = "${baseUrl}api/songs/$songId/cover"

    /** Backend URL for the audio stream, used by ExoPlayer in M5. */
    fun streamUrl(songId: Long): String = "${baseUrl}api/songs/$songId/stream"

    private fun String.ensureTrailingSlash(): String =
        if (endsWith("/")) this else "$this/"
}
