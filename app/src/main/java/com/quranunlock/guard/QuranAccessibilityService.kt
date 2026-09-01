package com.applicreation0.quransafeguard

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class QuranAccessibilityService : AccessibilityService() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val pendingLaunches = mutableListOf<Runnable>()
    private var foregroundPackage: String? = null
    private var foregroundUnlockedPackage: String? = null
    private var pendingForegroundPause: Runnable? = null
    private var screenReceiverRegistered = false
    private var broadExitDetection = false
    private var guardPrefs: SharedPreferences? = null

    private val scopePreferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == GuardPrefs.PROTECTED_PACKAGES && !broadExitDetection) {
                applyEventPackageScope(broad = false)
            }
        }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                foregroundUnlockedPackage?.let {
                    GuardPrefs.endUnlockForeground(this@QuranAccessibilityService, it)
                }
                foregroundUnlockedPackage = null
                foregroundPackage = null
                GuardRuntime.resetForeground()
                cancelPendingLaunches()
                val snapshot = GuardRuntime.interception.snapshot()
                if (!snapshot.guardVisible) {
                    GuardRuntime.interception.reset()
                }
                applyEventPackageScope(broad = false)
                GuardDiagnostics.log(this@QuranAccessibilityService, "SCREEN_OFF_USAGE_PAUSED")
            }
        }
    }

    private val heartbeat = object : Runnable {
        override fun run() {
            GuardHealth.heartbeat(this@QuranAccessibilityService)
            mainHandler.postDelayed(this, 30_000L)
        }
    }

    private val usageTicker = object : Runnable {
        override fun run() {
            val packageName = foregroundUnlockedPackage
            if (packageName != null) {
                val remaining = GuardPrefs.remainingUnlockMs(
                    this@QuranAccessibilityService,
                    packageName
                )

                if (remaining <= 0L) {
                    GuardPrefs.expireUnlock(this@QuranAccessibilityService, packageName)
                    foregroundUnlockedPackage = null
                    showGentleMessage(
                        "Cette session est terminée. Une nouvelle lecture vous permettra de continuer."
                    )

                    if (foregroundPackage == packageName &&
                        ProtectedApps.isProtected(this@QuranAccessibilityService, packageName)
                    ) {
                        GuardRuntime.interception.reset()
                        val now = SystemClock.elapsedRealtime()
                        if (GuardRuntime.interception.begin(packageName, now)) {
                            GuardDiagnostics.log(
                                this@QuranAccessibilityService,
                                "USAGE_BUDGET_EXPIRED",
                                packageName
                            )
                            cancelPendingLaunches()
                            launchGate(packageName, "time_expired")
                            scheduleRetry(packageName, 350L, "expiry_retry_1")
                            scheduleRetry(packageName, 900L, "expiry_retry_2")
                        }
                    }
                } else {
                    maybeShowUsageReminder(packageName)
                }
            }

            mainHandler.postDelayed(this, 1_000L)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        BrowserDetector.refresh()
        GuardRuntime.interception.reset()
        GuardHealth.markConnected(this)
        GuardDiagnostics.log(this, "SERVICE_CONNECTED")
        guardPrefs = getSharedPreferences(GuardPrefs.FILE, Context.MODE_PRIVATE).also {
            it.registerOnSharedPreferenceChangeListener(scopePreferenceListener)
        }
        applyEventPackageScope(broad = false)
        registerScreenReceiverIfNeeded()
        mainHandler.removeCallbacks(heartbeat)
        mainHandler.post(heartbeat)
        mainHandler.removeCallbacks(usageTicker)
        mainHandler.post(usageTicker)
    }

    private fun registerScreenReceiverIfNeeded() {
        if (screenReceiverRegistered) return
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenReceiver, filter)
        }
        screenReceiverRegistered = true
    }

    private fun unregisterScreenReceiverIfNeeded() {
        if (!screenReceiverRegistered) return
        runCatching { unregisterReceiver(screenReceiver) }
        screenReceiverRegistered = false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return

        // Fixed-scope model: anything outside selected social/browser targets,
        // Settings and Safeguard itself is treated identically. No label lookup,
        // category lookup, diagnostic entry or sensitive-app association occurs.
        if (!ProtectedApps.isEventScopePackage(this, packageName)) {
            handleOutsideScopeForeground()
            return
        }

        val isProtectedPackage = ProtectedApps.isProtected(this, packageName)

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            (event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED &&
                (isProtectedPackage || packageName == this.packageName))
        ) {
            handleForegroundPackage(packageName)
        }

        if (!isProtectedPackage) return

        // A protected target is active. Broaden only temporarily so a transition
        // to ANY other app can pause actual-foreground time and cancel gate races.
        applyEventPackageScope(broad = true)

        if (GuardPrefs.isUnlocked(this, packageName)) return

        val now = SystemClock.elapsedRealtime()
        GuardHealth.markProtectedEvent(this, packageName)

        if (!GuardRuntime.interception.begin(packageName, now)) {
            return
        }

        GuardDiagnostics.log(
            this,
            code = "TARGET_DETECTED",
            packageName = packageName,
            detail = "eventType=${event.eventType} class=${event.className ?: "?"}"
        )

        cancelPendingLaunches()
        launchGate(packageName, "initial")
        scheduleRetry(packageName, 350L, "retry_1")
        scheduleRetry(packageName, 900L, "retry_2")
    }

    private fun handleOutsideScopeForeground() {
        pendingForegroundPause?.let(mainHandler::removeCallbacks)
        pendingForegroundPause = null

        foregroundUnlockedPackage?.let {
            GuardPrefs.endUnlockForeground(this, it)
        }
        foregroundUnlockedPackage = null
        foregroundPackage = null
        GuardRuntime.resetForeground()

        cancelPendingLaunches()
        val snapshot = GuardRuntime.interception.snapshot()
        if (!snapshot.guardVisible) {
            GuardRuntime.interception.reset()
        }

        // Once the transition away from a protected target is known, stop
        // receiving events from unrelated apps again.
        applyEventPackageScope(broad = false)
    }

    private fun applyEventPackageScope(broad: Boolean) {
        if (broadExitDetection == broad && broad) return
        val info = serviceInfo ?: return
        info.packageNames = if (broad) {
            null
        } else {
            ProtectedApps.eventScopePackages(this).toTypedArray()
        }
        setServiceInfo(info)
        broadExitDetection = broad
    }

    private fun handleForegroundPackage(packageName: String) {
        if (packageName != this.packageName) {
            GuardRuntime.markExternalForeground(packageName)
            val snapshot = GuardRuntime.interception.snapshot()
            if (snapshot.targetPackage != null &&
                snapshot.targetPackage != packageName &&
                !snapshot.guardVisible
            ) {
                cancelPendingLaunches()
                GuardRuntime.interception.reset()
            }
        }
        if (foregroundPackage == packageName) {
            pendingForegroundPause?.let(mainHandler::removeCallbacks)
            pendingForegroundPause = null
            return
        }

        if (ProtectedApps.isProtected(this, packageName) &&
            GuardPrefs.isUnlocked(this, packageName)
        ) {
            pendingForegroundPause?.let(mainHandler::removeCallbacks)
            pendingForegroundPause = null

            val previousUnlocked = foregroundUnlockedPackage
            if (previousUnlocked != null && previousUnlocked != packageName) {
                GuardPrefs.endUnlockForeground(this, previousUnlocked)
            }

            foregroundPackage = packageName
            GuardPrefs.beginUnlockForeground(this, packageName)
            foregroundUnlockedPackage = packageName
            return
        }

        val previousUnlocked = foregroundUnlockedPackage
        foregroundPackage = packageName
        if (previousUnlocked == null) return

        pendingForegroundPause?.let(mainHandler::removeCallbacks)
        val pauseTask = Runnable {
            if (foregroundUnlockedPackage == previousUnlocked &&
                foregroundPackage != previousUnlocked
            ) {
                GuardPrefs.endUnlockForeground(this, previousUnlocked)
                foregroundUnlockedPackage = null
            }
            pendingForegroundPause = null
        }
        pendingForegroundPause = pauseTask
        mainHandler.postDelayed(pauseTask, 750L)
    }

    private fun maybeShowUsageReminder(packageName: String) {
        val thresholds = listOf(
            10 to "Il vous reste 10 min 🌿",
            5 to "Encore 5 min. Profitez-en sereinement.",
            1 to "Dernière minute avant la prochaine pause Quran."
        )

        thresholds.forEach { (minutes, message) ->
            if (GuardPrefs.markUsageReminderShown(this, packageName, minutes)) {
                GuardDiagnostics.log(
                    this,
                    "USAGE_REMINDER",
                    packageName,
                    "remaining=" + minutes + "min"
                )
                showGentleMessage(message)
            }
        }
    }

    private fun showGentleMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun launchGate(packageName: String, reason: String) {
        if (GuardPrefs.isUnlocked(this, packageName)) return
        if (!ProtectedApps.isProtected(this, packageName)) return
        if (GuardRuntime.externalForegroundPackage() != packageName) return
        if (!GuardRuntime.interception.shouldRetry(packageName) && reason != "initial") return

        // A delayed retry is valid only while its original target is still
        // foreground. This prevents a gate from appearing over the next app.
        if (reason != "initial" && foregroundPackage != packageName) {
            GuardDiagnostics.log(this, "GATE_RETRY_SKIPPED", packageName, "target_not_foreground")
            return
        }

        GuardRuntime.interception.markGateRequested(packageName)
        GuardDiagnostics.log(this, "GATE_REQUESTED", packageName, reason)

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
            .onFailure {
                GuardDiagnostics.log(
                    this,
                    "GATE_START_FAILED",
                    packageName,
                    it.javaClass.simpleName
                )
            }
    }

    private fun scheduleRetry(
        packageName: String,
        delayMs: Long,
        reason: String
    ) {
        lateinit var task: Runnable
        task = Runnable {
            pendingLaunches.remove(task)
            if (GuardRuntime.interception.shouldRetry(packageName)) {
                launchGate(packageName, reason)
            } else {
                GuardDiagnostics.log(this, "GATE_RETRY_SKIPPED", packageName, reason)
            }
        }

        pendingLaunches += task
        mainHandler.postDelayed(task, delayMs)
    }

    private fun cancelPendingLaunches() {
        pendingLaunches.forEach(mainHandler::removeCallbacks)
        pendingLaunches.clear()
    }

    override fun onInterrupt() {
        GuardDiagnostics.log(this, "SERVICE_INTERRUPTED")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        GuardHealth.markDisconnected(this)
        GuardDiagnostics.log(this, "SERVICE_UNBOUND")
        foregroundUnlockedPackage?.let { GuardPrefs.endUnlockForeground(this, it) }
        foregroundUnlockedPackage = null
        foregroundPackage = null
        GuardRuntime.resetForeground()
        mainHandler.removeCallbacks(heartbeat)
        mainHandler.removeCallbacks(usageTicker)
        pendingForegroundPause?.let(mainHandler::removeCallbacks)
        pendingForegroundPause = null
        unregisterScreenReceiverIfNeeded()
        guardPrefs?.unregisterOnSharedPreferenceChangeListener(scopePreferenceListener)
        guardPrefs = null
        cancelPendingLaunches()
        GuardRuntime.interception.reset()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        GuardHealth.markDisconnected(this)
        GuardDiagnostics.log(this, "SERVICE_DESTROYED")
        foregroundUnlockedPackage?.let { GuardPrefs.endUnlockForeground(this, it) }
        foregroundUnlockedPackage = null
        foregroundPackage = null
        GuardRuntime.resetForeground()
        mainHandler.removeCallbacks(heartbeat)
        mainHandler.removeCallbacks(usageTicker)
        pendingForegroundPause?.let(mainHandler::removeCallbacks)
        pendingForegroundPause = null
        unregisterScreenReceiverIfNeeded()
        guardPrefs?.unregisterOnSharedPreferenceChangeListener(scopePreferenceListener)
        guardPrefs = null
        cancelPendingLaunches()
        GuardRuntime.interception.reset()
        super.onDestroy()
    }
}
