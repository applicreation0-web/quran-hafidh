package com.applicreation0.quransafeguard

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * Debug-only entry point used to capture the real compiled application UI.
 * It is excluded from release builds by the Android debug source set.
 */
class ScreenshotLauncherActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val target = when (intent.getStringExtra(EXTRA_SCREEN)) {
            "dashboard" -> Intent(this, DashboardActivity::class.java)
            "settings" -> {
                GuardPrefs.saveAccessibilityConsent(this)
                Intent(this, MainActivity::class.java)
            }
            "protection" -> Intent(this, ProtectionSetupActivity::class.java)
            "library" -> Intent(this, SpiritualLibraryActivity::class.java)
            "hikam" -> Intent(this, HikamDetailActivity::class.java)
                .putExtra(HikamDetailActivity.EXTRA_HIKMA_ID, "hikam_010")
            "adhkar" -> Intent(this, AdhkarActivity::class.java)
                .putExtra(AdhkarActivity.EXTRA_PERIOD, AdhkarPeriod.MORNING.name)
            "selection" -> Intent(this, ReadingSelectionActivity::class.java)
            "reader" -> Intent(this, MushafReaderActivity::class.java)
                .putExtra(MushafReaderActivity.EXTRA_PAGE, 1)
                .putExtra(MushafReaderActivity.EXTRA_CHALLENGE_KEY, "com.whatsapp")
            else -> Intent(this, DashboardActivity::class.java)
        }

        startActivity(target)
        finish()
    }

    companion object {
        const val EXTRA_SCREEN = "screenshot_screen"
    }
}
