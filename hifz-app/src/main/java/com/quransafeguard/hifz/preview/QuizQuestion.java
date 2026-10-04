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
        if (type == Type.PREVIOUS) {
            return expectedPage == promptPage
                ? "Récitez le verset qui précède le verset surligné."
                : "Récitez le verset qui précède le verset surligné (page précédente).";
        }
        return "Récitez la suite du verset surligné.";
    }

}
