package com.applicreation0.quransafeguard

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import org.brotli.dec.BrotliInputStream

/**
 * Voluntary Qur'an reader used outside Safeguard challenges.
 *
 * This activity deliberately has no challenge key and never calls GuardPrefs reading
 * validation or unlock APIs. Reading here therefore cannot credit an app-unlock quota.
 * Tafsir Quran references open a second internal, read-only instance of this reader;
 * the original activity remains alive so Back returns to the exact commentary state.
 */
class FreeQuranReaderActivity : ComponentActivity() {
    companion object {
        const val EXTRA_PAGE = "page"
        private const val EXTRA_REFERENCE_MODE = "reference_mode"
        private const val EXTRA_REFERENCE_SURAH = "reference_surah"
        private const val EXTRA_REFERENCE_START_AYAH = "reference_start_ayah"
        private const val EXTRA_REFERENCE_END_AYAH = "reference_end_ayah"
        private const val PREFS = "free_quran_reader"
        private const val KEY_LAST_PAGE = "last_page"
        private const val FIRST_PAGE = 1
        private const val LAST_PAGE = 604
    }

    private var selectedTafsirVerse by mutableStateOf<VerseRef?>(null)
    private var tafsirLoadState by mutableStateOf<TafsirLoadState>(TafsirLoadState.Closed)
    private var preserveTafsirOnNextPause = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val reference = if (intent.getBooleanExtra(EXTRA_REFERENCE_MODE, false)) {
            val surah = intent.getIntExtra(EXTRA_REFERENCE_SURAH, 0)
            val start = intent.getIntExtra(EXTRA_REFERENCE_START_AYAH, 0)
            val end = intent.getIntExtra(EXTRA_REFERENCE_END_AYAH, start)
            if (surah > 0 && start > 0 && end >= start) {
                QuranReferenceRef(surah, start, end)
            } else {
                null
            }
        } else {
            null
        }
        val referenceMode = reference != null
        val requestedPage = intent.getIntExtra(EXTRA_PAGE, 0)
        val storedPage = getSharedPreferences(PREFS, MODE_PRIVATE)
            .getInt(KEY_LAST_PAGE, FIRST_PAGE)
        val initialPage = when {
            requestedPage in FIRST_PAGE..LAST_PAGE -> requestedPage
            !referenceMode && storedPage in FIRST_PAGE..LAST_PAGE -> storedPage
            else -> FIRST_PAGE
        }

