#!/usr/bin/env python3
"""Apply the Quran Safeguard 0.10.8 reader-only contract.

This migration is intentionally narrow: Quran reader presentation, reader comfort,
and Tafsir display normalization only. It must not alter unlock/budget/protected-app
logic. The script is idempotent so CI can run it again after the generated source is
committed.
"""
from __future__ import annotations

from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]


def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")


def write(rel: str, text: str) -> None:
    path = ROOT / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"0.10.8 migration: expected exactly one {label}, found {count}")
    return text.replace(old, new, 1)


def sub_once(text: str, pattern: str, repl: str, label: str, flags: int = 0) -> str:
    text, count = re.subn(pattern, repl, text, count=1, flags=flags)
    if count != 1:
        raise SystemExit(f"0.10.8 migration: expected exactly one {label}, found {count}")
    return text


# Fast idempotence gate for the second CI pass.
comfort_path = "app/src/main/java/com/quranunlock/guard/ReaderComfortPrefs.kt"
challenge_path = "app/src/main/java/com/quranunlock/guard/MushafReaderActivity.kt"
free_path = "app/src/main/java/com/quranunlock/guard/FreeQuranReaderActivity.kt"
if (
    "READER_CREAM_HEX" in read(comfort_path)
    and "onReaderTap" in read(challenge_path)
    and "onReaderTap" in read(free_path)
    and "JalalaynHonorificPresentation.normalize" in read(
        "app/src/plus/java/com/quranunlock/guard/TafsirRepository.kt"
    )
):
    print("0.10.8 reader migration already applied; nothing to change")
    raise SystemExit(0)


# 1. Reader comfort is brightness-only. Legacy visual-mode APIs remain as harmless
# compatibility shims so old local preferences cannot re-enable a non-cream theme.
write(
    comfort_path,
    '''package com.applicreation0.quransafeguard

import android.content.Context
import android.view.WindowManager

/** Legacy persisted values from <=0.10.7. They no longer change Quran reader colors. */
enum class ReaderVisualMode {
    COMFORT,
    LIGHT,
    DARK
}

/**
 * Shared local comfort settings for every Quran reader.
 *
 * 0.10.8 freezes the reader surface to cream. The sun control adjusts only window
 * brightness and never changes theme, challenge state, Juz/Hizb progression, unlock
 * credit, or protected-app timing.
 */
object ReaderComfortPrefs {
    internal const val READER_CREAM_HEX = "#F7F2E8"
    private const val PREFS = "reader_comfort"
    private const val KEY_VISUAL_MODE = "visual_mode"
    private const val KEY_BRIGHTNESS = "brightness"
    private const val SYSTEM_BRIGHTNESS = -1f

    /** Compatibility read: all historical modes now resolve to the single cream mode. */
    fun visualMode(context: Context): ReaderVisualMode = ReaderVisualMode.COMFORT

    /** Compatibility write: never persist a non-cream visual mode again. */
    fun setVisualMode(context: Context, mode: ReaderVisualMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_VISUAL_MODE, ReaderVisualMode.COMFORT.name)
            .apply()
    }

    /** -1f means follow Android/system brightness. */
    fun brightness(context: Context): Float =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getFloat(KEY_BRIGHTNESS, SYSTEM_BRIGHTNESS)
            .let { value ->
                if (value == SYSTEM_BRIGHTNESS) SYSTEM_BRIGHTNESS else value.coerceIn(0.12f, 1f)
            }

    fun setBrightness(context: Context, value: Float?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_BRIGHTNESS, value?.coerceIn(0.12f, 1f) ?: SYSTEM_BRIGHTNESS)
            .apply()
    }

    fun applyBrightness(window: android.view.Window, value: Float) {
        val params = window.attributes
        params.screenBrightness = if (value < 0f) {
            WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        } else {
            value.coerceIn(0.12f, 1f)
        }
        window.attributes = params
    }

    fun pageBackground(): String = READER_CREAM_HEX

    /** Compatibility overload for <=0.10.7 callers; every mode is cream in 0.10.8. */
    fun pageBackground(mode: ReaderVisualMode): String = READER_CREAM_HEX
}
'''
)


