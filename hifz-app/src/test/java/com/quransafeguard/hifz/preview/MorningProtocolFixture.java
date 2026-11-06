package com.quransafeguard.hifz.preview;
import java.util.List;
import static org.junit.Assert.*;
/** Seed a real persisted Sabqi repetition protocol before exercising completion in older fixtures. */
final class MorningProtocolFixture {
    static boolean completeSabqi(HifzPrefs p,int start,int end,int next,List<String> ids,String date,String label) {
        if(p.sabqiLineCursor()<0)p.setSabqiLineCursor(start);
        try {
            if(EarnedEveningProtocolRegressionTest.geometry==null)EarnedEveningProtocolRegressionTest.setup();
            assertEquals(java.time.LocalDate.parse(date),p.openSabqiLesson(EarnedEveningProtocolRegressionTest.geometry,java.time.LocalDate.parse(date)));
        }catch(Exception error){throw new AssertionError(error);}
        String token=p.sabqiLessonToken();
        for(int rep=0;rep<PreviewConfig.SABQI_TOTAL_REPS;rep++)assertTrue(p.recordSabqiRepetition(token,rep,false));
        return p.completeSabqiBlockV6(start,end,next,ids,date,label);
    }
    static boolean completeItqan(HifzPrefs p,List<String> ids,int nextBlock,boolean finalBlock,
            com.quransafeguard.hifz.core.VerseRef start,com.quransafeguard.hifz.core.VerseRef end,
            com.quransafeguard.hifz.core.VerseRef next,String date,String label,List<String> bonus) {
        prepareItqan(p,start,end);
        return p.completeStabilizationBlockV6(ids,nextBlock,finalBlock,start,end,next,date,label,bonus);
    }
    static void prepareItqan(HifzPrefs p,com.quransafeguard.hifz.core.VerseRef start,
            com.quransafeguard.hifz.core.VerseRef end) {
        assertTrue(p.setItqanProgress(0,0,start,end));
        ItqanRegimeStore.UnitPlan plan=p.itqanUnitPlan(new AnchoringQueue.Entry(start.toString(),end.toString(),
            AnchoringQueue.Origin.PROMOTED,AnchoringQueue.ItqanProtocol.FULL,0));
        assertTrue(p.stampItqanUnitPlan(new AnchoringQueue.Entry(start.toString(),end.toString(),
            AnchoringQueue.Origin.PROMOTED,AnchoringQueue.ItqanProtocol.FULL,0),plan));
        for(int rep=1;rep<=ItqanMaintenancePolicy.totalReps(plan);rep++)assertTrue(p.setItqanProgress(rep,0,start,end));
    }
}
