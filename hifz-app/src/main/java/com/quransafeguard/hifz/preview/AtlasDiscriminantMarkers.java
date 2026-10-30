package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import org.json.JSONArray;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Read-only Atlas relation markers in the OUTSIDE gutter of the
 * original QCF Mushaf. Never edits glyph SVG or Hifz data and never guesses
 * the verse position from line numbers.
 */
final class AtlasDiscriminantMarkers {
    private AtlasDiscriminantMarkers() {}

    static JSONArray onPage(int page,GeometryRepository geometry) {
        JSONArray marked=new JSONArray();
        Set<String> unique=new LinkedHashSet<>();
        for(AtlasDiscriminantCases.Entry entry:AtlasDiscriminantCases.PILOT) {
            for(VerseRef v:new VerseRef[]{entry.first,entry.second}) {
                if(geometry.pageForVerse(v)==page && unique.add(v.toString()))
                    marked.put(v.toString());
            }
        }
        return marked;
    }

    static void show(MushafView view,int page,GeometryRepository geometry,boolean suppressed) {
        // Hide all hints with the free active mask; never annotate scheduled Hifz.
        JSONArray rows=suppressed || geometry==null?new JSONArray():onPage(page,geometry);
        show(view,rows);
    }

    /** Session callers supply only their permitted exact-page/corpus references. */
    static void show(MushafView view,JSONArray rows) {
        String js="(function(){"+
            "var old=document.getElementById('atlas-discriminant-marks');if(old)old.remove();"+
            "var host=document.getElementById('box'),m=document.getElementById('mushaf');"+
            "if(!host||!m)return;"+
            "var rows="+rows.toString()+";if(!rows.length)return;"+
            "var frame=host.getBoundingClientRect(),page=m.getBoundingClientRect();"+
            "var left=page.left-frame.left,right=frame.right-page.right;"+
            // A narrow reader has no safe outside gutter: toolbar action is the fallback.
            "var side=left>=32?'left':right>=32?'right':null;if(!side)return;"+
            "var x=side==='left'?left-30:page.right-frame.left+4;"+
            "var root=document.createElement('div');root.id='atlas-discriminant-marks';"+
            "root.style.cssText='position:absolute;inset:0;z-index:9;pointer-events:none';"+
            "host.appendChild(root);"+
            "var polys=m.querySelectorAll('.ayahPolygon');"+
            "rows.forEach(function(ref){"+
            "var p=ref.split(':'),target=null;"+
            "for(var i=0;i<polys.length;i++){var e=polys[i];"+
            "if(e.getAttribute('data-verse')===ref||"+
            "(e.getAttribute('surah')===p[0]&&e.getAttribute('ayah')===p[1])){"+
            "var r=e.getBoundingClientRect();if(!target||r.top<target.top)target=r;"+
            "}}"+
            "if(!target)return;"+
            "var y=Math.max(1,Math.min(host.clientHeight-28,target.top-frame.top));"+
            "var btn=document.createElement('button');btn.type='button';"+
            "btn.setAttribute('aria-label','Versets ressemblants '+ref);"+
            "btn.setAttribute('title','Versets ressemblants '+ref);"+
            "btn.style.cssText='position:absolute;left:'+x+'px;top:'+y+'px;"+
            "width:26px;height:26px;padding:3px;margin:0;pointer-events:auto;"+
            "border:1px solid #777;border-radius:6px;background:#faf8f2;color:#121211';"+
            "btn.innerHTML='<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" "+
            "width=\"18\" height=\"18\" fill=\"none\" stroke=\"currentColor\" "+
            "stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\">"+
            "<path d=\"M2.5 5.5h6v13h-6zM15.5 5.5h6v13h-6zM8.5 12h7\"/>"+
            "<circle cx=\"5.5\" cy=\"12\" r=\"1.7\"/><circle cx=\"18.5\" cy=\"12\" r=\"1.7\"/></svg>';"+
            "btn.addEventListener('click',function(event){event.stopPropagation();"+
            "HifzNative.comparisonTap(Number(p[0]),Number(p[1]));});"+
            "root.appendChild(btn);"+
            "});"+
            "})();";
        view.evaluateJavascript(js,null);
    }
}