# 2. Free reader: fixed cream shell, brightness-only sun control, automatic calm
# immersion, tap-to-reveal chrome, and screen-awake while the reader is visible.
free = read(free_path)
free = replace_once(
    free,
    "import android.view.MotionEvent\n",
    "import android.view.MotionEvent\nimport android.view.WindowManager\n",
    "Free reader WindowManager import",
)
free = replace_once(
    free,
    "import kotlinx.coroutines.launch\n",
    "import kotlinx.coroutines.delay\nimport kotlinx.coroutines.launch\n",
    "Free reader delay import",
)
free = replace_once(
    free,
    "        super.onCreate(savedInstanceState)\n\n        val reference =",
    "        super.onCreate(savedInstanceState)\n        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)\n\n        val reference =",
    "Free reader keep-screen-on flag",
)
free = sub_once(
    free,
    r'''                var comfortOpen by remember \{ mutableStateOf\(false\) \}\n                var pureReading by remember \{ mutableStateOf\(false\) \}\n                var tafsirExpanded by remember \{ mutableStateOf\(false\) \}\n                var visualMode by remember \{.*?                val chromeHidden = pureReading && !referenceMode && !quickNavOpen && !comfortOpen\n''',
    '''                var comfortOpen by remember { mutableStateOf(false) }
                var pureReading by remember { mutableStateOf(false) }
                var tafsirExpanded by remember { mutableStateOf(false) }
                var chromeInteraction by remember { mutableIntStateOf(0) }
                var readerBrightness by remember {
                    mutableFloatStateOf(ReaderComfortPrefs.brightness(this@FreeQuranReaderActivity))
                }
                var message by remember { mutableStateOf("") }

                val shellColor = SafeguardReadingSurface
                val shellPrimary = MaterialTheme.colorScheme.primary
                val shellSecondary = MaterialTheme.colorScheme.secondary
                val shellMuted = MaterialTheme.colorScheme.onSurfaceVariant
                val chromeHidden = pureReading && !referenceMode && !quickNavOpen && !comfortOpen
''',
    "Free reader visual-mode state block",
    re.S,
)
free = replace_once(
    free,
    '''                LaunchedEffect(readerBrightness) {
                    ReaderComfortPrefs.applyBrightness(window, readerBrightness)
                }
''',
    '''                LaunchedEffect(readerBrightness) {
                    ReaderComfortPrefs.applyBrightness(window, readerBrightness)
                }

                LaunchedEffect(
                    page,
                    chromeInteraction,
                    selectedTafsirVerse,
                    quickNavOpen,
                    comfortOpen,
                    pureReading
                ) {
                    if (!referenceMode &&
                        selectedTafsirVerse == null &&
                        !quickNavOpen &&
                        !comfortOpen &&
                        !pureReading
                    ) {
                        delay(2_500L)
                        pureReading = true
                    }
                }
''',
    "Free reader immersion effect",
)
free = replace_once(
    free,
    "                    page = nextPage\n                    quickNavOpen = false",
    "                    page = nextPage\n                    pureReading = false\n                    chromeInteraction += 1\n                    quickNavOpen = false",
    "Free reader page interaction reset",
)
free = free.replace('"Aa / ☼"', '"☼"')
free = sub_once(
    free,
    r'''\n                                        Text\(\n                                            "Masquer",.*?\n                                        \)''',
    "",
    "Free reader manual Masquer control",
    re.S,
)
free = replace_once(
    free,
    '''                                            Text(
                                                "Confort de lecture",
''',
    '''                                            Text(
                                                "Luminosité",
''',
    "Free reader brightness panel title",
)
free = sub_once(
    free,
    r'''\n                                            Row\(\n                                                modifier = Modifier\.fillMaxWidth\(\),\n                                                horizontalArrangement = Arrangement\.spacedBy\(6\.dp\)\n                                            \) \{\n                                                ReaderVisualMode\.entries\.forEach \{ mode ->.*?\n                                            \}\n                                            Text\(''',
    "\n                                            Text(",
    "Free reader theme-choice row",
    re.S,
)
free = sub_once(
    free,
    r'''\n                                Text\(\n                                    if \(TafsirEdition\.isEnabled\) message else\n                                        "Lecture libre du Mushaf de Médine\.",.*?\n                                \)''',
    "",
    "Free reader explanatory message line",
    re.S,
)
free = free.replace("key(visualMode) {", "key(ReaderComfortPrefs.READER_CREAM_HEX) {")
free = free.replace(
    "pageBackground = ReaderComfortPrefs.pageBackground(visualMode),",
    "pageBackground = ReaderComfortPrefs.pageBackground(),",
)
free = replace_once(
    free,
    '''                                            onSwipePrevious = { showPage(page - 1) },
                                            onSwipeNext = { showPage(page + 1) },
                                            onVerseTapped = { verse ->''',
    '''                                            onSwipePrevious = { showPage(page - 1) },
                                            onSwipeNext = { showPage(page + 1) },
                                            onReaderTap = {
                                                pureReading = false
                                                chromeInteraction += 1
                                            },
                                            onVerseTapped = { verse ->''',
    "Free reader page tap callback",
)
free = replace_once(
    free,
    ".clickable { pureReading = false }",
    ".clickable {\n                                        pureReading = false\n                                        chromeInteraction += 1\n                                    }",
    "Free reader reveal interaction",
)
free = replace_once(
    free,
    '''    onSwipePrevious: () -> Unit,
    onSwipeNext: () -> Unit,
    onVerseTapped: (VerseRef) -> Unit
) {''',
    '''    onSwipePrevious: () -> Unit,
    onSwipeNext: () -> Unit,
    onReaderTap: () -> Unit,
    onVerseTapped: (VerseRef) -> Unit
) {''',
    "Free reader WebView tap signature",
)
free = replace_once(
    free,
    '''    val currentOnSwipePrevious = rememberUpdatedState(onSwipePrevious)
    val currentOnSwipeNext = rememberUpdatedState(onSwipeNext)
    val currentOnVerseTapped = rememberUpdatedState(onVerseTapped)
''',
    '''    val currentOnSwipePrevious = rememberUpdatedState(onSwipePrevious)
    val currentOnSwipeNext = rememberUpdatedState(onSwipeNext)
    val currentOnReaderTap = rememberUpdatedState(onReaderTap)
    val currentOnVerseTapped = rememberUpdatedState(onVerseTapped)
''',
    "Free reader WebView tap state",
)
free = replace_once(
    free,
    '''                                ReaderSwipe.NEXT -> currentOnSwipeNext.value()
                                ReaderSwipe.PREVIOUS -> currentOnSwipePrevious.value()
                                null -> Unit
''',
    '''                                ReaderSwipe.NEXT -> currentOnSwipeNext.value()
                                ReaderSwipe.PREVIOUS -> currentOnSwipePrevious.value()
                                null -> currentOnReaderTap.value()
''',
    "Free reader WebView tap dispatch",
)
write(free_path, free)


