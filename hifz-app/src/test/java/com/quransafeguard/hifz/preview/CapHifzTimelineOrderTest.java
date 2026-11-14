package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

/** Contradictory chronological / canonical checks against real 604-page QCF. */
public final class CapHifzTimelineOrderTest {
    private static GeometryRepository geometry() throws Exception {
        Path source=Path.of("app/src/main/assets/reader109/geometry.json");
        if(!Files.exists(source))source=Path.of("..").resolve(source);
        return GeometryRepository.fromJson(Files.readString(source));
    }

    private static List<CapHifzForecast.Quarter> project(GeometryRepository geo, double attendance,
            boolean tailComplete, int days) {
        return CapHifzForecast.quarters(geo, LocalDate.of(2026,11,14),
            geo.firstLineIndex(new VerseRef(2,125)),
            tailComplete?-1:geo.firstLineIndex(new VerseRef(53,35)),
            tailComplete,
            day -> WeeklyProgramPolicy.actionFor(day.getDayOfWeek(),
                HifzPrefs.WeeklyProgram.MANUAL_SEVEN,days),
            days>0, !tailComplete, List.of(),Set.of(),null,attendance);
    }

    @Test public void every80To100PercentForecastIsChronologicalAndEndsWithBothRoutes() throws Exception {
        GeometryRepository geo=geometry();
        for(double rate:new double[]{.8,.9,1.0}){
            List<CapHifzForecast.Quarter> rows=project(geo,rate,false,3);
            CapHifzTimelineOrder.verify(rows);
            LocalDate goal=CapHifzTimelineOrder.firstFullCoverageQuarter(rows);
            assertNotNull("Both routes must finish at a real quarter",goal);
            assertEquals(rows.get(rows.size()-1).date,goal);
            assertEquals(SabqiRoute.ROUTE_END,rows.get(rows.size()-1).sabqi);
            assertTrue(rows.get(rows.size()-1).firstTailComplete);
            int previousSabqi=0,previousTail=0; boolean nas=false;
            for(CapHifzForecast.Quarter q:rows){
                assertTrue(q.date.getMonthValue()==3||q.date.getMonthValue()==6
                    ||q.date.getMonthValue()==9||q.date.getMonthValue()==12);
                if(q.sabqi!=null){
                    int position=GeometryRepository.ordinal(q.sabqi);
                    assertTrue(position>=previousSabqi);
                    assertTrue(position<=GeometryRepository.ordinal(SabqiRoute.ROUTE_END));
                    previousSabqi=position;
                }
                if(q.itqan!=null){
                    assertFalse("Tail must never reappear post-An-Nas",nas);
                    int pos=GeometryRepository.ordinal(q.itqan);
                    assertTrue(pos>=previousTail);
                    assertTrue(pos>=GeometryRepository.ordinal(ItqanRotationPolicy.TAIL_START));
                    assertTrue(pos<=GeometryRepository.ordinal(ItqanRotationPolicy.TAIL_END));
                    previousTail=pos;
                }
                if(q.firstTailComplete)nas=true;
            }
            assertTrue(nas);
        }
    }

    @Test public void everyJuzAndHizbBoundaryIsCanonicalAndStrictlyAscending() throws Exception {
        GeometryRepository geo=geometry();
        int lastLine=-1;
        for(int juz=1;juz<=30;juz++){
            int ordinal=JuzProgressSummary.boundaries()[juz-1];
            int line=JuzProgressSummary.firstOwnedLine(geo,ordinal);
            assertTrue(line>lastLine);
            lastLine=line;
            VerseRef ref=com.quransafeguard.hifz.core.QuranCanon.INSTANCE.fromOrdinal(ordinal);
            assertEquals(juz,CapHifzForecast.juz(ref));
            assertEquals(2*juz-1,CapHifzForecast.hizb(ref));
        }
        assertEquals(26,CapHifzForecast.juz(SabqiRoute.ROUTE_END));
        assertEquals(30,CapHifzForecast.juz(ItqanRotationPolicy.TAIL_END));
    }

    @Test public void rejectReversedSabqiOrTailAndPostNasPhantomPath() {
        LocalDate a=LocalDate.of(2027,3,31),b=LocalDate.of(2027,6,30);
        List<CapHifzForecast.Quarter> reversedSabqi=List.of(
            new CapHifzForecast.Quarter(a,new VerseRef(2,150),new VerseRef(53,10),false,false),
            new CapHifzForecast.Quarter(b,new VerseRef(2,125),new VerseRef(53,30),false,false));
        expectFailure(reversedSabqi);
        List<CapHifzForecast.Quarter> reversedTail=List.of(
            new CapHifzForecast.Quarter(a,new VerseRef(2,125),new VerseRef(54,10),false,false),
            new CapHifzForecast.Quarter(b,new VerseRef(2,150),new VerseRef(53,30),false,false));
        expectFailure(reversedTail);
        List<CapHifzForecast.Quarter> phantomPostNas=List.of(
            new CapHifzForecast.Quarter(a,new VerseRef(2,125),null,false,true),
            new CapHifzForecast.Quarter(b,new VerseRef(2,150),new VerseRef(55,1),false,true));
        expectFailure(phantomPostNas);
        List<CapHifzForecast.Quarter> reversedTime=List.of(
            new CapHifzForecast.Quarter(b,new VerseRef(2,125),null,false,false),
            new CapHifzForecast.Quarter(a,new VerseRef(2,150),null,false,false));
        expectFailure(reversedTime);
    }

    private static void expectFailure(List<CapHifzForecast.Quarter> rows){
        try{CapHifzTimelineOrder.verify(rows);fail("False timeline order accepted");}
        catch(IllegalStateException expected){}
    }

    @Test public void totalHifzIsNeverAttestedByAForecast() throws Exception {
        GeometryRepository geo=geometry();
        Set<String> acquired=new HashSet<>();
        for(int i=0;i<geo.lineCount();i++) acquired.add(geo.line(i).id);
        assertTrue(CapHifzTimelineOrder.allThirtyJuzAcquired(geo,acquired));
        acquired.remove(geo.line(0).id);
        assertFalse("A missing Fatiha line prevents claiming 30 Juz",
            CapHifzTimelineOrder.allThirtyJuzAcquired(geo,acquired));
        assertNotNull(CapHifzTimelineOrder.firstFullCoverageQuarter(project(geo,.9,false,3)));
        assertFalse(CapHifzTimelineOrder.allThirtyJuzAcquired(geo,acquired));
    }

    @Test public void noCadenceMeansNoInventedFullHifzDeadline() throws Exception {
        GeometryRepository geo=geometry();
        List<CapHifzForecast.Quarter> rows=project(geo,1.0,true,0);
        CapHifzTimelineOrder.verify(rows);
        assertNull(CapHifzTimelineOrder.firstFullCoverageQuarter(rows));
    }
}
