package com.mediaplayer.android.playback

import android.app.Application
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper.shadowMainLooper

/**
 * Behavioural tests for [EndlessQueueController], driven by a real
 * [ExoPlayer] so that the ordering between `onMediaItemTransition` and
 * `onPositionDiscontinuity` is Media3's own rather than a fake's.
 *
 * Seeking rather than playing is deliberate: playback would need real media,
 * and the controller branches on neither discontinuity reason nor transition
 * reason — a seek drives exactly the same two callbacks in the same order as
 * an auto-advance.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@UnstableApi
class EndlessQueueControllerTest {

    private lateinit var player: ExoPlayer
    private lateinit var endless: EndlessQueueController

    @Before
    fun setUp() {
        player = ExoPlayer.Builder(ApplicationProvider.getApplicationContext()).build()
        endless = EndlessQueueController(player)
        endless.install()
    }

    @After
    fun tearDown() {
        endless.release()
        player.release()
        shadowMainLooper().idle()
    }

    @Test
    fun `landing on the last item appends a fresh pass`() {
        setQueue(pool(POOL))
        val before = player.mediaItemCount

        seekToItem(before - 1)

        assertTrue(
            "expected a refill past $before items, timeline is ${player.mediaItemCount}",
            player.mediaItemCount > before,
        )
    }

    /**
     * The precondition behind the Android Auto "skip does nothing" report: a
     * one-item timeline yields a one-item source pool, `ensureEndlessTail`
     * bails on `sourceItems.size < 2`, and the queue can never grow a next
     * item on its own. Guards the fix that stopped a bare `song:` leaf from
     * producing such a timeline.
     */
    @Test
    fun `a single-item pool never refills`() {
        setQueue(pool(1))

        seekToItem(0)

        assertEquals(1, player.mediaItemCount)
    }

    /** Baseline: leaving a user-queued item consumes it, no pruning involved. */
    @Test
    fun `user-queued item is consumed when left`() {
        setQueue(pool(POOL))
        seekToItem(0)
        insertUserQueued(at = 1)
        assertEquals("uq", player.getMediaItemAt(1).mediaId)

        seekToItem(1)
        seekToItem(2)

        assertNull("user-queued item outlived the skip", findItem("uq"))
    }

    /**
     * The suspected defect: `onMediaItemTransition` prunes played history with
     * `removeMediaItems(0, pruneEnd)`, shifting every index down, while
     * `onPositionDiscontinuity` removes the departed user-queued item using
     * the index captured *before* that shift. If the two ever collided, the
     * stale index would address the wrong item — potentially the one now
     * playing.
     *
     * Leaving the user-queued item by several positions rather than one is
     * what makes both removals land in the same event batch. Skipping by one
     * cannot collide: the user-queue removal drops the play index back to the
     * history window, so pruning finds nothing left to trim.
     */
    @Test
    fun `user-queued item is consumed while history pruning is active`() {
        setQueue(pool(POOL))
        // Land on the last item so the engine appends a second pass, giving us
        // a timeline long enough that pruning kicks in.
        seekToItem(POOL - 1)
        val refilled = player.mediaItemCount
        assertTrue("refill did not happen, got $refilled items", refilled > POOL)

        // Push past the history window once so pruning is live from here on.
        seekToItem(POOL + 5)
        val settled = player.currentMediaItemIndex

        insertUserQueued(at = settled + 1)
        seekToItem(settled + 1)
        assertEquals("uq", player.currentMediaItem?.mediaId)

        val from = player.currentMediaItemIndex
        val expectedNext = player.getMediaItemAt(from + JUMP).mediaId
        val countBefore = player.mediaItemCount
        seekToItem(from + JUMP)

        // Guards against a vacuous pass: the user-queue removal alone accounts
        // for exactly one item, so anything less than two means pruning never
        // ran and the collision this test exists for never occurred.
        assertTrue(
            "history pruning did not run — the scenario under test never " +
                "occurred (timeline went from $countBefore to ${player.mediaItemCount})",
            player.mediaItemCount <= countBefore - 2,
        )
        assertNull("user-queued item outlived the skip", findItem("uq"))
        assertEquals(
            "playback landed on the wrong item — an index shifted under the removal",
            expectedNext,
            player.currentMediaItem?.mediaId,
        )
    }

    /**
     * The refill rotates the pool so it resumes after the finishing song
     * instead of restarting at the top — otherwise every loop seam would replay
     * the song that just ended.
     */
    @Test
    fun `refill does not immediately repeat the song it followed`() {
        setQueue(pool(POOL))

        seekToItem(POOL - 1)
        val seam = player.currentMediaItemIndex

        assertEquals("$POOL", player.getMediaItemAt(seam).mediaId)
        assertNotEquals(
            "the appended pass restarted on the song that was just playing",
            "$POOL",
            player.getMediaItemAt(seam + 1).mediaId,
        )
    }

    /**
     * Under repeat-ALL the player already loops the whole timeline, so the
     * engine must not trim history — pruning would eat the very items the loop
     * is going to come back to.
     */
    @Test
    fun `repeat all suppresses history pruning`() {
        setQueue(pool(POOL))
        seekToItem(POOL - 1)
        player.repeatMode = Player.REPEAT_MODE_ALL
        shadowMainLooper().idle()
        val before = player.mediaItemCount

        seekToItem(POOL + 5)

        assertEquals("timeline was trimmed under repeat-ALL", before, player.mediaItemCount)
    }

    /** Under repeat-ALL the engine must not append either — the loop is the tail. */
    @Test
    fun `repeat all suppresses tail refill`() {
        setQueue(pool(POOL))
        player.repeatMode = Player.REPEAT_MODE_ALL
        shadowMainLooper().idle()

        seekToItem(POOL - 1)

        assertEquals(POOL, player.mediaItemCount)
    }

    /**
     * Backs the "brano sbagliato" flow: a song dropped from the pool must not
     * come back on the next refill, or the user would be handed the track they
     * just reported over and over.
     *
     * The timeline removal is not incidental setup — it is load-bearing.
     * `captureSourceIfNew` rebuilds the pool from the live timeline whenever it
     * spots a media id the pool doesn't have, so a song dropped from the pool
     * while still sitting in the timeline is resurrected by the very next
     * timeline change. The real caller removes every matching item first, and
     * the drop only sticks because of that ordering.
     */
    @Test
    fun `a dropped song is never re-appended by a later refill`() {
        setQueue(pool(POOL))
        removeFromTimeline("7")
        endless.dropFromSource(7L)
        shadowMainLooper().idle()

        seekToItem(player.mediaItemCount - 1)

        val appended = (POOL - 1 until player.mediaItemCount)
            .map { player.getMediaItemAt(it).mediaId }
        assertTrue("refill produced nothing to inspect", appended.isNotEmpty())
        assertFalse("dropped song came back in the refill", appended.contains("7"))
    }

    /** Turning shuffle on must not disturb whatever is playing right now. */
    @Test
    fun `enabling shuffle leaves the current item playing`() {
        setQueue(pool(POOL))
        seekToItem(3)
        val playing = player.currentMediaItem?.mediaId

        endless.applyShuffle(true)
        shadowMainLooper().idle()

        assertTrue(endless.isShuffleEnabled())
        assertEquals("shuffle moved the song out from under the listener", playing, player.currentMediaItem?.mediaId)
    }

    /**
     * A user-queued item is a promise about what plays next, so a shuffle
     * toggle must leave it pinned directly after the current item rather than
     * scattering it into the reordered tail.
     */
    @Test
    fun `shuffle keeps user-queued items pinned after the current item`() {
        setQueue(pool(POOL))
        seekToItem(3)
        insertUserQueued(at = 4)

        endless.applyShuffle(true)
        shadowMainLooper().idle()

        val current = player.currentMediaItemIndex
        assertEquals("uq", player.getMediaItemAt(current + 1).mediaId)
    }

    // --- helpers -----------------------------------------------------------

    private fun setQueue(items: List<MediaItem>) {
        player.setMediaItems(items)
        player.prepare()
        shadowMainLooper().idle()
    }

    private fun seekToItem(index: Int) {
        player.seekTo(index, 0L)
        shadowMainLooper().idle()
    }

    private fun insertUserQueued(at: Int) {
        player.addMediaItem(at, userQueuedItem("uq"))
        shadowMainLooper().idle()
    }

    /** Mirrors what the ViewModel does before telling the service to drop a song. */
    private fun removeFromTimeline(mediaId: String) {
        for (i in (player.mediaItemCount - 1) downTo 0) {
            if (player.getMediaItemAt(i).mediaId == mediaId) player.removeMediaItem(i)
        }
        shadowMainLooper().idle()
    }

    private fun findItem(mediaId: String): MediaItem? =
        (0 until player.mediaItemCount)
            .map { player.getMediaItemAt(it) }
            .firstOrNull { it.mediaId == mediaId }

    /**
     * Numeric media ids on purpose: that is the form the phone UI produces, and
     * [EndlessQueueController.dropFromSource] matches on the parsed song id, so
     * a non-numeric id would silently make that path a no-op in the test while
     * working fine in production.
     */
    private fun pool(size: Int): List<MediaItem> = (1..size).map { item("$it") }

    private fun item(id: String): MediaItem =
        MediaItem.Builder().setMediaId(id).setUri(URI_PREFIX + id).build()

    private fun userQueuedItem(id: String): MediaItem =
        MediaItem.Builder()
            .setMediaId(id)
            .setUri(URI_PREFIX + id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setExtras(Bundle().apply { putBoolean(KEY_USER_QUEUED, true) })
                    .build()
            )
            .build()

    private companion object {
        const val URI_PREFIX = "https://test.invalid/"

        /**
         * Above the engine's 20-item minimum history window, so that a second
         * pass pushes the play position past `historyKeep()` and pruning
         * actually runs.
         */
        const val POOL = 25

        /**
         * How far past the user-queued item to skip. Large enough that the
         * front-prune still has items to trim after the user-queue removal has
         * pulled the play index down.
         */
        const val JUMP = 5
    }
}
