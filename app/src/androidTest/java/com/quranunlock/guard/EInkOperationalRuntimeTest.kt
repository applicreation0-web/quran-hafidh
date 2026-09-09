package com.applicreation0.quransafeguard

import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class EInkOperationalRuntimeTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val device: UiDevice get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    fun forceEInk() {
        DisplayProfileManager.setPreference(context, DisplayProfilePreference.EINK)
    }

    @After
    fun restoreAutomatic() {
        DisplayProfileManager.setPreference(context, DisplayProfilePreference.AUTOMATIC)
    }

    @Test
    fun forcedEInkSupportsNormalReaderControlsPhysicalKeysAndBack() {
        val intent = Intent(context, FreeQuranReaderActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(FreeQuranReaderActivity.EXTRA_PAGE, 499)
        ActivityScenario.launch<FreeQuranReaderActivity>(intent).use { scenario ->
            awaitReady(scenario)
            assertEquals("true", js(scenario, "document.body.classList.contains('eink')"))
            assertEquals("499", js(scenario, "Number(document.getElementById('progress').value)"))

            js(scenario, "document.getElementById('bookmark').click(); true")
            assertEquals("true", js(scenario, "document.getElementById('bookmark').classList.contains('active')"))

            val zoomBefore = js(scenario, "Number(getComputedStyle(document.documentElement).getPropertyValue('--zoom'))")!!.toDouble()
            js(scenario, "document.getElementById('plus').click(); true")
            val zoomAfter = js(scenario, "Number(getComputedStyle(document.documentElement).getPropertyValue('--zoom'))")!!.toDouble()
            assertTrue(zoomAfter > zoomBefore)

            scenario.onActivity { activity ->
                assertTrue(activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_PAGE_DOWN)))
            }
            awaitPage(scenario, 500)
            scenario.onActivity { activity ->
                assertTrue(activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_PAGE_UP)))
            }
            awaitPage(scenario, 499)

            js(scenario, "document.getElementById('progress').value=304;document.getElementById('progress').dispatchEvent(new Event('change'));true")
            awaitPage(scenario, 304)
            assertTrue(js(scenario, "Number(window.einkCleanerRuns||0)")!!.toInt() > 0)

            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            eventually { scenario.state.name != "RESUMED" }
        }
    }

    @Test
    fun forcedEInkKeepsMemorizationIndependentAndTafsirEditionBounded() {
        val memoryIntent = Intent(context, FreeQuranReaderActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(FreeQuranReaderActivity.EXTRA_PAGE, 2)
            .putExtra(FreeQuranReaderActivity.EXTRA_MEMORIZATION, true)
        ActivityScenario.launch<FreeQuranReaderActivity>(memoryIntent).use { scenario ->
            awaitReady(scenario)
            assertEquals("true", js(scenario, "document.body.classList.contains('eink')"))
            assertEquals("true", js(scenario, "document.getElementById('tafsir').hidden"))
            assertEquals("Mémorisation", js(scenario, "document.getElementById('title').textContent.split(' · ')[0]"))
        }

        val readingIntent = Intent(context, FreeQuranReaderActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(FreeQuranReaderActivity.EXTRA_PAGE, 1)
        ActivityScenario.launch<FreeQuranReaderActivity>(readingIntent).use { scenario ->
            awaitReady(scenario)
            val hidden = js(scenario, "document.getElementById('tafsir').hidden") == "true"
            if (TafsirEdition.isEnabled) {
                assertFalse(hidden)

                // Native bridge must reject an impossible Al-Fatiha reference before any panel opens.
                js(scenario, "QsgNative.tafsir(1,8);true")
                Thread.sleep(250)
                assertFalse(device.hasObject(By.text("Fermer")))

                js(scenario, "document.querySelector('.ayahPolygon[data-verse]').dispatchEvent(new Event('click',{bubbles:true}));true")
                assertEquals("false", js(scenario, "document.getElementById('tafsir').disabled"))
                js(scenario, "document.getElementById('tafsir').click();true")
                assertNotNull(device.wait(Until.findObject(By.text("Fermer")), 5_000))
            } else {
                assertTrue(hidden)
            }
        }

        ActivityScenario.launch(HifzJourneyActivity::class.java).use {
            assertNotNull(device.wait(Until.findObject(By.text("Parcours Hifz")), 5_000))
        }
    }

    private fun awaitReady(scenario: ActivityScenario<FreeQuranReaderActivity>) {
        eventually(15_000) {
            js(scenario, "document.getElementById('mushaf')?.dataset.runtimeReady==='true'") == "true"
        }
    }

    private fun awaitPage(scenario: ActivityScenario<FreeQuranReaderActivity>, page: Int) {
        eventually(10_000) {
            js(scenario, "Number(document.getElementById('progress').value)") == page.toString()
        }
    }

    private fun js(scenario: ActivityScenario<FreeQuranReaderActivity>, script: String): String? {
        var result: String? = null
        val latch = CountDownLatch(1)
        scenario.onActivity { activity ->
            val web = findWebView(activity.window.decorView)
            assertNotNull(web)
            web!!.evaluateJavascript(script) { value ->
                result = value?.trim('"')
                latch.countDown()
            }
        }
        assertTrue("JavaScript callback timed out", latch.await(5, TimeUnit.SECONDS))
        return result
    }

    private fun findWebView(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                findWebView(view.getChildAt(i))?.let { return it }
            }
        }
        return null
    }

    private fun eventually(timeoutMs: Long = 5_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        var last = false
        while (System.currentTimeMillis() < deadline) {
            last = runCatching(condition).getOrDefault(false)
            if (last) return
            Thread.sleep(80)
        }
        assertTrue("condition not reached before timeout", last)
    }
}
