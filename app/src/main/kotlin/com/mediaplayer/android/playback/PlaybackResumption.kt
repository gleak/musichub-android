package com.mediaplayer.android.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.mediaplayer.android.data.Network
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
     *
     * The trigger set deliberately excludes [Player.EVENT_IS_PLAYING_CHANGED]:
     * play/pause toggles don't move the resume target, and the AA lyrics
     * ticker's once-per-second `replaceMediaItem` fans EVENT_TIMELINE_CHANGED
     * out to every other listener — including this one — so leaving
     * IS_PLAYING_CHANGED in the trigger set on top of the per-line timeline
     * churn meant a SharedPreferences JSON-encode + write per lyric line.
     * The in-memory snapshot signature below additionally skips timeline-
     * changed writes when the queue identity hasn't actually changed
     * (lyric-line in-place updates pass through cheaply).
     */
    fun install(player: Player): Player.Listener {
        val listener = object : Player.Listener {
            override fun onEvents(p: Player, events: Player.Events) {
                if (events.containsAny(
                        Player.EVENT_TIMELINE_CHANGED,
                        Player.EVENT_MEDIA_ITEM_TRANSITION,
                        Player.EVENT_PLAY_WHEN_READY_CHANGED,
                    )
                ) {
                    save(p)
                }
            }
        }
        player.addListener(listener)
        return listener
    }

    // Last-saved queue signature so an in-place replaceMediaItem on the
    // current index (AA lyrics ticker, redownloadCurrent, refreshLocalDownload)
    // doesn't trigger another disk write — the resume target is identical
    // when the id list + current index haven't changed.
    private var lastSig: String? = null

    /**
     * Restore the last saved queue into playable [MediaItem]s (with
     * stream URIs) plus the start index + position. `null` when nothing
     * has been saved yet — the service returns an error result in that
     * case so Android Auto simply doesn't show a resume chip.
     */
    fun load(): Snapshot? {
        val json = prefs.getString(KEY_SNAPSHOT, null) ?: return null
        return try {
            val dto = Json.decodeFromString<SnapshotDto>(json)
            val items = dto.items.map { s ->
                MediaItem.Builder()
                    .setMediaId("song:${s.id}")
                    .setUri(Network.streamUrl(s.id))
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(s.title)
                            .setArtist(s.artist)
                            .setArtworkUri(CoverContentProvider.uriFor(s.id))
                            .setIsBrowsable(false)
                            .setIsPlayable(true)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                            .build()
                    )
                    .build()
            }
            Snapshot(items, dto.index, dto.positionMs)
        } catch (e: Exception) {
            null
        }
    }

    private fun save(player: Player) {
        val count = player.mediaItemCount
        if (count == 0) {
            prefs.edit().clear().apply()
            lastSig = null
            return
        }

        val items = mutableListOf<SongSnapshotDto>()
        for (i in 0 until count) {
            val item = player.getMediaItemAt(i)
            val id = item.mediaId.removePrefix("song:").toLongOrNull() ?: continue
            items.add(
                SongSnapshotDto(
                    id = id,
                    title = item.mediaMetadata.title?.toString() ?: "Sconosciuto",
                    artist = item.mediaMetadata.artist?.toString() ?: "Sconosciuto",
                )
            )
        }

        val index = player.currentMediaItemIndex
        // Signature: queue ids + current index. Excludes position because
        // position drifts continuously without firing Player.Events — only
        // the events in install() trigger a save, so the position captured
        // here is always the position at that event boundary.
        val sig = buildString {
            for (s in items) append(s.id).append(',')
            append('@').append(index)
        }
        if (sig == lastSig) return
        lastSig = sig

        val dto = SnapshotDto(
            items = items,
            index = index,
            positionMs = player.currentPosition,
        )

        prefs.edit()
            .putString(KEY_SNAPSHOT, Json.encodeToString(dto))
            .apply()
    }

    @Serializable
    private data class SongSnapshotDto(
        val id: Long,
        val title: String,
        val artist: String,
    )

    @Serializable
    private data class SnapshotDto(
        val items: List<SongSnapshotDto>,
        val index: Int,
        val positionMs: Long,
    )

    data class Snapshot(
        val items: List<MediaItem>,
        val startIndex: Int,
        val startPositionMs: Long,
    )

    companion object {
        private const val PREFS = "mediaplayer_playback_resume"
        private const val KEY_SNAPSHOT = "playback_snapshot_v2"
    }
}
