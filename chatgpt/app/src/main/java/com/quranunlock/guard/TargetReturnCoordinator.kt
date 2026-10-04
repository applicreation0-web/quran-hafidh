package com.applicreation0.quransafeguard

import android.app.Activity
import android.content.Intent

object TargetReturnCoordinator {
    fun returnImmediately(
        activity: Activity,
        targetPackage: String,
        reason: String
    ) {
        val launchIntent = activity.packageManager
            .getLaunchIntentForPackage(targetPackage)
        val route = TargetReturnPolicy.route(
            targetPackage = targetPackage,
            lastExternalPackage = GuardRuntime.externalForegroundPackage(),
            launcherAvailable = launchIntent != null
        )

        GuardDiagnostics.log(
            activity,
            "TARGET_RETURN",
            targetPackage,
            "reason=$reason route=${route.name}"
        )
        activity.setResult(Activity.RESULT_OK)

        if (route == TargetReturnRoute.RELAUNCH_TARGET && launchIntent != null) {
            launchIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            )
            runCatching { activity.startActivity(launchIntent) }
                .onFailure {
                    GuardDiagnostics.log(
                        activity,
                        "TARGET_RETURN_FALLBACK_FAILED",
                        targetPackage,
                        it.javaClass.simpleName
                    )
                }
        }

        // GateActivity and MushafReaderActivity live in Safeguard's temporary
        // task. Removing it reveals the untouched target task without a result
        // screen, toast or artificial delay.
        activity.finishAndRemoveTask()
    }
}
