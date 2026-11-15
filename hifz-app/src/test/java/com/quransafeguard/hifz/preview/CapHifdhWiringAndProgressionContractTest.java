package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.QuranCanon;
import com.quransafeguard.hifz.core.VerseRef;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Contradictory checks: source routing, canonical continuation, true one-way post-Nas latch. */
public final class CapHifdhWiringAndProgressionContractTest {
    private static String source(String file) throws Exception {
        Path path = Paths.get(file);
        if (!Files.exists(path)) path = Paths.get("..", file);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    @Test public void alBaqaraContinuesAtAlImranAndOnlyStopsAfterAlFath() {
        assertEquals(new VerseRef(3, 1), QuranCanon.INSTANCE.next(new VerseRef(2, 286)));
        assertFalse(SabqiRoute.allNewSabqiComplete(new VerseRef(3, 1)));
        assertFalse(SabqiRoute.allNewSabqiComplete(new VerseRef(48, 29)));
        assertTrue(SabqiRoute.allNewSabqiComplete(new VerseRef(49, 1)));
        assertEquals(new VerseRef(48, 29), SabqiRoute.ROUTE_END);
    }

    @Test public void postNasMaintienCannotStartFromForecastOrBeRolledBack() {
        InMemoryPrefs disk = new InMemoryPrefs();
        assertFalse(ItqanRegimeStore.postNasMaintenance(disk));
        ItqanRotationPolicy.State current = new ItqanRotationPolicy.State(
            ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS,
            new VerseRef(114, 5), false);
        assertTrue(ItqanRegimeStore.saveRotationState(disk, current));
        assertFalse(ItqanRegimeStore.postNasMaintenance(disk));
        ItqanRotationPolicy.State completed = ItqanRotationPolicy.advancedPast(current,
            new VerseRef(114, 6));
        assertTrue(completed.initialTailCompleted);
        assertTrue(ItqanRegimeStore.saveRotationState(disk, completed));
        assertTrue(ItqanRegimeStore.postNasMaintenance(disk));
        assertTrue(ItqanRegimeStore.saveRotationState(disk, current));
        assertTrue("A stale cursor cannot undo the certified arrival", ItqanRegimeStore.postNasMaintenance(disk));
    }

    @Test public void realCapHifdhIsWiredToHomeAndProgressWithoutWrites() throws Exception {
        String home = source("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        String panel = source("hifz-app/src/main/java/com/quransafeguard/hifz/preview/CapHifzPanel.java");
        String view = source("hifz-app/src/main/java/com/quransafeguard/hifz/preview/CapHifzTimelineView.java");
        String prefs = source("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        assertTrue(home.contains("capHifz = new CapHifzPanel(this)")
            && home.contains("root.addView(capHifz)")
            && home.contains("capHifz.refresh(geometry)"));
        assertTrue(panel.contains("prefs.sabqiLineCursor()")
            && panel.contains("prefs.itqanPostNasMaintenance()")
            && panel.contains("prefs.juzMaintenance().read()")
            && panel.contains("PROJECTION_ATTENDANCE_PERCENT / 100.0")
            && panel.contains("openMushaf(milestone.verse)"));
        assertTrue(prefs.contains("migrateLegacySabqiEndDefault()")
            && prefs.contains("SabqiRoute.ROUTE_END.toString()"));
        assertTrue(home.contains("boolean afterNas=prefs.itqanPostNasMaintenance()")
            && home.contains("afterNas ? \"Maintien\" : \"Stabiliser\"")
            && home.contains("prochain.setVisibility(afterNas ? View.GONE : View.VISIBLE)"));
        assertTrue(view.contains("QuranSurahNames.rawName(coming.get(i).verse.getSurah())")
            && view.contains("coming.get(i).verse.getAyah()")
            && view.contains("drawMaintenanceLane(canvas"));
        assertFalse(panel.contains("completeDailyLesson("));
    }
}
