package com.example.data

import android.util.Log

object AppDelayManager {
    private const val TAG = "AppDelayManager"
    private var whitelistedPackage: String? = null
    private var whitelistExpirationTime: Long = 0L

    // Safe session grace period (30 seconds)
    private const val SESSION_GRACE_PERIOD_MS = 30000L

    // Cooldown & Usage Limit States
    private var activeSessionPackage: String? = null
    private var sessionStartMs: Long = 0L
    private var sessionLimitMs: Long = 0L
    private var cooldownPackage: String? = null
    private var cooldownUntilMs: Long = 0L

    // Prevention of rapid activity spawning / multi-triggers
    private var isDelayActivityActive = false
    private var lastTriggerTime: Long = 0L

    @Synchronized
    fun setDelayActivityActive(active: Boolean) {
        isDelayActivityActive = active
        Log.d(TAG, "DelayActivity active state changed to: $active")
    }

    @Synchronized
    fun isDelayActivityActive(): Boolean = isDelayActivityActive

    @Synchronized
    fun canTriggerDelay(): Boolean {
        if (isDelayActivityActive) {
            Log.d(TAG, "Preventing trigger: DelayActivity is already active")
            return false
        }
        val now = System.currentTimeMillis()
        if (now - lastTriggerTime < 1500L) {
            Log.d(TAG, "Preventing trigger: Cooldown rate-limit between consecutive launches (${now - lastTriggerTime} ms)")
            return false
        }
        lastTriggerTime = now
        return true
    }

    @Synchronized
    fun isWhitelisted(packageName: String): Boolean {
        val currentTime = System.currentTimeMillis()
        
        // If they are in active cooldown, they are absolutely NOT whitelisted
        if (currentTime < cooldownUntilMs && packageName == cooldownPackage) {
            Log.d(TAG, "Package $packageName is in active cooldown")
            return false
        }

        val isOk = packageName == whitelistedPackage && currentTime < whitelistExpirationTime
        if (isOk) {
            Log.d(TAG, "Package $packageName is whitelisted. Time scale: ${whitelistExpirationTime - currentTime} ms remaining.")
        }
        return isOk
    }

    @Synchronized
    fun whitelistPackage(packageName: String, durationMs: Long = 24 * 60 * 60 * 1000L) {
        val currentTime = System.currentTimeMillis()
        whitelistedPackage = packageName
        // Enable full daily block-free active usage by default
        whitelistExpirationTime = currentTime + durationMs
        Log.d(TAG, "Whitelisted package $packageName for active usage session")
    }

    @Synchronized
    fun startSession(packageName: String, limitMinutes: Int) {
        activeSessionPackage = packageName
        sessionStartMs = System.currentTimeMillis()
        sessionLimitMs = limitMinutes * 60 * 1000L
        Log.d(TAG, "Started active usage session for $packageName, limit is $limitMinutes min")
    }

    @Synchronized
    fun checkSessionExceeded(packageName: String): Boolean {
        if (packageName == activeSessionPackage && sessionStartMs > 0L) {
            val elapsed = System.currentTimeMillis() - sessionStartMs
            return elapsed > sessionLimitMs
        }
        return false
    }

    @Synchronized
    fun triggerCooldown(packageName: String, cooldownMinutes: Int) {
        cooldownPackage = packageName
        cooldownUntilMs = System.currentTimeMillis() + (cooldownMinutes * 60 * 1000L)
        activeSessionPackage = null
        sessionStartMs = 0L
        clearWhitelist()
        Log.d(TAG, "Triggered cooldown for $packageName for $cooldownMinutes minutes")
    }

    @Synchronized
    fun getCooldownRemainingMs(packageName: String): Long {
        val now = System.currentTimeMillis()
        return if (packageName == cooldownPackage && now < cooldownUntilMs) {
            cooldownUntilMs - now
        } else {
            0L
        }
    }

    @Synchronized
    fun recordEntering(packageName: String) {
        if (packageName == whitelistedPackage) {
            val now = System.currentTimeMillis()
            if (now < cooldownUntilMs && packageName == cooldownPackage) {
                // If in cooldown, they cannot re-enter whitelisted state
                return
            }
            // Restore full block-free active usage session when returning
            whitelistExpirationTime = System.currentTimeMillis() + 24 * 60 * 60 * 1000L
            Log.d(TAG, "User active in $packageName, extended whitelist session")
        }
    }

    @Synchronized
    fun recordExit() {
        if (whitelistedPackage != null && whitelistExpirationTime > System.currentTimeMillis() + SESSION_GRACE_PERIOD_MS) {
            // Set expiration to 30 seconds from now when they leave
            whitelistExpirationTime = System.currentTimeMillis() + SESSION_GRACE_PERIOD_MS
            Log.d(TAG, "User exited whitelisted package $whitelistedPackage, grace period started: 30 seconds")
        }
    }

    @Synchronized
    fun clearWhitelist() {
        whitelistedPackage = null
        whitelistExpirationTime = 0
    }
}
