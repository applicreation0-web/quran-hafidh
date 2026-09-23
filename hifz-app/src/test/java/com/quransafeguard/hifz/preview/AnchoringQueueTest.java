package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public final class AnchoringQueueTest {
    @Test public void reconstructionEntryKeepsItsPersistentClassification() {
        AnchoringQueue.Entry entry = new AnchoringQueue.Entry(
            "49:1", "49:18",
            AnchoringQueue.Origin.RECONSTRUCTION,
            AnchoringQueue.ItqanProtocol.LIGHT,
            0
        );

        assertEquals("49:1", entry.start);
        assertEquals("49:18", entry.end);
        assertEquals(AnchoringQueue.Origin.RECONSTRUCTION, entry.origin);
        assertEquals(AnchoringQueue.ItqanProtocol.LIGHT, entry.protocol);
        assertEquals(0, entry.failures);
    }

    @Test public void deferralMovesTheActuallyDisplayedPageThreePlacesLater() {
        List<AnchoringQueue.Entry> input = entries("A", "B", "C", "D", "E", "F");

        AnchoringQueue.Deferral result = AnchoringQueue.defer(input, 2, 3);

        assertEquals(Arrays.asList("D", "E", "F", "C", "A", "B"), starts(result.entries));
        assertEquals(0, result.nextIndex);
    }

    @Test public void deferralWrapsAtQueueEndInsteadOfRepeatingFailedPageImmediately() {
        List<AnchoringQueue.Entry> input = entries("A", "B", "C", "D", "E", "F");

        AnchoringQueue.Deferral result = AnchoringQueue.defer(input, 5, 3);

        assertEquals(Arrays.asList("A", "B", "C", "F", "D", "E"), starts(result.entries));
        assertEquals(0, result.nextIndex);
    }

    @Test public void inProgressRangeKeepsItsOwnProtocolWhenQueueHeadChanges() {
        List<AnchoringQueue.Entry> input = new ArrayList<>();
        input.add(new AnchoringQueue.Entry("A", "A",
            AnchoringQueue.Origin.RECONSTRUCTION, AnchoringQueue.ItqanProtocol.LIGHT, 0));
        input.add(new AnchoringQueue.Entry("B", "B",
            AnchoringQueue.Origin.PROMOTED, AnchoringQueue.ItqanProtocol.FULL, 0));

        AnchoringQueue.Entry resolved = AnchoringQueue.findByRange(input, "B", "B");

        assertEquals("B", resolved.start);
        assertEquals(AnchoringQueue.ItqanProtocol.FULL, resolved.protocol);
    }

    @Test public void thirdFailureSwitchesThatPageToFullAndDefersItThreePlaces() {
        List<AnchoringQueue.Entry> input = entries("A", "B", "C", "D", "E", "F");
        input.set(2, new AnchoringQueue.Entry("C", "C",
            AnchoringQueue.Origin.RECONSTRUCTION, AnchoringQueue.ItqanProtocol.LIGHT, 2));

        AnchoringQueue.Deferral result = AnchoringQueue.failAndDefer(input, 2, 3);

        assertEquals(Arrays.asList("D", "E", "F", "C", "A", "B"), starts(result.entries));
        AnchoringQueue.Entry failed = result.entries.get(3);
        assertEquals(3, failed.failures);
        assertEquals(AnchoringQueue.ItqanProtocol.FULL, failed.protocol);
        assertEquals(0, result.nextIndex);
    }

    private static List<AnchoringQueue.Entry> entries(String... starts) {
        ArrayList<AnchoringQueue.Entry> out = new ArrayList<>();
        for (String start : starts) {
            out.add(new AnchoringQueue.Entry(start, start,
                AnchoringQueue.Origin.RECONSTRUCTION,
                AnchoringQueue.ItqanProtocol.LIGHT, 0));
        }
        return out;
    }

    private static List<String> starts(List<AnchoringQueue.Entry> entries) {
        ArrayList<String> out = new ArrayList<>();
        for (AnchoringQueue.Entry entry : entries) out.add(entry.start);
        return out;
    }
}
