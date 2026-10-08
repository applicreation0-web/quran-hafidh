package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Regression: validated Qāf blocks must not reappear as Saturday/next Tuesday. */
public final class ItqanWeeklyForecastTest {
    private static AnchoringQueue.Entry entry(String start, String end) {
        return new AnchoringQueue.Entry(start, end, AnchoringQueue.Origin.PROMOTED,
            AnchoringQueue.ItqanProtocol.FULL, 0);
    }

    @Test public void afterTuesdayThursdayQafSatStartsAtNextUnitNotQafAgain() {
        AnchoringQueue.Entry qaf = entry("50:1", "50:45");
        AnchoringQueue.Entry next = entry("51:1", "51:60");
        ItqanRotationPolicy.State afterQaf = new ItqanRotationPolicy.State(
            ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS, new VerseRef(51, 1), false);

        ItqanWeeklyForecast forecast = new ItqanWeeklyForecast(afterQaf, next, 0,
            (leg, postNas) -> leg == ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS
                ? Arrays.asList(qaf, next) : Collections.emptyList());

        // Tuesday 6 + Thursday 8 already validated Qāf. Saturday 10 must show 51:1.
        assertEquals("51:1", forecast.entry().start);
        assertEquals(0, forecast.blockIndex());
        forecast.completedBlock(2);
        // Following Tuesday 13 must show second block of the next unit, NOT Qāf.
        assertEquals("51:1", forecast.entry().start);
        assertEquals(1, forecast.blockIndex());
    }

    @Test public void partiallyFinishedQafResumesItsSecondBlockThenMovesForward() {
        AnchoringQueue.Entry qaf = entry("50:1", "50:45");
        AnchoringQueue.Entry next = entry("51:1", "51:60");
        ItqanRotationPolicy.State duringQaf = new ItqanRotationPolicy.State(
            ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS, new VerseRef(50, 1), false);

        ItqanWeeklyForecast forecast = new ItqanWeeklyForecast(duringQaf, qaf, 1,
            (leg, postNas) -> leg == ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS
                ? Arrays.asList(qaf, next) : Collections.emptyList());
        assertTrue(forecast.isInitialUnit());
        assertEquals(1, forecast.blockIndex());
        forecast.completedBlock(2);
        assertFalse(forecast.isInitialUnit());
        assertEquals("51:1", forecast.entry().start);
        assertEquals(0, forecast.blockIndex());
        // The real saved state was not mutated by the forecast.
        assertEquals(new VerseRef(50, 1), duringQaf.cursor);
    }

    @Test public void crossingAnNasSwitchesForecastToMaintenanceWithoutChangingSavedState() {
        AnchoringQueue.Entry nas = entry("114:1", "114:6");
        AnchoringQueue.Entry front = entry("2:1", "2:10");
        ItqanRotationPolicy.State state = new ItqanRotationPolicy.State(
            ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS, new VerseRef(114, 1), false);

        ItqanWeeklyForecast forecast = new ItqanWeeklyForecast(state, nas, 0,
            (leg, postNas) -> {
                if (!postNas) return leg == ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS
                    ? Collections.singletonList(nas) : Collections.emptyList();
                return leg == ItqanRotationPolicy.Leg.FRONT_BAQARA_HUJURAT
                    ? Collections.singletonList(front) : Collections.emptyList();
            });
        assertEquals(ItqanMaintenancePolicy.Regime.DEEP_FIRST_PASS, forecast.regime());
        forecast.completedBlock(1);
        assertEquals(ItqanMaintenancePolicy.Regime.POST_NAS_MAINTENANCE, forecast.regime());
        assertEquals("2:1", forecast.entry().start);
        assertFalse(state.initialTailCompleted);
    }
}
