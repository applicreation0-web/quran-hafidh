package com.quransafeguard.hifz.preview;

import android.content.SharedPreferences;
import com.quransafeguard.hifz.core.VerseRef;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Versioned, physical Itqan session evidence. The old scalar repetition counter,
 * a unit's verse bounds, and a caller-supplied completion payload are NOT proofs.
 * Missing evidence on a historical OPEN never grants a credit or rewrites its date.
 */
final class ItqanPhysicalProof11711 {
    static final String KEY="itqanPhysicalBlockProofV7";
    private ItqanPhysicalProof11711(){}

    static JSONObject read(SharedPreferences p) {
        String raw=p.getString(KEY,"");
        if(raw==null||raw.isEmpty())return null;
        try {
            JSONObject v=new JSONObject(raw);
            if(v.getInt("v")!=7||v.getString("token").isEmpty()
                    ||LocalDate.parse(v.getString("date"))==null
                    ||v.getJSONArray("lines").length()==0)return null;
            return v;
        } catch(Exception malformed){return null;}
    }
    static String token(SharedPreferences p) {
        JSONObject v=read(p);
        return v==null?"":v.optString("token","");
    }
    static String date(SharedPreferences p) {
        JSONObject v=read(p);
        return v==null?"":v.optString("date","");
    }
    private static JSONArray array(List<String> items) {
        JSONArray a=new JSONArray();
        for(String id:items)a.put(id);
        return a;
    }
    private static boolean equal(JSONArray actual,List<String> expected){
        if(actual==null||expected==null||actual.length()!=expected.size())return false;
        for(int i=0;i<expected.size();i++)
            if(!expected.get(i).equals(actual.optString(i,null)))return false;
        return true;
    }

    /**
     * Calculate the entire canonical block afresh from the authentic QCF geometry,
     * physical continuation, saved consumed bonuses and the accepted bonus offer.
     * Never copy the displayed lines as the expected canonical payload.
     */
    static boolean begin(HifzPrefs h,SharedPreferences p,GeometryRepository geometry,
            List<String> displayed,VerseRef start,VerseRef end,
            ItqanRegimeStore.UnitPlan plan,String requestedDate) {
        if(h==null||p==null||geometry==null||displayed==null
                ||start==null||end==null||plan==null)return false;
        try {
            LocalDate date=LocalDate.parse(requestedDate);
            if(date.isAfter(HifzClock.today()))return false;
            List<String> whole=h.ordinaryItqanLineIds(start,end,geometry);
            if(whole.isEmpty())return false;
            List<GeometryRepository.LineMeta> physical=geometry.linesForExactIds(whole);
            boolean maintenance=plan.regime==ItqanMaintenancePolicy.Regime.POST_NAS_MAINTENANCE;
            List<StabilizationHalfPagePolicy.Unit> blocks=maintenance
                ?Collections.singletonList(new StabilizationHalfPagePolicy.Unit(
                    physical.get(0).page,physical.get(0).verses.get(0).getSurah(),whole))
                :StabilizationHalfPagePolicy.planPage(physical);
            int index=h.itqanBlockIndex();
            if(index<0||index>=blocks.size())return false;
            List<String> own=new ArrayList<>(blocks.get(index).lineIds);
            own.removeAll(h.itqanConsumedBonusLineIds());
            if(own.isEmpty())return false;
            ItqanPlanSnapshot decision=h.itqanBonusSnapshot();
            List<String> bonus=Collections.emptyList();
            if(decision!=null&&decision.matches(start,end,index)
                    &&decision.decision==ItqanPlanSnapshot.Decision.EXTEND){
                if(maintenance||index+1>=blocks.size())return false;
                ItqanPageCompletionPolicy.Offer offer=ItqanCompletionBoundaryPolicy.offer(
                    geometry,own,blocks.get(index+1).lineIds);
                if(offer==null||!decision.bonusLineIds.equals(offer.bonusLineIds))return false;
                bonus=decision.bonusLineIds;
            }
            ArrayList<String> expected=new ArrayList<>(own);
            expected.addAll(bonus);
            if(!expected.equals(displayed)
                    ||new LinkedHashSet<>(expected).size()!=expected.size())return false;
            // The proof verifier must never mutate rotation preferences merely
            // by checking a physical session. Canonical fallback is read-only.
            com.quransafeguard.hifz.core.EligibleCorpus corpus=h.itqanWorkCorpus();
            VerseRef rotation=h.itqanRotationStart();
            if(rotation==null||!corpus.contains(rotation))rotation=corpus.getRanges().get(0).getStart();
            VerseRef next=corpus.nextAnchored(end,rotation);
            String cursor=next==null?"":next.toString();
            int target=ItqanMaintenancePolicy.totalReps(plan);
            JSONObject existing=read(p);
            if(existing!=null){
                return existing.optString("start").equals(start.toString())
                    &&existing.optString("end").equals(end.toString())
                    &&existing.optInt("block",-1)==index
                    &&existing.optInt("target",-1)==target
                    &&existing.optString("cursor").equals(cursor)
                    &&existing.optString("regime").equals(plan.regime.name())
                    &&existing.optString("protocol").equals(plan.protocol.name())
                    &&existing.optString("mask").equals(plan.maskProfile.name())
                    &&existing.optBoolean("final",! (index+1==blocks.size()))
                        ==(index+1==blocks.size())
                    &&equal(existing.optJSONArray("lines"),expected)
                    &&equal(existing.optJSONArray("bonus"),bonus)
                    &&existing.optInt("reps",-1)==h.itqanRep();
            }
            // Legacy scalar-only OPEN: preserve, quarantine from credit, offer
            // explicit protocol replay; never grant today's dated receipt.
            if(h.itqanRep()!=0)return false;
            JSONObject fresh=new JSONObject()
                .put("v",7).put("token",UUID.randomUUID().toString())
                .put("date",requestedDate).put("start",start.toString())
                .put("end",end.toString()).put("block",index)
                .put("target",target).put("reps",0)
                .put("regime",plan.regime.name())
                .put("protocol",plan.protocol.name())
                .put("mask",plan.maskProfile.name())
                .put("cursor",cursor).put("final",index+1==blocks.size())
                .put("lines",array(expected)).put("bonus",array(bonus));
            return p.edit().putString(KEY,fresh.toString()).commit();
        }catch(Exception invalid){return false;}
    }

