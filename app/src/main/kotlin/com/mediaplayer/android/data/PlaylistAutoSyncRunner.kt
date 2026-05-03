package com.mediaplayer.android.data

import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Cold-launch sync for playlists flagged with `autoSync = true`.
 *
 * For every flagged playlist the runner fetches its detail, diffs the song
 * ids against [DownloadRepository.downloadedIds], and enqueues
 * [DownloadRepository.download] for anything missing. The DownloadManager
 * itself respects the user's "Solo Wi-Fi" preference via the Requirements
 * wired in [DownloadRepository.init], so this layer doesn't gate by network.
 *
 * Failures are swallowed silently: a failing list/detail call (offline,
 * 5xx, auth not yet ready) just means we'll re-attempt on the next launch.
 */
@UnstableApi
object PlaylistAutoSyncRunner {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun run(repository: PlaylistRepository = PlaylistRepository()) {
        scope.launch {
            val playlists = runCatching { repository.list() }.getOrNull() ?: return@launch
            val flagged = playlists.filter { it.autoSync }
            for (p in flagged) {
                val detail = runCatching { repository.detail(p.id) }.getOrNull() ?: continue
                val missing = detail.songs
                    .map { it.song.id }
                    .filterNot { DownloadRepository.isDownloaded(it) }
                if (missing.isNotEmpty()) DownloadRepository.downloadAll(missing)
            }
        }
    }
}
