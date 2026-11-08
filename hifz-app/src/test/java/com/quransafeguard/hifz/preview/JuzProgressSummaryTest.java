package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

/** Every Juz gauge is a ratio of genuinely completed physical QCF lines. */
public final class JuzProgressSummaryTest {
    @Test public void percentCountsLinesNotVersesAndNeverPrematurelyRoundsUp() {
        assertEquals(0,JuzProgressSummary.percent(0,300));
        assertEquals(0,JuzProgressSummary.percent(1,300));
        assertEquals(50,JuzProgressSummary.percent(150,300));
        assertEquals(99,JuzProgressSummary.percent(299,300));
        assertEquals(100,JuzProgressSummary.percent(300,300));
    }

    @Test public void impossibleLineTotalsFailClosed() {
        for(int[] pair:new int[][]{{0,0},{2,1},{-1,20},{21,20}}){
            try{
                JuzProgressSummary.percent(pair[0],pair[1]);
                fail("No false Juz progress");
            }catch(IllegalArgumentException expected){}
        }
    }

    @Test public void eachPhysicalLineBelongsToOnlyOneCanonicalJuz() {
        ArrayList<GeometryRepository.LineMeta> sample=new ArrayList<>();
        int idx=0;
        for(int j=0;j<30;j++){
            int[] rub=QuranRubBoundaries.rowForIndex(j*8+1);
            sample.add(new GeometryRepository.LineMeta(idx++,"qcf-"+j,rub[3],
                Collections.singletonList(new VerseRef(rub[1],rub[2]))));
        }
        Set<String> acquired=new HashSet<>();
        acquired.add("qcf-0");
        Set<String> learned=new HashSet<>();
        learned.add("qcf-0");
        learned.add("qcf-1");
        Set<String> stabilized=new HashSet<>();
        stabilized.add("qcf-0");
        stabilized.add("qcf-2");
        int[][] counts=JuzProgressSummary.fromLines(sample,learned,stabilized,acquired);
        assertEquals(30,counts.length);
        int total=0;
        for(int[] c:counts)total+=c[JuzProgressSummary.TOTAL];
        assertEquals(30,total);
        assertEquals(1,counts[0][JuzProgressSummary.ACQUIRED]);
        assertEquals(0,counts[0][JuzProgressSummary.LEARNED]);
        assertEquals(0,counts[0][JuzProgressSummary.STABILIZED]);
        assertEquals(1,counts[1][JuzProgressSummary.LEARNED]);
        assertEquals(1,counts[2][JuzProgressSummary.STABILIZED]);
        assertEquals(0,JuzProgressSummary.done(counts[29]));
    }

    @Test public void pagePatternsAndGaugesOnlyUseRecordedLines() throws Exception {
        java.nio.file.Path source = java.nio.file.Path.of("app/src/main/assets/reader109/geometry.json");
        if (!java.nio.file.Files.exists(source)) source = java.nio.file.Path.of("..").resolve(source);
        GeometryRepository geometry = GeometryRepository.fromJson(java.nio.file.Files.readString(source));
        InMemoryPrefs disk = PartialFinalEightWeekStressTest.empty();
        HifzPrefs prefs = new HifzPrefs(disk);
        prefs.setV6StabilizationRanges(Collections.singletonList(
            new com.quransafeguard.hifz.core.VerseRange(new VerseRef(1,1), new VerseRef(1,7))), geometry);
        HifzPrefs.ProgressionSnapshot snapshot = prefs.progressionSnapshotV6();
        assertEquals(ProgressGridView.VIDE, ProgressMapActivity.computeStatuses(geometry, snapshot)[0][1]);
        assertEquals(0, JuzProgressSummary.done(JuzProgressSummary.summarize(geometry, snapshot)[0]));

        String learned = geometry.lineIdsOnPage(1).get(0);
        String stabilized = geometry.lineIdsOnPage(1).get(2);
        String acquired = geometry.lineIdsOnPage(1).get(4);
        disk.disk.put("v6LearnedLineIds", "[\"" + learned + "\"]");
        disk.disk.put("v6StabilizedLineIds", "[\"" + stabilized + "\"]");
        disk.disk.put("v6AcquiredCreditLineIds", "[\"" + acquired + "\"]");
        snapshot = prefs.progressionSnapshotV6();
        assertEquals(ProgressGridView.STABILISER, ProgressMapActivity.computeStatuses(geometry, snapshot)[0][1]);
        int[] juz1 = JuzProgressSummary.summarize(geometry, snapshot)[0];
        assertEquals(1, juz1[JuzProgressSummary.LEARNED]);
        assertEquals(1, juz1[JuzProgressSummary.STABILIZED]);
        assertEquals(1, juz1[JuzProgressSummary.ACQUIRED]);
        assertEquals(0, JuzProgressSummary.done(JuzProgressSummary.summarize(geometry, snapshot)[29]));
    }