# 3. Challenge/morning reader: same cream e-reader shell, safe system-bar insets,
# brightness-only sun control, calm auto-hiding header with tap reveal. Critical
# validation/timer/footer behavior is deliberately left intact.
challenge = read(challenge_path)
challenge = replace_once(
    challenge,
    "import android.view.MotionEvent\n",
    "import android.view.MotionEvent\nimport android.view.WindowManager\n",
    "Challenge reader WindowManager import",
)
challenge = replace_once(
    challenge,
    "import androidx.compose.animation.togetherWith\n",
    "import androidx.compose.animation.togetherWith\nimport androidx.compose.foundation.clickable\n",
    "Challenge reader clickable import",
)
challenge = replace_once(
    challenge,
    "import androidx.compose.foundation.layout.padding\n",
    "import androidx.compose.foundation.layout.padding\nimport androidx.compose.foundation.layout.navigationBarsPadding\nimport androidx.compose.foundation.layout.statusBarsPadding\n",
    "Challenge reader system inset imports",
)
challenge = replace_once(
    challenge,
    "import androidx.compose.material3.Surface\n",
    "import androidx.compose.material3.Slider\nimport androidx.compose.material3.Surface\n",
    "Challenge reader slider import",
)
challenge = replace_once(
    challenge,
    "import androidx.compose.runtime.mutableIntStateOf\n",
    "import androidx.compose.runtime.mutableFloatStateOf\nimport androidx.compose.runtime.mutableIntStateOf\n",
    "Challenge reader float state import",
)
challenge = replace_once(
    challenge,
    "        super.onCreate(savedInstanceState)\n\n        challengeKey =",
    "        super.onCreate(savedInstanceState)\n        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)\n\n        challengeKey =",
    "Challenge reader keep-screen-on flag",
)
challenge = replace_once(
    challenge,
    '''                var quotaReached by remember {
                    mutableStateOf(false)
                }
                val quotaPageCount = remember { initialPages.size }
''',
    '''                var quotaReached by remember {
                    mutableStateOf(false)
                }
                var brightnessOpen by remember { mutableStateOf(false) }
                var chromeVisible by remember { mutableStateOf(true) }
                var chromeInteraction by remember { mutableIntStateOf(0) }
                var readerBrightness by remember {
                    mutableFloatStateOf(ReaderComfortPrefs.brightness(this@MushafReaderActivity))
                }
                val quotaPageCount = remember { initialPages.size }
''',
    "Challenge reader comfort state",
)
challenge = replace_once(
    challenge,
    '''                BackHandler(enabled = selectedTafsirVerse != null) {
                    closeTafsir()
                }
''',
    '''                LaunchedEffect(readerBrightness) {
                    ReaderComfortPrefs.applyBrightness(window, readerBrightness)
                }

                LaunchedEffect(
                    chromeVisible,
                    chromeInteraction,
                    brightnessOpen,
                    selectedTafsirVerse
                ) {
                    if (chromeVisible && !brightnessOpen && selectedTafsirVerse == null) {
                        delay(2_500L)
                        chromeVisible = false
                    }
                }

                BackHandler(enabled = brightnessOpen && selectedTafsirVerse == null) {
                    brightnessOpen = false
                }

                BackHandler(enabled = selectedTafsirVerse != null) {
                    closeTafsir()
                }
''',
    "Challenge reader comfort effects",
)
challenge = sub_once(
    challenge,
    r'''                Surface\(modifier = Modifier\.fillMaxSize\(\)\) \{\n                    BoxWithConstraints\(modifier = Modifier\.fillMaxSize\(\)\) \{\n                        Column\(\n                            modifier = Modifier\n                                \.fillMaxSize\(\)\n                                \.padding\(horizontal = 0\.dp, vertical = 4\.dp\)\n                        \) \{\n                        Text\(\n                            "Mushaf de Médine • Page \$currentDisplayedPage",.*?                        Spacer\(Modifier\.height\(3\.dp\)\)\n''',
    '''                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SafeguardReadingSurface
                ) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .navigationBarsPadding()
                                .padding(horizontal = 0.dp, vertical = 2.dp)
                        ) {
                        if (chromeVisible || brightnessOpen) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Qur’an & Tafsîr",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "☼",
                                    modifier = Modifier
                                        .clickable {
                                            brightnessOpen = !brightnessOpen
                                            chromeVisible = true
                                            chromeInteraction += 1
                                        }
                                        .padding(horizontal = 7.dp, vertical = 5.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                "Mushaf de Médine • Page $currentDisplayedPage / 604",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (brightnessOpen) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    shape = MaterialTheme.shapes.small,
                                    color = SafeguardReadingSurface,
                                    tonalElevation = 0.dp
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            "Luminosité",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            if (readerBrightness < 0f) {
                                                "Luminosité : téléphone"
                                            } else {
                                                "Luminosité : ${(readerBrightness * 100).toInt()} %"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Slider(
                                            value = if (readerBrightness < 0f) 0.72f else readerBrightness,
                                            onValueChange = { value ->
                                                readerBrightness = value.coerceIn(0.12f, 1f)
                                                ReaderComfortPrefs.setBrightness(
                                                    this@MushafReaderActivity,
                                                    readerBrightness
                                                )
                                            },
                                            valueRange = 0.12f..1f,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            SafeguardOutlinedButton(
                                                modifier = Modifier.weight(1f),
                                                onClick = {
                                                    readerBrightness = -1f
                                                    ReaderComfortPrefs.setBrightness(
                                                        this@MushafReaderActivity,
                                                        null
                                                    )
                                                }
                                            ) { Text("Auto") }
                                            SafeguardOutlinedButton(
                                                modifier = Modifier.weight(1f),
                                                onClick = { brightnessOpen = false }
                                            ) { Text("Fermer") }
                                        }
                                    }
                                }
                            }
                        }

                        Text(
                            when {
                                quotaReached ->
                                    "Quota de $quotaPageCount pages atteint • lecture libre"
                                level == ChallengeLevel.MORNING ->
                                    "Filtre matinal • page ${displayedIndex + 1}/$quotaPageCount"
                                level == ChallengeLevel.MICRO ->
                                    "Pause 15 minutes • page 1/1"
                                else ->
                                    "Palier 90 minutes • page ${displayedIndex + 1}/$quotaPageCount"
                            },
                            modifier = Modifier.padding(horizontal = 12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (sectionContext.isNotBlank()) {
                            Text(
                                sectionContext,
                                modifier = Modifier.padding(horizontal = 12.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            when {
                                quotaReached -> "Quota atteint • sortie libre"
                                viewingCompletedPage -> "Page validée"
                                readingMs >= GuardPrefs.MIN_READING_MS -> "01:00 atteint • prêt à valider"
                                else -> formatReadingDuration(readingMs) + " / 01:00"
                            },
                            modifier = Modifier.padding(horizontal = 12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (viewingCompletedPage || canValidate) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Spacer(Modifier.height(3.dp))
''',
    "Challenge reader top shell",
    re.S,
)
challenge = replace_once(
    challenge,
    '''                                    onSwipeNext = {
                                        validateAndAdvance()
                                    },
                                    onVerseTapped = { verse ->''',
    '''                                    onSwipeNext = {
                                        validateAndAdvance()
                                    },
                                    onReaderTap = {
                                        chromeVisible = true
                                        chromeInteraction += 1
                                    },
                                    onVerseTapped = { verse ->''',
    "Challenge reader page tap callback",
)
challenge = replace_once(
    challenge,
    '''    onSwipePrevious: () -> Unit,
    onSwipeNext: () -> Unit,
    onVerseTapped: (VerseRef) -> Unit,
    onFailure: () -> Unit
) {''',
    '''    onSwipePrevious: () -> Unit,
    onSwipeNext: () -> Unit,
    onReaderTap: () -> Unit,
    onVerseTapped: (VerseRef) -> Unit,
    onFailure: () -> Unit
) {''',
    "Challenge WebView tap signature",
)
challenge = replace_once(
    challenge,
    '''    val currentOnSwipePrevious = rememberUpdatedState(onSwipePrevious)
    val currentOnSwipeNext = rememberUpdatedState(onSwipeNext)
    val currentOnVerseTapped = rememberUpdatedState(onVerseTapped)
''',
    '''    val currentOnSwipePrevious = rememberUpdatedState(onSwipePrevious)
    val currentOnSwipeNext = rememberUpdatedState(onSwipeNext)
    val currentOnReaderTap = rememberUpdatedState(onReaderTap)
    val currentOnVerseTapped = rememberUpdatedState(onVerseTapped)
''',
    "Challenge WebView tap state",
)
challenge = replace_once(
    challenge,
    "                setBackgroundColor(android.graphics.Color.WHITE)",
    "                setBackgroundColor(\n                    android.graphics.Color.parseColor(ReaderComfortPrefs.pageBackground())\n                )",
    "Challenge WebView cream background",
)
challenge = replace_once(
    challenge,
    '''                                ReaderSwipe.NEXT -> currentOnSwipeNext.value()
                                ReaderSwipe.PREVIOUS -> currentOnSwipePrevious.value()
                                null -> Unit
''',
    '''                                ReaderSwipe.NEXT -> currentOnSwipeNext.value()
                                ReaderSwipe.PREVIOUS -> currentOnSwipePrevious.value()
                                null -> currentOnReaderTap.value()
''',
    "Challenge WebView tap dispatch",
)
challenge = challenge.replace(
    "background: #ffffff;",
    "background: ${ReaderComfortPrefs.READER_CREAM_HEX};",
)
write(challenge_path, challenge)


