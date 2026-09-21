package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public final class AstraFixPolicyTest {
    @Test public void newPromotionIsInsertedBeforePendingReconstruction() {
        AnchoringQueue.Entry reconstruction = new AnchoringQueue.Entry(
            "49:1", "49:5", AnchoringQueue.Origin.RECONSTRUCTION, AnchoringQueue.ItqanProtocol.LIGHT, 0);
        AnchoringQueue.Entry promoted = new AnchoringQueue.Entry(
            "2:75", "2:82", AnchoringQueue.Origin.PROMOTED, AnchoringQueue.ItqanProtocol.FULL, 0);

        List<AnchoringQueue.Entry> merged = AnchoringQueue.mergeWithPromotionPriority(
            Collections.singletonList(reconstruction), 0, false, Collections.singletonList(promoted));

        assertEquals(2, merged.size());
        assertEquals(AnchoringQueue.Origin.PROMOTED, merged.get(0).origin);
        assertEquals(AnchoringQueue.Origin.RECONSTRUCTION, merged.get(1).origin);
    }

    @Test public void inProgressFractionatedEntryIsNeverPreemptedByNewPromotion() {
        AnchoringQueue.Entry current = new AnchoringQueue.Entry(
            "49:1", "49:5", AnchoringQueue.Origin.RECONSTRUCTION, AnchoringQueue.ItqanProtocol.LIGHT, 0);
        AnchoringQueue.Entry promoted = new AnchoringQueue.Entry(
            "2:75", "2:82", AnchoringQueue.Origin.PROMOTED, AnchoringQueue.ItqanProtocol.FULL, 0);

        List<AnchoringQueue.Entry> merged = AnchoringQueue.mergeWithPromotionPriority(
            Collections.singletonList(current), 0, true, Arrays.asList(promoted));

        assertEquals(AnchoringQueue.Origin.RECONSTRUCTION, merged.get(0).origin);
        assertEquals(AnchoringQueue.Origin.PROMOTED, merged.get(1).origin);
    }
}
