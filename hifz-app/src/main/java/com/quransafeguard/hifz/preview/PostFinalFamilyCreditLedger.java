package com.quransafeguard.hifz.preview;

import android.content.SharedPreferences;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Read-only per-family provenance of acquired Hifz lines for post-final recall.
 * No mutation of frozen Sabqi/Itqan engines, credits, schema or backup keys.
 *
 * Historical family snowball units can prove the provenance of previously
 * acquired blocks. A validated final in the durable post-final queue extends
 * this proof; old 1.17.7 queued current pages require physical history as a
 * second witness, because their page could contain global Acquis from Itqan
 * or Sabqi. New plans tagged familySourceVersion=1 contain only that family.
 */
final class PostFinalFamilyCreditLedger {
    private PostFinalFamilyCreditLedger() {}

    private static boolean sabqi(ConsolidationCycleEngine.Family family) {
        if (family == ConsolidationCycleEngine.Family.LEARNING) return true;
        if (family == ConsolidationCycleEngine.Family.STABILIZATION) return false;
        throw new IllegalArgumentException("Unsupported post-final family");
    }
    static String historyKey(ConsolidationCycleEngine.Family family) {
        return sabqi(family) ? "learningSnowball8WeekHistory" : "stabilizationSnowball8WeekHistory";
    }
    static String queueFamily(ConsolidationCycleEngine.Family family) {
        return sabqi(family) ? PostFinalRecallStore.SABQI : PostFinalRecallStore.ITQAN;
    }
    private static final class Event {
        final LocalDate date;
        final int sourceRank, serial;
        final List<String> ids;
        Event(LocalDate date,int sourceRank,int serial,List<String> ids){
            this.date=date;this.sourceRank=sourceRank;this.serial=serial;this.ids=ids;
        }
    }

    static List<String> previouslyAcquired(SharedPreferences prefs,
            ConsolidationCycleEngine.Family family,Set<String> acquired) {
        if(prefs==null||acquired==null)
            throw new IllegalArgumentException("Explicit family acquisition proof required");
        String familyName=queueFamily(family);
        ArrayList<Event> events=new ArrayList<>();
        try {
            JSONArray history=new JSONArray(prefs.getString(historyKey(family),"[]"));
            java.util.HashMap<String,Set<String>> byWeek=new java.util.HashMap<>();
            for(int i=0;i<history.length();i++){
                JSONObject week=history.getJSONObject(i);
                LocalDate start=LocalDate.parse(week.getString("week"));
                JSONArray units=week.getJSONArray("units");
                ArrayList<String> verified=new ArrayList<>();
                LinkedHashSet<String> allHistorical=new LinkedHashSet<>();
                for(int j=0;j<units.length();j++){
                    List<String> ids=ConsolidationPhysicalUnitPolicy.decodeLineUnit(units.getString(j));
                    allHistorical.addAll(ids);
                    // A partly acquired block is not proof of a completed finale.
                    if(acquired.containsAll(ids)) verified.addAll(ids);
                }
                byWeek.put(start.toString(),allHistorical);
                if(!verified.isEmpty())events.add(new Event(start.plusDays(6),0,i,verified));
            }
            JSONArray queue=new JSONArray(prefs.getString(PostFinalRecallStore.KEY,"[]"));
            for(int i=0;i<queue.length();i++){
                JSONObject item=queue.getJSONObject(i);
                if(!familyName.equals(item.getString("family")))continue;
                LocalDate date=LocalDate.parse(item.getString("date"));
                String status=item.optString("status","");
                String last=prefs.getString(sabqi(family)
                    ?"lastLearningConsolidationDate":"lastRecentSabqiReviewDate","");
                if(!"READY".equals(status)&&!"DONE".equals(status)
                    &&!("PREPARED".equals(status)&&date.toString().equals(last)))continue;
                boolean tagged=item.optInt("familySourceVersion",0)==1;
                Set<String> witnessed=byWeek.get(date.minusDays(6).toString());
                JSONArray pages=item.getJSONArray("pages");
                ArrayList<String> confirmed=new ArrayList<>();
                for(int j=0;j<pages.length();j++){
                    JSONObject page=pages.getJSONObject(j);
                    if(!page.getBoolean("current"))continue;
                    JSONArray lines=page.getJSONArray("lines");
                    for(int k=0;k<lines.length();k++){
                        String id=lines.getString(k);
                        if(id.isEmpty())throw new IllegalStateException("Empty final line receipt");
                        // 1.17.7 current page sometimes included unrelated Acquis
                        // from the opposite family. Require its dated family unit.
                        if(acquired.contains(id)&&(tagged
                            || (witnessed!=null && witnessed.contains(id))))confirmed.add(id);
                    }
                }
                if(!confirmed.isEmpty())events.add(new Event(date,1,i,confirmed));
            }
        } catch(Exception malformed) {
            throw new IllegalStateException(
                "Missing or corrupt family-specific credit proof: global Acquis fallback forbidden",malformed);
        }
        // Read-only canonical chronology, deterministic even after skipped Sundays.
        events.sort(Comparator.comparing((Event e)->e.date)
            .thenComparingInt(e->e.sourceRank).thenComparingInt(e->e.serial));
        LinkedHashSet<String> result=new LinkedHashSet<>();
        for(Event event:events)result.addAll(event.ids);
        return Collections.unmodifiableList(new ArrayList<>(result));
    }
}
