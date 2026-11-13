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
        assertTrue(main.contains("showRevisionSelector(false)"));
        assertTrue(main.contains("HifzSessionActivity.MURAJAAH"));
    }

    @Test public void vectorArtworkFillsBooxColumnsNotTinyIntrinsicBounds() throws Exception {
        String main = source("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        assertTrue(main.contains("booxIconDp(2, 112)"));
        assertTrue(main.contains("booxIconDp(2, 124)"));
        assertTrue(main.contains("primaryTile(practiceRow, \"Progression\""));
        assertTrue(main.contains("architectural.setBounds(0, 0, Ui.dp(this, sideDp), Ui.dp(this, sideDp))"));
        assertFalse(main.contains("new BooxFlowStroke(this)"));
    }

    @Test public void booxHasNoCardPavementAndMaintienChangesOnlyAfterNas() throws Exception {
        String main = source("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        assertFalse("BOOX six icons must not become bordered cards", main.contains("tileBg.setStroke("));
        assertFalse(main.contains("tile.setBackground(tileBg)"));
        assertTrue(main.contains("tile.setBackgroundColor(Ui.PAPER)"));
        assertTrue("First draw must reflect a post-Nas installation",
            main.contains("refreshItqanCardVocabulary(); // Correct title on first draw"));
        assertTrue(main.contains("setText(afterNas ? \"Maintien\" : \"Stabiliser\")"));
        assertTrue(main.contains("prochain.setVisibility(afterNas ? View.GONE : View.VISIBLE)"));
        assertTrue(main.contains("?\"Maintien · \"+JuzMaintenanceStore.CUE"));
    }

    @Test public void weeklyBooxCirclesAreEmptyAndStateUsesFillAndOutline() throws Exception {
        String main = source("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        int start = main.indexOf("private void renderSelectedWeekDay()");
        int end = main.indexOf("private void ", start + 1);
        String calendar = main.substring(start, end > start ? end : main.length());
        assertTrue("No letters, arrows or ticks in calendar circles",
            calendar.contains("TextView node = Ui.text(this, \"\", 15f, true);"));
        assertFalse("No rotating Itqān arrows in circles", calendar.contains("↻"));
        assertFalse("No finale F inside circles", calendar.contains("done?\"✓\""));
        assertTrue("Only real receipts fill the validation circle",
            calendar.contains("boolean done = !future && \"Validé\".equals(row.state);")
            && calendar.contains("circle.setColor(done ? Ui.INK : Ui.PAPER);"));
        assertTrue("Selected day remains outlined, independently of completion",
            calendar.contains("if (active)") && calendar.contains("selection.setStroke(Ui.dp(this,2),Ui.INK);"));
        assertTrue("Weekday and date remain separate from the circle",
            calendar.contains("station.addView(day);") && calendar.contains("station.addView(date);"));
    }

    @Test public void maintiensTitleIsAdjacentAndUsesSameTypefaceBeforeNas() throws Exception {
        String main = source("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        assertTrue("Future Maintien is a second title line, in the same font as Stabiliser",
            main.contains("Ui.bookText(this, \"(Maintien)\", 19f, true)"));
        assertTrue("Subtitle is inserted immediately after heading, before weighted pictogram",
            main.contains("itqanCard.addView(maintienCue, 1);"));
        assertFalse("Do not strand future Maintien below the pictogram",
            main.contains("itqanCard.addView(maintienCue);"));
        assertTrue("After An-Nas, title becomes Maintien; parenthesized second title is hidden",
            main.contains("setText(afterNas ? \"Maintien\" : \"Stabiliser\")")
            && main.contains("prochain.setVisibility(afterNas ? View.GONE : View.VISIBLE)"));
        assertTrue("Restored programme cue must not overwrite visible Maintien subtitle",
            main.contains("itqanCard.getChildAt(3) instanceof TextView"));
        assertFalse("Wrong child-1 lookup must never replace the subtitle onResume",
            main.contains("Ui.settingValue(itqanCard)"));
    }

    @Test public void exactNineSemanticsAreSeparated() throws Exception {
        String ui = source("hifz-app/src/main/java/com/quransafeguard/hifz/preview/Ui.java");
        for (String name : new String[]{"home", "route", "compass", "door", "column",
                "buttress", "keystone", "torch", "mirror"}) {
            assertTrue("Missing palace resource: " + name, ui.contains("R.drawable.ic_palace_" + name));
        }
    }
}
