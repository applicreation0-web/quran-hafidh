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
 * Non-crediting provenance for the post-final 20-page reading. The global
 * Acquis corpus is NEVER a substitute for an Itqan/Sabqi-specific history.
 * First use may bootstrap only from dated physical units of the same family's
 * existing snowball history, and only when every line was already Acquis.
 * Once the durable family ledger exists, a finale extends it atomically with
 * its own Acquis transitions. No migration grants any credit.
 */
final class PostFinalFamilyCreditLedger {
    private PostFinalFamilyCreditLedger() {}

    private static boolean sabqi(ConsolidationCycleEngine.Family family) {
        if (family == ConsolidationCycleEngine.Family.LEARNING) return true;
        if (family == ConsolidationCycleEngine.Family.STABILIZATION) return false;
        throw new IllegalArgumentException("Unsupported post-final credit family");
    }
    static String key(ConsolidationCycleEngine.Family family) {
        return sabqi(family)
            ? "v6PostFinalSabqiAcquiredLineHistoryV1"
            : "v6PostFinalItqanAcquiredLineHistoryV1";
    }
    private static String historicalKey(ConsolidationCycleEngine.Family family) {
        return sabqi(family) ? "learningSnowball8WeekHistory" : "stabilizationSnowball8WeekHistory";
    }

    static List<String> previouslyAcquired(SharedPreferences prefs,
            ConsolidationCycleEngine.Family family,Set<String> acquired) {
        if(prefs==null||acquired==null)throw new IllegalArgumentException("Explicit family acquisition proof required");
        LinkedHashSet<String> proven=new LinkedHashSet<>();
        try {
            if(prefs.contains(key(family))) {
                JSONArray ledger=new JSONArray(prefs.getString(key(family),"[]"));
                for(int i=0;i<ledger.length();i++){
                    String id=ledger.getString(i);
                    if(id.isEmpty())throw new IllegalStateException("Empty family acquisition ID");
                    if(acquired.contains(id))proven.add(id);
                }
            } else {
                // An old 1.17.7 global Acquis array is not evidence of which
                // family earned its lines. Only the family's verified source
                // units, dated and fully acquired, are eligible for bootstrap.
                JSONArray weeks=new JSONArray(prefs.getString(historicalKey(family),"[]"));
                ArrayList<JSONObject> ordered=new ArrayList<>();
                for(int i=0;i<weeks.length();i++){
                    JSONObject week=weeks.getJSONObject(i);
                    LocalDate.parse(week.getString("week"));
                    ordered.add(week);
                }
                ordered.sort(Comparator.comparing(week->{
                    try{return LocalDate.parse(week.getString("week"));}
                    catch(Exception malformed){throw new IllegalStateException("Bad family week",malformed);}
                }));
                for(JSONObject week:ordered){
                    JSONArray units=week.getJSONArray("units");
                    for(int i=0;i<units.length();i++){
                        List<String> ids=ConsolidationPhysicalUnitPolicy.decodeLineUnit(units.getString(i));
                        if(acquired.containsAll(ids))proven.addAll(ids);
                    }
                }
            }
        } catch(Exception malformed) {
            throw new IllegalStateException("Family post-final provenance unavailable; refuse global Acquis fallback",malformed);
        }
        return Collections.unmodifiableList(new ArrayList<>(proven));
    }

    static String afterFinal(SharedPreferences prefs,
            ConsolidationCycleEngine.Family family,
            Set<String> acquiredBefore, Set<String> acquiredAfter,
            List<String> exactFinalLineIds) {
        if(acquiredBefore==null||acquiredAfter==null||exactFinalLineIds==null)
            throw new IllegalArgumentException("Before/after and frozen finale required");
        LinkedHashSet<String> result=new LinkedHashSet<>(
            previouslyAcquired(prefs,family,acquiredBefore));
        for(String id:exactFinalLineIds){
            if(id==null||id.isEmpty()||!acquiredAfter.contains(id))
                throw new IllegalStateException("Cannot label unacquired line as family final Acquis");
            // Old already-Acquis lines may be on a physical Sunday page.
            // A second credit must not manufacture new family provenance.
            if(!acquiredBefore.contains(id))result.add(id);
        }
        JSONArray array=new JSONArray();
        for(String id:result)array.put(id);
        return array.toString();
    }
}
