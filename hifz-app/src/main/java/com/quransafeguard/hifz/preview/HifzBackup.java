package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Personal, offline Hifz export/import.
 * V2 preserves both the schema-6 progress AND the separate M9 completion ledger
 * and legacy J10 review history. V1 imports remain readable, without inventing
 * receipts that the old exporter never stored.
 */
final class HifzBackup {
    private static final String PREFS_NAME = "quran_hifz_preview_v1";
    private static final String DASHBOARD_NAME = "quran_hifz_dashboard_ledger_v1";
    private static final String MAGIC = "quran-hifz-backup";
    private static final int VERSION = 2;

    private HifzBackup() {}

    static JSONObject export(Context context) throws JSONException {
        JSONObject root = new JSONObject();
        root.put("magic", MAGIC);
        root.put("version", VERSION);
        root.put("exportedAtEpochMs", System.currentTimeMillis());
        root.put("values", encoded(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getAll()));
        root.put("dashboard", encoded(context.getSharedPreferences(DASHBOARD_NAME, Context.MODE_PRIVATE).getAll()));
        root.put("j10", encoded(context.getSharedPreferences(J10ReviewStore.NAME, Context.MODE_PRIVATE).getAll()));
        return root;
    }

    /** Validate ALL imported stores and types before any destructive write. */
    static int restore(Context context, JSONObject root) throws JSONException {
        if (!MAGIC.equals(root.optString("magic")))
            throw new JSONException("Fichier de sauvegarde non reconnu.");
        int version=root.optInt("version", -1);
        if (version!=1 && version!=VERSION)
            throw new JSONException("Version de sauvegarde non prise en charge.");
        JSONObject mainValues=root.getJSONObject("values");
        JSONObject ledgerValues=version==VERSION ? root.getJSONObject("dashboard") : new JSONObject();
        JSONObject j10Values=version==VERSION ? root.getJSONObject("j10") : new JSONObject();
        checkValues(mainValues);
        checkValues(ledgerValues);
        checkValues(j10Values);
        // Sanity check the M9 completion list rather than importing invented dates.
        if (ledgerValues.has("completed_session_keys_v1")) {
            JSONObject e=ledgerValues.getJSONObject("completed_session_keys_v1");
            JSONArray keys=new JSONArray(e.getString("v"));
            for(int i=0;i<keys.length();i++){
                String item=keys.getString(i);
                int at=item.indexOf('|');
                if(at<=0 || at==item.length()-1)
                    throw new JSONException("Reçu de validation M9 invalide.");
                try {java.time.LocalDate.parse(item.substring(0,at));}
                catch(RuntimeException invalid){throw new JSONException("Date de validation M9 invalide.");}
            }
        }
        SharedPreferences main=context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences ledger=context.getSharedPreferences(DASHBOARD_NAME, Context.MODE_PRIVATE);
        SharedPreferences j10=context.getSharedPreferences(J10ReviewStore.NAME, Context.MODE_PRIVATE);
        synchronized (J10ReviewStore.LEGACY_STORE_LOCK) {
            JSONObject oldMain=encoded(main.getAll());
            JSONObject oldLedger=encoded(ledger.getAll());
            JSONObject oldJ10=encoded(j10.getAll());
            try {
                replace(main,mainValues);
                replace(ledger,ledgerValues);
                replace(j10,j10Values);
            }catch(JSONException invalid){
                // Cross-file Android preferences are not an atomic database.
                // Best-effort rollback rather than silently accepting half a restore.
                boolean recovered=true;
                try{replace(main,oldMain);}catch(JSONException failure){recovered=false;}
                try{replace(ledger,oldLedger);}catch(JSONException failure){recovered=false;}
                try{replace(j10,oldJ10);}catch(JSONException failure){recovered=false;}
                if(!recovered)throw new JSONException("Échec de restauration et de reprise; conserver le fichier original.");
                throw invalid;
            }
        }
        return mainValues.length();
    }

    private static JSONObject encoded(Map<String,?> all) throws JSONException {
        JSONObject out=new JSONObject();
        for(Map.Entry<String,?> e:all.entrySet())out.put(e.getKey(),encode(e.getValue()));
        return out;
    }

    private static void checkValues(JSONObject values) throws JSONException {
        Iterator<String> keys=values.keys();
        while(keys.hasNext()){
            String key=keys.next();
            if(key.isEmpty())throw new JSONException("Clé de sauvegarde vide.");
            JSONObject entry=values.getJSONObject(key);
            String type=entry.getString("t");
            switch(type){
                case "b":entry.getBoolean("v");break;
                case "i":entry.getInt("v");break;
                case "l":entry.getLong("v");break;
                case "f": {
                    double n=entry.getDouble("v");
                    if(Double.isNaN(n)||Double.isInfinite(n))throw new JSONException("Vitesse invalide.");
                    break;
                }
                case "s":entry.getString("v");break;
                case "set": {
                    JSONArray a=entry.getJSONArray("v");
                    for(int i=0;i<a.length();i++)a.getString(i);
                    break;
                }
                default:throw new JSONException("Type de sauvegarde inconnu : "+type);
            }
        }
    }

    private static void replace(SharedPreferences store, JSONObject values) throws JSONException {
        SharedPreferences.Editor editor=store.edit().clear();
        Iterator<String> keys=values.keys();
        while(keys.hasNext()){
            String key=keys.next();
            apply(editor,key,values.getJSONObject(key));
        }
        if(!editor.commit())throw new JSONException("Échec de l'écriture de la sauvegarde.");
    }

    private static JSONObject encode(Object value) throws JSONException {
        JSONObject entry = new JSONObject();
        if (value instanceof Boolean) {
            entry.put("t", "b").put("v", value);
        } else if (value instanceof Integer) {
            entry.put("t", "i").put("v", value);
        } else if (value instanceof Long) {
            entry.put("t", "l").put("v", value);
        } else if (value instanceof Float) {
            entry.put("t", "f").put("v", (double)(Float)value);
        } else if (value instanceof String) {
            entry.put("t", "s").put("v", value);
        } else if (value instanceof Set) {
            JSONArray array = new JSONArray();
            for (Object item : (Set<?>)value) array.put(item.toString());
            entry.put("t", "set").put("v", array);
        } else {
            throw new JSONException("Type de préférence non pris en charge : "+value.getClass());
        }
        return entry;
    }

    private static void apply(SharedPreferences.Editor editor,String key,JSONObject entry) throws JSONException {
        switch(entry.getString("t")){
            case "b": editor.putBoolean(key,entry.getBoolean("v"));break;
            case "i": editor.putInt(key,entry.getInt("v"));break;
            case "l": editor.putLong(key,entry.getLong("v"));break;
            case "f": editor.putFloat(key,(float)entry.getDouble("v"));break;
            case "s": editor.putString(key,entry.getString("v"));break;
            case "set": {
                JSONArray array=entry.getJSONArray("v");
                LinkedHashSet<String> items=new LinkedHashSet<>();
                for(int i=0;i<array.length();i++)items.add(array.getString(i));
                editor.putStringSet(key,items);
                break;
            }
            default:throw new JSONException("Type de sauvegarde inconnu.");
        }
    }
}
