package com.applicreation0.quransafeguard

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import org.brotli.dec.BrotliInputStream

/**
 * Voluntary smartphone reader for Quran Safeguard.
 *
 * Deliberately contains only Lecture/Study + Tafsir. Memorization, Sabqi, Itqan,
 * Murajaah and Hifz audio do not exist in this screen or its state.
 */
class SafeguardStudyReaderActivity : ComponentActivity() {
    private var currentPageForKeys = 1
    private var pageKeyHandler: ((Int) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuranSafeguardTheme {
                StudyReaderScreen(
                    initialPage = intent.getIntExtra("page", 1).coerceIn(1, 604),
                    onPageChanged = { page -> currentPageForKeys = page },
                    registerPageHandler = { handler -> pageKeyHandler = handler }
                )
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_PAGE_DOWN -> {
                pageKeyHandler?.invoke((currentPageForKeys + 1).coerceAtMost(604))
                true
            }
            KeyEvent.KEYCODE_PAGE_UP -> {
                pageKeyHandler?.invoke((currentPageForKeys - 1).coerceAtLeast(1))
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun loadMushafPage(page: Int): String? {
        if (page !in 1..604) return null
        val assetPath = "mushaf/hafs/kfqc/svg-br/%03d.svg.br".format(page)
        return runCatching {
            assets.open(assetPath).use { compressed ->
                BrotliInputStream(compressed)
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }
            }
        }.getOrNull()
    }

    @Composable
    private fun StudyReaderScreen(
        initialPage: Int,
        onPageChanged: (Int) -> Unit,
        registerPageHandler: (((Int) -> Unit) -> Unit)
    ) {
        var page by remember { mutableIntStateOf(initialPage) }
        var selectedVerse by remember { mutableStateOf<VerseRef?>(null) }
        var loadFailed by remember(page) { mutableStateOf(false) }
        val svg = remember(page) { loadMushafPage(page) }

        fun goToPage(next: Int) {
            selectedVerse = null
            TafsirEdition.closeAndRestore()
            page = next.coerceIn(1, 604)
            onPageChanged(page)
        }

        registerPageHandler(::goToPage)
        BackHandler(enabled = selectedVerse != null) {
            selectedVerse = null
            TafsirEdition.closeAndRestore()
        }

        Surface(modifier = Modifier.fillMaxSize(), color = SafeguardReadingSurface) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Lecture / Étude", style = MaterialTheme.typography.titleMedium)
                        Text("Page $page / 604", style = MaterialTheme.typography.bodyMedium)
                    }

                    val selected = selectedVerse
                    Text(
                        if (selected == null) {
                            "Touchez un verset pour ouvrir le Tafsir."
                        } else {
                            "Sourate ${selected.surah} • verset ${selected.ayah}"
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.bodySmall
                    )

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (!svg.isNullOrBlank() && !loadFailed) {
                            StudyMushafWebView(
                                svgContent = svg,
                                pageNumber = page,
                                tafsirOpen = selected != null,
                                modifier = Modifier.fillMaxSize(),
                                onSwipePrevious = { if (page > 1) goToPage(page - 1) },
                                onSwipeNext = { if (page < 604) goToPage(page + 1) },
                                onVerseTapped = { verse ->
                                    selectedVerse = verse
                                    TafsirEdition.selectVerse(verse)
                                },
                                onFailure = { loadFailed = true }
                            )
                        } else {
                            Text(
                                "La page du Mushaf n’est pas disponible.",
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        selected?.let { verse ->
                            TafsirEdition.Panel(
                                verse = verse,
                                state = TafsirLoadState.Loading,
                                modifier = Modifier.align(Alignment.BottomCenter),
                                maxPanelHeight = maxHeight * 0.5f,
                                onPanelTopInWindow = { top -> TafsirEdition.revealAbove(top) },
                                onQuranReferenceSelected = null
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            enabled = selectedVerse == null && page > 1,
                            onClick = { goToPage(page - 1) }
                        ) { Text("Page précédente") }
                        Button(
                            modifier = Modifier.weight(1f),
                            enabled = selectedVerse == null && page < 604,
                            onClick = { goToPage(page + 1) }
                        ) { Text("Page suivante") }
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
private fun StudyMushafWebView(
    svgContent: String,
    pageNumber: Int,
    tafsirOpen: Boolean,
    modifier: Modifier,
    onSwipePrevious: () -> Unit,
    onSwipeNext: () -> Unit,
    onVerseTapped: (VerseRef) -> Unit,
    onFailure: () -> Unit
) {
    val currentTafsirOpen = rememberUpdatedState(tafsirOpen)
    val currentOnSwipePrevious = rememberUpdatedState(onSwipePrevious)
    val currentOnSwipeNext = rememberUpdatedState(onSwipeNext)
    val currentOnVerseTapped = rememberUpdatedState(onVerseTapped)
    val currentOnFailure = rememberUpdatedState(onFailure)

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(android.graphics.Color.parseColor(ReaderComfortPrefs.pageBackground()))
                settings.javaScriptEnabled = TafsirEdition.isEnabled
                settings.domStorageEnabled = false
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.blockNetworkLoads = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true

                val swipeThreshold = 72f * resources.displayMetrics.density
                val gestures = ReaderGestureClassifier(swipeThreshold)
                setOnTouchListener { _, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> gestures.onDown(event.x, event.y, event.pointerCount)
                        MotionEvent.ACTION_MOVE -> gestures.onMove(event.x, event.y, event.pointerCount)
                        MotionEvent.ACTION_POINTER_DOWN -> gestures.onAdditionalPointer()
                        MotionEvent.ACTION_CANCEL -> gestures.onCancel()
                        MotionEvent.ACTION_UP -> when (
                            gestures.onUp(
                                event.x,
                                event.y,
                                gesturesEnabled = !currentTafsirOpen.value
                            )
                        ) {
                            ReaderSwipe.NEXT -> currentOnSwipeNext.value()
                            ReaderSwipe.PREVIOUS -> currentOnSwipePrevious.value()
                            null -> Unit
                        }
                    }
                    false
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?, request: WebResourceRequest?
                    ): Boolean = true

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: android.webkit.WebResourceError?
                    ) {
                        if (request?.isForMainFrame == true) currentOnFailure.value()
                    }
                }

                val html = """
                    <!doctype html>
                    <html dir="rtl">
                    <head>
                      <meta name="viewport" content="width=device-width, initial-scale=1.0">
                      <style>
                        html, body {
                          margin: 0; padding: 0;
                          background: ${ReaderComfortPrefs.READER_CREAM_HEX};
                          width: 100%; min-height: 100%; overflow-x: hidden;
                        }
                        svg { display: block; width: 100%; height: auto; max-width: 100%; }
                      </style>
                    </head>
                    <body>${TafsirEdition.prepareHtml(svgContent, pageNumber)}</body>
                    </html>
                """.trimIndent()

                TafsirEdition.configureWebView(
                    webView = this,
                    pageNumber = pageNumber,
                    verseIndex = MushafVerseIndex.fromSvg(svgContent),
                    onVerseTapped = { verse -> currentOnVerseTapped.value(verse) }
                )
                loadDataWithBaseURL(
                    "https://quran-safeguard.local/",
                    html,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        },
        update = { webView ->
            webView.setBackgroundColor(android.graphics.Color.parseColor(ReaderComfortPrefs.pageBackground()))
        }
    )
}
