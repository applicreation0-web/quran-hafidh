package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Source-level C1-C23 closure guards; behavior/runtime remains covered by dedicated tests. */
public final class PreBoox074SourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void c1StructuredCreditsDoNotDependOnVisibleLabels() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        assertTrue(prefs.contains("lastItqanCreditStart") && prefs.contains("lastItqanCreditBlockIndex"));
        assertTrue(prefs.contains("lastMurajaahCreditStart") && prefs.contains("lastMurajaahCreditEnd"));
    }

    @Test public void c5ToC9AndEinkUiHardeningRemainPresent() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String mushaf = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MushafView.java");
        String eink = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/EinkController.java");
        assertTrue(session.contains("progress.setMaxLines(2)"));
        assertTrue(session.contains("progress.setEllipsize(TextUtils.TruncateAt.END)"));
        assertTrue(mushaf.contains("MotionEvent.ACTION_CANCEL"));
        assertTrue(mushaf.contains("timeoutAfterObservedRender"));
        assertTrue(mushaf.contains("MAX_PAGE_TIMEOUT_MS"));
        assertTrue(mushaf.contains("updateObservedRender"));
        assertTrue(eink.contains("if (!isEink(prefs)) { view.invalidate(); return; }"));
    }

    @Test public void c12SessionFlowCapturesDateOnceAndOnlyHifzClockCallsLocalDateNow() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue(session.contains("LocalDate capturedToday = HifzClock.today()"));
        assertTrue(session.indexOf("HifzClock.today()") == session.lastIndexOf("HifzClock.today()"));
        Path root = Paths.get("hifz-app/src/main/java/com/quransafeguard/hifz/preview");
        if (!Files.exists(root)) root = Paths.get("..", root.toString());
        for (Path path : (Iterable<Path>) Files.walk(root).filter(p -> p.toString().endsWith(".java"))::iterator) {
            String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            if (path.getFileName().toString().equals("HifzClock.java")) assertTrue(text.contains("LocalDate.now(clock)"));
            else assertFalse(path.toString(), text.contains("LocalDate.now("));
        }
    }

    @Test public void c14ToC21BooxControlsStayHardened() throws Exception {
        String ui = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/Ui.java");
        String study = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java");
        String free = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/FreeMemActivity.java");
        assertTrue(ui.contains("state_pressed") && ui.contains("shape(INK, INK"));
        assertTrue(ui.contains("new int[]{MUTED,PAPER,PAPER,INK}"));
        assertTrue(ui.contains("text(context, label, 11f"));
        assertTrue(study.contains("Ui.dp(this, 48)"));
        assertTrue(study.contains("eink.isEink(hifzPrefs)) return"));
        assertTrue(free.contains("setLayoutDirection(View.LAYOUT_DIRECTION_RTL)"));
        assertTrue(free.contains("if(count==0)return"));
    }

    @Test public void c19GeometryEntryPointsFailClosed() throws Exception {
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String free = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/FreeMemActivity.java");
        String settings = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        assertTrue(main.contains("setGeometryActionsEnabled(false)") && main.contains("setGeometryActionsEnabled(true)"));
        assertTrue(session.contains("Ui.showFatal") && free.contains("Ui.showFatal") && settings.contains("Ui.showFatal"));
        assertTrue(session.contains("if(clock==null)return;"));
        assertTrue(session.contains("if(clock==null){super.onPause();return;}"));
        assertTrue(session.contains("if(clock==null)return super.onKeyDown(code,e);"));
        assertTrue(free.contains("if(mushaf==null)return super.onKeyDown(code,e);"));
    }

    @Test public void c23AllProductionFractionationUsesStrictSurahSegments() throws Exception {
        String geometry = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/GeometryRepository.java");
        String stabilization = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/StabilizationHalfPagePolicy.java");
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        String weekly = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/WeeklyDashboardPlanner.java");
        assertTrue(geometry.contains("surahSegmentLineCounts(VerseRef start, VerseRef end)"));
        assertTrue(stabilization.contains("singleSurah(line)"));
        assertTrue(stabilization.contains("Physical Mushaf line crosses surah boundary"));
        assertTrue(session.contains("StabilizationHalfPagePolicy.planPage"));
        for (String text : new String[]{main, weekly}) {
            assertTrue(text.contains("CorpusLinePolicy.ownedLineIdsForRangeOnPage"));
            assertTrue(text.contains("StabilizationHalfPagePolicy.planPage"));
            assertFalse(text.contains("fractionatedBlockLength(unit.size()"));
            assertFalse(text.contains("fractionatedBlockStart(unit.size()"));
        }
        assertFalse(session.contains("fractionatedBlockLength(unit.size()"));
        assertFalse(session.contains("fractionatedBlockStart(unit.size()"));
    }

    @Test public void c22EveryLiteralUiActionLabelIsPinnedByTheMappingTest() throws Exception {
        String mapping = read("hifz-app/src/test/java/com/quransafeguard/hifz/preview/UiIconMappingTest.java");
        Path root = Paths.get("hifz-app/src/main/java/com/quransafeguard/hifz/preview");
        if (!Files.exists(root)) root = Paths.get("..", root.toString());
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "(?:iconButton|roundAction|cardAction|modeCard)\\([^,\n]+,\\s*\"[^\"]*\",\\s*\"([^\"]+)\"");
        for (Path path : (Iterable<Path>) Files.walk(root).filter(p -> p.toString().endsWith(".java"))::iterator) {
            String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            java.util.regex.Matcher matcher = pattern.matcher(text);
            while (matcher.find()) assertTrue(path + " label=" + matcher.group(1), mapping.contains("\"" + matcher.group(1) + "\""));
        }
    }

    @Test public void personalEmbeddedAudioWorkflowIsAbsent() {
        Path direct = Paths.get(".github/workflows/hifz-personal-embedded-audio.yml");
        Path parent = Paths.get("..", direct.toString());
        assertFalse(Files.exists(direct) || Files.exists(parent));
    }
}
