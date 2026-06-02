package com.example.data

/**
 * The single source of truth for "is the user currently allowed into a blocked app".
 *
 * Only one app is ever in the foreground, so we only ever track one grant at a time.
 * The model is intentionally tiny:
 *
 *  - A *grant* is created when the user finishes the conscious pause. It stays valid
 *    while the app is in the foreground, and survives a short [RETURN_GRACE_MS] window
 *    after the user leaves so quick app-switches (opening a shared link, answering a
 *    notification) don't force a brand-new pause.
 *  - A *cooldown* is an optional lock that blocks the app for a while once a usage
 *    session runs out.
 *
 * Everything is guarded by `@Synchronized` because the accessibility service and the
 * pause activity touch it from different threads.
 */
object AppDelayManager {

    /** Grace window after leaving a granted app during which re-entry needs no new pause. */
    private const val RETURN_GRACE_MS = 20_000L

    /** Minimum gap between two pause screens, so a burst of window events can't stack them. */
    private const val PAUSE_RATE_LIMIT_MS = 1_500L

    /** Sentinel meaning "valid as long as the app stays in the foreground". */
    private const val FOREGROUND = Long.MAX_VALUE

    private var grantedPackage: String? = null
    private var grantValidUntil: Long = 0L
    private var sessionStartedAt: Long = 0L

    private var cooldownPackage: String? = null
    private var cooldownUntil: Long = 0L

    private var lastPauseShownAt: Long = 0L

    private fun now() = System.currentTimeMillis()

    // --- Access grant ---------------------------------------------------------

    @Synchronized
    fun isAccessGranted(pkg: String): Boolean =
        pkg == grantedPackage && now() < grantValidUntil

    /** The user completed the pause; let them into [pkg] and start a usage session. */
    @Synchronized
    fun grantAccess(pkg: String) {
        grantedPackage = pkg
        grantValidUntil = FOREGROUND
        sessionStartedAt = now()
    }

    /** [pkg] is in the foreground right now; keep its grant from lapsing. */
    @Synchronized
    fun keepAlive(pkg: String) {
        if (pkg == grantedPackage) grantValidUntil = FOREGROUND
    }

    /** The user just left [pkg]; if it was granted, start the short return grace. */
    @Synchronized
    fun onLeftForeground(pkg: String) {
        if (pkg == grantedPackage && grantValidUntil == FOREGROUND) {
            grantValidUntil = now() + RETURN_GRACE_MS
        }
    }

    /** Milliseconds left in the current usage session for [pkg], 0 if none. */
    @Synchronized
    fun sessionRemainingMs(pkg: String, limitMinutes: Int): Long {
        if (pkg != grantedPackage) return 0L
        return (sessionStartedAt + limitMinutes * 60_000L - now()).coerceAtLeast(0L)
    }

    @Synchronized
    fun clearGrant() {
        grantedPackage = null
        grantValidUntil = 0L
        sessionStartedAt = 0L
    }

    // --- Cooldown -------------------------------------------------------------

    /** Milliseconds left on an active cooldown for [pkg], 0 if it isn't locked. */
    @Synchronized
    fun cooldownRemainingMs(pkg: String): Long {
        val remaining = cooldownUntil - now()
        return if (pkg == cooldownPackage && remaining > 0L) remaining else 0L
    }

    @Synchronized
    fun startCooldown(pkg: String, minutes: Int) {
        cooldownPackage = pkg
        cooldownUntil = now() + minutes * 60_000L
        clearGrant()
    }

    // --- Pause rate-limiting --------------------------------------------------

    /** True if enough time has passed to show another pause screen (and records it). */
    @Synchronized
    fun canShowPause(): Boolean {
        val t = now()
        if (t - lastPauseShownAt < PAUSE_RATE_LIMIT_MS) return false
        lastPauseShownAt = t
        return true
    }
}
