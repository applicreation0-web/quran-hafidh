package com.applicreation0.quransafeguard

import android.content.Context
import android.content.Intent
import android.view.ViewGroup
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class MushafVisibilityRuntimeTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun clearReaderState() {
        context.getSharedPreferences("reader109", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun freeReaderRendersMushafPageAtUsableViewportSize() {
        ActivityScenario.launch<FreeQuranReaderActivity>(
            Intent(context, FreeQuranReaderActivity::class.java)
                .putExtra(FreeQuranReaderActivity.EXTRA_PAGE, 12)
        ).use { scenario ->
            awaitJs(
                scenario,
                "!document.body.classList.contains('booting') && document.querySelector('#mushaf > svg') !== null"
            )
            val result = JSONObject(
                evaluate(
                    scenario,
                    """
                    (() => {
                      const host = document.getElementById('mushaf');
                      const svg = host.querySelector('svg');
                      const r = host.getBoundingClientRect();
                      const sr = svg.getBoundingClientRect();
                      return JSON.stringify({
                        hostWidth:r.width,
                        hostHeight:r.height,
                        svgWidth:sr.width,
                        svgHeight:sr.height,
                        viewportWidth:window.innerWidth,
                        viewportHeight:window.innerHeight,
                        visibility:getComputedStyle(host).visibility,
                        display:getComputedStyle(svg).display,
                        paths:svg.querySelectorAll('path').length,
                        pageCount:document.getElementById('pageCount').textContent
                      });
                    })()
                    """.trimIndent()
                )
            )
            assertTrue("Mushaf host must be visible", result.getString("visibility") == "visible")
            assertTrue("Mushaf SVG must be displayed", result.getString("display") != "none")
            assertTrue("Mushaf must occupy a usable share of the viewport", result.getDouble("hostWidth") >= result.getDouble("viewportWidth") * 0.45)
            assertTrue("Rendered SVG width must be non-trivial", result.getDouble("svgWidth") >= result.getDouble("viewportWidth") * 0.45)
            assertTrue("Rendered SVG height must be non-trivial", result.getDouble("svgHeight") >= result.getDouble("viewportHeight") * 0.45)
            assertTrue("The page must contain Quran vector paths", result.getInt("paths") > 100)
            assertTrue("Reader must report the requested Mushaf page", result.getString("pageCount") == "12 / 604")
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
