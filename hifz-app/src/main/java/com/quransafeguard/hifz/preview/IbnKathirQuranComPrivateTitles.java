package com.quransafeguard.hifz.preview;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * M9: exactly ONE independently selected English heading per Ibn Kathir group.
 * The M7-B private pack preserves real provenance separately; no Quran.com-only
 * authorship claim is made for the Darussalam and user-approved thematic headings.
 * The private pack MUST stay outside this public repository and public CI.
 * Missing or invalid data means no title; no generated fallback or cycling.
 */
final class IbnKathirQuranComPrivateTitles {
    private static final String SCHEMA = "IK_APPROVED_FINAL_V2";
    private static final String CORPUS = "M7B_1903_2026-10-13";
    private static final Map<Integer, JSONObject> CACHE = new HashMap<>();
    private static final Map<Integer, Boolean> FAILED = new HashMap<>();
    private IbnKathirQuranComPrivateTitles() {}

    static String approvedTitle(Context context, IbnKathirGroupIndex.Group group) {
        if(context==null||group==null||group.surah<1||group.surah>114)return null;
        JSONObject titles=load(context.getApplicationContext(),(group.surah-1)/15);
        if(titles==null)return null;
        return validateApprovedTitle(titles.opt(group.id));
    }

    /** Reject V1 arrays of multiple titles, including single-element arrays. */
    static String validateApprovedTitle(Object raw) {
        if(!(raw instanceof String))return null;
        String title=(String)raw;
        if(title.isEmpty()||title.length()>145||!title.equals(title.trim())||
                title.indexOf('\n')>=0||title.indexOf('\r')>=0)return null;
        return title;
    }

    private static synchronized JSONObject load(Context context,int chunk) {
        if(CACHE.containsKey(chunk))return CACHE.get(chunk);
        if(Boolean.TRUE.equals(FAILED.get(chunk)))return null;
        int first=chunk*15+1,last=Math.min(114,first+14);
        String filename=String.format(java.util.Locale.ROOT,
                "ibn_kathir_qurancom_private/chunk-%03d-%03d.json",first,last);
        try(InputStream stream=context.getAssets().open(filename);
            ByteArrayOutputStream bytes=new ByteArrayOutputStream()){
            byte[] buffer=new byte[8192];int count;
            while((count=stream.read(buffer))!=-1){
                bytes.write(buffer,0,count);
                if(bytes.size()>400_000)throw new IllegalStateException("Oversized private title source");
            }
            JSONObject root=new JSONObject(new String(bytes.toByteArray(),StandardCharsets.UTF_8));
            if(!SCHEMA.equals(root.getString("schema"))||
                    !CORPUS.equals(root.getString("corpus")))
                throw new IllegalStateException("Wrong private editorial schema");
            JSONArray range=root.getJSONArray("range");
            if(range.length()!=2||range.getInt(0)!=first||range.getInt(1)!=last)
                throw new IllegalStateException("Wrong private title range");
            JSONObject groups=root.getJSONObject("groups");
            CACHE.put(chunk,groups);
            return groups;
        }catch(Exception invalid){
            FAILED.put(chunk,true);
            return null;
        }
    }
}
