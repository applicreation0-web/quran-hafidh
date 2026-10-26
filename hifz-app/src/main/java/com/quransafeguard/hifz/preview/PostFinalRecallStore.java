package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.util.List;

/**
 * Isolated, non-crediting finale follow-up. Lives in the schema-6 preferences
 * already included verbatim in the existing HifzBackup v3, not in a new store.
 * PREPARED is persisted before the actual finale; it becomes eligible only
 * once the historical finale date has actually been validated.
 */
final class PostFinalRecallStore {
    static final String KEY="v6PostFinalMaskedReviewQueueV1";
    static final String SABQI="SABQI", ITQAN="ITQAN";
    private final SharedPreferences prefs;
    PostFinalRecallStore(Context c) {
        this(c.getSharedPreferences("quran_hifz_preview_v1",Context.MODE_PRIVATE));
    }
    PostFinalRecallStore(SharedPreferences prefs){this.prefs=prefs;}

    static final class Review {
        final String family,date;
        final int position,total,previousCount;
        final int page;
        final List<String> lines;
        Review(String family,String date,int position,int total,int previousCount,int page,List<String> lines){
            this.family=family;this.date=date;this.position=position;this.total=total;
            this.previousCount=previousCount;this.page=page;
            this.lines=Collections.unmodifiableList(new ArrayList<>(lines));
        }
    }
    private JSONArray read(){
        try {
            JSONArray a=new JSONArray(prefs.getString(KEY,"[]"));
            for(int i=0;i<a.length();i++)if(a.optJSONObject(i)==null)
                throw new IllegalStateException("Invalid final follow-up queue entry");
            return a;
        } catch(Exception invalid){
            throw new IllegalStateException("Invalid persisted final follow-up queue; leave untouched",invalid);
        }
    }
    void prepare(String family,String date,List<PostFinalCumulativePolicy.Page> pages) {
        if((!SABQI.equals(family)&&!ITQAN.equals(family))||date==null||date.isEmpty()
                ||pages==null||pages.isEmpty())throw new IllegalArgumentException("Invalid post-final plan");
        JSONArray queue=read();
        for(int i=0;i<queue.length();i++){
            JSONObject item=queue.optJSONObject(i);
            if(family.equals(item.optString("family"))&&date.equals(item.optString("date")))
                return; // frozen exact plan survives retry/restart
        }
        JSONArray serialized=new JSONArray();int prior=0;
        for(PostFinalCumulativePolicy.Page page:pages){
            if(!page.fromCurrentFinal)prior++;
            JSONArray lines=new JSONArray();
            for(String id:page.lineIds)lines.put(id);
            try{
                serialized.put(new JSONObject().put("page",page.mushafPage)
                    .put("lines",lines).put("current",page.fromCurrentFinal));
            }catch(Exception malformed){throw new IllegalStateException(malformed);}
        }
        try {
            queue.put(new JSONObject().put("family",family).put("date",date)
                .put("status","PREPARED").put("position",0).put("previous",prior)
                .put("pages",serialized));
        }catch(Exception malformed){throw new IllegalStateException(malformed);}
        if(!prefs.edit().putString(KEY,queue.toString()).commit())
            throw new IllegalStateException("Unable to persist post-final plan");
    }
    /**
     * One-way recovery for a legacy finale that was committed before the
     * post-final reading plan existed. The last completed final date is the
     * credit proof; dated physical snowball history supplies eligible pages.
     * This never reopens a final, edits Acquis or credits any additional line.
     *
     * Refuse incomplete/corrupt evidence rather than invent an entire week.
     * Both prior and final Acquis are family-specific. Never infer prior
     * pages from the global acquired-credit array.
     */
    boolean recoverCommittedFromHistory(String family,String date,Set<String>acquired,
                                        GeometryRepository geometry) {
        if((!SABQI.equals(family)&&!ITQAN.equals(family))
                ||date==null||date.isEmpty()||acquired==null||geometry==null)return false;
        String completedKey=SABQI.equals(family)
            ?"lastLearningConsolidationDate":"lastRecentSabqiReviewDate";
        if(!date.equals(prefs.getString(completedKey,"")))return false;
        LocalDate sunday;
        try { sunday=LocalDate.parse(date); }
        catch(RuntimeException invalid){return false;}
        if(sunday.getDayOfWeek()!=DayOfWeek.SUNDAY
                ||sunday.isAfter(HifzClock.today()))return false;

        // Existing PREPARED, READY and DONE snapshots are authoritative.
        JSONArray queue=read();
        for(int i=0;i<queue.length();i++){
            JSONObject item=queue.optJSONObject(i);
            if(family.equals(item.optString("family"))&&date.equals(item.optString("date")))
                return false;
        }

        String historyKey=SABQI.equals(family)
            ?"learningSnowball8WeekHistory":"stabilizationSnowball8WeekHistory";
        JSONArray weeks;
        try { weeks=new JSONArray(prefs.getString(historyKey,"[]")); }
        catch(Exception invalid) {
            android.util.Log.w("QuranHifz","Historical post-final source invalid",invalid);
            return false;
        }
        LocalDate monday=sunday.minusDays(6);
        LinkedHashSet<String> currentIds=new LinkedHashSet<>();
        boolean foundCurrentWeek=false;
        try {
            for(int i=0;i<weeks.length();i++){
                JSONObject week=weeks.optJSONObject(i);
                if(week==null)return false;
                LocalDate weekDate=LocalDate.parse(week.getString("week"));
                if(!weekDate.equals(monday))continue;
                // Historical weekly receipts identify this final's family-specific
                // source. Older Acquis are taken from the canonical progression
                // snapshot, as in the normal pre-final preparation path.
                foundCurrentWeek=true;
                JSONArray units=week.getJSONArray("units");
                for(int j=0;j<units.length();j++){
                    for(String id:ConsolidationPhysicalUnitPolicy.decodeLineUnit(units.getString(j))) {
                        // A completed date alone does not make incomplete
                        // source lines acquired. Refuse partial reconstruction.
                        if(!acquired.contains(id))return false;
                        currentIds.add(id);
                    }
                }
            }
        }catch(Exception incomplete){
            android.util.Log.w("QuranHifz","Historical post-final lines unavailable",incomplete);
            return false;
        }
        if(!foundCurrentWeek||currentIds.isEmpty())return false;
        // Historical recovery must obey the SAME family-only source rule as
        // new post-final preparation; global Acquis can contain the other track.
        ConsolidationCycleEngine.Family provenance=SABQI.equals(family)
            ?ConsolidationCycleEngine.Family.LEARNING
            :ConsolidationCycleEngine.Family.STABILIZATION;
        LinkedHashSet<String> priorIds;
        try {
            priorIds=new LinkedHashSet<>(PostFinalFamilyCreditLedger.previouslyAcquired(
                prefs,provenance,acquired));
        } catch(RuntimeException unavailable) {return false;}
        priorIds.removeAll(currentIds);
        try {
            // The geometry map is canonical, but the historical family/week
            // order determines which eligible 20 *pages* are most recent.
            java.util.HashMap<String,GeometryRepository.LineMeta> byId=new java.util.HashMap<>();
            for(int i=0;i<geometry.lineCount();i++){
                GeometryRepository.LineMeta line=geometry.line(i);
                byId.put(line.id,line);
            }
            ArrayList<GeometryRepository.LineMeta> previous=new ArrayList<>();
            ArrayList<GeometryRepository.LineMeta> current=new ArrayList<>();
            for(String id:priorIds){
                GeometryRepository.LineMeta meta=byId.get(id);
                if(meta==null)return false;
                previous.add(meta);
            }
            for(String id:currentIds){
                GeometryRepository.LineMeta meta=byId.get(id);
                if(meta==null)return false;
                current.add(meta);
            }
            List<PostFinalCumulativePolicy.Page> pages=PostFinalCumulativePolicy.plan(previous,current);
            // PREPARED persisted first; a crash before READY is still recoverable
            // because nextReady verifies the committed historical final date.
            prepare(family,date,pages);
            return markReady(family,date);
        }catch(RuntimeException unavailable){
            android.util.Log.w("QuranHifz","Historical post-final recovery not possible",unavailable);
            return false;
        }
    }


