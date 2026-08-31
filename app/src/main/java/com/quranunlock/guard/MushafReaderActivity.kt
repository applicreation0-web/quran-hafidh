package com.quranunlock.guard

import android.annotation.SuppressLint
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.brotli.dec.BrotliInputStream

class MushafReaderActivity : ComponentActivity() {
    companion object {
        const val EXTRA_PAGE = "page"
        const val EXTRA_CHALLENGE_KEY = "challenge_key"
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
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var loadFailed by remember {
                        mutableStateOf(svgContent.isNullOrBlank())
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        Text(
                            "Mushaf de Médine • Hafs ‘an ‘Asim • Page " + page,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))

                        if (!svgContent.isNullOrBlank() && !loadFailed) {
                            MushafPageWebView(
                                svgContent = svgContent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                onReady = {
                                    markPageReady()
                                },
                                onFailure = {
                                    loadFailed = true
                                    markPageUnavailable()
                                }
                            )
                        } else {
                            Text(
                                "La page du Mushaf n’est pas disponible. " +
                                    "La lecture n’est pas comptabilisée."
                            )
                            Spacer(Modifier.weight(1f))
                        }

                        Spacer(Modifier.height(8.dp))
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { finish() }
                        ) {
                            Text("Retour au contrôle")
                        }
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

                loadDataWithBaseURL(
                    null,
                    svgContent,
                    "image/svg+xml",
                    "UTF-8",
                    null
                )
            }
        }
    )
}
