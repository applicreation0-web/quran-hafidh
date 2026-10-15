package com.quransafeguard.hifz.preview;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;

/** No editorial titles or copyrighted commentary are included in these fixtures. */
public final class BaqaraAyahThemeTitlesEditorialGateTest {
    private static String draft(boolean spoofApproval) throws Exception {
        JSONArray passages = new JSONArray();
        for (int i = 1; i <= 142; i++) {
            JSONObject row = new JSONObject()
                .put("start", i)
                .put("end", i == 142 ? 286 : i)
                .put("title", "Synthetic navigation " + i);
            if (spoofApproval) {
                row.put("verified", true);
                row.put("editorial_status", "APPROVED");
            }
            passages.put(row);
        }
        return new JSONObject().put("surah", 2).put("source", "Ayah Themes")
            .put("passages", passages).toString();
    }

    @Test public void draftPresenceNeverImpliesEditorialApproval() throws Exception {
        List<BaqaraAyahThemeTitles.Passage> parsed =
            BaqaraAyahThemeTitles.parseUnapprovedDraft(draft(true));
        assertEquals(142, parsed.size());
        assertEquals(1, parsed.get(0).first);
        assertEquals(286, parsed.get(parsed.size() - 1).last);
        for (BaqaraAyahThemeTitles.Passage p : parsed)
            assertFalse("Unreviewed titles must be visibly pending", p.verified);
    }

    @Test public void privateDraftRejectsWrongSource() throws Exception {
        String invalid = draft(false).replace("\"Ayah Themes\"", "\"Unverified source\"");
        try {
            BaqaraAyahThemeTitles.parseUnapprovedDraft(invalid);
            fail("Wrong source was accepted");
        } catch (IllegalStateException expected) {
            assertEquals("Wrong English navigation corpus", expected.getMessage());
        }
    }

    @Test public void structurallyValidDraftRemainsUnapproved() throws Exception {
        List<BaqaraAyahThemeTitles.Passage> parsed =
            BaqaraAyahThemeTitles.parseUnapprovedDraft(draft(false));
        assertEquals(142, parsed.size());
        for (BaqaraAyahThemeTitles.Passage p : parsed) assertFalse(p.verified);
    }
}
