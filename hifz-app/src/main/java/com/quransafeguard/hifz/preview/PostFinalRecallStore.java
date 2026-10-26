package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
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
    /** Selection only. A PREPARED entry is not usable unless its finale was actually validated. */
    Review nextReady(String completedSabqiDate,String completedItqanDate){
        JSONArray queue=read();
        for(int i=0;i<queue.length();i++){
            JSONObject item=queue.optJSONObject(i);
            String family=item.optString("family"), date=item.optString("date");
            if(!SABQI.equals(family)&&!ITQAN.equals(family))
                throw new IllegalStateException("Invalid final family");
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
