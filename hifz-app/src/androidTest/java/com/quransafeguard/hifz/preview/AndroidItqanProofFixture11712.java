package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import java.util.Collections;
import java.util.List;

/**
 * Instrumentation fixtures must earn the same canonical physical proof as the
 * actual learner. All past scalar/date-only APIs remain intentionally retired.
 * Never synthesize a lesson receipt or bypass a validation token.
 */
final class AndroidItqanProofFixture11712 {
    private AndroidItqanProofFixture11712(){}
    static boolean earnReps(HifzPrefs prefs, GeometryRepository geometry,
            VerseRef start, VerseRef end,String date,int repetitions,boolean revealOnce){
        if(prefs==null||geometry==null||start==null||end==null||prefs.itqanRep()!=0)return false;
        AnchoringQueue.Entry queue=prefs.anchoringEntryFor(start,end);
        if(queue==null)queue=new AnchoringQueue.Entry(start.toString(),end.toString(),
            AnchoringQueue.Origin.PROMOTED,AnchoringQueue.ItqanProtocol.FULL,0);
        ItqanRegimeStore.UnitPlan plan=prefs.itqanUnitPlan(queue);
        List<String> unitIds=prefs.ordinaryItqanLineIds(start,end,geometry);
        if(unitIds.isEmpty())return false;
        List<StabilizationHalfPagePolicy.Unit> blocks=
            StabilizationHalfPagePolicy.planPage(geometry.linesForExactIds(unitIds));
        int block=prefs.itqanBlockIndex();
        if(block<0||block>=blocks.size())return false;
        List<String> ids=blocks.get(block).lineIds;
        if(!prefs.setItqanProgress(0,0,start,end)
                ||!prefs.stampItqanUnitPlan(queue,plan)
                ||!prefs.ensureItqanBlockProofV7(geometry,ids,start,end,plan,date))return false;
        String token=prefs.itqanBlockProofTokenV7();
        if(token.isEmpty()||repetitions>ItqanMaintenancePolicy.totalReps(plan))return false;
        for(int rep=0;rep<repetitions;rep++)
            if(!prefs.recordItqanRepV7(token,rep,revealOnce&&rep==0,plan))return false;
        return true;
    }
    static boolean completeOneBlock(HifzPrefs prefs,GeometryRepository geometry,
            VerseRef start,VerseRef end,String date,String label){
        AnchoringQueue.Entry queue=prefs.anchoringEntryFor(start,end);
        if(queue==null)queue=new AnchoringQueue.Entry(start.toString(),end.toString(),
            AnchoringQueue.Origin.PROMOTED,AnchoringQueue.ItqanProtocol.FULL,0);
        ItqanRegimeStore.UnitPlan plan=prefs.itqanUnitPlan(queue);
        int quota=ItqanMaintenancePolicy.totalReps(plan);
        if(!earnReps(prefs,geometry,start,end,date,quota,false))return false;
        List<String> whole=prefs.ordinaryItqanLineIds(start,end,geometry);
        List<StabilizationHalfPagePolicy.Unit> blocks=
            StabilizationHalfPagePolicy.planPage(geometry.linesForExactIds(whole));
        int i=prefs.itqanBlockIndex();
        if(i<0||i>=blocks.size())return false;
        VerseRef rotation=prefs.isRotationStartValid()?prefs.itqanRotationStart():
            prefs.itqanWorkCorpus().getRanges().get(0).getStart();
        VerseRef next=prefs.itqanWorkCorpus().nextAnchored(end,rotation);
        return prefs.completeStabilizationBlockV6(blocks.get(i).lineIds,i+1,
            i+1==blocks.size(),start,end,next,date,label,Collections.emptyList());
    }
}
