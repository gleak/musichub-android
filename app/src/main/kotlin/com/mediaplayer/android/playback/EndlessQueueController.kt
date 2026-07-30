package com.mediaplayer.android.playback

import android.util.Log
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi

/**
 * Owns the "endless queue" behaviour directly on the service's own player, so
 * it keeps working when no Activity — and therefore no [PlaybackViewModel] — is
 * alive. That is exactly the situation on an Android Auto cold start or a
 * screen-off drive, which is where car NEXT used to silently no-op at the end
 * of a queue: the old refill lived only in the Activity-scoped ViewModel, so a
 * head-unit skip that goes straight to the session player never triggered it.
 *
 * Responsibilities (single owner of queue *structure* under repeat-OFF):
 *  - **Refill**: when playback sits on the last item, append a fresh shuffled
 *    pass of the FULL original source so `hasNextMediaItem()` stays true.
 *  - **Prune**: trim already-played back-history beyond a size-scaled window so
 *    a long drive stays bounded while "previous" still reaches far back.
 *  - **User-queue consumption**: drop a "Play next"/"Add to queue" item the
 *    moment we leave it (Spotify contract).
 *
 * The refill pool is the full un-shuffled source, captured whenever a brand-new
 * set of songs appears (a fresh playlist, or a cold-start resume) — NOT rebuilt
 * from the live, already-pruned timeline. Rebuilding from the timeline is what
 * let the previous implementation collapse a 400-song playlist down to the ~21
 * songs that survived pruning, so the user "always heard the same songs".
 *
 * Reshuffle-on-wrap (repeat-ALL) intentionally stays in the ViewModel: it fires
 * only under repeat-ALL, where this engine does nothing, and it needs the
 * app-level shuffle flag (the player's own shuffle mode is forced off because
 * the app owns shuffle).
 *
 * All work runs on the player's application looper (its listener callbacks and
 * the [EndlessForwardingPlayer] overrides are all looper-confined), so every
 * timeline mutation here is synchronous and index-consistent.
 */
@UnstableApi
internal class EndlessQueueController(private val player: Player) : Player.Listener {

    /** Full, un-shuffled source pool. Fresh copies — never live timeline instances. */
    private var sourceItems: List<MediaItem> = emptyList()
    private var sourceIds: Set<String> = emptySet()

    /**
     * App-level shuffle state. Shuffle is owned here (not by the native
     * `Player.shuffleModeEnabled`, which is kept off) so it works headless
     * (Android Auto / screen off) and always reorders the WHOLE [sourceItems]
     * pool — not just the handful of items that happen to be ahead in the
     * (pruned) live timeline. Driven by the shared shuffle pref via
     * [MediaPlaybackService], which calls [applyShuffle] on the app looper.
     */
    @Volatile private var shuffleEnabled = false

    fun install() {
        player.addListener(this)
        captureSourceIfNew()
    }

    /** Current app-level shuffle state (mirrors the shared shuffle pref). */
    fun isShuffleEnabled(): Boolean = shuffleEnabled

    /**
     * Drop [songId] from the source pool so a refill/wrap never re-appends it.
     * Called after "brano sbagliato" (flagWrong) removes the item from the live
     * timeline — the timeline removal alone wouldn't stop the endless engine
     * from re-adding the flagged song on the next full-pool pass.
     */
    fun dropFromSource(songId: Long) {
        if (sourceItems.isEmpty()) return
        val target = songId.toString()
        val kept = sourceItems.filterNot {
            it.mediaId.removePrefix("song:").toLongOrNull()?.toString() == target
        }
        if (kept.size == sourceItems.size) return
        sourceItems = kept
        sourceIds = kept.mapTo(HashSet()) { it.mediaId }
    }