# 4. Tafsir: poetry remains semantically styled but receives no application-authored
# explanatory label. Source text itself is untouched.
panel_path = "app/src/plus/java/com/quranunlock/guard/TafsirPanel.kt"
panel = read(panel_path)
panel = sub_once(
    panel,
    r'''\n            if \(isPoetry\) \{\n                Text\(\n                    text = "Poésie · lignes conservées selon l’édition source",.*?\n                \)\n            \}''',
    "",
    "Qushayri poetry explanatory label",
    re.S,
)
write(panel_path, panel)


# 5. Jalalayn stays English and byte-identical on disk. Only presentation-time
# honorific shorthand is normalized; no translation or paraphrase is introduced.
honorific_path = "app/src/plus/java/com/quranunlock/guard/JalalaynHonorificPresentation.kt"
write(
    honorific_path,
    '''package com.applicreation0.quransafeguard

/**
 * Display-only normalization for the approved English Jalalayn corpus.
 *
 * The bundled SQLite and its checksum remain unchanged. These substitutions only
 * expand source honorific shorthand while preserving every surrounding English word.
 */
internal object JalalaynHonorificPresentation {
    private val replacements = listOf(
        "(ṣʿa)" to "ﷺ",
        "(ṣ)" to "ﷺ",
        "(ʿa)" to "عليه السلام"
    )

    fun normalize(source: String): String = replacements.fold(source) { text, (from, to) ->
        text.replace(from, to)
    }
}
''',
)
repo_path = "app/src/plus/java/com/quranunlock/guard/TafsirRepository.kt"
repo = read(repo_path)
repo = replace_once(
    repo,
    '                val text = value.getString("text")\n',
    '                val text = JalalaynHonorificPresentation.normalize(value.getString("text"))\n',
    "Jalalayn display normalization hook",
)
write(repo_path, repo)


