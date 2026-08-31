package com.applicreation0.quransafeguard

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent

class QuranAccessibilityService : AccessibilityService() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingGateLaunch: Runnable? = null
    private var lastInterceptedPackage: String? = null
    private var lastInterceptAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        BrowserDetector.refresh()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        if (!ProtectedApps.isProtected(this, packageName)) return
        if (GuardPrefs.isUnlocked(this, packageName)) return

        val now = SystemClock.elapsedRealtime()
        if (lastInterceptedPackage == packageName && now - lastInterceptAt < 1200L) return
        lastInterceptedPackage = packageName
        lastInterceptAt = now

        pendingGateLaunch?.let(mainHandler::removeCallbacks)

        val launchGate = Runnable {
            pendingGateLaunch = null
            if (GuardPrefs.isUnlocked(this, packageName)) return@Runnable

            val intent = Intent(this, GateActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
                putExtra(GateActivity.EXTRA_TARGET_PACKAGE, packageName)
            }

            runCatching { startActivity(intent) }
        }

        pendingGateLaunch = launchGate

        // GLOBAL_ACTION_HOME is asynchronous. Launching the gate immediately
        // can race with the Home action, causing Android to put the gate behind
        // the launcher. Wait briefly for Home to settle, then show the Quran gate.
        val wentHome = performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
        mainHandler.postDelayed(launchGate, if (wentHome) 350L else 0L)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        pendingGateLaunch?.let(mainHandler::removeCallbacks)
        pendingGateLaunch = null
        super.onDestroy()
    }
}
