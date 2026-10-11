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
 * Source-extracted English Ibn Kathir headings. The indexed commentary
 * intervals match the pinned digital source, not an invented heading model.
 *
 * Each chunk is immutable, bundled offline, and only opened on a user's
 * explicit title tap. The first original heading is displayed alone so
 * unreviewed multi-heading strings cannot create a huge BOOX overlay.
 * The existing 16 editorially VERIFIED Qaf/Najm titles take precedence in
 * IbnKathirOfflineTitles and are never overwritten by these candidates.
 *
 * Editorial and underlying Darussalam redistribution rights remain a
 * mandatory independent audit gate before distributing the next debug.
 * This loader NEVER supplies Quranic wording, boxes, masks or Hifz progress.
 */
final class IbnKathirTitlePack {
    private static final String EXPECTED_SHA="eb82bb6294efe30ad5c135c03b1864afaa70e855";
    private static final Map<Integer, JSONObject> CACHE=new HashMap<>();
    private static final Map<Integer, Boolean> FAILED=new HashMap<>();

    private IbnKathirTitlePack(){}

    static String[] sourceHeadings(Context context,IbnKathirGroupIndex.Group group) {
        if(context==null||group==null||group.surah<1||group.surah>114)return new String[0];
        int chunk=(group.surah-1)/15;
        JSONObject data=readChunk(context.getApplicationContext(),chunk);
        if(data==null)return new String[0]; // never fabricate an absent heading
        JSONArray items=data.optJSONArray(group.id);
        if(items==null||items.length()==0)return new String[0];
        // One compact source heading per tap. If a first group opens with a
        // general introduction/recitation notice and the source also has an
        // actual thematic heading, prefer the thematic one. Never paraphrase.
        String[] candidates=new String[items.length()];
        for(int i=0;i<candidates.length;i++) {
            String heading=items.optString(i,"").trim();
            if(heading.isEmpty()||heading.length()>145)return new String[0];
            candidates[i]=heading;
        }
        return new String[]{preferredSourceHeading(candidates)};
    }

    /**
     * For the SAME verified commentary interval, prefer an original contextual
     * heading to a surah-level preamble about virtues or recitation. Never invent
     * or translate a heading, never borrow from adjacent intervals.
     *
     * All 1,415 staged groups remain editorial candidates until Work reviews
     * the full source context and downstream redistribution rights.
     */
    static String preferredSourceHeading(String[] candidates) {
        if(candidates==null||candidates.length==0)return "";
        for(String heading:candidates) {
            if(heading!=null&&!heading.trim().isEmpty()&&!isGenericPreamble(heading))
                return heading.trim();
        }
        return candidates[0]==null?"":candidates[0].trim();
    }

    static boolean isGenericPreamble(String heading) {
        if(heading==null)return false;
        String h=heading.trim().toLowerCase(java.util.Locale.ROOT);
        return h.startsWith("introduction to ")
            ||h.startsWith("the beginning of ")
            ||h.startsWith("the virtues of ")
            ||h.startsWith("virtues of ")
            ||h.startsWith("the recitation of ")
            ||h.startsWith("recitation of ")
            ||h.startsWith("what has been narrated about ")
            ||h.startsWith("the first surah in which ")
            ||h.startsWith("the revelation of surah ")
            ||((h.startsWith("surah ")||h.startsWith("surat "))
                && (h.contains(" was revealed ")||h.endsWith(" was revealed")));
    }

    private static synchronized JSONObject readChunk(Context context,int chunk){
        if(CACHE.containsKey(chunk))return CACHE.get(chunk);
        if(Boolean.TRUE.equals(FAILED.get(chunk)))return null;
        int from=chunk*15+1,to=Math.min(114,from+14);
        String name=String.format(java.util.Locale.ROOT,
            "ibn_kathir_headings/chunk-%03d-%03d.json",from,to);
        try(InputStream input=context.getAssets().open(name);
            ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buffer=new byte[8192];int read;
            while((read=input.read(buffer))!=-1){
                out.write(buffer,0,read);
                if(out.size()>350_000)throw new IllegalStateException("Unbounded title pack");
            }
            JSONObject file=new JSONObject(new String(out.toByteArray(),StandardCharsets.UTF_8));
            if(!"IK_TITLES_SOURCE_EXTRACT_V1".equals(file.getString("schema"))
                    ||!EXPECTED_SHA.equals(file.getString("source_sha")))
                throw new IllegalStateException("Unpinned title source");
            JSONArray range=file.getJSONArray("range");
            if(range.getInt(0)!=from||range.getInt(1)!=to)
                throw new IllegalStateException("Title chunk does not match the surah");
            JSONObject groups=file.getJSONObject("groups");
            CACHE.put(chunk,groups);
            return groups;
        }catch(Exception damaged){
            FAILED.put(chunk,true);
            return null; // preserve the Mushaf, do not crash Lecture or expose tech labels
        }
    }
}
