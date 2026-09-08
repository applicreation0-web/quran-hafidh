package com.applicreation0.quransafeguard

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.webkit.WebView
import java.lang.ref.WeakReference
import java.lang.reflect.Modifier

enum class VisualChange(val ghostingWeight: Int) {
    PAGE(8), MASK_LEVEL(3), REVEAL(4), REVEAL_RETURN(6),
    LINE(3), MILESTONE(2), HIGHLIGHT(1)
}

internal enum class RefreshAction { NONE, PARTIAL, FULL_NOW, FULL_LATER }
internal data class RefreshDecision(val action: RefreshAction, val dueAtMs: Long = 0L)

/** Pure scheduling policy: a refused cleanup remains pending until it can run. */
internal class EInkRefreshPolicy(
    private val threshold: Int = EInkRefreshController.FULL_REFRESH_THRESHOLD,
    private val minimumIntervalMs: Long = EInkRefreshController.MIN_FULL_REFRESH_INTERVAL_MS
) {
    private var score = 0
    private var lastFullAt = Long.MIN_VALUE
    private var pendingFull = false

    fun onChange(change: VisualChange, nowMs: Long): RefreshDecision {
        if (change == VisualChange.PAGE) return fullNow(nowMs)
        score += change.ghostingWeight
        // A mask restored after a temporary reveal needs a clean final frame even
        // if another refresh happened while the text was still revealed.
        if (change == VisualChange.REVEAL_RETURN || score >= threshold) pendingFull = true
        return if (pendingFull) decide(nowMs) else RefreshDecision(RefreshAction.PARTIAL)
    }

    fun onPendingDue(nowMs: Long): RefreshDecision =
        if (pendingFull) decide(nowMs) else RefreshDecision(RefreshAction.NONE)

    private fun decide(nowMs: Long): RefreshDecision {
        val due = if (lastFullAt == Long.MIN_VALUE) nowMs else lastFullAt + minimumIntervalMs
        return if (nowMs >= due) fullNow(nowMs)
        else RefreshDecision(RefreshAction.FULL_LATER, due)
    }

    private fun fullNow(nowMs: Long): RefreshDecision {
        score = 0
        pendingFull = false
        lastFullAt = nowMs
        return RefreshDecision(RefreshAction.FULL_NOW)
    }
}

/** STANDARD is a strict no-op; EINK only performs visual refreshes. */
class EInkRefreshController(
    @Suppress("UNUSED_PARAMETER") activity: Activity,
    private val profile: DisplayProfile,
    private val handler: Handler = Handler(Looper.getMainLooper()),
    private val clock: () -> Long = { SystemClock.elapsedRealtime() }
) {
    companion object {
        internal const val FULL_REFRESH_THRESHOLD = 10
        internal const val MIN_FULL_REFRESH_INTERVAL_MS = 2_500L
    }

    private val policy = EInkRefreshPolicy()
    private var pendingView = WeakReference<View>(null)
    private var scheduledDueAt = Long.MIN_VALUE
    private val deferred = Runnable { runDeferred() }

    fun onVisualChange(view: View?, change: VisualChange) {
        if (profile == DisplayProfile.STANDARD || view == null) return
        pendingView = WeakReference(view)
        apply(view, policy.onChange(change, clock()))
    }

    private fun apply(view: View, decision: RefreshDecision) {
        when (decision.action) {
            RefreshAction.NONE -> Unit
            RefreshAction.PARTIAL -> view.postInvalidateOnAnimation()
            RefreshAction.FULL_NOW -> {
                cancelDeferred()
                requestFullRefresh(view)
            }
            RefreshAction.FULL_LATER -> schedule(view, decision.dueAtMs)
        }
    }

    private fun schedule(view: View, dueAtMs: Long) {
        pendingView = WeakReference(view)
        if (scheduledDueAt == dueAtMs) return
        handler.removeCallbacks(deferred)
        scheduledDueAt = dueAtMs
        handler.postDelayed(deferred, (dueAtMs - clock()).coerceAtLeast(0L))
    }

    private fun runDeferred() {
        scheduledDueAt = Long.MIN_VALUE
        val view = pendingView.get() ?: return
        apply(view, policy.onPendingDue(clock()))
    }

    private fun cancelDeferred() {
        handler.removeCallbacks(deferred)
        scheduledDueAt = Long.MIN_VALUE
    }

    private fun requestFullRefresh(view: View) {
        if (tryVendorFullRefresh(view)) return
        // This HTML cleaner is rendered above the opaque Mushaf WebView content.
        if (view is WebView) {
            view.postOnAnimation {
                view.evaluateJavascript(
                    "window.einkFullRefreshFallback && window.einkFullRefreshFallback()",
                ) { result -> if (result != "true") nativeOverlay(view) }
            }
            return
        }
        nativeOverlay(view)
    }

    private fun nativeOverlay(view: View) {
        val overlay = view.overlay
        val black = ColorDrawable(Color.BLACK).apply { setBounds(0, 0, view.width, view.height) }
        val cream = ColorDrawable(Color.rgb(247, 242, 232)).apply { setBounds(0, 0, view.width, view.height) }
        overlay.add(black)
        view.invalidate()
        handler.postDelayed({
            overlay.remove(black)
            overlay.add(cream)
            view.invalidate()
            handler.postDelayed({ overlay.remove(cream); view.invalidate() }, 55L)
        }, 55L)
    }

    private fun tryVendorFullRefresh(view: View): Boolean = runCatching {
        val controller = Class.forName("com.onyx.android.sdk.api.device.epd.EpdController")
        val modeClass = Class.forName("com.onyx.android.sdk.api.device.epd.UpdateMode")
        val gc = modeClass.enumConstants?.firstOrNull {
            it.toString().equals("GC", true) || it.toString().contains("FULL", true)
        } ?: return false
        val method = controller.methods.firstOrNull { candidate ->
            Modifier.isStatic(candidate.modifiers) && candidate.parameterTypes.size == 2 &&
                View::class.java.isAssignableFrom(candidate.parameterTypes[0]) &&
                candidate.parameterTypes[1].isAssignableFrom(modeClass)
        } ?: return false
        method.invoke(null, view, gc)
        true
    }.getOrDefault(false)
}
