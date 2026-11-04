package com.wangzan101.mika.core.shell

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsbUnplugReapTest {
    private var now = 1_000L

    private fun reap(windowMs: Long = UsbUnplugReap.WINDOW_MS) = UsbUnplugReap(windowMs) { now }

    @Test
    fun unplugThenHelperLossWithinTheWindowIsDetected() {
        val r = reap()
        r.onPowerDisconnected()
        now += UsbUnplugReap.WINDOW_MS
        assertTrue(r.onHelperLost(alreadyKnown = false))
    }

    @Test
    fun helperLossOutsideTheWindowIsIgnored() {
        val r = reap()
        r.onPowerDisconnected()
        now += UsbUnplugReap.WINDOW_MS + 1
        assertFalse(r.onHelperLost(alreadyKnown = false))
    }

    @Test
    fun helperLossWithoutAPowerDisconnectIsIgnored() {
        assertFalse(reap().onHelperLost(alreadyKnown = false))
    }

    @Test
    fun helperLossOnlyCountsAfterAPowerDisconnect() {
        val r = reap()
        // 先没助手、后断电:那次死亡不算(当时还没断过电)
        assertFalse(r.onHelperLost(alreadyKnown = false))
        r.onPowerDisconnected()
        now += 1_000
        assertTrue(r.onHelperLost(alreadyKnown = false))
    }

    @Test
    fun alreadyKnownDevicesDoNotReEvaluate() {
        val r = reap()
        r.onPowerDisconnected()
        assertFalse(r.onHelperLost(alreadyKnown = true))
        // 断电事件已被这次死亡用掉:再判不算
        now += 1_000
        assertFalse(r.onHelperLost(alreadyKnown = false))
    }

    @Test
    fun oneDetectionIsEnoughAndDoesNotRepeat() {
        val r = reap()
        r.onPowerDisconnected()
        assertTrue(r.onHelperLost(alreadyKnown = false))
        r.onPowerDisconnected()
        now += 1_000
        assertFalse(r.onHelperLost(alreadyKnown = true))
    }
}
