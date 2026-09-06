#!/usr/bin/env python3
"""Prepare Quran Safeguard 0.10.7 from the audited 0.10.6 tree.

This script is intentionally fail-closed and idempotent. It applies only the
0.10.7 changes requested after 0.10.6 field testing:
- strict Accessibility scope: selected targets + Safeguard itself, never outside apps;
- target-window-removal exit proof without observing the next application;
- easier Tafsir Quran-reference activation while preserving source text;
- calmer, retractable free-reader chrome and honest local brightness controls;
- exact 0.10.7 Android metadata and release gates.
"""
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
    if count == 0 and new in text:
        return text
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one source occurrence, got {count}")
    return text.replace(old, new, 1)


def regex_once(text: str, pattern: str, replacement: str, label: str) -> str:
    updated, count = re.subn(pattern, replacement, text, count=1, flags=re.S)
    if count == 0 and re.search(re.escape(replacement[:60]), text):
        return text
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one regex match, got {count}")
    return updated


# ---------------------------------------------------------------------------
# 1) Strict product scope. No System UI/launcher/outside package is observed.
# ---------------------------------------------------------------------------
protected_apps = '''package com.applicreation0.quransafeguard

import android.content.Context

enum class SafeguardTargetCategory {
    SOCIAL,
    BROWSER
}

data class SafeguardTarget(
    val label: String,
    val packageName: String,
    val category: SafeguardTargetCategory
)

/**
 * Quran Safeguard 0.10.7 has a strict, closed Accessibility boundary.
 *
 * Only explicitly selected social apps/browsers and Quran Safeguard itself may
 * enter the runtime event package list. No launcher, System UI, bank, identity,
 * security, health, transport, work or other outside package is admitted even as
 * an anonymous transition signal. Privacy wins over inferring an exit by observing
 * the next application.
 */
object ProtectedApps {
    val socialTargets: List<SafeguardTarget> = listOf(
        SafeguardTarget("WhatsApp", "com.whatsapp", SafeguardTargetCategory.SOCIAL),
        SafeguardTarget("X", "com.twitter.android", SafeguardTargetCategory.SOCIAL),
        SafeguardTarget("Instagram", "com.instagram.android", SafeguardTargetCategory.SOCIAL),
        SafeguardTarget("Facebook", "com.facebook.katana", SafeguardTargetCategory.SOCIAL),
        SafeguardTarget("YouTube", "com.google.android.youtube", SafeguardTargetCategory.SOCIAL),
        SafeguardTarget("TikTok", "com.zhiliaoapp.musically", SafeguardTargetCategory.SOCIAL)
    )

    val browserTargets: List<SafeguardTarget> = listOf(
        SafeguardTarget("Chrome", "com.android.chrome", SafeguardTargetCategory.BROWSER),
        SafeguardTarget("Firefox", "org.mozilla.firefox", SafeguardTargetCategory.BROWSER),
        SafeguardTarget("Microsoft Edge", "com.microsoft.emmx", SafeguardTargetCategory.BROWSER),
        SafeguardTarget("Brave", "com.brave.browser", SafeguardTargetCategory.BROWSER),
        SafeguardTarget("Opera", "com.opera.browser", SafeguardTargetCategory.BROWSER),
        SafeguardTarget("Samsung Internet", "com.sec.android.app.sbrowser", SafeguardTargetCategory.BROWSER),
        SafeguardTarget("DuckDuckGo", "com.duckduckgo.mobile.android", SafeguardTargetCategory.BROWSER),
        SafeguardTarget("Vivaldi", "com.vivaldi.browser", SafeguardTargetCategory.BROWSER)
    )

    val selectableTargets: List<SafeguardTarget> = socialTargets + browserTargets
    val selectableScopePackages: Set<String> =
        selectableTargets.map(SafeguardTarget::packageName).toSet()

    fun isSelectableTarget(packageName: String): Boolean =
        packageName in selectableScopePackages

    fun shouldNeverPersist(context: Context, packageName: String): Boolean =
        packageName != context.packageName && !isSelectableTarget(packageName)

    fun eventScopePackages(context: Context): Set<String> =
        GuardPrefs.protectedPackages(context)
            .filterTo(linkedSetOf())(::isSelectableTarget)
            .also { it += context.packageName }

    fun isEventScopePackage(context: Context, packageName: String): Boolean =
        packageName in eventScopePackages(context)

    fun isProtected(context: Context, packageName: String): Boolean =
        packageName != context.packageName &&
            isSelectableTarget(packageName) &&
            packageName in GuardPrefs.protectedPackages(context)
}
'''
write("app/src/main/java/com/quranunlock/guard/ProtectedApps.kt", protected_apps)

