package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.webkit.WebView;
import com.quransafeguard.hifz.core.VerseRef;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;

/**
 * Private semantic-map route only: unobtrusive U1/U2 boundary markers in the
 * outside gutter of a REAL QCF page.  No change to Quran SVG, Hifz state,
 * or shared reader assets. The caller must be in the map or its linked reader.
 */
final class MapUnitMarkers {
 private static final String PREFS="haafidh_semantic_map";
 private static final String VISIBLE="unit_boundary_markers_enabled";
 private MapUnitMarkers(){}
 static boolean enabled(Context context){
  return context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getBoolean(VISIBLE,true);
 }
 static boolean toggle(Context context){
  boolean value=!enabled(context);
  context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putBoolean(VISIBLE,value).apply();
  return value;
 }
 static JSONArray onPage(int page,int surah,List<MawdudiMapData.Macro> units,GeometryRepository geometry){
  JSONArray result=new JSONArray();
  for(int k=0;k<units.size();k++){
   MawdudiMapData.Macro unit=units.get(k);
   if(unit.surah!=surah)throw new IllegalArgumentException("wrong surah unit");
   if(geometry.pageForVerse(new VerseRef(surah,unit.start))!=page)continue;
   result.put(new JSONObject().put("verse",surah+":"+unit.start).put("unit",k+1));
  }
  return result;
 }
 static void show(WebView view,int page,int surah,List<MawdudiMapData.Macro> units,GeometryRepository geometry){
  JSONArray markers=enabled(view.getContext())?onPage(page,surah,units,geometry):new JSONArray();
  // The only injected nodes belong to the semantic-map reading zone. They are
  // outside the Quran SVG and intercept neither Quran text nor taps.
  String js="(function(){"+
   "var host=document.getElementById('box'),m=document.getElementById('mushaf');"+
   "if(!host||!m)return;"+
   "var old=document.getElementById('haafidh-map-unit-marks');if(old)old.remove();"+
   "var root=document.createElement('div');root.id='haafidh-map-unit-marks';"+
   "root.style.cssText='position:absolute;inset:0;pointer-events:none;z-index:7';"+
   "host.appendChild(root);"+
   "var marks="+markers.toString()+";var box=host.getBoundingClientRect(),r=m.getBoundingClientRect();"+
   "var left=Math.max(1,r.left-box.left-29);"+
   "marks.forEach(function(x){"+
   "var els=m.querySelectorAll('.ayahPolygon');var target=null;"+
   "for(var i=0;i<els.length;i++){if(els[i].getAttribute('data-verse')===x.verse){"+
   "var t=els[i].getBoundingClientRect();if(!target||t.top<target.top)target=t;"+
   "}}"+
   "if(!target)return;"+
   "var y=Math.max(0,Math.min(host.clientHeight-18,target.top-box.top));"+
   "var label=document.createElement('span');label.textContent='U'+x.unit;"+
   "label.setAttribute('aria-label','Unité '+x.unit);"+
   "label.style.cssText='position:absolute;display:block;left:'+left+'px;top:'+y+'px;"+
   "font:600 11px sans-serif;line-height:15px;color:#000;background:#fff;"+
   "border:1px solid #000;padding:0 2px;white-space:nowrap';"+
   "root.appendChild(label);"+
   "});"+
   "})();";
  view.evaluateJavascript(js,null);
 }
}
