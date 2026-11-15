package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/**
 * Independent BOOX source contract. This does not claim to replace real-device checks.
 * The official Hifz engines and progress stores must remain untouched.
 */
public final class BooxPortraitHomeSourceContractTest {
    private static String read(String path) throws Exception {
        Path file=Paths.get(path);
        if (!Files.exists(file)) file=Paths.get("..",path);
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    private static final String MAIN = "hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java";
    private static final String RES = "hifz-app/src/main/res/drawable/";

    @Test public void portraitIsRequestedForHomeAndNoOtherScreen() throws Exception {
        String manifest=read("hifz-app/src/main/AndroidManifest.xml");
        int at=manifest.indexOf("android:name=\".MainActivity\"");
        assertTrue(at>0);
        assertTrue(manifest.substring(at, Math.min(manifest.length(),at+180))
            .contains("android:screenOrientation=\"portrait\""));
        assertEquals(1, manifest.split("android:screenOrientation", -1).length-1);
    }

    @Test public void homeOccupiesEntireUsefulViewportWithoutPhoneWidthCap() throws Exception {
        String home=read(MAIN);
        assertTrue(home.contains("scroll.setFillViewport(true)"));
        assertTrue(home.contains("scroll.addView(root, new ScrollView.LayoutParams("));
        assertTrue(home.contains("ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT"));
        assertFalse(home.contains("Ui.dp(this, 720)"));
        assertFalse(home.contains("contentWidth"));
        assertTrue(home.contains("Ui.respectSystemBars(this, scroll, 0, 0, 0, 0)"));
        assertFalse("The former navigation list must be removed", home.contains("homeNavLine("));
        assertFalse("No seven stacked day cards", home.contains("dashboard.addView(day)"));
    }

    @Test public void homeShowsDominantSessionAndFourEqualDestinations() throws Exception {
        String home=read(MAIN);
        assertTrue(home.contains("todayAction = todayLine()"));
        assertTrue(home.contains("Ui.bookText(this, \"AUJOURD’HUI\", 17f, true)"));
        assertTrue(home.contains("Ui.bookText(this, \"…\", 23f, true)"));
        assertTrue(home.contains("Ui.bookText(this, \"CONTINUER  ›\", 20f, true)"));
        assertTrue(home.contains("tile.setMinimumHeight(Ui.dp(this, sideDp + 22))"));
        assertTrue("Labels are available to accessibility without captions under the icons",
            home.contains("tile.setContentDescription(display);"));
        assertFalse(home.contains("tile.addView(title);"));
        assertTrue(home.contains("booxIconDp(2, 112)"));
        assertTrue(home.contains("primaryTile(practiceRow, \"Parcours Hifz\", \"Parcours\""));
        assertTrue(home.contains("primaryTile(practiceRow, \"Progression\", \"Progression\""));
        assertTrue(home.contains("primaryTile(readingRow, \"Lecture\", \"Lecture\""));
        assertTrue(home.contains("primaryTile(readingRow, \"Atlas\", \"Atlas\""));
        // All four home destinations must be presented identically, without a visual divider.
        int practiceEnd = home.indexOf("addHomeAccessRow(root, practiceRow)");
        int readingEnd = home.indexOf("addHomeAccessRow(root, readingRow)");
        int actionsStart = home.indexOf("geometryActions.add(study)");
        int capStart = home.indexOf("root.addView(capHifz)");
        assertTrue(practiceEnd > 0 && readingEnd > practiceEnd && capStart > readingEnd
            && actionsStart > capStart);
        assertTrue("BOOX uses a fixed viewport with two equally weighted access rows",
            home.contains("homeViewport = wideBoox() ? fixedBooxParcours(root) : scrollOf(root);")
            && home.contains("root.addView(row, new LinearLayout.LayoutParams(-1, 0, 1));"));
        assertFalse("Home destination rows must not be split by a rule",
            home.substring(practiceEnd, actionsStart).contains("Ui.divider(this)"));
        assertFalse("No asymmetric footer navigation", home.contains("secondaryTile("));
        assertFalse("Free memorisation is an optional advanced tool, not a Home destination",
            home.contains("secondaryTile(secondary, \"Mémorisation libre\""));
        assertTrue(home.contains("new Intent(this, ProgressMapActivity.class)"));
        assertTrue("No dimmed E-Ink labels", home.contains("line.setAlpha(1f)"));
        assertFalse("Never fade a control to 42%", home.contains("0.42f"));
    }

    @Test public void sevenDayRailShowsOneSelectedDayAndRealPlannerData() throws Exception {
        String home=read(MAIN);
        assertTrue(home.contains("weekRows = rows"));
        assertTrue(home.contains("new WeeklyDashboardPlanner(prefs, geometry, ledger, speedStore).week(HifzClock.today())"));
        assertTrue(home.contains("selectedWeekDate = row.date"));
        assertTrue(home.contains("renderSelectedWeekDay()"));
        assertTrue("Atlas-inspired 7-day rail must retain seven real clickable day stations",
            home.contains("weekStrip.addView(timeline")
            && home.contains("stations.addView(station")
            && home.contains("station.setOnClickListener")
            && home.contains("selectedWeekDate = row.date")
            && home.contains("boolean done = !future && \"Validé\".equals(row.state)"));
        assertTrue(home.contains("scheduleDetail(chosen.morning)"));
        assertTrue(home.contains("scheduleDetail(chosen.evening)"));
        assertTrue(home.contains("showRevisionSelector()"));
    }

    @Test public void booxNavigationHasNoCardsAndRetainsApprovedHifzIcons() throws Exception {
        String home=read(MAIN);
        assertFalse("The previous bordered-card UI is rejected by the BOOX owner",
            home.contains("Ui.panel(line)") || home.contains("Ui.panel(tile)")
            || home.contains("Ui.panel(day)"));
        assertFalse("Decorative curves were rejected by the BOOX owner",
            home.contains("new BooxFlowStroke(this)"));
        assertTrue("Each Hifz mode uses an approved dedicated full-size vector",
            home.contains("int icon = largeIconFor(label)")
                && home.contains("if (icon == 0) icon = Ui.iconFor(label, label)")
                && home.contains("architectural.setBounds(0, 0, Ui.dp(this, sideDp), Ui.dp(this, sideDp))")
                && home.contains("booxIconDp(2, 124)"));
        assertTrue("Compact mode names must fit in one BOOX column",
            home.contains("\"Apprentissage\".equals(label) ? \"Apprendre\"")
                && home.contains("\"Stabilisation\".equals(label) ? \"Stabiliser\""));
        assertTrue("Real verse boundaries must be shown rather than discarded into only modes",
            home.contains("scheduleDetail(chosen.morning)")
                && home.contains("scheduleDetail(chosen.evening)")
                && home.contains("return raw.replace(\" · \", "));
        assertFalse(home.contains("compactScheduleLine("));
    }

    @Test public void capHifdhYearZoomAndRealJuzMilestonesNavigateToOriginalMushaf() throws Exception {
        String home = read(MAIN);
        String panel = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/CapHifzPanel.java");
        String timeline = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/CapHifzTimelineView.java");
        assertTrue("The two group headings are French and do not repeat icon labels",
            home.contains("pathSectionTitle(\"ACQUISITION\")")
            && home.contains("pathSectionTitle(\"MAÎTRISE\")"));
        assertFalse(home.contains("pathSectionTitle(\"SABQI\")"));
        assertFalse(home.contains("pathSectionTitle(\"ITQĀN\")"));
        assertTrue("A year zoom prevents decades from flattening into endpoints",
            timeline.contains("LocalDate.of(selectedYear, 12, 31)")
            && timeline.contains("p.date.getYear() != selectedYear"));
        assertTrue("Real next-line cursors must be kept distinct from acquired receipts",
            panel.contains("geometry.line(cursor).page")
            && panel.contains("geometry.line(nextItqan).page")
            && timeline.contains("actualSabqiPage")
            && timeline.contains("actualTailPage")
            && timeline.contains("Prochain jalon : indéterminé")
            && timeline.contains("CIBLE AL-FATḤ")
            && timeline.contains("CIBLE AN-NĀS"));
        String ui = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/Ui.java");
        assertFalse("Unapproved Kalam titles must not be rendered on BOOX",
            ui.contains("createFromAsset(context.getAssets(), \"fonts/Kalam-Regular.ttf\")"));
        assertTrue("All French interface text must use one offline font family",
            ui.contains("INTERFACE_FONT = Typeface.create(\"sans-serif\", Typeface.NORMAL)")
            && ui.contains("view.setTypeface(typeface(bold ? Typeface.BOLD : Typeface.NORMAL))")
            && timeline.contains("Ui.typeface(bold ? Typeface.BOLD : Typeface.NORMAL)"));
        assertFalse("No stray serif headings or sublines", ui.contains("Typeface.create(\"serif\""));
        assertFalse("Rail captions must never collide with the following rail at 150% scale",
            timeline.contains("centerY + dp(14)") || timeline.contains("lastLabelX")
            || timeline.contains("text(canvas, \"ICI\""));
        assertTrue("Next boundary is searched from the visible year, not from the first year",
            timeline.contains("!point.date.isBefore(windowStart())"));
        assertTrue("Touch must open a canonical Juz or Hizb milestone",
            timeline.contains("openMilestone.accept(true, hit)")
            && timeline.contains("openMilestone.accept(false, hit)")
            && timeline.contains("exploreYear.accept(selectedYear)"));
        assertTrue("No modal for Cap Hifdh roadmap: all markers are selected inline",
            panel.contains("shownCheckpoints = checkpointsForYear(quarters, year)")
            && panel.contains("ignoredYear -> nextCheckpoint()")
            && panel.contains("openMushaf(shownCheckpoints.get(checkpointIndex).verse)")
            && panel.contains("checkpointLabel.setContentDescription(\"Ouvrir le Mushaf depuis Cap Hifdh")); 
        assertFalse("Cap Hifdh itself must never create an Android dialog",
            panel.contains("new AlertDialog.Builder("));
        assertTrue("Acquired lines are browsed inline using the same controls",
            panel.contains("private List<AcquiredChoice> acquiredChoices()")
            && panel.contains("historyMode = !historyMode")
            && panel.contains("history.setText(\"Acquis · revenir à la projection")
            && panel.contains("Ouvrir un acquis dans le Mushaf depuis Cap Hifdh"));
        assertTrue("Reader entry point must use the actual QCF page and verse",
            panel.contains("geometry.pageForVerse(verse)")
            && panel.contains("new Intent(context, StudyReaderActivity.class)")
            && panel.contains("StudyReaderActivity.EXTRA_JUMP_PAGE")
            && panel.contains("StudyReaderActivity.EXTRA_JUMP_VERSE"));
        assertFalse("Forecast does not write Hifdh credits",panel.contains("completeDailyLesson("));
    }

    @Test public void todayAndHifzNavigationKeepCanonicalRouting() throws Exception {
        String home=read(MAIN);
        assertTrue(home.contains("line.setOnClickListener(v -> openToday())"));
        assertFalse("Even one session must show the choice window",
            home.contains("if(sessions.size()<=1){ openMode(mode,scheduled); return; }"));
        assertTrue(home.contains("showTodayWindow(sessions);"));
        assertTrue("Quick access must use the actual, dated, unvalidated own-family slot",
            home.contains("HifzBacklog.onDate(backlogPending,today)"));
        assertTrue("Sabqi access is allowed only on an unvalidated Sabqi date",
            home.contains("setLineEnabled(sabqiQuickAccess,slot!=null && HifzSessionActivity.SABQI.equals(slot.mode))"));
        assertTrue("Itqan access is allowed only on an unvalidated Itqan date",
            home.contains("setLineEnabled(itqanQuickAccess,slot!=null && HifzSessionActivity.ITQAN.equals(slot.mode))"));
        assertTrue(home.contains("new Intent(this, StudyReaderActivity.class)"));
        assertTrue(home.contains("new Intent(this, AtlasMapActivity.class)"));
        assertFalse("No FreeMem shortcut on the Home navigation",
            home.contains("new Intent(this, FreeMemActivity.class)"));
        assertTrue(home.contains("new Intent(this, ProgressMapActivity.class)"));
        assertFalse(home.contains("new Intent(this, IbnKathirMapActivity.class)"));
        assertFalse(home.contains("new Intent(this, QuizActivity.class)"));
    }

    @Test public void fixedLucideSilhouettesAreActuallyPresentNotOnlyNames() throws Exception {
        String quiz=read(RES+"ic_ui_quiz.xml");
        assertTrue(quiz.contains("M22,12 A10,10 0 1,0 2,12 A10,10 0 1,0 22,12"));
        assertTrue(quiz.contains("m9 12 2 2 4-4"));
        String progress=read(RES+"ic_ui_progress_map.xml");
        assertTrue(progress.contains("M18,20 L18,10"));
        assertTrue(progress.contains("M12,20 L12,4"));
        assertTrue(progress.contains("M6,20 L6,14"));
        String back=read(RES+"ic_ui_back.xml");
        assertTrue(back.contains("m12 19-7-7 7-7") && back.contains("M19 12H5"));
        String resume=read(RES+"ic_ui_resume.xml");
        assertTrue(resume.contains("M3 12a9 9"));
        assertTrue(resume.contains("M21 3v5h-5"));
        String ui=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/Ui.java");
        assertTrue(ui.contains("if (s.equals(\"reprendre\")) return R.drawable.ic_ui_resume;"));
        for (String xml : new String[]{quiz,progress,back,resume}) {
            assertTrue(xml.contains("android:strokeColor=\"#121211\""));
            assertTrue(xml.contains("android:viewportWidth=\"24\""));
        }
    }
}
