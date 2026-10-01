package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Whole-Mushaf progress map (visual mockup approved before implementation): one cell per page,
 * status told apart by fill PATTERN, never color, since hue carries no meaning on an e-ink BOOX
 * screen and color changes ghost. Status must be read from HifzPrefs' own live per-line schema6
 * progression state (progressionSnapshotV6), the same state every session-completion method for
 * both the Sabqi/Renforcement and Itqan/Stabilisation tracks updates, so this view can never lag
 * behind what the learner actually just did.
 */
public final class ProgressMapSourceContractTest {
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

    private static int countOccurrences(String haystack, String needle) {
        int count = 0, from = 0;
        while (true) {
            int at = haystack.indexOf(needle, from);
            if (at < 0) return count;
            count++;
            from = at + needle.length();
        }
    }

    @Test public void activityIsRegisteredAndReachableFromHome() throws Exception {
        String manifest = read("hifz-app/src/main/AndroidManifest.xml");
        assertTrue("the screen must actually be declared, or Android refuses to launch it",
            manifest.contains("<activity android:name=\".ProgressMapActivity\" android:exported=\"false\" />"));

        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        assertTrue("Home must offer a direct way in, alongside Lecture/Mémoriser/Paramètres",
            main.contains("Ui.cardAction(this, \"\", \"Progression\", v -> startActivity(new Intent(this, ProgressMapActivity.class)));"));
        assertTrue("it must gate behind geometry loading like the other three home cards",
            main.contains("geometryActions.add(progress);"));

        String ui = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/Ui.java");
        assertTrue("the card needs its own icon mapping or it renders with neither icon nor text",
            ui.contains("if (s.contains(\"progression\")) return R.drawable.ic_ui_progress_map;"));
    }

    @Test public void statusIsPatternedNeverColoredAndFourStatusesExist() throws Exception {
        String grid = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/ProgressGridView.java");
        assertTrue("must define exactly the four statuses the design review approved",
            grid.contains("static final int VIDE = 0;") && grid.contains("static final int APPRENTISSAGE = 1;")
                && grid.contains("static final int STABILISER = 2;") && grid.contains("static final int ACQUIS = 3;"));
        String drawCell = method(grid, "static void drawCell(Canvas canvas, RectF rect, int status, Paint fill, Paint stroke) {", "\n}");
        assertTrue("Acquis must be a solid fill", drawCell.contains("canvas.drawRoundRect(rect, radius, radius, fill);"));
        assertTrue("À stabiliser must be a diagonal hatch, not a fill", drawCell.contains("canvas.drawLine(x, rect.bottom, x + rect.height(), rect.top, fill);"));
        assertTrue("En apprentissage must be dots, not a fill", drawCell.contains("canvas.drawCircle(x, y, dotRadius, fill);"));
        int totalSetColor = countOccurrences(grid, ".setColor(");
        int inkOrLineSetColor = countOccurrences(grid, ".setColor(Ui.INK)") + countOccurrences(grid, ".setColor(Ui.LINE)");
        assertTrue("every Paint.setColor call must use the shared ink/line tokens — anything else "
                + "would smuggle a hue-based distinction back in, meaningless on e-ink anyway",
            totalSetColor > 0 && totalSetColor == inkOrLineSetColor);
    }

    /**
     * Reported directly (twice): crediting an Itqan/Stabilisation line never moved anything on
     * this screen while it read historical range-based promotion buckets that only the Sabqi/
     * Renforcement track ever writes to. Switching straight to per-line STABILIZED state fixed
     * that but broke something else: a declared Itqan/Ancrage range starts every one of its lines
     * in state NONE (completeStabilizationBlockV6's own comment confirms this is Ancrage material's
     * normal starting state), so a huge declared-but-untouched range showed as "pas commencé"
     * instead of "à stabiliser" — indistinguishable from a page never declared for any track.
     * "À stabiliser" must therefore be declared-corpus membership (effectiveItqanRanges) combined
     * with "not yet ACQUIRED", not a specific per-line sub-state.
     */
    @Test public void pageStatusCombinesLiveAcquiredStateWithDeclaredItqanCorpusMembership() throws Exception {
        String activity = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/ProgressMapActivity.java");
        assertTrue("must read HifzPrefs' own live per-line state snapshot for Acquis, not a historical range bucket",
            activity.contains("HifzPrefs.ProgressionSnapshot progression = prefs.progressionSnapshotV6();"));
        assertTrue("à stabiliser must be declared Itqan-corpus membership (base range + every Sabqi-fed promotion)",
            activity.contains("Set<String> itqanEligible = CorpusLinePolicy.ownedLineIds(prefs.effectiveItqanRanges(), allLines);"));
        assertFalse("must not fall back to the Sabqi-only pending-promotion bucket for à stabiliser — "
                + "it never includes a declared-but-untouched Ancrage range still in state NONE",
            activity.contains("prefs.unconsolidatedPromotedRanges()") || activity.contains("prefs.consolidatedPromotedRanges()")
                || activity.contains("progression.stabilized"));
        String computeStatuses = method(activity, "private int[] computeStatuses() {", "@Override protected void onDestroy()");
        assertTrue("ACQUIRED lines must show Acquis", computeStatuses.contains("progression.acquired.contains(lineId)) anyAcquis = true;"));
        assertTrue("declared-Itqan-corpus lines not yet Acquis must show à stabiliser, whatever their NONE/LEARNED/STABILIZED sub-state",
            computeStatuses.contains("itqanEligible.contains(lineId)) anyStabiliser = true;"));
        assertTrue("Sabqi's own LEARNED lines must show en apprentissage", computeStatuses.contains("progression.learned.contains(lineId)) anyApprentissage = true;"));
        assertTrue("Acquis must be checked before à stabiliser so a graduated line never gets stuck hatched",
            computeStatuses.indexOf("progression.acquired.contains(lineId)) anyAcquis = true;") <
                computeStatuses.indexOf("itqanEligible.contains(lineId)) anyStabiliser = true;"));
        assertTrue("à stabiliser must win over every other status on a mixed page",
            computeStatuses.indexOf("anyStabiliser ? ProgressGridView.STABILISER") <
                computeStatuses.indexOf("anyApprentissage ? ProgressGridView.APPRENTISSAGE"));
        assertTrue("en apprentissage must win over acquis on a mixed page (still-incomplete work stays visible)",
            computeStatuses.indexOf("anyApprentissage ? ProgressGridView.APPRENTISSAGE") <
                computeStatuses.indexOf("anyAcquis ? ProgressGridView.ACQUIS"));
    }

