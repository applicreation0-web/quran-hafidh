package com.applicreation0.quransafeguard

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.KeyguardManager
import android.content.IntentFilter
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.annotation.RequiresApi

class QuranAccessibilityService : AccessibilityService() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val pendingLaunches = mutableListOf<Runnable>()

    /**
     * Exactly one protected package may advance the shared target-only usage cycle.
     * foregroundPackage is the latest actual app/window owner; an IME never replaces it.
     */
    private var foregroundPackage: String? = null
    private var foregroundUnlockedPackage: String? = null

    private var screenReceiverRegistered = false
    private var guardPrefs: SharedPreferences? = null

    private var audioManager: AudioManager? = null
    private var audioModeListener31: Any? = null
    private var callFreezeActive = false
    private var whatsappCallUiActive = false
    private var lastCheckpointElapsedMs = 0L
    private var pendingOrphanRecovery: OrphanedUnlockRecovery? = null

    private val scopePreferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == GuardPrefs.PROTECTED_PACKAGES) {
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
            try {
                GuardHealth.heartbeat(this@QuranAccessibilityService)
            } catch (error: Exception) {
                reportNonFatal("HEARTBEAT_FAILED", error)
            } finally {
                mainHandler.postDelayed(this, 30_000L)
            }
        }
    }

    private val usageTicker = object : Runnable {
        override fun run() {
            try {
                // Fallback on all Android versions and safety net for listener delivery.
                val currentMode = audioManager?.mode ?: AudioManager.MODE_NORMAL
                handleAudioModeChanged(currentMode)

                if (!callFreezeActive) {
                    val packageName = foregroundUnlockedPackage
                    if (packageName != null) {
                        if (!ProtectedApps.isProtected(
                                this@QuranAccessibilityService,
                                packageName
                            )
                        ) {
                            pauseForegroundBudget(clearForeground = true)
                            applyEventPackageScope(broad = false)
                            GuardDiagnostics.log(
                                this@QuranAccessibilityService,
                                "TARGET_SELECTION_EXPIRED"
                            )
                            return
                        }

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
                            foregroundUnlockedPackage = null
                            applyEventPackageScope(broad = false)
                            showGentleMessage(
                                "15 minutes cumulées dans les applications cibles sont atteintes. Une nouvelle lecture vous permettra de continuer."
                            )
                            triggerExpiredGateIfNeeded(packageName)
                        } else {
                            maybeShowUsageReminder(packageName)
                        }
                    }
                }

            } catch (error: Exception) {
                reportNonFatal("USAGE_TICK_FAILED", error)
            } finally {
                // 250 ms bounds call-state fallback and expiration reaction without
                // writing SharedPreferences on every tick (checkpoints remain 1 s).
                mainHandler.postDelayed(this, 250L)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Every vendor hook is isolated: a non-essential OEM failure must never
        // make Android disable the whole accessibility service after activation.
        val orphanedRecovery = try {
            GuardPrefs.reconcileOrphanedUnlockForeground(this)
        } catch (error: Exception) {
            reportNonFatal("FOREGROUND_RECOVERY_INIT_FAILED", error)
            null
        }

        startupStep("BROWSER_SCOPE_INIT_FAILED") { BrowserDetector.refresh() }
        startupStep("RUNTIME_INIT_FAILED") { GuardRuntime.interception.reset() }
        startupStep("HEALTH_INIT_FAILED") { GuardHealth.markConnected(this) }
        startupStep("DIAGNOSTIC_INIT_FAILED") {
            GuardDiagnostics.log(this, "SERVICE_CONNECTED")
        }

        guardPrefs = try {
            getSharedPreferences(GuardPrefs.FILE, Context.MODE_PRIVATE).also {
                it.registerOnSharedPreferenceChangeListener(scopePreferenceListener)
            }
        } catch (error: Exception) {
            reportNonFatal("PREFERENCE_LISTENER_INIT_FAILED", error)
            null
        }

        audioManager = try {
            getSystemService(AudioManager::class.java)
        } catch (error: Exception) {
            reportNonFatal("AUDIO_SERVICE_INIT_FAILED", error)
            null
        }
        callFreezeActive = try {
            UnlockBudgetIntegrity.shouldFreezeForAudioMode(
                audioManager?.mode ?: AudioManager.MODE_NORMAL
            )
        } catch (error: Exception) {
            reportNonFatal("AUDIO_MODE_INIT_FAILED", error)
            false
        }
        registerAudioModeListenerIfSupported()

        applyEventPackageScope(broad = false)
        registerScreenReceiverIfNeeded()

        if (orphanedRecovery != null) {
            startupStep("FOREGROUND_RECOVERY_ARM_FAILED") {
                if (!callFreezeActive &&
                    isScreenInteractiveAndUnlocked() &&
                    ProtectedApps.isProtected(this, orphanedRecovery.packageName) &&
                    GuardPrefs.isUnlocked(this, orphanedRecovery.packageName)
                ) {
                    // Arm recovery but do not charge downtime until a real event proves
                    // that the same protected app is still the interaction owner.
                    pendingOrphanRecovery = orphanedRecovery
                    foregroundPackage = orphanedRecovery.packageName
                    GuardRuntime.markExternalForeground(orphanedRecovery.packageName)
                    applyEventPackageScope(broad = true)
                    GuardDiagnostics.log(
                        this,
                        "SERVICE_FOREGROUND_RECOVERY_ARMED",
                        orphanedRecovery.packageName
                    )
                }
            }
        }

        mainHandler.removeCallbacks(heartbeat)
        mainHandler.post(heartbeat)
        mainHandler.removeCallbacks(usageTicker)
        mainHandler.post(usageTicker)
    }

    private fun registerScreenReceiverIfNeeded() {
        if (screenReceiverRegistered) return
        try {
            val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(screenReceiver, filter)
            }
            screenReceiverRegistered = true
        } catch (error: Exception) {
            reportNonFatal("SCREEN_RECEIVER_INIT_FAILED", error)
        }
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
        try {
            manager.addOnModeChangedListener(mainExecutor, listener)
            audioModeListener31 = listener
        } catch (error: Exception) {
            reportNonFatal("AUDIO_LISTENER_INIT_FAILED", error)
        }
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
            pauseForegroundBudget(clearForeground = true)
            applyEventPackageScope(broad = false)
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
            // Never revive a stale target merely because the call ended. The
            // next real event from a selected target proves foreground presence
            // and starts the shared budget again.
            foregroundPackage = null
            whatsappCallUiActive = false
            GuardRuntime.resetForeground()
            applyEventPackageScope(broad = false)
        }
    }

    private fun isScreenInteractiveAndUnlocked(): Boolean {
        val power = getSystemService(PowerManager::class.java)
        val keyguard = getSystemService(KeyguardManager::class.java)
        return power?.isInteractive == true && keyguard?.isKeyguardLocked != true
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
        if (AccessibilityStatus.isOtherEditionEnabled(this)) {
            pauseForegroundBudget(clearForeground = true)
            applyEventPackageScope(broad = false)
            return
        }
        try {
            processAccessibilityEvent(event)
        } catch (error: Exception) {
            reportNonFatal("ACCESSIBILITY_EVENT_FAILED", error)
        }
    }

    private fun processAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        val eventClassName = event.className?.toString()

        if (packageName != "com.whatsapp" && whatsappCallUiActive) {
            // The explicit WhatsApp call Activity is no longer the window owner.
            // If a call is genuinely continuing (PiP/background), AudioManager
            // remains the authoritative freeze signal.
            whatsappCallUiActive = false
        }

        if (packageName == "com.whatsapp" &&
            event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) {
            val isCallUi = UnlockBudgetIntegrity.isKnownWhatsAppCallActivity(
                packageName,
                eventClassName
            )
            if (isCallUi) {
                whatsappCallUiActive = true
                pauseForegroundBudget(clearForeground = false)
                foregroundPackage = packageName
                GuardRuntime.markExternalForeground(packageName)
                applyEventPackageScope(broad = true)
                GuardDiagnostics.log(
                    this,
                    "WHATSAPP_CALL_UI_FREEZE_STARTED",
                    packageName,
                    eventClassName.orEmpty()
                )
                return
            }

            // A new non-call WhatsApp Activity proves the VoIP UI has gone.
            if (whatsappCallUiActive &&
                eventClassName?.contains("Activity") == true
            ) {
                whatsappCallUiActive = false
                GuardDiagnostics.log(
                    this,
                    "WHATSAPP_CALL_UI_FREEZE_ENDED",
                    packageName
                )
            }
        }

        // Click/scroll events from inside the call Activity must not restart the
        // WhatsApp budget while the audio mode is still transitioning.
        if (whatsappCallUiActive && packageName == "com.whatsapp") {
            return
        }

        // The active keyboard belongs to another package but is not an app exit.
        // Keep the protected app as the sole budget owner while typing.
        val protectedForeground = foregroundPackage
            ?.takeIf { ProtectedApps.isProtected(this, it) }
        val imePseudoForeground = UnlockBudgetIntegrity.isImePseudoForeground(
            eventPackage = packageName,
            activeImePackage = activeInputMethodPackage(),
            currentProtectedPackage = protectedForeground,
            eventType = event.eventType,
            className = event.className?.toString()
        )

        if (imePseudoForeground) {
            pendingOrphanRecovery?.let { recovery ->
                if (recovery.packageName == protectedForeground) {
                    GuardPrefs.chargeRecoveredForegroundGap(this, recovery)
                    if (GuardPrefs.isUnlocked(this, recovery.packageName) &&
                        !callFreezeActive
                    ) {
                        GuardPrefs.beginUnlockForeground(this, recovery.packageName)
                        foregroundUnlockedPackage = recovery.packageName
                        lastCheckpointElapsedMs = SystemClock.elapsedRealtime()
                    }
                    pendingOrphanRecovery = null
                }
            }
            return
        }

        pendingOrphanRecovery?.let { recovery ->
            if (packageName == recovery.packageName) {
                GuardPrefs.chargeRecoveredForegroundGap(this, recovery)
            }
            // Any first real non-IME event settles the ambiguity. If it is
            // another package, no protected budget is charged for downtime.
            pendingOrphanRecovery = null
        }

        // Fixed-scope model: anything outside selected social/browser targets
        // and Safeguard itself is a one-shot anonymous exit signal. No label lookup,
        // category lookup, diagnostic package entry or persistent association.
        if (!ProtectedApps.isEventScopePackage(this, packageName)) {
            handleOutsideScopeForeground()
            return
        }

        val isProtectedPackage = ProtectedApps.isProtected(this, packageName)

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            (event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED &&
                (isProtectedPackage || packageName == this.packageName)) ||
            ((event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
                event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) &&
                isProtectedPackage)
        ) {
            handleForegroundPackage(packageName)
        }

        if (!isProtectedPackage) {
            // Safeguard and Android transition signals are the only non-target
            // packages admitted to the strict event scope.
            applyEventPackageScope(broad = false)
            return
        }

        // Arm the one-shot exit sentinel only after a selected target has become
        // the sole owner of the shared 15/90-minute presence ledger.
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

    private fun applyEventPackageScope(
        broad: Boolean
    ) {
        try {
            val info = serviceInfo ?: return
            val anonymousExitSentinel =
                TargetPresenceScopePolicy.requiresAnonymousExitSentinel(
                    broadRequested = broad,
                    foregroundPackage = foregroundPackage,
                    runningBudgetPackage = foregroundUnlockedPackage,
                    selectedTargets = GuardPrefs.protectedPackages(this)
                )

            // Android does not emit an "application left" callback for a package-
            // filtered AccessibilityService. While a selected target is actively
            // consuming the shared budget, accept exactly the first outside event
            // as an anonymous exit signal. handleOutsideScopeForeground() pauses
            // the budget and restores the narrow list immediately. Window content
            // retrieval remains disabled and the outside package is never logged.
            info.packageNames = if (anonymousExitSentinel) {
                null
            } else {
                ProtectedApps.eventScopePackages(this).toTypedArray()
            }
            setServiceInfo(info)
        } catch (error: Exception) {
            reportNonFatal("EVENT_SCOPE_UPDATE_FAILED", error)
        }
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
            if (UnlockBudgetIntegrity.shouldStartBudgetOnForegroundEvent(
                    eventPackage = packageName,
                    trackedForegroundPackage = foregroundPackage,
                    runningBudgetPackage = foregroundUnlockedPackage,
                    isProtected = ProtectedApps.isProtected(this, packageName),
                    isUnlocked = GuardPrefs.isUnlocked(this, packageName),
                    callFrozen = callFreezeActive ||
                        whatsappCallUiActive
                )
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


    private fun triggerExpiredGateIfNeeded(packageName: String) {
        val remaining = GuardPrefs.remainingUnlockMs(this, packageName)
        if (!UnlockBudgetIntegrity.shouldGateOnExpiration(
                remainingMs = remaining,
                targetPackage = packageName,
                foregroundPackage = foregroundPackage,
                isProtected = ProtectedApps.isProtected(this, packageName),
                callFrozen = callFreezeActive || whatsappCallUiActive
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
            10 to "Il vous reste 10 min sur le cumul partagé 🌿",
            5 to "Encore 5 min cumulées dans les applications cibles.",
            1 to "Dernière minute cumulée avant la prochaine pause Quran."
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
        if (callFreezeActive || whatsappCallUiActive) return
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

    private fun startupStep(code: String, block: () -> Unit) {
        try {
            block()
        } catch (error: Exception) {
            reportNonFatal(code, error)
        }
    }

    private fun reportNonFatal(code: String, error: Exception) {
        Log.e(TAG, "$code: ${error.javaClass.simpleName}", error)
        runCatching {
            GuardDiagnostics.log(
                this,
                code,
                detail = error.javaClass.simpleName
            )
        }
    }

    override fun onInterrupt() {
        // Not relied upon for correctness, but if Android does call it we freeze
        // immediately and wait for the next real protected foreground event.
        pauseForegroundBudget(clearForeground = true)
        applyEventPackageScope(broad = false)
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
        pendingOrphanRecovery = null
        whatsappCallUiActive = false

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

    private companion object {
        const val TAG = "QuranSafeguardService"
    }
}
