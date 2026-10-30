package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;
import com.quransafeguard.hifz.core.VerseRef;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Isolated checkpoint for visible rereading, then complete-page masked recall.
 * Never writes Hifz credits, learning boundaries or the validated active cursor.
 */
final class ActiveReviewCorrectionStore {
    private static final String PREFS = "quran_hifz_preview_v1";
    private static final String KEY_REVEALED = "activeReviewRevealedPagesV1";
    private static final String KEY_STATE = "activeReviewCorrectionV2";
    private final SharedPreferences prefs;

    ActiveReviewCorrectionStore(Context context) {
        this(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE));
    }
    ActiveReviewCorrectionStore(SharedPreferences prefs) {
        if (prefs == null) throw new IllegalArgumentException("Missing correction store");
        this.prefs = prefs;
    }

    static final class State {
        final String date;
        final VerseRef start, end;
        final long activeElapsedMs;
        final List<Integer> pages;
        final int originPage; // Original current page, before visible/masked replay.
        final int pageIndex;
        final String stage; // READ, RECALL, DONE
        final boolean assisted;
        State(String date, VerseRef start, VerseRef end, long elapsed,
              List<Integer> pages, int originPage, int pageIndex, String stage, boolean assisted) {
            this.date=date;this.start=start;this.end=end;activeElapsedMs=elapsed;
            this.pages=Collections.unmodifiableList(new ArrayList<>(pages));
            this.originPage=originPage;this.pageIndex=pageIndex;this.stage=stage;this.assisted=assisted;
            if(date==null||start==null||end==null||elapsed<0
                ||!date.equals(LocalDate.parse(date).toString())
                ||pages.isEmpty()||pages.size()>604||originPage<1||originPage>604
                ||pageIndex<0||pageIndex>=pages.size()
                ||(!"READ".equals(stage)&&!"RECALL".equals(stage)&&!"DONE".equals(stage))
                ||new LinkedHashSet<>(pages).size()!=pages.size())
                throw new IllegalArgumentException("Invalid correction state");
            for(Integer page:pages)
                if(page==null||page<1||page>604)throw new IllegalArgumentException("Invalid page");
        }
        int page(){return pages.get(pageIndex);}
        State move(int index,String phase,boolean help){
            return new State(date,start,end,activeElapsedMs,pages,originPage,index,phase,help);
        }
    }

    State pending() {
        String raw=prefs.getString(KEY_STATE,"");
        if(raw==null||raw.isEmpty())return null;
        try {
            JSONObject doc=new JSONObject(raw);
            JSONArray values=doc.getJSONArray("pages");
            if(values.length()<1||values.length()>604)
                throw new IllegalArgumentException("Invalid page count");
            List<Integer> pages=new ArrayList<>();
            for(int i=0;i<values.length();i++)pages.add(values.getInt(i));
            return new State(doc.getString("date"),
                GeometryRepository.parseVerse(doc.getString("start")),
                GeometryRepository.parseVerse(doc.getString("end")),
                doc.getLong("activeElapsedMs"),pages,
                doc.getInt("originPage"),doc.getInt("pageIndex"),doc.getString("stage"),
                doc.optBoolean("assisted",false));
        }catch(Exception invalid){
            // Never silently delete a damaged checkpoint or advance the Hifz cursor.
            throw new IllegalStateException("État de reprise des pages à vérifier ; données conservées.",invalid);
        }
    }

    boolean begin(String date,VerseRef start,VerseRef end,long elapsed,List<Integer> pages,int originPage){
        if(pending()!=null||pages==null||pages.isEmpty())return false;
        return save(new State(date,start,end,elapsed,pages,originPage,0,"READ",false));
    }
    boolean pageReread(State state){
        return state!=null&&"READ".equals(state.stage)
            &&save(state.move(state.pageIndex,"RECALL",false));
    }
    boolean revealDuringControl(State state){
        return state!=null&&"RECALL".equals(state.stage)
            &&save(state.move(state.pageIndex,"RECALL",true));
    }
    boolean restartPage(State state){
        return state!=null&&"RECALL".equals(state.stage)
            &&save(state.move(state.pageIndex,"READ",false));
    }
    boolean pageRecalled(State state){
        if(state==null||!"RECALL".equals(state.stage)||state.assisted)return false;
        int next=state.pageIndex+1;
        return next<state.pages.size()
            ?save(state.move(next,"READ",false))
            :save(state.move(state.pageIndex,"DONE",false));
    }

    private boolean save(State state){
        try{
            JSONObject doc=new JSONObject();
            doc.put("date",state.date);doc.put("start",state.start.toString());
            doc.put("end",state.end.toString());doc.put("activeElapsedMs",state.activeElapsedMs);
            doc.put("originPage",state.originPage);
            doc.put("pageIndex",state.pageIndex);doc.put("stage",state.stage);
            doc.put("assisted",state.assisted);
            JSONArray array=new JSONArray();
            for(Integer page:state.pages)array.put(page);
            doc.put("pages",array);
            return prefs.edit().putString(KEY_STATE,doc.toString()).commit();
        }catch(Exception invalid){return false;}
    }

    Set<Integer> revealed(String date,VerseRef start){
        try{
            String raw=prefs.getString(KEY_REVEALED,"");
            if(raw==null||raw.isEmpty())return Collections.emptySet();
            JSONObject doc=new JSONObject(raw);
            if(!date.equals(doc.optString("date"))||!start.toString().equals(doc.optString("start")))
                return Collections.emptySet();
            JSONArray values=doc.getJSONArray("pages");
            LinkedHashSet<Integer> pages=new LinkedHashSet<>();
            for(int i=0;i<values.length();i++){
                int page=values.getInt(i);
                if(page<1||page>604||!pages.add(page))
                    throw new IllegalArgumentException("Malformed revealed page list");
            }
            return pages;
        }catch(Exception invalid){
            throw new IllegalStateException("Revealed-page checkpoint requires review",invalid);
        }
    }
    boolean recordReveal(String date,VerseRef start,int page){
        if(date==null||start==null||page<1||page>604)return false;
        try{
            Set<Integer> pages=new LinkedHashSet<>(revealed(date,start));
            pages.add(page);
            JSONObject doc=new JSONObject();
            doc.put("date",date);doc.put("start",start.toString());
            JSONArray array=new JSONArray();
            for(Integer value:pages)array.put(value);
            doc.put("pages",array);
            return prefs.edit().putString(KEY_REVEALED,doc.toString()).commit();
        }catch(Exception invalid){return false;}
    }
    boolean clear(){
        return prefs.edit().remove(KEY_STATE).remove(KEY_REVEALED).commit();
    }
}
