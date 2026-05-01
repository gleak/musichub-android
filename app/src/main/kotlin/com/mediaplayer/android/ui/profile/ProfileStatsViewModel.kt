package com.mediaplayer.android.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediaplayer.android.data.Network
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileStats(
    val songs: String = "—",
    val playlists: String = "—",
    val artists: String = "—",
    val loading: Boolean = true,
)

/**
 * Fetches the three per-user counts shown on the [ProfileScreen] header
 * via a single `GET /api/auth/stats` round-trip. Counts reflect the
 * user's own engagement: songs liked + in their playlists, playlists
 * they own, artists they follow. Failure falls back to em dashes —
 * stats are decorative, never block the UI.
 */
class ProfileStatsViewModel : ViewModel() {
    private val _state = MutableStateFlow(ProfileStats())
    val state: StateFlow<ProfileStats> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            val result = runCatching { Network.api.getStats() }.getOrNull()
            _state.value = ProfileStats(
                songs = result?.songs?.let(::format) ?: "—",
                playlists = result?.playlists?.let(::format) ?: "—",
                artists = result?.artists?.let(::format) ?: "—",
                loading = false,
            )
        }
    }

    private fun format(n: Long): String = when {
        n < 1_000 -> n.toString()
        n < 1_000_000 -> {
            val v = n / 1_000.0
            if (v % 1.0 == 0.0) "${v.toInt()}K" else "%.1fK".format(v)
        }
        else -> {
            val v = n / 1_000_000.0
            if (v % 1.0 == 0.0) "${v.toInt()}M" else "%.1fM".format(v)
        }
    }
}
