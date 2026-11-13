package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** User decision: "Aujourd’hui" opens a window listing every session expected today. */
public final class TodayWindowSourceContractTest {
    private static String read(String path) throws Exception {
        Path direct = Paths.get(path);
        Path file = Files.exists(direct) ? direct : Paths.get("..", path);
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    @Test public void todayAlwaysShowsSessionChoicesAndCompletionStates() throws Exception {
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        org.junit.Assert.assertFalse(main.contains("if(sessions.size()<=1){ openMode(mode,scheduled); return; }"));
        assertTrue(main.contains("showTodayWindow(sessions);"));
        assertTrue(main.contains("if(sessions.isEmpty()){"));
        assertTrue("Both active slots have their own receipt and selectable entry",
            main.contains("addActiveReviewToday(out,today,ActiveReviewSlots.Slot.MORNING")
            && main.contains("addActiveReviewToday(out,today,ActiveReviewSlots.Slot.EVENING")
            && main.contains("slot.mode,today,reviews.receipt(today,slot)!=null,nextMode);"));
        assertTrue("Passive reading is optional",main.contains("?\" · Facultative\""));
        assertTrue("each line carries its state and the next one due is marked",
            main.contains("items[i]=(s.done?\"✓ \":s.next?\"▸ \":\"  \")+slot+title+")
                && main.contains("(s.done?\" · Terminé\""));
    }
}
