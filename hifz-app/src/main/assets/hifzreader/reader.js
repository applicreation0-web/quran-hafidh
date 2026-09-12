'use strict';
/* Quran Hifz local reader: canonical SVG, deterministic random word masks, no network. */
const N=window.HifzNative;
const boot=window.HIFZ_BOOT||{};
let pageGeo=boot.geometry||null;
let currentPage=Number(boot.page||1);
let selected=(boot.selection||[]).map(String);
let lineIds=(boot.lines||[]).map(String);
let mask=Number(boot.mask||0);
let eink=!!boot.eink;
let audioVerse=null;
const NS='http://www.w3.org/2000/svg';
const mushaf=document.getElementById('mushaf');

function currentSvg(){return mushaf.querySelector('svg')}
function verseOf(p){return p.getAttribute('surah')+':'+p.getAttribute('ayah')}
function selectedPolygons(svg){return [...svg.querySelectorAll('.ayahPolygon')].filter(p=>selected.includes(String(p.dataset.verse)))}

function prepare(){
  const svg=currentSvg();
  if(!svg){N?.error('SVG Mushaf absent');return}
  svg.querySelectorAll('.ayahPolygon').forEach(p=>{
    const k=verseOf(p);p.dataset.verse=k;
    p.removeAttribute('tabindex');
    p.onclick=e=>{e.stopPropagation();const [s,a]=k.split(':').map(Number);N?.verseTap(s,a)};
  });
  render();
  N?.pageShown(currentPage);
}
document.addEventListener('click',()=>N?.surfaceTap?.());

function hash32(text){
  let h=2166136261>>>0;
  for(let i=0;i<text.length;i++){
    h^=text.charCodeAt(i);
    h=Math.imul(h,16777619)>>>0;
  }
  return h>>>0;
}
function nextRandom(state){
  let x=state>>>0;
  x^=x<<13;x^=x>>>17;x^=x<<5;
  return x>>>0;
}
function canonicalWords(words){
  const unique=new Map();
  (words||[]).forEach(word=>{
    if(word&&word.kind!=='marker'&&word.id&&!unique.has(String(word.id))) unique.set(String(word.id),word);
  });
  return [...unique.values()].sort((a,b)=>String(a.id)<String(b.id)?-1:String(a.id)>String(b.id)?1:0);
}
function shuffledWords(words,seed){
  const out=canonicalWords(words);
  let state=hash32(String(seed||'hifz-word-mask'))||0x9e3779b9;
  for(let i=out.length-1;i>0;i--){
    state=nextRandom(state);
    const j=state%(i+1);
    const tmp=out[i];out[i]=out[j];out[j]=tmp;
  }
  return out;
}
function maskedWordIds(words,percent,seed){
  const ordered=shuffledWords(words,seed);
  const p=Math.max(0,Math.min(100,Number(percent)||0));
  if(!ordered.length||p<=0)return [];
  let count=Math.round(ordered.length*p/100);
  if(count<1)count=1;
  if(p>=100)count=ordered.length;
  return ordered.slice(0,Math.min(count,ordered.length)).map(word=>String(word.id));
}
function eligibleWordsForMask(geometry,selection,lines){
  if(!geometry)return [];
  const wantedLines=new Set((lines||[]).map(String));
  const wantedVerses=new Set((selection||[]).map(String));
  if(!wantedLines.size||!wantedVerses.size)return [];
  const out=[];
  (geometry.lines||[]).forEach(line=>{
    if(!wantedLines.has(String(line.id)))return;
    (line.words||[]).forEach(word=>{
      if(word&&word.kind!=='marker'&&wantedVerses.has(String(word.verse)))out.push(word);
    });
  });
  return canonicalWords(out);
}
function maskBoxesForPage(geometry,maskedIds){
  if(!geometry)return [];
  const wanted=maskedIds instanceof Set?maskedIds:new Set((maskedIds||[]).map(String));
  const out=[];
  (geometry.lines||[]).forEach(line=>(line.words||[]).forEach(word=>{
    if(!word||word.kind==='marker'||!wanted.has(String(word.id)))return;
    const x=Number(word.x),y=Number(word.y),w=Number(word.w),h=Number(word.h);
    if(!(w>0&&h>0)&&Number.isFinite(x)&&Number.isFinite(y))return;
    if(!Number.isFinite(x)||!Number.isFinite(y)||!Number.isFinite(w)||!Number.isFinite(h)||w<=0||h<=0)return;
    out.push({id:String(word.id),x,y,w,h});
  }));
  return out;
}
function maskSeed(){
  return selected.join(',')+'|'+lineIds.join(',');
}

