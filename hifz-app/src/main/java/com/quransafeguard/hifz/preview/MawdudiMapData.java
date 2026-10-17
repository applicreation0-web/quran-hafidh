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
  List<Macro> rows=new ArrayList<>();
  for(AtlasPrivateV2.Unit u:source.units())
   rows.add(new Macro(u.surah,u.start,u.end,"Unité "+u.ordinal+" · "+u.start+"–"+u.end));
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
 int count(){return all.size();}
 List<Macro> forSurah(int s){List<Macro> r=new ArrayList<>();for(Macro m:all)if(m.surah==s)r.add(m);return r;}
 Macro at(int s,int a){for(Macro m:all)if(m.surah==s&&m.start<=a&&m.end>=a)return m;throw new IllegalArgumentException("macro verse");}
}
