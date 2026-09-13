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
            AnchoringQueue.Protocol.LIGHT,
            0
        );

        assertEquals("49:1", entry.start);
        assertEquals("49:18", entry.end);
        assertEquals(AnchoringQueue.Origin.RECONSTRUCTION, entry.origin);
        assertEquals(AnchoringQueue.Protocol.LIGHT, entry.protocol);
        assertEquals(0, entry.failures);
    }

    @Test public void deferralMovesTheActuallyDisplayedPageThreePlacesLater() {
        List<AnchoringQueue.Entry> input = entries("A", "B", "C", "D", "E", "F");

        AnchoringQueue.Deferral result = AnchoringQueue.defer(input, 2, 3);

        assertEquals(Arrays.asList("A", "B", "D", "E", "F", "C"), starts(result.entries));
        assertEquals(2, result.nextIndex);
    }

    private static List<AnchoringQueue.Entry> entries(String... starts) {
        ArrayList<AnchoringQueue.Entry> out = new ArrayList<>();
        for (String start : starts) {
            out.add(new AnchoringQueue.Entry(start, start,
                AnchoringQueue.Origin.RECONSTRUCTION,
                AnchoringQueue.Protocol.LIGHT, 0));
        }
        return out;
    }

    private static List<String> starts(List<AnchoringQueue.Entry> entries) {
        ArrayList<String> out = new ArrayList<>();
        for (AnchoringQueue.Entry entry : entries) out.add(entry.start);
        return out;
    }
}
