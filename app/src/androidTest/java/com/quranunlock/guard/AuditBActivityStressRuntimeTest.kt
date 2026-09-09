package com.applicreation0.quransafeguard

import android.content.Context
import android.content.Intent
import android.view.ViewGroup
import android.webkit.WebView
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Audit B is deliberately not a replay of Audit A: it starts from clean state and
 * attacks user journeys, gesture/function separation, lifecycle and repeated visual churn.
 */
@RunWith(AndroidJUnit4::class)
class AuditBActivityStressRuntimeTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Before fun clean() {
        context.getSharedPreferences("reader109", Context.MODE_PRIVATE).edit().clear().commit()
        DisplayProfileManager.setPreference(context, DisplayProfilePreference.STANDARD)
    }

    @After fun restoreProfile() {
        DisplayProfileManager.setPreference(context, DisplayProfilePreference.AUTOMATIC)
    }

    @Test fun readingAndMemorizationUseExclusiveVerseActionsAcrossEditionBoundary() {
        launch(304).use { scenario ->
            awaitReady(scenario, 304)
            evaluate(scenario, "const k=Object.keys(geo.verses).find(x=>geo.verses[x].includes(page));verseTap(k);'selected'")
            assertEquals("false", evaluate(scenario, "String(!selected)"))
            val plusEdition = evaluate(scenario, "String(Boolean(initial.plus))") == "true"
            assertEquals(
                (!plusEdition).toString(),
                evaluate(scenario, "String(document.getElementById('tafsir').hidden)")
            )

            evaluate(scenario, "enterMemory();'memory'")
            val beforeActive = evaluate(scenario, "String(state.active)")
            evaluate(scenario, "const k=Object.keys(geo.verses).find(x=>geo.verses[x].includes(page));verseTap(k);'focused'")
            assertEquals("true", evaluate(scenario, "String(memory)"))
            assertEquals("true", evaluate(scenario, "String(document.getElementById('tafsir').hidden)"))
            assertEquals(beforeActive, evaluate(scenario, "String(state.active)"))
            assertEquals("true", evaluate(scenario, "String(document.body.classList.contains('runtime-ready'))"))
        }
    }

    @Test fun memorizationMaskRevealReturnAndLifecycleResumeKeepVisibleMushaf() {
        launch(305).use { scenario ->
            awaitReady(scenario, 305)
            evaluate(scenario, """
                enterMemory();
                const ks=Object.keys(geo.verses).filter(k=>geo.verses[k].includes(page)).slice(0,2);
                selectStart=ks[0];selectEnd=ks[1];begin(ks,selectedLines(ks));
                while(P.current(currentSession()) && P.current(currentSession()).mask!==100){
                  const s=currentSession(),p=P.current(s);
                  for(let i=0;i<p.min;i++)P.record(s,true);
                  if(!P.validate(s)) throw Error('transition refused '+p.id);
                }
                renderMemory();renderMasks();'prepared'
            """)
            awaitJs(scenario, "document.querySelectorAll('.masklayer rect').length>0")
            val before = evaluate(scenario, "String(document.querySelectorAll('.masklayer rect').length)").toInt()
            assertTrue(before > 0)
            evaluate(scenario, "briefReveal(currentSession());'revealed'")
            awaitJs(scenario, "document.querySelectorAll('.masklayer rect').length===0")
            Thread.sleep(1_800)
            awaitJs(scenario, "document.querySelectorAll('.masklayer rect').length>0")

            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            awaitJs(scenario, "document.body.classList.contains('runtime-ready') && document.querySelector('#mushaf svg')")
            assertEquals("true", evaluate(scenario, "String(document.getElementById('tafsir').hidden)"))
        }
    }

    @Test fun forcedEInkSurvivesTwentySuccessiveVisualChangesPlusBoundaryPages() {
        DisplayProfileManager.setPreference(context, DisplayProfilePreference.EINK)
        launch(1).use { scenario ->
            awaitReady(scenario, 1)
            val cleanerBefore = evaluate(scenario, "String(window.einkCleanerRuns)").toInt()
            for (target in 2..21) {
                evaluate(scenario, "showPage($target);'started'")
                awaitReady(scenario, target)
            }
            evaluate(scenario, "showPage(604);'edge'")
            awaitReady(scenario, 604)
            evaluate(scenario, "showPage(1);'edge'")
            awaitReady(scenario, 1)
            val cleanerAfter = evaluate(scenario, "String(window.einkCleanerRuns)").toInt()
            assertTrue("generic E-Ink refresh path did not run", cleanerAfter >= cleanerBefore + 22)
            assertEquals("true", evaluate(scenario, "String(document.body.classList.contains('runtime-ready'))"))
        }
    }

    private fun launch(page: Int) = ActivityScenario.launch<FreeQuranReaderActivity>(
        Intent(context, FreeQuranReaderActivity::class.java)
            .putExtra(FreeQuranReaderActivity.EXTRA_PAGE, page)
    )

    private fun awaitReady(scenario: ActivityScenario<FreeQuranReaderActivity>, page: Int) {
        awaitJs(
            scenario,
            "document.body.classList.contains('runtime-ready') && document.getElementById('mushaf').dataset.runtimeReady==='true' && window.page===$page && document.querySelector('#mushaf svg').getBoundingClientRect().width>1"
        )
    }

    private fun awaitJs(scenario: ActivityScenario<FreeQuranReaderActivity>, condition: String) {
        val limit = System.currentTimeMillis() + 20_000L
        while (System.currentTimeMillis() < limit) {
            if (evaluate(scenario, "String(Boolean($condition))") == "true") return
            Thread.sleep(120)
        }
        throw AssertionError("Audit B timed out: $condition")
    }

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
