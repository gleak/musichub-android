package com.mediaplayer.android.playback

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network as AndroidNetwork
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.SimpleCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Keeps the previous and next queue neighbours warm in the disk cache
 * (M10).
 *
 * # Shape of the work
 * Every time the timeline or the current item changes, we recompute the
 * set of URIs we *want* warm (just prev + next for now) and diff it
 * against what is already downloading. Out-of-window jobs get cancelled;
 * new ones get a fresh [CacheWriter] on [Dispatchers.IO]. The cache
 * itself is the source of truth for "already warm" — `CacheWriter`
 * no-ops on byte ranges that are already resident, so starting a
 * job for a fully-cached track is cheap.
 *
 * # Network gate
 * Prefetch is bandwidth the user didn't explicitly ask for, so we only
 * run it when the active network is unmetered. Wi-Fi / Ethernet → go;
 * mobile data / metered hotspot → pause. A [ConnectivityManager.NetworkCallback]
 * flips the gate live so unplugging from Wi-Fi stops prefetch mid-file
 * (cancelling in-flight jobs) and plugging back in resumes from the
 * next transition. Actual playback is unaffected — the user opted
 * into that bandwidth.
 *
 * # Threading
 * [Player] methods must be invoked on `player.applicationLooper`.
 * `Player.Listener` callbacks already fire on that looper, but the
 * `NetworkCallback` fires on a system thread — so we bounce back to
 * the player's looper via a [Handler] before touching the queue.
 * CacheWriter itself runs on [Dispatchers.IO] and doesn't touch the
 * player at all.
 *
 * # Lifecycle
 * [install] hooks the listener + registers the network callback.
 * [release] unregisters, cancels every in-flight download, and detaches
 * the listener. It is safe to call multiple times.
 */
@UnstableApi
class PrefetchOrchestrator(
    context: Context,
    private val cache: SimpleCache,
    private val upstreamFactory: DataSource.Factory,
) : Player.Listener {

    private val appContext = context.applicationContext
    private val connectivity = appContext.getSystemService(ConnectivityManager::class.java)

    /**
     * SupervisorJob so one failed prefetch (e.g. 404 on a stale id)
     * doesn't cancel the rest of the window.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** URI → in-flight CacheWriter job. Keyed by the String URI so it matches the diff set. */
    private val jobs = ConcurrentHashMap<String, Job>()

    @Volatile private var allowed: Boolean = !connectivity.isActiveNetworkMetered

    private var player: Player? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var looperHandler: Handler? = null

    fun install(player: Player) {
        this.player = player
        this.looperHandler = Handler(player.applicationLooper)
        player.addListener(this)

        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: AndroidNetwork,
                caps: NetworkCapabilities,
            ) {
                val unmetered =
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                postToPlayerLooper {
                    if (unmetered != allowed) {
                        allowed = unmetered
                        if (!unmetered) cancelAll() else reschedule()
                    }
                }
            }

            override fun onLost(network: AndroidNetwork) {
                postToPlayerLooper {
                    allowed = !connectivity.isActiveNetworkMetered
                    if (!allowed) cancelAll()
                }
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivity.registerNetworkCallback(request, cb)
        networkCallback = cb

        // Kick once in case a queue is already loaded (e.g. after resumption).
        reschedule()
    }

    fun release() {
        networkCallback?.let {
            runCatching { connectivity.unregisterNetworkCallback(it) }
        }
        networkCallback = null
        player?.removeListener(this)
        player = null
        cancelAll()
        scope.cancel()
        looperHandler = null
    }

    // --- Player.Listener --------------------------------------------------

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        reschedule()
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        reschedule()
    }

    // --- Internals --------------------------------------------------------

    private fun postToPlayerLooper(block: () -> Unit) {
        looperHandler?.post(block) ?: block()
    }

    /**
     * Diff the desired window (prev + next URIs of the current item)
     * against in-flight jobs. Cancel what's no longer needed, start
     * what's missing. Must run on the player's looper.
     */
    private fun reschedule() {
        val p = player ?: return
        if (!allowed) {
            cancelAll()
            return
        }
        val desired = neighborUris(p).toSet()

        // Cancel out-of-window.
        val iter = jobs.entries.iterator()
        while (iter.hasNext()) {
            val (uri, job) = iter.next()
            if (uri !in desired) {
                job.cancel()
                iter.remove()
            }
        }

        // Start missing.
        for (uri in desired) {
            if (jobs.containsKey(uri)) continue
            jobs[uri] = startPrefetch(uri)
        }
    }

    private fun neighborUris(p: Player): List<String> {
        val count = p.mediaItemCount
        if (count <= 1) return emptyList()
        val idx = p.currentMediaItemIndex
        val out = ArrayList<String>(2)
        if (idx - 1 in 0 until count) {
            p.getMediaItemAt(idx - 1).localConfiguration?.uri?.toString()?.let(out::add)
        }
        if (idx + 1 in 0 until count) {
            p.getMediaItemAt(idx + 1).localConfiguration?.uri?.toString()?.let(out::add)
        }
        return out
    }

    private fun cancelAll() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
    }

    private fun startPrefetch(uri: String): Job = scope.launch {
        try {
            val dataSpec = DataSpec.Builder()
                .setUri(uri)
                .build()
            val cacheSource = CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(upstreamFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                .createDataSource()
            CacheWriter(cacheSource, dataSpec, /* temporaryBuffer = */ null, /* progressListener = */ null)
                .cache()
        } catch (t: Throwable) {
            // Prefetch is strictly best-effort: swallow IO, cancellation,
            // 404s on stale ids, etc. Nothing actionable to surface.
            if (t is kotlinx.coroutines.CancellationException) throw t
            Log.d(TAG, "prefetch failed for $uri: ${t.message}")
        } finally {
            // Clean up the job entry if it was removed via cancellation
            // (fast path) or finished successfully.
            jobs.remove(uri)
        }
    }

    private companion object {
        const val TAG = "PrefetchOrchestrator"
    }
}
