package com.applicreation0.quransafeguard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.ClickableText
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val TAFSIR_PREFS = "tafsir_reader_preferences"
private const val FONT_SIZE_KEY = "commentary_font_size_sp"
private const val MIN_FONT_SIZE = 16f
private const val MAX_FONT_SIZE = 26f
private const val DEFAULT_FONT_SIZE = 18f
private const val COMMENTARY_LINE_HEIGHT_RATIO = 1.50f
private const val NOTE_LINE_HEIGHT_RATIO = 1.45f
private const val POETRY_LINE_HEIGHT_RATIO = 1.55f
private const val NOTE_LINK_TAG = "tafsir_note"
private const val QURAN_LINK_TAG = "tafsir_quran"

/**
 * Single Tafsir renderer for Plus.
 *
 * Visual semantics are shared by every edition while source semantics remain intact:
 * - regular: commentary prose;
 * - italic/bold: source emphasis preserved when the source supplies it;
 * - bold italic: source-provided Qur'an translation;
 * - technical term: semi-bold only when explicitly source/semantic tagged;
 * - transliteration: italic only when explicitly source/semantic tagged;
 * - poetry: italic, source line breaks preserved, never justified;
 * - superscript: source note call.
 *
 * Only explicit, canonically valid source chapter:verse notation is interactive.
 * Invalid source citations stay visible as source text but never become false links.
 * A source note call jumps to its exact linked note and offers an exact scroll return.
 */
