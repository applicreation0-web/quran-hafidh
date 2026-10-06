package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.EligibleCorpus;
import com.quransafeguard.hifz.core.VerseRef;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** User decision: at the end of the Révision corpus the cycle comes back to Al-Fātiḥa. */
public final class RevisionOpensWithFatihaTest {
    private static HifzPrefs prefs() {
        InMemoryPrefs store = new InMemoryPrefs();
        store.disk.put("itqanRanges", "[{\"start\":\"49:1\",\"end\":\"114:6\"}]");
        store.disk.put("promotedRanges", "[{\"start\":\"2:1\",\"end\":\"2:88\"}]");
        return new HifzPrefs(store);
    }

    @Test public void passiveRevisionWrapsFromAnNasToAlFatiha() {
        EligibleCorpus corpus = prefs().murajaahCorpus();
        assertEquals(new VerseRef(1, 1), corpus.next(new VerseRef(114, 6)));
        assertEquals("Al-Fātiḥa flows straight into Al-Baqara", new VerseRef(2, 1), corpus.next(new VerseRef(1, 7)));
        assertFalse("nothing invented past the real corpus", corpus.contains(new VerseRef(2, 89)));
    }

    @Test public void activeRevisionWrapsToAlFatihaToo() {
        EligibleCorpus corpus = prefs().activeMurajaahCorpus();
        assertEquals(new VerseRef(1, 1), corpus.next(new VerseRef(114, 6)));
        assertTrue(corpus.contains(new VerseRef(1, 7)));
    }
}
