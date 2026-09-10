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
    private val overlayCallbacks = mutableSetOf<Runnable>()
    private val overlayDrawables = mutableListOf<Pair<WeakReference<View>, ColorDrawable>>()
    private var disposed = false

    fun onVisualChange(view: View?, change: VisualChange) {
        if (disposed || profile == DisplayProfile.STANDARD || view == null) return
        pendingView = WeakReference(view)
        apply(view, policy.onChange(change, clock()))
    }

    fun dispose() {
        if (disposed) return
        disposed = true
        cancelDeferred()
        overlayCallbacks.toList().forEach(handler::removeCallbacks)
        overlayCallbacks.clear()
        overlayDrawables.toList().forEach { (ref, drawable) -> ref.get()?.overlay?.remove(drawable) }
        overlayDrawables.clear()
        pendingView.clear()
    }

    private fun apply(view: View, decision: RefreshDecision) {
        if (disposed) return
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
        if (disposed) return
        pendingView = WeakReference(view)
        if (scheduledDueAt == dueAtMs) return
        handler.removeCallbacks(deferred)
        scheduledDueAt = dueAtMs
        handler.postDelayed(deferred, (dueAtMs - clock()).coerceAtLeast(0L))
    }

    private fun runDeferred() {
        scheduledDueAt = Long.MIN_VALUE
        if (disposed) return
        val view = pendingView.get() ?: return
        apply(view, policy.onPendingDue(clock()))
    }

    private fun cancelDeferred() {
        handler.removeCallbacks(deferred)
        scheduledDueAt = Long.MIN_VALUE
    }

    private fun requestFullRefresh(view: View) {
        if (disposed || tryVendorFullRefresh(view)) return
        if (view is WebView) {
            view.postOnAnimation {
                if (disposed) return@postOnAnimation
                view.evaluateJavascript(
                    "window.einkFullRefreshFallback && window.einkFullRefreshFallback()",
                ) { result -> if (!disposed && result != "true") nativeOverlay(view) }
            }
            return
        }
        nativeOverlay(view)
    }

    private fun postTracked(delayMs: Long, action: () -> Unit) {
        if (disposed) return
        lateinit var callback: Runnable
        callback = Runnable {
            overlayCallbacks.remove(callback)
            if (!disposed) action()
        }
        overlayCallbacks.add(callback)
        handler.postDelayed(callback, delayMs)
    }

    private fun addOverlay(view: View, drawable: ColorDrawable) {
        if (disposed) return
        view.overlay.add(drawable)
        overlayDrawables.add(WeakReference<View>(view) to drawable)
        view.invalidate()
    }

    private fun removeOverlay(view: View, drawable: ColorDrawable) {
        view.overlay.remove(drawable)
        overlayDrawables.removeAll { it.first.get() === view && it.second === drawable }
        view.invalidate()
    }

    private fun nativeOverlay(view: View) {
        if (disposed) return
        val black = ColorDrawable(Color.BLACK).apply { setBounds(0, 0, view.width, view.height) }
        val cream = ColorDrawable(Color.rgb(247, 242, 232)).apply { setBounds(0, 0, view.width, view.height) }
        addOverlay(view, black)
        postTracked(55L) {
            removeOverlay(view, black)
            addOverlay(view, cream)
            postTracked(55L) { removeOverlay(view, cream) }
        }
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
