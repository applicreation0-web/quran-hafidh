package com.applicreation0.quransafeguard

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.annotation.RequiresApi

class QuranAccessibilityService : AccessibilityService() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val pendingLaunches = mutableListOf<Runnable>()

    /**
     * Exactly one protected package may consume an unlock budget at a time.
     * foregroundPackage is the latest actual app/window owner; an IME never replaces it.
     */
    private var foregroundPackage: String? = null
    private var foregroundUnlockedPackage: String? = null

    private var screenReceiverRegistered = false
    private var broadExitDetection = false
    private var guardPrefs: SharedPreferences? = null

    private var audioManager: AudioManager? = null
    private var audioModeListener31: Any? = null
    private var callFreezeActive = false
    private var lastCheckpointElapsedMs = 0L

    private val scopePreferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == GuardPrefs.PROTECTED_PACKAGES && !broadExitDetection) {
                applyEventPackageScope(broad = false)
            }
        }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                pauseForegroundBudget(clearForeground = true)
                GuardRuntime.resetForeground()
                cancelPendingLaunches()

                val snapshot = GuardRuntime.interception.snapshot()
                if (!snapshot.guardVisible) {
                    GuardRuntime.interception.reset()
                }

                applyEventPackageScope(broad = false)
                GuardDiagnostics.log(
                    this@QuranAccessibilityService,
                    "SCREEN_OFF_USAGE_PAUSED"
                )
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
            // Fallback on all Android versions and safety net for listener delivery.
            val currentMode = audioManager?.mode ?: AudioManager.MODE_NORMAL
            handleAudioModeChanged(currentMode)

            if (!callFreezeActive) {
                val packageName = foregroundUnlockedPackage
                if (packageName != null) {
                    val now = SystemClock.elapsedRealtime()
                    if (now - lastCheckpointElapsedMs >= 1_000L) {
                        GuardPrefs.checkpointUnlockForeground(
                            this@QuranAccessibilityService,
                            packageName
                        )
                        lastCheckpointElapsedMs = now
                    }

                    val remaining = GuardPrefs.remainingUnlockMs(
                        this@QuranAccessibilityService,
                        packageName
                    )

                    if (remaining <= 0L) {
                        GuardPrefs.expireUnlock(
                            this@QuranAccessibilityService,
                            packageName
                        )
                        foregroundUnlockedPackage = null
                        showGentleMessage(
                            "Cette session est terminée. Une nouvelle lecture vous permettra de continuer."
                        )
                        triggerExpiredGateIfNeeded(packageName)
                    } else {
                        maybeShowUsageReminder(packageName)
                    }
                }
            }

            // 250 ms bounds call-state fallback and expiration reaction without
            // writing SharedPreferences on every tick (checkpoints remain 1 s).
            mainHandler.postDelayed(this, 250L)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Must happen before any new foreground interval is started. This cleans
        // reboot/service-death markers using the last persisted checkpoint.
        GuardPrefs.reconcileOrphanedUnlockForeground(this)

        BrowserDetector.refresh()
        GuardRuntime.interception.reset()
        GuardHealth.markConnected(this)
        GuardDiagnostics.log(this, "SERVICE_CONNECTED")

        guardPrefs = getSharedPreferences(GuardPrefs.FILE, Context.MODE_PRIVATE).also {
            it.registerOnSharedPreferenceChangeListener(scopePreferenceListener)
        }

        audioManager = getSystemService(AudioManager::class.java)
        callFreezeActive = UnlockBudgetIntegrity.shouldFreezeForAudioMode(
            audioManager?.mode ?: AudioManager.MODE_NORMAL
        )
        registerAudioModeListenerIfSupported()

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

    private fun registerAudioModeListenerIfSupported() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            registerAudioModeListener31()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun registerAudioModeListener31() {
        if (audioModeListener31 != null) return
        val manager = audioManager ?: return
        val listener = AudioManager.OnModeChangedListener { mode ->
            handleAudioModeChanged(mode)
        }
        manager.addOnModeChangedListener(mainExecutor, listener)
        audioModeListener31 = listener
    }

    private fun unregisterAudioModeListenerIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            unregisterAudioModeListener31()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun unregisterAudioModeListener31() {
        val listener =
            audioModeListener31 as? AudioManager.OnModeChangedListener ?: return
        runCatching { audioManager?.removeOnModeChangedListener(listener) }
        audioModeListener31 = null
    }

    /**
     * AudioManager gives a package-independent signal for telephony and VoIP.
     * MODE_IN_COMMUNICATION therefore freezes WhatsApp calls even though normal
     * chats and calls share the same com.whatsapp package.
     */
    private fun handleAudioModeChanged(mode: Int) {
        val shouldFreeze = UnlockBudgetIntegrity.shouldFreezeForAudioMode(mode)
        if (shouldFreeze == callFreezeActive) return

        callFreezeActive = shouldFreeze

        if (shouldFreeze) {
            pauseForegroundBudget(clearForeground = false)
            GuardDiagnostics.log(
                this,
                "CALL_USAGE_FREEZE_STARTED",
                detail = "audioMode=$mode"
            )
        } else {
            GuardDiagnostics.log(
                this,
                "CALL_USAGE_FREEZE_ENDED",
                detail = "audioMode=$mode"
            )
            resumeCurrentProtectedPackageIfEligible()
        }
    }

    private fun activeInputMethodPackage(): String? =
        runCatching {
            Settings.Secure.getString(
                contentResolver,
                Settings.Secure.DEFAULT_INPUT_METHOD
            )
        }.getOrNull()
            ?.substringBefore('/')
            ?.takeIf(String::isNotBlank)

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return

        // The active keyboard belongs to another package but is not an app exit.
        // Keep the protected app as the sole budget owner while typing.
        val protectedForeground = foregroundPackage
            ?.takeIf { ProtectedApps.isProtected(this, it) }
        if (UnlockBudgetIntegrity.isImePseudoForeground(
                eventPackage = packageName,
                activeImePackage = activeInputMethodPackage(),
                currentProtectedPackage = protectedForeground
            )
        ) {
            return
        }

        // Fixed-scope model: anything outside selected social/browser targets,
        // Settings and Safeguard itself is treated identically. No label lookup,
        // category lookup, diagnostic package entry or sensitive-app association.
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

        // While a protected target is active, broaden only long enough to see
        // the first real transition away. IME windows are ignored above.
        applyEventPackageScope(broad = true)

        // Never display the Quran gate on top of an ongoing/ringing call.
        if (callFreezeActive) return

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

    /**
     * A genuine exit is immediate. The former 750 ms grace period is removed:
     * keyboard events are handled explicitly instead of delaying every exit.
     */
    private fun handleOutsideScopeForeground() {
        pauseForegroundBudget(clearForeground = true)
        GuardRuntime.resetForeground()

        cancelPendingLaunches()
        val snapshot = GuardRuntime.interception.snapshot()
        if (!snapshot.guardVisible) {
            GuardRuntime.interception.reset()
        }

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

    /**
     * Latest actual foreground/window owner wins. We always pause the previous
     * package before starting another, so PiP/split-screen cannot debit two budgets.
     */
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
            if (!callFreezeActive &&
                foregroundUnlockedPackage == null &&
                ProtectedApps.isProtected(this, packageName) &&
                GuardPrefs.isUnlocked(this, packageName)
            ) {
                GuardPrefs.beginUnlockForeground(this, packageName)
                foregroundUnlockedPackage = packageName
                lastCheckpointElapsedMs = SystemClock.elapsedRealtime()
            }
            return
        }

        // Immediate exact pause of the previous owner.
        foregroundUnlockedPackage?.let {
            GuardPrefs.endUnlockForeground(this, it)
        }
        foregroundUnlockedPackage = null
        foregroundPackage = packageName

        if (!callFreezeActive &&
            ProtectedApps.isProtected(this, packageName) &&
            GuardPrefs.isUnlocked(this, packageName)
        ) {
            GuardPrefs.beginUnlockForeground(this, packageName)
            foregroundUnlockedPackage = packageName
            lastCheckpointElapsedMs = SystemClock.elapsedRealtime()
        }
    }

    private fun pauseForegroundBudget(clearForeground: Boolean) {
        foregroundUnlockedPackage?.let {
            GuardPrefs.endUnlockForeground(this, it)
        }
        foregroundUnlockedPackage = null
        if (clearForeground) {
            foregroundPackage = null
        }
    }

    private fun resumeCurrentProtectedPackageIfEligible() {
        val packageName = foregroundPackage ?: return
        if (!ProtectedApps.isProtected(this, packageName)) return

        if (GuardPrefs.isUnlocked(this, packageName)) {
            GuardPrefs.beginUnlockForeground(this, packageName)
            foregroundUnlockedPackage = packageName
            lastCheckpointElapsedMs = SystemClock.elapsedRealtime()
        } else {
            triggerExpiredGateIfNeeded(packageName)
        }
    }

    private fun triggerExpiredGateIfNeeded(packageName: String) {
        val remaining = GuardPrefs.remainingUnlockMs(this, packageName)
        if (!UnlockBudgetIntegrity.shouldGateOnExpiration(
                remainingMs = remaining,
                targetPackage = packageName,
                foregroundPackage = foregroundPackage,
                isProtected = ProtectedApps.isProtected(this, packageName),
                callFrozen = callFreezeActive
            )
        ) {
            return
        }

        GuardPrefs.expireUnlock(this, packageName)
        GuardRuntime.interception.reset()
        val now = SystemClock.elapsedRealtime()

        if (GuardRuntime.interception.begin(packageName, now)) {
            GuardDiagnostics.log(
                this,
                "USAGE_BUDGET_EXPIRED",
                packageName
            )
            cancelPendingLaunches()
            launchGate(packageName, "time_expired")
            scheduleRetry(packageName, 350L, "expiry_retry_1")
            scheduleRetry(packageName, 900L, "expiry_retry_2")
        }
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
        if (callFreezeActive) return
        if (GuardPrefs.isUnlocked(this, packageName)) return
        if (!ProtectedApps.isProtected(this, packageName)) return
        if (GuardRuntime.externalForegroundPackage() != packageName) return
        if (!GuardRuntime.interception.shouldRetry(packageName) && reason != "initial") return

        if (reason != "initial" && foregroundPackage != packageName) {
            GuardDiagnostics.log(
                this,
                "GATE_RETRY_SKIPPED",
                packageName,
                "target_not_foreground"
            )
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
                GuardDiagnostics.log(
                    this,
                    "GATE_RETRY_SKIPPED",
                    packageName,
                    reason
                )
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
        // Not relied upon for correctness, but if Android does call it we freeze
        // immediately and wait for the next real protected foreground event.
        pauseForegroundBudget(clearForeground = false)
        GuardDiagnostics.log(this, "SERVICE_INTERRUPTED")
    }

    private fun shutdownRuntime() {
        pauseForegroundBudget(clearForeground = true)
        GuardRuntime.resetForeground()

        mainHandler.removeCallbacks(heartbeat)
        mainHandler.removeCallbacks(usageTicker)
        unregisterScreenReceiverIfNeeded()
        unregisterAudioModeListenerIfNeeded()

        guardPrefs?.unregisterOnSharedPreferenceChangeListener(scopePreferenceListener)
        guardPrefs = null
        audioManager = null

        cancelPendingLaunches()
        GuardRuntime.interception.reset()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        GuardHealth.markDisconnected(this)
        GuardDiagnostics.log(this, "SERVICE_UNBOUND")
        shutdownRuntime()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        GuardHealth.markDisconnected(this)
        GuardDiagnostics.log(this, "SERVICE_DESTROYED")
        shutdownRuntime()
        super.onDestroy()
    }
}