presence_policy = '''package com.applicreation0.quransafeguard

/**
 * Historical 0.10.4 sentinel API retained only as a fail-closed regression guard.
 * It is not called by the 0.10.7 runtime. Unfiltered Accessibility delivery is
 * forbidden, so this can never authorize a broad package scope.
 */
object TargetPresenceScopePolicy {
    @Suppress("UNUSED_PARAMETER")
    fun requiresAnonymousExitSentinel(
        broadRequested: Boolean,
        foregroundPackage: String?,
        runningBudgetPackage: String?,
        selectedTargets: Set<String>
    ): Boolean = false
}
'''
write("app/src/main/java/com/quranunlock/guard/TargetPresenceScopePolicy.kt", presence_policy)

# Static XML is deliberately self-only. The service narrows/expands within the
# selected-target set after GuardPrefs is available; it never needs outside apps.
def accessibility_xml(package_name: str) -> str:
    return f'''<?xml version="1.0" encoding="utf-8"?>
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeWindowStateChanged|typeWindowsChanged|typeViewClicked|typeViewScrolled"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:notificationTimeout="0"
    android:canRetrieveWindowContent="false"
    android:packageNames="{package_name}"
    android:isAccessibilityTool="false"
    android:settingsActivity="com.applicreation0.quransafeguard.MainActivity"
    android:description="@string/accessibility_description" />
'''
write("app/src/main/res/xml/accessibility_service_config.xml",
      accessibility_xml("com.applicreation0.quransafeguard"))
write("app/src/plus/res/xml/accessibility_service_config.xml",
      accessibility_xml("com.applicreation0.quransafeguard.plus"))

window_exit_policy = '''package com.applicreation0.quransafeguard

import android.view.accessibility.AccessibilityEvent

/**
 * Uses only an event emitted by the selected target itself to prove its window
 * disappeared. This can stop target-only time without ever observing which app
 * became foreground next. No inference about the destination package is made.
 */
object TargetWindowExitPolicy {
    fun provesSelectedTargetWindowRemoved(
        eventType: Int,
        windowChanges: Int,
        eventPackage: String,
        runningBudgetPackage: String?
    ): Boolean =
        eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED &&
            windowChanges and AccessibilityEvent.WINDOWS_CHANGE_REMOVED != 0 &&
            runningBudgetPackage != null &&
            eventPackage == runningBudgetPackage
}
'''
write("app/src/main/java/com/quranunlock/guard/TargetWindowExitPolicy.kt", window_exit_policy)

window_exit_test = '''package com.applicreation0.quransafeguard

import android.view.accessibility.AccessibilityEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TargetWindowExitPolicyTest {
    @Test
    fun selectedTargetRemovalProvesExitWithoutKnowingDestination() {
        assertTrue(
            TargetWindowExitPolicy.provesSelectedTargetWindowRemoved(
                AccessibilityEvent.TYPE_WINDOWS_CHANGED,
                AccessibilityEvent.WINDOWS_CHANGE_REMOVED,
                "com.whatsapp",
                "com.whatsapp"
            )
        )
    }

    @Test
    fun unrelatedOrNonRemovalEventCannotStopTargetBudget() {
        assertFalse(
            TargetWindowExitPolicy.provesSelectedTargetWindowRemoved(
                AccessibilityEvent.TYPE_WINDOWS_CHANGED,
                AccessibilityEvent.WINDOWS_CHANGE_ADDED,
                "com.whatsapp",
                "com.whatsapp"
            )
        )
        assertFalse(
            TargetWindowExitPolicy.provesSelectedTargetWindowRemoved(
                AccessibilityEvent.TYPE_WINDOWS_CHANGED,
                AccessibilityEvent.WINDOWS_CHANGE_REMOVED,
                "com.instagram.android",
                "com.whatsapp"
            )
        )
        assertFalse(
            TargetWindowExitPolicy.provesSelectedTargetWindowRemoved(
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                AccessibilityEvent.WINDOWS_CHANGE_REMOVED,
                "com.whatsapp",
                "com.whatsapp"
            )
        )
    }
}
'''
write("app/src/test/java/com/quranunlock/guard/TargetWindowExitPolicyTest.kt", window_exit_test)

