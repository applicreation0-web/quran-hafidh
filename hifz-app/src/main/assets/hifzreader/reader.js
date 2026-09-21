'use strict';
/* Quran Hifz local reader: canonical SVG, non-deterministic random source-ink masks, no network. */
const N=window.HifzNative;
const boot=window.HIFZ_BOOT||{};
let pageGeo=boot.geometry||null;
let currentPage=Number(boot.page||1);
let selected=(boot.selection||[]).map(String);
let lineIds=(boot.lines||[]).map(String);
let mask=Number(boot.mask||0);
const maskEntropy=String(boot.maskEntropy||'hifz-test');
let eink=!!boot.eink;
let strictLineFocus=!!boot.strictLineFocus;
let highlighted=new Set((boot.highlights||[]).map(String));
let landmarkStart=boot.landmarkStart?String(boot.landmarkStart):null;
let landmarkEnd=boot.landmarkEnd?String(boot.landmarkEnd):null;
/*
 * Sabqi/Itqan's `selected` verses ARE the memorization block, and can share a physical line with
 * un-selected neighbor verses (a rep's block may start or end mid-line) — for those modes, masking
 * must stay clipped to the selected verses' own polygons so a neighbor's text on the same line
 * never gets exposed as maskable. Murajaah instead uses `selected` purely to flag the "last verse
 * actually revised" for display; it has no bearing on how much of the page should be maskable, so
 * it must NOT also narrow the mask pool down to that one verse's own shape.
 */
let maskFollowsSelection=boot.maskFollowsSelection!==false;
let audioVerse=null;
let maskOrderSignature='';
let maskOrder=[];
const NS='http://www.w3.org/2000/svg';
const mushaf=document.getElementById('mushaf');

function currentSvg(){return mushaf.querySelector('svg')}
function verseOf(p){return p.getAttribute('surah')+':'+p.getAttribute('ayah')}
function selectedPolygons(svg){return [...svg.querySelectorAll('.ayahPolygon')].filter(p=>selected.includes(String(p.dataset.verse)))}
function shadeVerseSelection(){if(strictLineFocus)return false;return true}

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

function insideSelection(polys,x,y){
  const svg=currentSvg();if(!svg)return false;
  const pt=svg.createSVGPoint();pt.x=x;pt.y=y;
  return polys.some(p=>{try{return p.isPointInFill(pt)}catch(_e){return false}});
}

/*
 * A page's first/last line can be flagged as a synchronization landmark: half of it (by cell
 * count) is excluded from masking entirely, so it always stays visible. Cells are stored in
 * ascending x order (left to right) while Arabic reads right to left, so the cell array's tail
 * holds the line's *first*-read words and its head holds the *last*-read words. landmarkStart
 * therefore keeps the tail half (the words a reader starts the page on); landmarkEnd keeps the
 * head half (the words right before the page turns) — the other half of that same line still
 * masks normally, like every other line.
 */
function landmarkCellIndices(cellCount,role){
  if(cellCount<=1)return null;
  const reveal=Math.ceil(cellCount/2);
  return role==='start'
    ? {from:0,to:cellCount-reveal}   // mask candidates: the head half only
    : {from:reveal,to:cellCount};    // mask candidates: the tail half only
}

function maskCandidates(lines,polys){
  const out=[];
  lines.forEach(line=>{
    const top=Number(line.top),bottom=Number(line.bottom);
    const cells=line.cells||[];
    const lineId=String(line.id);
    const role=lineId===landmarkStart?'start':(lineId===landmarkEnd?'end':null);
    const range=role?landmarkCellIndices(cells.length,role):null;
    cells.forEach((cell,ci)=>{
      if(range&&(ci<range.from||ci>=range.to))return;
      const x0=Number(cell[0]),x1=Number(cell[1]);
      if(polys.length&&!insideSelection(polys,(x0+x1)/2,(top+bottom)/2))return;
      out.push({key:lineId+':'+ci,lineId,index:ci,x0,x1,top,bottom});
    });
  });
  return out;
}

function maskRect(segment){
  const el=document.createElementNS(NS,'rect');
  el.setAttribute('class','maskcell');
  el.setAttribute('x',segment.x);el.setAttribute('y',segment.y);
  el.setAttribute('width',segment.width);el.setAttribute('height',segment.height);
  el.setAttribute('rx','2');el.setAttribute('ry','2');
  return el;
}

