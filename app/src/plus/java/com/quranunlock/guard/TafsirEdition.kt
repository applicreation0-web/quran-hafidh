package com.applicreation0.quransafeguard

import android.content.Context
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONObject
import java.lang.ref.WeakReference

object TafsirEdition {
    const val isEnabled: Boolean = true
    private const val ORIGIN = "https://quran-safeguard.local"
    private const val BRIDGE_NAME = "quranSafeguardVerse"
    private var currentWebView = WeakReference<WebView>(null)

    fun prepareHtml(svgContent: String, pageNumber: Int): String {
        val styledSvg = svgContent + """
            <style>
              html, body, svg { background: ${ReaderComfortPrefs.pageBackground()} !important; }
            </style>
        """.trimIndent()
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            return styledSvg
        }
        return buildString {
            append(styledSvg)
            append(
                """
                <style>
                  .ayahPolygon { pointer-events: all; cursor: pointer; }
                  .ayahPolygon.qsg-selected {
                    fill: #C8CEC8 !important;
                    fill-opacity: .44 !important;
                    stroke: none !important;
                  }
                </style>
                <script>
                  (() => {
                    'use strict';
                    const PAGE = $pageNumber;
                    let pointer = null;
                    let savedPosition = null;
                    const polygons = () => Array.from(document.querySelectorAll('path.ayahPolygon'));
                    const sameVerse = (surah, ayah) => polygons().filter((path) =>
                      Number(path.getAttribute('surah')) === surah &&
                      Number(path.getAttribute('ayah')) === ayah
                    );
                    const clearHighlight = () => polygons().forEach((path) => {
                      path.classList.remove('qsg-selected');
                      path.removeAttribute('aria-pressed');
                    });
                    window.qsgTafsir = {
                      select(surah, ayah) {
                        if (savedPosition === null) savedPosition = { x: window.scrollX, y: window.scrollY };
                        clearHighlight();
                        sameVerse(surah, ayah).forEach((path) => {
                          path.classList.add('qsg-selected');
                          path.setAttribute('aria-pressed', 'true');
                        });
                      },
                      reveal(occludedDevicePixels) {
                        const ratio = window.devicePixelRatio || 1;
                        const occlusion = Math.max(0, occludedDevicePixels / ratio);
                        document.body.style.paddingBottom = occlusion + 'px';
                        const selected = Array.from(document.querySelectorAll('.ayahPolygon.qsg-selected'));
                        if (!selected.length) return;
                        requestAnimationFrame(() => {
                          const bottom = Math.max(...selected.map((path) => path.getBoundingClientRect().bottom));
                          const visibleBottom = window.innerHeight - occlusion - 12;
                          if (bottom > visibleBottom) window.scrollBy({ top: bottom - visibleBottom, behavior: 'smooth' });
                        });
                      },
                      clearAndRestore() {
                        clearHighlight();
                        document.body.style.paddingBottom = '0px';
                        const restore = savedPosition;
                        savedPosition = null;
                        if (restore !== null) requestAnimationFrame(() => window.scrollTo({ left: restore.x, top: restore.y, behavior: 'smooth' }));
                      }
                    };
                    const activate = (path) => {
                      const surah = Number(path.getAttribute('surah'));
                      const ayah = Number(path.getAttribute('ayah'));
                      if (!Number.isInteger(surah) || !Number.isInteger(ayah)) return;
                      window.qsgTafsir.select(surah, ayah);
                      if (window.$BRIDGE_NAME && window.$BRIDGE_NAME.postMessage) {
                        window.$BRIDGE_NAME.postMessage(JSON.stringify({ type: 'verseTap', page: PAGE, surah, ayah }));
                      }
                    };
                    const pathAt = (x, y) => document.elementsFromPoint(x, y)
                      .map((element) => element.closest && element.closest('path.ayahPolygon'))
                      .find(Boolean);
                    document.addEventListener('pointerdown', (event) => {
                      if (pointer !== null || !event.isPrimary) {
                        if (pointer !== null) pointer.cancelled = true;
                        return;
                      }
                      pointer = { id: event.pointerId, x: event.clientX, y: event.clientY, cancelled: false };
                    }, { passive: true });
                    document.addEventListener('pointermove', (event) => {
                      if (pointer === null || pointer.id !== event.pointerId) return;
                      if (Math.hypot(event.clientX - pointer.x, event.clientY - pointer.y) > 12) pointer.cancelled = true;
                    }, { passive: true });
                    document.addEventListener('pointercancel', () => { pointer = null; }, { passive: true });
                    document.addEventListener('pointerup', (event) => {
                      const candidate = pointer;
                      pointer = null;
                      if (candidate === null || candidate.id !== event.pointerId || candidate.cancelled) return;
                      const path = pathAt(event.clientX, event.clientY);
                      if (path) activate(path);
                    }, { passive: true });
                    const firstByVerse = new Set();
                    polygons().forEach((path) => {
                      const surah = Number(path.getAttribute('surah'));
                      const ayah = Number(path.getAttribute('ayah'));
                      const key = surah + ':' + ayah;
                      if (firstByVerse.has(key)) {
                        path.setAttribute('aria-hidden', 'true');
                        return;
                      }
                      firstByVerse.add(key);
                      path.setAttribute('role', 'button');
                      path.setAttribute('tabindex', '0');
                      path.setAttribute('aria-label', 'Sourate ' + surah + ', verset ' + ayah);
                      path.addEventListener('keydown', (event) => {
                        if (event.key === 'Enter' || event.key === ' ') {
                          event.preventDefault();
                          activate(path);
                        }
                      });
                    });
                  })();
                </script>
                """.trimIndent()
            )
        }
    }

    fun configureWebView(
        webView: WebView,
        pageNumber: Int,
        verseIndex: Set<VerseRef>,
        onVerseTapped: (VerseRef) -> Unit
    ) {
        currentWebView = WeakReference(webView)
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) return
        WebViewCompat.addWebMessageListener(webView, BRIDGE_NAME, setOf(ORIGIN)) {
                view, message, sourceOrigin, isMainFrame, _ ->
            if (!isMainFrame || sourceOrigin.toString().trimEnd('/') != ORIGIN) return@addWebMessageListener
            val data = runCatching { JSONObject(message.data.orEmpty()) }.getOrNull()
                ?: return@addWebMessageListener
            if (data.optString("type") != "verseTap") return@addWebMessageListener
            val messagePage = data.strictInt("page") ?: return@addWebMessageListener
            val surah = data.strictInt("surah") ?: return@addWebMessageListener
            val ayah = data.strictInt("ayah") ?: return@addWebMessageListener
            val verse = VerseRef(surah, ayah)
            if (messagePage != pageNumber || verse !in verseIndex) return@addWebMessageListener
            view.post { onVerseTapped(verse) }
        }
    }

    fun selectVerse(verse: VerseRef) {
        currentWebView.get()?.apply {
            evaluateJavascript(
                "window.qsgTafsir && window.qsgTafsir.select(${verse.surah},${verse.ayah});",
                null
            )
            announceForAccessibility("Sourate ${verse.surah}, verset ${verse.ayah}")
        }
    }

    fun revealAbove(panelTopInWindowPx: Int) {
        currentWebView.get()?.let { webView ->
            val location = IntArray(2)
            webView.getLocationInWindow(location)
            val overlap = (location[1] + webView.height - panelTopInWindowPx).coerceAtLeast(0)
            webView.evaluateJavascript("window.qsgTafsir && window.qsgTafsir.reveal($overlap);", null)
        }
    }

    fun closeAndRestore() {
        currentWebView.get()?.evaluateJavascript(
            "window.qsgTafsir && window.qsgTafsir.clearAndRestore();",
            null
        )
    }

    suspend fun referencePage(
        context: Context,
        reference: QuranReferenceRef
    ): Int? = TafsirReferenceNavigation.pageFor(context.applicationContext, reference)

    // Compatibility hook used by the shared reader. Plus data loading is owned
    // exclusively by MultiTafsirPanel so opening Qurtubi/Qushayri cannot trigger
    // an obsolete parallel Jalalayn database read.
    suspend fun load(context: Context, verse: VerseRef): TafsirEntry? = null

    @Composable
    fun Panel(
        verse: VerseRef,
        state: TafsirLoadState,
        modifier: Modifier,
        maxPanelHeight: Dp,
        expanded: Boolean = false,
        onExpandedChange: (Boolean) -> Unit = {},
        onPanelTopInWindow: (Int) -> Unit,
        onQuranReferenceSelected: ((QuranReferenceRef) -> Unit)? = null
    ) {
        MultiTafsirPanel(
            verse = verse,
            modifier = modifier,
            maxPanelHeight = maxPanelHeight,
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            onPanelTopInWindow = onPanelTopInWindow,
            onQuranReferenceSelected = onQuranReferenceSelected
        )
    }

    private fun JSONObject.strictInt(name: String): Int? {
        if (!has(name) || isNull(name)) return null
        return get(name) as? Int
    }
}