service_rel = "app/src/main/java/com/quranunlock/guard/QuranAccessibilityService.kt"
service = read(service_rel)
# Legacy API calls become an unconditional strict scope refresh.
service = service.replace("applyEventPackageScope(broad = false)", "applyEventPackageScope()")
service = service.replace("applyEventPackageScope(broad = true)", "applyEventPackageScope()")
old_scope_re = re.compile(
    r'''    private fun applyEventPackageScope\(\s*broad: Boolean\s*\) \{.*?\n    \}\n\n    /\*\*\n     \* Latest actual foreground/window owner wins\.''',
    re.S,
)
new_scope = '''    private fun applyEventPackageScope() {
        try {
            val info = serviceInfo ?: return
            // 0.10.7: selected targets + Safeguard only. packageNames is never null.
            info.packageNames = ProtectedApps.eventScopePackages(this).toTypedArray()
            setServiceInfo(info)
        } catch (error: Exception) {
            reportNonFatal("EVENT_SCOPE_UPDATE_FAILED", error)
        }
    }

    /**
     * Latest actual foreground/window owner wins.'''
service, count = old_scope_re.subn(new_scope, service, count=1)
if count == 0:
    if "anonymousExitSentinel" in service or "packageNames = if" in service:
        raise RuntimeError("Could not replace legacy Accessibility scope block")
# Add a target-owned removal proof before ordinary foreground processing.
needle = '''        val isProtectedPackage = ProtectedApps.isProtected(this, packageName)

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||'''
insert = '''        val isProtectedPackage = ProtectedApps.isProtected(this, packageName)

        if (isProtectedPackage &&
            TargetWindowExitPolicy.provesSelectedTargetWindowRemoved(
                eventType = event.eventType,
                windowChanges = event.windowChanges,
                eventPackage = packageName,
                runningBudgetPackage = foregroundUnlockedPackage
            )
        ) {
            pauseForegroundBudget(clearForeground = true)
            GuardRuntime.resetForeground()
            cancelPendingLaunches()
            GuardDiagnostics.log(this, "TARGET_WINDOW_REMOVED", packageName)
            applyEventPackageScope()
            return
        }

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||'''
if needle in service:
    service = service.replace(needle, insert, 1)
elif "TargetWindowExitPolicy.provesSelectedTargetWindowRemoved" not in service:
    raise RuntimeError("Could not insert strict target-owned window removal handling")
# Rewrite misleading legacy comments, then fail if any runtime broad-scope marker survives.
service = service.replace(
    "// Fixed-scope model: anything outside selected social/browser targets\n        // and Safeguard itself is a one-shot anonymous exit signal. No label lookup,\n        // category lookup, diagnostic package entry or persistent association.",
    "// Strict model: outside packages are not admitted to Accessibility delivery.\n        // This guard is defensive only and never classifies or persists an outside package."
)
service = service.replace(
    "// Arm the one-shot exit sentinel only after a selected target has become\n        // the sole owner of the shared 15/90-minute presence ledger.\n        applyEventPackageScope()",
    "// Keep the runtime package filter synchronized with the current selected targets.\n        applyEventPackageScope()"
)
for forbidden in ("anonymousExitSentinel", "packageNames = null", "packageNames = if (", "broad = true", "broadRequested ="):
    if forbidden in service:
        raise RuntimeError(f"Strict scope preparation left forbidden runtime marker: {forbidden}")
write(service_rel, service)

# ---------------------------------------------------------------------------
# 2) Free reader: system insets, calmer chrome, retractable lower actions,
#    honest Auto/manual local brightness. No challenge/progression code touched.
# ---------------------------------------------------------------------------
reader_rel = "app/src/main/java/com/quranunlock/guard/FreeQuranReaderActivity.kt"
reader = read(reader_rel)
for import_line, after in (
    ("import androidx.compose.foundation.layout.navigationBarsPadding\n", "import androidx.compose.foundation.layout.height\n"),
    ("import androidx.compose.foundation.layout.statusBarsPadding\n", "import androidx.compose.foundation.layout.padding\n"),
    ("import androidx.compose.material3.TextButton\n", "import androidx.compose.material3.Text\n"),
):
    if import_line.strip() not in reader:
        reader = replace_once(reader, after, after + import_line, f"reader import {import_line.strip()}")
