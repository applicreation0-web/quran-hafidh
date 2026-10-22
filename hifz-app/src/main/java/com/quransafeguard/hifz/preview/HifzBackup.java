package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
    private static final String LEGACY_GATES = "hifz_preview_session_gates";
    private static final String MAGIC = "quran-hifz-backup";
    private static final int VERSION = 2;
    private static final String JOURNAL_NAME = "quran_hifz_restore_journal_v1";
    private static final String JOURNAL_KEY = "pending_restore";
    private static final String COVERAGE_KEY = "coverage_start_date_v1";
    private static final Object RESTORE_LOCK = new Object();
    // Instrumentation only; never set in production.
    static volatile String failAfterStoreForTest;

    private HifzBackup() {}

    static JSONObject export(Context context) throws JSONException {
        recoverIfNecessary(context);
        JSONObject root = new JSONObject();
        root.put("magic", MAGIC);
        root.put("version", VERSION);
        root.put("exportedAtEpochMs", System.currentTimeMillis());
        root.put("values", encoded(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getAll()));
        root.put("dashboard", encoded(context.getSharedPreferences(DASHBOARD_NAME, Context.MODE_PRIVATE).getAll()));
        root.put("j10", encoded(context.getSharedPreferences(J10ReviewStore.NAME, Context.MODE_PRIVATE).getAll()));
        root.put("legacy_gates", encoded(context.getSharedPreferences(LEGACY_GATES, Context.MODE_PRIVATE).getAll()));
        return root;
    }


    private static JSONObject typed(String value) throws JSONException {
        return new JSONObject().put("t","s").put("v",value);
    }

    /** V1 never contained session receipts. Its pre-import attendance is UNKNOWN. */
    private static JSONObject legacyDashboard() throws JSONException {
        String today=HifzClock.today().toString();
        return new JSONObject().put("started",typed(today))
            .put(COVERAGE_KEY,typed(today))
            .put("completed_session_keys_v1",typed("[]"))
            .put("records",typed("[]"));
    }

    private static JSONObject withCoverage(JSONObject dashboard) throws JSONException {
        if(!dashboard.has(COVERAGE_KEY))
            dashboard.put(COVERAGE_KEY,typed(HifzClock.today().toString()));
        return dashboard;
    }

    private static String sha256(String value) {
        try {
            byte[] digest=MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result=new StringBuilder(digest.length*2);
            for(byte b:digest)result.append(String.format(java.util.Locale.ROOT,"%02x",b&0xff));
            return result.toString();
        }catch(java.security.NoSuchAlgorithmException impossible){
            throw new IllegalStateException("SHA-256 unavailable",impossible);
        }
    }

    /**
     * Roll back a pending multi-store import at process start, before any Activity
     * or HifzPrefs schema migration. If killed during rollback, the journal
     * remains and rollback is retried on next launch.
     */
    static void recoverIfNecessary(Context context) {
        synchronized(RESTORE_LOCK) {
            synchronized(J10ReviewStore.LEGACY_STORE_LOCK) {
                SharedPreferences journal=context.getSharedPreferences(JOURNAL_NAME,Context.MODE_PRIVATE);
                String pending=journal.getString(JOURNAL_KEY,null);
                if(pending==null)return;
                try {
                    JSONObject entry=new JSONObject(pending);
                    String beforeJson=entry.getString("before");
                    if(!sha256(beforeJson).equals(entry.getString("sha256")))
                        throw new JSONException("Corrupt restore journal.");
                    JSONObject before=new JSONObject(beforeJson);
                    JSONObject values=before.getJSONObject("values");
                    JSONObject dashboard=before.getJSONObject("dashboard");
                    JSONObject oldJ10=before.getJSONObject("j10");
                    JSONObject gates=before.getJSONObject("legacy_gates");
                    checkValues(values);checkValues(dashboard);
                    checkValues(oldJ10);checkValues(gates);
                    replace(context.getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE),values);
                    replace(context.getSharedPreferences(DASHBOARD_NAME,Context.MODE_PRIVATE),dashboard);
                    replace(context.getSharedPreferences(J10ReviewStore.NAME,Context.MODE_PRIVATE),oldJ10);
                    replace(context.getSharedPreferences(LEGACY_GATES,Context.MODE_PRIVATE),gates);
                    if(!journal.edit().remove(JOURNAL_KEY).commit())
                        throw new JSONException("Unable to finalize restore recovery.");
                }catch(JSONException invalid) {
                    // Fail closed rather than boot a mixed Hifz state.
                    throw new IllegalStateException("Unfinished restore: keep original backup.",invalid);
                }
            }
        }
    }

    /** All-old or all-new after process death; four SharedPreferences are not atomic. */
    static int restore(Context context,JSONObject root) throws JSONException {
        synchronized(RESTORE_LOCK) {
            recoverIfNecessary(context);
            if(!MAGIC.equals(root.optString("magic")))
                throw new JSONException("Fichier de sauvegarde non reconnu.");
            int version=root.optInt("version",-1);
            if(version!=1 && version!=VERSION)
                throw new JSONException("Version de sauvegarde non prise en charge.");
            JSONObject values=root.getJSONObject("values");
            // A missing schema would initialize defaults over the imported acquisitions.
            if(!values.has("schema"))
                throw new JSONException("Schéma de progression absent de la sauvegarde.");
            JSONObject dashboard=version==VERSION
                ?withCoverage(root.getJSONObject("dashboard")):legacyDashboard();
            JSONObject j10=version==VERSION?root.getJSONObject("j10"):new JSONObject();
            JSONObject gates=version==VERSION?root.getJSONObject("legacy_gates"):new JSONObject();
            checkValues(values);checkValues(dashboard);
            checkValues(j10);checkValues(gates);
            validateReceipts(dashboard);
            synchronized(J10ReviewStore.LEGACY_STORE_LOCK) {
                SharedPreferences main=context.getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE);
                SharedPreferences ledger=context.getSharedPreferences(DASHBOARD_NAME,Context.MODE_PRIVATE);
                SharedPreferences legacy=context.getSharedPreferences(J10ReviewStore.NAME,Context.MODE_PRIVATE);
                SharedPreferences gate=context.getSharedPreferences(LEGACY_GATES,Context.MODE_PRIVATE);
                SharedPreferences journal=context.getSharedPreferences(JOURNAL_NAME,Context.MODE_PRIVATE);
                JSONObject before=new JSONObject()
                    .put("values",encoded(main.getAll()))
                    .put("dashboard",encoded(ledger.getAll()))
                    .put("j10",encoded(legacy.getAll()))
                    .put("legacy_gates",encoded(gate.getAll()));
                String beforeJson=before.toString();
                JSONObject entry=new JSONObject().put("before",beforeJson)
                    .put("sha256",sha256(beforeJson));
                if(!journal.edit().putString(JOURNAL_KEY,entry.toString()).commit())
                    throw new JSONException("Unable to prepare restore journal.");
                try {
                    replace(main,values);injectedProcessDeath("MAIN");
                    replace(ledger,dashboard);injectedProcessDeath("LEDGER");
                    replace(legacy,j10);injectedProcessDeath("J10");
                    replace(gate,gates);injectedProcessDeath("GATES");
                    if(!journal.edit().remove(JOURNAL_KEY).commit())
                        throw new JSONException("Unable to complete restore.");
                }catch(JSONException | RuntimeException failure) {
                    try{recoverIfNecessary(context);}
                    catch(RuntimeException recovery){failure.addSuppressed(recovery);}
                    if(failure instanceof JSONException)throw (JSONException)failure;
                    throw new JSONException("Restore aborted: "+failure.getMessage());
                }
                return values.length();
            }
        }
    }

    private static void injectedProcessDeath(String store) {
        if(store.equals(failAfterStoreForTest))
            throw new AssertionError("Simulated process death after "+store);
    }

    private static void validateReceipts(JSONObject dashboard) throws JSONException {
        JSONObject watermark=dashboard.getJSONObject(COVERAGE_KEY);
        if(!"s".equals(watermark.getString("t")))
            throw new JSONException("Invalid attendance coverage type.");
        try{java.time.LocalDate.parse(watermark.getString("v"));}
        catch(RuntimeException invalid){throw new JSONException("Invalid attendance coverage date.");}
        if(!dashboard.has("completed_session_keys_v1"))return;
        JSONObject entry=dashboard.getJSONObject("completed_session_keys_v1");
        if(!"s".equals(entry.getString("t")))
            throw new JSONException("Invalid completion ledger type.");
        JSONArray keys=new JSONArray(entry.getString("v"));
        for(int i=0;i<keys.length();i++){
            String key=keys.getString(i);
            int separator=key.indexOf('|');
            if(separator<=0 || separator==key.length()-1)
                throw new JSONException("Invalid completion receipt.");
            try{java.time.LocalDate.parse(key.substring(0,separator));}
            catch(RuntimeException invalid){throw new JSONException("Invalid receipt date.");}
        }
    }

    private static JSONObject encoded(Map<String,?> all) throws JSONException {
        JSONObject out=new JSONObject();
        for(Map.Entry<String,?> e:all.entrySet())out.put(e.getKey(),encode(e.getValue()));
        return out;
    }

    private static void checkValues(JSONObject values) throws JSONException {
        if(values.has("schema")) {
            JSONObject schema=values.getJSONObject("schema");
            Object number=schema.get("v");
            // JSONObject.getInt coerces strings and fractions; SharedPreferences does not.
            if(!"i".equals(schema.getString("t")) || !(number instanceof Integer)
                    || (Integer)number<1 || (Integer)number>PreviewConfig.SCHEMA_VERSION)
                throw new JSONException("Schéma de progression invalide ou non pris en charge.");
        }
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
