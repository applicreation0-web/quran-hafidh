package com.applicreation0.quransafeguard

import android.os.ParcelFileDescriptor
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.BufferedReader
import java.io.InputStreamReader
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runtime accessibility/layout stress for the 0.10.9 clipping regressions.
 *
 * Vertical scrolling is intentionally allowed. The invariant under test is
 * that rendered/accessible content never escapes the physical screen
 * horizontally, including at 150% Android font scale.
 */
@RunWith(AndroidJUnit4::class)
class SettingsUiScaleRuntimeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @After
    fun restoreDisplay() {
        shell("settings put system font_scale 1.0")
        shell("wm size reset")
        instrumentation.waitForIdleSync()
    }

    @Test
    fun settingsRemainInsideViewportAt100130And150Percent() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        GuardPrefs.saveAccessibilityConsent(context)

        listOf("1.0", "1.3", "1.5").forEach { scale ->
            shell("wm size 900x1600")
            shell("settings put system font_scale $scale")
            Thread.sleep(350)

            ActivityScenario.launch(SettingsHubActivity::class.java).use {
                instrumentation.waitForIdleSync()
                val top = hierarchy()
                assertTrue("Settings title missing at font scale $scale", top.contains("Réglages"))
                assertHorizontalBoundsInsideDisplay(top, scale, "SettingsHub-top")

                val appearance = scrollUntil(
                    "Apparence &amp; confort",
                    alternate = "Apparence & confort"
                )
                assertTrue(
                    "Appearance section missing at font scale $scale",
                    appearance.contains("Apparence &amp; confort") ||
                        appearance.contains("Apparence & confort")
                )
                assertTrue("Comfort action missing at font scale $scale", appearance.contains("Confort"))
                assertTrue("Light action missing at font scale $scale", appearance.contains("Clair"))
                assertTrue("Dark action missing at font scale $scale", appearance.contains("Sombre"))
                assertHorizontalBoundsInsideDisplay(appearance, scale, "SettingsHub-appearance")
            }

            ActivityScenario.launch(MainActivity::class.java).use {
                instrumentation.waitForIdleSync()
                val top = hierarchy()
                assertTrue(
                    "Dashboard settings heading missing at font scale $scale",
                    top.contains("Réglages")
                )
                if (TafsirEdition.isEnabled) {
                    assertTrue(
                        "Plus display profile section missing at font scale $scale",
                        top.contains("Type d’écran") || top.contains("Type d&apos;écran")
                    )
                }
                assertHorizontalBoundsInsideDisplay(top, scale, "MainActivity-top")

                shell("input swipe 450 1350 450 450 500")
                instrumentation.waitForIdleSync()
                val lower = hierarchy()
                assertFalse("Dashboard hierarchy empty after scroll", lower.isBlank())
                assertHorizontalBoundsInsideDisplay(lower, scale, "MainActivity-scroll")
            }
        }
    }

    private fun scrollUntil(primary: String, alternate: String? = null): String {
        repeat(5) {
            val xml = hierarchy()
            if (xml.contains(primary) || (alternate != null && xml.contains(alternate))) {
                return xml
            }
            shell("input swipe 450 1350 450 420 450")
            instrumentation.waitForIdleSync()
        }
        return hierarchy()
    }

    private fun assertHorizontalBoundsInsideDisplay(
        xml: String,
        scale: String,
        screen: String
    ) {
        val size = shell("wm size")
        val effective = Regex("Override size: (\\d+)x(\\d+)")
            .find(size)
            ?: Regex("Physical size: (\\d+)x(\\d+)").find(size)
            ?: error("Unable to read display size: $size")
        val width = effective.groupValues[1].toInt()
        val bounds = Regex("bounds=\"\\[(-?\\d+),(-?\\d+)\\]\\[(-?\\d+),(-?\\d+)\\]\"")
            .findAll(xml)
            .map { it.groupValues.drop(1).map(String::toInt) }
            .toList()
        assertTrue("No rendered bounds found for $screen at $scale", bounds.isNotEmpty())
        bounds.forEach { (left, _, right, _) ->
            assertTrue("$screen left overflow at $scale: $left", left >= 0)
            assertTrue("$screen right overflow at $scale: $right > $width", right <= width)
            assertTrue("$screen invalid horizontal bounds at $scale", right >= left)
        }
    }

    private fun hierarchy(): String {
        shell("uiautomator dump /sdcard/qsg-window.xml")
        return shell("cat /sdcard/qsg-window.xml")
    }

    private fun shell(command: String): String {
        val descriptor = instrumentation.uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { stream ->
            BufferedReader(InputStreamReader(stream)).readText()
        }
    }
}
