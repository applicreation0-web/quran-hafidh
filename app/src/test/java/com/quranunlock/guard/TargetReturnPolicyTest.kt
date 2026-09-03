package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Test

class TargetReturnPolicyTest {
    @Test
    fun normalUnlockRevealsTheExactTriggerTaskWithoutRelaunch() {
        assertEquals(
            TargetReturnRoute.REVEAL_EXISTING_TASK,
            TargetReturnPolicy.route(
                targetPackage = "com.android.chrome",
                lastExternalPackage = "com.android.chrome",
                launcherAvailable = true
            )
        )
    }

    @Test
    fun missingTriggerTaskUsesLauncherFallback() {
        assertEquals(
            TargetReturnRoute.RELAUNCH_TARGET,
            TargetReturnPolicy.route(
                targetPackage = "com.google.android.youtube",
                lastExternalPackage = "com.android.systemui",
                launcherAvailable = true
            )
        )
    }

    @Test
    fun unavailableTargetOnlyClosesSafeguard() {
        assertEquals(
            TargetReturnRoute.CLOSE_SAFEGUARD_ONLY,
            TargetReturnPolicy.route(
                targetPackage = "com.example.uninstalled",
                lastExternalPackage = null,
                launcherAvailable = false
            )
        )
    }

    @Test
    fun blankTargetCanNeverBeLaunched() {
        assertEquals(
            TargetReturnRoute.CLOSE_SAFEGUARD_ONLY,
            TargetReturnPolicy.route(
                targetPackage = "",
                lastExternalPackage = "com.android.chrome",
                launcherAvailable = true
            )
        )
    }
}