    @Test public void aSharedFolioIsReachableInBothJuzWithOnlyItsOwnLineStatuses() throws Exception {
        java.nio.file.Path source = java.nio.file.Path.of("app/src/main/assets/reader109/geometry.json");
        if (!java.nio.file.Files.exists(source)) source = java.nio.file.Path.of("..").resolve(source);
        GeometryRepository geometry = GeometryRepository.fromJson(java.nio.file.Files.readString(source));
        assertEquals(62, JuzProgressSummary.pageWindow(geometry, 3)[1]);
        assertEquals(62, JuzProgressSummary.pageWindow(geometry, 4)[0]);

        String acquiredInJuz3 = geometry.lineIdsOnPage(62).get(0);
        String stabilizedInJuz4 = geometry.lineIdsOnPage(62).get(2);
        InMemoryPrefs disk = PartialFinalEightWeekStressTest.empty();
        disk.disk.put("v6StabilizedLineIds", new org.json.JSONArray(Set.of(stabilizedInJuz4)).toString());
        disk.disk.put("v6AcquiredCreditLineIds", new org.json.JSONArray(Set.of(acquiredInJuz3)).toString());
        HifzPrefs.ProgressionSnapshot progress = new HifzPrefs(disk).progressionSnapshotV6();
        int[][] pages = ProgressMapActivity.computeStatuses(geometry, progress);
        assertEquals(ProgressGridView.ACQUIS, pages[2][62]);
        assertEquals(ProgressGridView.STABILISER, pages[3][62]);

        int[][] counts = JuzProgressSummary.summarize(geometry, progress);
        assertEquals(1, JuzProgressSummary.done(counts[2]));
        assertEquals(1, JuzProgressSummary.done(counts[3]));
        assertEquals(1, counts[2][JuzProgressSummary.ACQUIRED]);
        assertEquals(1, counts[3][JuzProgressSummary.STABILIZED]);
    }

    @Test public void positionPreservingGaugesMatchHujuratThroughNajmAcrossJuzBoundary() throws Exception {
        java.nio.file.Path source=java.nio.file.Path.of("app/src/main/assets/reader109/geometry.json");
        if(!java.nio.file.Files.exists(source))source=java.nio.file.Path.of("..").resolve(source);
        GeometryRepository geometry=GeometryRepository.fromJson(java.nio.file.Files.readString(source));
        int[] boundaries=JuzProgressSummary.boundaries();
        assertEquals("Al-Hujurat is physically in Juz 26",25,
            JuzProgressSummary.ownerJuzIndex(GeometryRepository.ordinal(new VerseRef(49,1)),boundaries));
        assertEquals("Beginning of An-Najm belongs to Juz 27",26,
            JuzProgressSummary.ownerJuzIndex(GeometryRepository.ordinal(new VerseRef(53,1)),boundaries));

        ArrayList<GeometryRepository.LineMeta> all=new ArrayList<>();
        ArrayList<GeometryRepository.LineMeta> j26=new ArrayList<>(),j27=new ArrayList<>();
        for(int i=0;i<geometry.lineCount();i++){
            GeometryRepository.LineMeta line=geometry.line(i);
            all.add(line);
            int j=JuzProgressSummary.ownerJuzIndex(
                GeometryRepository.ordinal(line.verses.get(0)),boundaries);
            if(j==25)j26.add(line);
            if(j==26)j27.add(line);
        }
        assertTrue(j26.size()>30 && j27.size()>30);
        Set<String> acquired=new HashSet<>(),learned=new HashSet<>(),stabilized=new HashSet<>();
        // The end of Juz 26 is done; the beginning of Juz 27 is done.
        for(int i=j26.size()-20;i<j26.size();i++)acquired.add(j26.get(i).id);
        for(int i=0;i<25;i++)learned.add(j27.get(i).id);
        // An isolated later Itqan segment may exist; blank lines must remain blank.
        stabilized.add(j27.get(40).id);
        int[][] positions=JuzProgressSummary.statusesFromLines(all,learned,stabilized,acquired);
        assertEquals(30,positions.length);
        assertEquals(j26.size(),positions[25].length);
        assertEquals(j27.size(),positions[26].length);
        assertEquals(ProgressGridView.VIDE,positions[25][0]);
        assertEquals(ProgressGridView.ACQUIS,positions[25][positions[25].length-1]);
        assertEquals(ProgressGridView.APPRENTISSAGE,positions[26][0]);
        assertEquals(ProgressGridView.VIDE,positions[26][positions[26].length-1]);
        assertEquals(ProgressGridView.VIDE,positions[26][30]);
        assertEquals(ProgressGridView.STABILISER,positions[26][40]);
        assertEquals(ProgressGridView.VIDE,positions[26][41]);

        int[][] counts=JuzProgressSummary.fromLines(all,learned,stabilized,acquired);
        assertEquals(20,JuzProgressSummary.done(counts[25]));
        assertEquals(26,JuzProgressSummary.done(counts[26]));
        assertEquals(20,counts[25][JuzProgressSummary.ACQUIRED]);
        assertEquals(25,counts[26][JuzProgressSummary.LEARNED]);
        assertEquals(1,counts[26][JuzProgressSummary.STABILIZED]);

        // A duplicate line credit is never a second physical stripe.
        learned.add(j26.get(j26.size()-1).id);
        int[][] overlap=JuzProgressSummary.statusesFromLines(all,learned,stabilized,acquired);
        assertEquals(j26.size(),overlap[25].length);
        assertEquals(ProgressGridView.ACQUIS,overlap[25][overlap[25].length-1]);
        assertEquals(20,JuzProgressSummary.done(
            JuzProgressSummary.fromLines(all,learned,stabilized,acquired)[25]));
    }
}
