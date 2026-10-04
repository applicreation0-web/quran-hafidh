package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Found during the deep audit requested right after the rotation-reset bug: Settings and
 * Diagnostic showed "Position Stabilisation" from the legacy itqanCursor field, which is only
 * ever written by the Settings "reposition on invalid range" repair action — never by a real
 * Stabilisation session completion. Since P4's perpetual rotation replaced cursor-based unit
 * selection, this display had been silently disconnected from the position actually driving
 * Stabilisation: the learner could be shown a number nothing real ever updates. Fixed by reading
 * HifzPrefs.itqanRotationLiveCursor (which bootstraps and returns the real rotation position)
 * everywhere the app claims to show the current Stabilisation position.
 */
public final class ItqanRotationLiveCursorDisplaySourceContractTest {
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

    @Test public void hifzPrefsExposesABootstrapAwareLiveCursorForDisplay() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        String m = method(prefs, "public VerseRef itqanRotationLiveCursor(GeometryRepository geometry) {", "\n    }");
        assertTrue("must run the same one-time bootstrap currentAnchoringEntry relies on",
            m.contains("ensureItqanRotationBootstrapped(geometry);"));
        assertTrue("must return the real rotation cursor, not the legacy vestigial one",
            m.contains("return itqanRotationState().cursor;"));
    }

    @Test public void settingsNoLongerDisplaysTheStaleLegacyCursor() throws Exception {
        String settings = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        assertTrue("Settings status line must read the live rotation position",
            settings.contains("prefs.itqanRotationLiveCursor(geometry)"));
        assertTrue("Diagnostic must read the live rotation position too",
            settings.contains("\"\\nPosition Stabilisation : \"+prefs.itqanRotationLiveCursor(geometry)"));
        assertFalse("neither display site may still read the legacy field directly",
            settings.contains("\"Position Stabilisation · \"+prefs.itqanCursor()")
                || settings.contains("\"\\nPosition Stabilisation : \"+prefs.itqanCursor()"));
    }
}