# 6. Regression tests for fixed cream rendering and English-preserving honorifics.
write(
    "app/src/test/java/com/quranunlock/guard/ReaderComfortPrefsTest.kt",
    '''package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderComfortPrefsTest {
    @Test
    fun everyLegacyVisualModeNowUsesTheSameCreamBackground() {
        assertEquals("#F7F2E8", ReaderComfortPrefs.pageBackground())
        ReaderVisualMode.entries.forEach { mode ->
            assertEquals("#F7F2E8", ReaderComfortPrefs.pageBackground(mode))
        }
    }
}
''',
)
write(
    "app/src/testPlus/java/com/quranunlock/guard/JalalaynHonorificPresentationTest.kt",
    '''package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Test

class JalalaynHonorificPresentationTest {
    @Test
    fun prophetHonorificIsNormalizedWithoutTranslatingEnglish() {
        val source = "The Prophet (ṣ) is mentioned in this English commentary."
        assertEquals(
            "The Prophet ﷺ is mentioned in this English commentary.",
            JalalaynHonorificPresentation.normalize(source)
        )
    }

    @Test
    fun otherProphetHonorificIsNormalizedWithoutChangingTheName() {
        val source = "Moses (ʿa) returned to his people."
        assertEquals(
            "Moses عليه السلام returned to his people.",
            JalalaynHonorificPresentation.normalize(source)
        )
    }

    @Test
    fun unrelatedEnglishIsByteForByteUnchanged() {
        val source = "This sentence contains no honorific shorthand."
        assertEquals(source, JalalaynHonorificPresentation.normalize(source))
    }
}
''',
)


