package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import org.json.JSONObject;
import org.junit.Test;
import java.util.Collections;
import static org.junit.Assert.*;

/** Damaged imported checkpoints must never convert assistance into clean recall. */
public final class Independent11710IntegrityTest {
    @Test public void malformedAssistanceCannotBecomeUnassistedRecall() throws Exception {
        for (Object invalid : new Object[]{"false", "true", "damaged", 0, 1, JSONObject.NULL}) {
            InMemoryPrefs disk = new InMemoryPrefs();
            ActiveReviewCorrectionStore store = new ActiveReviewCorrectionStore(disk);
            assertTrue(store.begin("2026-11-02", new VerseRef(30,37), new VerseRef(30,38),
                12000L, Collections.singletonList(408), 408));
            assertTrue(store.pageReread(store.pending()));
            assertTrue(store.revealDuringControl(store.pending()));
            JSONObject state = new JSONObject(disk.getString("activeReviewCorrectionV2", ""));
            state.put("assisted", invalid);
            String original = state.toString();
            disk.edit().putString("activeReviewCorrectionV2", original).commit();
            try { store.pending(); fail("Malformed assistance accepted: " + invalid); }
            catch (IllegalStateException expected) {
                assertEquals(original, disk.getString("activeReviewCorrectionV2", ""));
            }
        }
    }
    @Test public void absentAssistanceCannotEraseARecordedReveal() throws Exception {
        InMemoryPrefs disk = new InMemoryPrefs();
        ActiveReviewCorrectionStore store = new ActiveReviewCorrectionStore(disk);
        assertTrue(store.begin("2026-11-02", new VerseRef(30,37), new VerseRef(30,38),
            12000L, Collections.singletonList(408), 408));
        assertTrue(store.pageReread(store.pending()));
        assertTrue(store.revealDuringControl(store.pending()));
        JSONObject state = new JSONObject(disk.getString("activeReviewCorrectionV2", ""));
        state.remove("assisted");
        String original = state.toString();
        disk.edit().putString("activeReviewCorrectionV2", original).commit();
        try { store.pending(); fail("Missing assistance accepted as a clean recall"); }
        catch (IllegalStateException expected) {
            assertEquals(original, disk.getString("activeReviewCorrectionV2", ""));
        }
    }
}
