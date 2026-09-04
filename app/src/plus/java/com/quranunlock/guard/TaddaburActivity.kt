package com.applicreation0.quransafeguard

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import org.brotli.dec.BrotliInputStream
import java.time.LocalTime

class TaddaburActivity : ComponentActivity() {
    private var activityResumed = false
    private var activityTopResumed = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
    private var pageReady = false
    private var selectedTafsirVerse by mutableStateOf<VerseRef?>(null)
    private var tafsirLoadState by mutableStateOf<TafsirLoadState>(TafsirLoadState.Closed)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TaddaburEdition.scheduleReminder(this)
        val initial = TaddaburPrefs.progress(this)

        setContent {
            QuranSafeguardTheme {
                var progress by remember { mutableStateOf(initial) }
                var page by remember { mutableStateOf(initial.bookmarkPage) }
                var readingMs by remember {
                    mutableLongStateOf(TaddaburPrefs.elapsedMs(this@TaddaburActivity, page))
                }

                BackHandler(enabled = selectedTafsirVerse != null) {
                    closeTafsir()
                }

                LaunchedEffect(selectedTafsirVerse) {
                    val requested = selectedTafsirVerse ?: return@LaunchedEffect
                    tafsirLoadState = TafsirLoadState.Loading
                    val entry = TafsirEdition.load(this@TaddaburActivity, requested)
                    if (selectedTafsirVerse == requested) {
                        tafsirLoadState = if (entry == null) {
                            TafsirLoadState.Unavailable
                        } else {
                            TafsirLoadState.Available(entry)
                        }
                    }
                }

                LaunchedEffect(page) {
                    pageReady = false
                    TaddaburPrefs.setBookmark(this@TaddaburActivity, page)
                    readingMs = TaddaburPrefs.elapsedMs(this@TaddaburActivity, page)
                    while (true) {
                        if (mayCountActiveReading() &&
                            pageReady &&
                            TaddaburPolicy.mayAccumulate(LocalTime.now())
                        ) {
                            progress = TaddaburPrefs.recordActiveMs(
                                this@TaddaburActivity,
                                page,
                                1_000L
                            )
                            readingMs = TaddaburPrefs.elapsedMs(this@TaddaburActivity, page)
                        }
                        delay(1_000L)
                    }
                }

                fun showPage(nextPage: Int) {
                    if (selectedTafsirVerse != null) return
                    if (nextPage !in progress.startPage..progress.endPage) return
                    closeTafsir()
                    page = nextPage
                    TaddaburPrefs.setBookmark(this@TaddaburActivity, nextPage)
                    readingMs = TaddaburPrefs.elapsedMs(this@TaddaburActivity, nextPage)
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "Taddabur • Hizb ${progress.hizb}",
                                modifier = Modifier.padding(horizontal = 12.dp),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${progress.completedCount}/${progress.totalPages} pages validées • 90 s minimum par page",
                                modifier = Modifier.padding(horizontal = 12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "🔖 Marque-page : page ${progress.bookmarkPage} • page affichée : $page",
                                modifier = Modifier.padding(horizontal = 12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            QuranStructureMetadata.boundaryNotice(
                                QuranSelectionMode.HIZB,
                                progress.hizb,
                                page
                            )?.let { notice ->
                                Text(
                                    notice,
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                when {
                                    progress.complete -> "Hizb du jour terminé ✓"
                                    LocalTime.now().hour < TaddaburPolicy.START_HOUR ->
                                        "Le chrono Taddabur commencera à 07:00."
                                    readingMs >= TaddaburPolicy.MIN_PAGE_MS -> "Page validée ✓"
                                    else -> "Temps actif sur cette page : ${formatTaddaburDuration(readingMs)} / 01:30"
                                },
                                modifier = Modifier.padding(horizontal = 12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )

                            val svgContent = remember(page) { loadMushafPage(page) }
                            if (svgContent.isNullOrBlank()) {
                                Text(
                                    "La page du Mushaf n’est pas disponible.",
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(18.dp)
                                )
                            } else {
                                TaddaburMushafWebView(
                                    svgContent = svgContent,
                                    pageNumber = page,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    onReady = { pageReady = true },
                                    onVerseTapped = { verse -> openTafsir(verse) },
                                    onFailure = { pageReady = false }
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SafeguardOutlinedButton(
                                    modifier = Modifier.weight(1f),
                                    enabled = selectedTafsirVerse == null && page > progress.startPage,
                                    onClick = { showPage(page - 1) }
                                ) {
                                    Text("Page précédente")
                                }
                                SafeguardButton(
                                    modifier = Modifier.weight(1f),
                                    enabled = selectedTafsirVerse == null && page < progress.endPage,
                                    onClick = { showPage(page + 1) }
                                ) {
                                    Text("Page suivante")
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            SafeguardOutlinedButton(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp),
                                enabled = selectedTafsirVerse == null,
                                onClick = {
                                    TaddaburPrefs.setBookmark(this@TaddaburActivity, page)
                                    finish()
                                }
                            ) {
                                Text("Fermer • garder le marque-page")
                            }
                        }

                        selectedTafsirVerse?.let { verse ->
                            TafsirEdition.Panel(
                                verse = verse,
                                state = tafsirLoadState,
                                modifier = Modifier.align(Alignment.BottomCenter),
                                maxPanelHeight = maxHeight * 0.5f,
                                onPanelTopInWindow = { top -> TafsirEdition.revealAbove(top) }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun mayCountActiveReading(): Boolean =
        activityResumed &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || activityTopResumed)

    override fun onResume() {
        super.onResume()
        activityResumed = true
    }

    override fun onPause() {
        activityResumed = false
        closeTafsir()
        super.onPause()
    }

    override fun onTopResumedActivityChanged(isTopResumedActivity: Boolean) {
        super.onTopResumedActivityChanged(isTopResumedActivity)
        activityTopResumed = isTopResumedActivity
    }

    private fun openTafsir(verse: VerseRef) {
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
}

@SuppressLint("SetJavaScriptEnabled")
@androidx.compose.runtime.Composable
private fun TaddaburMushafWebView(
    svgContent: String,
    pageNumber: Int,
    modifier: Modifier = Modifier,
    onReady: () -> Unit,
    onVerseTapped: (VerseRef) -> Unit,
    onFailure: () -> Unit
) {
    val currentOnReady = rememberUpdatedState(onReady)
    val currentOnVerseTapped = rememberUpdatedState(onVerseTapped)
    val currentOnFailure = rememberUpdatedState(onFailure)

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                // Warm ivory only: Quran Safeguard deliberately has no black/dark reading mode.
                setBackgroundColor(android.graphics.Color.rgb(247, 242, 232))
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = false
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.blockNetworkLoads = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean = true

                    override fun onPageFinished(view: WebView?, url: String?) {
                        currentOnReady.value()
                    }

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
                        html, body { margin:0; padding:0; background:#F7F2E8; width:100%; min-height:100%; overflow-x:hidden; }
                        svg { display:block; width:100%; height:auto; max-width:100%; }
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
        }
    )
}

private fun formatTaddaburDuration(milliseconds: Long): String {
    val seconds = (milliseconds / 1_000L).coerceAtLeast(0L)
    return "%02d:%02d".format(seconds / 60L, seconds % 60L)
}
