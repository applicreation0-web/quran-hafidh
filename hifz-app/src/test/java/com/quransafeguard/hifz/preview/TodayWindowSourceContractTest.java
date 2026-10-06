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

    @Test public void todayOpensTheWindowWhenSeveralSessionsAreExpected() throws Exception {
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        assertTrue(main.contains("if(sessions.size()<=1){ openMode(mode,scheduled); return; }\n        showTodayWindow(sessions);"));
        assertTrue("both Révision sessions are always listed",
            main.contains("HifzSessionActivity.MURAJAAH_ACTIVE,today,todayStr.equals(prefs.lastActiveMurajaahDate()),nextMode);")
            && main.contains("HifzSessionActivity.MURAJAAH,today,todayStr.equals(prefs.lastMurajaahDate()),nextMode);"));
        assertTrue("each line carries its state and the next one due is marked",
            main.contains("items[i]=(s.done?\"✓ \":s.next?\"▸ \":\"   \")+s.label+\" · \"+(s.done?\"fait\":\"à faire\");"));
    }
}
