package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** User decision: verse-number rosettes are never erased by the paper mask, in any mode. */
public final class VerseRosettesNeverErasedSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        Path file = Files.exists(direct) ? direct : Paths.get("..", repoPath);
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    @Test public void maskLayerAlwaysRedrawsRosettesAndNoModeCanTurnThatOff() throws Exception {
        String reader = read("hifz-app/src/main/assets/hifzreader/reader.js");
        assertTrue(reader.contains("      layer.appendChild(markerLayer(svg,polys,lines));"));
        assertFalse(reader.contains("preserveVerseMarkersOnMask"));
        for (String path : new String[]{"MushafView.java", "QuizActivity.java", "HifzSessionActivity.java", "StudyReaderActivity.java"}) {
            assertFalse(path, read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/" + path).contains("PreserveVerseMarkers"));
        }
    }
}
