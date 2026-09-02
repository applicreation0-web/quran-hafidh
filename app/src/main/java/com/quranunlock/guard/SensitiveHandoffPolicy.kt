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
        if (eventPackage == ProtectedApps.ANDROID_SETTINGS) return true
        return isTrustedBrowserAuthenticationWindow(eventPackage, eventClassName)
    }
}
