package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * StudyReaderActivity previously had no way to open at a specific page/verse — it always resumed
 * whatever page was last read (SharedPreferences "hifz_study"/"page"), with no Intent extras read
 * at all. The weak-verses list needs to jump straight to a flagged verse's location, so
 * StudyReaderActivity gained two extras: EXTRA_JUMP_PAGE (which page to open) and EXTRA_JUMP_VERSE
 * (which verse on it to pre-select/highlight, one-shot — cleared after the first onReady()).
 */
public final class StudyReaderJumpToVerseSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    private static String method(String source, String start, String end) {
        int a = source.indexOf(start);
        int b = source.indexOf(end, a + start.length());
        if (a < 0 || b < 0 || b <= a) throw new IllegalStateException("Method boundary missing: " + start);
        return source.substring(a, b);
    }

    @Test public void onCreateReadsTheJumpExtrasBeforeFallingBackToTheRememberedPage() throws Exception {
        String study = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java");
        assertTrue("must expose the page extra's key as a public constant callers can reference",
            study.contains("public static final String EXTRA_JUMP_PAGE = \"jumpPage\";"));
        assertTrue("must expose the verse extra's key as a public constant callers can reference",
            study.contains("public static final String EXTRA_JUMP_VERSE = \"jumpVerse\";"));

        String onCreate = method(study, "@Override protected void onCreate(Bundle state) {", "private void setPage(int requested) {");
        assertTrue("an explicit jump page must win over the remembered last-read page",
            onCreate.contains("page = jumpPage >= 1 && jumpPage <= 604\n            ? jumpPage : getSharedPreferences(\"hifz_study\", MODE_PRIVATE).getInt(\"page\", 1);"));
        assertTrue("a malformed jump-verse extra must not crash the screen, just be ignored",
            onCreate.contains("catch (RuntimeException malformed) { /* ignore */ }"));
    }

    @Test public void onReadyHighlightsThePendingVerseOnceThenClearsIt() throws Exception {
        String study = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java");
        String onReady = method(study, "@Override public void onReady() {", "@Override public void onError(");
        assertTrue("the pending verse must be pre-selected in the very first show() after boot",
            onReady.contains("mushaf.show(page, Collections.singletonList(pendingJumpVerse), Collections.emptyList(), 0);"));
        assertTrue("it must be one-shot: a later page swipe must not keep re-highlighting it",
            onReady.contains("pendingJumpVerse = null;"));
        assertTrue("without a jump verse, boot must behave exactly as before",
            onReady.contains("mushaf.show(page, Collections.emptyList(), Collections.emptyList(), 0);"));
    }
}
