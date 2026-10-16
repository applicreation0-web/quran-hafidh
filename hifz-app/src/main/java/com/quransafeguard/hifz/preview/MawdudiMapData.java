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
  return parse(read(c,"ayah_themes_private/macros_114.json"),index);
 }
 static MawdudiMapData parse(String raw,MawdudiPassageIndex index)throws JSONException {
  JSONObject root=new JSONObject(raw);
  if(!"MAWDUDI_SOURCE_SAFE_MACROS_V1".equals(root.optString("schema")))throw new IllegalStateException("wrong macro schema");
  JSONArray rows=root.getJSONArray("macros");if(rows.length()!=119)throw new IllegalStateException("missing macro");
  List<Macro> macros=new ArrayList<>();int i=0;
  for(int s=1;s<=114;s++) {
   List<MawdudiPassageIndex.Passage> ps=index.forSurah(s);int last=ps.get(ps.size()-1).lastAyah;
   int[][] exact=s==3?new int[][]{{1,32},{33,63},{64,120},{121,200}}:
    s==9?new int[][]{{1,37},{38,72},{73,129}}:new int[][]{{1,last}};
   for(int[] range:exact) {
    JSONObject row=rows.getJSONObject(i++);
    String expected=exact.length==1?"WHOLE_SURAH_ONLY_NO_NEW_SPLIT_INVENTED":"SOURCE_BOUNDARY_CONFIRMED_FROM_PDF";
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
