package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** RED contract for explicit user resolution of contradictory legacy corpus state. */
public final class HifzV6QuarantineResolutionTest {
    @Test public void keepAsAcquiredRestoresExactLegacyDateAndRemovesQuarantine() throws Exception {
        long old = LocalDate.of(2026, 8, 30).toEpochDay();
        HifzCorpusState initial = quarantined("L1", old);

        HifzCorpusState resolved = resolve(initial, "L1", "KEEP_AS_ACQUIRED");

        assertFalse(resolved.quarantineLineIds().contains("L1"));
        assertFalse(resolved.quarantineLegacyLastReviewed().containsKey("L1"));
        assertFalse(resolved.toAnchorLineIds().contains("L1"));
        assertTrue(resolved.acquiredCreditLineIds().contains("L1"));
        assertEquals(Long.valueOf(old), resolved.activeLastReviewedEpochDays().get("L1"));
        assertTrue(resolved.legacyImportedLineIds().contains("L1"));
        assertFalse(resolved.unknownDueLineIds().contains("L1"));
    }

    @Test public void keepAsAcquiredWithoutReliableDateBecomesUnknownDueWithoutInventingToday() throws Exception {
        HifzCorpusState initial = quarantinedWithoutDate("L2");

        HifzCorpusState resolved = resolve(initial, "L2", "KEEP_AS_ACQUIRED");

        assertFalse(resolved.quarantineLineIds().contains("L2"));
        assertTrue(resolved.acquiredCreditLineIds().contains("L2"));
        assertTrue(resolved.unknownDueLineIds().contains("L2"));
        assertFalse(resolved.activeLastReviewedEpochDays().containsKey("L2"));
        assertFalse(resolved.legacyImportedLineIds().contains("L2"));
    }

    @Test public void returnToStabilizationPreservesHistoricalDateOnlyAsInactiveOrphan() throws Exception {
        long old = LocalDate.of(2026, 8, 29).toEpochDay();
        HifzCorpusState initial = quarantined("L3", old);

        HifzCorpusState resolved = resolve(initial, "L3", "RETURN_TO_STABILIZATION");

        assertFalse(resolved.quarantineLineIds().contains("L3"));
        assertFalse(resolved.quarantineLegacyLastReviewed().containsKey("L3"));
        assertTrue(resolved.toAnchorLineIds().contains("L3"));
        assertEquals(Long.valueOf(old), resolved.legacyOrphanDates().get("L3"));
        assertFalse(resolved.acquiredCreditLineIds().contains("L3"));
        assertFalse(resolved.activeJ10LineIds().contains("L3"));
        assertFalse(resolved.legacyImportedLineIds().contains("L3"));
    }

    @Test(expected = IllegalStateException.class)
    public void onlyQuarantinedLinesCanBeResolved() throws Exception {
        HifzCorpusState normal = HifzV6Migration.classify(new HifzV6Migration.Input(
            set("L4"), Collections.emptySet(), Collections.emptySet(),
            Collections.emptyMap(), Collections.emptyMap()));
        resolve(normal, "L4", "KEEP_AS_ACQUIRED");
    }

    private static HifzCorpusState quarantined(String lineId, long oldDate) {
        return HifzV6Migration.classify(new HifzV6Migration.Input(
            set(lineId), Collections.emptySet(), set(lineId),
            map(lineId, oldDate), Collections.emptyMap()));
    }

    private static HifzCorpusState quarantinedWithoutDate(String lineId) {
        return HifzV6Migration.classify(new HifzV6Migration.Input(
            set(lineId), Collections.emptySet(), set(lineId),
            Collections.emptyMap(), Collections.emptyMap()));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static HifzCorpusState resolve(HifzCorpusState state, String lineId, String resolutionName)
            throws Exception {
        Class<?> resolutionClass = null;
        for (Class<?> nested : HifzV6Migration.class.getDeclaredClasses()) {
            if (nested.getSimpleName().equals("QuarantineResolution")) {
                resolutionClass = nested;
                break;
            }
        }
        assertNotNull("HifzV6Migration.QuarantineResolution must exist", resolutionClass);
        Object resolution = Enum.valueOf((Class<? extends Enum>) resolutionClass.asSubclass(Enum.class), resolutionName);
        Method method = HifzV6Migration.class.getDeclaredMethod(
            "resolveQuarantine", HifzCorpusState.class, String.class, resolutionClass);
        method.setAccessible(true);
        try {
            return (HifzCorpusState) method.invoke(null, state, lineId, resolution);
        } catch (java.lang.reflect.InvocationTargetException error) {
            Throwable cause = error.getCause();
            if (cause instanceof IllegalStateException) throw (IllegalStateException) cause;
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            throw error;
        }
    }

    private static LinkedHashSet<String> set(String... values) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        Collections.addAll(result, values);
        return result;
    }

    private static Map<String, Long> map(String key, long value) {
        LinkedHashMap<String, Long> result = new LinkedHashMap<>();
        result.put(key, value);
        return result;
    }
}