    /** Read-only summary for the weekly recap. Never creates a new reading or credit. */
    static final class Recap {
        final String family,date;
        final int completedPages,totalPages,previousPages;
        final boolean finished;
        Recap(String family,String date,int completedPages,int totalPages,
              int previousPages,boolean finished){
            this.family=family;this.date=date;this.completedPages=completedPages;
            this.totalPages=totalPages;this.previousPages=previousPages;
            this.finished=finished;
        }
    }

    Recap recapFor(String family,String completedSabqiDate,String completedItqanDate){
        if(!SABQI.equals(family)&&!ITQAN.equals(family))
            throw new IllegalArgumentException("Expected final family");
        JSONArray queue=read();
        Recap mostRecentlyFinished=null;
        // Same FIFO order as nextReadyFor(): after several Sundays, the recap
        // must describe the exact reading that its Reprendre action will open.
        // A later READY entry cannot conceal an earlier unfinished reading.
        for(int i=0;i<queue.length();i++){
            JSONObject item=queue.optJSONObject(i);
            if(!family.equals(item.optString("family")))continue;
            String status=item.optString("status");
            String date=item.optString("date");
            // PREPARED alone is not proof that the Sunday final was validated.
            String last=family.equals(SABQI)?completedSabqiDate:completedItqanDate;
            if(!"DONE".equals(status)&&!"READY".equals(status)
                    &&!("PREPARED".equals(status)&&date.equals(last)))continue;
            JSONArray pages=item.optJSONArray("pages");
            if(pages==null||pages.length()<1)
                throw new IllegalStateException("Invalid saved recap pages");
            int completed=item.optInt("position",-1);
            if(completed<0||completed>pages.length())
                throw new IllegalStateException("Invalid saved recap cursor");
            boolean done="DONE".equals(status);
            Recap result=new Recap(family,date,done?pages.length():completed,
                pages.length(),item.optInt("previous",0),done);
            if(!done)return result;
            mostRecentlyFinished=result;
        }
        return mostRecentlyFinished;
    }

