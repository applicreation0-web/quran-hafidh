package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class SemanticQuizPolicyTest {
    @Test public void quizMixNeverBecomesSemanticAnchorsOnly() {
        List<SemanticQuizPolicy.Type> types = SemanticQuizPolicy.balancedTypes(6);
        assertEquals(6, types.size());
        assertTrue(types.contains(SemanticQuizPolicy.Type.SEMANTIC_RESTART));
        assertTrue(types.contains(SemanticQuizPolicy.Type.NEXT_PASSAGE));
        assertTrue(types.contains(SemanticQuizPolicy.Type.RANDOM_RESTART));
    }

    @Test public void randomRestartAvoidsSemanticStartsWhenAnotherVerseExists() {
        VerseRef a = new VerseRef(49, 11);
        VerseRef b = new VerseRef(49, 12);
        VerseRef c = new VerseRef(49, 13);
        List<VerseRef> candidates = SemanticQuizPolicy.randomRestartCandidates(
            Arrays.asList(a, b, c), Arrays.asList(a, c));
        assertEquals(Collections.singletonList(b), candidates);
        assertFalse(candidates.contains(a));
        assertFalse(candidates.contains(c));
    }

    @Test public void randomRestartFallsBackInsteadOfReturningNoQuestion() {
        VerseRef only = new VerseRef(50, 15);
        List<VerseRef> candidates = SemanticQuizPolicy.randomRestartCandidates(
            Collections.singletonList(only), Collections.singletonList(only));
        assertEquals(Collections.singletonList(only), candidates);
    }
}
