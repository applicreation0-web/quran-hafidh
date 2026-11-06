package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.EligibleCorpus;
import com.quransafeguard.hifz.core.VerseRange;
import com.quransafeguard.hifz.core.VerseRef;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

/**
 * JVM fixture: genuine QCF geometry, immutable Itqan physical session,
 * token-bound earned repetitions, and exactly one source completion.
 * Never inject a scalar count or fake a daily receipt.
 */
final class MorningProtocolFixture {
    private static GeometryRepository geometry;
    static GeometryRepository realGeometry(){
        if(geometry!=null)return geometry;
        try {
            Path f=Paths.get("app/src/main/assets/reader109/geometry.json");
            if(!Files.exists(f))f=Paths.get("../app/src/main/assets/reader109/geometry.json");
            geometry=GeometryRepository.fromJson(Files.readString(f));
            return geometry;
        }catch(Exception e){throw new AssertionError("Authentic QCF geometry required",e);}
    }

    static boolean completeSabqi(HifzPrefs p,int start,int end,int next,List<String> ids,String date,String label) {
        if(p.sabqiLineCursor()<0)p.setSabqiLineCursor(start);
        assertEquals(LocalDate.parse(date),p.openSabqiLesson(realGeometry(),LocalDate.parse(date)));
        String token=p.sabqiLessonToken();
        for(int rep=0;rep<PreviewConfig.SABQI_TOTAL_REPS;rep++)
            assertTrue("Earned Sabqi repetition "+rep,p.recordSabqiRepetition(token,rep,false));
        return p.completeSabqiBlockV6(start,end,next,ids,date,label);
    }

    static List<StabilizationHalfPagePolicy.Unit> canonicalItqanBlocks(
            VerseRef start,VerseRef end) {
        return StabilizationHalfPagePolicy.planPage(realGeometry().linesForExactIds(
            CorpusLinePolicy.ownedLineIdsForRangeOnPage(start,end,realGeometry())));
    }

    static GeometryRepository.VerseUnit canonicalWeeklyItqan(VerseRef start,VerseRef end) {
        EligibleCorpus corpus=EligibleCorpus.Companion.of(
            Collections.singletonList(new VerseRange(start,end)));
        return realGeometry().eligibleWeeklyStabilizationUnit(start,end,corpus);
    }

    static boolean beginAndEarnItqan(HifzPrefs p,List<String> ids,
            VerseRef start,VerseRef end,String date){
        if(p.itqanRep()!=0)return false;
        AnchoringQueue.Entry entry=new AnchoringQueue.Entry(start.toString(),end.toString(),
            AnchoringQueue.Origin.PROMOTED,AnchoringQueue.ItqanProtocol.FULL,0);
        if(!p.setItqanProgress(0,0,start,end))return false;
        ItqanRegimeStore.UnitPlan plan=p.itqanUnitPlan(entry);
        if(!p.stampItqanUnitPlan(entry,plan))return false;
        if(!p.ensureItqanBlockProofV7(realGeometry(),ids,start,end,plan,date))return false;
        String token=p.itqanBlockProofTokenV7();
        if(token.isEmpty())return false;
        for(int rep=0;rep<ItqanMaintenancePolicy.totalReps(plan);rep++)
            if(!p.recordItqanRepV7(token,rep,false,plan))return false;
        return true;
    }

    static boolean completeItqan(HifzPrefs p,List<String> ids,int nextBlock,boolean finalBlock,
            VerseRef start,VerseRef end,VerseRef next,String date,String label,List<String> bonus) {
        if(!beginAndEarnItqan(p,ids,start,end,date))return false;
        return p.completeStabilizationBlockV6(ids,nextBlock,finalBlock,start,end,
            next,date,label,bonus);
    }

    static void prepareItqan(HifzPrefs p,VerseRef start,VerseRef end) {
        List<StabilizationHalfPagePolicy.Unit> blocks=canonicalItqanBlocks(start,end);
        assertTrue("Itqan fixture requires current canonical sub-block",p.itqanBlockIndex()<blocks.size());
        assertTrue("Real ×40 repetitions and physical proof required",
            beginAndEarnItqan(p,blocks.get(p.itqanBlockIndex()).lineIds,
                start,end,"2026-10-11"));
    }
}
