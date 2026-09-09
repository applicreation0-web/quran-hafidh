package com.applicreation0.quransafeguard

import android.content.Context
import android.content.Intent
import android.view.ViewGroup
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ReaderStaleStateRuntimeTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun staleSchemaOneStateWithoutPageRecoversAndDisplaysMushaf() {
        context.getSharedPreferences("reader109", Context.MODE_PRIVATE).edit()
            .putString(
                "state",
                """{"schema":1,"zoom":1,"marks":[],"sessions":[],"active":null,"ui":{"mode":"READING","selectStart":null,"selectEnd":null}}"""
            )
            .commit()

        ActivityScenario.launch<FreeQuranReaderActivity>(
            Intent(context, FreeQuranReaderActivity::class.java)
                .putExtra(FreeQuranReaderActivity.EXTRA_PAGE, 1)
        ).use { scenario ->
            awaitJs(
                scenario,
                "typeof geo==='object' && !document.body.classList.contains('booting') && page===1"
            )
            assertEquals("1 / 604", evaluate(scenario, "document.getElementById('pageCount').textContent"))
            assertEquals("true", evaluate(scenario, "String(document.querySelector('#mushaf > svg') !== null)"))
            assertEquals("true", evaluate(scenario, "String(document.getElementById('mushaf').getBoundingClientRect().width > 100)"))
            assertEquals("true", evaluate(scenario, "String(document.querySelectorAll('#mushaf path').length > 0)"))
        }
    }

    private fun awaitJs(
        scenario: ActivityScenario<FreeQuranReaderActivity>,
        condition: String
    ) {
        val limit = System.currentTimeMillis() + 15_000
        while (System.currentTimeMillis() < limit) {
            if (evaluate(scenario, "String(Boolean($condition))") == "true") return
            Thread.sleep(100)
        }
        throw AssertionError("Timed out waiting for: $condition")
    }

    private fun evaluate(
        scenario: ActivityScenario<FreeQuranReaderActivity>,
        script: String
    ): String {
        val latch = CountDownLatch(1)
        var result = ""
        scenario.onActivity { activity ->
            val web = findWebView(activity.window.decorView as ViewGroup)
                ?: throw AssertionError("WebView not found")
            web.evaluateJavascript(script) {
                result = decode(it)
                latch.countDown()
            }
        }
        assertTrue("JavaScript callback timed out", latch.await(10, TimeUnit.SECONDS))
        return result
    }

    private fun findWebView(group: ViewGroup): WebView? {
        for (i in 0 until group.childCount) {
            val child = group.getChildAt(i)
            if (child is WebView) return child
            if (child is ViewGroup) findWebView(child)?.let { return it }
        }
        return null
    }

    private fun decode(value: String): String =
        if (value.startsWith("\"")) JSONObject("{\"v\":$value}").getString("v") else value
}
