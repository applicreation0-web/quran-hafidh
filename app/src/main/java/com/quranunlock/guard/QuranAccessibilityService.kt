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
    private val permanentlyExcludedCache = mutableSetOf<String>()

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

        // Sensitive / critical apps are a hard trust boundary. We use their
        // foreground event only to stop any previous Safeguard work, then
        // return before protection, diagnostics or challenge logic can run.
        if (isPermanentlyExcluded(packageName)) {
            handlePermanentlyExcludedForeground()
            return
        }

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

    private fun isPermanentlyExcluded(packageName: String): Boolean {
        if (packageName in permanentlyExcludedCache) return true
        val excluded = ProtectedApps.isAlwaysAllowed(this, packageName)
        if (excluded) permanentlyExcludedCache += packageName
        return excluded
    }

    private fun handlePermanentlyExcludedForeground() {
        pendingForegroundPause?.let(mainHandler::removeCallbacks)
        pendingForegroundPause = null

        foregroundUnlockedPackage?.let {
            GuardPrefs.endUnlockForeground(this, it)
        }
        foregroundUnlockedPackage = null
        foregroundPackage = null

        // Most importantly, retries created for a previous protected app must
        // never spill over on top of banking, identity or security apps.
        cancelPendingLaunches()
        val snapshot = GuardRuntime.interception.snapshot()
        if (!snapshot.guardVisible) {
            GuardRuntime.interception.reset()
        }
    }

    private fun handleForegroundPackage(packageName: String) {
        if (packageName != this.packageName) {
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
        if (isPermanentlyExcluded(packageName)) return
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
        mainHandler.removeCallbacks(heartbeat)
        mainHandler.removeCallbacks(usageTicker)
        pendingForegroundPause?.let(mainHandler::removeCallbacks)
        pendingForegroundPause = null
        unregisterScreenReceiverIfNeeded()
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
        mainHandler.removeCallbacks(heartbeat)
        mainHandler.removeCallbacks(usageTicker)
        pendingForegroundPause?.let(mainHandler::removeCallbacks)
        pendingForegroundPause = null
        unregisterScreenReceiverIfNeeded()
        cancelPendingLaunches()
        GuardRuntime.interception.reset()
        super.onDestroy()
    }
}
