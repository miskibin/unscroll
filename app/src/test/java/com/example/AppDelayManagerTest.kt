package com.example

import com.example.data.AppDelayManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Covers the access/cooldown/emergency state machine that decides whether a blocked app needs
 * a pause. These are the rules that previously misfired and closed apps mid-use.
 */
class AppDelayManagerTest {

    private val instagram = "com.instagram.android"

    @Before
    fun reset() {
        AppDelayManager.clearGrant()
        AppDelayManager.cancelEmergencyDisable()
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
    fun `unlimited grant reports no session limit`() {
        AppDelayManager.grantAccess(instagram, sessionLimitMinutes = 0)
        assertEquals(AppDelayManager.NO_SESSION_LIMIT, AppDelayManager.sessionRemainingMs(instagram))
    }

    @Test
    fun `limited grant counts down from the configured limit`() {
        AppDelayManager.grantAccess(instagram, sessionLimitMinutes = 5)
        val remaining = AppDelayManager.sessionRemainingMs(instagram)
        assertTrue(remaining in (5 * 60_000L - 2_000L)..(5 * 60_000L))
    }

    @Test
    fun `earning bonus time clears cooldown and opens a short session`() {
        AppDelayManager.startCooldown(instagram, 10)
        AppDelayManager.grantBonusTime(instagram, minutes = 5)

        assertEquals(0L, AppDelayManager.cooldownRemainingMs(instagram))
        assertTrue(AppDelayManager.isAccessGranted(instagram))
        val remaining = AppDelayManager.sessionRemainingMs(instagram)
        assertTrue(remaining in (5 * 60_000L - 2_000L)..(5 * 60_000L))
    }

    @Test
    fun `emergency disable is active for the chosen window`() {
        assertFalse(AppDelayManager.isEmergencyDisabled())
        AppDelayManager.startEmergencyDisable(20)
        assertTrue(AppDelayManager.isEmergencyDisabled())
        assertTrue(AppDelayManager.emergencyRemainingMs() > 0L)
    }

    @Test
    fun `emergency disable can be cancelled`() {
        AppDelayManager.startEmergencyDisable(20)
        AppDelayManager.cancelEmergencyDisable()
        assertFalse(AppDelayManager.isEmergencyDisabled())
        assertEquals(0L, AppDelayManager.emergencyRemainingMs())
    }

    @Test
    fun `pause screens are rate-limited`() {
        assertTrue(AppDelayManager.canShowPause())
        assertFalse(AppDelayManager.canShowPause())
    }
}