reader = replace_once(
    reader,
    "                var pureReading by remember { mutableStateOf(false) }\n",
    "                var pureReading by remember { mutableStateOf(false) }\n                var bottomActionsOpen by remember { mutableStateOf(false) }\n",
    "bottom action state",
)
reader = replace_once(
    reader,
    "                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {",
    "                    BoxWithConstraints(\n                        modifier = Modifier\n                            .fillMaxSize()\n                            .statusBarsPadding()\n                            .navigationBarsPadding()\n                    ) {",
    "system bar insets",
)
reader = reader.replace('"Qur’an & Tafsîr"', '"Qur’an"')
reader = reader.replace("style = MaterialTheme.typography.titleLarge,", "style = MaterialTheme.typography.titleMedium,", 1)
reader = reader.replace("fontWeight = FontWeight.Bold\n                                    )", "fontWeight = FontWeight.SemiBold\n                                    )", 1)
reader = reader.replace('"Mushaf de Médine • Page $page / $LAST_PAGE"', '"Mushaf de Médine • p. $page/$LAST_PAGE"')
reader = reader.replace('"Mushaf de Médine • Page $page / $LAST_PAGE ${if (quickNavOpen) "▴" else "▾"}"', '"Mushaf de Médine • p. $page/$LAST_PAGE ${if (quickNavOpen) "▴" else "▾"}"')
reader = reader.replace(
    '"Mushaf arabe : balayez vers la droite pour avancer, " +\n                                "vers la gauche pour revenir."',
    '"Lecture libre • touchez un verset pour le Tafsîr."'
)
reader = reader.replace(
    "val shellSecondary = if (darkShell) Color(0xFFD2AE6C) else MaterialTheme.colorScheme.secondary",
    "val shellSecondary = if (darkShell) Color(0xFFC9B88F) else MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)",
)
# Brightness panel: remove fake 72% Auto slider position.
old_brightness = '''                                            Text(
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
                                                        this@FreeQuranReaderActivity,
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
                                                            this@FreeQuranReaderActivity,
                                                            null
                                                        )
                                                    }
                                                ) { Text("Auto") }
                                                SafeguardButton(
                                                    modifier = Modifier.weight(1f),
                                                    onClick = {
                                                        pureReading = true
                                                        comfortOpen = false
                                                    }
                                                ) { Text("Lecture pure") }
                                            }'''
new_brightness = '''                                            Text(
                                                if (readerBrightness < 0f) {
                                                    "Luminosité : automatique (téléphone)"
                                                } else {
                                                    "Luminosité du lecteur : ${(readerBrightness * 100).toInt()} %"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (readerBrightness >= 0f) {
                                                Slider(
                                                    value = readerBrightness,
                                                    onValueChange = { value ->
                                                        readerBrightness = value.coerceIn(0.08f, 1f)
                                                        ReaderComfortPrefs.setBrightness(
                                                            this@FreeQuranReaderActivity,
                                                            readerBrightness
                                                        )
                                                    },
                                                    valueRange = 0.08f..1f,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                SafeguardOutlinedButton(
                                                    modifier = Modifier.weight(1f),
                                                    onClick = {
                                                        if (readerBrightness < 0f) {
                                                            readerBrightness = 0.55f
                                                            ReaderComfortPrefs.setBrightness(
                                                                this@FreeQuranReaderActivity,
                                                                readerBrightness
                                                            )
                                                        } else {
                                                            readerBrightness = -1f
                                                            ReaderComfortPrefs.setBrightness(
                                                                this@FreeQuranReaderActivity,
                                                                null
                                                            )
                                                        }
                                                    }
                                                ) {
                                                    Text(if (readerBrightness < 0f) "Manuel 55 %" else "Auto")
                                                }
                                                SafeguardOutlinedButton(
                                                    modifier = Modifier.weight(1f),
                                                    onClick = {
                                                        pureReading = true
                                                        comfortOpen = false
                                                    }
                                                ) { Text("Lecture pure") }
                                            }'''
reader = replace_once(reader, old_brightness, new_brightness, "reader brightness panel")
# Make both navigation buttons visually secondary.
reader = reader.replace(
    '''                                        SafeguardButton(
                                            modifier = Modifier.weight(1f),
                                            enabled = selectedTafsirVerse == null && page < LAST_PAGE,''',
    '''                                        SafeguardOutlinedButton(
                                            modifier = Modifier.weight(1f),
                                            enabled = selectedTafsirVerse == null && page < LAST_PAGE,''',
    1,
)
# Insert retractable secondary actions before the bookmark action.
bookmark_marker = '''                                    Spacer(Modifier.height(4.dp))
                                    SafeguardOutlinedButton(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp),
                                        enabled = selectedTafsirVerse == null,
                                        onClick = { toggleBookmark() }
                                    ) {'''
