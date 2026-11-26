package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import org.json.JSONObject;
import org.json.JSONException;
import org.junit.Test;
import java.util.Arrays;
import java.util.BitSet;
import static org.junit.Assert.*;

public final class ActiveReviewCoverageTest {
    @Test public void onlyConfirmedPrefixCountsAcrossGapsAndWraps(){
        VerseRef a=new VerseRef(114,5),b=new VerseRef(114,6),c=new VerseRef(2,1),d=new VerseRef(2,2);
        BitSet covered=ActiveReviewCoverage.through(Arrays.asList(a,b,c,d),c);
        assertEquals(3,covered.cardinality());
        assertTrue(covered.get(GeometryRepository.ordinal(a)));
        assertTrue(covered.get(GeometryRepository.ordinal(c)));
        assertFalse(covered.get(GeometryRepository.ordinal(d)));
        assertEquals(covered,ActiveReviewCoverage.decode(ActiveReviewCoverage.encode(covered)));
        assertEquals(Arrays.asList(a, b, c), ActiveReviewCoverage.restoreTraversal(
            ActiveReviewCoverage.encode(covered), a, c));
        assertTrue(ActiveReviewCoverage.restoreTraversal(null, a, c).isEmpty());
    }

    @Test(expected=IllegalArgumentException.class)
    public void outOfPlanEndpointIsRejected(){
        ActiveReviewCoverage.through(Arrays.asList(new VerseRef(2,1)),new VerseRef(2,2));
    }

    @Test public void repeatedSmallCorpusDoesNotInventExtraUniqueCoverage(){
        VerseRef a=new VerseRef(2,1),b=new VerseRef(2,2);
        assertEquals(2,ActiveReviewCoverage.through(Arrays.asList(a,b,a,b),b).cardinality());
    }

    private JSONObject entry(String value)throws JSONException {
        return new JSONObject().put("t","s").put("v",value);
    }

    @Test public void oldBackupWithoutNewJournalsRemainsAccepted()throws Exception {
        HifzBackup.validateActiveReview(new JSONObject());
    }

    @Test(expected=JSONException.class)
    public void duplicatedQueuePagesRefuseRestore()throws Exception {
        HifzBackup.validateActiveReview(new JSONObject().put(ActiveReviewCorrectionStore.KEY_QUEUE,entry("[7,7]")));
    }

    @Test(expected=JSONException.class)
    public void coverageWithoutCorrespondingReceiptRefusesRestore()throws Exception {
        HifzBackup.validateActiveReview(new JSONObject().put(ActiveReviewCoverage.KEY+"|2026-11-26|MORNING",entry("8:11")));
    }

    @Test(expected=JSONException.class)
    public void revealJournalWithInvalidPageRefusesRestore() throws Exception {
        String key = ActiveReviewCorrectionStore.KEY_REVEALED + "|2026-11-26|MORNING";
        JSONObject journal = new JSONObject().put("date", "2026-11-26")
            .put("start", "2:1").put("pages", new org.json.JSONArray().put(605));
        HifzBackup.validateActiveReview(new JSONObject().put(key, entry(journal.toString())));
    }

    @Test(expected=JSONException.class)
    public void revealJournalFromAnotherSlotDateRefusesRestore() throws Exception {
        String key = ActiveReviewCorrectionStore.KEY_REVEALED + "|2026-11-26|MORNING";
        JSONObject journal = new JSONObject().put("date", "2026-11-25")
            .put("start", "2:1").put("pages", new org.json.JSONArray().put(2));
        HifzBackup.validateActiveReview(new JSONObject().put(key, entry(journal.toString())));
    }

    @Test public void validCoverageWithReceiptAccepted()throws Exception {
        String suffix="|2026-11-26|MORNING";
        HifzBackup.validateActiveReview(new JSONObject()
            .put("activeReviewReceiptV1"+suffix,entry("preuve"))
            .put(ActiveReviewCoverage.KEY+suffix,entry("8:11")));
    }
}
