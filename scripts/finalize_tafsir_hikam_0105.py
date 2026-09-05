#!/usr/bin/env python3
"""Finalize the already-approved 0.10.5 Tafsir typography and Hikam terminology rules.

This script is intentionally deterministic and fail-closed. It changes rendering/
terminology code and tests only; it never rewrites Tafsir databases or the 264 Hikam
Arabic/French corpus.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def path(rel: str) -> Path:
    return ROOT / rel


def replace_once(rel: str, old: str, new: str) -> None:
    p = path(rel)
    text = p.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{rel}: expected exactly one replacement target, found {count}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")


# --- Tafsir semantic block model -------------------------------------------------
replace_once(
    "app/src/main/java/com/quranunlock/guard/TafsirModels.kt",
    """enum class TafsirRunStyle {\n    REGULAR,\n    ITALIC,\n    BOLD,\n    BOLD_ITALIC,\n    NOTE_REF\n}\n\ndata class TafsirRun(\n    val style: TafsirRunStyle,\n    val text: String\n)\n""",
    """enum class TafsirRunStyle {\n    REGULAR,\n    ITALIC,\n    BOLD,\n    BOLD_ITALIC,\n    TECHNICAL_TERM,\n    TRANSLITERATION,\n    POETRY,\n    NOTE_REF\n}\n\ndata class TafsirRun(\n    val style: TafsirRunStyle,\n    val text: String\n)\n\nenum class TafsirBlockKind {\n    PROSE,\n    POETRY\n}\n\ndata class TafsirRenderBlock(\n    val kind: TafsirBlockKind,\n    val runs: List<TafsirRun>\n)\n\n/**\n * Preserve explicit source semantics at paragraph level. Poetry is recognized\n * only when the source/extractor supplied the POETRY role; italics alone never\n * imply poetry. This makes the non-justified poetry rule executable without\n * guessing from typography.\n */\ninternal fun splitTafsirRenderBlocks(runs: List<TafsirRun>): List<TafsirRenderBlock> {\n    if (runs.isEmpty()) return emptyList()\n    val blocks = mutableListOf<TafsirRenderBlock>()\n    var currentKind: TafsirBlockKind? = null\n    var currentRuns = mutableListOf<TafsirRun>()\n\n    fun flush() {\n        val kind = currentKind ?: return\n        if (currentRuns.isNotEmpty()) {\n            blocks += TafsirRenderBlock(kind, currentRuns.toList())\n        }\n        currentRuns = mutableListOf()\n    }\n\n    runs.forEach { run ->\n        val kind = if (run.style == TafsirRunStyle.POETRY) {\n            TafsirBlockKind.POETRY\n        } else {\n            TafsirBlockKind.PROSE\n        }\n        if (currentKind != null && currentKind != kind) flush()\n        currentKind = kind\n        currentRuns += run\n    }\n    flush()\n    return blocks\n}\n""",
)

# Jalalayn parser: support explicit semantic tags without changing the approved DB.
replace_once(
    "app/src/plus/java/com/quranunlock/guard/TafsirRepository.kt",
    """                    \"bold_italic\" -> TafsirRunStyle.BOLD_ITALIC\n                    \"note_ref\" -> TafsirRunStyle.NOTE_REF\n""",
    """                    \"bold_italic\" -> TafsirRunStyle.BOLD_ITALIC\n                    \"technical_term\" -> TafsirRunStyle.TECHNICAL_TERM\n                    \"transliteration\" -> TafsirRunStyle.TRANSLITERATION\n                    \"poetry\" -> TafsirRunStyle.POETRY\n                    \"note_ref\" -> TafsirRunStyle.NOTE_REF\n""",
)