bookmark_replacement = '''                                    TextButton(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp),
                                        enabled = selectedTafsirVerse == null,
                                        onClick = { bottomActionsOpen = !bottomActionsOpen }
                                    ) {
                                        Text(
                                            if (bottomActionsOpen) {
                                                "Options de lecture ▴"
                                            } else {
                                                "Options de lecture ▾"
                                            },
                                            color = shellMuted
                                        )
                                    }
                                    if (bottomActionsOpen) {
                                        SafeguardOutlinedButton(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp),
                                            enabled = selectedTafsirVerse == null,
                                            onClick = { toggleBookmark() }
                                        ) {'''
reader = replace_once(reader, bookmark_marker, bookmark_replacement, "retractable reader actions start")
# Close the new conditional after the existing close-reading button.
close_marker = '''                                    SafeguardOutlinedButton(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp),
                                        enabled = selectedTafsirVerse == null,
                                        onClick = { finish() }
                                    ) {
                                        Text("Fermer la lecture")
                                    }
                                }
                            }'''
close_replacement = '''                                    TextButton(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp),
                                        enabled = selectedTafsirVerse == null,
                                        onClick = { finish() }
                                    ) {
                                        Text("Fermer la lecture", color = shellMuted)
                                    }
                                    }
                                }
                            }'''
reader = replace_once(reader, close_marker, close_replacement, "retractable reader actions end")
if "0.72f" in reader:
    raise RuntimeError("Free reader still contains fake Auto brightness position")
write(reader_rel, reader)

comfort_rel = "app/src/main/java/com/quranunlock/guard/ReaderComfortPrefs.kt"
comfort = read(comfort_rel)
comfort = comfort.replace("value.coerceIn(0.12f, 1f)", "value.coerceIn(0.08f, 1f)")
comfort = comfort.replace("value?.coerceIn(0.12f, 1f)", "value?.coerceIn(0.08f, 1f)")
comfort = comfort.replace("value.coerceIn(0.12f, 1f)", "value.coerceIn(0.08f, 1f)")
write(comfort_rel, comfort)

# ---------------------------------------------------------------------------
# 3) Tafsir links: preserve inline source text, add forgiving offset resolution
#    and explicit 48dp reference controls for reliable finger use.
# ---------------------------------------------------------------------------
panel_rel = "app/src/plus/java/com/quranunlock/guard/TafsirPanel.kt"
panel = read(panel_rel)
if "import androidx.compose.foundation.horizontalScroll" not in panel:
    panel = replace_once(panel, "import androidx.compose.foundation.ScrollState\n",
                         "import androidx.compose.foundation.ScrollState\nimport androidx.compose.foundation.horizontalScroll\n",
                         "Tafsir horizontalScroll import")
if "import androidx.compose.foundation.rememberScrollState" not in panel:
    panel = replace_once(panel, "import androidx.compose.foundation.relocation.bringIntoViewRequester\n",
                         "import androidx.compose.foundation.relocation.bringIntoViewRequester\nimport androidx.compose.foundation.rememberScrollState\n",
                         "Tafsir rememberScrollState import")
# Source-tagged poetry remains a distinct block with exact line/stanza boundaries,
# but no longer receives an extra renderer-imposed italic layer.
panel = panel.replace(
    '''            TafsirRunStyle.POETRY -> SpanStyle(
                fontStyle = FontStyle.Italic
            )''',
    '''            TafsirRunStyle.POETRY -> SpanStyle()'''
)
old_click = '''                onClick = { offset ->
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
            )'''
new_click = '''                onClick = { offset ->
                    resolveTafsirAnnotation(annotated, NOTE_LINK_TAG, offset)
                        ?.toIntOrNull()
                        ?.let(onNoteSelected)
                        ?: resolveTafsirAnnotation(annotated, QURAN_LINK_TAG, offset)
                            ?.let(::decodeQuranReference)
                            ?.let { reference -> onQuranReferenceSelected?.invoke(reference) }
                }
            )
            TafsirAccessibleReferenceRow(
                runs = block.runs,
                enableQuranLinks = enableQuranLinks,
                onNoteSelected = onNoteSelected,
                onQuranReferenceSelected = onQuranReferenceSelected
            )'''