function markerLayer(svg,polys){
  const g=document.createElementNS(NS,'g');
  const markers=svg.querySelectorAll('#ayah_markers > g');
  if(!markers.length)return g;
  const rootCtm=svg.getScreenCTM();if(!rootCtm)return g;
  const inv=rootCtm.inverse();
  markers.forEach(m=>{
    const ctm=m.getScreenCTM();if(!ctm)return;
    const full=inv.multiply(ctm),b=m.getBBox();
    const pt=svg.createSVGPoint();pt.x=b.x+b.width/2;pt.y=b.y+b.height/2;
    const c=pt.matrixTransform(full);
    if(!polys.some(p=>{try{return p.isPointInFill(c)}catch(_e){return false}}))return;
    const wrap=document.createElementNS(NS,'g');
    wrap.setAttribute('transform',`matrix(${full.a} ${full.b} ${full.c} ${full.d} ${full.e} ${full.f})`);
    [...m.childNodes].forEach(n=>wrap.appendChild(n.cloneNode(true)));
    g.appendChild(wrap);
  });
  return g;
}

function render(){
  document.body.classList.toggle('eink',eink);
  const svg=currentSvg();if(!svg)return;
  svg.querySelectorAll('.ayahPolygon').forEach(p=>{
    p.classList.toggle('selected',selected.includes(String(p.dataset.verse)));
    p.classList.toggle('audio',audioVerse!==null&&String(p.dataset.verse)===audioVerse);
  });
  svg.querySelectorAll('.masklayer').forEach(n=>n.remove());
  const clamped=Math.max(0,Math.min(100,Number(mask)||0));
  if(!clamped||!pageGeo||!lineIds.length||!selected.length)return;

  const words=eligibleWordsForMask(pageGeo,selected,lineIds);
  if(!words.length)return;
  const hidden=new Set(maskedWordIds(words,clamped,maskSeed()));
  const boxes=maskBoxesForPage(pageGeo,hidden);
  if(!boxes.length)return;

  const layer=document.createElementNS(NS,'g');layer.setAttribute('class','masklayer');
  boxes.forEach(box=>{
    const inset=Math.min(.35,box.w*.025);
    const el=document.createElementNS(NS,'rect');
    el.setAttribute('class','maskcell');
    el.setAttribute('x',box.x+inset);el.setAttribute('y',box.y);
    el.setAttribute('width',Math.max(.5,box.w-inset*2));el.setAttribute('height',box.h);
    el.setAttribute('rx','2');el.setAttribute('ry','2');
    layer.appendChild(el);
  });
  const polys=selectedPolygons(svg);
  if(polys.length)layer.appendChild(markerLayer(svg,polys));
  svg.appendChild(layer);
}

/* Move only when the selected passage would actually be hidden by the Tafsir panel. */
function revealSelection(visibleFraction){
  const svg=currentSvg();
  document.documentElement.style.setProperty('--reveal-shift','0px');
  if(!svg||!selected.length)return;
  const nodes=selectedPolygons(svg);if(!nodes.length)return;
  requestAnimationFrame(()=>{
    const viewport=Math.max(1,window.innerHeight||document.documentElement.clientHeight||1);
    const fraction=Math.max(.30,Math.min(.70,Number(visibleFraction)||.46));
    let top=Infinity,bottom=-Infinity;
    nodes.forEach(p=>{const r=p.getBoundingClientRect();if(r.height>0){top=Math.min(top,r.top);bottom=Math.max(bottom,r.bottom)}});
    if(!Number.isFinite(top))return;
    const limit=viewport*fraction-12;
    const safeTop=viewport*.04;
    if(bottom<=limit&&top>=safeTop)return;
    const height=bottom-top;
    let needed=Math.max(0,bottom-limit);
    let maxUp=Math.max(0,top-safeTop);
    if(height>limit-safeTop)needed=maxUp;
    const delta=Math.min(needed,maxUp);
    if(delta>0)document.documentElement.style.setProperty('--reveal-shift',(-delta)+'px');
  });
}
function clearReveal(){document.documentElement.style.setProperty('--reveal-shift','0px')}

window.HifzReader={
  setGeometry(geometry){pageGeo=geometry||null;render()},
  setMask(hidden){mask=Number(hidden||0);render()},
  setSelection(selection,lines){selected=(selection||[]).map(String);lineIds=(lines||[]).map(String);clearReveal();render()},
  setAudioVerse(value){audioVerse=value==null?null:String(value);render()},
  setEink(value){eink=!!value;render()},
  revealSelection(visibleFraction){revealSelection(visibleFraction)},
  clearReveal(){clearReveal()},
  page(){return currentPage}
};

if(typeof module!=='undefined'&&module.exports)module.exports={maskedWordIds,eligibleWordsForMask,maskBoxesForPage};
prepare();
N?.ready();