    /**
     * Adopt a new shuffle state and reorder the queue accordingly. Called on
     * the player's application looper by [MediaPlaybackService]'s shuffle-pref
     * collector, so every mutation here is synchronous and index-consistent.
     *
     *  - repeat-OFF: rebuild the tail from a fresh full-pool pass (shuffled or
     *    original order). This is what makes "shuffle" span the entire playlist.
     *  - repeat-ALL: reorder the items already ahead in place — the whole pool
     *    is already looping in the timeline, and this engine must not prune or
     *    refill under repeat-ALL (see [ensureEndlessTail]/[pruneHistory]).
     */
    fun applyShuffle(enabled: Boolean) {
        if (shuffleEnabled == enabled) return
        shuffleEnabled = enabled
        if (sourceItems.size < 2) return
        if (player.repeatMode == Player.REPEAT_MODE_ALL) reorderFutureInPlace()
        else rebuildTail()
    }

    fun release() {
        player.removeListener(this)
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        // Safety net for any timeline set that doesn't route through
        // [EndlessForwardingPlayer.setMediaItems] (e.g. a direct set on the
        // inner player): a fresh playlist introduces media IDs we've never
        // seen, so we recapture. Our own reshuffled refills and the ViewModel's
        // shuffle-toggle rearrange reuse KNOWN IDs, so they never recapture —
        // which is what keeps the pool from collapsing to the pruned window.
        // Authoritative (re)capture on a real replace happens in
        // [onSourceReplaced]; this only ever ADDS newly-seen songs to an empty
        // or superseded pool, never corrupts an existing one.
        captureSourceIfNew()
    }

