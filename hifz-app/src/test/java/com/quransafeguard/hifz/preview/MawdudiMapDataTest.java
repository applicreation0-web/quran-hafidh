package com.quransafeguard.hifz.preview;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
public final class MawdudiMapDataTest {
 static JSONObject synthetic()throws Exception {
  MawdudiPassageIndex index=AyahThemesCueIndexTest.canonical();JSONArray rows=new JSONArray();
  for(int s=1;s<=114;s++) {
   java.util.List<MawdudiPassageIndex.Passage> ps=index.forSurah(s);
   int last=ps.get(ps.size()-1).lastAyah;
   int[][] bounds=s==3?new int[][]{{1,32},{33,63},{64,120},{121,200}}:
    s==9?new int[][]{{1,37},{38,72},{73,129}}:new int[][]{{1,last}};
   for(int[] b:bounds)rows.put(new JSONObject().put("surah",s).put("start",b[0]).put("end",b[1]).put("label","SYNTHETIC MACRO").put("status",bounds.length==1?"WHOLE_SURAH_ONLY_NO_NEW_SPLIT_INVENTED":"SOURCE_BOUNDARY_CONFIRMED_FROM_PDF"));
  }
  return new JSONObject().put("schema","MAWDUDI_SOURCE_SAFE_MACROS_V1").put("macros",rows);
 }
 @Test public void authenticatedBytesRejectCoordinatedSourceAndDisplayRewrite()throws Exception {
  byte[] original="approved synthetic wording".getBytes(java.nio.charset.StandardCharsets.UTF_8);
  String digest="f2831504829969a63dc49fa2000086f7e38b024884efb498f5fd860282e602b6";
  assertTrue(PrivateDisplayIntegrity.matches(original,digest));
  assertFalse(PrivateDisplayIntegrity.matches("rewritten synthetic wording".getBytes(java.nio.charset.StandardCharsets.UTF_8),digest));
  try{PrivateDisplayIntegrity.verify(AyahThemesCueIndex.ASSET,original);fail("unapproved bytes accepted");}catch(IllegalStateException expected){}
 }
 @Test public void completeSourceSafeMacrosCoverAll114Surahs()throws Exception {
  MawdudiMapData data=MawdudiMapData.parse(synthetic().toString(),AyahThemesCueIndexTest.canonical());
  assertEquals(119,data.count());assertEquals(4,data.forSurah(3).size());assertEquals(1,data.forSurah(2).size());
  assertEquals(121,data.at(3,175).start);assertEquals(200,data.at(3,175).end);
 }
 @Test public void inventedSplitOrOverlapFailsClosed()throws Exception {
  JSONObject raw=synthetic();raw.getJSONArray("macros").getJSONObject(0).put("end",6);
  try{MawdudiMapData.parse(raw.toString(),AyahThemesCueIndexTest.canonical());fail("gap accepted");}catch(IllegalStateException expected){}
 }
 @Test public void missingMacroFailsClosed()throws Exception {
  JSONObject raw=synthetic();raw.getJSONArray("macros").remove(0);
  try{MawdudiMapData.parse(raw.toString(),AyahThemesCueIndexTest.canonical());fail("missing source macro accepted");}catch(IllegalStateException expected){}
 }
}
