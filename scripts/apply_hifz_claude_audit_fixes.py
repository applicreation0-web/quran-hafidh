from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace_once(rel: str, old: str, new: str) -> None:
    path = ROOT / rel
    text = path.read_text(encoding="utf-8")
    if new in text and old not in text:
        print(f"OK already patched: {rel}")
        return
    count = text.count(old)
    if count != 1:
        raise AssertionError(f"{rel}: expected exactly one old fragment, found {count}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")
    print(f"PATCH {rel}")


def remove_once(rel: str, old: str) -> None:
    path = ROOT / rel
    text = path.read_text(encoding="utf-8")
    if old not in text:
        print(f"OK already removed: {rel}")
        return
    count = text.count(old)
    if count != 1:
        raise AssertionError(f"{rel}: expected exactly one removable fragment, found {count}")
    path.write_text(text.replace(old, "", 1), encoding="utf-8")
    print(f"REMOVE {rel}")


# F1/F2 — runtime and UI schedule use the same HifzSchedule plan; remove duplicate fixed durations.
replace_once(
    "hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java",
    "import com.quransafeguard.hifz.core.EligibleCorpus;\nimport com.quransafeguard.hifz.core.VerseRef;\n\nimport java.time.LocalDate;",
    "import com.quransafeguard.hifz.core.DailyPlan;\nimport com.quransafeguard.hifz.core.EligibleCorpus;\nimport com.quransafeguard.hifz.core.HifzSchedule;\nimport com.quransafeguard.hifz.core.SessionKind;\nimport com.quransafeguard.hifz.core.VerseRef;\n\nimport java.time.DayOfWeek;\nimport java.time.LocalDate;",
)
replace_once(
    "hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java",
    "        mushaf = new MushafView(this); mushaf.setListener(this);",
    "        mushaf = new MushafView(this);\n        mushaf.setMaskEntropy(prefs.maskEntropyFor(mode));\n        mushaf.setListener(this);",
)
replace_once(
    "hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java",
    "        int capacity = Math.max(PreviewConfig.SABQI_LINES,\n            (int)Math.floor(PreviewConfig.MURAJAAH_RECENT_SABQI_MINUTES_WORKING * 60.0 / prefs.recentSecondsPerLine()));",
    "        int recentWindowMinutes = HifzSchedule.INSTANCE.planFor(DayOfWeek.SATURDAY).getMorning().getTargetMinutes();\n        int capacity = Math.max(PreviewConfig.SABQI_LINES,\n            (int)Math.floor(recentWindowMinutes * 60.0 / prefs.recentSecondsPerLine()));",
)
replace_once(
    "hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java",
    "    private void closeClockForCompletedSession(){sessionCompleted=true;awaitingValidation=false;clock.pause();clock.reset();prefs.setElapsedFor(mode,0L);}\n    private boolean isTimedMode() {\n        return SABQI_TODAY_REVIEW.equals(mode) || RECENT_SABQI_REVIEW.equals(mode) || MURAJAAH.equals(mode);\n    }\n    private int targetMinutes(){\n        if (SABQI.equals(mode)) return PreviewConfig.SABQI_MINUTES_WORKING;\n        if (SABQI_TODAY_REVIEW.equals(mode)) return PreviewConfig.SABQI_TODAY_REVIEW_MINUTES;\n        if (ITQAN.equals(mode)) return PreviewConfig.ITQAN_MINUTES_WORKING;\n        if (RECENT_SABQI_REVIEW.equals(mode)) return PreviewConfig.WEEKEND_RECENT_REVIEW_MINUTES;\n        java.time.DayOfWeek day = LocalDate.now().getDayOfWeek();\n        return day == java.time.DayOfWeek.TUESDAY || day == java.time.DayOfWeek.THURSDAY\n            ? PreviewConfig.WEEKDAY_MURAJAAH_MINUTES : PreviewConfig.WEEKEND_MURAJAAH_MINUTES;\n    }",
    "    private void closeClockForCompletedSession(){\n        sessionCompleted=true;awaitingValidation=false;clock.pause();clock.reset();prefs.setElapsedFor(mode,0L);prefs.clearMaskEntropy(mode);\n    }\n    private boolean isTimedMode() {\n        return SABQI_TODAY_REVIEW.equals(mode) || RECENT_SABQI_REVIEW.equals(mode) || MURAJAAH.equals(mode);\n    }\n    private int scheduledTargetMinutes(SessionKind kind) {\n        DailyPlan plan = HifzSchedule.INSTANCE.planFor(LocalDate.now().getDayOfWeek());\n        if (plan.getMorning().getKind() == kind) return plan.getMorning().getTargetMinutes();\n        if (plan.getEvening().getKind() == kind) return plan.getEvening().getTargetMinutes();\n        throw new IllegalStateException(\"Mode \" + kind + \" absent du planning \" + LocalDate.now().getDayOfWeek());\n    }\n    private int targetMinutes(){\n        if (SABQI.equals(mode)) return PreviewConfig.SABQI_MINUTES_WORKING;\n        if (SABQI_TODAY_REVIEW.equals(mode)) return scheduledTargetMinutes(SessionKind.SABQI_TODAY_REVIEW);\n        if (ITQAN.equals(mode)) return PreviewConfig.ITQAN_MINUTES_WORKING;\n        if (RECENT_SABQI_REVIEW.equals(mode)) return scheduledTargetMinutes(SessionKind.RECENT_SABQI_REVIEW);\n        return scheduledTargetMinutes(SessionKind.OLD_ITQAN_MURAJAAH);\n    }",
)

