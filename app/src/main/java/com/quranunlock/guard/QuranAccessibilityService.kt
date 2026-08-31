package com.applicreation0.quransafeguard

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent

class QuranAccessibilityService : AccessibilityService() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val pendingLaunches = mutableListOf<Runnable>()
    private var lastObservedPackage: String? = null
    private var lastInterceptedPackage: String? = null
    private var lastInterceptAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        BrowserDetector.refresh()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        lastObservedPackage = packageName

        if (!ProtectedApps.isProtected(this, packageName)) return
        if (GuardPrefs.isUnlocked(this, packageName)) return

        val now = SystemClock.elapsedRealtime()
        if (lastInterceptedPackage == packageName && now - lastInterceptAt < 450L) return

        lastInterceptedPackage = packageName
        lastInterceptAt = now
        cancelPendingLaunches()

        val wentHome = performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)

        scheduleGateLaunch(packageName, if (wentHome) 250L else 0L)
        // Safety retry: some Android builds occasionally drop the first
        // foreground launch after GLOBAL_ACTION_HOME. If Quran Safeguard is not
        // observed in front, try once more shortly afterwards.
        scheduleGateLaunch(packageName, if (wentHome) 900L else 500L, retry = true)
    }

    private fun scheduleGateLaunch(
        packageName: String,
        delayMs: Long,
        retry: Boolean = false
    ) {
        lateinit var task: Runnable
        task = Runnable {
            pendingLaunches.remove(task)

            if (GuardPrefs.isUnlocked(this, packageName)) return@Runnable
            if (retry && lastObservedPackage == this.packageName) return@Runnable

            val intent = Intent(this, GateActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
                putExtra(GateActivity.EXTRA_TARGET_PACKAGE, packageName)
            }

            runCatching { startActivity(intent) }
        }

        pendingLaunches += task
        mainHandler.postDelayed(task, delayMs)
    }

    private fun cancelPendingLaunches() {
        pendingLaunches.forEach(mainHandler::removeCallbacks)
        pendingLaunches.clear()
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        cancelPendingLaunches()
        super.onDestroy()
    }
}
