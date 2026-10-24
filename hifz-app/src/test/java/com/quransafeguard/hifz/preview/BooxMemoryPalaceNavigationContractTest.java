package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** The approved memory-palace icons must be genuinely large on portrait BOOX. */
public final class BooxMemoryPalaceNavigationContractTest {
    private static String source(String relative) throws Exception {
        Path path = Paths.get(relative);
        if (!Files.exists(path)) path = Paths.get("..", relative);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    @Test public void sixPathsAppearAndRemainIndividuallySelectable() throws Exception {
        String main = source("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        assertTrue(main.contains("pathTile(learningModes, \"Apprentissage\""));
        assertTrue(main.contains("pathTile(otherModes, \"Stabilisation\""));
        assertTrue(main.contains("pathTile(reviewModes, \"Révision active\""));
        assertTrue(main.contains("pathTile(reviewModes, \"Révision passive\""));
        assertTrue(main.contains("pathTile(learningModes, \"Renforcement\""));
        assertTrue(main.contains("pathTile(otherModes, \"Consolidation\""));
        assertTrue(main.contains("HifzSessionActivity.MURAJAAH_ACTIVE"));
        assertTrue(main.contains("HifzSessionActivity.MURAJAAH"));
    }

    @Test public void vectorArtworkFillsBooxColumnsNotTinyIntrinsicBounds() throws Exception {
        String main = source("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        assertTrue(main.contains("booxIconDp(3, 112)"));
        assertTrue(main.contains("booxIconDp(2, 124)"));
        assertTrue(main.contains("booxIconDp(2, 82)"));
        assertTrue(main.contains("architectural.setBounds(0, 0, Ui.dp(this, sideDp), Ui.dp(this, sideDp))"));
        assertFalse(main.contains("new BooxFlowStroke(this)"));
    }

    @Test public void exactNineSemanticsAreSeparated() throws Exception {
        String ui = source("hifz-app/src/main/java/com/quransafeguard/hifz/preview/Ui.java");
        for (String name : new String[]{"home", "route", "compass", "door", "column",
                "buttress", "keystone", "torch", "mirror"}) {
            assertTrue("Missing palace resource: " + name, ui.contains("R.drawable.ic_palace_" + name));
        }
    }
}
