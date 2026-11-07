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
            main.contains("primaryTile(practiceRow, \"Progression\", \"Progression\",")
            && main.contains("new Intent(this, ProgressMapActivity.class)"));
        assertTrue("it must gate behind geometry loading like the other home destinations",
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
        int inkOrLineSetColor = countOccurrences(grid, ".setColor(Ui.INK)") +
            countOccurrences(grid, ".setColor(Ui.LINE)");
        assertTrue("Only shared monochrome ink/line tokens are allowed",
            totalSetColor > 0 && totalSetColor == inkOrLineSetColor);
        assertFalse("Page numbers are not shown inside the Juz page grid",
            grid.contains("canvas.drawText(Integer.toString(page)"));
    }

    @Test public void pageAndJuzUseOneEarnedProgressionSnapshot() throws Exception {
        String activity = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/ProgressMapActivity.java");
        assertTrue(activity.contains("statuses = computeStatuses(geometry, progression);"));
        assertTrue(activity.contains("JuzProgressSummary.summarize(geometry, progression)"));
        assertFalse(activity.contains("effectiveItqanRanges()"));
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

    @Test public void progressionHasNoRedundantEtaLineCounts() throws Exception {
        String activity = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/ProgressMapActivity.java");
        assertFalse("No future-date ETA or lines-remaining text in minimalist Juz progress",
            activity.contains("apprentissageEtaValue") || activity.contains("stabilisationEtaValue")
            || activity.contains("weeksEtaSummary(")
            || activity.contains("etaBox(root,"));
    }

    @Test public void backgroundComputationFailureIsVisibleNotSilent() throws Exception {
        String activity = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/ProgressMapActivity.java");
        assertTrue(activity.contains("HifzPrefs.ProgressionSnapshot progression = prefs.progressionSnapshotV6();"));
        assertTrue(activity.contains("catch (RuntimeException corruptOrUnconfiguredState)"));
        assertTrue("a failed computation must tell the learner, not just leave the grid blank forever",
            activity.contains("Toast.makeText(this, \"Progression indisponible · ouvrez Diagnostic si le problème persiste.\", Toast.LENGTH_LONG).show();"));
        assertTrue("posted UI updates must not touch a destroyed Activity's views (no configChanges "
                + "is declared for this screen, so rotation recreates it while the background scan may "
                + "still be in flight)",
            countOccurrences(activity, "if (isFinishing() || isDestroyed()) return;") == 2);
    }

    @Test public void thirtyCanonicalJuzGaugesArePrimaryWithNoRedundantPicker() throws Exception {
        String activity = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/ProgressMapActivity.java");
        String summary=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/JuzProgressSummary.java");
        assertTrue("exactly thirty interactive Juz rows must replace the default 604-cell wall",
            activity.contains("for(int j=1;j<=30;j++)")
            && activity.contains("root.addView(juzOverview);")
            && activity.contains("root.addView(pageDetails);")
            && activity.contains("pageDetails.setVisibility(View.GONE);")
            && activity.contains("item.setOnClickListener(v->showJuz(selected));"));
        assertFalse("No extra Juz modal selector is permitted",activity.contains("chooseJuz()"));
        String page=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/JuzProgressBar.java");
        assertTrue("Each Juz must be a vertical QCF-shaped page gauge, not a horizontal bar",
            page.contains("width*550f/345f")
            && page.contains("canvas.clipPath(outline)")
            && page.contains("learned*scale")
            && page.contains("stabilized*scale")
            && page.contains("acquired*scale"));
        assertTrue("Every real physical QCF line counted exactly once by canonical Juz",
            summary.contains("for(GeometryRepository.LineMeta line:lines)")
            && summary.contains("rowForIndex(j*8+1)")
            && summary.contains("counts[j][TOTAL]++")
            && summary.contains("if(acquired.contains(line.id))")
            && summary.contains("else if(stabilized.contains(line.id))")
            && summary.contains("else if(learned.contains(line.id))"));
        assertFalse("No redundant on-screen AI instructions",
            activity.contains("toucher un juz pour afficher ses pages")
            || activity.contains("toucher une page pour ouvrir le Mushaf")
            || activity.contains("toucher pour ouvrir en Lecture"));
        assertFalse("Juz miniature pages must not display percentage or line counts",
            activity.contains("juzPercentLabels")
            || activity.contains("juzCountLabels")
            || activity.contains("percent+\" %\"")
            || activity.contains("done+\"/\"+total+\" lignes\""));
        assertTrue("One page silhouette per Juz and one short visible label",
            activity.contains("JuzProgressBar page=new JuzProgressBar(this);")
            && activity.contains("Ui.bookText(this,\"Juz \"+j")
            && activity.contains("item.addView(page)")
            && activity.contains("item.addView(label)"));
        assertTrue("The Back button must return to the thirty juz, not close the app",
            activity.contains("if(selectedJuz>0)showAllPages();"));
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
