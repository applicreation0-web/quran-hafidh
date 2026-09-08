package com.applicreation0.quransafeguard

import android.content.Context
import android.content.Intent
import android.webkit.WebView
import android.view.ViewGroup
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

@RunWith(AndroidJUnit4::class)
class ReaderLifecycleRuntimeTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Before fun clearReader() {
        context.getSharedPreferences("reader109", Context.MODE_PRIVATE).edit().clear().commit()
        DisplayProfileManager.setPreference(context, DisplayProfilePreference.STANDARD)
    }

    @After fun restoreProfile() {
        DisplayProfileManager.setPreference(context, DisplayProfilePreference.AUTOMATIC)
    }

    @Test fun memorizationRestoresFromPersistedModeWithoutUnmaskedFrame() {
        var before: JSONObject
        ActivityScenario.launch<FreeQuranReaderActivity>(
            Intent(context, FreeQuranReaderActivity::class.java)
                .putExtra(FreeQuranReaderActivity.EXTRA_PAGE, 12)
        ).use { scenario ->
            awaitJs(scenario, "typeof geo==='object' && !document.body.classList.contains('booting')")
            evaluate(scenario, """
                enterMemory();
                const ks=Object.keys(geo.verses).filter(k=>geo.verses[k].includes(page)).slice(0,2);
                selectStart=ks[0];selectEnd=ks[1];begin(ks,selectedLines(ks));
                while(P.current(currentSession()).mask!==100){
                  const s=currentSession(),p=P.current(s);
                  for(let i=0;i<p.min;i++)P.record(s,true);
                  if(!P.validate(s))throw Error('transition refused '+p.id);
                }
                const s=currentSession(),p=P.current(s);
                P.record(s,true);P.record(s,true);P.aid(s,'first-word',1);
                renderMemory();renderMasks();save();
                'ready'
            """)
            awaitJs(scenario, "state.ui.mode==='MEMORIZATION' && document.querySelectorAll('.masklayer rect').length>0")
            before = JSONObject(evaluate(scenario, snapshotScript()))
        }

        // A fresh Activity and WebView are created from the original READING intent.
        ActivityScenario.launch<FreeQuranReaderActivity>(
            Intent(context, FreeQuranReaderActivity::class.java)
                .putExtra(FreeQuranReaderActivity.EXTRA_PAGE, 1)
        ).use { restarted ->
            awaitJs(restarted, "typeof geo==='object' && !document.body.classList.contains('booting') && state.ui.mode==='MEMORIZATION'")
            val after = JSONObject(evaluate(restarted, snapshotScript()))
            listOf("mode","active","session","step","counts","wins","milestones","seed","mask","assistance").forEach {
                assertEquals(it, before.get(it).toString(), after.get(it).toString())
            }
            assertTrue(after.getInt("maskRects") > 0)
            assertEquals(true, after.getBoolean("tafsirHidden"))
            assertEquals(false, after.getBoolean("booting"))
        }
    }

    @Test fun genericCleanerCoversOpaqueWebViewOnlyInEInk() {
        DisplayProfileManager.setPreference(context, DisplayProfilePreference.EINK)
        ActivityScenario.launch<FreeQuranReaderActivity>(
            Intent(context, FreeQuranReaderActivity::class.java)
                .putExtra(FreeQuranReaderActivity.EXTRA_PAGE, 12)
        ).use { scenario ->
            awaitJs(scenario, "typeof geo==='object' && !document.body.classList.contains('booting')")
            assertEquals("true", evaluate(scenario, "String(window.einkFullRefreshFallback())"))
            assertEquals("false", evaluate(scenario, "String(document.getElementById('einkCleaner').hidden)"))
            Thread.sleep(180)
            assertEquals("true", evaluate(scenario, "String(document.getElementById('einkCleaner').hidden)"))
            val beforePageRefresh = evaluate(scenario, "String(window.einkCleanerRuns)").toInt()
            evaluate(scenario, "window.hardwarePage(1);'started'")
            awaitJs(scenario, "page===13 && document.getElementById('pageCount').textContent==='13 / 604' && window.einkCleanerRuns>$beforePageRefresh")
            assertTrue(evaluate(scenario, "String(window.einkCleanerRuns)").toInt() > beforePageRefresh)
        }
    }

    @Test fun standardGenericCleanerIsStrictNoOp() {
        ActivityScenario.launch<FreeQuranReaderActivity>(
            Intent(context, FreeQuranReaderActivity::class.java)
        ).use { scenario ->
            awaitJs(scenario, "typeof geo==='object' && !document.body.classList.contains('booting')")
            assertEquals("false", evaluate(scenario, "String(window.einkFullRefreshFallback())"))
            assertEquals("true", evaluate(scenario, "String(document.getElementById('einkCleaner').hidden)"))
        }
    }

    private fun snapshotScript() = """
        (()=>{const s=currentSession(),p=P.current(s);return JSON.stringify({
          mode:state.ui.mode,active:state.active,session:s.id,step:s.step,
          counts:s.counts,wins:s.wins,milestones:s.milestones,seed:s.seed,
          mask:p.mask,assistance:s.assistance,maskRects:document.querySelectorAll('.masklayer rect').length,
          tafsirHidden:document.getElementById('tafsir').hidden,
          booting:document.body.classList.contains('booting')
        })})()
    """

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
        if (value.startsWith(""")) JSONObject("{\"v\":$value}").getString("v") else value
}