# 7. Release-blocking static contract. This supplements existing behavioral tests and
# explicitly proves that the 60-second/unlock path survived the reader UI refactor.
verify_path = "scripts/verify_0108_reader_contract.py"
write(
    verify_path,
    '''#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def text(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")

def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit("0.10.8 reader contract: " + message)

free = text("app/src/main/java/com/quranunlock/guard/FreeQuranReaderActivity.kt")
challenge = text("app/src/main/java/com/quranunlock/guard/MushafReaderActivity.kt")
comfort = text("app/src/main/java/com/quranunlock/guard/ReaderComfortPrefs.kt")
panel = text("app/src/plus/java/com/quranunlock/guard/TafsirPanel.kt")
repo = text("app/src/plus/java/com/quranunlock/guard/TafsirRepository.kt")
honorific = text("app/src/plus/java/com/quranunlock/guard/JalalaynHonorificPresentation.kt")

require('READER_CREAM_HEX = "#F7F2E8"' in comfort, "cream constant missing")
require("ReaderVisualMode.entries.forEach" not in free, "theme choices remain in free reader UI")
require('"Clair"' not in free and '"Sombre"' not in free, "light/dark reader choices remain")
require("ReaderComfortPrefs.pageBackground()" in free, "free reader is not pinned to cream")
require("ReaderComfortPrefs.pageBackground()" in challenge, "challenge reader is not pinned to cream")
require("android.graphics.Color.WHITE" not in challenge, "challenge WebView still forces white")
require("background: #ffffff" not in challenge, "challenge HTML still forces white")
require("color = SafeguardReadingSurface" in challenge, "challenge shell is not cream")

for name, source in (("free", free), ("challenge", challenge)):
    require("FLAG_KEEP_SCREEN_ON" in source, f"{name} reader does not stay awake")
    require("statusBarsPadding()" in source, f"{name} reader does not respect status bar")
    require("navigationBarsPadding()" in source, f"{name} reader does not respect navigation bar")
    require("delay(2_500L)" in source, f"{name} reader lacks calm auto-hide")
    require("onReaderTap" in source, f"{name} reader lacks tap-to-reveal")
    require('"☼"' in source, f"{name} reader lacks brightness control")

require("Poésie · lignes conservées selon l’édition source" not in panel,
        "application-authored poetry explanation still rendered")
require("JalalaynHonorificPresentation.normalize" in repo,
        "Jalalayn presentation normalization is not wired")
for token in ('"(ṣ)" to "ﷺ"', '"(ʿa)" to "عليه السلام"'):
    require(token in honorific, "missing honorific mapping: " + token)
require("translate" not in honorific.lower(), "honorific layer must not implement translation")

# Existing safety-critical reader behavior must remain structurally present.
for token in (
    "SafeguardCyclePrefs.currentPlan",
    "ReadingValidationPolicy.canValidate",
    "GuardPrefs.completeReadingAndUnlock",
    "mayCountActiveReading()",
    "override fun onTopResumedActivityChanged",
    "GuardPrefs.MIN_READING_MS",
    "TargetReturnCoordinator.returnImmediately",
):
    require(token in challenge, "critical reading/unlock contract disappeared: " + token)

# No editorial/explanatory prose line may be injected above Qushayri poetry.
require("if (isPoetry) {\n                Text(" not in panel,
        "poetry renderer still injects an application Text label")

print("0.10.8 Quran reader contract PASS")
print("- fixed cream readers; brightness-only sun; safe system insets")
print("- calm immersion + tap reveal; keep-screen-on")
print("- Tafsir editorial poetry label removed")
print("- Jalalayn remains English; honorific shorthand normalized at display time")
print("- 60-second/foreground/unlock structural safeguards retained")
''',
)

