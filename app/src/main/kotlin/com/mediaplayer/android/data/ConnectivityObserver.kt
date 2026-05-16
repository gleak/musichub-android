package com.mediaplayer.android.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.mediaplayer.android.MediaPlayerApp
import java.util.concurrent.CopyOnWriteArraySet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide observer for "is the network usable right now?".
 *
 * Two signals are folded together:
 *  - [deviceOnline] — ConnectivityManager NetworkCallback. True while at least
 *    one INTERNET-capable network has fired `onAvailable` and not yet `onLost`.
 *  - [backendReachable] — flipped by the OkHttp interceptor in [Network] on
 *    every request: success → true, IOException/timeout → false. Lets us
 *    surface "no signal" when the device is on Wi-Fi but the backend is down.
 *
 * [networkAvailable] is the AND of both. UI uses it to show the offline icon;
 * actual playback of downloaded songs keeps working regardless because
 * ExoPlayer's download cache serves them locally.
 */
object ConnectivityObserver {

    // Start optimistic. A synchronous `cm.activeNetwork` probe at init time
    // can briefly return null during `Application.onCreate` even on a fully
    // connected device — flipping this to false would flash the offline icon
    // on every cold start. Trust the NetworkCallback to drive transitions.
    private val _deviceOnline = MutableStateFlow(true)
    val deviceOnline: StateFlow<Boolean> = _deviceOnline.asStateFlow()

    private val _backendReachable = MutableStateFlow(true)
    val backendReachable: StateFlow<Boolean> = _backendReachable.asStateFlow()

    private val _networkAvailable = MutableStateFlow(true)
    val networkAvailable: StateFlow<Boolean> = _networkAvailable.asStateFlow()

    private val available = CopyOnWriteArraySet<Network>()
    private var cb: ConnectivityManager.NetworkCallback? = null
    private var cmRef: ConnectivityManager? = null

    fun init() {
        if (cb != null) return
        val ctx = MediaPlayerApp.instance
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cmRef = cm
        // NET_CAPABILITY_INTERNET filter only — NET_CAPABILITY_VALIDATED is
        // too strict: captive-portal-free Wi-Fi, hotspots and corporate
        // networks routinely omit it even when the connection actually works.
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                available.add(network)
                setDeviceOnline(true)
            }
            override fun onLost(network: Network) {
                available.remove(network)
                setDeviceOnline(available.isNotEmpty())
            }
        }
        cm.registerNetworkCallback(request, callback)
        cb = callback
    }

    /**
     * Tear-down for tests. Production code never calls this — the singleton
     * lives as long as the process. Without it parallel-class test runs leak
     * one [ConnectivityManager.NetworkCallback] per re-init.
     */
    fun stop() {
        val cm = cmRef ?: return
        cb?.let { cm.unregisterNetworkCallback(it) }
        cb = null
        cmRef = null
        available.clear()
    }

    private fun setDeviceOnline(online: Boolean) {
        val wasOnline = _deviceOnline.value
        _deviceOnline.value = online
        // On offline→online transition optimistically clear the backend
        // flag — the next failing request will mark it unreachable again.
        // Without this the icon sticks: EventQueue parks on networkAvailable
        // so nothing actively probes the backend after reconnect.
        if (online && !wasOnline) {
            _backendReachable.value = true
        }
        recompute()
    }

    fun recordBackendSuccess() {
        if (!_backendReachable.value) {
            _backendReachable.value = true
            recompute()
        }
    }

    fun recordBackendFailure() {
        if (_backendReachable.value) {
            _backendReachable.value = false
            recompute()
        }
    }

    private fun recompute() {
        _networkAvailable.value = _deviceOnline.value && _backendReachable.value
    }
}
