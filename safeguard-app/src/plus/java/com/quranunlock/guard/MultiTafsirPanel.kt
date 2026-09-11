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
 *
 * Availability is resolved from real source-backed rows for the tapped verse.
 * A remembered edition that does not cover the verse is never left selected:
 * Jalalayn is the first fallback, then the first genuinely available edition.
 * Primary reader labels stay author-based: Jalalayn, Qurtubi and Qushayri.
 */
@Composable
internal fun MultiTafsirPanel(
    verse: VerseRef,
    modifier: Modifier,
    maxPanelHeight: Dp,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onPanelTopInWindow: (Int) -> Unit,
    onQuranReferenceSelected: ((QuranReferenceRef) -> Unit)? = null
) {
    val context = LocalContext.current
    val contextual = (context as? android.app.Activity)?.intent?.getBooleanExtra("contextual", false) == true
    val prefs = remember {
        context.getSharedPreferences(
            MULTI_TAFSIR_PREFS,
            android.content.Context.MODE_PRIVATE
        )
    }
    var selectedEdition by remember {
        mutableStateOf(
            if (contextual) PrivateTafsirEdition.JALALAYN else PrivateTafsirEdition.fromStorage(
                prefs.getString(
                    MULTI_EDITION_KEY,
                    PrivateTafsirEdition.JALALAYN.storageValue
                )
            )
        )
    }
    var availability by remember(verse) {
        mutableStateOf<MultiTafsirAvailability?>(null)
    }

    LaunchedEffect(verse) {
        availability = null
        val loaded = MultiTafsirRepository.loadAvailable(context, verse)
        val resolved = loaded.resolveEdition(selectedEdition)
        if (resolved != null && resolved != selectedEdition) {
            if (!contextual) prefs.edit()
                .putString(MULTI_EDITION_KEY, resolved.storageValue)
                .apply()
            selectedEdition = resolved
        }
        availability = loaded
    }

    fun choose(edition: PrivateTafsirEdition) {
        val available = availability ?: return
        if (edition !in available.editions || edition == selectedEdition) return
        if (!contextual) prefs.edit()
            .putString(MULTI_EDITION_KEY, edition.storageValue)
            .apply()
        selectedEdition = edition
    }

    val loaded = availability
    val state = when {
        loaded == null -> TafsirLoadState.Loading
        loaded.entries[selectedEdition] != null ->
            TafsirLoadState.Available(loaded.entries.getValue(selectedEdition))
        else -> TafsirLoadState.Unavailable
    }

    TafsirPanel(
        verse = verse,
        state = state,
        selectedEdition = selectedEdition,
        availableEditions = loaded?.editions.orEmpty(),
        onEditionSelected = ::choose,
        onQuranReferenceSelected = onQuranReferenceSelected,
        modifier = modifier,
        maxPanelHeight = maxPanelHeight,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        onPanelTopInWindow = onPanelTopInWindow
    )
}

