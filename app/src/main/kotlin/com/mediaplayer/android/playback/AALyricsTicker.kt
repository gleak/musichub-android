package com.mediaplayer.android.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.mediaplayer.android.data.LyricsRepository
import com.mediaplayer.android.data.dto.LyricLineDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Drives the Android Auto now-playing card with the current synced lyric
 * line by writing it into [MediaMetadata.description]. AA disallows a
 * scrolling lyrics view (driving-unsafe), so we surface lyrics one line at
 * a time in the metadata that AA already renders.
 *
 * Strategy:
 *  - On [Player.Listener.onMediaItemTransition], reload lyrics for the new
 *    song id (in-process cache keyed by id avoids re-fetch on revisit).
 *  - While the player is playing, poll position once per second and resolve
 *    the active line via `indexOfLast { it.positionMs <= position }`.
 *  - When the active line index changes, swap the current MediaItem with
 *    one whose metadata's description = new line via [Player.replaceMediaItem]
 *    so playback state stays untouched and AA listeners repaint via
 *    `onMediaMetadataChanged`.
 *
 * Quiet failures:
 *  - song has no lyrics (404 / empty)            → description untouched
 *  - mediaId is not `song:{id}`                  → ticker idle
 *  - network throws                              → cached as empty list,
 *                                                  retried only on next song
 */
@UnstableApi
internal class AALyricsTicker(
    private val player: Player,
    private val scope: CoroutineScope,
    private val repository: LyricsRepository = LyricsRepository(),
) {

    private var listener: Player.Listener? = null
    private var tickerJob: Job? = null

    private val lyricCache = mutableMapOf<Long, List<LyricLineDto>>()

    @Volatile private var currentSongId: Long? = null
    @Volatile private var currentLines: List<LyricLineDto> = emptyList()
    @Volatile private var lastLineIndex: Int = -1
    @Volatile private var originalDescription: CharSequence? = null

    /**
     * Phone-only playback shouldn't pay for the AA card refresh — every
     * line transition triggers `replaceMediaItem` which fans out to every
     * `Player.Listener` (PrefetchOrchestrator, PlaybackResumption, the
     * activity VM). Off by default; service flips this on/off when an
     * AA / Automotive controller connects.
     */
    @Volatile private var aaConnected: Boolean = false

    fun install() {
        if (listener != null) return
        val l = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                onTrackChanged(mediaItem)
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying && aaConnected) startTicker() else stopTicker()
            }
        }
        listener = l
        player.addListener(l)
        // Ticker stays parked until an AA controller actually connects —
        // see [setAaConnected].
    }

    fun uninstall() {
        listener?.let { player.removeListener(it) }
        listener = null
        stopTicker()
    }

    /**
     * Toggle whether the ticker drives the now-playing card. Called by the
     * service from `onPostConnect` / `onDisconnected` when AA / Automotive
     * controllers attach or detach. While disconnected the ticker doesn't
     * fetch lyrics, doesn't poll position, and doesn't mutate the queue.
     */
    fun setAaConnected(connected: Boolean) {
        if (aaConnected == connected) return
        aaConnected = connected
        if (connected) {
            // Lazy-load lyrics for the current item so the first line shows
            // up without waiting for the next track transition.
            onTrackChanged(player.currentMediaItem)
            if (player.isPlaying) startTicker()
        } else {
            stopTicker()
            // Best-effort: clear any lyric we wrote so the AA card doesn't
            // show a stale line if it reconnects mid-song.
            if (lastLineIndex >= 0) {
                applyDescription(originalDescription?.toString())
                lastLineIndex = -1
            }
        }
    }

    private fun onTrackChanged(item: MediaItem?) {
        val id = item?.mediaId?.removePrefix("song:")?.toLongOrNull()
        currentSongId = id
        currentLines = emptyList()
        lastLineIndex = -1
        // Capture the pristine description from the queue's MediaItem so a
        // seek-before-first-line can restore it instead of leaving a stale
        // lyric line on the card.
        originalDescription = item?.mediaMetadata?.description
        if (id == null) return
        // Skip the lyrics fetch when AA isn't watching — the phone has its
        // own LyricsSheet that subscribes to position directly. Cache hits
        // still surface for the moment AA reconnects mid-song.
        if (!aaConnected) return
        val cached = lyricCache[id]
        if (cached != null) {
            currentLines = cached
            return
        }
        scope.launch {
            val lines = try {
                withContext(Dispatchers.IO) { repository.getLyrics(id) }
            } catch (_: Throwable) {
                emptyList()
            }
            // Guard against late returns after a fast skip.
            if (currentSongId == id) {
                lyricCache[id] = lines
                currentLines = lines
            }
        }
    }

    private fun startTicker() {
        if (!aaConnected) return
        if (tickerJob?.isActive == true) return
        tickerJob = scope.launch {
            while (true) {
                tickOnce()
                delay(POLL_MS)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun tickOnce() {
        val lines = currentLines
        if (lines.isEmpty()) return
        val pos = player.currentPosition
        val idx = lines.indexOfLast { it.positionMs <= pos }
        if (idx == lastLineIndex) return
        lastLineIndex = idx
        val text = if (idx >= 0) eyebrowed(lines[idx].text) else originalDescription?.toString()
        applyDescription(text)
    }

    private fun applyDescription(text: String?) {
        val item = player.currentMediaItem ?: return
        val newMetadata = MediaMetadata.Builder()
            .populate(item.mediaMetadata)
            .setDescription(text)
            .build()
        val newItem = item.buildUpon().setMediaMetadata(newMetadata).build()
        player.replaceMediaItem(player.currentMediaItemIndex, newItem)
    }

    /**
     * Wraps a raw lyric line so AA's description-line render reads as a
     * lyric (not a generic third-line caption). The `// ORA` prefix gives
     * provenance — closes audit `08-auto-extra.md` D8, partial mapping of
     * the mockup's accent-bordered lyric block (D6 chrome stays open
     * because Media3 has no styled-card primitive). Long lines are clipped
     * to [MAX_LINE_CHARS] with a trailing ellipsis so different head-unit
     * renderers don't wrap or truncate inconsistently (D7).
     */
    private fun eyebrowed(text: String): String {
        val collapsed = text.replace('\n', ' ').trim()
        val clipped = if (collapsed.length > MAX_LINE_CHARS) {
            collapsed.substring(0, MAX_LINE_CHARS - 1).trimEnd() + "…"
        } else {
            collapsed
        }
        return "// ORA · $clipped"
    }

    private companion object {
        const val POLL_MS = 1_000L

        /**
         * Single-line clip target for `MediaMetadata.description`. Mockup
         * specifies `whiteSpace: nowrap; overflow: hidden; textOverflow:
         * ellipsis` for the lyric block; Media3 has no truncation hint, so
         * we cap before AA sees it. ~60 chars matches the typical ~14-word
         * sung line and fits the AA card without wrapping on common heads.
         */
        const val MAX_LINE_CHARS = 60
    }
}
