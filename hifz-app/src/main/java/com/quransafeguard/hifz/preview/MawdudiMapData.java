package com.quransafeguard.hifz.preview;
import android.content.Context;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

/** Source-safe macro headings are a separate private display asset, never Quran/Hifz data. */
final class MawdudiMapData {
 static final class Macro {
  final int surah,start,end;final String label;
  Macro(int s,int a,int b,String t){surah=s;start=a;end=b;label=t;}
 }
 private final List<Macro> all;
 private MawdudiMapData(List<Macro> rows){all=Collections.unmodifiableList(rows);}
 static String read(Context c,String asset)throws Exception {
  try(InputStream in=c.getAssets().open(asset);ByteArrayOutputStream out=new ByteArrayOutputStream()) {
   byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(out.size()+n>2_000_000)throw new IllegalStateException("oversized map pack");out.write(b,0,n);}
   PrivateDisplayIntegrity.verify(asset,out.toByteArray());
   return new String(out.toByteArray(),StandardCharsets.UTF_8);
  }
 }
 static MawdudiMapData load(Context c,MawdudiPassageIndex index)throws Exception {
  AtlasPrivateV2 source=AtlasPrivateV2.load(c,index);
  // Existing debug versions can still load without the newly approved label
  // asset; malformed or tampered copies must not silently fall back.
  java.util.Map<String,String> approved=null;
  try{approved=readApprovedTitles(c,source);}
  catch(java.io.FileNotFoundException notYetPackaged){/* structural IDs remain valid */}
  List<Macro> rows=new ArrayList<>();
  for(AtlasPrivateV2.Unit u:source.units()){
   String title=approved==null?null:approved.get(u.id);
   rows.add(new Macro(u.surah,u.start,u.end,
       title==null?"Unité "+u.ordinal+" · "+u.start+"–"+u.end:title));
  }
  return new MawdudiMapData(rows);
 }
 static MawdudiMapData parse(String raw,MawdudiPassageIndex index)throws JSONException {
  JSONObject root=new JSONObject(raw);
  if(!"MAWDUDI_SOURCE_SAFE_MACROS_V1".equals(root.optString("schema")))throw new IllegalStateException("wrong macro schema");
  JSONArray rows=root.getJSONArray("macros");if(rows.length()!=173)throw new IllegalStateException("missing macro");
  List<Macro> macros=new ArrayList<>();int i=0;
  for(int s=1;s<=114;s++) {
   List<MawdudiPassageIndex.Passage> ps=index.forSurah(s);int last=ps.get(ps.size()-1).lastAyah;
   int[][] exact=s==2?new int[][]{{1,20},{21,29},{30,39},{40,120},{121,141},{142,152},{153,251},{252,260},{261,283},{284,286}}:
    s==3?new int[][]{{1,32},{33,63},{64,120},{121,200}}:
    s==4?new int[][]{{1,35},{36,42},{43,43},{44,57},{58,72},{73,100},{101,103},{104,104},{105,135},{136,175},{176,176}}:
    s==5?new int[][]{{1,10},{11,26},{27,32},{33,40},{41,50},{51,69},{70,86},{87,108},{109,119},{120,120}}:
    s==6?new int[][]{{1,12},{13,24},{25,32},{33,73},{74,90},{91,108},{109,154},{155,160},{161,165}}:
    s==7?new int[][]{{1,10},{11,25},{26,53},{54,58},{59,171},{172,174},{175,179},{180,198},{199,206}}:
    s==8?new int[][]{{1,41},{42,54},{55,59},{60,66},{67,71},{72,75}}:
    s==9?new int[][]{{1,12},{13,37},{38,72},{73,90},{91,110},{111,118},{119,127},{128,129}}:new int[][]{{1,last}};
   for(int[] range:exact) {
    JSONObject row=rows.getJSONObject(i++);
    String expected=exact.length==1?"WHOLE_SURAH_ONLY_NO_NEW_SPLIT_INVENTED":s==5&&range[0]==120?"SOURCE_CONCLUSION_FROM_INTRODUCTION":(s==2||s>=4)?"SOURCE_BOUNDARY_CONFIRMED_IN_INTRODUCTION":"SOURCE_BOUNDARY_CONFIRMED_FROM_PDF";
    if(row.getInt("surah")!=s||row.getInt("start")!=range[0]||row.getInt("end")!=range[1]
       ||!expected.equals(row.getString("status")))throw new IllegalStateException("unapproved macro boundary");
    String label=row.getString("label");if(label.trim().isEmpty()||label.length()>1000)throw new IllegalStateException("missing macro label");
    macros.add(new Macro(s,range[0],range[1],label));
   }
  }
  return new MawdudiMapData(macros);
 }
 /** Approved private summaries: exact edition-safe labels, SHA-256 pinned and unit-bound.
  * Never treat them as literal titles copied from Maududi.
  */
 private static java.util.Map<String,String> readApprovedTitles(Context c,AtlasPrivateV2 index)throws Exception{
  final String asset="atlas_private/atlas_approved_unit_titles_366_v1.json";
  final String checksum="421277c59519f2b904de23bbc1e0eee6faeb569e33ed6b425b4b9235cf82ff79";
  byte[] bytes;
  try(InputStream in=c.getAssets().open(asset);ByteArrayOutputStream out=new ByteArrayOutputStream()){
   byte[] buffer=new byte[4096];int n;
   while((n=in.read(buffer))!=-1){
    if(out.size()+n>150_000)throw new IllegalStateException("oversized approved headings");
    out.write(buffer,0,n);
   }
   bytes=out.toByteArray();
  }
  StringBuilder digest=new StringBuilder();
  for(byte b:java.security.MessageDigest.getInstance("SHA-256").digest(bytes))
   digest.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
  if(!checksum.equals(digest.toString()))throw new IllegalStateException("Atlas label digest mismatch");
  JSONObject root=new JSONObject(new String(bytes,StandardCharsets.UTF_8));
  if(!"HAAFIDH_ATLAS_USER_APPROVED_EDITORIAL_LABELS_V1".equals(root.optString("schema"))
       ||root.optInt("unit_index_count")!=366||root.optInt("titled_unit_count")!=120
       ||root.optBoolean("original_quotes"))throw new IllegalStateException("unapproved Atlas labels");
  java.util.Map<String,AtlasPrivateV2.Unit> source=new java.util.HashMap<>();
  for(AtlasPrivateV2.Unit unit:index.units())source.put(unit.id,unit);
  JSONArray entries=root.getJSONArray("labels");
  if(entries.length()!=120)throw new IllegalStateException("incomplete approved Atlas labels");
  java.util.Map<String,String> labels=new java.util.HashMap<>();
  for(int i=0;i<entries.length();i++){
   JSONObject item=entries.getJSONObject(i);
   String id=item.getString("id");
   AtlasPrivateV2.Unit unit=source.get(id);
   if(unit==null||unit.surah!=item.getInt("surah")||unit.start!=item.getInt("start")
       ||unit.end!=item.getInt("end")
       ||!"USER_APPROVED_EDITORIAL_SOURCE_SUMMARY".equals(item.getString("origin")))
    throw new IllegalStateException("mismatched Atlas label");
   String label=item.getString("title").trim();
   if(label.isEmpty()||label.length()>240||labels.put(id,label)!=null)
    throw new IllegalStateException("duplicate/invalid Atlas label");
  }
  return labels;
 }
 int count(){return all.size();}
 List<Macro> forSurah(int s){List<Macro> r=new ArrayList<>();for(Macro m:all)if(m.surah==s)r.add(m);return r;}
 Macro at(int s,int a){for(Macro m:all)if(m.surah==s&&m.start<=a&&m.end>=a)return m;throw new IllegalArgumentException("macro verse");}
}