# Tafsir renderer: prose stays justified; source-tagged poetry gets Start alignment
# and its own 1.55 line-height. Technical/transliteration roles remain inline.
replace_once(
    "app/src/plus/java/com/quranunlock/guard/TafsirPanel.kt",
    "private const val NOTE_LINE_HEIGHT_RATIO = 1.45f\n",
    "private const val NOTE_LINE_HEIGHT_RATIO = 1.45f\nprivate const val POETRY_LINE_HEIGHT_RATIO = 1.55f\n",
)
replace_once(
    "app/src/plus/java/com/quranunlock/guard/TafsirPanel.kt",
    """ * - bold italic: source-provided Qur'an translation;\n * - superscript: source note call.\n""",
    """ * - bold italic: source-provided Qur'an translation;\n * - technical term: semi-bold only when explicitly source/semantic tagged;\n * - transliteration: italic only when explicitly source/semantic tagged;\n * - poetry: italic, source line breaks preserved, never justified;\n * - superscript: source note call.\n""",
)
old_interactive = '''@Suppress("DEPRECATION")\n@Composable\nprivate fun InteractiveTafsirText(\n    runs: List<TafsirRun>,\n    fontSize: Float,\n    lineHeightRatio: Float,\n    color: Color,\n    linkColor: Color,\n    enableQuranLinks: Boolean,\n    onNoteSelected: (Int) -> Unit,\n    onQuranReferenceSelected: ((QuranReferenceRef) -> Unit)?\n) {\n    val annotated = remember(runs, linkColor, enableQuranLinks) {\n        runsToAnnotatedString(runs, linkColor, enableQuranLinks)\n    }\n    ClickableText(\n        text = annotated,\n        style = TextStyle(\n            color = color,\n            fontSize = fontSize.sp,\n            lineHeight = (fontSize * lineHeightRatio).sp,\n            textAlign = TextAlign.Justify\n        ),\n        onClick = { offset ->\n            annotated.getStringAnnotations(NOTE_LINK_TAG, offset, offset)\n                .firstOrNull()\n                ?.item\n                ?.toIntOrNull()\n                ?.let(onNoteSelected)\n                ?: annotated.getStringAnnotations(QURAN_LINK_TAG, offset, offset)\n                    .firstOrNull()\n                    ?.item\n                    ?.let(::decodeQuranReference)\n                    ?.let { reference -> onQuranReferenceSelected?.invoke(reference) }\n        }\n    )\n}\n'''
new_interactive = '''@Suppress("DEPRECATION")\n@Composable\nprivate fun InteractiveTafsirText(\n    runs: List<TafsirRun>,\n    fontSize: Float,\n    lineHeightRatio: Float,\n    color: Color,\n    linkColor: Color,\n    enableQuranLinks: Boolean,\n    onNoteSelected: (Int) -> Unit,\n    onQuranReferenceSelected: ((QuranReferenceRef) -> Unit)?\n) {\n    val blocks = remember(runs) { splitTafsirRenderBlocks(runs) }\n    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {\n        blocks.forEach { block ->\n            val annotated = remember(block.runs, linkColor, enableQuranLinks) {\n                runsToAnnotatedString(block.runs, linkColor, enableQuranLinks)\n            }\n            val isPoetry = block.kind == TafsirBlockKind.POETRY\n            ClickableText(\n                text = annotated,\n                style = TextStyle(\n                    color = color,\n                    fontSize = fontSize.sp,\n                    lineHeight = (\n                        fontSize * if (isPoetry) POETRY_LINE_HEIGHT_RATIO else lineHeightRatio\n                    ).sp,\n                    textAlign = if (isPoetry) TextAlign.Start else TextAlign.Justify\n                ),\n                onClick = { offset ->\n                    annotated.getStringAnnotations(NOTE_LINK_TAG, offset, offset)\n                        .firstOrNull()\n                        ?.item\n                        ?.toIntOrNull()\n                        ?.let(onNoteSelected)\n                        ?: annotated.getStringAnnotations(QURAN_LINK_TAG, offset, offset)\n                            .firstOrNull()\n                            ?.item\n                            ?.let(::decodeQuranReference)\n                            ?.let { reference -> onQuranReferenceSelected?.invoke(reference) }\n                }\n            )\n        }\n    }\n}\n'''
replace_once("app/src/plus/java/com/quranunlock/guard/TafsirPanel.kt", old_interactive, new_interactive)
replace_once(
    "app/src/plus/java/com/quranunlock/guard/TafsirPanel.kt",
    """            TafsirRunStyle.BOLD_ITALIC -> SpanStyle(\n                fontWeight = FontWeight.Bold,\n                fontStyle = FontStyle.Italic\n            )\n            TafsirRunStyle.NOTE_REF -> SpanStyle(\n""",
    """            TafsirRunStyle.BOLD_ITALIC -> SpanStyle(\n                fontWeight = FontWeight.Bold,\n                fontStyle = FontStyle.Italic\n            )\n            TafsirRunStyle.TECHNICAL_TERM -> SpanStyle(\n                fontWeight = FontWeight.SemiBold\n            )\n            TafsirRunStyle.TRANSLITERATION -> SpanStyle(\n                fontStyle = FontStyle.Italic\n            )\n            TafsirRunStyle.POETRY -> SpanStyle(\n                fontStyle = FontStyle.Italic\n            )\n            TafsirRunStyle.NOTE_REF -> SpanStyle(\n""",
)

