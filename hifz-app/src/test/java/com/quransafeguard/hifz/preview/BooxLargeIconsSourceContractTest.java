package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Drawable source guard: primary BOOX images remain large and do not replace frozen action icons. */
public final class BooxLargeIconsSourceContractTest {
    private static String read(String path) throws Exception {
        Path direct = Paths.get(path);
        if (!Files.exists(direct)) direct = Paths.get("..", path);
        return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
    }

    @Test public void eachDedicatedPrimaryIconRetainsItsOwnTwoToneVector() throws Exception {
        String[] icons = {"accueil", "parcours", "lecture", "atlas", "progression", "parametres",
            "apprentissage", "renforcement", "stabilisation", "consolidation",
            "revision_active", "revision_passive"};
        for (String icon : icons) {
            String svg = read("hifz-app/src/main/res/drawable/ic_haafidh_" + icon + ".xml");
            assertTrue(icon, svg.contains("<vector"));
            assertTrue(icon, svg.contains("android:width=\"124dp\""));
            assertTrue(icon, svg.contains("android:pathData="));
            assertFalse("Do not flatten independent grey layers: " + icon, svg.contains("android:tint="));
        }
    }

    @Test public void mainRetainsLargeBooxSizingAndNoBlackTintForImages() throws Exception {
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        assertTrue(main.contains("booxIconDp(3, 112)"));
        assertTrue(main.contains("booxIconDp(2, 124)"));
        assertTrue(main.contains("largeIconFor(role)"));
        assertTrue(main.contains("largeIconFor(label)"));
        assertFalse(main.contains("icon.setColorFilter(Ui.INK)"));
        assertFalse(main.contains("name.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(Ui.INK))"));
        String ui = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/Ui.java");
        assertTrue(ui.contains("if (s.contains(\"ancre\")) return R.drawable.ic_ui_semantic_anchor;"));
        assertTrue(ui.contains("if (s.contains(\"amorce\")) return R.drawable.ic_ui_recall_key;"));
    }
}
