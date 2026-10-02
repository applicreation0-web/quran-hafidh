package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pure semantic-quiz planning only. No UI or progression state is modified here.
 *
 * It deliberately keeps semantic restarts separate from random restarts so a learner cannot become
 * dependent on semantic anchors as the only valid recall entry points.
 */
final class SemanticQuizPolicy {
    enum Type {
        SEMANTIC_RESTART,
        NEXT_PASSAGE,
        RANDOM_RESTART
    }

    static final class Question {
        final Type type;
        final String prompt;
        final String passageId;
        final VerseRef answerStart;

        Question(Type type, String prompt, String passageId, VerseRef answerStart) {
            this.type = type;
            this.prompt = prompt;
            this.passageId = passageId;
            this.answerStart = answerStart;
        }
    }

    private SemanticQuizPolicy() {}

    static List<Type> balancedTypes(int count) {
        if (count <= 0) return Collections.emptyList();
        ArrayList<Type> types = new ArrayList<>();
        Type[] cycle = { Type.SEMANTIC_RESTART, Type.RANDOM_RESTART, Type.NEXT_PASSAGE };
        for (int i = 0; i < count; i++) types.add(cycle[i % cycle.length]);
        return Collections.unmodifiableList(types);
    }

    /**
     * Random recall must not systematically begin on a semantic anchor. The caller provides the
     * candidate verses from the learned corpus; anchor starts are excluded when another candidate
     * exists, otherwise the full candidate set is returned as a safe fallback.
     */
    static List<VerseRef> randomRestartCandidates(List<VerseRef> learned, List<VerseRef> semanticStarts) {
        if (learned == null || learned.isEmpty()) return Collections.emptyList();
        ArrayList<VerseRef> filtered = new ArrayList<>();
        for (VerseRef verse : learned) {
            if (semanticStarts == null || !semanticStarts.contains(verse)) filtered.add(verse);
        }
        if (filtered.isEmpty()) filtered.addAll(learned);
        return Collections.unmodifiableList(filtered);
    }
}
