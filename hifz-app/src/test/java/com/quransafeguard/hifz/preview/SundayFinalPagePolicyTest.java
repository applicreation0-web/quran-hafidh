package com.quransafeguard.hifz.preview;

import static org.junit.Assert.*;
import org.junit.Test;
import java.util.*;

public final class SundayFinalPagePolicyTest {
    private static String u(String... lineIds) {
        return ConsolidationPhysicalUnitPolicy.encodeLineUnit(Arrays.asList(lineIds));
    }
    private static Map<String,SundayFinalPagePolicy.LinePosition> geometry() {
        Map<String,SundayFinalPagePolicy.LinePosition> positions=new HashMap<>();
        positions.put("p11-l1",new SundayFinalPagePolicy.LinePosition(11,151));
        positions.put("p11-l2",new SundayFinalPagePolicy.LinePosition(11,152));
        positions.put("p11-l3",new SundayFinalPagePolicy.LinePosition(11,153));
        positions.put("p12-l1",new SundayFinalPagePolicy.LinePosition(12,166));
        positions.put("p12-l2",new SundayFinalPagePolicy.LinePosition(12,167));
        return positions;
    }
    @Test public void groupsSplitBlocksIntoRealPagesWithoutRepeatingAnyLine() {
        List<String> pages=SundayFinalPagePolicy.physicalPageUnits(Arrays.asList(
            u("p11-l2","p12-l1"),u("p11-l1","p11-l3","p12-l2")),geometry());
        assertEquals(2,pages.size());
        assertEquals(Arrays.asList("p11-l1","p11-l2","p11-l3"),
            ConsolidationPhysicalUnitPolicy.decodeLineUnit(pages.get(0)));
        assertEquals(Arrays.asList("p12-l1","p12-l2"),
            ConsolidationPhysicalUnitPolicy.decodeLineUnit(pages.get(1)));
    }
    @Test public void overlappingReceiptsDoNotCreateDoubleCredit() {
        List<String> pages=SundayFinalPagePolicy.physicalPageUnits(
            Arrays.asList(u("p11-l1","p11-l2"),u("p11-l2","p11-l3")),geometry());
        assertEquals(1,pages.size());
        assertEquals(3,ConsolidationPhysicalUnitPolicy.decodeLineUnit(pages.get(0)).size());
    }
    @Test public void everyUnacquiredLineRequiresReceipt() {
        List<String> page=Arrays.asList("p11-l1","p11-l2","p11-l3");
        assertFalse(SundayFinalPagePolicy.earnedEveryLine(page,
            Arrays.asList(u("p11-l1","p11-l2")),Collections.emptySet()));
        assertTrue(SundayFinalPagePolicy.earnedEveryLine(page,
            Arrays.asList(u("p11-l1","p11-l2")),Collections.singleton("p11-l3")));
    }
    @Test public void absentQcfGeometryMustFailClosed() {
        try {
            SundayFinalPagePolicy.physicalPageUnits(
                Collections.singletonList(u("p11-l1","not-a-real-line")),geometry());
            fail("Missing line metadata must fail closed");
        } catch(IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("Missing verified QCF"));
        }
    }
}
