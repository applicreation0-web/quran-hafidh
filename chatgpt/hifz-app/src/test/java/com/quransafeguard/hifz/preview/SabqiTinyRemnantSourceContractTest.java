package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Reported directly: a fixed 5-line block (PreviewConfig.SABQI_LINES, frozen) has no smaller
 * variant, so renderSabqi() dead-ended forever the moment fewer than 5 lines remained before
 * sabqiEnd() — "Apprentissage · fin de plage" / "X ligne(s) restante(s) · bloc requis : 5" — with
 * no way past a 1-4 line remnant short of manually extending sabqiEnd() in Settings. Mirrors the
 * Itqan tiny-fragment fast path already built for the exact same class of problem: the remnant is
 * credited straight to Acquis instead, skipping the full repetition drill.
 */
public final class SabqiTinyRemnantSourceContractTest {
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

    @Test public void tinyRemnantIsCreditedInsteadOfDeadEnding() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String renderSabqi = method(session, "private void renderSabqi() {", "private void creditTinySabqiRemnant(");
        assertTrue("the remnant branch must hand off to the new credit path, not dead-end",
            renderSabqi.contains("creditTinySabqiRemnant(cursor, endLimit, today);"));
        assertFalse("the old permanent dead-end must be gone", session.contains("bloc requis : 5"));
        assertFalse("the old permanent dead-end label must be gone", session.contains("Apprentissage · fin de plage"));

        String credit = method(session, "private void creditTinySabqiRemnant(", "\n    }");
        assertTrue("must call the new HifzPrefs fast path", credit.contains("prefs.completeSabqiTinyBlockV6(lineIds, endLimit + 1, date, label)"));
        assertTrue("a failed commit must surface an error, not silently pretend success",
            credit.contains("onError(\"Impossible d’enregistrer l’Apprentissage.\");"));
    }

    @Test public void hifzPrefsTinyBlockCreditsStraightToAcquisSkippingTheDrillAndSnowball() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        String method = method(prefs, "boolean completeSabqiTinyBlockV6(", "\n    }");
        assertTrue("must reject a remnant that could fit a real 5-line block (nothing to guard against otherwise)",
            method.contains("lineIds.size() >= PreviewConfig.SABQI_LINES"));
        assertTrue("must move straight to ACQUIRED, mirroring completeItqanTinyBlockV6's fast path",
            method.contains("acquired.add(lineId);"));
        assertFalse("must skip the weekly Renforcement snowball entirely — there is nothing left to review",
            method.contains("weeklySnowballAppendEntries"));
        assertFalse("must skip the recent-Sabqi review queue — the remnant is already Acquis, not pending review",
            method.contains("recentSabqi()"));
        assertTrue("must still advance the cursor past the credited remnant",
            method.contains("putInt(\"sabqiLineCursor\", nextLineCursor)"));
    }
}
