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

    /** Spec UI pass 2: Lecture footer is [Tafsir] [Amorces] [Annoter], icons only, slim bars. */
    @Test public void studyTafsirIsAnIconActionInASlimAlwaysClickableBar() throws Exception {
        String study = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java");
        assertFalse("no caption-under-icon action", study.contains("Ui.roundAction(this, \"\", \"Tafsir\""));
        assertTrue("Tafsir uses the shared icon grammar", study.contains("Ui.iconButton(this, \"\", \"Tafsir\", v -> openTafsir())"));
        assertTrue("Tafsir must explain the missing selection instead of being disabled", study.contains("Touchez d’abord un verset pour ouvrir le Tafsir."));
        assertFalse("Tafsir action must never be disabled in Lecture", study.contains("tafsirButton.setEnabled(false)"));
        assertTrue("action bar keeps the 48dp touch height only", study.contains("readerActions.setMinimumHeight(Ui.dp(this, 48))"));
        assertTrue("no blank band between actions and the surah/hizb rail", study.contains("railParams.topMargin = 0;"));
    }

    @Test public void audioRemainsAvailableInHifzAndFreeMemOnly() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String free = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/FreeMemActivity.java");
        assertTrue(session.contains("HifzAudioDialog"));
        assertTrue(session.contains("Écouter"));
        assertTrue(free.contains("HifzAudioDialog"));
        assertTrue(free.contains("Audio"));
    }

    @Test public void userFacingHifzVocabularyUsesCanonicalSchema6Names() throws Exception {
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String settings = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        for (String action : new String[]{"Apprentissage", "Stabilisation", "Révision"}) assertTrue(main.contains("\"" + action + "\""));
        assertTrue(session.contains("return \"Apprentissage\""));
        assertTrue(session.contains("return \"Stabilisation\""));
        assertTrue(session.contains("return \"Consolidation\""));
        assertTrue(session.contains("return \"Révision\""));
        assertTrue(settings.contains("section(root,\"Schéma\")"));
        assertTrue(settings.contains("Apprentissage → Appris → Stabilisation → Stabilisé → Consolidation → Acquis → Révision"));
        assertTrue(settings.contains("consolidationSchemaNote.setText(\"Consolidation · soir \"+stabilizationDays);"));
        assertFalse(settings.contains("section(root,\"Repères\")"));
        assertFalse(settings.contains("addRepere(root"));
    }

    @Test public void settingsExposeReadableSeparatedSpeeds() throws Exception {
        String settings = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        String speed = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSpeedStore.java");
        assertTrue(settings.contains("section(root,\"Vitesses\")"));
        assertTrue(settings.contains("speedStore.maintenanceSummary()"));
        assertTrue(settings.contains("speedStore.consolidationSummary()"));
        assertTrue(speed.contains("s/ligne"));
        assertFalse(speed.contains("+ \"L/\""));
    }

    @Test public void stabilizationHalfPagePolicyIsTheRuntimeProjectionContract() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String policy = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/StabilizationHalfPagePolicy.java");
        assertTrue(policy.contains("planPage"));
        assertTrue(policy.contains("count <= 11"));
        assertTrue(session.contains("StabilizationHalfPagePolicy.planPage"));
        assertFalse(session.contains("Ancrage fractionné"));
    }

@Test public void semanticHifzIconsStayMonochromeOutlineAndTwentyFourDp() throws Exception {
        String[] files = {
            "ic_hifz_new_lesson.xml", "ic_hifz_reprise.xml", "ic_hifz_consolidation.xml",
            "ic_hifz_anchor.xml", "ic_hifz_maintenance.xml", "ic_hifz_strengthen.xml",
            "ic_hifz_waiting.xml", "ic_hifz_acquired.xml"
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

    @Test public void maintenanceAndConsolidationUseSeparatedSpeedCalibration() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String store = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSpeedStore.java");
        assertTrue(session.contains("calibrateMaintenance"));
        assertTrue(session.contains("calibrateConsolidation"));
        assertTrue(session.contains("instrumentationLabel"));
        assertFalse(session.contains("calibrateOldSpeed"));
        assertTrue(store.contains("murajaahSecPerLine"));
        assertTrue(store.contains("recentSecPerLine"));
    }

    @Test public void legacyCoreActionsStillExist() throws Exception {
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        String free = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/FreeMemActivity.java");
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue(main.contains("\"Lecture\""));
        assertTrue(main.contains("\"Mémorisation libre\""));
        assertTrue(main.contains("\"Paramètres\""));
        assertTrue(free.contains("Retirer une répétition"));
        assertTrue(free.contains("Ajouter une répétition"));
        assertTrue(free.contains("Remettre à zéro"));
        assertTrue(free.contains("Page suivante"));
        assertTrue(free.contains("Page précédente"));
        assertTrue(session.contains("\"Répétition\""));
        assertTrue(session.contains("\"Révéler\""));
        assertTrue(session.contains("\"Valider\""));
        assertTrue(session.contains("\"Écouter\""));
    }

    @Test public void runtimeExceptionsAreLoggedButNotShownRawToLearner() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue(session.contains("android.util.Log.e"));
        assertTrue(session.contains("La séance ne peut pas être affichée"));
        assertFalse(session.contains("progress.setText(detail)"));
    }
}