/**
 * A canonical Ayah Themes passage may cross Atlas unit boundaries.
 * The local context is display-only; it never creates a new source passage,
 * changes its ID, or attributes the complete source heading to a partial range.
 */
final class AtlasPassageContext {
    private AtlasPassageContext() {}

    static int intersectionStart(int sourceStart, int unitStart) {
        return Math.max(sourceStart, unitStart);
    }

    static int intersectionEnd(int sourceEnd, int unitEnd) {
        return Math.min(sourceEnd, unitEnd);
    }

    static String displayTitle(int surah, int sourceStart, int sourceEnd,
                               int unitStart, int unitEnd, String original) {
        if (surah < 1 || surah > 114 || sourceStart < 1 || unitStart < 1 ||
                sourceEnd < sourceStart || unitEnd < unitStart)
            throw new IllegalArgumentException("Invalid canonical or Atlas boundary");
        int start = intersectionStart(sourceStart, unitStart);
        int end = intersectionEnd(sourceEnd, unitEnd);
        if (start > end)
            throw new IllegalArgumentException("No passage/Atlas intersection");
        // Full passage: the untouched source title remains the correct caption.
        if (start == sourceStart && end == sourceEnd)
            return original == null ? "" : original;
        // Partial passage: NEVER attribute the full source heading to this unit.
        // Use a grounded reference until a local title is reviewed and approved.
        String portion = surah + ":" + start + (start == end ? "" : "–" + end);
        String source = surah + ":" + sourceStart
                + (sourceStart == sourceEnd ? "" : "–" + sourceEnd);
        return portion + " · partie du passage " + source;
    }
}