    /** This is the ONLY repetition writer for a V7 physical session. */
    static boolean record(SharedPreferences p,String token,int expectedRep,
            boolean revealed,ItqanRegimeStore.UnitPlan plan) {
        JSONObject proof=read(p);
        if(proof==null||plan==null||token==null
                ||!token.equals(proof.optString("token",""))
                ||expectedRep<0||expectedRep!=proof.optInt("reps",-1)
                ||expectedRep!=p.getInt("itqanRep",0)
                ||expectedRep>=proof.optInt("target",0)
                ||proof.optInt("block",-1)!=p.getInt("itqanBlockIndex",0)
                ||!proof.optString("regime").equals(plan.regime.name())
                ||!proof.optString("protocol").equals(plan.protocol.name())
                ||!proof.optString("mask").equals(plan.maskProfile.name()))
            return false;
        String currentStart=p.getString("itqanUnitStart","");
        String currentEnd=p.getString("itqanUnitEnd","");
        if(!currentStart.isEmpty()&&!currentStart.equals(proof.optString("start")))return false;
        if(!currentEnd.isEmpty()&&!currentEnd.equals(proof.optString("end")))return false;
        int next=expectedRep+1;
        int assisted=p.getInt("itqanAssisted",0)+(revealed?1:0);
        int finalReveals=p.getInt("itqanFinalReveals",0)
            +(revealed&&ItqanMaintenancePolicy.isValidationRep(plan,expectedRep)?1:0);
        try{
            proof.put("reps",next);
            return p.edit().putInt("itqanRep",next)
                .putInt("itqanAssisted",assisted)
                .putInt("itqanFinalReveals",finalReveals)
                .putString("itqanUnitStart",proof.getString("start"))
                .putString("itqanUnitEnd",proof.getString("end"))
                .putString(KEY,proof.toString()).commit();
        }catch(Exception invalid){return false;}
    }

    /** Strict replay guard, also applied to reinforcement and maintenance. */
    static boolean complete(SharedPreferences p,List<String> lines,List<String> bonus,
            int nextBlockIndex,boolean finalBlock,VerseRef start,VerseRef end,
            VerseRef nextCursor,String date) {
        JSONObject v=read(p);
        if(v==null||start==null||end==null||date==null)return false;
        return v.optString("start").equals(start.toString())
            &&v.optString("end").equals(end.toString())
            &&v.optString("date").equals(date)
            &&v.optInt("block",-1)==p.getInt("itqanBlockIndex",0)
            &&v.optInt("block",-1)+1==nextBlockIndex
            &&v.optBoolean("final",!finalBlock)==finalBlock
            &&v.optString("cursor").equals(nextCursor==null?"":nextCursor.toString())
            &&v.optInt("target",-1)==p.getInt("itqanRep",0)
            &&v.optInt("reps",-1)==p.getInt("itqanRep",0)
            &&(lines==null||equal(v.optJSONArray("lines"),lines))
            &&(bonus==null||equal(v.optJSONArray("bonus"),bonus))
            &&v.optString("start").equals(p.getString("itqanUnitStart",""))
            &&v.optString("end").equals(p.getString("itqanUnitEnd",""));
    }
}
