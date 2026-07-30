package com.mediaplayer.android.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pure index arithmetic, so no player and no Android here.
 *
 * Two forces make this non-trivial: the service prunes played history on every
 * transition, shifting every index down, and the endless engine appends whole
 * fresh passes of the pool, so one song can occupy many positions at once.
 */
class QueueIndexResolverTest {

    @Test
    fun `a hint that still points at the song is trusted`() {
        val index = QueueIndexResolver.resolve(
            expectedSongId = 30L,
            hintIndex = 2,
            timelineIds = listOf(10L, 20L, 30L, 40L),
        )

        assertEquals(2, index)
    }

    /** The everyday case: history was pruned, so everything slid down. */
    @Test
    fun `a hint shifted by pruning finds the song at its new index`() {
        val index = QueueIndexResolver.resolve(
            expectedSongId = 30L,
            hintIndex = 4,
            timelineIds = listOf(20L, 30L, 40L),
        )

        assertEquals(1, index)
    }

    /**
     * With a repeated pool the nearest occurrence to the hint is the one the
     * user meant. Picking any other seeks backwards into history, or removes a
     * row they are not looking at.
     */
    @Test
    fun `among duplicates the occurrence nearest the hint wins`() {
        val index = QueueIndexResolver.resolve(
            expectedSongId = 7L,
            hintIndex = 10,
            timelineIds = listOf(7L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 7L),
        )

        assertEquals("index 11 is one away, index 7 is three away", 11, index)
    }

    @Test
    fun `a tie resolves to the earlier occurrence`() {
        val index = QueueIndexResolver.resolve(
            expectedSongId = 5L,
            hintIndex = 2,
            timelineIds = listOf(5L, 9L, 9L, 9L, 5L),
        )

        assertEquals(0, index)
    }

    @Test
    fun `a song no longer in the timeline resolves to null`() {
        assertNull(
            QueueIndexResolver.resolve(
                expectedSongId = 99L,
                hintIndex = 1,
                timelineIds = listOf(1L, 2L, 3L),
            )
        )
    }

    @Test
    fun `an empty timeline resolves to null`() {
        assertNull(QueueIndexResolver.resolve(1L, 0, emptyList()))
    }

    @Test
    fun `a hint past the end still finds the song`() {
        val index = QueueIndexResolver.resolve(
            expectedSongId = 2L,
            hintIndex = 500,
            timelineIds = listOf(1L, 2L, 3L),
        )

        assertEquals(1, index)
    }

    @Test
    fun `a negative hint still finds the song`() {
        val index = QueueIndexResolver.resolve(
            expectedSongId = 3L,
            hintIndex = -4,
            timelineIds = listOf(1L, 2L, 3L),
        )

        assertEquals(2, index)
    }

    /** Local tracks resolve to negative ids; unparseable rows come through as null. */
    @Test
    fun `unresolvable rows are skipped rather than matched`() {
        val index = QueueIndexResolver.resolve(
            expectedSongId = -5L,
            hintIndex = 0,
            timelineIds = listOf(null, null, -5L),
        )

        assertEquals(2, index)
    }

    @Test
    fun `a null expectation never matches a null row`() {
        val index = QueueIndexResolver.resolve(
            expectedSongId = 1L,
            hintIndex = 0,
            timelineIds = listOf(null, null),
        )

        assertNull(index)
    }
}
