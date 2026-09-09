package com.applicreation0.quransafeguard

import android.content.Context
import android.content.Intent
import android.view.ViewGroup
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Audit A: hostile persistence/boundary states plus a normal-mode control journey. */
@RunWith(AndroidJUnit4::class)
class AuditABoundaryRecoveryRuntimeTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val reader get() = context.getSharedPreferences("reader109", Context.MODE_PRIVATE)

    @Before fun reset() {
        reader.edit().clear().commit()
        context.getSharedPreferences("free_quran_reader", Context.MODE_PRIVATE).edit().clear().commit()
        DisplayProfileManager.setPreference(context, DisplayProfilePreference.STANDARD)
    }

    @After fun restore() {
        DisplayProfileManager.setPreference(context, DisplayProfilePreference.AUTOMATIC)
    }

    @Test fun sentinelPagesAreActuallyVisibleNotMerelyLoaded() {
        listOf(1, 2, 304, 305, 499, 604).forEach { target ->
            reader.edit().putString(
                "state",
                """{"schema":1,"page":$target,"zoom":1,"marks":[],"sessions":[],"active":null,"ui":{"mode":"READING","selectStart":null,"selectEnd":null}}"""
            ).commit()
            launch(target).use { scenario ->
                awaitReady(scenario)
                val snap = JSONObject(evaluate(scenario, visibilitySnapshot()))
                assertEquals(target, snap.getInt("page"))
                assertEquals("svg", snap.getString("tag"))
                assertTrue("width page $target", snap.getDouble("width") > 1.0)
                assertTrue("height page $target", snap.getDouble("height") > 1.0)
                assertTrue("content page $target", snap.getInt("nodes") >= 8)
                assertTrue(snap.getBoolean("runtimeReady"))
                assertFalse(snap.getBoolean("errorVisible"))
            }
        }
    }

    @Test fun corrupt109StatesRecoverToDeterministicVisibleFallback() {
        val corrupt = listOf(
            "{",
            """{"schema":1}""",
            """{"schema":1,"page":0}""",
            """{"schema":1,"page":605}""",
            """{"schema":1,"page":null}""",
            """{"schema":1,"page":"499"}""",
            """{"schema":1,"page":499.5}"""
        )
        corrupt.forEach { raw ->
            reader.edit().clear().putString("state", raw).commit()
            launch(2).use { scenario ->
                awaitReady(scenario)
                assertEquals("2", evaluate(scenario, "String(page)"))
                assertEquals("true", evaluate(scenario, "String(document.body.classList.contains('runtime-ready'))"))
                assertEquals("false", evaluate(scenario, "String(!document.getElementById('readerError').hidden)"))
            }
        }
    }

    @Test fun wrongSharedPreferenceTypeCannotCrashBoot() {
        reader.edit().putInt("state", 499).commit()
        context.getSharedPreferences("free_quran_reader", Context.MODE_PRIVATE)
            .edit().putString("last_page", "499").commit()
        launch(1).use { scenario ->
            awaitReady(scenario)
            assertEquals("1", evaluate(scenario, "String(page)"))
            assertTrue(reader.all["state"] is String)
            assertTrue(context.getSharedPreferences("free_quran_reader", Context.MODE_PRIVATE).all["last_page"] is Int)
        }
    }

    @Test fun normalMemorizationTapRemainsExclusiveAndMushafVisible() {
        launch(305).use { scenario ->
            awaitReady(scenario)
            evaluate(scenario, "enterMemory();'ok'")
            evaluate(scenario, "const k=Object.keys(geo.verses).find(x=>geo.verses[x].includes(page));verseTap(k);'ok'")
            assertEquals("true", evaluate(scenario, "String(memory)"))
            assertEquals("true", evaluate(scenario, "String(document.getElementById('tafsir').hidden)"))
            assertEquals("true", evaluate(scenario, "String(document.body.classList.contains('runtime-ready'))"))
            assertEquals("false", evaluate(scenario, "String(!document.getElementById('readerError').hidden)"))
        }
    }

    private fun launch(page: Int) = ActivityScenario.launch<FreeQuranReaderActivity>(
        Intent(context, FreeQuranReaderActivity::class.java)
            .putExtra(FreeQuranReaderActivity.EXTRA_PAGE, page)
    )

    private fun awaitReady(scenario: ActivityScenario<FreeQuranReaderActivity>) {
        val limit = System.currentTimeMillis() + 20_000L
        while (System.currentTimeMillis() < limit) {
            if (evaluate(scenario, "String(Boolean(document.body.classList.contains('runtime-ready') && document.querySelector('#mushaf svg') && document.getElementById('mushaf').dataset.runtimeReady==='true'))") == "true") return
            Thread.sleep(120)
        }
        throw AssertionError("Audit A: reader never reached proven-visible state")
    }

    private fun visibilitySnapshot() = """
        (()=>{const s=document.querySelector('#mushaf svg'),r=s.getBoundingClientRect();return JSON.stringify({
          page,tag:s.localName,width:r.width,height:r.height,
          nodes:s.querySelectorAll('path,text,use,polygon,polyline,line,circle,ellipse,rect').length,
          runtimeReady:document.body.classList.contains('runtime-ready')&&document.getElementById('mushaf').dataset.runtimeReady==='true',
          errorVisible:!document.getElementById('readerError').hidden
        })})()
    """

    private fun evaluate(scenario: ActivityScenario<FreeQuranReaderActivity>, script: String): String {
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