# Wire the new contract into every normal preBuild/release audit.
build_path = "app/build.gradle.kts"
build = read(build_path)
build = replace_once(
    build,
    '''val verifyHikam264 by tasks.registering(Exec::class) {
    workingDir = rootProject.projectDir
    commandLine("python3", "scripts/verify_hikam_264.py")
}
''',
    '''val verifyHikam264 by tasks.registering(Exec::class) {
    workingDir = rootProject.projectDir
    commandLine("python3", "scripts/verify_hikam_264.py")
}

val verify0108ReaderContract by tasks.registering(Exec::class) {
    workingDir = rootProject.projectDir
    commandLine("python3", "scripts/verify_0108_reader_contract.py")
}
''',
    "0.10.8 Gradle audit task",
)
build = replace_once(
    build,
    "    dependsOn(verifyEditionIsolation)\n}\n\nandroid {",
    "    dependsOn(verifyEditionIsolation)\n    dependsOn(verify0108ReaderContract)\n}\n\nandroid {",
    "0.10.8 release-audit dependency",
)
build = replace_once(
    build,
    "    dependsOn(verifyEditionIsolation)\n}\n\ndependencies {",
    "    dependsOn(verifyEditionIsolation)\n    dependsOn(verify0108ReaderContract)\n}\n\ndependencies {",
    "0.10.8 preBuild dependency",
)
write(build_path, build)

print("Applied Quran Safeguard 0.10.8 reader-only migration")
