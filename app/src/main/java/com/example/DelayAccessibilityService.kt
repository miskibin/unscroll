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
import kotlinx.coroutines.launch

class DelayAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var blockedList = listOf("com.instagram.android", "com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.ss.android.ugc.aweme")
    private var cooldownEnabled = false
    private var cooldownUsageMins = 5
    private var cooldownPeriodMins = 10

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        Log.d(TAG, "Window state changed: $packageName")

        if (packageName in blockedList) {
            // Check if user is in active cooldown
            val cooldownRemaining = AppDelayManager.getCooldownRemainingMs(packageName)
            if (cooldownRemaining > 0L) {
                redirectHomeAndShowCooldown(packageName, cooldownRemaining)
                return
            }

            if (AppDelayManager.isWhitelisted(packageName)) {
                AppDelayManager.recordEntering(packageName)

                if (cooldownEnabled) {
                    if (AppDelayManager.checkSessionExceeded(packageName)) {
                        if (isExclusionActive(event)) {
                            Log.d(TAG, "Usage duration exceeded, but user is in exclusion (chatting/posting). Grace period allowed.")
                        } else {
                            AppDelayManager.triggerCooldown(packageName, cooldownPeriodMins)
                            redirectHomeAndShowCooldown(packageName, AppDelayManager.getCooldownRemainingMs(packageName))
                        }
                    }
                }
                return
            }

            // Prevent rapid multiple triggers of DelayActivity
            if (!AppDelayManager.canTriggerDelay()) {
                return
            }

            Log.d(TAG, "Intercepting package: $packageName")

            // Go to home screen to stop instant access
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)

            // Open full-screen DelayActivity to show countdown
            val delayIntent = Intent(this, DelayActivity::class.java).apply {
                putExtra("target_package", packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(delayIntent)
        } else {
            // Ignore keyboards/IMEs, common system dialog overlays and our own package so we don't start the exit grace period
            val isIgnored = packageName == this.packageName ||
                    packageName.contains("inputmethod", ignoreCase = true) ||
                    packageName.contains("keyboard", ignoreCase = true) ||
                    packageName.contains("ime", ignoreCase = true) ||
                    packageName == "com.android.systemui" ||
                    packageName == "com.google.android.permissioncontroller" ||
                    packageName == "com.android.permissioncontroller" ||
                    packageName == "com.google.android.packageinstaller" ||
                    packageName == "com.android.packageinstaller" ||
                    packageName == "android" ||
                    packageName == "com.google.android.gms"

            if (!isIgnored) {
                AppDelayManager.recordExit()
            }
        }
    }

    private fun redirectHomeAndShowCooldown(packageName: String, remainingMs: Long) {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)

        val delayIntent = Intent(this, DelayActivity::class.java).apply {
            putExtra("target_package", packageName)
            putExtra("is_cooldown", true)
            putExtra("cooldown_remaining_ms", remainingMs)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(delayIntent)
    }

    private fun isExclusionActive(event: AccessibilityEvent): Boolean {
        val className = event.className?.toString() ?: ""
        if (className.contains("EditText", ignoreCase = true)) {
            Log.d(TAG, "Exclusion detected: user typing")
            return true
        }

        val textsToSearch = mutableListOf<String>()
        event.contentDescription?.let { textsToSearch.add(it.toString()) }
        event.text.forEach { charSec ->
            textsToSearch.add(charSec.toString())
        }

        val keywords = listOf(
            "direct", "dm", "message", "chat", "czat", "wiadomośc", "rozmow", "napisz", 
            "camera", "aparat", "story", "relacj", "utwórz", "nowy post", "create", "composer", 
            "post", "share", "opublikuj", "dodaj", "kamera", "wątek", "threads"
        )

        for (text in textsToSearch) {
            val textLower = text.lowercase()
            if (keywords.any { textLower.contains(it) }) {
                Log.d(TAG, "Exclusion detected via keyword: $textLower")
                return true
            }
        }

        val root = rootInActiveWindow
        if (root != null) {
            val focusedNode = root.findFocus(android.view.accessibility.AccessibilityNodeInfo.FOCUS_INPUT)
            if (focusedNode != null) {
                Log.d(TAG, "Exclusion detected: active input focus")
                focusedNode.recycle()
                return true
            }
        }

        return false
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Service connected successfully")

        val database = AppDatabase.getDatabase(this)
        val repository = SettingRepository(database.settingDao())

        // Reactively observe key settings
        serviceScope.launch {
            repository.blockedPackagesFlow.collect { listStr ->
                blockedList = listStr.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                Log.d(TAG, "Observer update blocked packages: $blockedList")
            }
        }
        serviceScope.launch {
            repository.cooldownEnabledFlow.collect { enabled ->
                cooldownEnabled = enabled
                Log.d(TAG, "Observer update cooldown enabled: $cooldownEnabled")
            }
        }
        serviceScope.launch {
            repository.cooldownUsageMinutesFlow.collect { mins ->
                cooldownUsageMins = mins
                Log.d(TAG, "Observer update cooldown usage: $cooldownUsageMins min")
            }
        }
        serviceScope.launch {
            repository.cooldownPeriodMinutesFlow.collect { mins ->
                cooldownPeriodMins = mins
                Log.d(TAG, "Observer update cooldown period: $cooldownPeriodMins min")
            }
        }
    }

    companion object {
        private const val TAG = "DelayAccessibility"
    }
}
