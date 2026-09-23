package com.applicreation0.quransafeguard

import android.content.Context
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

object TafsirEdition {
    const val isEnabled: Boolean = false

    fun prepareHtml(svgContent: String, pageNumber: Int): String =
        svgContent + """
            <style>
              html, body, svg { background: ${ReaderComfortPrefs.pageBackground()} !important; }
            </style>
        """.trimIndent()

    fun configureWebView(
        webView: WebView,
        pageNumber: Int,
        verseIndex: Set<VerseRef>,
        onVerseTapped: (VerseRef) -> Unit
    ) = Unit

    fun selectVerse(verse: VerseRef) = Unit

    fun revealAbove(panelTopInWindowPx: Int) = Unit

    fun closeAndRestore() = Unit

    suspend fun load(context: Context, verse: VerseRef): TafsirEntry? = null

    suspend fun referencePage(
        context: Context,
        reference: QuranReferenceRef
    ): Int? = null

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
    ) = Unit
}

