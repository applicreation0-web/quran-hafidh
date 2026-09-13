package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Physical-BOOX regressions reported on the 0.7.3 candidate. */
public final class BooxFinalUiContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void preferredEditionSurvivesOnlyWhileActuallyAvailable() {
        Set<MultiTafsirRepository.Edition> qurtubiAndQushayri = EnumSet.of(
            MultiTafsirRepository.Edition.QURTUBI,
            MultiTafsirRepository.Edition.QUSHAYRI);
        assertEquals(MultiTafsirRepository.Edition.QUSHAYRI,
            TafsirEditionPolicy.resolve(MultiTafsirRepository.Edition.QUSHAYRI, qurtubiAndQushayri));
        assertEquals("fallback must be the first available edition, never a missing edition",
            MultiTafsirRepository.Edition.QURTUBI,
            TafsirEditionPolicy.resolve(MultiTafsirRepository.Edition.JALALAYN, qurtubiAndQushayri));
        assertNull(TafsirEditionPolicy.resolve(MultiTafsirRepository.Edition.JALALAYN,
            EnumSet.noneOf(MultiTafsirRepository.Edition.class)));
    }

    @Test public void selectorExistsOnlyWhenThereIsARealChoice() {
        assertFalse(TafsirEditionPolicy.hasMultiple(
            EnumSet.of(MultiTafsirRepository.Edition.JALALAYN)));
        assertTrue(TafsirEditionPolicy.hasMultiple(
            EnumSet.of(MultiTafsirRepository.Edition.JALALAYN, MultiTafsirRepository.Edition.QURTUBI)));
    }

    @Test public void readerActionsAndTafsirSelectorStayVisuallyLight() throws Exception {
        String study = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java");
        assertFalse("Tafsir must not use the bordered smallButton grammar",
            study.contains("Ui.smallButton(this, \"Tafsir\""));
        assertFalse("Audio must not use the bordered smallButton grammar",
            study.contains("Ui.smallButton(this, \"♪ Audio\""));
        assertTrue("reader actions need a borderless hit-target grammar",
            study.contains("Ui.readerActionButton"));
        assertTrue("multiple available Tafsir editions must use a compact dropdown",
            study.contains("PopupMenu") && study.contains("showEditionMenu"));
        assertTrue("dropdown must be conditional on a real choice",
            study.contains("TafsirEditionPolicy.hasMultiple"));
        assertFalse("unavailable authors must never be rendered as disabled tabs",
            study.contains("edition.displayName + \" · —\"") || study.contains("setEnabled(covered)"));
        assertFalse("unavailable authors must not leak into metadata",
            study.contains("Hors couverture"));
    }
}
