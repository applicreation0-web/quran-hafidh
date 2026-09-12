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
        if(!p.contains(START))p.edit().putString(START,LocalDate.now().toString()).apply();
    }

    LocalDate started(){return LocalDate.parse(p.getString(START,LocalDate.now().toString()));}

    void capture(HifzPrefs prefs){
        upsert(prefs.lastSabqiDate(),HifzSessionActivity.SABQI,prefs.lastSabqiLabel());
        upsert(prefs.lastItqanDate(),HifzSessionActivity.ITQAN,prefs.lastItqanLabel());
        upsert(prefs.lastMurajaahDate(),HifzSessionActivity.MURAJAAH,prefs.lastMurajaahLabel());
    }

    Record find(LocalDate date,String type){
        for(Record r:records())if(r.date.equals(date)&&r.type.equals(type))return r;
        return null;
    }

    private void upsert(String dateText,String type,String label){
        if(dateText==null||dateText.isEmpty())return;
        LocalDate date;
        try{date=LocalDate.parse(dateText);}catch(RuntimeException e){return;}
        List<Record> items=records();
        boolean same=false;
        ArrayList<Record> next=new ArrayList<>();
        for(Record r:items){
            if(r.date.equals(date)&&r.type.equals(type)){
                if(r.label.equals(label))same=true;
                next.add(new Record(date,type,label));
            }else if(!r.date.isBefore(LocalDate.now().minusDays(120)))next.add(r);
        }
        if(same)return;
        boolean exists=false;
        for(Record r:next)if(r.date.equals(date)&&r.type.equals(type)){exists=true;break;}
        if(!exists)next.add(new Record(date,type,label));
        JSONArray array=new JSONArray();
        try{
            for(Record r:next){JSONObject o=new JSONObject();o.put("date",r.date.toString());o.put("type",r.type);o.put("label",r.label);array.put(o);}
            p.edit().putString(KEY,array.toString()).apply();
        }catch(Exception ignored){}
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
