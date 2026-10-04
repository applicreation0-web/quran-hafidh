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
import java.lang.reflect.Method

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
        return if (nowMs >= due) fullNow(nowMs) else RefreshDecision(RefreshAction.FULL_LATER, due)
    }

    private fun fullNow(nowMs: Long): RefreshDecision {
        score = 0
        pendingFull = false
        lastFullAt = nowMs
        return RefreshDecision(RefreshAction.FULL_NOW)
    }
}

/**
 * Rendering-only E-Ink controller. Product functionality is never selected here.
 *
 * On BOOX, the official Onyx EpdController path is preferred reflectively:
 * - REGAL (or GU on older SDKs) for text/partial changes;
 * - GC for cleanup/full refresh.
 * Reflection keeps the ordinary Android build independent from an Onyx SDK artifact.
 * If the device does not expose a compatible API, a portable WebView/View fallback is
 * used. STANDARD remains a strict no-op beyond Android's own normal rendering.
 */
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
            RefreshAction.PARTIAL -> requestPartialRefresh(view)
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

    private fun requestPartialRefresh(view: View) {
        if (tryOnyxRefresh(view, full = false)) return
        view.postInvalidateOnAnimation()
    }

    private fun requestFullRefresh(view: View) {
        if (tryOnyxRefresh(view, full = true)) return
        requestPortableFullRefresh(view)
    }

    /**
     * Supports modern and legacy BOOX package layouts without a compile-time dependency.
     * The reflective path fails closed to the portable renderer on any mismatch.
     */
    private fun tryOnyxRefresh(view: View, full: Boolean): Boolean = runCatching {
        val controller = runCatching {
            Class.forName("com.onyx.android.sdk.api.device.epd.EpdController")
        }.getOrElse {
            Class.forName("com.onyx.android.sdk.device.EpdController")
        }

        val modeClass = controller.declaredClasses.firstOrNull { it.simpleName == "UpdateMode" }
            ?: runCatching {
                Class.forName("com.onyx.android.sdk.api.device.epd.UpdateMode")
            }.getOrElse {
                Class.forName("com.onyx.android.sdk.device.EpdController\$UpdateMode")
            }

        val desiredNames = if (full) listOf("GC") else listOf("REGAL", "GU", "GU_FAST")
        val mode = requireNotNull(modeClass.enumConstants?.firstOrNull { constant ->
            desiredNames.any { it == (constant as Enum<*>).name }
        }) { "No compatible Onyx update mode." }

        if (!full) {
            findStaticMethod(controller, "setViewDefaultUpdateMode", View::class.java, modeClass)
                ?.invoke(null, view, mode)
            // Requesting the normal invalidation after the default mode is set lets the
            // platform coalesce local dirty regions rather than forcing a whole-screen GC.
            view.invalidate()
            true
        } else {
            val invalidate = findStaticMethod(controller, "invalidate", View::class.java, modeClass)
                ?: findStaticMethod(controller, "postInvalidate", View::class.java, modeClass)
                ?: error("No compatible Onyx GC invalidate method.")
            invalidate.invoke(null, view, mode)
            true
        }
    }.getOrDefault(false)

    private fun findStaticMethod(owner: Class<*>, name: String, first: Class<*>, second: Class<*>): Method? =
        owner.methods.firstOrNull { method ->
            method.name == name && method.parameterTypes.size == 2 &&
                method.parameterTypes[0].isAssignableFrom(first) &&
                method.parameterTypes[1].isAssignableFrom(second)
        }

    private fun requestPortableFullRefresh(view: View) {
        if (disposed) return
        if (view is WebView) {
            view.postOnAnimation {
                if (disposed) return@postOnAnimation
                view.evaluateJavascript(
                    "window.einkFullRefreshFallback && window.einkFullRefreshFallback()"
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

    /** Last-resort fallback for non-Onyx E-Ink devices only. */
    private fun nativeOverlay(view: View) {
        if (disposed) return
        val black = ColorDrawable(Color.rgb(23, 23, 21)).apply { setBounds(0, 0, view.width, view.height) }
        val cream = ColorDrawable(Color.rgb(247, 242, 232)).apply { setBounds(0, 0, view.width, view.height) }
        addOverlay(view, black)
        postTracked(55L) {
            removeOverlay(view, black)
            addOverlay(view, cream)
            postTracked(55L) { removeOverlay(view, cream) }
        }
    }
}
