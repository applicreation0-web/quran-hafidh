package com.quransafeguard.hifz.preview;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CP02: Read-only QCF-first-word markers for 1,053 Ayah Themes passage starts.
 * Private titles are NOT bundled in this public repository. The Arabic Mushaf,
 * Jalalayn, Hifz scheduling, masking and existing recall holes are never changed.
 * Without a fully validated private pack this index is unavailable.
 */
final class AyahThemesCueIndex {
    static final String ASSET = "ayah_themes_private/titles_114.json";
    static final String SCHEMA = "CP01_APPROVED_NAV_DISPLAY_NO_SOURCE_MUTATION_V1";
    static final String ORIGINAL = "AYAH_THEMES_ORIGINAL";
    static final String EDITORIAL = "EDITORIAL_USER_APPROVED_NOT_SOURCE_QUOTE";
    static final String ISRAELITES = "USER_APPROVED_ISRAELITES";
    static final int COUNT = 1053;

    static final class Cue {
        final String id;
        final String title;
        final int surah, start, end;
        Cue(String id, String title, int surah, int start, int end) {
            this.id=id; this.title=title; this.surah=surah; this.start=start; this.end=end;
        }
    }

    private final Map<String,Cue> byId;
    private final Map<String,Cue> byStart;
    private final WordGeometryRepository geometry;

    private AyahThemesCueIndex(Map<String,Cue> byId, Map<String,Cue> byStart,
                               WordGeometryRepository geometry) {
        this.byId = Collections.unmodifiableMap(byId);
        this.byStart = Collections.unmodifiableMap(byStart);
        this.geometry = geometry;
    }

    static AyahThemesCueIndex load(Context context) {
        if (context == null) return null;
        try (InputStream in=context.getAssets().open(ASSET);
             ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] chunk=new byte[8192];
            int n;
            while ((n=in.read(chunk))!=-1) {
                if(out.size()+n>2_000_000)throw new IllegalStateException("oversized title pack");
                out.write(chunk,0,n);
            }
            return parse(new String(out.toByteArray(),StandardCharsets.UTF_8),
                MawdudiPassageIndex.load(context),WordGeometryRepository.shared(context));
        } catch (Exception notInstalledOrInvalid) {
            return null;  // NEVER fall back to Ibn Kathir or Al-Munir headings.
        }
    }

    static AyahThemesCueIndex parse(String raw, MawdudiPassageIndex canonical,
                                   WordGeometryRepository geometry) throws JSONException {
        if(raw==null||canonical==null||canonical.count()!=COUNT)
            throw new IllegalStateException("missing canonical index");
        JSONObject root=new JSONObject(raw);
        if(!SCHEMA.equals(root.optString("schema")))
            throw new IllegalStateException("wrong private schema");
        JSONArray rows=root.getJSONArray("passages");
        if(rows.length()!=COUNT)throw new IllegalStateException("incomplete private titles");
        Map<String,Cue> ids=new HashMap<>();
        Map<String,Cue> starts=new HashMap<>();
        List<MawdudiPassageIndex.Passage> expected=canonical.all();
        int originalCount=0, editorialCount=0;
        for(int i=0;i<COUNT;i++) {
            JSONObject row=rows.getJSONObject(i);
            MawdudiPassageIndex.Passage src=expected.get(i);
            String id=row.getString("id");
            int surah=row.getInt("surah"), first=row.getInt("start"), last=row.getInt("end");
            if(!id.equals(src.id)||surah!=src.surah||first!=src.firstAyah||last!=src.lastAyah)
                throw new IllegalStateException("private boundary mismatch: "+id);
            if(!"LOCKED".equals(row.optString("source_immutability")))
                throw new IllegalStateException("source not locked");
            String display=row.getString("display_label").trim();
            if(display.isEmpty()||display.length()>350)
                throw new IllegalStateException("invalid English heading "+id);
            boolean signed=row.optBoolean("display_label_approved_by_user",false);
            String source=row.isNull("source_title_exact")?null:row.getString("source_title_exact");
            String origin=row.getString("display_label_origin");
            if(src.editorialAdaptation) {
                if(!EDITORIAL.equals(origin)||!signed||source!=null||!row.isNull("source"))
                    throw new IllegalStateException("unapproved editorial label "+id);
                editorialCount++;
            } else {
                if(!"Ayah Themes".equals(row.optString("source"))||source==null||source.isEmpty())
                    throw new IllegalStateException("missing source wording "+id);
                if(ORIGINAL.equals(origin)) {
                    if(!display.equals(source)||signed)
                        throw new IllegalStateException("original rewritten "+id);
                } else if(ISRAELITES.equals(origin)) {
                    // A revised display name requires user signoff; preserve source verbatim.
                    if(!signed||display.equals(source))
                        throw new IllegalStateException("unapproved Israelites substitution "+id);
                } else throw new IllegalStateException("unrecognized origin "+id);
                originalCount++;
            }
            Cue cue=new Cue(id,display,surah,first,last);
            if(ids.put(id,cue)!=null||starts.put(surah+":"+first,cue)!=null)
                throw new IllegalStateException("duplicate cue "+id);
        }
        if(originalCount!=1044||editorialCount!=9)
            throw new IllegalStateException("unexpected title provenance");
        return new AyahThemesCueIndex(ids,starts,geometry);
    }

    JSONArray forPage(int page) {
        return geometry==null?new JSONArray():forPageWords(byStart,geometry.wordsForPage(page));
    }

    /** Only exact page word 1 rectangles in viewBox 0 0 345 550; no estimation. */
    static JSONArray forPageWords(Map<String,Cue> starts,
                                 List<WordGeometryRepository.WordBox> words) {
        JSONArray out=new JSONArray();
        if(starts==null||words==null)return out;
        for(WordGeometryRepository.WordBox word:words) {
            if(word.word!=1)continue;
            Cue cue=starts.get(word.surah+":"+word.ayah);
            if(cue==null)continue;
            try {
                out.put(new JSONObject()
                    .put("id",cue.id)
                    .put("index",out.length()+1)
                    .put("title",cue.title)
                    .put("anchor","")
                    .put("anchorWordCount",1)
                    .put("boxes",new JSONArray().put(word.boxJson()))
                    .put("ranges",new JSONArray()));
            } catch (JSONException malformed) {
                return new JSONArray(); // fail closed
            }
        }
        return out;
    }

    /** Stable canonical label for the graphical map; source text is never changed. */
    String titleForPassage(String id) {
        Cue cue=byId.get(id);return cue==null?null:cue.title;
    }

    /** Refuse stale taps or locations from unrelated historical boundaries. */
    String titleForVisibleStart(int page,String id) {
        Cue cue=byId.get(id);
        if(cue==null||geometry==null)return null;
        for(WordGeometryRepository.WordBox word:geometry.wordsForPage(page))
            if(word.word==1&&word.surah==cue.surah&&word.ayah==cue.start)
                return cue.title;
        return null;
    }
}