panel = replace_once(panel, old_click, new_click, "Tafsir reliable link activation")
# Add helpers before runsToAnnotatedString.
helper_marker = '''private fun runsToAnnotatedString(
    runs: List<TafsirRun>,'''
helper = '''private fun resolveTafsirAnnotation(
    text: AnnotatedString,
    tag: String,
    offset: Int
): String? {
    text.getStringAnnotations(tag, offset, offset).firstOrNull()?.let { return it.item }
    // ClickableText reports a character offset. Snap a near miss to the closest
    // explicit annotation, making small superscripts/references usable by finger
    // without changing the source wording or inventing links.
    for (distance in 1..6) {
        val before = (offset - distance).coerceAtLeast(0)
        text.getStringAnnotations(tag, before, before).firstOrNull()?.let { return it.item }
        val after = (offset + distance).coerceAtMost(text.length)
        text.getStringAnnotations(tag, after, after).firstOrNull()?.let { return it.item }
    }
    return null
}

@Composable
private fun TafsirAccessibleReferenceRow(
    runs: List<TafsirRun>,
    enableQuranLinks: Boolean,
    onNoteSelected: (Int) -> Unit,
    onQuranReferenceSelected: ((QuranReferenceRef) -> Unit)?
) {
    val noteNumbers = remember(runs) {
        runs.asSequence()
            .filter { it.style == TafsirRunStyle.NOTE_REF }
            .mapNotNull { it.text.trim().toIntOrNull() }
            .distinct()
            .toList()
    }
    val quranReferences = remember(runs, enableQuranLinks) {
        if (!enableQuranLinks) {
            emptyList()
        } else {
            runs.asSequence()
                // The bold-italic run is the current source verse translation,
                // not a cross-reference. All explicit references in commentary
                // prose/notes remain eligible.
                .filter { it.style != TafsirRunStyle.BOLD_ITALIC }
                .flatMap { TafsirReferenceParser.find(it.text).asSequence() }
                .map { it.reference }
                .distinct()
                .toList()
        }
    }
    if (noteNumbers.isEmpty() && quranReferences.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        quranReferences.forEach { reference ->
            TextButton(
                modifier = Modifier.heightIn(min = 48.dp),
                onClick = { onQuranReferenceSelected?.invoke(reference) }
            ) {
                Text("Coran ${reference.label}")
            }
        }
        noteNumbers.forEach { number ->
            TextButton(
                modifier = Modifier.heightIn(min = 48.dp),
                onClick = { onNoteSelected(number) }
            ) {
                Text("Note $number")
            }
        }
    }
}

private fun runsToAnnotatedString(
    runs: List<TafsirRun>,'''
panel = replace_once(panel, helper_marker, helper, "Tafsir accessible reference helper")
write(panel_rel, panel)

# Add parser coverage for punctuation/brackets/ranges and ensure false numbers stay plain.
parser_test_rel = "app/src/testPlus/java/com/quranunlock/guard/TafsirReferenceParserTest.kt"
parser_test = read(parser_test_rel)
if "bracketsPunctuationAndUnicodeRangesRemainClickable" not in parser_test:
    insert_at = parser_test.rfind("}")
    extra = '''

    @Test
    fun bracketsPunctuationAndUnicodeRangesRemainClickable() {
        val refs = TafsirReferenceParser.find("See [2:11], 2:12–13; and (3:7—9).")
            .map { it.reference.label }
        assertEquals(listOf("2:11", "2:12–13", "3:7–9"), refs)
    }

    @Test
    fun ordinaryNumbersAndInvalidVersesNeverBecomeLinks() {
        assertTrue(TafsirReferenceParser.find("note 928, page 61, 2:999, 0:1").isEmpty())
    }
'''
    parser_test = parser_test[:insert_at] + extra + parser_test[insert_at:]
write(parser_test_rel, parser_test)

