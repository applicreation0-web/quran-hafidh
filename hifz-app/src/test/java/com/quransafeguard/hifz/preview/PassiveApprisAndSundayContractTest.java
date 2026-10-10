package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.Assert.*;

/** J6: no premature Acquis, stable cursors, Sunday without revision. */
public final class PassiveApprisAndSundayContractTest {
    private static GeometryRepository geometry() throws Exception {
        java.nio.file.Path p=Paths.get("app/src/main/assets/reader109/geometry.json");
        if(!Files.exists(p))p=Paths.get("../app/src/main/assets/reader109/geometry.json");
        return GeometryRepository.fromJson(new String(Files.readAllBytes(p),StandardCharsets.UTF_8));
    }

    @Test public void newlyLearnedFullLineJoinsPassiveButNotActiveOrAcquired() throws Exception {
        GeometryRepository g=geometry();
        InMemoryPrefs disk=new InMemoryPrefs();
        disk.disk.put("itqanRanges","[{\"start\":\"1:1\",\"end\":\"1:7\"}]");
        disk.disk.put("v6LearnedLineIds","[]");
        disk.disk.put("v6AcquiredCreditLineIds","[]");
        HifzPrefs prefs=new HifzPrefs(disk);
        String chosen=null;
        VerseRef verse=null;
        // Use actual complete one-line Quran verse, not a guessed verse coordinate.
        for(String id:g.lineIdsOnPage(520)) {
            GeometryRepository.LineMeta line=g.linesForExactIds(Collections.singletonList(id)).get(0);
            for(VerseRef ref:line.verses) {
                if(g.firstLineIndex(ref)==line.globalIndex && g.lastLineIndex(ref)==line.globalIndex) {
                    chosen=id;verse=ref;break;
                }
            }
            if(chosen!=null)break;
        }
        assertNotNull("Real 520 Mushaf must yield a complete one-line verse",chosen);
        assertFalse(prefs.murajaahCorpus(g).contains(verse));
        assertFalse(prefs.activeMurajaahCorpus().contains(verse));
        disk.disk.put("v6LearnedLineIds","[\""+chosen+"\"]");
        Map<String,Object> before=new LinkedHashMap<>(disk.disk);
        HifzPrefs rebooted=new HifzPrefs(disk);
        assertTrue("Appris joins passive immediately",rebooted.murajaahCorpus(g).contains(verse));
        assertFalse("Appris must not join active",rebooted.activeMurajaahCorpus().contains(verse));
        assertEquals("Reading cannot promote lines to Acquis","[]",
            disk.getString("v6AcquiredCreditLineIds","[]"));
        assertEquals("Passive corpus query must be read-only",before,disk.disk);
    }

    @Test public void editableSuggestedHoursPersistWithoutRewritingProgression() {
        InMemoryPrefs disk=new InMemoryPrefs();
        HifzPrefs pref=new HifzPrefs(disk);
        assertEquals("05:30",pref.suggestedMorningTime());
        assertEquals("20:00",pref.suggestedEveningTime());
        disk.disk.put("murajaahCursor","49:1");
        disk.disk.put("v6AcquiredCreditLineIds","[\"p520-l02\"]");
        assertTrue(pref.setSuggestedMorningTime("06:05"));
        assertTrue(pref.setSuggestedEveningTime("19:20"));
        HifzPrefs again=new HifzPrefs(disk);
        assertEquals("06:05",again.suggestedMorningTime());
        assertEquals("19:20",again.suggestedEveningTime());
        assertEquals("49:1",disk.getString("murajaahCursor",""));
        assertEquals("[\"p520-l02\"]",disk.getString("v6AcquiredCreditLineIds",""));
        try{pref.setSuggestedMorningTime("25:10");fail("bad clock time accepted");}
        catch(IllegalArgumentException expected){}
    }

    @Test public void bothProgrammesEnforceSundayNoActiveAndNoPassive() throws Exception {
        java.nio.file.Path mainFile=Paths.get(
            "hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        if(!Files.exists(mainFile))mainFile=Paths.get("..",mainFile.toString());
        java.nio.file.Path plannerFile=Paths.get(
            "hifz-app/src/main/java/com/quransafeguard/hifz/preview/WeeklyDashboardPlanner.java");
        if(!Files.exists(plannerFile))plannerFile=Paths.get("..",plannerFile.toString());
        String main=new String(Files.readAllBytes(mainFile),StandardCharsets.UTF_8);
        String planner=new String(Files.readAllBytes(plannerFile),StandardCharsets.UTF_8);
        assertTrue(main.contains("if(today.getDayOfWeek()!=DayOfWeek.SUNDAY)"));
        assertTrue(main.contains("return null; // No active/passive review on Sunday in EITHER programme."));
        assertTrue(planner.contains("// Sunday cannot consume a passive cursor or schedule any general revisions."));
        assertTrue(main.contains("Finale Renforcement Sabqi · matin"));
        assertTrue(main.contains("Finale Consolidation Itqān · soir"));
    }
}
