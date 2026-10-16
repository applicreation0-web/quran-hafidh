package com.quransafeguard.hifz.preview;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

/** All labels are synthetic: no private Ayah Themes text in source or CI. */
public final class AyahThemesCueIndexTest {
    static MawdudiPassageIndex canonical() throws Exception {
        Path p=Paths.get("hifz-app/src/main/assets/mawdudi_public/passages_v1.csv");
        if(!Files.exists(p))p=Paths.get("..",p.toString());
        return MawdudiPassageIndex.parse(new String(Files.readAllBytes(p),StandardCharsets.UTF_8));
    }
    static String privateSynthetic(MawdudiPassageIndex index) throws Exception {
        JSONArray rows=new JSONArray();
        for(MawdudiPassageIndex.Passage p:index.all()) {
            JSONObject o=new JSONObject();
            o.put("id",p.id).put("surah",p.surah)
                .put("start",p.firstAyah).put("end",p.lastAyah)
                .put("source_immutability","LOCKED");
            if(p.editorialAdaptation) {
                o.put("source",JSONObject.NULL).put("source_title_exact",JSONObject.NULL)
                    .put("display_label","Synthetic approved exception "+p.id)
                    .put("display_label_origin",AyahThemesCueIndex.EDITORIAL)
                    .put("display_label_approved_by_user",true);
            } else {
                o.put("source","Ayah Themes")
                    .put("source_title_exact","Synthetic original "+p.id)
                    .put("display_label","Synthetic original "+p.id)
                    .put("display_label_origin",AyahThemesCueIndex.ORIGINAL)
                    .put("display_label_approved_by_user",false);
            }
            rows.put(o);
        }
        return new JSONObject().put("schema",AyahThemesCueIndex.SCHEMA)
            .put("passages",rows).toString();
    }
    @Test public void fullCanonicalPrivatePackIsValid() throws Exception {
        MawdudiPassageIndex index=canonical();
        assertEquals(1053,index.count());
        assertNotNull(AyahThemesCueIndex.parse(privateSynthetic(index),index,null));
    }
    @Test public void sourceTitleCannotBeRewritten() throws Exception {
        MawdudiPassageIndex index=canonical();
        JSONObject pack=new JSONObject(privateSynthetic(index));
        pack.getJSONArray("passages").getJSONObject(0).put("display_label","A different title");
        try {AyahThemesCueIndex.parse(pack.toString(),index,null);fail("rewritten title passed");}
        catch(IllegalStateException expected){assertTrue(expected.getMessage().contains("rewritten"));}
    }
    @Test public void unsignedEditorialExceptionFailsClosed() throws Exception {
        MawdudiPassageIndex index=canonical();
        JSONObject pack=new JSONObject(privateSynthetic(index));
        JSONArray rows=pack.getJSONArray("passages");
        for(int i=0;i<rows.length();i++) {
            JSONObject o=rows.getJSONObject(i);
            if(AyahThemesCueIndex.EDITORIAL.equals(o.optString("display_label_origin"))) {
                o.put("display_label_approved_by_user",false);
                break;
            }
        }
        try {AyahThemesCueIndex.parse(pack.toString(),index,null);fail("unsigned exception passed");}
        catch(IllegalStateException expected){assertTrue(expected.getMessage().contains("unapproved"));}
    }
    @Test public void markersUseExactWordOneBoxAndIgnoreOtherWords() throws Exception {
        Map<String,AyahThemesCueIndex.Cue> starts=new HashMap<>();
        starts.put("2:1",new AyahThemesCueIndex.Cue("s002a001b005","Synthetic A",2,1,5));
        starts.put("2:6",new AyahThemesCueIndex.Cue("s002a006b007","Synthetic B",2,6,7));
        JSONArray cues=AyahThemesCueIndex.forPageWords(starts,Arrays.asList(
            new WordGeometryRepository.WordBox("2:1:1",12,22,32,42),
            new WordGeometryRepository.WordBox("2:1:2",40,22,50,42),
            new WordGeometryRepository.WordBox("2:6:1",75,22,95,42)));
        assertEquals(2,cues.length());
        assertEquals("s002a001b005",cues.getJSONObject(0).getString("id"));
        assertEquals("s002a006b007",cues.getJSONObject(1).getString("id"));
        assertEquals(1,cues.getJSONObject(0).getJSONArray("boxes").length());
        assertEquals(12d,cues.getJSONObject(0).getJSONArray("boxes").getJSONArray(0).getDouble(0),0d);
    }
}
