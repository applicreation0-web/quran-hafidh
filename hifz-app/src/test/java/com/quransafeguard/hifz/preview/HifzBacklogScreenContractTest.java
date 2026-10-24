package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public final class HifzBacklogScreenContractTest {
    private String file(String suffix)throws Exception {
        Path f=Paths.get("hifz-app/src/main/java/com/quransafeguard/hifz/preview/"+suffix);
        if(!Files.exists(f)) f=Paths.get("../hifz-app/src/main/java/com/quransafeguard/hifz/preview/"+suffix);
        return new String(Files.readAllBytes(f),StandardCharsets.UTF_8);
    }
    @Test public void overdueBypassesAreGuardedAtBothAndroidEntryPoints()throws Exception {
        String home=file("MainActivity.java"),session=file("HifzSessionActivity.java");
        assertTrue(home.contains("HifzBacklog.pending(prefs,ledger"));
        assertTrue(home.contains("oldest.date.equals(scheduledDate)"));
        assertTrue(session.contains("HifzBacklog.pending("));
        assertTrue(session.contains("oldest.date.equals(sessionDate)"));
        assertTrue(session.contains("attendance.capture(prefs)"));
    }
    @Test public void booxPalaceIconsAreRealAndLarge()throws Exception {
        String home=file("MainActivity.java"),ui=file("Ui.java");
        for(String s:new String[]{"door","column","buttress","keystone","torch","mirror"})
            assertTrue(ui.contains("R.drawable.ic_palace_"+s));
        assertTrue(home.contains("booxIconDp(2, 124)"));
        assertTrue(home.contains("architectural.setBounds(0, 0, Ui.dp(this, sideDp), Ui.dp(this, sideDp))"));
        assertTrue(home.contains("backlogParcoursRow = backlogRow()"));
        assertTrue(home.contains("backlogHomeRow = backlogRow()"));
    }
    @Test public void noRetroactiveSundayPromotion()throws Exception {
        String home=file("MainActivity.java");
        assertTrue(home.contains("aucun crédit anticipé"));
        assertTrue(home.contains("learningSnowballFinalUnits(HifzClock.today())"));
        assertTrue(home.contains("stabilizationSnowballFinalUnits(HifzClock.today())"));
    }
}
