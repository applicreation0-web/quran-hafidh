package com.quransafeguard.hifz.preview;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** No licensed commentary is included in this test. */
public final class IbnKathirFullCommentaryPrivateTest {
    private static String hash(String text) throws Exception {
        byte[] raw=MessageDigest.getInstance("SHA-256")
            .digest(text.getBytes(StandardCharsets.UTF_8));
        StringBuilder b=new StringBuilder();
        for(byte c:raw)b.append(String.format(Locale.ROOT,"%02x",c&255));
        return b.toString();
    }
    private static String entry(String id,String text,String sha) throws Exception {
        return new JSONObject().put("schema","IK_DARUSSALAM_PRIVATE_COMMENTARY_V1")
            .put("group_id",id).put("edition","Darussalam")
            .put("volume",1).put("page",42)
            .put("text_en",text).put("text_sha256",sha).toString();
    }
    @Test public void validatesIdAndHashAndRejectsOtherGroups() throws Exception {
        String id="IKEN002_148_148";
        String safeText="Fictional non-religious fixture, not a commentary.";
        String valid=entry(id,safeText,hash(safeText));
        assertNotNull(IbnKathirFullCommentaryPrivate.parseVerified(valid,id));
        assertNull(IbnKathirFullCommentaryPrivate.parseVerified(
            valid,"IKEN002_149_150"));
        assertNull(IbnKathirFullCommentaryPrivate.parseVerified(
            entry(id,safeText,hash("different")),id));
        assertNull(IbnKathirFullCommentaryPrivate.parseVerified("{}",id));
    }
    @Test public void noEmptyOrUnprovenTextCanBeShown() throws Exception {
        assertNull(IbnKathirFullCommentaryPrivate.parseVerified(
            entry("IKEN002_148_148","",hash("")),"IKEN002_148_148"));
        JSONObject wrong=new JSONObject(entry("IKEN002_148_148","test",hash("test")));
        wrong.put("edition","Unverified");
        assertNull(IbnKathirFullCommentaryPrivate.parseVerified(
            wrong.toString(),"IKEN002_148_148"));
    }
}