    /**
     * Called by [EndlessForwardingPlayer] right after a genuine timeline
     * *replacement* (`setMediaItems` / `setMediaItem` — a new playlist, album,
     * single track, or a cold-start resume). A replace defines a brand-new
     * source pool regardless of how it overlaps the old one, so we drop the old
     * pool and recapture from the just-set timeline. This is what makes playing
     * a small playlist that happens to be a subset of a previously-played large
     * one refill from the small one, not the stale large pool.
     */
    fun onSourceReplaced() {
        sourceItems = emptyList()
        sourceIds = emptySet()
        captureSourceIfNew()
        // Starting a brand-new source while shuffle is on must shuffle the whole
        // thing (the freshly-set timeline arrives in original order). Under
        // repeat-ALL reorder in place; otherwise rebuild the tail from a fresh
        // full-pool shuffled pass.
        if (shuffleEnabled && sourceItems.size >= 2) {
            if (player.repeatMode == Player.REPEAT_MODE_ALL) reorderFutureInPlace()
            else rebuildTail()
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        // Auto-advance path (also covers headless drives): keep the tail full
        // and trim history so hasNext stays true and memory stays bounded.
        ensureEndlessTail()
        pruneHistory()
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int,
    ) {
        val oldIdx = oldPosition.mediaItemIndex
        val newIdx = newPosition.mediaItemIndex
        if (oldIdx == newIdx) return

        // Reshuffle-on-wrap (repeat-ALL, shuffle on): when the last item
        // auto-advances back to index 0, reshuffle everything after the (new)
        // current so each loop is a fresh order. Under repeat-ALL this engine
        // does not prune/refill, so nothing else mutates the timeline here — no
        // race. Owned here (not the ViewModel) so it also fires headless.
        if (shuffleEnabled &&
            player.repeatMode == Player.REPEAT_MODE_ALL &&
            reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION &&
            oldIdx == player.mediaItemCount - 1 && newIdx == 0
        ) {
            reorderFutureInPlace()
            return
        }

        // Consume a user-queued item the moment we leave it (auto-advance or
        // manual skip). Done here rather than in the ViewModel so it can't race
        // this engine's prune/refill — those shift indices, and a stale index
        // computed elsewhere would remove the wrong item.
        if (oldIdx < 0 || oldIdx >= player.mediaItemCount) return
        if (player.getMediaItemAt(oldIdx).isUserQueued()) {
            player.removeMediaItem(oldIdx)
        }
    }

    /**
     * If we're on the last item and repeat is OFF, append a fresh shuffled pass
     * of the full source. Safe to call repeatedly — no-ops unless actually at
     * the tail. Called from [onMediaItemTransition] (auto-advance) and from
     * [EndlessForwardingPlayer] before a manual NEXT, so a head-unit skip at the
     * tail advances into the refill instead of no-op-ing.
     */
    fun ensureEndlessTail() {
        if (player.repeatMode != Player.REPEAT_MODE_OFF) return
        val total = player.mediaItemCount
        if (total == 0) return
        if (player.currentMediaItemIndex != total - 1) return
        if (sourceItems.size < 2) return
        val currentId = player.getMediaItemAt(total - 1).mediaId
        player.addMediaItems(buildPass(currentId))
    }

    /**
     * A fresh full-pool pass to append/queue as the future, honouring the
     * app-level shuffle flag. Shuffle on → a random ordering of the whole pool;
     * shuffle off → the original order rotated to resume right after
     * [afterId] so the loop continues in sequence instead of restarting. Either
     * way the seam never immediately repeats the current song.
     */
    private fun buildPass(afterId: String?): List<MediaItem> {
        val ordered: List<MediaItem> = if (shuffleEnabled) {
            var s = sourceItems.shuffled()
            if (s.isNotEmpty() && s.first().mediaId == afterId) s = s.drop(1) + s.first()
            s
        } else {
            val idx = sourceItems.indexOfFirst { it.mediaId == afterId }
            if (idx >= 0) sourceItems.drop(idx + 1) + sourceItems.take(idx + 1)
            else sourceItems
        }
        return ordered.map { it.buildUpon().build() }
    }

    /**
     * repeat-OFF reorder: drop everything after the current item (past the
     * pinned user-queue block) and append a fresh full-pool pass. This is what
     * makes a shuffle toggle span the WHOLE playlist even after history has been
     * pruned down to a small window.
     */
    private fun rebuildTail() {
        val total = player.mediaItemCount
        if (total == 0) return
        val currentIdx = player.currentMediaItemIndex
        if (currentIdx < 0) return
        var keepEnd = currentIdx + 1
        while (keepEnd < total && player.getMediaItemAt(keepEnd).isUserQueued()) keepEnd++
        val currentId = player.getMediaItemAt(currentIdx).mediaId
        val future = buildPass(currentId)
        if (keepEnd < total) player.removeMediaItems(keepEnd, total)
        player.addMediaItems(keepEnd, future)
    }

    /**
     * repeat-ALL reorder: the whole pool is already looping in the timeline, so
     * reorder only the items still ahead (past the pinned user-queue block).
     * Shuffle on → random; shuffle off → restore original order for whatever's
     * still ahead. Items already played stay in place as history.
     */
    private fun reorderFutureInPlace() {
        val total = player.mediaItemCount
        val currentIdx = player.currentMediaItemIndex
        if (currentIdx < 0 || total <= currentIdx + 1) return
        var keepEnd = currentIdx + 1
        while (keepEnd < total && player.getMediaItemAt(keepEnd).isUserQueued()) keepEnd++
        if (keepEnd >= total) return
        val future = (keepEnd until total).map { player.getMediaItemAt(it) }
        val byId = future.associateBy { it.mediaId }
        val targetIds = if (shuffleEnabled) {
            future.map { it.mediaId }.shuffled()
        } else {
            sourceItems.map { it.mediaId }.filter { byId.containsKey(it) }
        }
        val items = targetIds.mapNotNull { byId[it]?.buildUpon()?.build() }
        if (items.isEmpty()) return
        player.removeMediaItems(keepEnd, total)
        player.addMediaItems(keepEnd, items)
    }

    /**
     * Trim played back-history beyond a window that scales with the playlist
     * size, so bigger playlists keep a longer reachable history (more variety,
     * "previous" reaches further) while small ones stay tight. Bounded on both
     * ends so a huge library can't grow the timeline without limit.
     *
     * Only runs under repeat-OFF; under repeat-ALL the player loops the whole
     * timeline and the ViewModel reshuffles it on wrap, so nothing is pruned.
     */
    private fun pruneHistory() {
        if (player.repeatMode != Player.REPEAT_MODE_OFF) return
        val pruneEnd = player.currentMediaItemIndex - historyKeep()
        if (pruneEnd > 0) player.removeMediaItems(0, pruneEnd)
    }

    /** History window scaled to the pool: a full pass stays reachable, clamped. */
    private fun historyKeep(): Int =
        sourceItems.size.coerceIn(MIN_HISTORY_KEEP, MAX_HISTORY_KEEP)

    private fun captureSourceIfNew() {
        val count = player.mediaItemCount
        if (count == 0) return
        val current = (0 until count)
            .map { player.getMediaItemAt(it) }
            .filterNot { it.isUserQueued() }
        if (current.isEmpty()) return
        val hasNew = current.any { it.mediaId !in sourceIds }
        if (!hasNew) return
        sourceItems = current.map { it.buildUpon().build() }
        sourceIds = sourceItems.mapTo(HashSet()) { it.mediaId }
        Log.i(TAG, "captured source pool of ${sourceItems.size} songs (history keep=${historyKeep()})")
    }

    private companion object {
        const val TAG = "EndlessQueue"
        // Never keep fewer than this behind the current item, so "previous"
        // still works even for tiny playlists.
        const val MIN_HISTORY_KEEP = 20
        // Upper bound so a several-thousand-song library can't grow the
        // timeline without limit on an all-day drive.
        const val MAX_HISTORY_KEEP = 500
    }
}

/**
 * Wraps the service's player so external transport skips (Android Auto,
 * Bluetooth/AVRCP, steering-wheel keys) refill the tail before advancing. These
 * skips reach the session player directly — they never call
 * [PlaybackViewModel.skipNext] — so without this a NEXT at the end of the queue
 * would just no-op. Refilling here (synchronously, on the application looper)
 * makes the queue endless for car controllers exactly like it is in-app.
 */
@UnstableApi
internal class EndlessForwardingPlayer(
    inner: Player,
    private val endless: EndlessQueueController,
) : ForwardingPlayer(inner) {

    override fun seekToNextMediaItem() {
        endless.ensureEndlessTail()
        super.seekToNextMediaItem()
    }

    override fun seekToNext() {
        endless.ensureEndlessTail()
        super.seekToNext()
    }

    // A genuine timeline replacement (new playlist / album / track, or a
    // cold-start resume) defines a brand-new endless-source pool. Catch every
    // overload — whichever a controller or the ViewModel happens to call — and
    // recapture authoritatively after the set has landed on the inner player.
    override fun setMediaItems(mediaItems: MutableList<MediaItem>) {
        super.setMediaItems(mediaItems)
        endless.onSourceReplaced()
    }

    override fun setMediaItems(mediaItems: MutableList<MediaItem>, resetPosition: Boolean) {
        super.setMediaItems(mediaItems, resetPosition)
        endless.onSourceReplaced()
    }

    override fun setMediaItems(
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ) {
        super.setMediaItems(mediaItems, startIndex, startPositionMs)
        endless.onSourceReplaced()
    }

    override fun setMediaItem(mediaItem: MediaItem) {
        super.setMediaItem(mediaItem)
        endless.onSourceReplaced()
    }

    override fun setMediaItem(mediaItem: MediaItem, resetPosition: Boolean) {
        super.setMediaItem(mediaItem, resetPosition)
        endless.onSourceReplaced()
    }

    override fun setMediaItem(mediaItem: MediaItem, startPositionMs: Long) {
        super.setMediaItem(mediaItem, startPositionMs)
        endless.onSourceReplaced()
    }
}
