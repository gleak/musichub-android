package com.mediaplayer.android.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Plain JVM tests — the policy has no Android dependency by design.
 *
 * These pin two things the offline queue depends on: that a user's pending
 * action is only ever destroyed when it genuinely cannot succeed, and that
 * every "try again" comes with a delay attached.
 */
class EventDispatchPolicyTest {

    /**
     * The regression this policy was extracted for. An auth failure is a
     * perfectly good HTTP response, so connectivity still looks healthy and the
     * drain loop does not block; the row is only kept out of the next
     * iteration by its backoff. Answering 401 with anything but RETRY_LATER —
     * or with a RETRY_LATER that skipped the backoff — reinstates a loop that
     * sends requests continuously for the life of the process.
     */
    @Test
    fun `auth failures are retried later, never dropped`() {
        assertEquals(EventDispatchAction.RETRY_LATER, actionFor(401))
        assertEquals(EventDispatchAction.RETRY_LATER, actionFor(403))
    }

    @Test
    fun `every retry carries a non-zero delay`() {
        for (attempts in 1..12) {
            assertTrue(
                "attempt $attempts produced no delay",
                EventDispatchPolicy.backoffMsFor(attempts) > 0L,
            )
        }
    }

    @Test
    fun `throttling responses are retried later`() {
        assertEquals(EventDispatchAction.RETRY_LATER, actionFor(408))
        assertEquals(EventDispatchAction.RETRY_LATER, actionFor(429))
    }

    @Test
    fun `permanent client rejections are dropped`() {
        for (code in listOf(400, 404, 409, 410, 422)) {
            assertEquals("HTTP $code", EventDispatchAction.DROP, actionFor(code))
        }
    }

    @Test
    fun `server errors are retried later`() {
        for (code in listOf(500, 502, 503, 504)) {
            assertEquals("HTTP $code", EventDispatchAction.RETRY_LATER, actionFor(code))
        }
    }

    /** No response at all — a transport failure, transient by definition. */
    @Test
    fun `transport failures are retried later`() {
        assertEquals(EventDispatchAction.RETRY_LATER, actionFor(null))
    }

    /** A payload that cannot be parsed will not parse on the tenth attempt either. */
    @Test
    fun `poison pills are dropped regardless of status`() {
        assertEquals(EventDispatchAction.DROP, actionFor(null, isPoisonPill = true))
        assertEquals(EventDispatchAction.DROP, actionFor(500, isPoisonPill = true))
        assertEquals(EventDispatchAction.DROP, actionFor(401, isPoisonPill = true))
    }

    @Test
    fun `backoff grows with attempts`() {
        val series = (1..6).map { EventDispatchPolicy.backoffMsFor(it) }

        assertEquals(series.sorted(), series)
        assertEquals(2_000L, EventDispatchPolicy.backoffMsFor(1))
        assertEquals(4_000L, EventDispatchPolicy.backoffMsFor(2))
    }

    /** The cap has to be reachable, or it is not a cap. */
    @Test
    fun `backoff is capped and the cap is actually reached`() {
        val far = EventDispatchPolicy.backoffMsFor(Int.MAX_VALUE)

        assertEquals(5 * 60_000L, far)
        assertEquals(far, EventDispatchPolicy.backoffMsFor(64))
    }

    private fun actionFor(httpCode: Int?, isPoisonPill: Boolean = false) =
        EventDispatchPolicy.actionFor(httpCode = httpCode, isPoisonPill = isPoisonPill)
}
