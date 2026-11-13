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
    private final String revealedKey;
    private final String stateKey;

    ActiveReviewCorrectionStore(Context context) {
        this(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE));
    }
    ActiveReviewCorrectionStore(SharedPreferences prefs) {
        this(prefs, "");
    }
    ActiveReviewCorrectionStore(SharedPreferences prefs, String scope) {
        if (prefs == null) throw new IllegalArgumentException("Missing correction store");
        this.prefs = prefs;
        revealedKey = KEY_REVEALED + scope;
        stateKey = KEY_STATE + scope;
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
                ||("DONE".equals(stage)&&pageIndex!=pages.size()-1)
                ||(assisted&&!"RECALL".equals(stage))
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
        String raw=prefs.getString(stateKey,"");
        if(raw==null||raw.isEmpty())return null;
        try {
            JSONObject doc=new JSONObject(raw);
            JSONArray values=doc.getJSONArray("pages");
            if(values.length()<1||values.length()>604)
                throw new IllegalArgumentException("Invalid page count");
            List<Integer> pages=new ArrayList<>();
            for(int i=0;i<values.length();i++)pages.add(strictInt(values.get(i)));
            return new State(doc.getString("date"),
                GeometryRepository.parseVerse(doc.getString("start")),
                GeometryRepository.parseVerse(doc.getString("end")),
                strictLong(doc.get("activeElapsedMs")),pages,
                strictInt(doc.get("originPage")),strictInt(doc.get("pageIndex")),doc.getString("stage"),
                strictBoolean(doc.get("assisted")));
        }catch(Exception invalid){
            // Never silently delete a damaged checkpoint or advance the Hifz cursor.
            throw new IllegalStateException("État de reprise des pages à vérifier ; données conservées.",invalid);
        }
    }

    /** JSONObject.getInt/getLong coerce fractional numbers and numeric strings.
     * Disk checkpoints contain integral JSON numbers only: fail closed on damage,
     * retaining the original bytes instead of silently changing page/time/index. */
    // Assistance is always written as a boolean in V2. Missing/coerced values
    // must not erase a recorded reveal after a damaged backup is restored.
    private static boolean strictBoolean(Object value){
        if(!(value instanceof Boolean))
            throw new IllegalArgumentException("Non-boolean checkpoint assistance");
        return (Boolean)value;
    }

    private static long strictLong(Object value){
        if(!(value instanceof Integer)&&!(value instanceof Long))
            throw new IllegalArgumentException("Non-integral checkpoint number");
        return ((Number)value).longValue();
    }
    private static int strictInt(Object value){
        long number=strictLong(value);
        if(number<Integer.MIN_VALUE||number>Integer.MAX_VALUE)
            throw new IllegalArgumentException("Checkpoint number out of range");
        return (int)number;
    }

    boolean begin(String date,VerseRef start,VerseRef end,long elapsed,List<Integer> pages,int originPage){
        if(pending()!=null||pages==null||pages.isEmpty())return false;
        return save(new State(date,start,end,elapsed,pages,originPage,0,"READ",false));
    }
    boolean pageReread(State state){
        return isCurrent(state)&&"READ".equals(state.stage)
            &&save(state.move(state.pageIndex,"RECALL",false));
    }
    boolean revealDuringControl(State state){
        return isCurrent(state)&&"RECALL".equals(state.stage)
            &&save(state.move(state.pageIndex,"RECALL",true));
    }
    boolean restartPage(State state){
        return isCurrent(state)&&"RECALL".equals(state.stage)
            &&save(state.move(state.pageIndex,"READ",false));
    }
    boolean pageRecalled(State state){
        if(!isCurrent(state)||!"RECALL".equals(state.stage)||state.assisted)return false;
        int next=state.pageIndex+1;
        return next<state.pages.size()
            ?save(state.move(next,"READ",false))
            :save(state.move(state.pageIndex,"DONE",false));
    }

    private boolean isCurrent(State state){
        State saved=pending();
        return state!=null&&saved!=null&&state.date.equals(saved.date)
            &&state.start.equals(saved.start)&&state.end.equals(saved.end)
            &&state.activeElapsedMs==saved.activeElapsedMs&&state.pages.equals(saved.pages)
            &&state.originPage==saved.originPage&&state.pageIndex==saved.pageIndex
            &&state.stage.equals(saved.stage)&&state.assisted==saved.assisted;
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
            return prefs.edit().putString(stateKey,doc.toString()).commit();
        }catch(Exception invalid){return false;}
    }

    Set<Integer> revealed(String date,VerseRef start){
        try{
            String raw=prefs.getString(revealedKey,"");
            if(raw==null||raw.isEmpty())return Collections.emptySet();
            JSONObject doc=new JSONObject(raw);
            if(!date.equals(doc.optString("date"))||!start.toString().equals(doc.optString("start")))
                return Collections.emptySet();
            JSONArray values=doc.getJSONArray("pages");
            LinkedHashSet<Integer> pages=new LinkedHashSet<>();
            for(int i=0;i<values.length();i++){
                int page=strictInt(values.get(i));
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
            String raw=prefs.getString(revealedKey,"");
            if(raw!=null&&!raw.isEmpty()){
                JSONObject existing=new JSONObject(raw);
                if(!date.equals(existing.getString("date"))
                    ||!start.toString().equals(existing.getString("start")))return false;
            }
            Set<Integer> pages=new LinkedHashSet<>(revealed(date,start));
            pages.add(page);
            JSONObject doc=new JSONObject();
            doc.put("date",date);doc.put("start",start.toString());
            JSONArray array=new JSONArray();
            for(Integer value:pages)array.put(value);
            doc.put("pages",array);
            return prefs.edit().putString(revealedKey,doc.toString()).commit();
        }catch(Exception invalid){return false;}
    }

    /** Restore the original date even when force-stop preceded the replay's begin(). */
    String revealedSessionDate(VerseRef start,String validatedDate){
        String raw=prefs.getString(revealedKey,"");
        if(raw==null||raw.isEmpty())return null;
        try{
            JSONObject doc=new JSONObject(raw);
            String date=LocalDate.parse(doc.getString("date")).toString();
            // A receipt may have committed just before process death and cleanup.
            if(date.equals(validatedDate))return date;
            if(!start.toString().equals(doc.getString("start")))
                throw new IllegalArgumentException("Revealed-page cursor mismatch");
            if(revealed(date,start).isEmpty())throw new IllegalArgumentException("Empty reveal checkpoint");
            return date;
        }catch(Exception invalid){
            throw new IllegalStateException("Revealed-page date requires review",invalid);
        }
    }
    boolean clear(){
        return clear(prefs.edit()).commit();
    }

    SharedPreferences.Editor clear(SharedPreferences.Editor editor) {
        return editor.remove(stateKey).remove(revealedKey);
    }
}
