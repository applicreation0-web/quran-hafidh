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
        assertTrue(home.contains("ViewGroup.LayoutParams.WRAP_CONTENT, 1f"));
    }

    @Test public void routingAndComputedTodayStayIntact() throws Exception {
        String home=read(MAIN);
        assertTrue(home.contains("Ui.settingRow(this, \"Aujourd’hui\", \"…\", v -> openToday())"));
        for (String label : new String[]{"Lecture","Parcours Hifz","Mémorisation libre","Quiz","Progression"}) {
            assertTrue("Missing "+label,home.contains("homeNavLine(root, \""+label+"\""));
        }
        assertTrue(home.contains("new Intent(this, StudyReaderActivity.class)"));
        assertTrue(home.contains("new Intent(this, FreeMemActivity.class)"));
        assertTrue(home.contains("new Intent(this, QuizActivity.class)"));
        assertTrue(home.contains("new Intent(this, ProgressMapActivity.class)"));
        assertTrue(home.contains("showParcours()"));
        assertTrue(home.contains("refreshToday()"));
        assertTrue(home.contains("WeeklyDashboardPlanner"));
        assertTrue(home.contains("setMinimumHeight(Ui.dp(this, 68))"));
        assertFalse(home.contains("homeNavLine(root, \"Carte\""));
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