# Strengthen the blocking gate so a mere prose contract cannot masquerade as an
# executable poetry/semantic renderer again.
replace_once(
    "scripts/verify_0105_tafsir_typography_contract.py",
    'source_rendering = read("app/src/plus/java/com/quranunlock/guard/TafsirSourceRendering.kt")\n',
    'source_rendering = read("app/src/plus/java/com/quranunlock/guard/TafsirSourceRendering.kt")\nmodels = read("app/src/main/java/com/quranunlock/guard/TafsirModels.kt")\nrepository = read("app/src/plus/java/com/quranunlock/guard/TafsirRepository.kt")\n',
)
replace_once(
    "scripts/verify_0105_tafsir_typography_contract.py",
    """require(\"TextDecoration.Underline\" in renderer, \"interactive source references must remain visually distinct\")\nrequire(\"TafsirRunStyle.BOLD_ITALIC, row.translation.trim()\" in source_rendering,\n        \"Qushayri/Qurtubi source translation must remain bold-italic and source-first\")\n""",
    """require(\"TextDecoration.Underline\" in renderer, \"interactive source references must remain visually distinct\")\nrequire(\"POETRY\" in models and \"TECHNICAL_TERM\" in models and \"TRANSLITERATION\" in models,\n        \"semantic Tafsir run roles are missing from the executable model\")\nrequire(\"splitTafsirRenderBlocks\" in models and \"run.style == TafsirRunStyle.POETRY\" in models,\n        \"source-tagged poetry is not separated from prose at block level\")\nrequire(\"POETRY_LINE_HEIGHT_RATIO = 1.55f\" in renderer, \"poetry line-height rule is not executable\")\nrequire(\"block.kind == TafsirBlockKind.POETRY\" in renderer, \"renderer does not branch on poetry semantics\")\nrequire(\"TextAlign.Start else TextAlign.Justify\" in renderer,\n        \"poetry must render Start while prose remains justified\")\nrequire(\"TafsirRunStyle.POETRY -> SpanStyle\" in renderer, \"poetry style rendering missing\")\nrequire(\"TafsirRunStyle.TECHNICAL_TERM -> SpanStyle\" in renderer, \"technical term style missing\")\nrequire(\"TafsirRunStyle.TRANSLITERATION -> SpanStyle\" in renderer, \"transliteration style missing\")\nfor marker in (\n    '\"poetry\" -> TafsirRunStyle.POETRY',\n    '\"technical_term\" -> TafsirRunStyle.TECHNICAL_TERM',\n    '\"transliteration\" -> TafsirRunStyle.TRANSLITERATION',\n):\n    require(marker in repository, f\"repository cannot decode semantic source role: {marker}\")\nrequire(\"TafsirRunStyle.BOLD_ITALIC, row.translation.trim()\" in source_rendering,\n        \"Qushayri/Qurtubi source translation must remain bold-italic and source-first\")\n""",
)
replace_once(
    "scripts/verify_0105_tafsir_typography_contract.py",
    'print("- current commentary, translation, note and reference rendering remains consistent with that contract")\n',
    'print("- prose remains justified; explicitly source-tagged poetry is Start-aligned at 1.55x and never inferred from italics")\nprint("- technical-term and transliteration roles are executable without rewriting source wording")\n',
)

# Add an executable unit test for poetry/prose block separation.
replace_once(
    "app/src/test/java/com/quranunlock/guard/TafsirInteractionTest.kt",
    """    @Test\n    fun verticalMovementPinchCancelAndOpenPanelNeverNavigate() {\n""",
    """    @Test\n    fun explicitPoetryStaysSeparateFromJustifiedProse() {\n        val blocks = splitTafsirRenderBlocks(\n            listOf(\n                TafsirRun(TafsirRunStyle.REGULAR, \"Prose A\"),\n                TafsirRun(TafsirRunStyle.POETRY, \"Line 1\\nLine 2\"),\n                TafsirRun(TafsirRunStyle.POETRY, \"\\nLine 3\"),\n                TafsirRun(TafsirRunStyle.REGULAR, \"Prose B\")\n            )\n        )\n        assertEquals(\n            listOf(TafsirBlockKind.PROSE, TafsirBlockKind.POETRY, TafsirBlockKind.PROSE),\n            blocks.map { it.kind }\n        )\n        assertEquals(\n            \"Line 1\\nLine 2\\nLine 3\",\n            blocks[1].runs.joinToString(separator = \"\") { it.text }\n        )\n    }\n\n    @Test\n    fun verticalMovementPinchCancelAndOpenPanelNeverNavigate() {\n""",
)

