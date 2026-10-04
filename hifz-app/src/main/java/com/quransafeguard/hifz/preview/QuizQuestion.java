package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

/** One self-assessed retrieval prompt. It carries no progression state and never mutates Hifz. */
final class QuizQuestion {
    enum Type { CONTINUE, PREVIOUS }

    final Type type;
    final VerseRef prompt;
    final VerseRef expected;
    final int promptPage;
    final int expectedPage;

    QuizQuestion(Type type, VerseRef prompt, VerseRef expected, int promptPage, int expectedPage) {
        if (type == null || prompt == null || expected == null) throw new IllegalArgumentException("quiz question");
        this.type = type;
        this.prompt = prompt;
        this.expected = expected;
        this.promptPage = promptPage;
        this.expectedPage = expectedPage;
    }

    String instruction() {
        return type == Type.PREVIOUS ? "Récitez le verset qui précède celui-ci." : "Récitez la suite de ce verset.";
    }

}
