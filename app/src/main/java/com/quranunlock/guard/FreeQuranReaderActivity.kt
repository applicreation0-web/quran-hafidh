package com.applicreation0.quransafeguard

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.brotli.dec.BrotliInputStream
import org.json.JSONObject
import java.io.ByteArrayInputStream

/** Voluntary reader. No protection, budget or unlock API is reachable from its bridge. */
class FreeQuranReaderActivity : ComponentActivity() {
    companion object { const val EXTRA_PAGE = "page"; const val EXTRA_MEMORIZATION = "memorization" }
    private var web: WebView? = null
    private var verse by mutableStateOf<VerseRef?>(null)
    private var expanded by mutableStateOf(false)
    private var memoryMode = false
    private var contextual = false
    private var nativeZoomBaseline = 0f
    private var nativeZoomed = false
    private val prefs by lazy { getSharedPreferences(QuranPersistenceNamespaces.FREE_READER_MEMORIZATION, MODE_PRIVATE) }
    private val displayProfile by lazy { DisplayProfileManager.resolve(this) }
    private val refreshController by lazy { EInkRefreshController(this, displayProfile) }
    private val audio by lazy { QuranAudioController(this) { event -> runOnUiThread { web?.evaluateJavascript("window.audioEvent && window.audioEvent(${event});", null) } } }

