package com.applicreation0.quransafeguard

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
    private var scopeReceiverRegistered = false
    private var activeAccessibilityScope: Set<String> = emptySet()

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                foregroundUnlockedPackage?.let {
                    GuardPrefs.endUnlockForeground(this@QuranAccessibilityService, it)
                }
                foregroundUnlockedPackage = null
                foregroundPackage = null
                GuardDiagnostics.log(this@QuranAccessibilityService, "SCREEN_OFF_USAGE_PAUSED")
            }
        }
    }

    private val scopeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AccessibilityScopeManager.ACTION_REFRESH_SCOPE) {
                refreshAccessibilityScope()
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
        registerScopeReceiverIfNeeded()
        refreshAccessibilityScope()
        GuardDiagnostics.log(this, "SERVICE_CONNECTED")
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

    private fun registerScopeReceiverIfNeeded() {
        if (scopeReceiverRegistered) return
        val filter = IntentFilter(AccessibilityScopeManager.ACTION_REFRESH_SCOPE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(scopeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(scopeReceiver, filter)
        }
        scopeReceiverRegistered = true
    }

    private fun unregisterScopeReceiverIfNeeded() {
        if (!scopeReceiverRegistered) return
        runCatching { unregisterReceiver(scopeReceiver) }
        scopeReceiverRegistered = false
    }

    private fun refreshAccessibilityScope() {
        val packages = AccessibilityScopeManager.applyTo(this)
        activeAccessibilityScope = packages

        pendingForegroundPause?.let(mainHandler::removeCallbacks)
        pendingForegroundPause = null

        foregroundUnlockedPackage?.let { unlockedPackage ->
            if (unlockedPackage !in packages) {
                GuardPrefs.endUnlockForeground(this, unlockedPackage)
                foregroundUnlockedPackage = null
            }
        }

        if (foregroundPackage != null && foregroundPackage !in packages) {
            foregroundPackage = null
        }

        cancelPendingLaunches()
        GuardRuntime.interception.reset()
        GuardDiagnostics.log(
            this,
            "ACCESSIBILITY_SCOPE_REFRESHED",
            detail = "packages=" + packages.size
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return

        // Defense in depth: packages outside the explicit accessibility scope
        // are ignored before any foreground, budget, diagnostic or gate logic.
        if (packageName !in activeAccessibilityScope) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            handleForegroundPackage(packageName)
        }

        if (!ProtectedApps.isProtected(this, packageName)) return
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

    private fun handleForegroundPackage(packageName: String) {
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
        if (!GuardRuntime.interception.shouldRetry(packageName) && reason != "initial") return

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
        activeAccessibilityScope = emptySet()
        mainHandler.removeCallbacks(heartbeat)
        mainHandler.removeCallbacks(usageTicker)
        pendingForegroundPause?.let(mainHandler::removeCallbacks)
        pendingForegroundPause = null
        unregisterScreenReceiverIfNeeded()
        unregisterScopeReceiverIfNeeded()
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
        activeAccessibilityScope = emptySet()
        mainHandler.removeCallbacks(heartbeat)
        mainHandler.removeCallbacks(usageTicker)
        pendingForegroundPause?.let(mainHandler::removeCallbacks)
        pendingForegroundPause = null
        unregisterScreenReceiverIfNeeded()
        unregisterScopeReceiverIfNeeded()
        cancelPendingLaunches()
        GuardRuntime.interception.reset()
        super.onDestroy()
    }
}
