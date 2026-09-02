package com.applicreation0.quransafeguard

object SensitiveHandoffPolicy {
    const val HANDOFF_WINDOW_MS = 120_000L

    fun isTrustedBrowserAuthenticationWindow(
        packageName: String,
        className: String?
    ): Boolean {
        if (packageName !in BrowserDetector.supportedPackages) return false
        val normalized = className.orEmpty().lowercase()
        return listOf(
            "customtab",
            "customtabs",
            "auth",
            "oauth",
            "webauthn",
            "payment",
            "checkout",
            "trustedwebactivity"
        ).any(normalized::contains)
    }

    fun isTrustedSensitiveSettingsWindow(className: String?): Boolean {
        val normalized = className.orEmpty().lowercase()
        if (normalized.contains("accessibility") ||
            normalized.contains("installedservice")
        ) {
            return false
        }
        return listOf(
            "biometric",
            "fingerprint",
            "face",
            "credential",
            "permission",
            "security",
            "lockscreen",
            "nfc",
            "identity"
        ).any(normalized::contains)
    }

    fun shouldAllowHandoff(
        originPackage: String?,
        validUntilElapsedMs: Long,
        nowElapsedMs: Long,
        eventPackage: String,
        eventClassName: String?
    ): Boolean {
        if (originPackage.isNullOrBlank()) return false
        if (nowElapsedMs > validUntilElapsedMs) return false
        if (eventPackage == originPackage) return true
        if (eventPackage == ProtectedApps.ANDROID_SETTINGS) {
            return isTrustedSensitiveSettingsWindow(eventClassName)
        }
        return isTrustedBrowserAuthenticationWindow(eventPackage, eventClassName)
    }
}
