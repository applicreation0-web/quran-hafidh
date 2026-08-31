package com.applicreation0.quransafeguard

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import org.brotli.dec.BrotliInputStream

class MushafReaderActivity : ComponentActivity() {
    companion object {
        const val EXTRA_PAGE = "page"
        const val EXTRA_CHALLENGE_KEY = "challenge_key"
        private const val REQUIRED_READING_MS = 60_000L
    }

    private var page = 0
    private var challengeKey: String = ""
    private var pageReady = false
    private var activityResumed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        page = intent.getIntExtra(EXTRA_PAGE, 0)
        challengeKey = intent.getStringExtra(EXTRA_CHALLENGE_KEY).orEmpty()

        if (page !in 1..604 || challengeKey.isBlank()) {
            finish()
            return
        }

        val assetPath = "mushaf/hafs/kfqc/svg-br/%03d.svg.br".format(page)
        val svgContent = runCatching {
            assets.open(assetPath).use { compressed ->
                BrotliInputStream(compressed).bufferedReader(Charsets.UTF_8).use {
                    it.readText()
                }
            }
        }.getOrNull()

        setContent {
            QuranSafeguardTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var loadFailed by remember {
                        mutableStateOf(svgContent.isNullOrBlank())
                    }
                    var readingMs by remember {
                        mutableLongStateOf(
                            GuardPrefs.readingElapsedMs(
                                this@MushafReaderActivity,
                                challengeKey,
                                page
                            )
                        )
                    }

                    LaunchedEffect(page, challengeKey) {
                        while (true) {
                            readingMs = GuardPrefs.readingElapsedMs(
                                this@MushafReaderActivity,
                                challengeKey,
                                page
                            )
                            delay(250)
                        }
                    }

                    val readingComplete = readingMs >= REQUIRED_READING_MS
                    val progress =
                        (readingMs.toFloat() / REQUIRED_READING_MS.toFloat()).coerceIn(0f, 1f)
                    val remainingSeconds =
                        ((REQUIRED_READING_MS - readingMs).coerceAtLeast(0L) + 999L) / 1000L

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            "Mushaf de Médine • Page $page",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (readingComplete) {
                                "Lecture validée • ${formatReadingDuration(readingMs)}"
                            } else {
                                "Lecture ${formatReadingDuration(readingMs)} / 01:00 • ${remainingSeconds}s restantes"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (readingComplete) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))

                        if (!svgContent.isNullOrBlank() && !loadFailed) {
                            MushafPageWebView(
                                svgContent = svgContent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                onReady = { markPageReady() },
                                onFailure = {
                                    loadFailed = true
                                    markPageUnavailable()
                                }
                            )
                        } else {
                            Text(
                                "La page du Mushaf n’est pas disponible. La lecture n’est pas comptabilisée."
                            )
                            Spacer(Modifier.weight(1f))
                        }

                        Spacer(Modifier.height(8.dp))
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = readingComplete,
                            onClick = {
                                GuardPrefs.completeReadingAndUnlock(
                                    this@MushafReaderActivity,
                                    challengeKey,
                                    page
                                )
                                continueToTarget()
                            }
                        ) {
                            Text(
                                if (readingComplete) {
                                    "Lecture validée — continuer"
                                } else {
                                    "Continuer après 60 secondes"
                                }
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { finish() }
                        ) {
                            Text("Retour au contrôle")
                        }
                        Text(
                            "Si tu quittes cet écran, le compteur se met en pause.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activityResumed = true
        if (pageReady) {
            GuardPrefs.beginReadingForeground(this, challengeKey, page)
        }
    }

    override fun onPause() {
        if (pageReady) {
            GuardPrefs.endReadingForeground(this, challengeKey, page)
        }
        activityResumed = false
        super.onPause()
    }

    private fun markPageReady() {
        if (pageReady) return
        pageReady = true
        if (activityResumed) {
            GuardPrefs.beginReadingForeground(this, challengeKey, page)
        }
    }

    private fun markPageUnavailable() {
        if (pageReady) {
            GuardPrefs.endReadingForeground(this, challengeKey, page)
        }
        pageReady = false
    }

    private fun continueToTarget() {
        val launchIntent = packageManager.getLaunchIntentForPackage(challengeKey)
        if (launchIntent != null) {
            launchIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            )
            startActivity(launchIntent)
        }
        finishAffinity()
    }
}

private fun formatReadingDuration(milliseconds: Long): String {
    val seconds = (milliseconds / 1000L).coerceAtLeast(0L)
    val minutesPart = seconds / 60L
    val secondsPart = seconds % 60L
    return "%02d:%02d".format(minutesPart, secondsPart)
}

@SuppressLint("SetJavaScriptEnabled")
@androidx.compose.runtime.Composable
private fun MushafPageWebView(
    svgContent: String,
    modifier: Modifier = Modifier,
    onReady: () -> Unit,
    onFailure: () -> Unit
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(android.graphics.Color.WHITE)
                settings.javaScriptEnabled = false
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
                        onReady()
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: android.webkit.WebResourceError?
                    ) {
                        if (request?.isForMainFrame == true) {
                            onFailure()
                        }
                    }
                }

                val html = """
                    <!doctype html>
                    <html>
                    <head>
                      <meta name="viewport" content="width=device-width, initial-scale=1.0">
                      <style>
                        html, body {
                          margin: 0;
                          padding: 0;
                          background: #ffffff;
                          width: 100%;
                          min-height: 100%;
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
                      $svgContent
                    </body>
                    </html>
                """.trimIndent()

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
