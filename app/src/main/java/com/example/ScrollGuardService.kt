package com.example

import android.accessibilityservice.AccessibilityService
import android.content.SharedPreferences
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.example.data.BudgetSnapshot
import com.example.data.GuardState
import com.example.data.ScrollBudget
import com.example.data.SettingsStore
import com.example.detection.Platform
import com.example.detection.Platforms
import com.example.detection.ShortsDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Watches Instagram / YouTube / TikTok and recognises their short-form
 * swipe feeds (Reels, Shorts, For You). The rest of each app — DMs,
 * search, profiles, regular videos — is never touched.
 *
 * While a feed is on screen, watch time is charged against a rolling
 * allowance (default 2 min per 10 min). When it runs out, the service
 * presses Back and shows a toast until the allowance refills.
 *
 * Reliability rules, in response to bugs in the previous version:
 *  - the only enforcement action is GLOBAL_ACTION_BACK — no activities are
 *    launched, the user is never thrown to the home screen;
 *  - Back is only pressed when the feed is POSITIVELY detected on screen
 *    AND the budget is exhausted AND the master switch is on;
 *  - Back presses are rate limited so a detection glitch can never cause
 *    a burst of navigation.
 */
class ScrollGuardService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var watchJob: Job? = null

    private lateinit var settingsStore: SettingsStore
    private lateinit var detector: ShortsDetector
    private lateinit var budget: ScrollBudget
    private lateinit var statePrefs: SharedPreferences

    private var lastBackAt = 0L
    private var lastToastAt = 0L
    private var lastPersistAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        settingsStore = SettingsStore.get(this)
        val metrics = resources.displayMetrics
        detector = ShortsDetector(metrics.widthPixels, metrics.heightPixels)

        val settings = settingsStore.settings.value
        budget = ScrollBudget(settings.allowanceMs, settings.windowMs)
        statePrefs = getSharedPreferences("scroll_guard_state", MODE_PRIVATE)
        budget.restore(
            windowStartAt = statePrefs.getLong("window_start_at", 0L),
            usedMs = statePrefs.getLong("used_ms", 0L),
            now = System.currentTimeMillis(),
        )

        scope.launch {
            settingsStore.settings.collect {
                budget.allowanceMs = it.allowanceMs
                budget.windowMs = it.windowMs
                publishState()
            }
        }

        GuardState.publishServiceRunning(true)
        publishState()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        val platform = Platforms.forPackage(pkg) ?: return

        val settings = settingsStore.settings.value
        if (!settings.enabled || platform.key !in settings.enabledPlatforms) return

        // The 1 Hz watcher does the accounting; events only need to start it
        // (and bounce instantly when re-entering an already exhausted feed).
        if (watchJob?.isActive == true) return

        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != pkg) return
        if (!detector.isSurfaceVisible(root, platform)) return

        val now = System.currentTimeMillis()
        if (budget.onSurfaceVisible(now)) {
            enforce(now)
        }
        publishState()
        startWatching(platform)
    }

    /**
     * Runs while a short-form feed stays on screen: charges watch time,
     * enforces the limit, and stops itself as soon as the user leaves the
     * feed (so DMs and the rest of the app are never charged or blocked).
     */
    private fun startWatching(platform: Platform) {
        watchJob?.cancel()
        watchJob = scope.launch {
            while (isActive) {
                delay(1_000)
                val settings = settingsStore.settings.value
                if (!settings.enabled || platform.key !in settings.enabledPlatforms) break

                val root = rootInActiveWindow ?: break
                if (root.packageName?.toString() !in platform.packages) break
                if (!detector.isSurfaceVisible(root, platform)) break

                val now = System.currentTimeMillis()
                if (budget.onSurfaceVisible(now)) {
                    enforce(now)
                }
                publishState()
                persistIfDue(now)
            }
            persist()
            publishState()
        }
    }

    private fun enforce(now: Long) {
        if (now - lastBackAt < BACK_RATE_LIMIT_MS) return
        lastBackAt = now
        performGlobalAction(GLOBAL_ACTION_BACK)
        persist()

        if (now - lastToastAt > TOAST_RATE_LIMIT_MS) {
            lastToastAt = now
            val minutes = ((budget.msUntilReset(now) + 59_999) / 60_000).coerceAtLeast(1)
            Toast.makeText(
                this,
                getString(R.string.toast_limit_reached, minutes),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun publishState() {
        val now = System.currentTimeMillis()
        GuardState.publishBudget(
            BudgetSnapshot(
                usedMs = budget.usedMs,
                allowanceMs = budget.allowanceMs,
                windowMs = budget.windowMs,
                resetInMs = budget.msUntilReset(now),
                exhausted = budget.isExhausted(now),
                updatedAt = now,
            ),
        )
    }

    private fun persistIfDue(now: Long) {
        if (now - lastPersistAt > 5_000) persist()
    }

    private fun persist() {
        lastPersistAt = System.currentTimeMillis()
        statePrefs.edit()
            .putLong("window_start_at", budget.windowStartAt)
            .putLong("used_ms", budget.usedMs)
            .apply()
    }

    override fun onInterrupt() {
        watchJob?.cancel()
    }

    override fun onDestroy() {
        GuardState.publishServiceRunning(false)
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val BACK_RATE_LIMIT_MS = 2_000L
        private const val TOAST_RATE_LIMIT_MS = 8_000L
    }
}
