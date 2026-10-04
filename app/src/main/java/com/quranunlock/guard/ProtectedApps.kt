package com.applicreation0.quransafeguard

import android.content.Context
import android.content.Intent

enum class SafeguardTargetCategory {
    SOCIAL,
    BROWSER
}

data class SafeguardTarget(
    val label: String,
    val packageName: String,
    val category: SafeguardTargetCategory
)

/**
 * Quran Safeguard has a fixed, narrow product boundary.
 *
 * Only explicitly selected social apps and browsers can be protected or persisted.
 * No banking, health, transport, identity or work application is classified,
 * selectable, logged or persisted. Accessibility is also kept permanently inside
 * that product boundary plus Quran Safeguard, System UI and the current launcher;
 * excluded applications are never admitted as anonymous exit sentinels.
 */
object ProtectedApps {
    const val SYSTEM_UI_PACKAGE = "com.android.systemui"

    val socialTargets: List<SafeguardTarget> = listOf(
        SafeguardTarget("WhatsApp", "com.whatsapp", SafeguardTargetCategory.SOCIAL),
        SafeguardTarget("X", "com.twitter.android", SafeguardTargetCategory.SOCIAL),
        SafeguardTarget("Instagram", "com.instagram.android", SafeguardTargetCategory.SOCIAL),
        SafeguardTarget("Facebook", "com.facebook.katana", SafeguardTargetCategory.SOCIAL),
        SafeguardTarget("YouTube", "com.google.android.youtube", SafeguardTargetCategory.SOCIAL),
        SafeguardTarget("TikTok", "com.zhiliaoapp.musically", SafeguardTargetCategory.SOCIAL)
    )

    val browserTargets: List<SafeguardTarget> = listOf(
        SafeguardTarget("Chrome", "com.android.chrome", SafeguardTargetCategory.BROWSER),
        SafeguardTarget("Firefox", "org.mozilla.firefox", SafeguardTargetCategory.BROWSER),
        SafeguardTarget("Microsoft Edge", "com.microsoft.emmx", SafeguardTargetCategory.BROWSER),
        SafeguardTarget("Brave", "com.brave.browser", SafeguardTargetCategory.BROWSER),
        SafeguardTarget("Opera", "com.opera.browser", SafeguardTargetCategory.BROWSER),
        SafeguardTarget("Samsung Internet", "com.sec.android.app.sbrowser", SafeguardTargetCategory.BROWSER),
        SafeguardTarget("DuckDuckGo", "com.duckduckgo.mobile.android", SafeguardTargetCategory.BROWSER),
        SafeguardTarget("Vivaldi", "com.vivaldi.browser", SafeguardTargetCategory.BROWSER)
    )

    val selectableTargets: List<SafeguardTarget> = socialTargets + browserTargets
    val selectableScopePackages: Set<String> =
        selectableTargets.map(SafeguardTarget::packageName).toSet()
    fun isSelectableTarget(packageName: String): Boolean =
        packageName in selectableScopePackages

    fun shouldNeverPersist(context: Context, packageName: String): Boolean =
        packageName != context.packageName && !isSelectableTarget(packageName)

    private fun launcherPackage(context: Context): String? =
        runCatching {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            context.packageManager.resolveActivity(intent, 0)
                ?.activityInfo
                ?.packageName
                ?.takeIf(String::isNotBlank)
        }.getOrNull()

    fun transitionSignalPackages(context: Context): Set<String> = buildSet {
        add(SYSTEM_UI_PACKAGE)
        launcherPackage(context)?.let(::add)
    }

    fun eventScopePackages(context: Context): Set<String> =
        GuardPrefs.protectedPackages(context) +
            context.packageName +
            transitionSignalPackages(context)

    fun isEventScopePackage(context: Context, packageName: String): Boolean =
        packageName in eventScopePackages(context)

    fun isTransitionSignal(context: Context, packageName: String): Boolean =
        packageName in transitionSignalPackages(context)

    fun isProtected(context: Context, packageName: String): Boolean =
        packageName != context.packageName &&
            isSelectableTarget(packageName) &&
            packageName in GuardPrefs.protectedPackages(context)
}