function lineFocusLayer(lines){
  const layer=document.createElementNS(NS,'g');
  layer.setAttribute('class','linefocuslayer');
  (lines||[]).forEach(line=>{
    const cells=line.cells||[];if(!cells.length)return;
    let x0=Infinity,x1=-Infinity;
    cells.forEach(cell=>{
      x0=Math.min(x0,Number(cell[0]));
      x1=Math.max(x1,Number(cell[1]));
    });
    const top=Number(line.top),bottom=Number(line.bottom);
    if(!Number.isFinite(x0)||!Number.isFinite(x1)||!Number.isFinite(top)||!Number.isFinite(bottom)||x1<=x0||bottom<=top)return;
    const rect=document.createElementNS(NS,'rect');
    rect.setAttribute('class','linefocuscell');
    rect.setAttribute('x',x0);
    rect.setAttribute('y',top+0.25);
    rect.setAttribute('width',x1-x0);
    rect.setAttribute('height',Math.max(0,bottom-top-0.5));
    rect.setAttribute('rx','1.5');rect.setAttribute('ry','1.5');
    layer.appendChild(rect);
  });
  return layer;
}

/* Personal weak-spot flags: a thin dashed outline cloned above every layer (including any
 * active mask), never a filled shade — deliberately the lightest possible mark to avoid E-Ink
 * ghosting from a shape that can stay on screen for many page views. */
function weakLayer(svg,weakSet){
  const g=document.createElementNS(NS,'g');
  g.setAttribute('class','weaklayer');
  if(!weakSet||!weakSet.size)return g;
  svg.querySelectorAll('.ayahPolygon').forEach(p=>{
    if(!weakSet.has(String(p.dataset.verse)))return;
    const outline=p.cloneNode(false);
    outline.removeAttribute('class');
    outline.removeAttribute('style');
    outline.removeAttribute('id');
    outline.setAttribute('class','weakoutline');
    g.appendChild(outline);
  });
  return g;
}

function hashSeed(value){
  let h=2166136261>>>0;
  const s=String(value);
  for(let i=0;i<s.length;i++){h^=s.charCodeAt(i);h=Math.imul(h,16777619)>>>0;}
  return h||0x9e3779b9;
}

function seededRandom(seed){
  let x=hashSeed(seed);
  return ()=>{
    x^=(x<<13);x>>>=0;
    x^=(x>>>17);x>>>=0;
    x^=(x<<5);x>>>=0;
    return x/4294967296;
  };
}

function randomOrderKeys(cells,rng){
  const out=(cells||[]).map(c=>String(c.key));
  const draw=typeof rng==='function'?rng:Math.random;
  for(let i=out.length-1;i>0;i--){
    const r=Math.max(0,Math.min(0.999999999999,Number(draw())||0));
    const j=Math.floor(r*(i+1));
    const t=out[i];out[i]=out[j];out[j]=t;
  }
  return out;
}

function currentRandomOrder(cells){
  const keys=(cells||[]).map(c=>String(c.key)).sort();
  const signature=selected.join(',')+'|'+lineIds.join(',')+'|'+keys.join(',');
  if(signature!==maskOrderSignature){
    maskOrderSignature=signature;
    maskOrder=randomOrderKeys(cells,seededRandom(maskEntropy+'|'+signature));
  }
  return maskOrder;
}

/* Random cumulative masking over real source-ink groups. 25% is a fresh draw for a
 * new passage; 50/75/100 extend that same draw. Whitespace is never bridged. */
function randomSegmentsForCells(cells,percent,order){
  const fraction=Math.max(0,Math.min(1,Number(percent)/100));
  if(!fraction||!cells||!cells.length)return [];
  const map=new Map(cells.map(c=>[String(c.key),c]));
  const ordered=(order||[]).map(k=>map.get(String(k))).filter(Boolean);
  const seen=new Set(ordered.map(c=>String(c.key)));
  cells.forEach(c=>{if(!seen.has(String(c.key)))ordered.push(c)});
  const totalWidth=cells.reduce((sum,c)=>sum+Math.max(0,c.x1-c.x0),0);
  if(!totalWidth)return [];
  let remaining=totalWidth*fraction;
  const segments=[];
  for(const cell of ordered){
    if(remaining<=0.0001)break;
    const cellWidth=Math.max(0,cell.x1-cell.x0);if(!cellWidth)continue;
    const hiddenWidth=Math.min(cellWidth,remaining);
    segments.push({key:String(cell.key),lineId:String(cell.lineId),x:cell.x1-hiddenWidth,y:cell.top+0.6,width:hiddenWidth,height:Math.max(0,(cell.bottom-cell.top)-1.2)});
    remaining-=hiddenWidth;
  }
  return segments;
}

