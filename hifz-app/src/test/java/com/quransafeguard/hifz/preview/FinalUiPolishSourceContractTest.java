package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Final BOOX UI contract: study stays audio-free and Hifz chrome stays explicit and e-ink safe. */
public final class FinalUiPolishSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void studyModeContainsNoAudioSurfaceOrPlayer() throws Exception {
        String study = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java");
        assertFalse(study.contains("audioButton"));
        assertFalse(study.contains("audioHost"));
        assertFalse(study.contains("openAudio()"));
        assertFalse(study.contains("HifzAudioDialog"));
    }

    @Test public void unavailableTafsirAuthorsAreAbsentRatherThanDisabled() throws Exception {
        String study = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java");
        assertFalse(study.contains("edition.displayName + \" · —\""));
        assertFalse(study.contains("Hors couverture"));
        assertTrue(study.contains("available.size() <= 1"));
    }

    @Test public void multipleAvailableTafsirsUseOneCompactDropdown() throws Exception {
        String study = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java");
        assertTrue(study.contains("PopupMenu"));
        assertTrue(study.contains("showTafsirEditionMenu"));
        assertFalse(study.contains("styleTafsirEditionButton"));
        assertFalse(study.contains("new GradientDrawable()"));
    }

    @Test public void studyTafsirUsesOneFlatLargeHitTargetSeparatedFromPageSlider() throws Exception {
        String study = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java");
        assertFalse("Study Tafsir must not use the vertical icon-over-caption action", study.contains("Ui.roundAction(this, \"\", \"Tafsir\""));
        assertTrue("Study Tafsir must use a dedicated flat action", study.contains("tafsirReaderAction()"));
        assertTrue("Tafsir needs a BOOX-friendly invisible hit height", study.contains("button.setMinimumHeight(Ui.dp(this, 56))"));
        assertTrue("Tafsir needs a wider BOOX-friendly invisible hit width", study.contains("button.setMinimumWidth(Ui.dp(this, 160))"));
        assertTrue("page slider must be clearly separated from Tafsir action", study.contains("railParams.topMargin = Ui.dp(this, 20)"));
        assertTrue("Tafsir action row itself must preserve the full touch height", study.contains("readerActions.setMinimumHeight(Ui.dp(this, 56))"));
    }

    @Test public void audioRemainsAvailableInHifzAndFreeMemOnly() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String free = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/FreeMemActivity.java");
        assertTrue(session.contains("HifzAudioDialog"));
        assertTrue(session.contains("Écouter"));
        assertTrue(free.contains("HifzAudioDialog"));
        assertTrue(free.contains("Audio"));
    }

    @Test public void userFacingHifzVocabularyUsesPlainFrenchNames() throws Exception {
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String settings = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        assertTrue(main.contains("\"Leçon neuve\""));
        assertTrue(main.contains("\"Ancrage\""));
        assertTrue(main.contains("\"Entretien\""));
        assertTrue(session.contains("return \"Leçon neuve\""));
        assertTrue(session.contains("return \"Reprise du soir\""));
        assertTrue(session.contains("return \"Consolidation\""));
        assertTrue(session.contains("return \"Ancrage\""));
        assertTrue(session.contains("return \"Entretien\""));
        assertTrue(settings.contains("section(root,\"Repères\")"));
        assertTrue(settings.contains("Une page entière travaillée en profondeur"));
    }

    @Test public void semanticHifzIconsStayMonochromeOutlineAndTwentyFourDp() throws Exception {
        String[] files = {
            "ic_hifz_new_lesson.xml", "ic_hifz_evening_review.xml", "ic_hifz_consolidation.xml",
            "ic_hifz_anchoring.xml", "ic_hifz_maintenance.xml", "ic_hifz_strengthen.xml",
            "ic_hifz_pending.xml", "ic_hifz_acquired.xml"
        };
        for (String file : files) {
            String xml = read("hifz-app/src/main/res/drawable/" + file);
            assertTrue(file, xml.contains("android:width=\"24dp\""));
            assertTrue(file, xml.contains("android:height=\"24dp\""));
            assertTrue(file, xml.contains("android:fillColor=\"@android:color/transparent\""));
            assertTrue(file, xml.contains("android:strokeWidth=\"2\""));
            assertFalse(file, xml.contains("#808080"));
            assertFalse(file, xml.contains("#888888"));
        }
    }
}
