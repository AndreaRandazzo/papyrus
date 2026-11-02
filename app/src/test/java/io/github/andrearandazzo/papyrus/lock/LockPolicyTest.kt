package io.github.andrearandazzo.papyrus.lock

import io.github.andrearandazzo.papyrus.data.LockTimeout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockPolicyTest {

    @Test
    fun immediatelyLocksAfterAnyTimeAway() {
        assertTrue(LockPolicy.shouldLock(LockTimeout.IMMEDIATELY, awayMillis = 0, wentToOtherApp = false))
    }

    @Test
    fun waitsForTheChosenTime() {
        assertFalse(LockPolicy.shouldLock(LockTimeout.ONE_MINUTE, awayMillis = 59_999, wentToOtherApp = false))
        assertTrue(LockPolicy.shouldLock(LockTimeout.ONE_MINUTE, awayMillis = 60_000, wentToOtherApp = false))
    }

    @Test
    fun openingAnotherAppGetsAGracePeriod() {
        // Back from the camera or a PDF viewer after 2 minutes: still unlocked, even with "Immediately".
        assertFalse(LockPolicy.shouldLock(LockTimeout.IMMEDIATELY, awayMillis = 120_000, wentToOtherApp = true))
        assertTrue(LockPolicy.shouldLock(LockTimeout.IMMEDIATELY, awayMillis = 300_000, wentToOtherApp = true))
        // A longer chosen time still wins.
        assertFalse(LockPolicy.shouldLock(LockTimeout.FIVE_MINUTES, awayMillis = 299_999, wentToOtherApp = true))
    }
}
