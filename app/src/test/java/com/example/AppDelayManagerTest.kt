package com.example

import com.example.data.AppDelayManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Covers the access/cooldown state machine that decides whether a blocked app needs a pause.
 * These are the rules that previously misfired and closed apps mid-use.
 */
class AppDelayManagerTest {

    private val instagram = "com.instagram.android"

    @Before
    fun reset() {
        AppDelayManager.clearGrant()
        // Expire any lingering cooldown from a previous test.
        AppDelayManager.startCooldown(instagram, 0)
        AppDelayManager.clearGrant()
    }

    @Test
    fun `no access before the pause is completed`() {
        assertFalse(AppDelayManager.isAccessGranted(instagram))
    }

    @Test
    fun `access is granted after completing the pause`() {
        AppDelayManager.grantAccess(instagram)
        assertTrue(AppDelayManager.isAccessGranted(instagram))
    }

    @Test
    fun `staying inside the app keeps access - the comment-section case`() {
        AppDelayManager.grantAccess(instagram)
        // Simulate many internal navigation events.
        repeat(50) { AppDelayManager.keepAlive(instagram) }
        assertTrue(AppDelayManager.isAccessGranted(instagram))
    }

    @Test
    fun `briefly leaving keeps access within the grace window`() {
        AppDelayManager.grantAccess(instagram)
        AppDelayManager.onLeftForeground(instagram)
        assertTrue(AppDelayManager.isAccessGranted(instagram))
    }

    @Test
    fun `a different app does not inherit the grant`() {
        AppDelayManager.grantAccess(instagram)
        assertFalse(AppDelayManager.isAccessGranted("com.zhiliaoapp.musically"))
    }

    @Test
    fun `cooldown blocks access and clears any grant`() {
        AppDelayManager.grantAccess(instagram)
        AppDelayManager.startCooldown(instagram, 10)

        assertFalse(AppDelayManager.isAccessGranted(instagram))
        assertTrue(AppDelayManager.cooldownRemainingMs(instagram) > 0L)
    }

    @Test
    fun `session countdown reflects the configured limit`() {
        AppDelayManager.grantAccess(instagram)
        val remaining = AppDelayManager.sessionRemainingMs(instagram, limitMinutes = 5)
        // Allow a little slack for execution time.
        assertTrue(remaining in (5 * 60_000L - 2_000L)..(5 * 60_000L))
    }

    @Test
    fun `pause screens are rate-limited`() {
        assertTrue(AppDelayManager.canShowPause())
        assertFalse(AppDelayManager.canShowPause())
    }
}
