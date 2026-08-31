package com.quranunlock.guard

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class QuranAccessibilityService : AccessibilityService() {
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

        val now = System.currentTimeMillis()
        if (lastInterceptedPackage == packageName && now - lastInterceptAt < 1200L) return
        lastInterceptedPackage = packageName
        lastInterceptAt = now

        // Remove the protected app from the foreground first. This prevents
        // interaction with content hidden behind the Quran challenge.
        performGlobalAction(GLOBAL_ACTION_HOME)

        val intent = Intent(this, GateActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            )
            putExtra(GateActivity.EXTRA_TARGET_PACKAGE, packageName)
        }
        startActivity(intent)
    }

    override fun onInterrupt() = Unit
}
