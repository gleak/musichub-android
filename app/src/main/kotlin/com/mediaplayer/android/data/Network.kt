package com.mediaplayer.android.data

import com.mediaplayer.android.BuildConfig
import com.mediaplayer.android.MediaPlayerApp
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

object AuthTokenHolder {
    @Volatile var idToken: String? = null
    @Volatile var anonymousId: String? = null
    // Resolved off main during MediaPlayerApp.onCreate; the OkHttp interceptor
    // awaits this on the OkHttp dispatcher thread when [anonymousId] is still
    // unset for the very first request after cold start. Subsequent requests
    // see [anonymousId] populated and skip the await entirely.
    @Volatile var anonymousIdDeferred: Deferred<String>? = null
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

    // Single host (one backend) handles every API call, every cover image,
    // every audio stream, every prefetch range, every telemetry POST.
    // OkHttp's default max-per-host of 5 serialises requests when scrolling
    // a cover-heavy grid + a parallel API call + prefetch is in flight;
    // bump to 16 since the backend is HTTP/1.1 (one request per connection).
    private val dispatcher = Dispatcher().apply {
        maxRequestsPerHost = 16
    }

    // Keep a slightly larger idle pool so cover-grid scroll doesn't churn
    // TCP handshakes between visible-window swaps. 5 min keepalive matches
    // the OkHttp default; only the count is bumped.
    private val connectionPool = ConnectionPool(
        maxIdleConnections = 10,
        keepAliveDuration = 5,
        timeUnit = TimeUnit.MINUTES,
    )

    // 50 MB on-disk response cache. Empty until the backend starts emitting
    // Cache-Control + ETag headers (Phase E); once it does, cover thumbs and
    // catalog list bodies hit the 304 path and skip the body re-download.
    private val responseCache: Cache by lazy {
        Cache(File(MediaPlayerApp.instance.cacheDir, "http"), 50L * 1024 * 1024)
    }

    val okHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .dispatcher(dispatcher)
            .connectionPool(connectionPool)
            .cache(responseCache)
            .addInterceptor { chain ->
                val token = AuthTokenHolder.idToken
                val anonId = AuthTokenHolder.anonymousId ?: run {
                    // Cold-start race: the resolver coroutine hasn't completed yet.
                    // Block this OkHttp dispatcher thread (never main) until it does;
                    // a completed Deferred returns instantly thereafter.
                    AuthTokenHolder.anonymousIdDeferred?.let { d ->
                        runBlocking { d.await() }
                    }
                }
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
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttp)
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType())
            )
            .build()
    }

    val api: MediaPlayerApi by lazy { retrofit.create(MediaPlayerApi::class.java) }

    fun coverUrl(songId: Long): String = "${baseUrl}api/songs/$songId/cover"
    fun streamUrl(songId: Long): String = "${baseUrl}api/songs/$songId/stream"
    fun videoStreamUrl(songId: Long): String = "${baseUrl}api/songs/$songId/stream/video"

    private fun String.ensureTrailingSlash(): String =
        if (endsWith("/")) this else "$this/"
}
