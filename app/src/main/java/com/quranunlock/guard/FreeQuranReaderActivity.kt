package com.applicreation0.quransafeguard

import android.annotation.SuppressLint
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.brotli.dec.BrotliInputStream

/**
 * Voluntary Qur'an reader used outside Safeguard challenges.
 *
 * This activity deliberately has no challenge key and never calls GuardPrefs reading
 * validation or unlock APIs. Reading here therefore cannot credit an app-unlock quota.
 */
class FreeQuranReaderActivity : ComponentActivity() {
    companion object {
        const val EXTRA_PAGE = "page"
        private const val PREFS = "free_quran_reader"
        private const val KEY_LAST_PAGE = "last_page"
        private const val FIRST_PAGE = 1
        private const val LAST_PAGE = 604
    }

    private var selectedTafsirVerse by mutableStateOf<VerseRef?>(null)
    private var selectedTafsirEdition by mutableStateOf(TafsirEditionId.JALALAYN)
    private var tafsirLoadState by mutableStateOf<TafsirLoadState>(TafsirLoadState.Closed)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedTafsirEdition = TafsirReaderPreferences.selectedEdition(this)

        val requestedPage = intent.getIntExtra(EXTRA_PAGE, 0)
        val storedPage = getSharedPreferences(PREFS, MODE_PRIVATE)
            .getInt(KEY_LAST_PAGE, FIRST_PAGE)
        val initialPage = when {
            requestedPage in FIRST_PAGE..LAST_PAGE -> requestedPage
            storedPage in FIRST_PAGE..LAST_PAGE -> storedPage
            else -> FIRST_PAGE
        }

        setContent {
            QuranSafeguardTheme {
                var page by remember { mutableIntStateOf(initialPage) }
                var message by remember {
                    mutableStateOf(
                        "Mushaf arabe : balayez vers la droite pour avancer, " +
                            "vers la gauche pour revenir."
                    )
                }

                BackHandler(enabled = selectedTafsirVerse != null) {
                    closeTafsir()
                }

                LaunchedEffect(selectedTafsirVerse, selectedTafsirEdition) {
                    val requestedVerse = selectedTafsirVerse ?: return@LaunchedEffect
                    val effectiveEdition = TafsirEditionId.effectiveFor(
                        requestedVerse,
                        selectedTafsirEdition
                    )
                    if (effectiveEdition != selectedTafsirEdition) {
                        selectedTafsirEdition = effectiveEdition
                        TafsirReaderPreferences.setSelectedEdition(
                            this@FreeQuranReaderActivity,
                            effectiveEdition
                        )
                        return@LaunchedEffect
                    }

                    val requestKey = TafsirRequestKey(requestedVerse, effectiveEdition)
                    tafsirLoadState = TafsirLoadState.Loading
                    val entry = TafsirEdition.load(
                        this@FreeQuranReaderActivity,
                        requestKey.verse,
                        requestKey.editionId
                    )
                    if (selectedTafsirVerse == requestKey.verse &&
                        selectedTafsirEdition == requestKey.editionId
                    ) {
                        tafsirLoadState = if (entry == null) {
                            TafsirLoadState.Unavailable
                        } else {
                            TafsirLoadState.Available(entry)
                        }
                    }
                }

                fun showPage(nextPage: Int) {
                    if (selectedTafsirVerse != null) return
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

                Surface(modifier = Modifier.fillMaxSize()) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 6.dp)
                        ) {
                            Text(
                                "Qur’an & Tafsîr",
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
                                        modifier = Modifier.fillMaxSize(),
                                        onSwipePrevious = { showPage(page - 1) },
                                        onSwipeNext = { showPage(page + 1) },
                                        onVerseTapped = { verse -> openTafsir(verse) }
                                    )
                                }
                            }

                            Spacer(Modifier.height(7.dp))
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
                                    Text("Page précédente")
                                }
                                SafeguardButton(
                                    modifier = Modifier.weight(1f),
                                    enabled = selectedTafsirVerse == null && page < LAST_PAGE,
                                    onClick = { showPage(page + 1) }
                                ) {
                                    Text("Page suivante")
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

                        selectedTafsirVerse?.let { verse ->
                            TafsirEdition.Panel(
                                verse = verse,
                                editionId = selectedTafsirEdition,
                                state = tafsirLoadState,
                                modifier = Modifier.align(Alignment.BottomCenter),
                                maxPanelHeight = maxHeight * 0.5f,
                                onEditionChange = { next ->
                                    if (next.covers(verse)) {
                                        selectedTafsirEdition = next
                                        TafsirReaderPreferences.setSelectedEdition(
                                            this@FreeQuranReaderActivity,
                                            next
                                        )
                                    }
                                },
                                onPanelTopInWindow = { top ->
                                    TafsirEdition.revealAbove(top)
                                }
                            )
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
        closeTafsir()
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
    modifier: Modifier = Modifier,
    onSwipePrevious: () -> Unit,
    onSwipeNext: () -> Unit,
    onVerseTapped: (VerseRef) -> Unit
) {
    val currentTafsirOpen = rememberUpdatedState(tafsirOpen)
    val currentOnSwipePrevious = rememberUpdatedState(onSwipePrevious)
    val currentOnSwipeNext = rememberUpdatedState(onSwipeNext)
    val currentOnVerseTapped = rememberUpdatedState(onVerseTapped)

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(android.graphics.Color.WHITE)
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
                                    gesturesEnabled = !currentTafsirOpen.value
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
                          background: #ffffff;
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
                    </body>
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
        }
    )
}
