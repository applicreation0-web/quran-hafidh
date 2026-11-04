package com.quransafeguard.hifz.preview;

import org.json.JSONArray;

/**
 * Verse-anchored, two-lane contextual affordances on the OUTSIDE of the QCF
 * folio: link-2 for Similitudes, exact Lucide git-compare for Correspondances.
 * Reads actual .ayahPolygon coordinates, never inserts inside the SVG.
 * Shared legacy disposal hook ensures masking removes both kinds immediately.
 */
final class CorrespondenceGutterMarkers {
    private CorrespondenceGutterMarkers() {}

    static void show(MushafView view, JSONArray similitudes, JSONArray correspondances) {
        if(view==null)return;
        JSONArray links=similitudes==null?new JSONArray():similitudes;
        JSONArray parallels=correspondances==null?new JSONArray():correspondances;
        String js="(function(){"+
          "if(window.__haafidhAtlasMarkerDispose){window.__haafidhAtlasMarkerDispose();window.__haafidhAtlasMarkerDispose=null;}"+
          "var old=document.getElementById('atlas-discriminant-marks');if(old)old.remove();"+
          "var host=document.getElementById('box'),m=document.getElementById('mushaf');if(!host||!m)return;"+
          "var links="+links.toString()+",parallels="+parallels.toString()+";"+
          "var items=[],seen={};"+
          "function append(rows,kind){rows.forEach(function(ref){if(!/^\\d+:\\d+$/.test(ref))return;"+
          "var key=kind+ref;if(seen[key])return;seen[key]=1;items.push({ref:ref,kind:kind});});}"+
          "append(links,'similarity');append(parallels,'correspondence');"+
          // Preserve the original QCF 345:550 ratio, reserving two 46px gutter lanes.
          "m.style.width=items.length?'min(calc(100vw - 96px),calc((100vh - 4px) * 345 / 550))':'';"+
          "window.dispatchEvent(new Event('resize'));if(!items.length)return;"+
          "var root=document.createElement('div');root.id='atlas-discriminant-marks';"+
          "root.style.cssText='position:absolute;inset:0;z-index:9;pointer-events:none';host.appendChild(root);"+
          "var linkIcon='<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" width=\"26\" height=\"26\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\">"+
          "<path d=\"M9 17H7A5 5 0 0 1 7 7h2\"/><path d=\"M15 7h2a5 5 0 1 1 0 10h-2\"/><line x1=\"8\" x2=\"16\" y1=\"12\" y2=\"12\"/></svg>';"+
          "var compareIcon='<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" width=\"26\" height=\"26\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\">"+
          "<circle cx=\"18\" cy=\"18\" r=\"3\"/><circle cx=\"6\" cy=\"6\" r=\"3\"/><path d=\"M13 6h3a2 2 0 0 1 2 2v7\"/><path d=\"M11 18H8a2 2 0 0 1-2-2V9\"/></svg>';"+
          "items.forEach(function(item){var p=item.ref.split(':'),btn=document.createElement('button');"+
          "btn.type='button';btn.style.cssText='position:absolute;width:44px;height:44px;margin:0;padding:6px;"+
          "border:1px solid #777;border-radius:6px;background:#faf8f2;color:#121211;pointer-events:auto;"+
          "display:flex;align-items:center;justify-content:center';"+
          "btn.setAttribute('aria-label',(item.kind==='similarity'?'Similitudes ':'Correspondances ')+item.ref);"+
          "btn.setAttribute('title',btn.getAttribute('aria-label'));"+
          "btn.innerHTML=item.kind==='similarity'?linkIcon:compareIcon;"+
          "btn.addEventListener('click',function(e){e.stopPropagation();"+
          "if(item.kind==='similarity')HifzNative.comparisonTap(Number(p[0]),Number(p[1]));"+
          "else HifzNative.correspondenceTap(Number(p[0]),Number(p[1]));});"+
          "item.button=btn;root.appendChild(btn);});"+
          "var pending=false;function layout(){pending=false;"+
          "var f=host.getBoundingClientRect(),q=m.getBoundingClientRect();"+
          "var l=q.left-f.left,r=f.right-q.right;"+
          "var polys=m.querySelectorAll('.ayahPolygon'),placed={left:[],right:[]};"+
          "var positioned=[];items.forEach(function(item){var p=item.ref.split(':'),anchor=null;"+
          "for(var i=0;i<polys.length;i++){var poly=polys[i];"+
          "if(poly.getAttribute('data-verse')===item.ref||"+
          "(poly.getAttribute('surah')===p[0]&&poly.getAttribute('ayah')===p[1])){"+
          "var rect=poly.getBoundingClientRect();if(!anchor||rect.top<anchor.top)anchor=rect;}}"+
          "if(anchor)positioned.push({item:item,y:anchor.top-f.top});"+
          "else item.button.style.visibility='hidden';});"+
          "positioned.sort(function(a,b){return a.y-b.y;});"+
          "positioned.forEach(function(row){var item=row.item,side=item.kind==='similarity'?'left':'right';"+
          "if(side==='left'&&l<46){side=r>=46?'right':null;}"+
          "if(side==='right'&&r<46){side=l>=46?'left':null;}"+
          "if(!side){item.button.style.visibility='hidden';return;}"+
          "var x=side==='left'?l-46:q.right-f.left+2;"+
          "var maxY=Math.max(0,host.clientHeight-44),desired=Math.max(0,Math.min(maxY,row.y-22));"+
          "var used=placed[side],pos=null;"+
          // Nearest available 44px tap target; never overlap icons or Quran ink.
          "for(var d=0;d<=maxY+2;d+=2){var up=desired-d,down=desired+d;"+
          "if(up>=0&&used.every(function(y){return Math.abs(y-up)>=44;})){pos=up;break;}"+
          "if(d>0&&down<=maxY&&used.every(function(y){return Math.abs(y-down)>=44;})){pos=down;break;}}"+
          "if(pos===null){item.button.style.visibility='hidden';return;}used.push(pos);"+
          "item.button.style.visibility='visible';item.button.style.left=Math.round(x)+'px';"+
          "item.button.style.top=Math.round(pos)+'px';});}"+
          "function schedule(){if(pending)return;pending=true;requestAnimationFrame(layout);}"+
          "var observer=typeof ResizeObserver==='function'?new ResizeObserver(schedule):null;"+
          "if(observer){observer.observe(host);observer.observe(m);}"+
          "window.addEventListener('resize',schedule,{passive:true});"+
          "window.__haafidhAtlasMarkerDispose=function(){if(observer)observer.disconnect();"+
          "window.removeEventListener('resize',schedule);};"+
          "schedule();requestAnimationFrame(schedule);"+
          "})();";
        view.evaluateJavascript(js,null);
    }
}