# --- Hikam technical lexicon -----------------------------------------------------
# Matching moves from substring to diacritic-insensitive exact Arabic word tokens.
# This deliberately rejects ambiguous inflected verbs such as يبسط/يقبض and the
# non-technical bare بسط in "بسط المواهب" (Hikma 176).
replace_once(
    "app/src/main/java/com/quranunlock/guard/HikamTechnicalLexicon.kt",
    """object HikamTechnicalLexicon {\n    private val terms = listOf(\n""",
    """object HikamTechnicalLexicon {\n    const val sourceNote: String =\n        \"Contrôle terminologique secondaire : glossaire Islamic Pearls ; le matn arabe vérifié reste l’autorité.\"\n\n    private val arabicDiacritics = Regex(\"[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]\")\n    private val arabicWord = Regex(\"[\\u0621-\\u063A\\u0641-\\u064A\\u066E-\\u06D3]+\")\n\n    private fun normalizedWords(text: String): Set<String> = arabicWord\n        .findAll(arabicDiacritics.replace(text, \"\"))\n        .map { it.value }\n        .toSet()\n\n    private val terms = listOf(\n""",
)
replace_once(
    "app/src/main/java/com/quranunlock/guard/HikamTechnicalLexicon.kt",
    '            arabicNeedles = listOf("القبض", "قبض")\n',
    '            arabicNeedles = listOf("القبض", "قبضك")\n',
)
replace_once(
    "app/src/main/java/com/quranunlock/guard/HikamTechnicalLexicon.kt",
    '            arabicNeedles = listOf("البسط", "بسط")\n',
    '            arabicNeedles = listOf("البسط", "بسطك")\n',
)
# Add a high-confidence nafs entry: definite noun only, avoiding bare نفس = breath/self.
replace_once(
    "app/src/main/java/com/quranunlock/guard/HikamTechnicalLexicon.kt",
    """        HikamTechnicalTerm(\n            key = \"raja\",\n""" if False else """        HikamTechnicalTerm(\n            key = \"warid\",\n""",
    """        HikamTechnicalTerm(\n            key = \"nafs\",\n            displayTerm = \"nafs\",\n            frenchMeaning = \"âme individuelle ; ego ou soi inférieur selon le contexte spirituel\",\n            arabicNeedles = listOf(\"النفس\")\n        ),\n        HikamTechnicalTerm(\n            key = \"warid\",\n""",
)
replace_once(
    "app/src/main/java/com/quranunlock/guard/HikamTechnicalLexicon.kt",
    """    fun forHikma(hikma: HikmaEntry): List<HikamTechnicalTerm> {\n        val arabic = hikma.canonicalArabicText\n        return terms.filter { term ->\n            term.arabicNeedles.any(arabic::contains)\n        }\n    }\n""",
    """    internal fun forArabicText(arabic: String): List<HikamTechnicalTerm> {\n        val words = normalizedWords(arabic)\n        return terms.filter { term ->\n            term.arabicNeedles.any { needle ->\n                normalizedWords(needle).singleOrNull()?.let(words::contains) == true\n            }\n        }\n    }\n\n    fun forHikma(hikma: HikmaEntry): List<HikamTechnicalTerm> =\n        forArabicText(hikma.canonicalArabicText)\n""",
)

