package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** BOOX menu visual contract only; Hifz engines and earned progress stay untouched. */
public final class BooxMenuHierarchySourceContractTest {
    private static String source(String relative) throws Exception {
        Path path = Paths.get(relative);
        if (!Files.exists(path)) path = Paths.get("..", relative);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    @Test public void sixLargeHifzModesHaveReadableDistinctSecondLine() throws Exception {
        String home = source("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        assertTrue(home.contains("TextView subtitle = Ui.menuSubline(this, pathSubtitle(label))"));
        assertTrue(home.contains("tile.addView(subtitle)"));
        for (String shortText : new String[] {
            "Nouvelles lignes", "Reprise du Sabqi", "Fixation Itqān",
            "Cumul Itqān", "Sans texte", "Avec texte"
        }) assertTrue(shortText, home.contains(shortText));
        assertTrue("Long stabilization state must remain available for updates",
            home.contains("cue.setVisibility(View.GONE);") && home.contains("tile.addView(cue);"));
    }

    @Test public void menuLabelsAndValuesHaveSeparateTypefacesAndKeepLiveUpdates() throws Exception {
        String ui = source("hifz-app/src/main/java/com/quransafeguard/hifz/preview/Ui.java");
        assertTrue(ui.contains("Typeface.create(\"serif\", Typeface.NORMAL)"));
        assertTrue(ui.contains("secondary.setMaxLines(2)"));
        assertTrue(ui.contains("TextView name = bookText(context, label, 17f, true)"));
        assertTrue(ui.contains("copy.addView(name)") && ui.contains("copy.addView(current)"));
        assertTrue(ui.contains("return (TextView) copy.getChildAt(1)"));
        assertTrue(ui.contains("row.setMinimumHeight(dp(context, 56))"));
    }

    @Test public void launcherAndHomeUseSameCleanBrandPictogram() throws Exception {
        String manifest = source("hifz-app/src/main/AndroidManifest.xml");
        String main = source("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        String asset = source("hifz-app/src/main/res/drawable/ic_quran_hifz_header.xml");
        assertTrue(manifest.contains("android:icon=\"@drawable/ic_quran_hifz_header\""));
        assertTrue(manifest.contains("android:roundIcon=\"@drawable/ic_quran_hifz_header\""));
        assertTrue(main.contains("appEmblem.setImageResource(R.drawable.ic_quran_hifz_header)"));
        assertTrue(asset.contains("<vector") && asset.contains("android:pathData="));
        assertFalse(asset.contains("<bitmap"));
    }
}
