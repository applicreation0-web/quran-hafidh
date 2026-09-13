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

    @Test public void schemaV4SeparatesAnchoringWorkFromAcquiredMaintenance() throws Exception {
        String config = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/PreviewConfig.java");
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");

        assertTrue("persistent anchoring state requires schema v4", config.contains("SCHEMA_VERSION = 4"));
        assertTrue("v2 state needs an explicit non-destructive migration", prefs.contains("migrateV2ToV3"));
        assertTrue("v3 state needs an explicit atomic migration", prefs.contains("migrateV3ToV4"));
        assertTrue("unconsolidated promoted material must be durable", prefs.contains("unconsolidatedPromotedRanges"));
        assertTrue("historical promoted material must retain migration-time Murajaah visibility", prefs.contains("legacyMurajaahPromotedRanges"));
        assertTrue("Itqan work corpus must include all promoted material", prefs.contains("itqanWorkCorpus()"));
        assertTrue("Murajaah corpus must have its own consolidated view", prefs.contains("murajaahCorpus()"));
        assertTrue("x40 validation must be able to consolidate promoted material", prefs.contains("markPromotedConsolidated"));
        assertFalse("cycle philosophy forbids a priority promotion queue", prefs.contains("pendingPromotedItqan"));
    }

    @Test public void runtimeUsesIndependentFixedSessionsWithoutHiddenTransfer() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue("same-day Sabqi review must be a real runtime mode", session.contains("SABQI_TODAY_REVIEW"));
        assertTrue("weekend recent Sabqi review must be a real runtime mode", session.contains("RECENT_SABQI_REVIEW"));
        assertTrue("old Itqan Murajaah must use the consolidated corpus only", session.contains("murajaahCorpus()"));
        assertTrue("Itqan must advance through the anchored cycle", session.contains("nextAnchored"));
        assertTrue("Itqan completion must consolidate naturally encountered promoted material", session.contains("completeItqanUnitAndConsolidate"));
        assertFalse("mixed A/B Murajaah phase must be removed", session.contains("murajaahBlockB") || session.contains("transitionToBlockB"));
        assertFalse("unused recent time may never be transferred", session.contains("unusedA") || session.contains("availableB"));
        assertFalse("weekend recent review must not stop after one pass", session.contains("recentMurajaahComplete"));
        assertTrue("runtime timed durations must consume HifzSchedule", session.contains("HifzSchedule") && session.contains("planFor"));
        assertTrue("mask entropy must persist for the logical Hifz session", session.contains("prefs.maskEntropyFor(mode)") && session.contains("prefs.clearMaskEntropy(mode)"));
    }

    @Test public void timedResumeAndEmptyWeekendHaveExplicitNonTransferPaths() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        String dashboard = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/WeeklyDashboardPlanner.java");
        String settings = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");

        assertTrue("expired timed work must be committed after process death", session.contains("completeExpiredTimedSession"));
        assertTrue("empty recent Sabqi must complete truthfully without opening old Itqan", session.contains("completeEmptyRecentSabqiSession"));
        assertTrue("Today must open the first incomplete morning/evening plan entry", main.contains("firstIncompleteMode") && main.contains("planFor"));
        assertTrue("dashboard must share the domain schedule", dashboard.contains("planFor"));
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
        assertTrue("0.7.3 must increment versionCode beyond installed 0.7.2", gradle.contains("versionCode = 10"));
        assertTrue("official candidate must identify the 0.7.3 BOOX release", gradle.contains("versionName = \"0.7.3-boox\""));
    }
}