# ---------------------------------------------------------------------------
# 4) Exact version metadata and release-audit tasks.
# ---------------------------------------------------------------------------
build_rel = "app/build.gradle.kts"
build = read(build_rel)
build = re.sub(r"versionCode\s*=\s*(?:22|25|26)", "versionCode = 26", build, count=1)
build = re.sub(r'versionName\s*=\s*"(?:0\.10\.3|0\.10\.6|0\.10\.7)"', 'versionName = "0.10.7"', build, count=1)
# The update-migration release tuple must accept the exact new metadata.
build = build.replace('buildFile.contains("versionCode = 24")', 'buildFile.contains("versionCode = 26")')
build = build.replace('buildFile.contains("versionName = \\\"0.10.5\\\"")', 'buildFile.contains("versionName = \\\"0.10.7\\\"")')
build = build.replace(
    '"Expected either the audited 0.10.3 baseline metadata or prepared 0.10.5 release metadata."',
    '"Expected either the audited 0.10.3 baseline metadata or prepared 0.10.7 release metadata."'
)
# Replace privacy gate wholesale so old sentinel/SystemUI/launcher assertions cannot
# silently bless a regression.
privacy_task = r'''val verifyPrivacyBoundary by tasks\.registering \{.*?\n\}\n\nval verifyUnlockBudgetIntegrity'''
privacy_replacement = '''val verifyPrivacyBoundary by tasks.registering {
    doLast {
        val manifest = file("src/main/AndroidManifest.xml").readText()
        val accessibility = file("src/main/res/xml/accessibility_service_config.xml").readText()
        val plusAccessibility = file("src/plus/res/xml/accessibility_service_config.xml").readText()
        val protectedApps = file("src/main/java/com/quranunlock/guard/ProtectedApps.kt").readText()
        val service = file("src/main/java/com/quranunlock/guard/QuranAccessibilityService.kt").readText()
        val presenceScope = file("src/main/java/com/quranunlock/guard/TargetPresenceScopePolicy.kt").readText()
        val migrationSource = file("src/main/java/com/quranunlock/guard/AppMigrations.kt").readText()

        check(manifest.contains("android:name=\\\".QuranSafeguardApp\\\""))
        check(migrationSource.contains("class QuranSafeguardApp"))
        check(migrationSource.contains("AppMigrations.run(this)"))
        listOf(
            "android.permission.INTERNET",
            "android.permission.QUERY_ALL_PACKAGES",
            "android.permission.PACKAGE_USAGE_STATS",
            "android.permission.READ_PHONE_STATE"
        ).forEach { forbidden -> check(!manifest.contains(forbidden)) }
        check(!manifest.contains("NotificationListenerService"))
        check(accessibility.contains("android:canRetrieveWindowContent=\\\"false\\\""))
        check(plusAccessibility.contains("android:canRetrieveWindowContent=\\\"false\\\""))
        check(accessibility.contains("android:packageNames=\\\"com.applicreation0.quransafeguard\\\""))
        check(plusAccessibility.contains("android:packageNames=\\\"com.applicreation0.quransafeguard.plus\\\""))

        val targets = listOf(
            "com.whatsapp", "com.twitter.android", "com.instagram.android",
            "com.facebook.katana", "com.google.android.youtube", "com.zhiliaoapp.musically",
            "com.android.chrome", "org.mozilla.firefox", "com.microsoft.emmx",
            "com.brave.browser", "com.opera.browser", "com.sec.android.app.sbrowser",
            "com.duckduckgo.mobile.android", "com.vivaldi.browser"
        )
        check(targets.size == 14 && targets.toSet().size == 14)
        targets.forEach { packageName ->
            check(protectedApps.contains(packageName))
            check(!accessibility.contains(packageName)) {
                "Static Accessibility XML must start self-only; targets are added only when selected: $packageName"
            }
            check(manifest.contains("<package android:name=\\\"" + packageName + "\\\" />"))
        }

        check(protectedApps.contains("GuardPrefs.protectedPackages(context)"))
        check(protectedApps.contains("filterTo(linkedSetOf())(::isSelectableTarget)"))
        check(protectedApps.contains("it += context.packageName"))
        listOf("SYSTEM_UI_PACKAGE", "launcherPackage", "transitionSignalPackages").forEach {
            check(!protectedApps.contains(it)) { "Outside transition signal survived strict target-only scope: $it" }
        }
        check(!service.contains("packageNames = null"))
        check(!service.contains("anonymousExitSentinel"))
        check(!service.contains("broad = true"))
        check(!service.contains("TargetPresenceScopePolicy.requiresAnonymousExitSentinel"))
        check(service.contains("info.packageNames = ProtectedApps.eventScopePackages(this).toTypedArray()"))
        check(service.contains("TargetWindowExitPolicy.provesSelectedTargetWindowRemoved"))
        check(presenceScope.contains("): Boolean = false"))
        check(!service.contains("UsageStatsManager"))
        check(!service.contains("FLAG_RETRIEVE_INTERACTIVE_WINDOWS"))
        check(!service.contains("getWindows()") && !service.contains(".windows"))
    }
}

val verifyUnlockBudgetIntegrity'''
build, count = re.subn(privacy_task, lambda _: privacy_replacement, build, count=1, flags=re.S)
if count != 1 and "Static Accessibility XML must start self-only" not in build:
    raise RuntimeError("Could not replace verifyPrivacyBoundary")

