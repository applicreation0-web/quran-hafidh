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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class EInkAdversarialRuntimeTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    fun forceEInk() {
        DisplayProfileManager.setPreference(context, DisplayProfilePreference.EINK)
    }

    @After
    fun restoreAutomatic() {
        DisplayProfileManager.setPreference(context, DisplayProfilePreference.AUTOMATIC)
    }

    @Test
    fun sentinelPagesRemainVisibleAndPhysicalKeysNeverCrossBounds() {
        val intent = Intent(context, FreeQuranReaderActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(FreeQuranReaderActivity.EXTRA_PAGE, 1)
        ActivityScenario.launch<FreeQuranReaderActivity>(intent).use { scenario ->
            awaitReady(scenario)
            for (page in listOf(1, 2, 304, 305, 499, 604)) {
                js(scenario, "showPage($page);true")
                awaitPage(scenario, page)
                assertEquals("true", js(scenario, "document.getElementById('mushaf').dataset.runtimeReady==='true'"))
                assertEquals("false", js(scenario, "document.body.classList.contains('booting')"))
                assertTrue(js(scenario, "document.querySelectorAll('#mushaf svg path,#mushaf svg text,#mushaf svg use,#mushaf svg rect').length")!!.toInt() >= 8)
            }

            js(scenario, "showPage(1);true")
            awaitPage(scenario, 1)
            scenario.onActivity { activity ->
                assertTrue(activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_PAGE_UP)))
            }
            Thread.sleep(250)
            assertEquals("1", js(scenario, "Number(document.getElementById('progress').value)"))

            js(scenario, "showPage(604);true")
            awaitPage(scenario, 604)
            scenario.onActivity { activity ->
                assertTrue(activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_PAGE_DOWN)))
            }
            Thread.sleep(250)
            assertEquals("604", js(scenario, "Number(document.getElementById('progress').value)"))
        }
    }

    @Test
    fun rapidVisualChurnAndCleanerCoalescingDoNotLoseFinalPage() {
        val intent = Intent(context, FreeQuranReaderActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(FreeQuranReaderActivity.EXTRA_PAGE, 1)
        ActivityScenario.launch<FreeQuranReaderActivity>(intent).use { scenario ->
            awaitReady(scenario)
            val before = js(scenario, "Number(window.einkCleanerRuns||0)")!!.toInt()
            for (page in 2..26) {
                js(scenario, "showPage($page);true")
                awaitPage(scenario, page)
            }
            assertEquals("26", js(scenario, "Number(document.getElementById('progress').value)"))
            assertEquals("true", js(scenario, "document.getElementById('mushaf').dataset.runtimeReady==='true'"))
            eventually(7_000) {
                js(scenario, "Number(window.einkCleanerRuns||0)")!!.toInt() > before
            }
            assertEquals("false", js(scenario, "document.body.classList.contains('booting')"))
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