remove_once(
    "hifz-app/src/main/java/com/quransafeguard/hifz/preview/PreviewConfig.java",
    "    public static final int MURAJAAH_MINUTES_WORKING = 60;\n",
)
remove_once(
    "hifz-app/src/main/java/com/quransafeguard/hifz/preview/PreviewConfig.java",
    "\n    // Fixed, independent sessions. Minutes are never redistributed between session types.\n    public static final int SABQI_TODAY_REVIEW_MINUTES = 30;\n    public static final int WEEKDAY_MURAJAAH_MINUTES = 60;\n    public static final int WEEKEND_RECENT_REVIEW_MINUTES = 30;\n    public static final int WEEKEND_MURAJAAH_MINUTES = 30;\n",
)
remove_once(
    "hifz-app/src/main/java/com/quransafeguard/hifz/preview/PreviewConfig.java",
    "    public static final int MURAJAAH_RECENT_SABQI_MINUTES_WORKING = 30;\n    public static final int MURAJAAH_ITQAN_MINUTES_WORKING = 30;\n",
)
remove_once(
    "hifz-app/src/main/java/com/quransafeguard/hifz/preview/PreviewConfig.java",
    "\n    /** Legacy v2 helper retained until the runtime session migration is completed in the next task. */\n    public static boolean recentMurajaahComplete(int reviewedLines, int totalRecentLines, long elapsedMs) {\n        if (totalRecentLines <= 0) return true;\n        if (reviewedLines >= totalRecentLines) return true;\n        return elapsedMs >= MURAJAAH_RECENT_SABQI_MINUTES_WORKING * 60_000L;\n    }\n",
)
remove_once(
    "hifz-app/src/test/java/com/quransafeguard/hifz/preview/PreviewConfigTest.java",
    "\n    @Test public void fixedSessionsNeverRedistributeMinutes() {\n        assertEquals(30, PreviewConfig.SABQI_TODAY_REVIEW_MINUTES);\n        assertEquals(60, PreviewConfig.WEEKDAY_MURAJAAH_MINUTES);\n        assertEquals(30, PreviewConfig.WEEKEND_RECENT_REVIEW_MINUTES);\n        assertEquals(30, PreviewConfig.WEEKEND_MURAJAAH_MINUTES);\n    }\n",
)

