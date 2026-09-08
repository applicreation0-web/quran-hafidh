package com.applicreation0.quransafeguard

import android.app.Activity
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.View
import java.lang.reflect.Modifier

enum class VisualChange(val ghostingWeight: Int) {
    PAGE(8),
    MASK_LEVEL(3),
    REVEAL(4),
    // A temporary reveal can otherwise leave readable ghost text behind the mask.
    REVEAL_RETURN(6),
    LINE(3),
    MILESTONE(2),
    HIGHLIGHT(1)
}

/**
 * Refresh policy has no access to memorization state and can only redraw a View.
 * STANDARD is a strict no-op. EINK coalesces redraws and requests a full refresh
 * only after real, accumulated ghosting risk.
 */
class EInkRefreshController(
    private val activity: Activity,
    private val profile: DisplayProfile
) {
    companion object {
        internal const val FULL_REFRESH_THRESHOLD = 10
        internal const val MIN_FULL_REFRESH_INTERVAL_MS = 2_500L
    }

    private val handler = Handler(Looper.getMainLooper())
    private var ghostingScore = 0
    private var lastFullRefreshAt = 0L

    fun onVisualChange(view: View?, change: VisualChange) {
        if (profile == DisplayProfile.STANDARD || view == null) return
        if (change == VisualChange.PAGE) {
            requestFullRefresh(view, force = true)
            return
        }
        ghostingScore += change.ghostingWeight
        view.postInvalidateOnAnimation()
        if (ghostingScore >= FULL_REFRESH_THRESHOLD) {
            requestFullRefresh(view, force = false)
        }
    }

    private fun requestFullRefresh(view: View, force: Boolean) {
        val now = android.os.SystemClock.elapsedRealtime()
        if (!force && now - lastFullRefreshAt < MIN_FULL_REFRESH_INTERVAL_MS) return
        lastFullRefreshAt = now
        ghostingScore = 0
        if (tryVendorFullRefresh(view)) return

        // Generic Android E-Ink fallback: one deliberate full-window black/cream
        // transition. It is rate-limited and never used for ordinary highlights.
        val root = activity.window.decorView
        root.setBackgroundColor(Color.BLACK)
        root.invalidate()
        handler.postDelayed({
            root.setBackgroundColor(Color.rgb(247, 242, 232))
            root.invalidate()
            view.invalidate()
        }, 55L)
    }

    private fun tryVendorFullRefresh(view: View): Boolean = runCatching {
        val controller = Class.forName("com.onyx.android.sdk.api.device.epd.EpdController")
        val modeClass = Class.forName("com.onyx.android.sdk.api.device.epd.UpdateMode")
        val gc = modeClass.enumConstants?.firstOrNull {
            it.toString().equals("GC", true) || it.toString().contains("FULL", true)
        } ?: return false
        val method = controller.methods.firstOrNull { candidate ->
            Modifier.isStatic(candidate.modifiers) &&
                candidate.parameterTypes.size == 2 &&
                View::class.java.isAssignableFrom(candidate.parameterTypes[0]) &&
                candidate.parameterTypes[1].isAssignableFrom(modeClass)
        } ?: return false
        method.invoke(null, view, gc)
        true
    }.getOrDefault(false)
}
