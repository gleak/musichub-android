package com.mediaplayer.android.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.mediaplayer.android.data.Network

/**
 * Tiny persistence layer so Android Auto's "resume where you left off"
 * chip has something to return on a cold car connect.
 *
 * Stores three things in a dedicated SharedPreferences file:
 * - the current queue as a comma-separated list of song ids,
 * - the active index within that queue,
 * - the playback position (ms) at the last checkpoint.
 *
 * It's SharedPreferences rather than DataStore / Room because the shape
 * is three primitives with no schema evolution in sight — adding a new
 * dependency (and a process-boundary initialiser in `Application`) for
 * this wouldn't earn its keep.
 *
 * Persistence fires on the [Player.Listener.onEvents] events that actually
 * change the resume target: timeline/media-item transitions and
 * playWhenReady toggles. That's cheap — [Player.getCurrentPosition] is a
 * plain property read — and avoids hot-looping.
 */
internal class PlaybackResumption(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * Attach to a [Player] so every meaningful change gets checkpointed.
     * Returns the listener so callers can detach it on service teardown.
     */
    fun install(player: Player): Player.Listener {
        val listener = object : Player.Listener {
            override fun onEvents(p: Player, events: Player.Events) {
                if (events.containsAny(
                        Player.EVENT_TIMELINE_CHANGED,
                        Player.EVENT_MEDIA_ITEM_TRANSITION,
                        Player.EVENT_PLAY_WHEN_READY_CHANGED,
                        Player.EVENT_IS_PLAYING_CHANGED,
                    )
                ) {
                    save(p)
                }
            }
        }
        player.addListener(listener)
        return listener
    }

    /**
     * Restore the last saved queue into playable [MediaItem]s (with
     * stream URIs) plus the start index + position. `null` when nothing
     * has been saved yet — the service returns an error result in that
     * case so Android Auto simply doesn't show a resume chip.
     */
    fun load(): Snapshot? {
        val csv = prefs.getString(KEY_QUEUE, null) ?: return null
        val ids = csv.split(",").mapNotNull { it.toLongOrNull() }
        if (ids.isEmpty()) return null
        val index = prefs.getInt(KEY_INDEX, 0).coerceIn(0, ids.lastIndex)
        val positionMs = prefs.getLong(KEY_POSITION_MS, 0L).coerceAtLeast(0L)
        val items = ids.map { id ->
            MediaItem.Builder()
                .setMediaId("song:$id")
                .setUri(Network.streamUrl(id))
                .build()
        }
        return Snapshot(items, index, positionMs)
    }

    private fun save(player: Player) {
        val count = player.mediaItemCount
        if (count == 0) {
            prefs.edit().clear().apply()
            return
        }
        val ids = buildList {
            for (i in 0 until count) {
                val id = player.getMediaItemAt(i).mediaId
                if (id.startsWith("song:")) add(id.removePrefix("song:"))
            }
        }
        prefs.edit()
            .putString(KEY_QUEUE, ids.joinToString(","))
            .putInt(KEY_INDEX, player.currentMediaItemIndex)
            .putLong(KEY_POSITION_MS, player.currentPosition)
            .apply()
    }

    data class Snapshot(
        val items: List<MediaItem>,
        val startIndex: Int,
        val startPositionMs: Long,
    )

    companion object {
        private const val PREFS = "mediaplayer_playback_resume"
        private const val KEY_QUEUE = "queue_song_ids"
        private const val KEY_INDEX = "queue_index"
        private const val KEY_POSITION_MS = "queue_position_ms"
    }
}
