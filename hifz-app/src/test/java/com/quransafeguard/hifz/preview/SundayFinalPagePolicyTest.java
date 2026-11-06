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
    /** Real Sunday pages have no mandatory 15/22-line minimum: skipped weekdays
     * never create imaginary physical lines or receipts. */
    private static void verifyPartialFinal(int actualLines, int firstBlockLines, int fullWeekTarget) {
        assertTrue("This represents a genuinely missed weekly slot",actualLines<fullWeekTarget);
        List<String> ids=new ArrayList<>();
        Map<String,SundayFinalPagePolicy.LinePosition> positions=new LinkedHashMap<>();
        for(int i=0;i<actualLines;i++){
            String id="p11-l"+(i+1);
            ids.add(id);
            positions.put(id,new SundayFinalPagePolicy.LinePosition(11,151+i));
        }
        String first=ConsolidationPhysicalUnitPolicy.encodeLineUnit(
            ids.subList(0,firstBlockLines));
        String second=ConsolidationPhysicalUnitPolicy.encodeLineUnit(
            ids.subList(firstBlockLines,ids.size()));
        List<String> receipts=Arrays.asList(first,second);
        List<String> pages=SundayFinalPagePolicy.physicalPageUnits(receipts,positions);
        assertEquals(1,pages.size());
        assertEquals("No missing slot may produce extra lines",ids,
            ConsolidationPhysicalUnitPolicy.decodeLineUnit(pages.get(0)));
        assertTrue("Every included line still has its own verified evening source",
            SundayFinalPagePolicy.earnedEveryLine(ids,receipts,Collections.emptySet()));
        assertFalse("An unverified evening block must not graduate",
            SundayFinalPagePolicy.earnedEveryLine(ids,
                Collections.singletonList(first),Collections.emptySet()));
    }

    @Test public void sabqiSundayAcceptsOnlyTwoOfThreeFiveLineLessons() {
        verifyPartialFinal(10,5,15);
    }

    @Test public void itqanSundayAcceptsOnlyEightPlusSevenOfTwentyTwoLines() {
        verifyPartialFinal(15,8,22);
    }

    @Test public void oneCompletedBlockCanBeAValidPartialFinal() {
        verifyPartialFinal(8,4,22);
        verifyPartialFinal(5,2,15);
    }

    @Test public void noRealSourceNeverCreatesAFakeSundayFinale() {
        assertTrue(SundayFinalPagePolicy.physicalPageUnits(
            Collections.emptyList(),Collections.emptyMap()).isEmpty());
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
