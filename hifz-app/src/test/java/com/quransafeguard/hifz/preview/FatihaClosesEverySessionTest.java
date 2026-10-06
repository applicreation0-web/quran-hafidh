package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.EligibleCorpus;
import com.quransafeguard.hifz.core.VerseRef;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** User decision: Al-Fātiḥa is read whole at the end of every session, outside the Révision loop. */
public final class FatihaClosesEverySessionTest {
    private static InMemoryPrefs store() {
        InMemoryPrefs store = new InMemoryPrefs();
        store.disk.put("itqanRanges", "[{\"start\":\"49:1\",\"end\":\"114:6\"}]");
        store.disk.put("promotedRanges", "[{\"start\":\"2:1\",\"end\":\"2:88\"}]");
        return store;
    }

    @Test public void revisionLoopWrapsToItsOwnStartWithoutAlFatiha() {
        HifzPrefs prefs = new HifzPrefs(store());
        EligibleCorpus passive = prefs.murajaahCorpus();
        assertEquals(new VerseRef(2, 1), passive.next(new VerseRef(114, 6)));
        assertFalse(passive.contains(new VerseRef(1, 1)));
        assertFalse(prefs.activeMurajaahCorpus().contains(new VerseRef(1, 7)));
    }

    @Test public void cursorsLeftInsideAlFatihaMoveToTheCorpusStart() {
        InMemoryPrefs store = store();
        store.disk.put("murajaahCursor", "1:3");
        store.disk.put("activeMurajaahCursor", "1:5");
        HifzPrefs prefs = new HifzPrefs(store);
        prefs.releaseRevisionCursorsFromFatiha();
        assertEquals(new VerseRef(2, 1), prefs.murajaahCursor());
        assertEquals(new VerseRef(2, 1), prefs.activeMurajaahCursor());
        prefs.releaseRevisionCursorsFromFatiha();
        assertEquals("idempotent", new VerseRef(2, 1), prefs.murajaahCursor());
    }

    @Test public void aCursorElsewhereIsNeverMoved() {
        InMemoryPrefs store = store();
        store.disk.put("murajaahCursor", "2:40");
        HifzPrefs prefs = new HifzPrefs(store);
        prefs.releaseRevisionCursorsFromFatiha();
        assertEquals(new VerseRef(2, 40), prefs.murajaahCursor());
    }

    @Test public void everyFinishedSessionShowsAlFatihaWhole() throws Exception {
        Path direct = Paths.get("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        Path file = Files.exists(direct) ? direct : Paths.get("..", direct.toString());
        String session = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        assertTrue("renderMode ends on Al-Fātiḥa once the session is done",
            session.contains("if (sessionCompleted && !awaitingValidation) showClosingFatiha();"));
        assertTrue("unmasked, no selection",
            session.contains("mushaf.show(page, Collections.emptyList(), Collections.emptyList(), 0, false);"));
        assertTrue("a session opened already done shows it too",
            session.contains("else if(!hasShown&&sessionCompleted&&!awaitingValidation)showClosingFatiha();"));
    }
}
