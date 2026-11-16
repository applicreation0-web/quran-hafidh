package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
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
        assertTrue("Captions require bounded icon row and sufficient text height",
            home.contains("tile.setMinimumHeight(Ui.dp(this, sideDp + 36))")
            && home.contains("TextView caption = Ui.bookText(this, display"));
        assertTrue("Four destinations have both concise visible captions and accessible descriptions",
            home.contains("tile.setContentDescription(display);")
            && home.contains("tile.addView(caption);"));
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
        int capStart = home.indexOf("root.addView(capHifz,");
        assertTrue(practiceEnd > 0 && readingEnd > practiceEnd && capStart > readingEnd
            && actionsStart > capStart);
        assertTrue("BOOX uses a fixed viewport with two bounded direct-access rows",
            home.contains("homeViewport = wideBoox() ? fixedBooxParcours(root) : scrollOf(root);")
            && home.contains("int rowDp = spaciousBooxHome() ? 148 : 98;")
            && home.contains("root.addView(row, new LinearLayout.LayoutParams(-1, Ui.dp(this, rowDp)));"));
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

    @Test public void capHifdhShowsTwoDatedNextStepsAndFinalGoalWithoutClutter() throws Exception {
        String home = read(MAIN);
        String panel = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/CapHifzPanel.java");
        String timeline = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/CapHifzTimelineView.java");
        assertTrue("The two group headings are French and do not repeat icon labels",
            home.contains("pathSectionTitle(\"ACQUISITION\")")
            && home.contains("pathSectionTitle(\"MAÎTRISE\")"));
        assertFalse(home.contains("pathSectionTitle(\"SABQI\")"));
        assertFalse(home.contains("pathSectionTitle(\"ITQĀN\")"));
        assertTrue("BOOX must fill the unused footer without any ScrollView",
            home.contains("if (wideBoox()) root.addView(capHifz,")
            && home.contains("new LinearLayout.LayoutParams(-1, 0, 1f)")
            && home.contains("homeViewport = wideBoox() ? fixedBooxParcours(root) : scrollOf(root)"));
        assertTrue("Large BOOX home must reserve space for the roadmap and show four concise captions",
            home.contains("int rowDp = spaciousBooxHome() ? 148 : 98;")
            && home.contains("TextView caption = Ui.bookText(this, display")
            && home.contains("tile.addView(caption)")
            && home.contains("spaciousBooxHome() ? 112 : 70"));
        assertTrue("Cap Hifdh needs legible large BOOX text and full-size touch controls",
            panel.contains("LinearLayout.LayoutParams graphLayout = boox")
            && panel.contains("new LinearLayout.LayoutParams(-1, 0, 1f)")
            && panel.contains("historyDp = context.getResources().getConfiguration().smallestScreenWidthDp >= 600 ? 48 : 64")
            && panel.contains("history.setMinHeight(Ui.dp(context, historyDp))")
            && panel.contains("history.setLayoutParams(new LinearLayout.LayoutParams(-1, Ui.dp(context, historyDp)))")
            && timeline.contains("1.55f")
            && timeline.contains("laneSeparation()")
            && timeline.contains("laneStart()")
            && timeline.contains("roomForSecondCaption")
            && timeline.contains("float preferred = spacious ? 184f : 136f")
            && timeline.contains("Math.max(0f, (availableDp - span) * .42f)"));
        assertTrue("After the forecast An-Nās year, Maîtrise must vanish even before real completion",
            panel.contains("CapHifzForecast.projectedTailFinishedBeforeYear(quarters, year)")
            && panel.contains("boolean showMastery = tailForecast && !forecastPostNas")
            && panel.contains("boolean showEntretien = postNas || forecastPostNas")
            && timeline.contains("private final boolean projectedMaintenance")
            && timeline.contains("if (projectedMaintenance)")
            && timeline.contains("Après An-Nās (prévision)"));
        assertTrue("After An-Nas, the second row becomes compact ongoing Entretien, without a second milestone rail",
            panel.contains("forecastSabqi || tailForecast || postNas")
            && panel.contains("prefs.juzMaintenance().read()")
            && timeline.contains("drawMaintenanceLane(")
            && timeline.contains("cycle sans échéance")
            && timeline.contains("drawFitted(canvas, \"ENTRETIEN\"")
            && !panel.contains("if (postNas) line(\"Maintien après An-Nās\""));
        assertTrue("A schematic short-term view must not compress decades into a time scale",
            timeline.contains("private List<CapHifzForecast.Milestone> nextMilestones")
            && timeline.contains("CapHifzForecast.visibleMilestones(upcoming)")
            && timeline.contains("nextMilestones(sabqi)")
            && timeline.contains("Stations have equal spacing ON PURPOSE"));
        String forecast = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/CapHifzForecast.java");
        assertTrue("Chapter starts must be projected from real QCF crossings, notably Āl ʿImrān 3:1",
            forecast.contains("new VerseRef(surah, 1)")
            && forecast.contains("SabqiRoute.ROUTE_END.getSurah()")
            && forecast.contains("crossings(geometry, sabqiBoundaries"));
        assertTrue("Surah names and arrow glyphs must be readable on BOOX without extra rows",
            timeline.contains("drawFitted(canvas, name, xx, top + dp(numberBaseline()), 15f")
            && panel.contains("previous.setTextSize(spaciousBoox() ? 22f : 20f)")
            && panel.contains("next.setTextSize(spaciousBoox() ? 22f : 20f)")
            && timeline.contains("drawFitted(canvas, target, x[3], top + dp(numberBaseline()), 15f"));
        assertFalse("No ghost circles or dash / à déterminer after a projected finish",
            timeline.contains("for (int i = coming.size(); i < 2; i++) ring(")
            || timeline.contains("à déterminer")
            || timeline.contains("String name = i < coming.size()"));
        assertTrue("Use spare BOOX space around the group, not a giant gap between tracks",
            timeline.contains("float top = laneStart()")
            && timeline.contains("roomForSecondCaption")
            && timeline.contains("laneStart() + (showSabqi && !sabqi ? laneSeparation() : 0f)"));
        assertFalse("Old yearly tick axis must be removed", timeline.contains("yearY"));
        assertTrue("A future checkpoint must be strictly after the authentic cursor",
            timeline.contains("GeometryRepository.ordinal(point.verse) > cursorOrdinal")
            && timeline.contains("sabqi ? actualSabqiVerse : actualTailVerse"));
        assertTrue("Real next-line cursors must be kept distinct from acquired receipts",
            panel.contains("geometry.line(cursor).verses.get(0)")
            && panel.contains("geometry.line(nextItqan).verses.get(0)")
            && timeline.contains("actualSabqiVerse")
            && timeline.contains("actualTailVerse")
            && timeline.contains("SabqiRoute.ROUTE_END.getSurah()")
            && timeline.contains("ItqanRotationPolicy.TAIL_END.getSurah()")
            && timeline.contains("targetDate.format(SHORT_DATE)"));
        String ui = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/Ui.java");
        assertFalse("Unapproved Kalam titles must not be rendered on BOOX",
            ui.contains("createFromAsset(context.getAssets(), \"fonts/Kalam-Regular.ttf\")"));
        assertTrue("All French interface text must use one offline font family",
            ui.contains("context.getResources().getFont(R.font.patrick_hand)")
            && ui.contains("view.setTypeface(typeface(context, bold ? Typeface.BOLD : Typeface.NORMAL))")
            && timeline.contains("Ui.typeface(getContext(), bold ? Typeface.BOLD : Typeface.NORMAL)"));
        assertFalse("No stray serif headings or sublines", ui.contains("Typeface.create(\"serif\""));
        assertFalse("No old caption under an adjacent rail",
            timeline.contains("lastLabelX") || timeline.contains("yearY")
            || timeline.contains("text(canvas, \"ICI\""));
        assertTrue("The actual cursor is displayed as surah and verse, not a page number",
            timeline.contains("QuranSurahNames.rawName(current.getSurah())")
            && timeline.contains("\"verset \" + current.getAyah()")
            && !timeline.contains("\"p.\" + page")
            && panel.contains("QuranSurahNames.name(choice.verse.getSurah())"));
        assertTrue("Every stage caption has a width budget at large BOOX text sizes",
            timeline.contains("drawFitted(") && timeline.contains("paint.measureText(shown) > width"));
        assertTrue("Future boundaries come from the shown year and real start date",
            timeline.contains("!point.date.isBefore(windowStart())")
            && timeline.contains("!point.date.isBefore(start)"));
        assertTrue("Projected stations present an actual sourate and ayah with forecast date",
            timeline.contains("QuranSurahNames.rawName(coming.get(i).verse.getSurah())")
            && timeline.contains("coming.get(i).verse.getAyah()")
            && timeline.contains("coming.get(i).date.format(SHORT_DATE)")
            && panel.contains("checkpointDisplay(choice)")
            && panel.contains("QuranSurahNames.name(choice.verse.getSurah())"));
        assertTrue("Accessibility uses complete Quran positions, not Juz/Hizb numbers",
            timeline.contains("spokenVerse(point.verse)")
            && timeline.contains("QuranSurahNames.name(verse.getSurah())"));
        assertFalse("Juz/Hizb glyphs and number labels are removed from Cap Hifdh",
            timeline.contains("R.drawable.ic_cap_juz")
            || timeline.contains("R.drawable.ic_cap_hizb")
            || timeline.contains("milestoneNumber(")
            || panel.contains("new android.text.style.ImageSpan(icon"));
        assertTrue("Post-Nas maintenance does not invent an ongoing Juz session",
            timeline.contains("maintenanceVerse == null")
            && timeline.contains("Aucune séance ouverte")
            && timeline.contains("Cycle continu · sans échéance")
            && !timeline.contains("ring(canvas, x[3], y, 7f")
            && panel.contains("prefs.juzMaintenance().read()"));
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
        assertTrue("Only one fixed 90% target; no choice row and no mutable scenario",
            panel.contains("PROJECTION_ATTENDANCE_PERCENT = 90")
            && panel.contains("Prévision · 90 % d’assiduité")
            && !panel.contains("new int[]{80, 90, 100}")
            && !panel.contains("private int scenario"));
        assertTrue("Single 90% target always passed to read-only forecast",
            panel.contains("PROJECTION_ATTENDANCE_PERCENT / 100.0"));
        assertFalse("The weekly attendance summary must not obscure the roadmap",
            panel.contains("renderWeek();"));
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

    @Test public void projectedNasFinishEndsMasteryOnlyInFollowingForecastYears() {
        CapHifzForecast.Quarter before = new CapHifzForecast.Quarter(
            LocalDate.of(2028, 6, 30), null, null, false, false);
        CapHifzForecast.Quarter finished = new CapHifzForecast.Quarter(
            LocalDate.of(2028, 12, 31), null, null, false, true,
            List.of(), List.of(), null, LocalDate.of(2028, 10, 26));
        List<CapHifzForecast.Quarter> dated = List.of(before, finished);
        assertFalse(CapHifzForecast.projectedTailFinishedBeforeYear(dated, 2027));
        assertFalse("The completion year retains An-Nās and its dated goal",
            CapHifzForecast.projectedTailFinishedBeforeYear(dated, 2028));
        assertTrue("Next year has Entretien, not the old Maîtrise timeline",
            CapHifzForecast.projectedTailFinishedBeforeYear(dated, 2029));
        assertTrue("The owner's 2031 navigation must not reintroduce Maîtrise",
            CapHifzForecast.projectedTailFinishedBeforeYear(dated, 2031));
        assertFalse("Unknown finish must not fabricate the completed phase",
            CapHifzForecast.projectedTailFinishedBeforeYear(List.of(before), 2031));
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
