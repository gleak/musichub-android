package com.mediaplayer.android.data

import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.mediaplayer.android.MediaPlayerApp
import com.mediaplayer.android.playback.DownloadRoot
import com.mediaplayer.android.playback.MediaDownloadService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Process-scoped tracker for the explicit download cache (M13).
 *
 * [downloadedIds] reflects the set of song IDs whose downloads have
 * reached [Download.STATE_COMPLETED]. Updated reactively via
 * [DownloadManager.Listener]; the initial value is loaded from the
 * persistent [DownloadManager.downloadIndex] on [init].
 *
 * Call [init] once from [com.mediaplayer.android.MediaPlayerApp.onCreate]
 * before any screen is shown.
 */
@UnstableApi
object DownloadRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _downloadedIds = MutableStateFlow<Set<Long>>(emptySet())
    val downloadedIds: StateFlow<Set<Long>> = _downloadedIds.asStateFlow()

    private val listener = object : DownloadManager.Listener {
        override fun onDownloadChanged(
            dm: DownloadManager,
            download: Download,
            finalException: Exception?,
        ) = refreshAsync(dm)

        override fun onDownloadRemoved(dm: DownloadManager, download: Download) =
            refreshAsync(dm)
    }

    fun init() {
        val dm = DownloadRoot.getDownloadManager(MediaPlayerApp.instance)
        dm.addListener(listener)
        refreshAsync(dm)
    }

    fun download(songId: Long) {
        val uri = Uri.parse(Network.streamUrl(songId))
        val request = DownloadRequest.Builder(songId.toString(), uri).build()
        DownloadService.sendAddDownload(
            MediaPlayerApp.instance,
            MediaDownloadService::class.java,
            request,
            /* foreground = */ false,
        )
    }

    fun remove(songId: Long) {
        DownloadService.sendRemoveDownload(
            MediaPlayerApp.instance,
            MediaDownloadService::class.java,
            songId.toString(),
            /* foreground = */ false,
        )
    }

    fun downloadAll(songIds: List<Long>) = songIds.forEach { download(it) }

    fun removeAll(songIds: List<Long>) = songIds.forEach { remove(it) }

    fun isDownloaded(songId: Long): Boolean = songId in _downloadedIds.value

    private fun refreshAsync(dm: DownloadManager) {
        scope.launch {
            val completed = mutableSetOf<Long>()
            val cursor = dm.downloadIndex.getDownloads(Download.STATE_COMPLETED)
            try {
                while (cursor.moveToNext()) {
                    cursor.download.request.id.toLongOrNull()?.let { completed.add(it) }
                }
            } finally {
                cursor.close()
            }
            _downloadedIds.value = completed
        }
    }
}