# Hikam readability: French prose is justified; Arabic remains RTL/right.
replace_once(
    "app/src/main/java/com/quranunlock/guard/HikamDetailActivity.kt",
    """                    Text(\n                        hikma.frenchText,\n                        style = MaterialTheme.typography.bodyLarge,\n                        lineHeight = 25.sp\n                    )\n""",
    """                    Text(\n                        hikma.frenchText,\n                        style = MaterialTheme.typography.bodyLarge,\n                        lineHeight = 25.sp,\n                        textAlign = TextAlign.Justify\n                    )\n""",
)
replace_once(
    "app/src/main/java/com/quranunlock/guard/HikamDetailActivity.kt",
    """                    style = MaterialTheme.typography.bodyMedium,\n                    lineHeight = 21.sp\n                )\n            }\n""",
    """                    style = MaterialTheme.typography.bodyMedium,\n                    lineHeight = 21.sp,\n                    textAlign = TextAlign.Justify\n                )\n            }\n            Text(\n                HikamTechnicalLexicon.sourceNote,\n                style = MaterialTheme.typography.bodySmall,\n                color = MaterialTheme.colorScheme.onSurfaceVariant\n            )\n""",
)
replace_once(
    "app/src/main/java/com/quranunlock/guard/HikamDetailActivity.kt",
    """                    style = MaterialTheme.typography.bodyLarge,\n                    lineHeight = 25.sp\n                )\n                Text(\n                    entry.printLocator,\n""",
    """                    style = MaterialTheme.typography.bodyLarge,\n                    lineHeight = 25.sp,\n                    textAlign = TextAlign.Justify\n                )\n                Text(\n                    entry.printLocator,\n""",
)

# Add focused lexicon regression tests.
path("app/src/test/java/com/quranunlock/guard/HikamTechnicalLexiconTest.kt").write_text(
    '''package com.applicreation0.quransafeguard\n\nimport org.junit.Assert.assertFalse\nimport org.junit.Assert.assertTrue\nimport org.junit.Test\n\nclass HikamTechnicalLexiconTest {\n    private fun keys(arabic: String) = HikamTechnicalLexicon\n        .forArabicText(arabic)\n        .map { it.key }\n        .toSet()\n\n    @Test\n    fun technicalNounsAreMatchedAfterRemovingArabicDiacritics() {\n        val found = keys(\"الْقَبْضُ والبَسْطُ والوَارِدُ\")\n        assertTrue(\"qabd\" in found)\n        assertTrue(\"bast\" in found)\n        assertTrue(\"warid\" in found)\n    }\n\n    @Test\n    fun ordinaryVerbOrCarpetMetaphorDoesNotCreateFalseTechnicalTerm() {\n        assertFalse(\"qabd\" in keys(\"تارة يقبض ذلك عنك\"))\n        assertFalse(\"bast\" in keys(\"الفاقات بسط المواهب\"))\n    }\n\n    @Test\n    fun bareNafsIsNotOverclassifiedButDefiniteTechnicalNafsIsAvailable() {\n        assertFalse(\"nafs\" in keys(\"ما من نفس تبديه\"))\n        assertTrue(\"nafs\" in keys(\"والقبض لا حظ للنفس فيه\"))\n    }\n}\n''',
    encoding="utf-8",
)

