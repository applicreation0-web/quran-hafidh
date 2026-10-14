package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

/** The English Ibn Kathir cue uses the exact unframed Al-Munir title-sheet grammar. */
public final class SemanticTitlePopupParitySourceContractTest {
    private static String code() throws Exception {
        Path p=Paths.get("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SemanticTitlePopup.java");
        if(!Files.exists(p))p=Paths.get("..",p.toString());
        return new String(Files.readAllBytes(p),StandardCharsets.UTF_8);
    }

    @Test public void englishAndArabicShareSameUnframedBottomSheet() throws Exception {
        String src=code();
        assertTrue(src.contains("return create(activity,title,previous,true);"));
        assertTrue(src.contains("return create(activity,title,previous,false);"));
        assertTrue(src.contains("window.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL)"));
        assertTrue(src.contains("window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)"));
        assertTrue(src.contains("window.setBackgroundDrawableResource(android.R.color.transparent)"));
        assertTrue(src.contains("}, arabic ? 3200L : 8000L);"));
        assertTrue("BOOX English title must use a readable font",
            src.contains(":Ui.text(activity, title.trim(), 14f, false);"));
        assertFalse("Al-Munir cues never had an outlined card",src.contains("shell.setBackground("));
        assertFalse("No bordered title surface",src.contains("panel.setStroke("));
        assertFalse("No artificial title card",src.contains("new android.graphics.drawable.GradientDrawable()"));
    }
}
