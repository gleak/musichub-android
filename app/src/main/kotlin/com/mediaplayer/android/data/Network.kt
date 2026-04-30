package com.mediaplayer.android.data

import com.mediaplayer.android.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object AuthTokenHolder {
    @Volatile var idToken: String? = null
    @Volatile var anonymousId: String? = null
}

object Network {

    val baseUrl: String = BuildConfig.BASE_URL.ensureTrailingSlash()

    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    // Client identification key — always sent so the backend can gate by allowed client.
    // User identity is layered on top via Bearer (signed-in) or X-Anonymous-Id (anon device).
    const val API_KEY = "cf3ea1ea-f12a-4557-b926-1ac32a5ac4e2"

    val okHttp: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val token = AuthTokenHolder.idToken
            val anonId = AuthTokenHolder.anonymousId
            val req = chain.request().newBuilder().apply {
                header("X-Api-Key", API_KEY)
                if (token != null) {
                    header("Authorization", "Bearer $token")
                } else if (anonId != null) {
                    header("X-Anonymous-Id", anonId)
                }
            }.build()
            try {
                val resp = chain.proceed(req)
                ConnectivityObserver.recordBackendSuccess()
                resp
            } catch (e: java.io.IOException) {
                ConnectivityObserver.recordBackendFailure()
                throw e
            }
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
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

    fun coverUrl(songId: Long): String = "${baseUrl}api/songs/$songId/cover"
    fun streamUrl(songId: Long): String = "${baseUrl}api/songs/$songId/stream"
    fun videoStreamUrl(songId: Long): String = "${baseUrl}api/songs/$songId/stream/video"

    private fun String.ensureTrailingSlash(): String =
        if (endsWith("/")) this else "$this/"
}
