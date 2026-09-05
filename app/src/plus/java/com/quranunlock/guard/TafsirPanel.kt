package com.applicreation0.quransafeguard

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private const val TAFSIR_PREFS = "tafsir_reader_preferences"
private const val FONT_SIZE_KEY = "commentary_font_size_sp"
private const val MIN_FONT_SIZE = 14f
private const val MAX_FONT_SIZE = 26f
private const val DEFAULT_FONT_SIZE = 18f

/**
 * Single Tafsir renderer for Plus.
 *
 * This is deliberately the 0.10.3 panel extended by only one permanent visual
 * control: the compact edition selector. Data loading stays outside the renderer.
 * Sustained reading uses the shared warm, low-glare reading surface.
 * Long-form Tafsir prose and notes are justified for book-like reading.
 */
@Composable
internal fun TafsirPanel(
    verse: VerseRef,
    state: TafsirLoadState,
    selectedEdition: PrivateTafsirEdition,
    onEditionSelected: (PrivateTafsirEdition) -> Unit,
    modifier: Modifier,
    maxPanelHeight: Dp,
    onPanelTopInWindow: (Int) -> Unit
) {
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences(TAFSIR_PREFS, android.content.Context.MODE_PRIVATE)
    }
    var fontSize by remember {
        mutableFloatStateOf(
            preferences.getFloat(FONT_SIZE_KEY, DEFAULT_FONT_SIZE)
                .coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE)
        )
    }
    var menuExpanded by remember { mutableStateOf(false) }
    var notesExpanded by remember(verse, selectedEdition) { mutableStateOf(false) }
    val scrollState = remember(verse, selectedEdition) { ScrollState(0) }

    LaunchedEffect(verse, selectedEdition) {
        scrollState.scrollTo(0)
    }

    fun changeFont(delta: Float) {
        val next = (fontSize + delta).coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE)
        fontSize = next
        preferences.edit().putFloat(FONT_SIZE_KEY, next).apply()
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = maxPanelHeight)
            .onGloballyPositioned { coordinates ->
                onPanelTopInWindow(coordinates.boundsInWindow().top.roundToInt())
            }
            .semantics { paneTitle = "Commentaire du verset" },
        color = SafeguardReadingSurface,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Sourate ${verse.surah}, verset ${verse.ayah}",
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    TextButton(
                        modifier = Modifier.semantics {
                            contentDescription = "Réduire la taille du commentaire"
                        },
                        enabled = fontSize > MIN_FONT_SIZE,
                        onClick = { changeFont(-2f) }
                    ) {
                        Text("A−")
                    }
                    TextButton(
                        modifier = Modifier.semantics {
                            contentDescription = "Agrandir la taille du commentaire"
                        },
                        enabled = fontSize < MAX_FONT_SIZE,
                        onClick = { changeFont(2f) }
                    ) {
                        Text("A+")
                    }
                }
            }

            Box {
                TextButton(
                    modifier = Modifier.semantics {
                        contentDescription = "Choisir le Tafsîr"
                        stateDescription = selectedEdition.displayName
                    },
                    onClick = { menuExpanded = true }
                ) {
                    Text("${selectedEdition.displayName} ▾")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    PrivateTafsirEdition.entries.forEach { edition ->
                        DropdownMenuItem(
                            text = { Text(edition.displayName) },
                            onClick = {
                                menuExpanded = false
                                onEditionSelected(edition)
                            }
                        )
                    }
                }
            }
            HorizontalDivider()

            when (state) {
                TafsirLoadState.Closed,
                TafsirLoadState.Loading -> Text(
                    "Chargement du commentaire…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TafsirLoadState.Unavailable -> Text(
                    "Commentaire anglais indisponible pour ce verset dans cette édition.",
                    fontSize = fontSize.sp
                )
                is TafsirLoadState.Available -> {
                    Text(
                        text = runsToAnnotatedString(state.entry.commentaryRuns),
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * 1.42f).sp,
                        textAlign = TextAlign.Justify,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (state.entry.notes.isNotEmpty()) {
                        HorizontalDivider()
                        TextButton(
                            modifier = Modifier.semantics {
                                stateDescription = if (notesExpanded) {
                                    "Notes développées"
                                } else {
                                    "Notes repliées"
                                }
                            },
                            onClick = { notesExpanded = !notesExpanded }
                        ) {
                            Text(
                                if (notesExpanded) {
                                    "Notes (${state.entry.notes.size}) — replier"
                                } else {
                                    "Notes (${state.entry.notes.size})"
                                }
                            )
                        }
                        if (notesExpanded) {
                            state.entry.notes.forEach { note ->
                                Text(
                                    text = AnnotatedString.Builder().apply {
                                        withStyle(
                                            SpanStyle(fontWeight = FontWeight.Bold)
                                        ) {
                                            append("${note.number}. ")
                                        }
                                        append(runsToAnnotatedString(note.runs))
                                    }.toAnnotatedString(),
                                    fontSize = (fontSize - 1f).coerceAtLeast(MIN_FONT_SIZE).sp,
                                    lineHeight = (fontSize * 1.35f).sp,
                                    textAlign = TextAlign.Justify,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun runsToAnnotatedString(runs: List<TafsirRun>): AnnotatedString {
    return AnnotatedString.Builder().apply {
        runs.forEach { run ->
            val style = when (run.style) {
                TafsirRunStyle.REGULAR -> SpanStyle()
                TafsirRunStyle.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
                TafsirRunStyle.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
                TafsirRunStyle.BOLD_ITALIC -> SpanStyle(
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic
                )
                TafsirRunStyle.NOTE_REF -> SpanStyle(
                    baselineShift = BaselineShift.Superscript,
                    fontSize = 0.78.em
                )
            }
            withStyle(style) { append(run.text) }
        }
    }.toAnnotatedString()
}
