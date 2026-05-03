package com.mediaplayer.android.playback

import android.app.Notification
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.PlatformScheduler
import androidx.media3.exoplayer.scheduler.Scheduler
import com.mediaplayer.android.R

/**
 * Foreground service that drives explicit downloads via [DownloadRoot.getDownloadManager].
 *
 * Clients submit work via [DownloadService.sendAddDownload] /
 * [DownloadService.sendRemoveDownload] (see [com.mediaplayer.android.data.DownloadRepository]).
 * The service binds the singleton [DownloadManager] and shows a persistent
 * notification while downloads are in flight.
 *
 * Wires a [PlatformScheduler] (JobScheduler-backed, persisted across
 * reboots via [android.Manifest.permission.RECEIVE_BOOT_COMPLETED]) so
 * interrupted downloads resume automatically when the network requirement
 * is met again — without the user having to reopen the app.
 */
@UnstableApi
class MediaDownloadService : DownloadService(
    NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    CHANNEL_ID,
    R.string.download_channel_name,
    0,
) {

    override fun getDownloadManager(): DownloadManager =
        DownloadRoot.getDownloadManager(this)

    override fun getScheduler(): Scheduler? =
        PlatformScheduler(this, JOB_ID)

    override fun getForegroundNotification(
        downloads: MutableList<Download>,
        notMetRequirements: Int,
    ): Notification {
        val pending = downloads.filter {
            it.state == Download.STATE_DOWNLOADING || it.state == Download.STATE_QUEUED
        }
        val active = pending.size
        val labelNow = pending.firstOrNull { it.state == Download.STATE_DOWNLOADING }
            ?.request?.data
            ?.toString(Charsets.UTF_8)
            ?.takeIf { it.isNotBlank() }
        val text = when {
            active == 0 -> getString(R.string.download_finishing)
            labelNow != null && active == 1 ->
                getString(R.string.download_in_progress_one, labelNow)
            labelNow != null ->
                getString(R.string.download_in_progress_with_more, labelNow, active - 1)
            active > 1 ->
                getString(R.string.download_in_progress_count, active)
            else -> getString(R.string.download_in_progress)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(getString(R.string.download_channel_name))
            .setContentText(text)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 2
        const val CHANNEL_ID = "media_downloads"
        // PlatformScheduler job id — must be unique per app. Persisted in
        // JobScheduler under this id so a re-install replaces (rather
        // than duplicates) the existing entry.
        private const val JOB_ID = 9101
    }
}
