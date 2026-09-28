package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * Reported directly: the POSITION_TO_TEXT/TRANSITION mask "ne donne pas exactement le début de
 * la zone masquée" — because the quiz masked the whole physical target line (an empty selection,
 * strictLineFocus alone) instead of the real target verse's own SVG polygon. Sabqi/Itqān sessions
 * already solve exactly this — a working block can start or end mid-line — by passing the real
 * selection so reader.js's maskCandidates only offers cells whose center falls inside the
 * selected verse's own polygon (insideSelection), and clips any remaining shared-line rectangle
 * to that polygon. The quiz simply never passed its own target verse into that path.
 */
public final class SpatialQuizMaskPrecisionSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void positionQuestionsPassTheRealVerseAsSelectionNotAnEmptyList() throws Exception {
        String activity = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SpatialQuizActivity.java");
        assertTrue("the mask must follow the real target verse's own polygon, not just its line",
            activity.contains("mushaf.show(q.prompt.page, Collections.singletonList(q.prompt.verse),\n"
                + "                    Collections.singletonList(q.prompt.targetLineId), 100, true);"));
    }

    @Test public void readerJsActuallyRestrictsMaskCellsToTheSelectedVersesOwnPolygon() throws Exception {
        String reader = read("hifz-app/src/main/assets/hifzreader/reader.js");
        assertTrue("a cell whose center falls outside the selected verse's own SVG shape must "
                + "never be offered as a mask candidate",
            reader.contains("if(polys.length&&!insideSelection(polys,(x0+x1)/2,(top+bottom)/2))return;"));
        assertTrue("maskFollowsSelection must default to true so an unset boot flag still restricts "
                + "candidates to the selection",
            reader.contains("let maskFollowsSelection=boot.maskFollowsSelection!==false;"));
    }

    @Test public void spatialQuizNeverOverridesMaskFollowsSelectionAwayFromItsTrueDefault() throws Exception {
        String activity = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SpatialQuizActivity.java");
        assertTrue("MurajaahOnly disables this; the quiz must keep the default (true) so its "
                + "selection actually narrows the mask",
            !activity.contains("setMaskFollowsSelection"));
    }
}
