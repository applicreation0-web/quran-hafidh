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
            // Clear observers when changing page or suppressing active recall hints.
            "if(window.__haafidhAtlasMarkerDispose){window.__haafidhAtlasMarkerDispose();window.__haafidhAtlasMarkerDispose=null;}"+
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
            "stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\">"+
            "<path d=\"M9 17H7A5 5 0 0 1 7 7h2\"/><path d=\"M15 7h2a5 5 0 1 1 0 10h-2\"/><line x1=\"8\" x2=\"16\" y1=\"12\" y2=\"12\"/></svg>';"+
            "btn.addEventListener('click',function(event){event.stopPropagation();"+
            "HifzNative.comparisonTap(Number(p[0]),Number(p[1]));});"+
            "root.appendChild(btn);"+
            // The QCF page can finish native WebView fitting AFTER the first page
            // measure. Always realign the button against the actual current page;
            // otherwise a BOOX landscape/portrait switch can overlay Quran ink.
            "var realign=function(){var f=host.getBoundingClientRect(),q=m.getBoundingClientRect();"+
            "var l=q.left-f.left,r=f.right-q.right;"+
            "var lane=l>=46?'left':r>=46?'right':null;"+
            "if(!lane){btn.style.visibility='hidden';return;}"+
            "btn.style.visibility='visible';"+
            "btn.style.left=(lane==='left'?l-46:q.right-f.left+2)+'px';};"+
            "var observer=typeof ResizeObserver==='function'?new ResizeObserver(function(){requestAnimationFrame(realign);}):null;"+
            "if(observer){observer.observe(host);observer.observe(m);}"+
            "window.addEventListener('resize',realign,{passive:true});"+
            "window.__haafidhAtlasMarkerDispose=function(){if(observer)observer.disconnect();window.removeEventListener('resize',realign);};"+
            "realign();requestAnimationFrame(function(){realign();requestAnimationFrame(realign);});"+
            "})();";
        view.evaluateJavascript(js,null);
    }
}
