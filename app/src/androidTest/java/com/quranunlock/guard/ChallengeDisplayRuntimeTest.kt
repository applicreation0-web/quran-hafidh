package com.applicreation0.quransafeguard

import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ChallengeDisplayRuntimeTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val device: UiDevice get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val challengeKey = "com.whatsapp"

    @Before
    fun reset() {
        context.getSharedPreferences(GuardPrefs.FILE, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @After
    fun cleanup() {
        DisplayProfileManager.setPreference(context, DisplayProfilePreference.AUTOMATIC)
        context.getSharedPreferences(GuardPrefs.FILE, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun standardChallengeCountsOnlyVisibleForegroundMushaf() {
        exercise(DisplayProfilePreference.STANDARD)
    }

    @Test
    fun forcedEInkChallengeCountsOnlyVisibleForegroundMushaf() {
        exercise(DisplayProfilePreference.EINK)
    }

    private fun exercise(preference: DisplayProfilePreference) {
        DisplayProfileManager.setPreference(context, preference)
        val (pages, index) = SafeguardCyclePrefs.currentPlan(context)
        val page = pages[index]
        val intent = Intent(context, MushafReaderActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(MushafReaderActivity.EXTRA_PAGE, page)
            .putExtra(MushafReaderActivity.EXTRA_CHALLENGE_KEY, challengeKey)

        ActivityScenario.launch<MushafReaderActivity>(intent).use { scenario ->
            eventually(15_000) { probeVisible(scenario) }
            assertFalse(device.hasObject(By.textContains("Tafs")))
            assertFalse(device.hasObject(By.textContains("Audio Al-Husary")))

            val start = GuardPrefs.readingElapsedMs(context, challengeKey, page)
            Thread.sleep(1_250)
            val foreground = GuardPrefs.readingElapsedMs(context, challengeKey, page)
            assertTrue("foreground reading time did not advance", foreground >= start + 700L)

            scenario.moveToState(Lifecycle.State.CREATED)
            Thread.sleep(150)
            val pausedStart = GuardPrefs.readingElapsedMs(context, challengeKey, page)
            Thread.sleep(1_250)
            val pausedEnd = GuardPrefs.readingElapsedMs(context, challengeKey, page)
            assertTrue(
                "background time leaked into the 60-second reading requirement",
                pausedEnd - pausedStart <= 150L
            )

            scenario.moveToState(Lifecycle.State.RESUMED)
            eventually(5_000) {
                GuardPrefs.readingElapsedMs(context, challengeKey, page) > pausedEnd
            }
        }
    }

    private fun probeVisible(scenario: ActivityScenario<MushafReaderActivity>): Boolean {
        val result = evaluate(scenario, MushafRuntimeVisibilityPolicy.probeJavascript()) ?: return false
        return MushafRuntimeVisibilityPolicy.probeResultIsReady(result)
    }

    private fun evaluate(
        scenario: ActivityScenario<MushafReaderActivity>,
        script: String
    ): String? {
        var result: String? = null
        val latch = CountDownLatch(1)
        scenario.onActivity { activity ->
            val web = findWebView(activity.window.decorView)
            if (web == null) {
                latch.countDown()
            } else {
                web.evaluateJavascript(script) {
                    result = it
                    latch.countDown()
                }
            }
        }
        if (!latch.await(5, TimeUnit.SECONDS)) return null
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
