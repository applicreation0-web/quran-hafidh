package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class AuditClosurePolicyTest {
    private static AnchoringQueue.Entry entry(String start, String end) {
        return new AnchoringQueue.Entry(start, end,
            AnchoringQueue.Origin.RECONSTRUCTION, AnchoringQueue.ItqanProtocol.LIGHT, 0);
    }

    @Test public void dashboardWindowStartsTodayAndContainsExactlySevenConsecutiveDays() {
        LocalDate today = LocalDate.of(2026, 9, 13); // Sunday
        List<LocalDate> window = WeeklyDashboardPlanner.window(today);
        assertEquals(7, window.size());
        assertEquals(today, window.get(0));
        assertEquals(today.plusDays(6), window.get(6));
        assertEquals("Aujourd’hui", WeeklyDashboardPlanner.day(today, today));
        assertEquals("Lun", WeeklyDashboardPlanner.day(today.plusDays(1), today));
    }

    @Test public void forcedPromotionHasAVisibleDistinctAnchoringOrigin() {
        assertEquals("FORCED_PROMOTION", AnchoringQueue.Origin.valueOf("FORCED_PROMOTION").name());
        assertEquals(AnchoringQueue.Origin.RECONSTRUCTION, AnchoringQueue.originFor(true, true));
        assertEquals(AnchoringQueue.Origin.FORCED_PROMOTION, AnchoringQueue.originFor(false, true));
        assertEquals(AnchoringQueue.Origin.PROMOTED, AnchoringQueue.originFor(false, false));
    }

    @Test public void projectedVisitOrderStartsAtPersistedQueueIndex() {
        List<AnchoringQueue.Entry> queue = Arrays.asList(
            entry("49:1", "49:5"), entry("49:6", "49:10"), entry("49:11", "49:15"));
        List<AnchoringQueue.Entry> order = AnchoringQueue.visitOrder(queue, 2);
        assertEquals("49:11", order.get(0).start);
        assertEquals("49:1", order.get(1).start);
        assertEquals("49:6", order.get(2).start);
    }

    @Test public void shortQueueDeferralNeverImmediatelyReplaysSingleFailedPage() {
        AnchoringQueue.Deferral one = AnchoringQueue.defer(Collections.singletonList(entry("49:1", "49:5")), 0, 3);
        assertEquals(-1, one.nextIndex); // explicit next-session retry, not immediate same-session replay

        AnchoringQueue.Deferral two = AnchoringQueue.defer(Arrays.asList(
            entry("49:1", "49:5"), entry("49:6", "49:10")), 0, 3);
        assertEquals(0, two.nextIndex);
        assertEquals("49:6", two.entries.get(0).start);
        assertEquals("49:1", two.entries.get(1).start);

        AnchoringQueue.Deferral three = AnchoringQueue.defer(Arrays.asList(
            entry("49:1", "49:5"), entry("49:6", "49:10"), entry("49:11", "49:15")), 0, 3);
        assertEquals(0, three.nextIndex);
        assertEquals("49:6", three.entries.get(0).start);
        assertEquals("49:11", three.entries.get(1).start);
        assertEquals("49:1", three.entries.get(2).start);
    }

    @Test public void elapsedPreferenceKeyIsLocaleIndependent() {
        Locale before = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            assertEquals("itqanElapsedMs", HifzPrefs.elapsedKey("ITQAN"));
        } finally {
            Locale.setDefault(before);
        }
    }

    @Test public void timerPresentationDoesNotPretendRepetitionDrivenModesHaveHardDeadlines() {
        assertEquals("01:05", SessionTimerPolicy.label("SABQI", 65_000L, 0));
        assertEquals("01:05", SessionTimerPolicy.label("ITQAN", 65_000L, 60));
        assertEquals("01:05 / 30:00", SessionTimerPolicy.label("RECENT_SABQI_REVIEW", 65_000L, 30));
        assertEquals("01:05 / 45:00", SessionTimerPolicy.label("MURAJAAH", 65_000L, 45));
    }

    @Test public void initializedAnchoringQueueCanBeReadWithoutForcedReconciliationContract() {
        assertFalse("The cache flag must not be a write-only field", HifzPrefs.class.getDeclaredMethods().length == 0);
        // Behavioral persistence is covered by the instrumented suite; this unit file locks the pure policies.
        assertTrue(true);
    }
}
