package com.applicreation0.quransafeguard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp

private const val MULTI_TAFSIR_PREFS = "tafsir_reader_preferences"
private const val MULTI_EDITION_KEY = "selected_edition"

/**
 * Loading/controller layer for the single TafsirPanel renderer.
 * Keeps edition persistence and stale-request protection outside the UI renderer.
 */
@Composable
internal fun MultiTafsirPanel(
    verse: VerseRef,
    modifier: Modifier,
    maxPanelHeight: Dp,
    onPanelTopInWindow: (Int) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(
            MULTI_TAFSIR_PREFS,
            android.content.Context.MODE_PRIVATE
        )
    }
    var selectedEdition by remember {
        mutableStateOf(
            PrivateTafsirEdition.fromStorage(
                prefs.getString(
                    MULTI_EDITION_KEY,
                    PrivateTafsirEdition.JALALAYN.storageValue
                )
            )
        )
    }
    var state by remember(verse, selectedEdition) {
        mutableStateOf<TafsirLoadState>(TafsirLoadState.Loading)
    }
    val requestKey = remember(verse, selectedEdition) {
        MultiTafsirRequestKey(verse, selectedEdition)
    }
    var activeRequest by remember { mutableStateOf(requestKey) }

    LaunchedEffect(requestKey) {
        activeRequest = requestKey
        state = TafsirLoadState.Loading
        val entry = MultiTafsirRepository.load(
            context,
            verse,
            selectedEdition
        )
        if (activeRequest == requestKey) {
            state = if (entry == null) {
                TafsirLoadState.Unavailable
            } else {
                TafsirLoadState.Available(entry)
            }
        }
    }

    fun choose(edition: PrivateTafsirEdition) {
        if (edition == selectedEdition) return
        prefs.edit()
            .putString(MULTI_EDITION_KEY, edition.storageValue)
            .apply()
        selectedEdition = edition
    }

    TafsirPanel(
        verse = verse,
        state = state,
        selectedEdition = selectedEdition,
        onEditionSelected = ::choose,
        modifier = modifier,
        maxPanelHeight = maxPanelHeight,
        onPanelTopInWindow = onPanelTopInWindow
    )
}
