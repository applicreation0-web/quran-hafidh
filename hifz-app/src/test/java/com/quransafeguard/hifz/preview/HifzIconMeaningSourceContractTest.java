package com.quransafeguard.hifz.preview;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;

/** Hifz pedagogical grammar is tested separately from the frozen 23 UI icons. */
public final class HifzIconMeaningSourceContractTest {
    private static String read(String file) throws Exception {
        Path p=Paths.get(file);
        if(!Files.exists(p))p=Paths.get("..",file);
        return new String(Files.readAllBytes(p),StandardCharsets.UTF_8);
    }
    private static String drawable(String icon) throws Exception {
        return read("hifz-app/src/main/res/drawable/ic_palace_"+icon+".xml");
    }
    @Test public void distinctModesHaveCoherentBookPageAndVisibilitySymbols() throws Exception {
        String ui=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/Ui.java");
        for(String name:new String[]{"door","buttress","column","keystone","torch","mirror"})
            assertTrue(ui.contains("R.drawable.ic_palace_"+name));
        // Reprise is present in both cumulative modes, never in initial introduction.
        assertTrue(drawable("buttress").contains("M12.8 9.5C"));
        assertTrue(drawable("column").contains("M8.5 13.5C"));
        assertTrue(drawable("keystone").contains("M10.5 11.7C"));
        assertFalse(drawable("door").contains("M12.8 9.5C"));
        assertTrue(drawable("torch").contains("m2 2 20 20")); // Lucide eye-off
        assertTrue(drawable("mirror").contains("cx") == false);
        assertNotEquals(drawable("torch"),drawable("mirror"));
    }
    @Test public void screenPairsActualSabqiItqanAndRevisionWorkflows() throws Exception {
        String main=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        assertTrue(main.contains("pathTile(learningModes, \"Apprentissage\""));
        assertTrue(main.contains("pathTile(learningModes, \"Renforcement\""));
        assertTrue(main.contains("pathTile(otherModes, \"Stabilisation\""));
        assertTrue(main.contains("pathTile(otherModes, \"Consolidation\""));
        assertTrue(main.contains("pathTile(reviewModes, \"Révision active\""));
        assertTrue(main.contains("pathTile(reviewModes, \"Révision passive\""));
        assertTrue(main.contains("ic_hifz_nav_reading"));
        assertTrue(main.contains("ic_hifz_nav_progress"));
        assertTrue(main.contains("ic_hifz_catchup_clock"));
    }
    @Test public void completionStatusesCannotBorrowActionIcons() throws Exception {
        String ui=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/Ui.java");
        assertTrue(ui.contains("R.drawable.ic_hifz_acquired"));
        assertTrue(ui.contains("R.drawable.ic_hifz_final_review"));
        assertTrue(ui.contains("R.drawable.ic_hifz_waiting"));
        assertFalse(ui.contains("if (s.equals(\"acquis\")) return R.drawable.ic_palace_"));
    }
}
