package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Regression contract for the official post-audit Quran Hifz 0.7.x release. */
public final class OfficialReleaseContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void readerBootMaskAndRevealUseOfficialContract() throws Exception {
        String reader = read("hifz-app/src/main/assets/hifzreader/reader.js");
        String index = read("hifz-app/src/main/assets/hifzreader/index.html");

        assertTrue("prepare must complete before native ready", reader.indexOf("prepare();") < reader.indexOf("N?.ready();"));
        assertTrue("runtime E-Ink setter required", reader.contains("setEink(value)"));
        assertTrue("line ids must be normalized", reader.contains("new Set") && reader.contains("String("));
        assertTrue("mask must randomize existing source-ink cells", reader.contains("randomOrderKeys") && reader.contains("randomSegmentsForCells"));
        assertTrue("random mask draw must survive WebView page reloads", reader.contains("seededRandom") && reader.contains("maskEntropy"));
        assertTrue("mask percentages must accumulate by source-ink width", reader.contains("totalWidth*fraction") && reader.contains("Math.min(cellWidth,remaining)"));
        assertTrue("verse-number rosettes must be redrawn above masks", reader.contains("markerLayer(svg,polys,lines)") && reader.contains("layer.appendChild(markerLayer(svg,polys,lines))"));
        assertFalse("final mask must not depend on linguistic word geometry", reader.contains("line.words") || reader.contains("maskedWordIds"));
        assertFalse("mask must never collapse a line into one min/max solid band", reader.contains("function hiddenBandForLine"));
        assertFalse("non-scrollable reader must not call window.scrollBy", reader.contains("window.scrollBy"));
        assertTrue("reveal must use an explicit Mushaf translation", reader.contains("--reveal-shift") || reader.contains("translateY"));
        assertFalse("opening Tafsir must not pre-shift the whole centered page using reveal padding", reader.contains("padding-bottom:var(--reveal-pad)"));
        assertTrue("CSP must explicitly allow the local boot nonce", index.contains("'nonce-hifz-local'"));
        assertTrue("reader must force light color scheme", index.contains("color-scheme:light") || index.contains("color-scheme: light"));
        assertFalse("BOOX reader must not enlarge the Mushaf beyond the reading zone", index.contains("--mushaf-scale") || index.contains("scale(var(--mushaf-scale))"));
        assertTrue("reader must reserve vertical safety room", index.contains("calc((100vh - 4px) * 345 / 550)"));
        assertTrue("user zoom must not override canonical BOOX fit", index.contains("maximum-scale=1") && index.contains("user-scalable=no"));
        String mushaf = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MushafView.java");
        assertTrue("WebView built-in zoom must stay disabled", mushaf.contains("setBuiltInZoomControls(false)"));
        assertFalse("mask entropy must not be regenerated per WebView instance", mushaf.contains("UUID.randomUUID"));
    }

    @Test public void schemaV6KeepsAnchoringWorkSeparatedFromAcquiredMaintenance() throws Exception {
        String config = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/PreviewConfig.java");
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");

        assertTrue("0.7.5 persistent state requires schema v6", config.contains("SCHEMA_VERSION = 6"));
        assertTrue("v2 state needs an explicit non-destructive migration", prefs.contains("migrateV2ToV3"));
        assertTrue("v3 state needs an explicit atomic migration", prefs.contains("migrateV3ToV4"));
        assertTrue("v4 state needs the C23-safe atomic migration", prefs.contains("migrateV4ToV5"));
        assertTrue("unconsolidated promoted material must be durable", prefs.contains("unconsolidatedPromotedRanges"));
        assertTrue("historical promoted material must retain migration-time Murajaah visibility", prefs.contains("legacyMurajaahPromotedRanges"));
        assertTrue("Itqan work corpus must include all promoted material", prefs.contains("itqanWorkCorpus()"));
        assertTrue("Murajaah corpus must have its own consolidated view", prefs.contains("murajaahCorpus()"));
        assertTrue("x40 validation must be able to consolidate promoted material", prefs.contains("completeStabilizationBlockV6"));
        assertFalse("cycle philosophy forbids a priority promotion queue", prefs.contains("pendingPromotedItqan"));
    }

    /**
     * The debug->release signature switch means every learner must uninstall and reinstall at
     * least once, so Export/Import (HifzBackup) is the only path that carries progress across
     * that boundary. It must stay a generic whole-store dump (so a brand-new key never needs an
     * export-code update to be included), and any key introduced after schema v6 froze must read
     * with a safe default rather than the strict required()/requiredV6String() helpers, so
     * restoring an older export never crashes on a key that export predates.
     */
    @Test public void backupStaysGenericAndPostSchemaKeysReadWithSafeDefaults() throws Exception {
        String backup = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzBackup.java");
        assertTrue("export must iterate the whole store, not a fixed key list", backup.contains("prefs.getAll()"));
        assertTrue("import must restore into the exact same preference file", backup.contains("quran_hifz_preview_v1"));
        assertTrue("import must be a single atomic commit", backup.contains("editor.commit()"));
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        assertTrue("weekly snowball evening gate must default to never-done, not require the key",
            prefs.contains("public String lastLearningSnowballEveningDate() { return p.getString(\"lastLearningSnowballEveningDate\", \"\"); }"));
        assertTrue("weekly snowball evening gate must default to never-done, not require the key",
            prefs.contains("public String lastStabilizationSnowballEveningDate() { return p.getString(\"lastStabilizationSnowballEveningDate\", \"\"); }"));
        String repair = method(prefs, "boolean repairV5 = p.contains(\"stableRecentLines\")", "if (repairV5) {");
        assertTrue("v5-era exports missing later-added keys must be backfilled on every launch, not just once",
            repair.contains("!p.contains(\"consolidationAttendanceDates\")"));
    }

    private static String method(String source, String start, String end) {
        int a = source.indexOf(start);
        int b = source.indexOf(end, a + start.length());
        if (a < 0 || b < 0 || b <= a) throw new IllegalStateException("Method boundary missing: " + start);
        return source.substring(a, b);
    }

    @Test public void runtimeUsesIndependentFixedSessionsWithoutHiddenTransfer() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String config = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/PreviewConfig.java");
        String core = read("hifz-core/src/main/kotlin/com/quransafeguard/hifz/core/HifzCore.kt");
        assertTrue("same-day learning review must remain a real internal phase", session.contains("SABQI_TODAY_REVIEW"));
        assertTrue("Consolidation runtime mode must remain explicit", session.contains("RECENT_SABQI_REVIEW"));
        assertTrue("Revision must use the acquired corpus only", session.contains("murajaahCorpus()"));
        assertFalse("mixed A/B Murajaah phase must be removed", session.contains("murajaahBlockB") || session.contains("transitionToBlockB"));
        assertFalse("unused recent time may never be transferred", session.contains("unusedA") || session.contains("availableB"));
        assertTrue("runtime timed durations must consume HifzSchedule fixed-mode targets", session.contains("HifzSchedule") && session.contains("targetMinutesFor"));
        assertFalse("directly opened sessions must not depend on today's plan", session.contains("scheduledTargetMinutes") || session.contains("absent du planning"));
        assertFalse("session durations must not be duplicated in PreviewConfig", config.contains("SABQI_MINUTES_WORKING") || config.contains("ITQAN_MINUTES_WORKING"));
        assertFalse("contradictory evening helper must be removed", core.contains("hasEveningMurajaah"));
        assertTrue("mask entropy must persist for the logical Hifz session", session.contains("prefs.maskEntropyFor(mode)") && session.contains("prefs.clearMaskEntropy(mode)"));
    }

    @Test public void timedResumeAndCanonicalCarryoverHaveExplicitNonTransferPaths() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        String dashboard = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/WeeklyDashboardPlanner.java");
        String settings = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");

        assertTrue("expired timed work must be committed after process death", session.contains("completeExpiredTimedSession"));
        assertTrue("Today must resolve oldest soft carryover with the canonical cadence", main.contains("nextDueCadence") && main.contains("nextMode") && main.contains("HifzSchedule.INSTANCE.nextDue"));
        assertTrue("dashboard must share the canonical cadence", dashboard.contains("HifzSchedule.INSTANCE.actionFor"));
        assertFalse("dashboard must not retain A/B transfer projection", dashboard.contains("secondsA") || dashboard.contains("availableB") || dashboard.contains("Murājaʿah A") || dashboard.contains("Murājaʿah B"));
        assertFalse("settings must not offer daily cursor reposition buttons", settings.contains("Repositionner Murājaʿah"));
    }

    @Test public void tafsirUiAndPackagingAreCompactAndUnified() throws Exception {
        String study = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java");
        String multi = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MultiTafsirRepository.java");
        String gradle = read("hifz-app/build.gradle.kts");

        assertTrue("Tafsir identity must stay on one line", study.contains("title.setSingleLine(true)"));
        assertTrue("A-/A+ must use compact controls", study.contains("tafsirTextControl") || study.contains("tafsirCompactButton"));
        assertTrue("multiple available Tafsir sources must use one compact dropdown", study.contains("PopupMenu") && study.contains("showTafsirEditionMenu") && study.contains("available.size() <= 1"));
        assertFalse("legacy permanent Tafsir edition tabs must stay removed", study.contains("tafsirEditionButton") || study.contains("tafsirCompactTab"));
        assertFalse("Quran Hifz must not copy divergent reader109/audio.json", gradle.contains("reader109/audio.json"));
        assertFalse("Tafsir loader must not retain Base64 decoding", multi.contains("android.util.Base64") || multi.contains("Base64.decode"));
        assertFalse("Tafsir asset paths must not use .gz.b64.part", multi.contains(".gz.b64.part"));
        assertTrue("Tafsir rights metadata must be enforced", multi.contains("personal_use_only") && multi.contains("redistribution_approved") && multi.contains("rights_note"));
    }

    @Test public void officialVersionIsIncremented() throws Exception {
        String gradle = read("hifz-app/build.gradle.kts");
        assertTrue("0.7.13 must increment versionCode beyond published 0.7.12", gradle.contains("versionCode = 20"));
        assertTrue("official candidate must identify the 0.7.13 BOOX release", gradle.contains("versionName = \"0.7.13-boox\""));
    }
}
