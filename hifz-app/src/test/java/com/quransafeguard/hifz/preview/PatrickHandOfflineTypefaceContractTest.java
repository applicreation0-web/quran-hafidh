package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** The user's approved Patrick Hand is packaged locally, not fetched at runtime. */
public final class PatrickHandOfflineTypefaceContractTest {
    private static Path locate(String file) {
        Path path = Paths.get(file);
        return Files.exists(path) ? path : Paths.get("..", file);
    }
    private static String source(String file) throws Exception {
        return new String(Files.readAllBytes(locate(file)), StandardCharsets.UTF_8);
    }
    @Test public void fontBinaryAndRedistributionLicenseAreBundled() throws Exception {
        byte[] font = Files.readAllBytes(locate("hifz-app/src/main/res/font/patrick_hand.ttf"));
        assertTrue("An actual Patrick Hand TTF must be bundled offline", font.length > 100000);
        assertEquals("TTF SFNT header byte 0", 0, font[0] & 0xff);
        assertEquals("TTF SFNT header byte 1", 1, font[1] & 0xff);
        assertEquals("TTF SFNT header byte 2", 0, font[2] & 0xff);
        assertEquals("TTF SFNT header byte 3", 0, font[3] & 0xff);
        String license = source("hifz-app/src/main/assets/licenses/OFL-Patrick-Hand.txt");
        assertTrue(license.contains("SIL OPEN FONT LICENSE Version 1.1"));
        assertTrue(license.contains("Patrick Wagesreiter"));
    }
    @Test public void androidThemeAndHifdhCanvasSharePatrickHandWithoutTouchingQcf() throws Exception {
        String theme = source("hifz-app/src/main/res/values/styles.xml");
        String ui = source("hifz-app/src/main/java/com/quransafeguard/hifz/preview/Ui.java");
        String timeline = source("hifz-app/src/main/java/com/quransafeguard/hifz/preview/CapHifzTimelineView.java");
        assertTrue(theme.contains("android:fontFamily\">@font/patrick_hand"));
        assertTrue(ui.contains("context.getResources().getFont(R.font.patrick_hand)"));
        assertTrue(ui.contains("static Typeface typeface(Context context, int style)"));
        assertTrue(ui.contains("private static volatile Typeface interfaceFont;"));
        assertTrue(timeline.contains("Ui.typeface(getContext(), bold ? Typeface.BOLD : Typeface.NORMAL)"));
        assertFalse(ui.contains("Typeface.create(\"sans-serif\""));
        assertFalse(ui.contains("Typeface.create(\"serif\""));
        assertFalse(ui.contains("createFromAsset(context.getAssets(), \"fonts/Kalam-Regular.ttf\")"));
    }
}
