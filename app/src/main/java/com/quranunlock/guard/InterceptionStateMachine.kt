package com.applicreation0.quransafeguard

enum class InterceptionPhase {
    IDLE,
    TARGET_DETECTED,
    GATE_REQUESTED,
    GATE_VISIBLE,
    READING_VISIBLE,
    UNLOCKED
}

data class InterceptionSnapshot(
    val phase: InterceptionPhase,
    val targetPackage: String?,
    val gateVisible: Boolean,
    val readerVisible: Boolean,
    val lastStartedAtMs: Long
) {
    val guardVisible: Boolean
        get() = gateVisible || readerVisible
}

class InterceptionStateMachine {
    private var phase: InterceptionPhase = InterceptionPhase.IDLE
    private var targetPackage: String? = null
    private var gateVisible: Boolean = false
    private var readerVisible: Boolean = false
    private var lastStartedAtMs: Long = Long.MIN_VALUE

    @Synchronized
    fun begin(target: String, nowMs: Long): Boolean {
        if (gateVisible || readerVisible) return false

        val sameTargetRecently =
            targetPackage == target &&
                lastStartedAtMs != Long.MIN_VALUE &&
                nowMs >= lastStartedAtMs &&
                nowMs - lastStartedAtMs < 1_500L

        if (sameTargetRecently) return false

        targetPackage = target
        lastStartedAtMs = nowMs
        phase = InterceptionPhase.TARGET_DETECTED
        return true
    }

    @Synchronized
    fun markGateRequested(target: String) {
        if (targetPackage == target) {
            phase = InterceptionPhase.GATE_REQUESTED
        }
    }

    @Synchronized
    fun markGateVisible(target: String) {
        targetPackage = target
        gateVisible = true
        phase = InterceptionPhase.GATE_VISIBLE
    }

    @Synchronized
    fun markGateHidden(target: String) {
        if (targetPackage == target) {
            gateVisible = false
            if (readerVisible) phase = InterceptionPhase.READING_VISIBLE
        }
    }

    @Synchronized
    fun markReaderVisible(target: String) {
        targetPackage = target
        readerVisible = true
        phase = InterceptionPhase.READING_VISIBLE
    }

    @Synchronized
    fun markReaderHidden(target: String) {
        if (targetPackage == target) {
            readerVisible = false
            if (gateVisible) phase = InterceptionPhase.GATE_VISIBLE
        }
    }

    @Synchronized
    fun markUnlocked(target: String) {
        if (targetPackage == target) {
            gateVisible = false
            readerVisible = false
            phase = InterceptionPhase.UNLOCKED
        }
    }

    @Synchronized
    fun shouldRetry(target: String): Boolean =
        targetPackage == target &&
            !gateVisible &&
            !readerVisible &&
            phase != InterceptionPhase.UNLOCKED

    @Synchronized
    fun reset() {
        phase = InterceptionPhase.IDLE
        targetPackage = null
        gateVisible = false
        readerVisible = false
        lastStartedAtMs = Long.MIN_VALUE
    }

    @Synchronized
    fun snapshot(): InterceptionSnapshot =
        InterceptionSnapshot(
            phase = phase,
            targetPackage = targetPackage,
            gateVisible = gateVisible,
            readerVisible = readerVisible,
            lastStartedAtMs = lastStartedAtMs
        )
}

object GuardRuntime {
    val interception = InterceptionStateMachine()

    @Volatile
    private var externalForegroundPackage: String? = null

    fun markExternalForeground(packageName: String?) {
        externalForegroundPackage = packageName
    }

    fun externalForegroundPackage(): String? = externalForegroundPackage

    fun resetForeground() {
        externalForegroundPackage = null
    }
}
