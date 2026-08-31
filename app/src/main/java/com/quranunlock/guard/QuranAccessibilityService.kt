package com.quranunlock.guard

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class QuranAccessibilityService : AccessibilityService() {
    private var lastInterceptedPackage: String? = null
    private var lastInterceptAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        if (!ProtectedApps.isProtected(this, packageName)) return
        if (GuardPrefs.isUnlocked(this, packageName)) return

        val now = System.currentTimeMillis()
        if (lastInterceptedPackage == packageName && now - lastInterceptAt < 1200L) return
        lastInterceptedPackage = packageName
        lastInterceptAt = now

        val intent = Intent(this, GateActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(GateActivity.EXTRA_TARGET_PACKAGE, packageName)
        }
        startActivity(intent)
    }

    override fun onInterrupt() = Unit
}
