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

/**
 * Runtime evidence for the 0.10.9 blank-Mushaf regression.
 *
 * These tests deliberately prove rendered dimensions and runtime-ready state. Merely
 * finding an SVG asset, WebView, DOM node or green boot callback is not sufficient.
 */
@RunWith(AndroidJUnit4::class)
class ReaderRuntimeSentinelAuditTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val readerPrefs get() = context.getSharedPreferences(
        QuranPersistenceNamespaces.FREE_READER_MEMORIZATION,
        Context.MODE_PRIVATE
    )

    @Before fun prepare() {
        readerPrefs.edit().clear().commit()
        context.getSharedPreferences(
            QuranPersistenceNamespaces.FREE_READER_LAST_PAGE,
            Context.MODE_PRIVATE
        ).edit().clear().commit()
        DisplayProfileManager.setPreference(context, DisplayProfilePreference.STANDARD)
    }

    @After fun cleanup() {
        readerPrefs.edit().clear().commit()
        DisplayProfileManager.setPreference(context, DisplayProfilePreference.AUTOMATIC)
    }

    @Test fun sixSentinelPagesAreActuallyRendered() {
        listOf(1, 2, 304, 305, 499, 604).forEach { page ->
            readerPrefs.edit().clear().commit()
            ActivityScenario.launch<FreeQuranReaderActivity>(
                Intent(context, FreeQuranReaderActivity::class.java)
                    .putExtra(FreeQuranReaderActivity.EXTRA_PAGE, page)
            ).use { scenario ->
                awaitReady(scenario)
                val evidence = JSONObject(evaluate(scenario, renderedEvidenceScript()))
                assertEquals("page $page", page, evidence.getInt("page"))
                assertTrue("page $page width", evidence.getDouble("width") > 1.0)
                assertTrue("page $page height", evidence.getDouble("height") > 1.0)
                assertTrue("page $page nodes", evidence.getInt("nodes") >= 8)
                assertTrue("page $page runtime-ready", evidence.getBoolean("runtimeReady"))
                assertFalse("page $page booting", evidence.getBoolean("booting"))
                assertTrue("page $page error hidden", evidence.getBoolean("errorHidden"))
            }
        }
    }

    @Test fun corrupt109PersistenceFallsBackToVisibleMushaf() {
        val cases = listOf(
            "missing-page" to "{\"schema\":1}",
            "page-zero" to "{\"schema\":1,\"page\":0}",
            "page-too-high" to "{\"schema\":1,\"page\":605}",
            "page-null" to "{\"schema\":1,\"page\":null}",
            "page-string" to "{\"schema\":1,\"page\":\"499\"}",
            "partial-json" to "{\"schema\":1,\"page\":499",
            "wrong-schema" to "{\"schema\":99,\"page\":499}",
            "array-root" to "[]"
        )

        cases.forEach { (label, raw) ->
            readerPrefs.edit().clear().putString("state", raw).commit()
            ActivityScenario.launch<FreeQuranReaderActivity>(
                Intent(context, FreeQuranReaderActivity::class.java)
            ).use { scenario ->
                awaitReady(scenario)
                val evidence = JSONObject(evaluate(scenario, renderedEvidenceScript()))
                assertTrue("$label page range", evidence.getInt("page") in 1..604)
                assertTrue("$label width", evidence.getDouble("width") > 1.0)
                assertTrue("$label height", evidence.getDouble("height") > 1.0)
                assertTrue("$label runtime-ready", evidence.getBoolean("runtimeReady"))
                assertFalse("$label booting", evidence.getBoolean("booting"))
                assertTrue("$label error hidden", evidence.getBoolean("errorHidden"))
            }
        }
    }

    @Test fun persistedMemorizationKeepsTafsirUnavailableAndMushafVisible() {
        readerPrefs.edit().putString(
            "state",
            "{\"schema\":1,\"page\":499,\"zoom\":1,\"marks\":[],\"sessions\":[],\"active\":null,\"ui\":{\"mode\":\"MEMORIZATION\",\"selectStart\":null,\"selectEnd\":null}}"
        ).commit()

        ActivityScenario.launch<FreeQuranReaderActivity>(
            Intent(context, FreeQuranReaderActivity::class.java)
        ).use { scenario ->
            awaitReady(scenario)
            val evidence = JSONObject(evaluate(scenario, """
                (()=>{
                  const svg=document.querySelector('#mushaf svg'),r=svg.getBoundingClientRect();
                  return JSON.stringify({page, memory, tafsirHidden:document.getElementById('tafsir').hidden,
                    width:r.width,height:r.height,runtimeReady:document.body.classList.contains('runtime-ready')});
                })()
            """))
            assertEquals(499, evidence.getInt("page"))
            assertTrue(evidence.getBoolean("memory"))
            assertTrue(evidence.getBoolean("tafsirHidden"))
            assertTrue(evidence.getDouble("width") > 1.0)
            assertTrue(evidence.getDouble("height") > 1.0)
            assertTrue(evidence.getBoolean("runtimeReady"))
        }
    }

    @Test fun forcedEInkSurvivesTwentyVisualPageChanges() {
        DisplayProfileManager.setPreference(context, DisplayProfilePreference.EINK)
        ActivityScenario.launch<FreeQuranReaderActivity>(
            Intent(context, FreeQuranReaderActivity::class.java)
                .putExtra(FreeQuranReaderActivity.EXTRA_PAGE, 304)
        ).use { scenario ->
            awaitReady(scenario)
            repeat(20) { index ->
                val target = if (index % 2 == 0) 305 else 304
                evaluate(scenario, "showPage($target);'started'")
                awaitJs(
                    scenario,
                    "page===$target && document.body.classList.contains('runtime-ready') && !document.body.classList.contains('booting')"
                )
                val evidence = JSONObject(evaluate(scenario, renderedEvidenceScript()))
                assertTrue("cycle $index width", evidence.getDouble("width") > 1.0)
                assertTrue("cycle $index height", evidence.getDouble("height") > 1.0)
                assertTrue("cycle $index error hidden", evidence.getBoolean("errorHidden"))
            }
            assertTrue(evaluate(scenario, "String(window.einkCleanerRuns)").toInt() > 0)
        }
    }

    private fun renderedEvidenceScript() = """
        (()=>{
          const svg=document.querySelector('#mushaf svg');
          if(!svg)return JSON.stringify({page,width:0,height:0,nodes:0,runtimeReady:false,booting:true,errorHidden:false});
          const r=svg.getBoundingClientRect();
          return JSON.stringify({
            page,
            width:r.width,
            height:r.height,
            nodes:svg.querySelectorAll('path,text,use,polygon,polyline,line,circle,ellipse,rect').length,
            runtimeReady:document.body.classList.contains('runtime-ready'),
            booting:document.body.classList.contains('booting'),
            errorHidden:document.getElementById('readerError').hidden
          });
        })()
    """

    private fun awaitReady(scenario: ActivityScenario<FreeQuranReaderActivity>) {
        awaitJs(
            scenario,
            "typeof geo==='object' && document.body.classList.contains('runtime-ready') && !document.body.classList.contains('booting') && document.querySelector('#mushaf svg')!==null"
        )
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