protected_task = r'''val verifyProtectedOnlyBoundary by tasks\.registering \{.*?\n\}\n\nval verifyUpdateMigrationIntegrity'''
protected_replacement = '''val verifyProtectedOnlyBoundary by tasks.registering {
    doLast {
        val protectedApps = file("src/main/java/com/quranunlock/guard/ProtectedApps.kt").readText()
        val service = file("src/main/java/com/quranunlock/guard/QuranAccessibilityService.kt").readText()
        val setupUi = file("src/main/java/com/quranunlock/guard/ProtectionSetupActivity.kt").readText()
        val manifest = file("src/main/AndroidManifest.xml").readText()
        val appCatalog = file("src/main/java/com/quranunlock/guard/AppCatalog.kt").readText()
        val prefs = file("src/main/java/com/quranunlock/guard/GuardPrefs.kt").readText()
        val applicationsUi = file("src/main/java/com/quranunlock/guard/ApplicationsActivity.kt").readText()
        val dashboard = file("src/main/java/com/quranunlock/guard/DashboardActivity.kt").readText()
        val selectionTests = file("src/test/java/com/quranunlock/guard/ProtectedSelectionPolicyTest.kt").readText()
        val exitTests = file("src/test/java/com/quranunlock/guard/TargetWindowExitPolicyTest.kt").readText()

        check(!file("src/main/java/com/quranunlock/guard/SensitiveAppsActivity.kt").exists())
        check(!file("src/main/java/com/quranunlock/guard/SensitiveHandoffPolicy.kt").exists())
        check(!protectedApps.contains("isAlwaysAllowed"))
        check(!protectedApps.contains("looksSensitive"))
        check(!service.contains("SensitiveHandoffPolicy"))
        check(!manifest.contains("SensitiveAppsActivity"))
        check(protectedApps.contains("fun isProtected(context: Context, packageName: String): Boolean"))
        check(protectedApps.contains("!isSelectableTarget(packageName)"))
        check(service.contains("applyEventPackageScope()"))
        check(!service.contains("applyEventPackageScope(broad"))
        check(!service.contains("packageNames = null"))
        check(!service.contains("broad = true"))
        check(!protectedApps.contains("transitionSignalPackages"))
        check(service.contains("TARGET_WINDOW_REMOVED"))
        check(exitTests.contains("selectedTargetRemovalProvesExitWithoutKnowingDestination"))
        check(appCatalog.contains("ProtectedApps.selectableTargets.mapNotNull"))
        check(appCatalog.contains("cachedLaunchableTargets"))
        check(applicationsUi.contains("AppCatalog.refresh()"))
        check(applicationsUi.contains("Confirmer pour demain"))
        check(applicationsUi.contains("cochez pour annuler"))
        check(prefs.contains("ProtectedSelectionPolicy.reconcile"))
        check(prefs.contains("installedTargets = installedTargets"))
        check(!prefs.contains("UNINSTALL_CHALLENGE_KEY"))
        check(dashboard.contains("joker(s) utilisé(s) aujourd’hui"))
        listOf(
            "addition_is_immediate",
            "removal_stays_active_until_next_day",
            "reselecting_cancels_pending_removal",
            "pending_removal_is_applied_on_next_day",
            "uninstall_is_immediate_and_clears_pending_entry",
            "missing_legacy_effective_day_is_repaired_to_tomorrow"
        ).forEach { scenario ->
            check(selectionTests.contains("fun " + scenario + "("))
        }
        check(manifest.contains("android:name=\\\".ProtectionSetupActivity\\\""))
        check(setupUi.contains("Activation guidée"))
    }
}

val verifyUpdateMigrationIntegrity'''
build, count = re.subn(protected_task, lambda _: protected_replacement, build, count=1, flags=re.S)
if count != 1 and "selectedTargetRemovalProvesExitWithoutKnowingDestination" not in build:
    raise RuntimeError("Could not replace verifyProtectedOnlyBoundary")
write(build_rel, build)

print("Prepared Quran Safeguard 0.10.7: strict selected-target scope, reliable Tafsir links, calmer reader, versionCode 26")