# F4 — random draw belongs to a logical Hifz session and survives Activity/WebView recreation.
replace_once(
    "hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java",
    "import java.util.ArrayList;\nimport java.util.Comparator;\nimport java.util.List;",
    "import java.util.ArrayList;\nimport java.util.Comparator;\nimport java.util.List;\nimport java.util.Locale;\nimport java.util.UUID;",
)
replace_once(
    "hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java",
    "    public int schema() { return p.getInt(\"schema\", 0); }\n    public LocalDate programStartDate() { return LocalDate.parse(required(\"programStartDate\")); }\n    public void setProgramStartDate(LocalDate value) { p.edit().putString(\"programStartDate\", value.toString()).apply(); }",
    "    public int schema() { return p.getInt(\"schema\", 0); }\n    public LocalDate programStartDate() { return LocalDate.parse(required(\"programStartDate\")); }\n    public void setProgramStartDate(LocalDate value) { p.edit().putString(\"programStartDate\", value.toString()).apply(); }\n\n    private static String maskEntropyKey(String mode) {\n        if (mode == null || mode.trim().isEmpty()) throw new IllegalArgumentException(\"mask entropy mode required\");\n        return \"maskEntropy_\" + mode.toLowerCase(Locale.ROOT);\n    }\n\n    public String maskEntropyFor(String mode) {\n        String key = maskEntropyKey(mode);\n        String current = p.getString(key, \"\");\n        if (current != null && !current.isEmpty()) return current;\n        String created = UUID.randomUUID().toString();\n        if (!p.edit().putString(key, created).commit()) throw new IllegalStateException(\"Unable to persist Hifz mask entropy\");\n        return created;\n    }\n\n    public void clearMaskEntropy(String mode) {\n        if (!p.edit().remove(maskEntropyKey(mode)).commit()) throw new IllegalStateException(\"Unable to clear Hifz mask entropy\");\n    }",
)
remove_once(
    "hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java",
    "    public void setMurajaahCursor(VerseRef value) { putRef(\"murajaahCursor\", value); }\n",
)
replace_once(
    "hifz-app/src/main/java/com/quransafeguard/hifz/preview/MushafView.java",
    "    private final String maskEntropy = java.util.UUID.randomUUID().toString();",
    "    private String maskEntropy;",
)
replace_once(
    "hifz-app/src/main/java/com/quransafeguard/hifz/preview/MushafView.java",
    "        prefs = new HifzPrefs(context);\n        setBackgroundColor(android.graphics.Color.rgb(250, 248, 242));",
    "        prefs = new HifzPrefs(context);\n        maskEntropy = prefs.maskEntropyFor(\"reader_default\");\n        setBackgroundColor(android.graphics.Color.rgb(250, 248, 242));",
)
replace_once(
    "hifz-app/src/main/java/com/quransafeguard/hifz/preview/MushafView.java",
    "        s.setBuiltInZoomControls(true);",
    "        s.setBuiltInZoomControls(false);",
)
replace_once(
    "hifz-app/src/main/java/com/quransafeguard/hifz/preview/MushafView.java",
    "    public void setListener(Listener value) {",
    "    public void setMaskEntropy(String value) {\n        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(\"mask entropy required\");\n        maskEntropy = value;\n    }\n\n    public void setListener(Listener value) {",
)

# F3 — canonical fit cannot be overridden with a 4x WebView pinch zoom.
replace_once(
    "hifz-app/src/main/assets/hifzreader/index.html",
    '<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=4,user-scalable=yes">',
    '<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">',
)

# F5 — rosettes stay above line masks even when there is no ayah selection polygon.
replace_once(
    "hifz-app/src/main/assets/hifzreader/reader.js",
    "function markerLayer(svg,polys){",
    "function markerLayer(svg,polys,lines){",
)
replace_once(
    "hifz-app/src/main/assets/hifzreader/reader.js",
    "    const c=pt.matrixTransform(full);\n    if(!insideSelection(polys,c.x,c.y))return;",
    "    const c=pt.matrixTransform(full);\n    const visible=polys.length\n      ? insideSelection(polys,c.x,c.y)\n      : (lines||[]).some(line=>c.y>=Number(line.top)&&c.y<=Number(line.bottom));\n    if(!visible)return;",
)
replace_once(
    "hifz-app/src/main/assets/hifzreader/reader.js",
    "  if(polys.length)layer.appendChild(markerLayer(svg,polys));",
    "  layer.appendChild(markerLayer(svg,polys,lines));",
)

# F7 — final UI clearly describes the destructive reset instead of calling it a test state.
replace_once(
    "hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java",
    '        root.addView(Ui.settingRow(this,"Réinitialiser","État de test",v->confirmReset()));',
    '        root.addView(Ui.settingRow(this,"Réinitialiser","Progression Hifz",v->confirmReset()));',
)
replace_once(
    "hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java",
    '        new AlertDialog.Builder(this).setTitle("Réinitialiser l’état de test ?")\n            .setMessage("Remet le scénario de référence. Le Mushaf, les Tafsir et l’audio installé ne sont pas modifiés.")',
    '        new AlertDialog.Builder(this).setTitle("Réinitialiser la progression Hifz ?")\n            .setMessage("Efface la progression Hifz locale (Sabqi, Itqān, Murājaʿah, curseurs et chronos). Le Mushaf, les Tafsir et l’audio installé ne sont pas modifiés.")',
)

