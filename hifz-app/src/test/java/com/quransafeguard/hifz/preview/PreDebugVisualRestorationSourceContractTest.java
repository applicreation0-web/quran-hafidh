package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Auditable UI-only regression guards; these are not BOOX hardware proofs.
 * Keep all Hifz engines and Quranic geometry outside the scope of this test. */
public final class PreDebugVisualRestorationSourceContractTest {
    private static String read(String path) throws Exception {
        Path f=Paths.get(path);
        if(!Files.exists(f))f=Paths.get("..",path);
        return new String(Files.readAllBytes(f),StandardCharsets.UTF_8);
    }

    @Test public void lectureHighlightsExactQcfWordBoxesAndNeverUsesUnderline() throws Exception {
        String js=read("hifz-app/src/main/assets/hifzreader/reader.js");
        int first=js.indexOf("function semanticCueRect(rect,cue){");
        int end=js.indexOf("function semanticCueLayer(svg){",first);
        assertTrue(first>0 && end>first);
        String cue=js.substring(first,end);
        assertTrue("The exact source word coordinates must be used",cue.contains("rect.x")
            && cue.contains("rect.y") && cue.contains("rect.width") && cue.contains("rect.height"));
        assertTrue("Restore translucent Al-Munir style highlight BEHIND the amorce words",
            cue.contains("setAttribute('fill','var(--sel)')")
            && cue.contains("setAttribute('fill-opacity',eink?'0.16':'0.10')"));
        assertTrue(cue.contains("N?.semanticCueTap?.(String(cue.id))"));
        assertFalse("User rejected underlining as a substitute for the highlight",
            cue.contains("createElementNS(NS,'line')"));
        assertFalse("No huge border around the calligraphy",
            cue.contains("setAttribute('stroke-width'"));
    }

    @Test public void contractPictogramsStayVisibleAndParcoursUsesBrain() throws Exception {
        String ui=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/Ui.java");
        String home=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        assertTrue(ui.contains("if (s.equals(\"parcours hifz\")) return R.drawable.ic_ui_revision;"));
        assertTrue(ui.contains("new int[]{MUTED,PAPER,INK,INK}"));
        assertTrue("Selected icon must be dark on light background",ui.contains(
            "shape(SURFACE, LINE, dp(context, 6), Math.max(1, dp(context, 1)))"));
        assertTrue("Home chevron cannot be made 8dp tall with excess vertical padding",
            home.contains("arrow.setPadding(Ui.dp(this, 3), Ui.dp(this, 11), Ui.dp(this, 3), Ui.dp(this, 11))"));
        assertFalse(home.contains("arrow.setPadding(Ui.dp(this, 4), Ui.dp(this, 20)"));
    }

    @Test public void weeklyDialogIsCompactAndManualIsTruthfullyNamed() throws Exception {
        String main=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        String settings=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        assertTrue(main.contains("Standard · 3 Sabqi / 4 Itqān"));
        assertTrue(main.contains("Manuel · nombre de jours"));
        assertFalse(main.contains("Manuel : répartition choisie"));
        assertTrue(settings.contains("jours Stabilisation (dimanche imposé)"));
        assertTrue(settings.contains("3 jours Apprentissage · 4 jours Stabilisation"));
        assertTrue("Historical cadence changes remain effective-dated",
            read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java")
                .contains("weeklyProgramTransitionsV1"));
    }

    @Test public void unverifiedIbnKathirHeadingsAreNotDisplayedAsTechnicalLabels() throws Exception {
        String reader=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java");
        String titles=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/IbnKathirOfflineTitles.java");
        assertTrue("First launch highlights the available exact-word amorces",
            reader.contains(".getBoolean(\"ibn_kathir_start_markers_visible\",true)"));
        assertTrue("The OFF toggle is still explicitly persisted",
            reader.contains(".putBoolean(\"ibn_kathir_start_markers_visible\",semanticCuesEnabled)"));
        assertTrue(reader.contains("if(titles.length==0)return;"));
        assertFalse(reader.contains("English heading not yet verified"));
        assertTrue(titles.contains("if(group.surah==50)return verifiedQafHeadings(group);"));
        assertTrue(titles.contains("if(group.surah==53)return verifiedNajmHeadings(group);"));
    }
}
