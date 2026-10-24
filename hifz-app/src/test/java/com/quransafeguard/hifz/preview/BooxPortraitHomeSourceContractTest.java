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

    @Test public void homeShowsDominantSessionAndThreeLargeDestinations() throws Exception {
        String home=read(MAIN);
        assertTrue(home.contains("todayAction = todayLine()"));
        assertTrue(home.contains("Ui.bookText(this, \"AUJOURD’HUI\", 17f, true)"));
        assertTrue(home.contains("Ui.bookText(this, \"…\", 23f, true)"));
        assertTrue(home.contains("Ui.bookText(this, \"CONTINUER  ›\", 20f, true)"));
        assertTrue(home.contains("tile.setMinimumHeight(Ui.dp(this, sideDp + 54))"));
        assertTrue(home.contains("booxIconDp(3, 112)"));
        for(String name: new String[]{"Parcours Hifz","Lecture","Atlas"})
            assertTrue("Missing direct destination "+name,home.contains("primaryTile(destinations, \""+name+"\""));
        assertFalse("Free memorisation is an optional advanced tool, not a Home destination",
            home.contains("secondaryTile(secondary, \"Mémorisation libre\""));
        assertTrue(home.contains("secondaryTile(secondary, \"Progression\""));
        assertTrue("No dimmed E-Ink labels", home.contains("line.setAlpha(1f)"));
        assertFalse("Never fade a control to 42%", home.contains("0.42f"));
    }

    @Test public void sevenDayRailShowsOneSelectedDayAndRealPlannerData() throws Exception {
        String home=read(MAIN);
        assertTrue(home.contains("weekRows = rows"));
        assertTrue(home.contains("new WeeklyDashboardPlanner(prefs, geometry, ledger, speedStore).week(HifzClock.today())"));
        assertTrue(home.contains("selectedWeekDate = row.date"));
        assertTrue(home.contains("renderSelectedWeekDay()"));
        assertTrue(home.contains("weekStrip.addView(day,new LinearLayout.LayoutParams(0,Ui.dp(this, 56),1f))"));
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

    @Test public void todayAndHifzNavigationKeepCanonicalRouting() throws Exception {
        String home=read(MAIN);
        assertTrue(home.contains("line.setOnClickListener(v -> openToday())"));
        assertTrue(home.contains("if(sessions.size()<=1){ openMode(mode,scheduled); return; }"));
        assertTrue(home.contains("setLineEnabled(sabqiQuickAccess, action == CadenceAction.LEARNING)"));
        assertTrue(home.contains("setLineEnabled(itqanQuickAccess, action == CadenceAction.STABILIZATION)"));
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
