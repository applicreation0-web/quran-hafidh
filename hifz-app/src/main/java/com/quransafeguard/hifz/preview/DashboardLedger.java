package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Dashboard-only history. It mirrors already-committed Hifz session results and never changes
 * Hifz cursors or progression. Capturing on home resume keeps the weekly dashboard truthful.
 */
final class DashboardLedger {
    static final class Record {
        final LocalDate date;
        final String type;
        final String label;
        Record(LocalDate date,String type,String label){this.date=date;this.type=type;this.label=label;}
    }

    private static final String PREFS="quran_hifz_dashboard_ledger_v1";
    private static final String KEY="records";
    private static final String START="started";
    // Display history still rolls over after 120 days; completion receipts must not.
    private static final String COMPLETED="completed_session_keys_v1";
    private final SharedPreferences p;
    // Avoid parsing the whole durable history for every date in nextDueCadence().
    private String cachedReceiptsJson;
    private LinkedHashSet<String> cachedReceipts;
    private String cachedDisplayJson;
    private List<Record> cachedDisplay;


    DashboardLedger(Context context){
        p=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        if(!p.contains(START))p.edit().putString(START,HifzClock.today().toString()).apply();
    }

    LocalDate started(){return LocalDate.parse(p.getString(START,HifzClock.today().toString()));}

    void capture(HifzPrefs prefs){ capture(prefs, HifzClock.today()); }

    /** One atomic SharedPreferences edit per screen refresh, not nine separate writes. */
    void capture(HifzPrefs prefs, LocalDate today){
        List<Record> display=records();
        LinkedHashSet<String> completed=mergeCompletionKeys(completionKeys(),display,null,null);
        display=captureOne(display,completed,prefs.lastSabqiDate(),HifzSessionActivity.SABQI,prefs.lastSabqiLabel(),today);
        display=captureOne(display,completed,prefs.lastSabqiTodayReviewDate(),HifzSessionActivity.SABQI_TODAY_REVIEW,prefs.lastSabqiTodayReviewLabel(),today);
        display=captureOne(display,completed,prefs.lastItqanDate(),HifzSessionActivity.ITQAN,prefs.lastItqanLabel(),today);
        display=captureOne(display,completed,prefs.lastStabilizationSnowballEveningDate(),HifzSessionActivity.RECENT_SABQI_REVIEW,"Consolidation · boule de neige",today);
        display=captureOne(display,completed,prefs.lastLearningSnowballEveningDate(),HifzSessionActivity.LEARNING_CONSOLIDATION,"Renforcement · boule de neige",today);
        display=captureOne(display,completed,prefs.lastRecentSabqiReviewDate(),HifzSessionActivity.CONSOLIDATION_FINAL,prefs.lastRecentSabqiReviewLabel(),today);
        display=captureOne(display,completed,prefs.lastLearningConsolidationDate(),HifzSessionActivity.LEARNING_FINAL,prefs.lastLearningConsolidationLabel(),today);
        display=captureOne(display,completed,prefs.lastMurajaahDate(),HifzSessionActivity.MURAJAAH,prefs.lastMurajaahLabel(),today);
        display=captureOne(display,completed,prefs.lastActiveMurajaahDate(),HifzSessionActivity.MURAJAAH_ACTIVE,prefs.lastActiveMurajaahLabel(),today);
        JSONArray rows=new JSONArray(), receipts=new JSONArray();
        try {
            for(Record r:display){
                rows.put(new JSONObject().put("date",r.date.toString())
                    .put("type",r.type).put("label",r.label));
            }
            for(String key:completed)receipts.put(key);
        }catch(Exception invalid){
            throw new IllegalStateException("Unable to serialize validated session receipts",invalid);
        }
        String rowJson=rows.toString(), receiptJson=receipts.toString();
        if(rowJson.equals(p.getString(KEY,"[]")) &&
                receiptJson.equals(p.getString(COMPLETED,"[]")))return;
        // For app process death, durable receipts must be on disk before the UI proceeds.
        if(!p.edit().putString(KEY,rowJson).putString(COMPLETED,receiptJson).commit())
            throw new IllegalStateException("Unable to persist validated session receipts");
        cachedDisplayJson=rowJson; cachedDisplay=new ArrayList<>(display);
        cachedReceiptsJson=receiptJson; cachedReceipts=new LinkedHashSet<>(completed);
    }

