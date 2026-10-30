package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import org.json.JSONArray;

/**
 * Read-only Atlas relation markers in the OUTSIDE gutter of the
 * original QCF Mushaf. Never edits glyph SVG or Hifz data and never guesses
 * the verse position from line numbers.
 */
final class AtlasDiscriminantMarkers {
    private AtlasDiscriminantMarkers() {}

    static JSONArray onPage(int page,GeometryRepository geometry) {
        JSONArray marked=new JSONArray();
        for(VerseRef verse:AtlasDiscriminantCases.versesOnPage(page))
            marked.put(verse.toString());
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
            "var rows="+rows.toString()+";"+
            "m.style.width=rows.length?'min(calc(100vw - 96px),calc((100vh - 4px) * 345 / 550))':'';"+
            "window.dispatchEvent(new Event('resize'));if(!rows.length)return;"+
            "var frame=host.getBoundingClientRect(),page=m.getBoundingClientRect();"+
            "var left=page.left-frame.left,right=frame.right-page.right;"+
            // Reserve a real outside margin on narrow screens; Quran retains its proportions.
            "var side=left>=46?'left':right>=46?'right':null;if(!side)return;"+
            "var x=side==='left'?left-46:page.right-frame.left+2;"+
            "var root=document.createElement('div');root.id='atlas-discriminant-marks';"+
            "root.style.cssText='position:absolute;inset:0;z-index:9;pointer-events:none';"+
            "host.appendChild(root);"+
            "var polys=m.querySelectorAll('.ayahPolygon');"+
            "var chosen=null,anchor=null;rows.forEach(function(ref){"+
            "var p=ref.split(':'),target=null;"+
            "for(var i=0;i<polys.length;i++){var e=polys[i];"+
            "if(e.getAttribute('data-verse')===ref||"+
            "(e.getAttribute('surah')===p[0]&&e.getAttribute('ayah')===p[1])){"+
            "var r=e.getBoundingClientRect();if(!target||r.top<target.top)target=r;"+
            "}}"+
            "if(target&&(!anchor||target.top<anchor.top)){anchor=target;chosen=ref;}"+
            "});if(!anchor||!chosen)return;var ref=chosen,p=ref.split(':');"+
            "var y=Math.max(1,Math.min(host.clientHeight-46,anchor.top-frame.top));"+
            "var btn=document.createElement('button');btn.type='button';"+
            "btn.setAttribute('aria-label','Versets ressemblants '+ref);"+
            "btn.setAttribute('title','Versets ressemblants '+ref);"+
            "btn.style.cssText='position:absolute;left:'+x+'px;top:'+y+'px;"+
            "width:44px;height:44px;padding:8px;margin:0;pointer-events:auto;"+
            "border:1px solid #777;border-radius:6px;background:#faf8f2;color:#121211';"+
            "btn.innerHTML='<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" "+
            "width=\"24\" height=\"24\" fill=\"none\" stroke=\"currentColor\" "+
            "stroke-width=\"2.6\" stroke-linecap=\"round\" stroke-linejoin=\"round\">"+
            "<path d=\"M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71\"/>"+
            "<path d=\"M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71\"/></svg>';"+
            "btn.addEventListener('click',function(event){event.stopPropagation();"+
            "HifzNative.comparisonTap(Number(p[0]),Number(p[1]));});"+
            "root.appendChild(btn);"+
            "})();";
        view.evaluateJavascript(js,null);
    }
}
