package com.quransafeguard.hifz.preview;

import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/** Optional private, user-reviewed Atlas labels. These are source-backed editorial
 * summaries, never represented as literal Maududi chapter headings.
 * Neither the public repo nor Hifz data contain the private label pack.
 */
final class AtlasUnitTitles {
    static final String ASSET="atlas_private/atlas_approved_unit_titles_366_v1.json";
    static final String SHA256="02b08050c290cf7e2a79cf579288b88e6b430f5a56a339059ce700dd4cc09e6b";
    static final String SCHEMA="HAAFIDH_ATLAS_USER_APPROVED_EDITORIAL_LABELS_V1";
    private final Map<String,String> names;
    private AtlasUnitTitles(Map<String,String> entries) {
        names=Collections.unmodifiableMap(entries);
    }
    String forUnit(String id) {return names.get(id);}
    static AtlasUnitTitles load(Context context,AtlasPrivateV2 index) throws Exception {
        if(context==null||index==null)throw new IllegalArgumentException("atlas");
        byte[] bytes;
        try(InputStream in=context.getAssets().open(ASSET);
            ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buffer=new byte[4096];int n;
            while((n=in.read(buffer))!=-1){
                if(out.size()+n>150_000)throw new IllegalStateException("oversized unit title asset");
                out.write(buffer,0,n);
            }
            bytes=out.toByteArray();
        }
        StringBuilder digest=new StringBuilder();
        for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))
            digest.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
        if(!SHA256.equals(digest.toString()))throw new IllegalStateException("unit titles digest mismatch");
        JSONObject root=new JSONObject(new String(bytes,StandardCharsets.UTF_8));
        if(!SCHEMA.equals(root.optString("schema"))||root.optInt("unit_index_count")!=366
            ||root.optInt("titled_unit_count")!=120||root.optBoolean("original_quotes"))
            throw new IllegalStateException("unapproved unit title schema");
        Map<String,AtlasPrivateV2.Unit> approved=new HashMap<>();
        for(AtlasPrivateV2.Unit u:index.units())approved.put(u.id,u);
        JSONArray rows=root.getJSONArray("labels");
        if(rows.length()!=120)throw new IllegalStateException("incomplete approved title labels");
        Map<String,String> names=new HashMap<>();
        Set<String> keys=new HashSet<>();
        for(int i=0;i<rows.length();i++){
            JSONObject row=rows.getJSONObject(i);
            String id=row.getString("id");
            AtlasPrivateV2.Unit unit=approved.get(id);
            if(unit==null||!keys.add(id)
                ||unit.surah!=row.getInt("surah")
                ||unit.start!=row.getInt("start")
                ||unit.end!=row.getInt("end")
                ||!"USER_APPROVED_EDITORIAL_SOURCE_SUMMARY".equals(row.getString("origin")))
                throw new IllegalStateException("unit title/source mismatch");
            String label=row.getString("title").trim();
            if(label.isEmpty()||label.length()>240)throw new IllegalStateException("invalid unit title");
            names.put(id,label);
        }
        return new AtlasUnitTitles(names);
    }
}