    private static List<Record> captureOne(List<Record> display, Set<String> completed,
            String dateText,String type,String label,LocalDate today){
        if(dateText==null||dateText.isEmpty())return display;
        LocalDate date;
        try{date=LocalDate.parse(dateText);}catch(RuntimeException invalid){return display;}
        if(type==null||type.isEmpty())return display;
        completed.add(completionKey(date,type));
        return mergeRecords(display,date,type,label,today);
    }

    Record find(LocalDate date,String type){
        if(date==null||type==null)return null;
        for(Record r:records())if(r.date.equals(date)&&r.type.equals(type))return r;
        // A display row expiring must never invalidate a genuinely earned completion.
        return containsCompletion(completionKeys(),date,type)?new Record(date,type,""):null;
    }

    List<LocalDate> completedDates(String type, LocalDate fromInclusive, LocalDate throughInclusive){
        ArrayList<LocalDate> out=new ArrayList<>();
        if(type==null||fromInclusive==null||throughInclusive==null||throughInclusive.isBefore(fromInclusive))return out;
        for(Record r:records()){
            if(!type.equals(r.type))continue;
            if(r.date.isBefore(fromInclusive)||r.date.isAfter(throughInclusive))continue;
            if(!out.contains(r.date))out.add(r.date);
        }
        for(String key:completionKeys()){
            int split=key.indexOf('|');
            if(split<=0||!type.equals(key.substring(split+1)))continue;
            LocalDate date;
            try{date=LocalDate.parse(key.substring(0,split));}
            catch(RuntimeException invalid){continue;}
            if(!date.isBefore(fromInclusive)&&!date.isAfter(throughInclusive)&&!out.contains(date))out.add(date);
        }
        return out;
    }

    static List<Record> mergeRecords(List<Record> items,LocalDate date,String type,String label,LocalDate today){
        ArrayList<Record> next=new ArrayList<>();
        LocalDate cutoff=today.minusDays(120);
        if(items!=null){
            for(Record r:items){
                if(r==null||r.date==null||r.date.isBefore(cutoff))continue;
                if(r.date.equals(date)&&r.type.equals(type))continue;
                next.add(r);
            }
        }
        if(date!=null&&!date.isBefore(cutoff))next.add(new Record(date,type,label==null?"":label));
        return next;
    }

    private static String completionKey(LocalDate date,String type){
        return date.toString()+"|"+type;
    }

    static boolean containsCompletion(Set<String> keys,LocalDate date,String type){
        return keys!=null&&date!=null&&type!=null&&keys.contains(completionKey(date,type));
    }

    static LinkedHashSet<String> mergeCompletionKeys(Set<String> saved,List<Record> display,
            LocalDate date,String type){
        LinkedHashSet<String> out=new LinkedHashSet<>();
        if(saved!=null)out.addAll(saved);
        if(display!=null)for(Record r:display){
            if(r!=null&&r.date!=null&&r.type!=null&&!r.type.isEmpty())
                out.add(completionKey(r.date,r.type));
        }
        if(date!=null&&type!=null&&!type.isEmpty())out.add(completionKey(date,type));
        return out;
    }

    private LinkedHashSet<String> completionKeys(){
        String raw=p.getString(COMPLETED,"[]");
        if(cachedReceipts!=null && raw.equals(cachedReceiptsJson))
            return new LinkedHashSet<>(cachedReceipts);
        LinkedHashSet<String> out=new LinkedHashSet<>();
        try{
            JSONArray stored=new JSONArray(raw);
            for(int i=0;i<stored.length();i++){
                String key=stored.getString(i);
                int delimiter=key.indexOf('|');
                if(delimiter<=0||delimiter==key.length()-1)throw new IllegalArgumentException("Invalid receipt");
                LocalDate.parse(key.substring(0,delimiter));
                out.add(key);
            }
        }catch(Exception malformed){
            throw new IllegalStateException("Unusable durable completion ledger",malformed);
        }
        cachedReceiptsJson=raw;cachedReceipts=new LinkedHashSet<>(out);
        return out;
    }

    private List<Record> records(){
        String raw=p.getString(KEY,"[]");
        if(cachedDisplay!=null && raw.equals(cachedDisplayJson))
            return new ArrayList<>(cachedDisplay);
        ArrayList<Record> out=new ArrayList<>();
        try{
            JSONArray array=new JSONArray(raw);
            for(int i=0;i<array.length();i++){
                JSONObject o=array.getJSONObject(i);
                out.add(new Record(LocalDate.parse(o.getString("date")),
                    o.getString("type"),o.optString("label","")));
            }
        }catch(Exception ignored){} // display history can be incomplete; never alter durable proofs
        cachedDisplayJson=raw;cachedDisplay=new ArrayList<>(out);
        return out;
    }
}
