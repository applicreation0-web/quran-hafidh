package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * Whole-Mushaf progress map (visual mockup approved before implementation): one cell per page,
 * status told apart by fill PATTERN, never color, since hue carries no meaning on an e-ink BOOX
 * screen and color changes ghost. Status must be read from the exact same public corpus buckets
 * Diagnostic already reports, so this view can never show a number Diagnostic would contradict.
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

    @Test public void pageStatusPriorityMatchesTheSameBucketsDiagnosticReports() throws Exception {
        String activity = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/ProgressMapActivity.java");
        assertTrue("Acquis must combine the declared base with the truly-consolidated bucket, "
                + "exactly like activeMurajaahCorpus does",
            activity.contains("List<VerseRange> acquis = new ArrayList<>(prefs.itqanRanges());")
                && activity.contains("acquis.addAll(prefs.consolidatedPromotedRanges());"));
        assertTrue("à stabiliser must read the same bucket Diagnostic labels \"À stabiliser\"",
            activity.contains("List<VerseRange> stabiliser = prefs.unconsolidatedPromotedRanges();"));
        assertTrue("en apprentissage must be bounded above by the real Sabqi front, not guessed",
            activity.contains("sabqiEnd = prefs.sabqiEnd();"));
        String computeStatuses = method(activity, "private int[] computeStatuses() {", "private static boolean containsVerse(");
        assertTrue("en apprentissage must also be bounded BELOW by sabqiStart — otherwise a verse "
                + "printed before the Apprentissage walk even begins (Al-Fatiha, under the default "
                + "2:75 start) gets wrongly counted just because its ordinal sits below sabqiEnd",
            computeStatuses.contains("if (ordinal >= sabqiStartOrdinal && ordinal <= sabqiEndOrdinal) anyApprentissage = true;"));
        assertTrue("à stabiliser must win over every other status on a mixed page",
            computeStatuses.indexOf("anyStabiliser ? ProgressGridView.STABILISER") <
                computeStatuses.indexOf("anyApprentissage ? ProgressGridView.APPRENTISSAGE"));
        assertTrue("en apprentissage must win over acquis on a mixed page (still-incomplete work stays visible)",
            computeStatuses.indexOf("anyApprentissage ? ProgressGridView.APPRENTISSAGE") <
                computeStatuses.indexOf("anyAcquis ? ProgressGridView.ACQUIS"));
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
        assertTrue("itqanRanges()/parseRanges() throws when nothing is configured yet or state is "
                + "corrupt (confirmed in HifzPrefs) — a background thread must not silently die on that, "
                + "whether it's the status scan or either live ETA computation that throws",
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
