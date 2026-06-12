package com.example.detection

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque

/**
 * One supported platform whose short-form surface (Reels / Shorts / For You
 * feed) can be recognised inside the app, leaving the rest of the app —
 * DMs, search, profiles — untouched.
 */
data class Platform(
    /** Stable key used for the settings toggle. */
    val key: String,
    val label: String,
    val packages: List<String>,
    /**
     * View id suffixes (the part after "<pkg>:id/") that only exist while the
     * short-form player is on screen. Any visible match counts as detection.
     */
    val surfaceViewIds: List<String> = emptyList(),
    /**
     * Fallback for apps with obfuscated ids (TikTok): treat a visible,
     * near-fullscreen ViewPager as the swipe feed. DM lists and comment
     * sheets are RecyclerViews, so they don't match.
     */
    val fullScreenPagerHeuristic: Boolean = false,
)

object Platforms {
    val all = listOf(
        Platform(
            key = "instagram",
            label = "Instagram Reels",
            packages = listOf("com.instagram.android"),
            // Present for the Reels tab and for reels opened from feed,
            // explore or DMs once they enter the fullscreen clips viewer.
            surfaceViewIds = listOf("clips_viewer_view_pager"),
        ),
        Platform(
            key = "youtube",
            label = "YouTube Shorts",
            packages = listOf("com.google.android.youtube", "app.revanced.android.youtube"),
            surfaceViewIds = listOf("reel_recycler", "reel_progress_bar", "reel_player_page_container"),
        ),
        Platform(
            key = "tiktok",
            label = "TikTok",
            packages = listOf(
                "com.zhiliaoapp.musically",
                "com.ss.android.ugc.trill",
                "com.ss.android.ugc.aweme",
            ),
            surfaceViewIds = listOf("video_container"),
            fullScreenPagerHeuristic = true,
        ),
    )

    private val byPackage: Map<String, Platform> =
        all.flatMap { p -> p.packages.map { it to p } }.toMap()

    fun forPackage(pkg: String?): Platform? = pkg?.let { byPackage[it] }

    val watchedPackages: Set<String> = byPackage.keys
}

class ShortsDetector(
    private val screenWidth: Int,
    private val screenHeight: Int,
) {

    /** True when the short-form surface of [platform] is currently visible in [root]. */
    fun isSurfaceVisible(root: AccessibilityNodeInfo, platform: Platform): Boolean {
        val pkg = root.packageName?.toString() ?: return false
        for (idSuffix in platform.surfaceViewIds) {
            val nodes = try {
                root.findAccessibilityNodeInfosByViewId("$pkg:id/$idSuffix")
            } catch (e: Exception) {
                null
            }
            if (nodes != null && nodes.any { it != null && isOnScreen(it) }) return true
        }
        if (platform.fullScreenPagerHeuristic && hasFullScreenPager(root)) return true
        return false
    }

    private fun isOnScreen(node: AccessibilityNodeInfo): Boolean {
        if (!node.isVisibleToUser) return false
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return rect.right > 0 && rect.left < screenWidth && rect.bottom > 0 && rect.top < screenHeight
    }

    private fun hasFullScreenPager(root: AccessibilityNodeInfo): Boolean {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var scanned = 0
        val rect = Rect()
        while (queue.isNotEmpty() && scanned < 250) {
            val node = queue.removeFirst()
            scanned++
            val className = node.className?.toString().orEmpty()
            if (className.contains("ViewPager", ignoreCase = true) && node.isVisibleToUser) {
                node.getBoundsInScreen(rect)
                val coversScreen = rect.height() >= screenHeight * 0.75 &&
                    rect.width() >= screenWidth * 0.9
                if (coversScreen) return true
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let(queue::add)
            }
        }
        return false
    }
}
