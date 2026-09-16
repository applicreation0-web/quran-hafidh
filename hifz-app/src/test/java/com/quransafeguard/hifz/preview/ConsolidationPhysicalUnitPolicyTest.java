package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Consolidation must consume the exact Stabilisation physical units, even inside one long verse. */
public final class ConsolidationPhysicalUnitPolicyTest {
    @Test public void fifteenLineSingleVerseProducesTwoReadyConsolidationUnits() throws Exception {
        List<GeometryRepository.LineMeta> lines = sameVerseLines(15);
        List<StabilizationHalfPagePolicy.Unit> planned = StabilizationHalfPagePolicy.planPage(lines);
        assertEquals(2, planned.size());
        Set<String> stabilized = ids(lines);

        List<?> ready = readyUnits(planned, stabilized, Collections.emptySet(), 3);
        assertEquals(2, ready.size());
        assertUnitSize(ready.get(0), 7);
        assertUnitSize(ready.get(1), 8);
    }

    @Test public void acquiredFirstHalfLeavesOnlySecondHalfReady() throws Exception {
        List<GeometryRepository.LineMeta> lines = sameVerseLines(15);
        List<StabilizationHalfPagePolicy.Unit> planned = StabilizationHalfPagePolicy.planPage(lines);
        Set<String> stabilized = ids(lines);
        Set<String> acquired = new LinkedHashSet<>(planned.get(0).lineIds);

        List<?> ready = readyUnits(planned, stabilized, acquired, 3);
        assertEquals(1, ready.size());
        assertUnitSize(ready.get(0), 8);
    }

    @Test public void maxUnitsFreezesOneToThreeWithoutMergingUnits() throws Exception {
        List<GeometryRepository.LineMeta> lines = sameVerseLines(15);
        List<StabilizationHalfPagePolicy.Unit> planned = StabilizationHalfPagePolicy.planPage(lines);
        List<?> ready = readyUnits(planned, ids(lines), Collections.emptySet(), 1);
        assertEquals(1, ready.size());
        assertUnitSize(ready.get(0), 7);
    }

    @Test public void codecRoundTripsExactPhysicalLineIdsWithoutVerseIdentity() throws Exception {
        List<String> ids = Arrays.asList("p3|line,1", "same/verse:line-2", "unicode-é-3");
        Class<?> policy = policyClass();
        Method encode = policy.getDeclaredMethod("encodeLineUnit", List.class);
        Method decode = policy.getDeclaredMethod("decodeLineUnit", String.class);
        encode.setAccessible(true);
        decode.setAccessible(true);
        String encoded = (String) encode.invoke(null, ids);
        @SuppressWarnings("unchecked")
        List<String> decoded = (List<String>) decode.invoke(null, encoded);
        assertTrue(encoded.startsWith("L:"));
        assertEquals(ids, decoded);
    }

    private static List<?> readyUnits(List<StabilizationHalfPagePolicy.Unit> planned,
                                      Set<String> stabilized,
                                      Set<String> acquired,
                                      int maxUnits) throws Exception {
        Class<?> policy = policyClass();
        Method method = policy.getDeclaredMethod("readyUnits", List.class, Set.class, Set.class, int.class);
        method.setAccessible(true);
        return (List<?>) method.invoke(null, planned, stabilized, acquired, maxUnits);
    }

    private static Class<?> policyClass() {
        try {
            return Class.forName("com.quransafeguard.hifz.preview.ConsolidationPhysicalUnitPolicy");
        } catch (ClassNotFoundException missing) {
            fail("ConsolidationPhysicalUnitPolicy is required");
            throw new AssertionError(missing);
        }
    }

    private static void assertUnitSize(Object value, int expected) throws Exception {
        java.lang.reflect.Field lineIds = value.getClass().getDeclaredField("lineIds");
        lineIds.setAccessible(true);
        assertEquals(expected, ((List<?>) lineIds.get(value)).size());
    }

    private static List<GeometryRepository.LineMeta> sameVerseLines(int count) {
        ArrayList<GeometryRepository.LineMeta> lines = new ArrayList<>();
        VerseRef verse = new VerseRef(2, 282);
        for (int i = 0; i < count; i++) {
            lines.add(new GeometryRepository.LineMeta(i, "p3-line-" + i, 3, Collections.singletonList(verse)));
        }
        return lines;
    }

    private static Set<String> ids(List<GeometryRepository.LineMeta> lines) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (GeometryRepository.LineMeta line : lines) ids.add(line.id);
        return ids;
    }
}
