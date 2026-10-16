/* Display-only targets. No access to masks, recall holes, selection or Hifz state. */
(function(root){
 'use strict';
 function mountTitleTargets(svg,cues,send){
  const doc=svg.ownerDocument,ns='http://www.w3.org/2000/svg';
  const layer=doc.createElementNS(ns,'g');
  layer.setAttribute('class','ayah-themes-title-targets');
  const vb=svg.viewBox&&svg.viewBox.baseVal;
  const dx=vb&&Number.isFinite(vb.x)?vb.x:0,dy=vb&&Number.isFinite(vb.y)?vb.y:0;
  (Array.isArray(cues)?cues:[]).forEach(c=>{
   if(!c||typeof c.id!=='string'||!c.id)return;
   (c.boxes||[]).forEach(b=>{
    if(!Array.isArray(b)||b.length!==4||!b.every(Number.isFinite)
       ||b[0]<0||b[1]<0||b[2]>345||b[3]>550||b[2]<=b[0]||b[3]<=b[1])return;
    const r=doc.createElementNS(ns,'rect');
    r.setAttribute('x',b[0]+dx);r.setAttribute('y',b[1]+dy);
    r.setAttribute('width',b[2]-b[0]);r.setAttribute('height',b[3]-b[1]);
    r.setAttribute('fill','transparent');r.setAttribute('stroke','currentColor');
    r.setAttribute('stroke-width','0.6');r.setAttribute('stroke-opacity','0.45');
    r.setAttribute('pointer-events','all');r.setAttribute('role','button');
    r.setAttribute('aria-label','Show passage title');
    r.onclick=e=>{e.stopPropagation();send('AT:'+c.id);};
    layer.appendChild(r);
   });
  });
  svg.appendChild(layer);return layer;
 }
 let observer=null,layer=null;
 function clear(){if(observer)observer.disconnect();observer=null;if(layer)layer.remove();layer=null;}
 function install(cues){
  clear();
  const svg=typeof currentSvg==='function'?currentSvg():null;
  if(!svg||!Array.isArray(cues)||!cues.length)return;
  layer=mountTitleTargets(svg,cues,id=>root.HifzNative&&root.HifzNative.semanticCueTap(id));
  // The immutable renderer can append a new mask. Move ONLY this display group
  // above it; never remove/rewrite any mask, historical cue or Quran glyph.
  observer=new MutationObserver(()=>{if(layer&&svg.lastElementChild!==layer)svg.appendChild(layer);});
  observer.observe(svg,{childList:true});
 }
 if(typeof module!=='undefined'&&module.exports)module.exports={mountTitleTargets};
 if(root){if(root.AyahThemesReviewDisplay)root.AyahThemesReviewDisplay.clear();root.AyahThemesReviewDisplay={install,clear};}
})(typeof window==='undefined'?null:window);