    /** Selection only. A PREPARED entry is not usable unless its finale was actually validated. */
    Review nextReady(String completedSabqiDate,String completedItqanDate){
        return findReady(null,completedSabqiDate,completedItqanDate);
    }
    /** A pending Sabqi review must never hide an Itqan review, and vice versa. */
    Review nextReadyFor(String family,String completedSabqiDate,String completedItqanDate){
        if(!SABQI.equals(family)&&!ITQAN.equals(family))
            throw new IllegalArgumentException("Expected final family");
        return findReady(family,completedSabqiDate,completedItqanDate);
    }
    private Review findReady(String targetFamily,String completedSabqiDate,String completedItqanDate){
        JSONArray queue=read();
        for(int i=0;i<queue.length();i++){
            JSONObject item=queue.optJSONObject(i);
            String family=item.optString("family"), date=item.optString("date");
            if(!SABQI.equals(family)&&!ITQAN.equals(family))
                throw new IllegalStateException("Invalid final family");
            if(targetFamily!=null&&!targetFamily.equals(family))continue;
            String status=item.optString("status");
            boolean committed=family.equals(SABQI)?date.equals(completedSabqiDate):date.equals(completedItqanDate);
            if("DONE".equals(status))continue;
            if(!"READY".equals(status)&&(!"PREPARED".equals(status)||!committed))continue;
            JSONArray pages=item.optJSONArray("pages");
            int idx=item.optInt("position",-1);
            if(pages==null||idx<0||idx>=pages.length())
                throw new IllegalStateException("Invalid saved masked review position");
            JSONObject page=pages.optJSONObject(idx);
            if(page==null)throw new IllegalStateException("Invalid saved review page");
            JSONArray raw=page.optJSONArray("lines");
            if(raw==null||raw.length()<1)throw new IllegalStateException("Missing page line ids");
            ArrayList<String> lines=new ArrayList<>();
            for(int k=0;k<raw.length();k++){
                String id=raw.optString(k,"");if(id.isEmpty())throw new IllegalStateException("Empty line id");
                lines.add(id);
            }
            return new Review(family,date,idx,pages.length(),item.optInt("previous",0),
                page.optInt("page",-1),lines);
        }
        return null;
    }
    boolean markReady(String family,String date){
        JSONArray queue=read();
        for(int i=0;i<queue.length();i++){
            JSONObject item=queue.optJSONObject(i);
            if(!family.equals(item.optString("family"))||!date.equals(item.optString("date")))continue;
            if("DONE".equals(item.optString("status")))return true;
            try{item.put("status","READY");}catch(Exception e){throw new IllegalStateException(e);}
            return prefs.edit().putString(KEY,queue.toString()).commit();
        }
        return false;
    }
    boolean advance(Review expected,String sabqiLast,String itqanLast) {
        if(expected==null)throw new IllegalArgumentException("Expected review state required");
        JSONArray queue=read();
        for(int i=0;i<queue.length();i++){
            JSONObject item=queue.optJSONObject(i);
            if(!expected.family.equals(item.optString("family"))||!expected.date.equals(item.optString("date")))continue;
            String status=item.optString("status");
            boolean committed=expected.family.equals(SABQI)
                ?expected.date.equals(sabqiLast):expected.date.equals(itqanLast);
            if(!"READY".equals(status)&&(!"PREPARED".equals(status)||!committed))return false;
            if(expected.position!=item.optInt("position",-1))return false; // double tap guard
            try {
                int next=expected.position+1;
                if(next>=item.getJSONArray("pages").length())item.put("status","DONE");
                item.put("position",next);
            }catch(Exception invalid){throw new IllegalStateException(invalid);}
            return prefs.edit().putString(KEY,queue.toString()).commit();
        }
        return false;
    }
}
