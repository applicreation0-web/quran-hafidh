package com.applicreation0.quransafeguard

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent

object AccessibilityScopePolicy {
    fun eventPackages(
        selectedPackages: Set<String>,
        browserPackages: Set<String>,
        infrastructurePackages: Set<String>,
        alwaysAllowedPackages: Set<String>,
        ownPackage: String
    ): Set<String> {
        val protectedTargets = (selectedPackages + browserPackages)
            .asSequence()
            .filterNot { it == ownPackage }
            .filterNot { it in alwaysAllowedPackages }
            .toSet()

        return protectedTargets + infrastructurePackages + ownPackage
    }
}

object AccessibilityScopeManager {
    const val ACTION_REFRESH_SCOPE =
        "com.applicreation0.quransafeguard.action.REFRESH_ACCESSIBILITY_SCOPE"

    private val safeInfrastructurePackages = setOf(
        "com.android.systemui"
    )

    fun eventPackages(context: Context): Set<String> {
        val homePackage = resolveHomePackage(context)
        val infrastructure = buildSet {
            addAll(safeInfrastructurePackages)
            if (!homePackage.isNullOrBlank()) add(homePackage)
        }

        return AccessibilityScopePolicy.eventPackages(
            selectedPackages = GuardPrefs.protectedPackages(context),
            browserPackages = BrowserDetector.supportedPackages,
            infrastructurePackages = infrastructure,
            alwaysAllowedPackages = ProtectedApps.staticAlwaysAllowedPackages(),
            ownPackage = context.packageName
        )
    }

    fun protectedTargetPackages(context: Context): Set<String> =
        eventPackages(context).filterTo(mutableSetOf()) {
            it != context.packageName &&
                it !in safeInfrastructurePackages &&
                it != resolveHomePackage(context) &&
                !ProtectedApps.isAlwaysAllowed(context, it)
        }

    fun shouldProcessEvent(context: Context, packageName: String): Boolean =
        packageName in eventPackages(context)

    fun applyTo(service: AccessibilityService): Set<String> {
        val packages = eventPackages(service)
        val info = service.serviceInfo ?: return packages

        info.packageNames = packages.sorted().toTypedArray()
        info.eventTypes =
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOWS_CHANGED

        service.serviceInfo = info
        return packages
    }

    fun requestRefresh(context: Context) {
        context.sendBroadcast(
            Intent(ACTION_REFRESH_SCOPE)
                .setPackage(context.packageName)
        )
    }

    private fun resolveHomePackage(context: Context): String? {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        return runCatching {
            context.packageManager
                .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo
                ?.packageName
        }.getOrNull()
    }
}