    @Test public void progressionSnapshotExposesTheSameThreeSetsEverySessionCompletionMethodUpdates() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        String snapshot = method(prefs,
            "public ProgressionSnapshot progressionSnapshotV6() {", "\n    }");
        assertTrue("must read the exact same three keys every completion method (learning, "
                + "Stabilisation, Consolidation/Renforcement, and the tiny-Itqan-fragment fast path) writes",
            snapshot.contains("v6LineIdSet(\"v6LearnedLineIds\")") && snapshot.contains("v6LineIdSet(\"v6StabilizedLineIds\")")
                && snapshot.contains("v6LineIdSet(\"v6AcquiredCreditLineIds\")"));
        assertTrue("must require schema6 state like every other progression accessor",
            snapshot.contains("requireSchema6ProgressionState();"));
    }

    @Test public void liveEtaEstimatesSitAboveTheGridUsingTheSameBucketsDiagnosticUses() throws Exception {
        String activity = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/ProgressMapActivity.java");
        assertTrue("the two estimates approved in the mockup must both be wired in, in that order",
            activity.indexOf("apprentissageEtaValue = etaBox(root, \"Estimation fin Apprentissage\");") <
                activity.indexOf("stabilisationEtaValue = etaBox(root, \"Estimation fin Stabilisation\");"));
        assertTrue("Apprentissage's estimate must use its own real weekly pace (SABQI_LINES × "
                + "learningDaysPerWeek), never Stabilisation's fixed one",
            activity.contains("weeksEtaSummary(prefs.sabqiLinesRemaining(geometry),\n"
                + "                    PreviewConfig.SABQI_LINES * prefs.learningDaysPerWeek());"));
        assertTrue("Stabilisation's estimate must reuse the exact same computation Diagnostic uses",
            activity.contains("weeksEtaSummary(prefs.stabilizationLinesRemaining(geometry),\n"
                + "                    PreviewConfig.STABILIZATION_WEEKLY_LINES);"));
    }

    @Test public void backgroundComputationFailureIsVisibleNotSilent() throws Exception {
        String activity = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/ProgressMapActivity.java");
        assertTrue("progressionSnapshotV6()/requireSchema6ProgressionState() throws when state is "
                + "not yet schema6 or corrupt (confirmed in HifzPrefs) — a background thread must not "
                + "silently die on that, whether it's the status scan or either live ETA computation that throws",
            activity.contains("try {\n"
                + "                statuses = computeStatuses();\n"
                + "                apprentissageEta = weeksEtaSummary(prefs.sabqiLinesRemaining(geometry),\n"
                + "                    PreviewConfig.SABQI_LINES * prefs.learningDaysPerWeek());\n"
                + "                stabilisationEta = weeksEtaSummary(prefs.stabilizationLinesRemaining(geometry),\n"
                + "                    PreviewConfig.STABILIZATION_WEEKLY_LINES);\n"
                + "            } catch (RuntimeException corruptOrUnconfiguredState) {"));
        assertTrue("a failed computation must tell the learner, not just leave the grid blank forever",
            activity.contains("Toast.makeText(this, \"Progression indisponible · ouvrez Diagnostic si le problème persiste.\", Toast.LENGTH_LONG).show();"));
        assertTrue("posted UI updates must not touch a destroyed Activity's views (no configChanges "
                + "is declared for this screen, so rotation recreates it while the background scan may "
                + "still be in flight)",
            countOccurrences(activity, "if (isFinishing() || isDestroyed()) return;") == 2);
    }

    @Test public void tappingAPageOpensLectureThere() throws Exception {
        String activity = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/ProgressMapActivity.java");
        assertTrue("a tapped cell must jump Lecture to that exact page",
            activity.contains("intent.putExtra(StudyReaderActivity.EXTRA_JUMP_PAGE, page);"));
    }

    @Test public void touchHandlingClaimsActionDownSoActionUpIsEverDelivered() throws Exception {
        String grid = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/ProgressGridView.java");
        String onTouchEvent = method(grid, "public boolean onTouchEvent(MotionEvent event) {", "\n    }");
        assertTrue("a View returning false for ACTION_DOWN never receives that gesture's ACTION_UP "
                + "(Android stops delivering it entirely) — ACTION_DOWN must be claimed with true, or "
                + "tapping a cell can never navigate anywhere",
            onTouchEvent.contains("if (event.getAction() == MotionEvent.ACTION_DOWN) return true;"));
        assertTrue("the ACTION_DOWN claim must come before the ACTION_UP handling it exists to unlock",
            onTouchEvent.indexOf("MotionEvent.ACTION_DOWN") < onTouchEvent.indexOf("MotionEvent.ACTION_UP"));
    }
}