function markerLayer(svg,polys,lines){
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
    const visible=polys.length
      ? insideSelection(polys,c.x,c.y)
      : (lines||[]).some(line=>c.y>=Number(line.top)&&c.y<=Number(line.bottom));
    if(!visible)return;
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
    p.classList.toggle('selected',shadeVerseSelection()&&selected.includes(String(p.dataset.verse)));
    p.classList.toggle('audio',audioVerse!==null&&String(p.dataset.verse)===audioVerse);
  });
  svg.querySelectorAll('.masklayer,.linefocuslayer,.weaklayer').forEach(n=>n.remove());

  const wanted=new Set(lineIds.map(String));
  const lines=pageGeo&&lineIds.length
    ? (pageGeo.lines||[]).filter(l=>wanted.has(String(l.id)))
    : [];
  if(strictLineFocus&&lines.length){
    const focus=lineFocusLayer(lines);
    if(focus.childNodes.length)svg.appendChild(focus);
  }

  const clamped=Math.max(0,Math.min(100,Number(mask)||0));
  if(clamped&&pageGeo&&lineIds.length&&lines.length){
    const polys=maskFollowsSelection?selectedPolygons(svg):[],cells=maskCandidates(lines,polys);
    if(cells.length){
      const segments=randomSegmentsForCells(cells,clamped,currentRandomOrder(cells));
      const layer=document.createElementNS(NS,'g');layer.setAttribute('class','masklayer');
      if(polys.length){
        /*
         * A cell's rectangle is the raw geometric line-cell box, not the selected verse's exact
         * glyph outline — on a line shared by more than one verse, that box can slightly overhang
         * a neighbor verse that isn't part of today's block, so clipping to the selected verses'
         * own polygons stays mandatory there. A single-verse line has no neighbor ink to bleed
         * onto, so its cells render as clean rectangles instead of being cut to the polygon shape.
         */
        const multiVerseLines=new Set(lines.filter(l=>(l.verses||[]).length>1).map(l=>String(l.id)));
        const open=[],clipped=[];
        segments.forEach(segment=>(multiVerseLines.has(segment.lineId)?clipped:open).push(segment));
        if(open.length){
          const openGroup=document.createElementNS(NS,'g');
          open.forEach(segment=>openGroup.appendChild(maskRect(segment)));
          layer.appendChild(openGroup);
        }
        if(clipped.length){
          const defs=document.createElementNS(NS,'defs'),clip=document.createElementNS(NS,'clipPath');
          clip.id='hifz-selection-clip';
          polys.forEach(p=>{const q=p.cloneNode(false);q.removeAttribute('class');q.removeAttribute('style');clip.appendChild(q)});
          defs.appendChild(clip);layer.appendChild(defs);
          const clippedGroup=document.createElementNS(NS,'g');
          clippedGroup.setAttribute('clip-path','url(#hifz-selection-clip)');
          clipped.forEach(segment=>clippedGroup.appendChild(maskRect(segment)));
          layer.appendChild(clippedGroup);
        }
      } else {
        const group=document.createElementNS(NS,'g');
        segments.forEach(segment=>group.appendChild(maskRect(segment)));
        layer.appendChild(group);
      }
      // Verse-number rosettes are deliberately redrawn above the random masks.
      layer.appendChild(markerLayer(svg,polys,lines));
      svg.appendChild(layer);
    }
  }

  // Weak-spot outlines always draw last, on top of any mask, so a flagged verse stays
  // recognizable (as a bare outline, revealing no text) even while its content is hidden.
  if(highlighted.size){
    const weak=weakLayer(svg,highlighted);
    if(weak.childNodes.length)svg.appendChild(weak);
  }
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
  setSelection(selection,lines){
    const nextSelected=(selection||[]).map(String),nextLines=(lines||[]).map(String);
    const changed=nextSelected.join(',')!==selected.join(',')||nextLines.join(',')!==lineIds.join(',');
    selected=nextSelected;lineIds=nextLines;
    if(changed){maskOrderSignature='';maskOrder=[];}
    clearReveal();render();
  },
  setAudioVerse(value){audioVerse=value==null?null:String(value);render()},
  setHighlights(list){highlighted=new Set((list||[]).map(String));render()},
  setLandmarks(startId,endId){landmarkStart=startId?String(startId):null;landmarkEnd=endId?String(endId):null;render()},
  setMaskFollowsSelection(value){maskFollowsSelection=!!value;render()},
  setEink(value){eink=!!value;render()},
  revealSelection(visibleFraction){revealSelection(visibleFraction)},
  clearReveal(){clearReveal()},
  page(){return currentPage}
};

if(typeof module!=='undefined'&&module.exports)module.exports={randomOrderKeys,randomSegmentsForCells,seededRandom,lineFocusLayer,landmarkCellIndices,maskCandidates};
prepare();
N?.ready();