@Composable
internal fun TafsirPanel(
    verse: VerseRef,
    state: TafsirLoadState,
    selectedEdition: PrivateTafsirEdition,
    availableEditions: List<PrivateTafsirEdition>,
    onEditionSelected: (PrivateTafsirEdition) -> Unit,
    onQuranReferenceSelected: ((QuranReferenceRef) -> Unit)?,
    modifier: Modifier,
    maxPanelHeight: Dp,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onPanelTopInWindow: (Int) -> Unit
) {
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences(TAFSIR_PREFS, android.content.Context.MODE_PRIVATE)
    }
    val coroutineScope = rememberCoroutineScope()
    var fontSize by remember {
        mutableFloatStateOf(
            preferences.getFloat(FONT_SIZE_KEY, DEFAULT_FONT_SIZE)
                .coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE)
        )
    }
    var menuExpanded by remember { mutableStateOf(false) }
    var notesExpanded by remember(verse, selectedEdition) { mutableStateOf(false) }
    var noteReturnScroll by remember(verse, selectedEdition) { mutableStateOf<Int?>(null) }
    var requestedNoteNumber by remember(verse, selectedEdition) { mutableStateOf<Int?>(null) }
    val scrollState = remember(verse, selectedEdition) { ScrollState(0) }
    val availableEntry = (state as? TafsirLoadState.Available)?.entry
    val noteRequesters = remember(availableEntry?.notes) {
        availableEntry?.notes
            ?.associate { it.number to BringIntoViewRequester() }
            .orEmpty()
    }

    LaunchedEffect(verse, selectedEdition) {
        scrollState.scrollTo(0)
    }

    LaunchedEffect(requestedNoteNumber, notesExpanded, availableEntry) {
        val number = requestedNoteNumber ?: return@LaunchedEffect
        if (!notesExpanded) return@LaunchedEffect
        noteRequesters[number]?.bringIntoView()
    }

    fun changeFont(delta: Float) {
        val next = (fontSize + delta).coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE)
        fontSize = next
        preferences.edit().putFloat(FONT_SIZE_KEY, next).apply()
    }

    fun openNote(number: Int) {
        val entry = availableEntry ?: return
        if (entry.notes.none { it.number == number }) return
        if (noteReturnScroll == null) noteReturnScroll = scrollState.value
        notesExpanded = true
        requestedNoteNumber = number
    }

    fun returnToCommentary() {
        val position = noteReturnScroll ?: return
        requestedNoteNumber = null
        noteReturnScroll = null
        coroutineScope.launch { scrollState.animateScrollTo(position) }
    }

    BackHandler(enabled = noteReturnScroll != null) {
        returnToCommentary()
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
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 1.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    TextButton(
                        modifier = Modifier.semantics {
                            contentDescription =
                                "Tafsîr ${selectedEdition.displayName}, sourate ${verse.surah}, verset ${verse.ayah}"
                            stateDescription = selectedEdition.displayName
                            heading()
                        },
                        enabled = availableEditions.size > 1,
                        onClick = { menuExpanded = true }
                    ) {
                        Text(
                            if (availableEditions.size > 1) {
                                "${selectedEdition.displayName} ▾ · ${verse.surah}:${verse.ayah}"
                            } else {
                                "${selectedEdition.displayName} · ${verse.surah}:${verse.ayah}"
                            },
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded && availableEditions.size > 1,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        availableEditions.forEach { edition ->
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
                Row {
                    TextButton(
                        modifier = Modifier.semantics {
                            contentDescription = if (expanded) {
                                "Réduire le panneau du Tafsîr"
                            } else {
                                "Agrandir le panneau du Tafsîr"
                            }
                            stateDescription = if (expanded) "Tafsîr agrandi" else "Tafsîr compact"
                        },
                        onClick = { onExpandedChange(!expanded) }
                    ) {
                        Text(
                            if (expanded) "⤡" else "⤢",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(
                        modifier = Modifier.semantics {
                            contentDescription = "Réduire la taille du commentaire"
                        },
                        enabled = fontSize > MIN_FONT_SIZE,
                        onClick = { changeFont(-2f) }
                    ) { Text("A−") }
                    TextButton(
                        modifier = Modifier.semantics {
                            contentDescription = "Agrandir la taille du commentaire"
                        },
                        enabled = fontSize < MAX_FONT_SIZE,
                        onClick = { changeFont(2f) }
                    ) { Text("A+") }
                }
            }

            HorizontalDivider()

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                when (state) {
                    TafsirLoadState.Closed,
                    TafsirLoadState.Loading -> Text(
                        "Chargement du commentaire…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TafsirLoadState.Unavailable -> Text(
                        "Aucun commentaire vérifié n’est disponible pour ce verset.",
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * COMMENTARY_LINE_HEIGHT_RATIO).sp
                    )
                    is TafsirLoadState.Available -> {
                        InteractiveTafsirText(
                            runs = state.entry.commentaryRuns,
                            fontSize = fontSize,
                            lineHeightRatio = COMMENTARY_LINE_HEIGHT_RATIO,
                            color = MaterialTheme.colorScheme.onSurface,
                            linkColor = MaterialTheme.colorScheme.primary,
                            enableQuranLinks = onQuranReferenceSelected != null,
                            onNoteSelected = ::openNote,
                            onQuranReferenceSelected = onQuranReferenceSelected
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
                                    },
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (notesExpanded) {
                                if (noteReturnScroll != null) {
                                    TextButton(onClick = ::returnToCommentary) {
                                        Text("← Retour au commentaire")
                                    }
                                }
                                state.entry.notes.forEach { note ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .bringIntoViewRequester(
                                                noteRequesters.getValue(note.number)
                                            )
                                    ) {
                                        Text(
                                            text = "${note.number}.",
                                            fontSize = fontSize.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        InteractiveTafsirText(
                                            runs = note.runs,
                                            fontSize = (fontSize - 1f).coerceAtLeast(MIN_FONT_SIZE),
                                            lineHeightRatio = NOTE_LINE_HEIGHT_RATIO,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            linkColor = MaterialTheme.colorScheme.primary,
                                            enableQuranLinks = onQuranReferenceSelected != null,
                                            onNoteSelected = ::openNote,
                                            onQuranReferenceSelected = onQuranReferenceSelected
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
private fun InteractiveTafsirText(
    runs: List<TafsirRun>,
    fontSize: Float,
    lineHeightRatio: Float,
    color: Color,
    linkColor: Color,
    enableQuranLinks: Boolean,
    onNoteSelected: (Int) -> Unit,
    onQuranReferenceSelected: ((QuranReferenceRef) -> Unit)?
) {
    val blocks = remember(runs) { splitTafsirRenderBlocks(runs) }
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        blocks.forEach { block ->
            val annotated = remember(block.runs, linkColor, enableQuranLinks) {
                runsToAnnotatedString(block.runs, linkColor, enableQuranLinks)
            }
            val isPoetry = block.kind == TafsirBlockKind.POETRY
            ClickableText(
                modifier = if (isPoetry) {
                    Modifier.padding(start = 12.dp, end = 4.dp, bottom = 3.dp)
                } else {
                    Modifier
                },
                text = annotated,
                style = TextStyle(
                    color = color,
                    fontSize = fontSize.sp,
                    lineHeight = (
                        fontSize * if (isPoetry) POETRY_LINE_HEIGHT_RATIO else lineHeightRatio
                    ).sp,
                    textAlign = if (isPoetry) TextAlign.Start else TextAlign.Justify
                ),
                onClick = { offset ->
                    annotated.getStringAnnotations(NOTE_LINK_TAG, offset, offset)
                        .firstOrNull()
                        ?.item
                        ?.toIntOrNull()
                        ?.let(onNoteSelected)
                        ?: annotated.getStringAnnotations(QURAN_LINK_TAG, offset, offset)
                            .firstOrNull()
                            ?.item
                            ?.let(::decodeQuranReference)
                            ?.let { reference -> onQuranReferenceSelected?.invoke(reference) }
                }
            )
        }
    }
}

private fun runsToAnnotatedString(
    runs: List<TafsirRun>,
    linkColor: Color,
    enableQuranLinks: Boolean
): AnnotatedString = AnnotatedString.Builder().apply {
    runs.forEach { run ->
        val displayText = if (run.style == TafsirRunStyle.POETRY) {
            run.text
        } else {
            run.text.replace(Regex("\\n{2,}"), "\n")
        }
        val start = length
        append(displayText)
        val end = length
        if (start == end) return@forEach

        val sourceStyle = when (run.style) {
            TafsirRunStyle.REGULAR -> SpanStyle()
            TafsirRunStyle.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
            TafsirRunStyle.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
            TafsirRunStyle.BOLD_ITALIC -> SpanStyle(
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic
            )
            TafsirRunStyle.TECHNICAL_TERM -> SpanStyle(
                fontWeight = FontWeight.SemiBold
            )
            TafsirRunStyle.TRANSLITERATION -> SpanStyle(
                fontStyle = FontStyle.Italic
            )
            TafsirRunStyle.POETRY -> SpanStyle(
                fontStyle = FontStyle.Italic
            )
            TafsirRunStyle.NOTE_REF -> SpanStyle(
                baselineShift = BaselineShift.Superscript,
                fontSize = 0.78.em,
                fontWeight = FontWeight.SemiBold
            )
        }
        addStyle(sourceStyle, start, end)

        if (run.style == TafsirRunStyle.NOTE_REF) {
            run.text.trim().toIntOrNull()?.let { noteNumber ->
                addStringAnnotation(
                    tag = NOTE_LINK_TAG,
                    annotation = noteNumber.toString(),
                    start = start,
                    end = end
                )
                addStyle(
                    SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline
                    ),
                    start,
                    end
                )
            }
        }

        // BOLD_ITALIC is the source verse translation itself for Qurtubi/Qushayri.
        // Do not turn its structural source anchor into a cross-reference link.
        if (enableQuranLinks && run.style != TafsirRunStyle.BOLD_ITALIC) {
            TafsirReferenceParser.find(displayText).forEach { match ->
                val linkStart = start + match.start
                val linkEnd = start + match.endExclusive
                val tapStart = (linkStart - 2).coerceAtLeast(start)
                val tapEnd = (linkEnd + 2).coerceAtMost(end)
                addStringAnnotation(
                    tag = QURAN_LINK_TAG,
                    annotation = encodeQuranReference(match.reference),
                    start = tapStart,
                    end = tapEnd
                )
                addStyle(
                    SpanStyle(
                        color = linkColor,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline
                    ),
                    linkStart,
                    linkEnd
                )
            }
        }
    }
}.toAnnotatedString()

private fun encodeQuranReference(reference: QuranReferenceRef): String =
    "${reference.surah}|${reference.startAyah}|${reference.endAyah}"

private fun decodeQuranReference(value: String): QuranReferenceRef? {
    val parts = value.split('|')
    if (parts.size != 3) return null
    val surah = parts[0].toIntOrNull() ?: return null
    val start = parts[1].toIntOrNull() ?: return null
    val end = parts[2].toIntOrNull() ?: return null
    return if (
        TafsirReferenceParser.isCanonical(surah, start) &&
        TafsirReferenceParser.isCanonical(surah, end) &&
        end >= start
    ) {
        QuranReferenceRef(surah, start, end)
    } else {
        null
    }
}
