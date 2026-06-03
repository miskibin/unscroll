package com.example

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.data.AppDatabase
import com.example.data.AppDelayManager
import com.example.data.SettingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Watches which app is in the foreground and shows the conscious-pause screen the moment
 * the user *enters* a blocked app.
 *
 * The key design point — and the fix for "it closes my app when I open the comment section" —
 * is that we only react to genuine app switches. While you stay inside the same app (scrolling,
 * opening comments, DMs, a profile — all the same package) no new pause is ever triggered.
 */
class DelayAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    @Volatile private var blockedPackages: Set<String> = emptySet()
    @Volatile private var cooldownEnabled = false
    @Volatile private var usageLimitMinutes = SettingRepository.DEFAULT_COOLDOWN_USAGE_MINUTES
    @Volatile private var cooldownMinutes = SettingRepository.DEFAULT_COOLDOWN_PERIOD_MINUTES

    /** Last real (non-transient) app seen in the foreground. */
    private var foregroundApp: String? = null

    /** Pending "your session ran out" timer for the current foreground app. */
    private var sessionLimitJob: Job? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkg = event.packageName?.toString() ?: return
        if (isTransient(pkg)) return

        // Same app as before: internal navigation (comments, reels, DMs...). Never re-pause.
        if (pkg == foregroundApp) {
            AppDelayManager.keepAlive(pkg)
            return
        }

        // Genuine foreground switch.
        foregroundApp?.let { AppDelayManager.onLeftForeground(it) }
        cancelSessionLimit()
        foregroundApp = pkg

        if (pkg in blockedPackages) {
            handleBlockedApp(pkg)
        }
    }

    private fun handleBlockedApp(pkg: String) {
        val cooldownRemaining = AppDelayManager.cooldownRemainingMs(pkg)
        if (cooldownRemaining > 0L) {
            openPauseScreen(pkg, cooldownRemainingMs = cooldownRemaining)
            return
        }

        if (AppDelayManager.isAccessGranted(pkg)) {
            AppDelayManager.keepAlive(pkg)
            if (cooldownEnabled) scheduleSessionLimit(pkg)
            return
        }

        if (!AppDelayManager.canShowPause()) return
        Log.d(TAG, "Intercepting $pkg")
        openPauseScreen(pkg)
    }

    /** Lock the app once the usage session runs out, but never mid-action of another app. */
    private fun scheduleSessionLimit(pkg: String) {
        cancelSessionLimit()
        sessionLimitJob = scope.launch {
            delay(AppDelayManager.sessionRemainingMs(pkg, usageLimitMinutes))
            if (foregroundApp == pkg && AppDelayManager.cooldownRemainingMs(pkg) == 0L) {
                Log.d(TAG, "Usage limit reached for $pkg, starting cooldown")
                AppDelayManager.startCooldown(pkg, cooldownMinutes)
                openPauseScreen(pkg, cooldownRemainingMs = cooldownMinutes * 60_000L)
            }
        }
    }

    private fun cancelSessionLimit() {
        sessionLimitJob?.cancel()
        sessionLimitJob = null
    }

    /**
     * Send the user home, then show the full-screen pause/lock over everything. Going home first
     * means the blocked app isn't sitting live behind the pause.
     */
    private fun openPauseScreen(pkg: String, cooldownRemainingMs: Long = 0L) {
        startActivity(Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })

        startActivity(Intent(this, DelayActivity::class.java).apply {
            putExtra(DelayActivity.EXTRA_TARGET_PACKAGE, pkg)
            if (cooldownRemainingMs > 0L) {
                putExtra(DelayActivity.EXTRA_IS_COOLDOWN, true)
                putExtra(DelayActivity.EXTRA_COOLDOWN_REMAINING_MS, cooldownRemainingMs)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    /** Packages that are overlays/system chrome, not a real foreground app switch. */
    private fun isTransient(pkg: String): Boolean =
        pkg == packageName ||
            pkg == "android" ||
            pkg == "com.android.systemui" ||
            pkg == "com.google.android.gms" ||
            pkg.endsWith("packageinstaller") ||
            pkg.endsWith("permissioncontroller") ||
            pkg.contains("inputmethod", ignoreCase = true) ||
            pkg.contains("keyboard", ignoreCase = true) ||
            pkg.endsWith(".ime")

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Service connected")

        val repository = SettingRepository(AppDatabase.getDatabase(this).settingDao())
        scope.launch {
            repository.blockedPackagesFlow.collect { csv ->
                blockedPackages = csv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            }
        }
        scope.launch { repository.cooldownEnabledFlow.collect { cooldownEnabled = it } }
        scope.launch { repository.cooldownUsageMinutesFlow.collect { usageLimitMinutes = it } }
        scope.launch { repository.cooldownPeriodMinutesFlow.collect { cooldownMinutes = it } }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        cancelSessionLimit()
        scope.coroutineContext[Job]?.cancel()
    }

    companion object {
        private const val TAG = "DelayAccessibility"
    }
}