# Keep Gradle convergence gate aligned with the single-source schedule contract.
replace_once(
    "hifz-app/build.gradle.kts",
    '        val config = file("src/main/java/com/quransafeguard/hifz/preview/PreviewConfig.java").readText()\n        val session = file("src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java").readText()',
    '        val config = file("src/main/java/com/quransafeguard/hifz/preview/PreviewConfig.java").readText()\n        val core = rootProject.file("hifz-core/src/main/kotlin/com/quransafeguard/hifz/core/HifzCore.kt").readText()\n        val session = file("src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java").readText()',
)
replace_once(
    "hifz-app/build.gradle.kts",
    '        check(config.contains("MURAJAAH_RECENT_SABQI_MINUTES_WORKING = 30"))\n        check(config.contains("MURAJAAH_ITQAN_MINUTES_WORKING = 30"))\n        check(config.contains("MURAJAAH_MINUTES_WORKING = 60"))',
    '        check(core.contains("PlannedSession(SessionKind.SABQI_TODAY_REVIEW, 30)")\n                && core.contains("PlannedSession(SessionKind.OLD_ITQAN_MURAJAAH, 60)")\n                && core.contains("PlannedSession(SessionKind.RECENT_SABQI_REVIEW, 30)")\n                && core.contains("PlannedSession(SessionKind.OLD_ITQAN_MURAJAAH, 30)")) {\n            "Fixed timed sessions must be defined by HifzSchedule, not duplicate PreviewConfig constants."\n        }',
)

# Existing release source contract now explicitly guards the audit regressions as part of normal app tests.
replace_once(
    "hifz-app/src/test/java/com/quransafeguard/hifz/preview/OfficialReleaseContractTest.java",
    '        assertTrue("reader must reserve vertical safety room", index.contains("calc((100vh - 4px) * 345 / 550)"));',
    '        assertTrue("reader must reserve vertical safety room", index.contains("calc((100vh - 4px) * 345 / 550)"));\n        assertTrue("user zoom must not override canonical BOOX fit", index.contains("maximum-scale=1") && index.contains("user-scalable=no"));\n        String mushaf = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MushafView.java");\n        assertTrue("WebView built-in zoom must stay disabled", mushaf.contains("setBuiltInZoomControls(false)"));\n        assertFalse("mask entropy must not be regenerated per WebView instance", mushaf.contains("UUID.randomUUID"));',
)
replace_once(
    "hifz-app/src/test/java/com/quransafeguard/hifz/preview/OfficialReleaseContractTest.java",
    '        assertFalse("weekend recent review must not stop after one pass", session.contains("recentMurajaahComplete"));',
    '        assertFalse("weekend recent review must not stop after one pass", session.contains("recentMurajaahComplete"));\n        assertTrue("runtime timed durations must consume HifzSchedule", session.contains("HifzSchedule") && session.contains("planFor"));\n        assertTrue("mask entropy must persist for the logical Hifz session", session.contains("prefs.maskEntropyFor(mode)") && session.contains("prefs.clearMaskEntropy(mode)"));',
)

# Final local sanity scan: no old duplicate fixed-duration constants may remain in production config/session.
config = (ROOT / "hifz-app/src/main/java/com/quransafeguard/hifz/preview/PreviewConfig.java").read_text(encoding="utf-8")
session = (ROOT / "hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java").read_text(encoding="utf-8")
for forbidden in (
    "MURAJAAH_MINUTES_WORKING",
    "MURAJAAH_RECENT_SABQI_MINUTES_WORKING",
    "MURAJAAH_ITQAN_MINUTES_WORKING",
    "WEEKDAY_MURAJAAH_MINUTES",
    "WEEKEND_MURAJAAH_MINUTES",
    "WEEKEND_RECENT_REVIEW_MINUTES",
    "SABQI_TODAY_REVIEW_MINUTES",
):
    assert forbidden not in config, f"duplicate remains in PreviewConfig: {forbidden}"
    assert forbidden not in session, f"duplicate remains in HifzSessionActivity: {forbidden}"

print("CLAUDE_AUDIT_FIX_PATCH_OK")
