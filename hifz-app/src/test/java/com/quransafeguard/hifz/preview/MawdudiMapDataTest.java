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
   int[][] bounds=s==2?new int[][]{{1,20},{21,29},{30,39},{40,120},{121,141},{142,152},{153,251},{252,260},{261,283},{284,286}}:
    s==3?new int[][]{{1,32},{33,63},{64,120},{121,200}}:
    s==4?new int[][]{{1,35},{36,42},{43,43},{44,57},{58,72},{73,100},{101,103},{104,104},{105,135},{136,175},{176,176}}:
    s==5?new int[][]{{1,10},{11,26},{27,32},{33,40},{41,50},{51,69},{70,86},{87,108},{109,119},{120,120}}:
    s==6?new int[][]{{1,12},{13,24},{25,32},{33,73},{74,90},{91,108},{109,154},{155,160},{161,165}}:
    s==7?new int[][]{{1,10},{11,25},{26,53},{54,58},{59,171},{172,174},{175,179},{180,198},{199,206}}:
    s==8?new int[][]{{1,41},{42,54},{55,59},{60,66},{67,71},{72,75}}:
    s==9?new int[][]{{1,12},{13,37},{38,72},{73,90},{91,110},{111,118},{119,127},{128,129}}:new int[][]{{1,last}};
   for(int[] b:bounds)rows.put(new JSONObject().put("surah",s).put("start",b[0]).put("end",b[1]).put("label","SYNTHETIC MACRO").put("status",bounds.length==1?"WHOLE_SURAH_ONLY_NO_NEW_SPLIT_INVENTED":
        s==5&&b[0]==120?"SOURCE_CONCLUSION_FROM_INTRODUCTION":
        (s==2||s>=4)?"SOURCE_BOUNDARY_CONFIRMED_IN_INTRODUCTION":"SOURCE_BOUNDARY_CONFIRMED_FROM_PDF"));
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
  assertEquals(173,data.count());assertEquals(4,data.forSurah(3).size());assertEquals(10,data.forSurah(2).size());assertEquals(11,data.forSurah(4).size());assertEquals(9,data.forSurah(7).size());
  assertEquals(121,data.at(2,121).start);
  assertEquals(121,data.at(3,175).start);assertEquals(200,data.at(3,175).end);
 }
 @Test public void parsedMacrosExposeUnitAndCanonicalPassageDisplayTitles()throws Exception {
  MawdudiPassageIndex index=AyahThemesCueIndexTest.canonical();
  MawdudiMapData data=MawdudiMapData.parse(synthetic().toString(),index);
  // Same legacy parsed fixture and U4 selection used by the native graph test.
  MawdudiMapData.Macro unit=data.at(3,175);
  assertEquals("s003a121b200",unit.id);
  assertEquals("SYNTHETIC MACRO",data.displayUnitTitle(unit.id));
  for(int surah=1;surah<=114;surah++)for(MawdudiMapData.Macro m:data.forSurah(surah))
   assertEquals(m.label,data.displayUnitTitle(m.id));
  for(MawdudiPassageIndex.Passage passage:index.all()) {
   String title="SYNTHETIC passage "+passage.id;
   assertEquals(title,data.displayPassageTitle(passage.id,title));
   for(MawdudiMapData.Macro m:data.forSurah(passage.surah)) {
    if(passage.firstAyah>m.end||passage.lastAyah<m.start)continue;
    assertEquals(AtlasPassageContext.displayTitle(passage.surah,passage.firstAyah,
      passage.lastAyah,m.start,m.end,title),data.displayContextTitle(passage.id,m.id,title));
   }
  }
  try{data.displayUnitTitle("s003a121b199");fail("unknown unit accepted");}
  catch(IllegalArgumentException expected){}
  try{data.displayPassageTitle("missing", "SYNTHETIC");fail("unknown passage accepted");}
  catch(IllegalArgumentException expected){}
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
