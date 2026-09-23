package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
    private final SharedPreferences p;

    DashboardLedger(Context context){
        p=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        if(!p.contains(START))p.edit().putString(START,HifzClock.today().toString()).apply();
    }

    LocalDate started(){return LocalDate.parse(p.getString(START,HifzClock.today().toString()));}

    void capture(HifzPrefs prefs){ capture(prefs, HifzClock.today()); }

    void capture(HifzPrefs prefs, LocalDate today){
        upsert(prefs.lastSabqiDate(),HifzSessionActivity.SABQI,prefs.lastSabqiLabel(),today);
        upsert(prefs.lastSabqiTodayReviewDate(),HifzSessionActivity.SABQI_TODAY_REVIEW,prefs.lastSabqiTodayReviewLabel(),today);
        upsert(prefs.lastItqanDate(),HifzSessionActivity.ITQAN,prefs.lastItqanLabel(),today);
        upsert(prefs.lastStabilizationSnowballEveningDate(),HifzSessionActivity.RECENT_SABQI_REVIEW,"Consolidation · boule de neige",today);
        upsert(prefs.lastLearningSnowballEveningDate(),HifzSessionActivity.LEARNING_CONSOLIDATION,"Renforcement · boule de neige",today);
        upsert(prefs.lastRecentSabqiReviewDate(),HifzSessionActivity.CONSOLIDATION_FINAL,prefs.lastRecentSabqiReviewLabel(),today);
        upsert(prefs.lastLearningConsolidationDate(),HifzSessionActivity.LEARNING_FINAL,prefs.lastLearningConsolidationLabel(),today);
        upsert(prefs.lastMurajaahDate(),HifzSessionActivity.MURAJAAH,prefs.lastMurajaahLabel(),today);
        upsert(prefs.lastActiveMurajaahDate(),HifzSessionActivity.MURAJAAH_ACTIVE,prefs.lastActiveMurajaahLabel(),today);
    }

    Record find(LocalDate date,String type){
        for(Record r:records())if(r.date.equals(date)&&r.type.equals(type))return r;
        return null;
    }

    List<LocalDate> completedDates(String type, LocalDate fromInclusive, LocalDate throughInclusive){
        ArrayList<LocalDate> out=new ArrayList<>();
        if(type==null||fromInclusive==null||throughInclusive==null||throughInclusive.isBefore(fromInclusive))return out;
        for(Record r:records()){
            if(!type.equals(r.type))continue;
            if(r.date.isBefore(fromInclusive)||r.date.isAfter(throughInclusive))continue;
            if(!out.contains(r.date))out.add(r.date);
        }
        return out;
    }

    private void upsert(String dateText,String type,String label,LocalDate today){
        if(dateText==null||dateText.isEmpty())return;
        LocalDate date;
        try{date=LocalDate.parse(dateText);}catch(RuntimeException e){return;}
        List<Record> next=mergeRecords(records(),date,type,label,today);
        JSONArray array=new JSONArray();
        try{
            for(Record r:next){JSONObject o=new JSONObject();o.put("date",r.date.toString());o.put("type",r.type);o.put("label",r.label);array.put(o);}
            p.edit().putString(KEY,array.toString()).apply();
        }catch(Exception ignored){}
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

    private List<Record> records(){
        ArrayList<Record> out=new ArrayList<>();
        try{
            JSONArray array=new JSONArray(p.getString(KEY,"[]"));
            for(int i=0;i<array.length();i++){
                JSONObject o=array.getJSONObject(i);
                out.add(new Record(LocalDate.parse(o.getString("date")),o.getString("type"),o.optString("label","")));
            }
        }catch(Exception ignored){}
        return out;
    }
}