    private fun publishNativeZoomState(view: WebView, currentScale: Float) {
        if (currentScale <= 0f) return
        if (nativeZoomBaseline <= 0f) nativeZoomBaseline = currentScale
        val zoomed = kotlin.math.abs(currentScale / nativeZoomBaseline - 1f) > 0.03f
        if (zoomed == nativeZoomed) return
        nativeZoomed = zoomed
        view.evaluateJavascript(
            "window.nativeZoomChanged && window.nativeZoomChanged($zoomed);",
            null
        )
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        Reader109StateSanitizer.migrateOnReaderEntry(this)
        ReaderComfortPrefs.applyBrightness(window, ReaderComfortPrefs.brightness(this))
        contextual = intent.getBooleanExtra("contextual", false)
        val persistedMode = runCatching {
            JSONObject(prefs.getString("state", null) ?: "{}")
                .optJSONObject("ui")?.optString("mode")
        }.getOrNull()
        memoryMode = !contextual && (
            intent.getBooleanExtra(EXTRA_MEMORIZATION, false) ||
                persistedMode == "MEMORIZATION"
            )
        setContent {
            QuranSafeguardTheme {
                BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                    val availableHeight = maxHeight
                    AndroidView(modifier = Modifier.fillMaxSize(), factory = { context ->
                        WebView(context).apply {
                            web = this
                            setBackgroundColor(android.graphics.Color.parseColor("#F7F2E8"))
                            settings.javaScriptEnabled = true
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            settings.domStorageEnabled = false
                            settings.blockNetworkLoads = true
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            addJavascriptInterface(ReaderBridge(), "QsgNative")
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = true
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    view?.post {
                                        nativeZoomBaseline = view.scale.takeIf { it > 0f } ?: 1f
                                        nativeZoomed = false
                                        view.evaluateJavascript(
                                            "window.nativeZoomChanged && window.nativeZoomChanged(false);",
                                            null
                                        )
                                    }
                                }
                                override fun onScaleChanged(view: WebView?, oldScale: Float, newScale: Float) {
                                    super.onScaleChanged(view, oldScale, newScale)
                                    view ?: return
                                    if (nativeZoomBaseline <= 0f) {
                                        nativeZoomBaseline = oldScale.takeIf { it > 0f } ?: newScale
                                    }
                                    publishNativeZoomState(view, newScale)
                                }
                                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse {
                                    val uri = request?.url
                                    if(uri?.scheme != "https" || uri.host != "quran-safeguard.local") return denied()
                                    val path = uri.path.orEmpty().removePrefix("/")
                                    return try {
                                        when {
                                            path.matches(Regex("reader109/[a-z0-9_.-]+")) -> WebResourceResponse(if(path.endsWith(".js")) "application/javascript" else if(path.endsWith(".json")) "application/json" else "text/html", "UTF-8", assets.open(path))
                                            path.matches(Regex("page/[0-9]{1,3}")) -> {
                                                val p = path.substringAfter('/').toInt(); require(p in 1..604)
                                                WebResourceResponse("image/svg+xml", "UTF-8", BrotliInputStream(assets.open("mushaf/hafs/kfqc/svg-br/%03d.svg.br".format(p))))
                                            }
                                            else -> denied()
                                        }
                                    } catch (_: Exception) { denied() }
                                }
                            }
                            loadUrl("https://quran-safeguard.local/reader109/index.html")
                        }
                    })
                    val selected = verse
                    if(selected != null && !memoryMode && TafsirEdition.isEnabled) {
                        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                            TextButton(onClick = { closeTafsir() }) { Text("Fermer") }
                            TafsirEdition.Panel(selected, TafsirLoadState.Closed, Modifier.fillMaxWidth(),
                                maxPanelHeight = availableHeight * if(expanded) .88f else .58f,
                                expanded = expanded, onExpandedChange = { expanded = it },
                                onPanelTopInWindow = { top ->
                                    val loc = IntArray(2);web?.getLocationInWindow(loc)
                                    val overlap = ((web?.height ?: 0)+loc[1]-top).coerceAtLeast(0)
                                    web?.evaluateJavascript("window.setOcclusion($overlap);",null)
                                }, onQuranReferenceSelected = { ref ->
                                    if(!memoryMode) startActivity(Intent(this@FreeQuranReaderActivity, FreeQuranReaderActivity::class.java).apply {
                                        putExtra("contextual",true);putExtra("surah",ref.surah);putExtra("ayah",ref.startAyah)
                                    })
                                })
                        }
                    }
                    BackHandler {
                        if(verse != null) {
                            if(contextual) finish() else closeTafsir()
                        } else web?.evaluateJavascript("window.handleBack();", null)
                    }
                }
            }
        }
    }
    private fun denied() = WebResourceResponse("text/plain", "UTF-8", 403, "Blocked", emptyMap(), ByteArrayInputStream(ByteArray(0)))
    private fun closeTafsir(){verse=null;expanded=false;web?.evaluateJavascript("window.setOcclusion(0);",null)}
    inner class ReaderBridge {
        @JavascriptInterface fun initial(): String = JSONObject().apply {
            put("state",prefs.getString("state",null));put("plus",TafsirEdition.isEnabled)
            put("page",intent.getIntExtra(EXTRA_PAGE,getSharedPreferences(QuranPersistenceNamespaces.FREE_READER_LAST_PAGE,MODE_PRIVATE).getInt("last_page",1)).coerceIn(1,604))
            put("legacyBookmarks",org.json.JSONArray(QuranBookmarkStore.load(this@FreeQuranReaderActivity).sorted()))
            put("memory",memoryMode);put("contextual",contextual);put("surah",intent.getIntExtra("surah",0));put("ayah",intent.getIntExtra("ayah",0))
            put("audio",audio.available)
            put("audioReciter",QuranAudioSource.RECITER_NAME)
            put("audioSource",QuranAudioSource.SOURCE_LABEL)
            put("displayProfile",displayProfile.name)
            put("brightness",ReaderComfortPrefs.brightness(this@FreeQuranReaderActivity).toDouble())
        }.toString()
        @JavascriptInterface fun save(data: String): Boolean {
            if(data.length>2_000_000)return false
            return runCatching { val o=JSONObject(data);if(o.optInt("schema")!=1)return false;prefs.edit().putString("state",data).commit() }.getOrDefault(false)
        }
        @JavascriptInterface fun setMode(memory: Boolean){runOnUiThread { memoryMode=memory;if(memory)closeTafsir() }}
        @JavascriptInterface fun setBrightness(value: Double){runOnUiThread {
            val adjusted = if(value < 0.0) -1f else value.toFloat().coerceIn(.12f,1f)
            ReaderComfortPrefs.setBrightness(this@FreeQuranReaderActivity, if(adjusted < 0f) null else adjusted)
            ReaderComfortPrefs.applyBrightness(window, adjusted)
        }}
        @JavascriptInterface fun visualChange(kind: String){runOnUiThread {
            val change = runCatching { VisualChange.valueOf(kind) }.getOrNull() ?: return@runOnUiThread
            refreshController.onVisualChange(web, change)
        }}
        @JavascriptInterface fun tafsir(surah:Int,ayah:Int){runOnUiThread {
            if(!memoryMode&&TafsirEdition.isEnabled&&surah in 1..114 && ayah in 1..286){verse=VerseRef(surah,ayah);expanded=false}
        }}
        @JavascriptInterface fun exit(){runOnUiThread { finish() }}
        @JavascriptInterface fun play(surah:Int,ayah:Int,repeats:Int){runOnUiThread { audio.playVerse(surah,ayah,repeats.coerceIn(1,100)) }}
        @JavascriptInterface fun pause(){runOnUiThread { audio.pause() }}
        @JavascriptInterface fun resume(){runOnUiThread { audio.resume() }}
        @JavascriptInterface fun isDownloaded(surah:Int,ayah:Int):Boolean = audio.isDownloaded(surah,ayah)
        @JavascriptInterface fun downloadVerse(surah:Int,ayah:Int){audio.downloadVerse(surah,ayah)}
        @JavascriptInterface fun downloadSurah(surah:Int,ayahCount:Int){audio.downloadSurah(surah,ayahCount)}
        @JavascriptInterface fun deleteSurah(surah:Int,ayahCount:Int){audio.deleteSurah(surah,ayahCount)}
        @JavascriptInterface fun announce(surah:Int,ayah:Int){runOnUiThread { web?.announceForAccessibility("Sourate $surah, verset $ayah") }}
    }
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if(displayProfile == DisplayProfile.EINK && event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            val delta = when(event.keyCode) {
                KeyEvent.KEYCODE_PAGE_UP -> -1
                KeyEvent.KEYCODE_PAGE_DOWN -> 1
                else -> 0
            }
            if(delta != 0) {
                web?.evaluateJavascript("window.hardwarePage && window.hardwarePage($delta);", null)
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }
    override fun onPause(){audio.pause();super.onPause()}
    override fun onDestroy(){refreshController.dispose();web?.removeJavascriptInterface("QsgNative");web?.destroy();web=null;audio.release();super.onDestroy()}
}
