package com.mediaplayer.android.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.mediaplayer.android.MediaPlayerApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide observer for "is the network usable right now?".
 *
 * Two signals are folded together:
 *  - [deviceOnline] — ConnectivityManager NetworkCallback. True when at least
 *    one network reports `NET_CAPABILITY_INTERNET + NET_CAPABILITY_VALIDATED`.
 *  - [backendReachable] — flipped by the OkHttp interceptor in [Network] on
 *    every request: success → true, IOException/timeout → false. Lets us
 *    surface "no signal" when the device is on Wi-Fi but the backend is down.
 *
 * [networkAvailable] is the AND of both. UI uses it to show the offline icon;
 * actual playback of downloaded songs keeps working regardless because
 * ExoPlayer's download cache serves them locally.
 */
object ConnectivityObserver {

    private val _deviceOnline = MutableStateFlow(true)
    val deviceOnline: StateFlow<Boolean> = _deviceOnline.asStateFlow()

    private val _backendReachable = MutableStateFlow(true)
    val backendReachable: StateFlow<Boolean> = _backendReachable.asStateFlow()

    private val _networkAvailable = MutableStateFlow(true)
    val networkAvailable: StateFlow<Boolean> = _networkAvailable.asStateFlow()

    private var registered = false

    fun init() {
        if (registered) return
        registered = true
        val ctx = MediaPlayerApp.instance
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = updateFromCm(cm)
            override fun onLost(network: Network) = updateFromCm(cm)
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) =
                updateFromCm(cm)
        }
        cm.registerNetworkCallback(request, cb)
        updateFromCm(cm)
    }

    private fun updateFromCm(cm: ConnectivityManager) {
        val active = cm.activeNetwork
        val caps = active?.let(cm::getNetworkCapabilities)
        // INTERNET capability only — NET_CAPABILITY_VALIDATED is too strict:
        // captive-portal-free Wi-Fi, hotspots and corporate networks routinely
        // omit it even when the connection actually works.
        val online = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        _deviceOnline.value = online
        if (!online) {
            // Device fully offline — backend can't be reachable until a call
            // proves otherwise. Proactively flip the flag so the badge appears
            // immediately rather than waiting for the next failed request.
            _backendReachable.value = false
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

    // The OkHttp interceptor is the authoritative signal — if a request just
    // succeeded, the network is by definition available, regardless of what
    // ConnectivityManager reports. CM is downgraded to a hint that lets us
    // surface offline state quickly when the device leaves all networks.
    private fun recompute() {
        _networkAvailable.value = _backendReachable.value
    }
}