# Replace the loose substring-oriented review aid by a blocking exact-token gate.
path("scripts/audit_hikam_technical_lexicon_0105.py").write_text(
    r'''#!/usr/bin/env python3
"""Fail-closed review of high-signal Sufi vocabulary in the 264 verified Hikam.

Arabic remains authoritative. Matching is diacritic-insensitive but token-exact so
ordinary inflected verbs and homographs are not silently turned into technical
terms. The script never rewrites French; it only blocks when a high-confidence
technical noun is present and the French translation has no acceptable contextual
rendering at all.
"""
from __future__ import annotations
import json, re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CORPUS = ROOT / "app/src/main/assets/hikam/al_hikam_verified.json"
DIACRITICS = re.compile(r"[\u064B-\u065F\u0670\u06D6-\u06ED]")
ARABIC_WORD = re.compile(r"[\u0621-\u063A\u0641-\u064A\u066E-\u06D3]+")


def words(text: str) -> set[str]:
    return set(ARABIC_WORD.findall(DIACRITICS.sub("", text)))


def compact(text: str) -> str:
    return re.sub(r"\s+", " ", text).strip().lower()

# Token lists deliberately prefer unambiguous noun forms. Bare بسط is excluded
# because Hikma 176 uses it in the carpet metaphor بسط المواهب, not the basṭ state.
# Bare نفس is excluded because it can mean breath/self rather than technical nafs.
TERMS = {
    "tajrid": ({"التجريد", "تجريد"}, ("dépouillement", "détachement", "dénuement")),
    "asbab": ({"الأسباب", "اسباب", "أسباب"}, ("cause", "causes", "moyen", "moyens")),
    "tadbir": ({"التدبير", "تدبير"}, ("régenter", "gestion", "gérer", "planifier", "disposition", "administrer", "souci")),
    "himma": ({"الهمة", "همة", "همم"}, ("aspiration", "aspirations", "élan", "élans", "résolution")),
    "marifa": ({"المعرفة", "معرفة"}, ("connaissance", "gnose", "connaître")),
    "tawhid": ({"التوحيد", "توحيد"}, ("unicité", "unité", "tawḥīd", "tawhid")),
    "fana": ({"الفناء", "فناء"}, ("anéantissement", "annihilation", "extinction", "effacement", "disparition")),
    "baqa": ({"البقاء", "بقاء"}, ("subsistance", "permanence", "demeure", "maintien", "rester", "reste")),
    "nafs": ({"النفس"}, ("âme", "ego", "moi", "soi", "part")),
    "warid": ({"الوارد", "الواردات", "واردات"}, ("influx", "inspiration", "survient", "surviennent", "arrive", "arrivent", "venue", "irruption", "afflux")),
    "qabd": ({"القبض"}, ("resserrement", "contraction", "contract")),
    "bast": ({"البسط"}, ("dilatation", "expansion", "élargissement", "déploiement", "élargit")),
    "anwar": ({"الأنوار", "انوار", "أنوار"}, ("lumière", "lumières")),
    "aghyar": ({"الأغيار", "اغيار", "أغيار"}, ("autres", "altérit", "autre que", "créatures", "étrang")),
    "asrar": ({"الأسرار", "اسرار", "أسرار"}, ("secret", "secrets")),
}

entries = json.loads(CORPUS.read_text(encoding="utf-8"))
flags = []
counts = {}
for label, (tokens, accepted) in TERMS.items():
    normalized_tokens = {next(iter(words(token)), token) for token in tokens}
    seen = 0
    for item in entries:
        if not (words(item.get("arabic", "")) & normalized_tokens):
            continue
        seen += 1
        french = compact(item.get("french", ""))
        if not any(term in french for term in accepted):
            flags.append({
                "number": int(item["source_number"]),
                "term": label,
                "arabic": item.get("arabic", ""),
                "french": item.get("french", ""),
            })
    counts[label] = seen

# Explicit regression checks for previously observed false positives.
by_number = {int(item["source_number"]): item for item in entries}
if "bast" in {label for label, (tokens, _) in TERMS.items() if words(by_number[176]["arabic"]) & {next(iter(words(t)), t) for t in tokens}}:
    raise SystemExit("Hikam lexicon audit failure: Hikma 176 carpet metaphor was misclassified as technical basṭ")
if "qabd" in {label for label, (tokens, _) in TERMS.items() if words(by_number[249]["arabic"]) & {next(iter(words(t)), t) for t in tokens}}:
    raise SystemExit("Hikam lexicon audit failure: Hikma 249 verb يقبض was misclassified as technical qabḍ")

payload = {
    "status": "PASS" if not flags else "REVIEW_REQUIRED",
    "authority": "verified Arabic matn",
    "count": len(entries),
    "matching": "diacritic-insensitive exact Arabic word tokens",
    "technical_term_occurrence_counts": counts,
    "review_flag_count": len(flags),
    "review_flags": flags,
}
print(json.dumps(payload, ensure_ascii=False, indent=2))
if flags:
    raise SystemExit(f"0.10.5 HIKAM LEXICON AUDIT FAILURE: {len(flags)} high-confidence occurrences need review")
''',
    encoding="utf-8",
)

# Contract wording: make clear that semantic roles are source-tagged, never guessed.
replace_once(
    "docs/TAFSIR_TYPOGRAPHY_ALIGNMENT_CONTRACT_0.10.5.md",
    "Poetry is never justified. Source line breaks and stanza spacing are retained.\n",
    "Poetry is never justified. Source line breaks and stanza spacing are retained. A poetry block is rendered as such only when the source/extractor explicitly tags that semantic role; italics alone never trigger poetry classification.\n",
)

print("0.10.5 Tafsir/Hikam deterministic finalization patch applied")
print("- no Tafsir database or Hikam Arabic/French corpus file modified")
print("- source-tagged poetry is executable and non-justified")
print("- Hikam terminology uses diacritic-insensitive exact Arabic word matching")
print("- French Hikam/Sharh prose is justified; Arabic remains RTL/right")
