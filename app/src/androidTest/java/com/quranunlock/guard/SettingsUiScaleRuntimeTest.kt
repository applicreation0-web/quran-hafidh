package com.applicreation0.quransafeguard

import android.app.Activity
import android.content.Intent
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
 * Cross-app runtime accessibility/layout stress for the 0.10.9 clipping and
 * "laboratory UI" regressions. Vertical scrolling is valid; horizontal escape
 * from the physical screen is never valid, including at 150% Android font.
 */
@RunWith(AndroidJUnit4::class)
class SettingsUiScaleRuntimeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @After
    fun restoreDisplay() {
        shell("settings put system font_scale 1.0")
        shell("wm size reset")
        instrumentation.waitForIdleSync()
    }

    @Test
    fun criticalSurfacesRemainUsableAt100130And150Percent() {
        GuardPrefs.saveAccessibilityConsent(context)

        listOf("1.0", "1.3", "1.5").forEach { scale ->
            shell("wm size 900x1600")
            shell("settings put system font_scale $scale")
            Thread.sleep(350)

            assertSurface(SettingsHubActivity::class.java, "Réglages", scale, scrolls = 4)
            assertSurface(MainActivity::class.java, "Réglages", scale, scrolls = 3)
            assertSurface(ApplicationsActivity::class.java, "Applications", scale, scrolls = 3)
            assertSurface(ProtectionSetupActivity::class.java, "Activation guidée", scale, scrolls = 3)
            assertSurface(QuranHubActivity::class.java, "Qur’an", scale, scrolls = 3)
            assertSurface(ReadingSelectionActivity::class.java, "Juz / Hizb", scale, scrolls = 4)
            assertSurface(HifzJourneyActivity::class.java, "Parcours Hifz", scale, scrolls = 4)
            assertSurface(AdhkarActivity::class.java, "Adhkâr du matin", scale, scrolls = 3)
            assertSurface(ReadingHistoryActivity::class.java, "Historique", scale, scrolls = 3)
            assertSurface(SpiritualLibraryActivity::class.java, "Bibliothèque spirituelle", scale, scrolls = 2)

            // Plus-only display-profile controls must also remain reachable.
            if (TafsirEdition.isEnabled) {
                ActivityScenario.launch(MainActivity::class.java).use {
                    instrumentation.waitForIdleSync()
                    val xml = scrollUntil("Type d’écran", "Type d&apos;écran", maxScrolls = 4)
                    assertTrue(
                        "Plus display profile section missing at font scale $scale",
                        xml.contains("Type d’écran") || xml.contains("Type d&apos;écran")
                    )
                    assertHorizontalBoundsInsideDisplay(xml, scale, "MainActivity-Plus-profile")
                }
            }
        }
    }

    @Test
    fun readerAndMemorizationChromeStayInsideNarrowViewport() {
        listOf("1.0", "1.5").forEach { scale ->
            shell("wm size 900x1600")
            shell("settings put system font_scale $scale")
            Thread.sleep(300)

            launchReader(memory = false).use {
                instrumentation.waitForIdleSync()
                Thread.sleep(900)
                val xml = hierarchy()
                assertFalse("Reader hierarchy empty at $scale", xml.isBlank())
                assertHorizontalBoundsInsideDisplay(xml, scale, "FreeReader")
            }
            launchReader(memory = true).use {
                instrumentation.waitForIdleSync()
                Thread.sleep(900)
                val xml = hierarchy()
                assertFalse("Memorization hierarchy empty at $scale", xml.isBlank())
                assertHorizontalBoundsInsideDisplay(xml, scale, "Memorization")
            }
        }
    }

    private fun <T : Activity> assertSurface(
        activity: Class<T>,
        expectedText: String,
        scale: String,
        scrolls: Int
    ) {
        ActivityScenario.launch(activity).use {
            instrumentation.waitForIdleSync()
            var xml = hierarchy()
            assertTrue(
                "${activity.simpleName} expected text missing at $scale: $expectedText",
                xml.contains(expectedText) || normalized(xml).contains(normalized(expectedText))
            )
            assertHorizontalBoundsInsideDisplay(xml, scale, "${activity.simpleName}-top")
            repeat(scrolls) { index ->
                shell("input swipe 450 1350 450 420 350")
                instrumentation.waitForIdleSync()
                xml = hierarchy()
                assertFalse("${activity.simpleName} hierarchy empty after scroll $index", xml.isBlank())
                assertHorizontalBoundsInsideDisplay(xml, scale, "${activity.simpleName}-scroll-$index")
            }
        }
    }

    private fun launchReader(memory: Boolean): ActivityScenario<FreeQuranReaderActivity> {
        val intent = Intent(context, FreeQuranReaderActivity::class.java)
            .putExtra(FreeQuranReaderActivity.EXTRA_PAGE, 1)
            .putExtra(FreeQuranReaderActivity.EXTRA_MEMORIZATION, memory)
        return ActivityScenario.launch(intent)
    }

    private fun scrollUntil(
        primary: String,
        alternate: String? = null,
        maxScrolls: Int = 5
    ): String {
        repeat(maxScrolls) {
            val xml = hierarchy()
            if (xml.contains(primary) || (alternate != null && xml.contains(alternate))) return xml
            shell("input swipe 450 1350 450 420 350")
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

    private fun normalized(value: String): String =
        value.replace("&amp;", "&")
            .replace("&apos;", "'")
            .replace("’", "'")

    private fun shell(command: String): String {
        val descriptor = instrumentation.uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { stream ->
            BufferedReader(InputStreamReader(stream)).readText()
        }
    }
}
