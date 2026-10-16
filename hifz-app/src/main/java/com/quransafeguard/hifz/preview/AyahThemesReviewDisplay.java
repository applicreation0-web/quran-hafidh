package com.quransafeguard.hifz.preview;

import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;

/** Independent visual targets: never calls setMask/setSemanticCues or touches recall state. */
final class AyahThemesReviewDisplay {
    private static final String SCRIPT="ayah_themes_display/review_titles.js";
    private AyahThemesReviewDisplay() {}
    static void apply(Context context,MushafView view,AyahThemesCueIndex index,int page) {
        if(view==null||index==null)return;
        try(InputStream in=context.getAssets().open(SCRIPT);
            ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] bytes=new byte[4096];int n;
            while((n=in.read(bytes))!=-1)out.write(bytes,0,n);
            JSONArray source=index.forPage(page),targets=new JSONArray();
            for(int i=0;i<source.length();i++) {
                JSONObject cue=source.getJSONObject(i);
                targets.put(new JSONObject().put("id",cue.getString("id"))
                    .put("boxes",cue.getJSONArray("boxes")));
            }
            String script=new String(out.toByteArray(),StandardCharsets.UTF_8);
            view.evaluateJavascript(script+"\nwindow.AyahThemesReviewDisplay.install("+targets+");",null);
        } catch(Exception unavailable) {
            // Missing display data never changes the page's historical mask/fail-open decision.
            view.evaluateJavascript("window.AyahThemesReviewDisplay&&window.AyahThemesReviewDisplay.clear();",null);
        }
    }
}
