package com.mediaplayer.android.playback

import android.content.Context
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.mediaplayer.android.data.DownloadRepository
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.data.local.LocalMediaResolver

/**
 * "Phantom skip" self-heal, moved off the Activity-scoped ViewModel onto the
 * service's own player so it also fires on a headless drive (Android Auto cold
 * start / screen off) — which is exactly where truncated downloads bite and the
 * car "skips a song by itself".
 *
 * A song that AUTO-advances long before its known duration almost always means
 * the local/cached bytes are truncated (a background download killed mid-write,
 * a partial cache entry): ExoPlayer hits an early end-of-stream and moves on. We
 * log it (greppable tag) and drop the offending file's cached bytes so the next
 * play refetches clean ones — how far that goes is decided by
 * [PhantomSkipPolicy.remedyFor], because deleting an offline copy that cannot
 * be rebuilt on the current network would make things worse rather than better.
 * This never fires for a Bluetooth phantom
 * NEXT key — that arrives as a controller SEEK, not
 * [Player.DISCONTINUITY_REASON_AUTO_TRANSITION] — so the presence/absence of the
 * log still cleanly distinguishes the two causes.
 *
 * Single owner: the ViewModel no longer heals, so there is no double cache-evict
 * / double re-download when an Activity happens to be alive.
 */
@UnstableApi
internal class PhantomSkipHealer(
    private val context: Context,
    private val player: Player,
) : Player.Listener {

    /**
     * Freshest known duration of the currently-playing item. Refreshed in
     * [onEvents] (which fires *after* the per-callback notifications), so at an
     * auto-transition — [onPositionDiscontinuity] fires before [onEvents] moves
     * this to the new item — it still holds the *finishing* item's duration.
     * Reading it in [onEvents] rather than on STATE_READY also survives gapless
     * transitions, where the player never re-enters BUFFERING.
     */
    private var currentDurationMs: Long = 0L

    fun install() {
        player.addListener(this)
    }

    fun release() {
        player.removeListener(this)
    }

    override fun onEvents(player: Player, events: Player.Events) {
        val d = player.duration
        if (d != C.TIME_UNSET && d > 0L) currentDurationMs = d
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int,
    ) {
        val item = oldPosition.mediaItem
        val detected = PhantomSkipPolicy.detect(
            discontinuityReason = reason,
            knownDurationMs = currentDurationMs,
            endedAtMs = oldPosition.positionMs,
            mediaId = item?.mediaId,
        ) ?: return
        val songId = detected.songId
        Log.w(
            PHANTOM_SKIP_TAG,
            "Song $songId auto-advanced at ${detected.endedAtMs}ms of " +
                "${detected.durationMs}ms (${detected.earlyByMs}ms early) — " +
                "likely truncated bytes, healing.",
        )
        // Local files (negative id) have no backend copy to refetch; skip.
        if (songId <= 0L || LocalMediaResolver.isLocal(songId)) return
        heal(songId, item?.mediaMetadata?.title?.toString().orEmpty())
    }

    /**
     * Drop the streaming cache + offline copy for [songId] without touching the
     * currently-playing item, so a song that played back truncated gets fresh
     * bytes the next time it's selected. The bad song already advanced away, so
     * there's nothing to re-prepare here.
     */
    private fun heal(songId: Long, title: String) {
        val streamUrl = Network.streamUrl(songId)
        runCatching { PlayerCache.get(context).removeResource(streamUrl) }
        if (PhantomSkipPolicy.remedyFor(canRedownloadNow()) ==
            PhantomSkipRemedy.EVICT_STREAM_CACHE_ONLY
        ) {
            Log.w(
                PHANTOM_SKIP_TAG,
                "Song $songId: kept its offline copy — a replacement download " +
                    "can't run on the current network, and deleting it now " +
                    "would leave nothing to play.",
            )
            return
        }
        runCatching {
            val wasDownloaded = DownloadRepository.isDownloaded(songId)
            DownloadRepository.remove(songId)
            if (wasDownloaded) DownloadRepository.download(songId, title)
        }
    }

    /**
     * Whether the download manager's own requirements (unmetered network by
     * default) are satisfied at this instant. Asking Media3 rather than
     * inspecting connectivity ourselves keeps this honest if the requirements
     * are ever changed in [DownloadRoot] or by the download-over-wifi setting.
     */
    private fun canRedownloadNow(): Boolean = runCatching {
        val manager = DownloadRoot.getDownloadManager(context)
        manager.requirements.getNotMetRequirements(context) == 0
    }.getOrDefault(false)

    private companion object {
        const val PHANTOM_SKIP_TAG = "PlaybackPhantomSkip"
    }
}