        setContent {
            QuranSafeguardTheme {
                val coroutineScope = rememberCoroutineScope()
                var page by remember { mutableIntStateOf(initialPage) }
                var message by remember {
                    mutableStateOf(
                        if (referenceMode) {
                            "Référence coranique ${reference!!.label} • retour pour reprendre le commentaire."
                        } else {
                            "Mushaf arabe : balayez vers la droite pour avancer, " +
                                "vers la gauche pour revenir."
                        }
                    )
                }

                BackHandler(enabled = selectedTafsirVerse != null) {
                    closeTafsir()
                }

                LaunchedEffect(selectedTafsirVerse) {
                    val requestedVerse = selectedTafsirVerse ?: return@LaunchedEffect
                    tafsirLoadState = TafsirLoadState.Loading
                    val entry = TafsirEdition.load(
                        this@FreeQuranReaderActivity,
                        requestedVerse
                    )
                    if (selectedTafsirVerse == requestedVerse) {
                        tafsirLoadState = if (entry == null) {
                            TafsirLoadState.Unavailable
                        } else {
                            TafsirLoadState.Available(entry)
                        }
                    }
                }

                fun showPage(nextPage: Int) {
                    if (referenceMode || selectedTafsirVerse != null) return
                    if (nextPage !in FIRST_PAGE..LAST_PAGE) {
                        message = if (nextPage < FIRST_PAGE) {
                            "Vous êtes sur la première page du Mushaf."
                        } else {
                            "Vous êtes sur la dernière page du Mushaf."
                        }
                        return
                    }
                    page = nextPage
                    getSharedPreferences(PREFS, MODE_PRIVATE)
                        .edit()
                        .putInt(KEY_LAST_PAGE, page)
                        .apply()
                    message =
                        "Lecture libre • appuyez sur un verset pour ouvrir le Tafsîr."
                }

                fun openReference(referenceToOpen: QuranReferenceRef) {
                    if (referenceMode || !TafsirEdition.isEnabled) return
                    coroutineScope.launch {
                        val referencePage = TafsirEdition.referencePage(
                            this@FreeQuranReaderActivity,
                            referenceToOpen
                        )
                        if (referencePage == null) {
                            message = "Référence ${referenceToOpen.label} : page du Mushaf introuvable."
                            return@launch
                        }
                        preserveTafsirOnNextPause = true
                        startActivity(
                            Intent(
                                this@FreeQuranReaderActivity,
                                FreeQuranReaderActivity::class.java
                            ).apply {
                                putExtra(EXTRA_PAGE, referencePage)
                                putExtra(EXTRA_REFERENCE_MODE, true)
                                putExtra(EXTRA_REFERENCE_SURAH, referenceToOpen.surah)
                                putExtra(EXTRA_REFERENCE_START_AYAH, referenceToOpen.startAyah)
                                putExtra(EXTRA_REFERENCE_END_AYAH, referenceToOpen.endAyah)
                            }
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SafeguardReadingSurface
                ) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 6.dp)
                        ) {
                            Text(
                                if (referenceMode) {
                                    "Référence Qur’an • ${reference!!.label}"
                                } else {
                                    "Qur’an & Tafsîr"
                                },
                                modifier = Modifier.padding(horizontal = 12.dp),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Mushaf de Médine • Page $page / $LAST_PAGE",
                                modifier = Modifier.padding(horizontal = 12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (TafsirEdition.isEnabled) message else
                                    "Lecture libre du Mushaf de Médine.",
                                modifier = Modifier.padding(horizontal = 12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))

                            AnimatedContent(
                                targetState = page,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                transitionSpec = {
                                    if (targetState > initialState) {
                                        slideInHorizontally { width -> -width } togetherWith
                                            slideOutHorizontally { width -> width }
                                    } else {
                                        slideInHorizontally { width -> width } togetherWith
                                            slideOutHorizontally { width -> -width }
                                    }
                                },
                                label = "free-mushaf-rtl-page"
                            ) { pageNumber ->
                                val svgContent = remember(pageNumber) {
                                    loadMushafPage(pageNumber)
                                }
                                if (svgContent.isNullOrBlank()) {
                                    Text(
                                        "La page du Mushaf n’est pas disponible.",
                                        modifier = Modifier.padding(18.dp)
                                    )
                                } else {
                                    FreeMushafPageWebView(
                                        svgContent = svgContent,
                                        pageNumber = pageNumber,
                                        tafsirOpen = selectedTafsirVerse != null,
                                        referenceHighlight = reference?.startVerse,
                                        modifier = Modifier.fillMaxSize(),
                                        onSwipePrevious = { showPage(page - 1) },
                                        onSwipeNext = { showPage(page + 1) },
                                        onVerseTapped = { verse ->
                                            if (!referenceMode) openTafsir(verse)
                                        }
                                    )
                                }
                            }

                            Spacer(Modifier.height(7.dp))
                            if (referenceMode) {
                                SafeguardOutlinedButton(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp),
                                    onClick = { finish() }
                                ) {
                                    Text("← Retour au commentaire")
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SafeguardOutlinedButton(
                                        modifier = Modifier.weight(1f),
                                        enabled = selectedTafsirVerse == null && page > FIRST_PAGE,
                                        onClick = { showPage(page - 1) }
                                    ) {
                                        Text("Précédente")
                                    }
                                    SafeguardButton(
                                        modifier = Modifier.weight(1f),
                                        enabled = selectedTafsirVerse == null && page < LAST_PAGE,
                                        onClick = { showPage(page + 1) }
                                    ) {
                                        Text("Suivante")
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                SafeguardOutlinedButton(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp),
                                    enabled = selectedTafsirVerse == null,
                                    onClick = { finish() }
                                ) {
                                    Text("Fermer la lecture")
                                }
                            }
                        }

                        if (!referenceMode) {
                            selectedTafsirVerse?.let { verse ->
                                TafsirEdition.Panel(
                                    verse = verse,
                                    state = tafsirLoadState,
                                    modifier = Modifier.align(Alignment.BottomCenter),
                                    maxPanelHeight = maxHeight * 0.5f,
                                    onPanelTopInWindow = { top ->
                                        TafsirEdition.revealAbove(top)
                                    },
                                    onQuranReferenceSelected = ::openReference
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun openTafsir(verse: VerseRef) {
        if (!TafsirEdition.isEnabled) return
        selectedTafsirVerse = verse
        tafsirLoadState = TafsirLoadState.Loading
        TafsirEdition.selectVerse(verse)
    }

    private fun closeTafsir() {
        if (selectedTafsirVerse == null) return
        TafsirEdition.closeAndRestore()
        selectedTafsirVerse = null
        tafsirLoadState = TafsirLoadState.Closed
    }

    override fun onPause() {
        if (preserveTafsirOnNextPause) {
            preserveTafsirOnNextPause = false
        } else {
            closeTafsir()
        }
        super.onPause()
    }

    private fun loadMushafPage(page: Int): String? {
        if (page !in FIRST_PAGE..LAST_PAGE) return null
        val assetPath = "mushaf/hafs/kfqc/svg-br/%03d.svg.br".format(page)
        return runCatching {
            assets.open(assetPath).use { compressed ->
                BrotliInputStream(compressed)
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }
            }
        }.getOrNull()
    }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@androidx.compose.runtime.Composable
private fun FreeMushafPageWebView(
    svgContent: String,
    pageNumber: Int,
    tafsirOpen: Boolean,
    referenceHighlight: VerseRef?,
    modifier: Modifier = Modifier,
    onSwipePrevious: () -> Unit,
    onSwipeNext: () -> Unit,
    onVerseTapped: (VerseRef) -> Unit
) {
    val currentTafsirOpen = rememberUpdatedState(tafsirOpen)
    val currentReferenceHighlight = rememberUpdatedState(referenceHighlight)
    val currentOnSwipePrevious = rememberUpdatedState(onSwipePrevious)
    val currentOnSwipeNext = rememberUpdatedState(onSwipeNext)
    val currentOnVerseTapped = rememberUpdatedState(onVerseTapped)

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(android.graphics.Color.rgb(244, 240, 230))
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
                val gestureClassifier = ReaderGestureClassifier(swipeThreshold)
                setOnTouchListener { _, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> gestureClassifier.onDown(
                            event.x,
                            event.y,
                            event.pointerCount
                        )
                        MotionEvent.ACTION_MOVE -> gestureClassifier.onMove(
                            event.x,
                            event.y,
                            event.pointerCount
                        )
                        MotionEvent.ACTION_POINTER_DOWN ->
                            gestureClassifier.onAdditionalPointer()
                        MotionEvent.ACTION_CANCEL -> gestureClassifier.onCancel()
                        MotionEvent.ACTION_UP -> {
                            when (
                                gestureClassifier.onUp(
                                    event.x,
                                    event.y,
                                    gesturesEnabled =
                                        !currentTafsirOpen.value &&
                                            currentReferenceHighlight.value == null
                                )
                            ) {
                                ReaderSwipe.NEXT -> currentOnSwipeNext.value()
                                ReaderSwipe.PREVIOUS -> currentOnSwipePrevious.value()
                                null -> Unit
                            }
                        }
                    }
                    false
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean = true

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        val highlight = currentReferenceHighlight.value ?: return
                        view?.evaluateJavascript(
                            "window.qsgTafsir && window.qsgTafsir.select(" +
                                "${highlight.surah},${highlight.ayah});",
                            null
                        )
                    }
                }

                val referenceLockStyle = if (referenceHighlight != null) {
                    "<style>.ayahPolygon{pointer-events:none!important;cursor:default!important}</style>"
                } else {
                    ""
                }
                val html = """
                    <!doctype html>
                    <html dir="rtl">
                    <head>
                      <meta name="viewport"
                            content="width=device-width, initial-scale=1.0">
                      <style>
                        html, body {
                          margin: 0;
                          padding: 0;
                          background: #F4F0E6;
                          width: 100%;
                          min-height: 100%;
                          overflow-x: hidden;
                        }
                        svg {
                          display: block;
                          width: 100%;
                          height: auto;
                          max-width: 100%;
                        }
                      </style>
                    </head>
                    <body>
                      ${TafsirEdition.prepareHtml(svgContent, pageNumber)}
                      $referenceLockStyle
                    </body>
                    </html>
                """.trimIndent()

                if (referenceHighlight == null) {
                    TafsirEdition.configureWebView(
                        webView = this,
                        pageNumber = pageNumber,
                        verseIndex = MushafVerseIndex.fromSvg(svgContent),
                        onVerseTapped = { verse -> currentOnVerseTapped.value(verse) }
                    )
                }

                loadDataWithBaseURL(
                    "https://quran-safeguard.local/",
                    html,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        }
    )
}
