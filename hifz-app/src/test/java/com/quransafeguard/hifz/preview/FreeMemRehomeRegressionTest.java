package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Hiding the standalone free-memory home tile must never delete its
 * independent memorisation, saved repetitions, archive, or manual masks.
 * Reader/Atlas remain byte-protected by guard_atlas_boox_visual_snapshot.py.
 */
public final class FreeMemRehomeRegressionTest {
    private static String source(String name) throws Exception {
        String prefix = "hifz-app/src/main/java/com/quransafeguard/hifz/preview/";
        Path file = Paths.get(prefix + name);
        if (!Files.exists(file)) file = Paths.get("..", prefix + name);
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    @Test public void homeRemovesCompetingLibreTileButRetainsFourDestinations() throws Exception {
        String main = source("MainActivity.java");
        assertFalse(main.contains("secondaryTile(secondary, \"Mémorisation libre\""));
        assertFalse(main.contains("geometryActions.add(free)"));
        assertTrue(main.contains("primaryTile(destinations, \"Parcours Hifz\""));
        assertTrue(main.contains("primaryTile(destinations, \"Lecture\""));
        assertTrue(main.contains("primaryTile(destinations, \"Atlas\""));
        assertTrue(main.contains("secondaryTile(secondary, \"Progression\""));
    }

    @Test public void standaloneMemorisationStaysDiscoverableAsAnAdvancedTool() throws Exception {
        String settings = source("SettingsActivity.java");
        assertTrue(settings.contains("Ui.settingRow(this,\"Mémorisation ponctuelle\""));
        assertTrue(settings.contains("startActivity(new Intent(this,FreeMemActivity.class))"));
        String standalone = source("FreeMemActivity.java");
        assertTrue(standalone.contains("getSharedPreferences(\"free_mem_preview\""));
        assertTrue(standalone.contains("int[] levels={0,25,50,75,100};"));
        assertTrue(standalone.contains("count++;save();updateCounter()"));
        assertTrue(standalone.contains("count--;save();updateCounter()"));
        assertTrue(standalone.contains("HifzAudioDialog"));
    }

    @Test public void backupsPreserveFreeMemoryAndGuidedHifzNeverSharesItsCounter() throws Exception {
        String backup = source("HifzBackup.java");
        String standalone = source("FreeMemActivity.java");
        assertTrue(backup.contains("root.put(\"free_mem\", encoded("));
        assertTrue(backup.contains("validateFreeMemory(freeMem)"));
        assertFalse(standalone.contains("completeSabqiBlockV6("));
        assertFalse(standalone.contains("completeStabilizationBlockV6("));
    }
}
