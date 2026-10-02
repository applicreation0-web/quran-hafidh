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
let semanticCues=Array.isArray(boot.semanticCues)?boot.semanticCues:[];
let semanticAnchorMaskMode=!!boot.semanticAnchorMaskMode;
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

/*
 * Right/left-page memory cue (odd page number = right-hand page, even = left-hand page, the same
 * parity every printed book uses). The 3 short bars must NEVER sit on top of the actual Quran
 * text, and real per-page margins inside the Mushaf image are far too thin on most of the 604
 * pages to guarantee that (as little as ~4px on some pages) — so this only ever draws in the
 * blank gutter that already exists beside #mushaf on wider screens (a side-effect of its fixed
 * 345:550 aspect ratio), never inside the Mushaf element itself, and stays hidden entirely on
 * narrower screens where no such gutter exists. The Quran page's own size and position are never
 * touched by this.
 */
function updateSideMarks(){
  const marks=document.getElementById('sidemarks');
  const mushafEl=document.getElementById('mushaf');
  if(!marks||!mushafEl)return;
  const markWidth=eink?1.7:1.4,markGap=3,safety=3;
  const marksWidth=3*markWidth+2*markGap;
  const minGutter=marksWidth+2*safety;
  const viewportWidth=document.documentElement.clientWidth||window.innerWidth||0;
  const rect=mushafEl.getBoundingClientRect();
  const rightGutter=viewportWidth-rect.right;
  const leftGutter=rect.left;
  const onOuterRight=currentPage%2===1;
  const gutter=onOuterRight?rightGutter:leftGutter;
  if(!(gutter>=minGutter)){marks.classList.remove('show');return}
  const inset=(gutter-marksWidth)/2;
  marks.style.left=onOuterRight?'auto':inset+'px';
  marks.style.right=onOuterRight?inset+'px':'auto';
  marks.style.top=(rect.top+rect.height*0.30)+'px';
  marks.style.height=(rect.height*0.40)+'px';
  marks.classList.add('show');
}
window.addEventListener('resize',()=>requestAnimationFrame(updateSideMarks));

/*
 * Center/spine cue: a single "tasbih" thread — a thin vertical line strung with small
 * diamond beads — drawn in the gutter on the OPPOSITE side from #sidemarks, marking where
 * the book's binding would be. Spans the Mushaf's full rendered height (there is no natural
 * midpoint to draw the eye to here, unlike the side bars' central taper), and, like the side
 * bars, only ever draws in a gutter #mushaf's own sizing has left beside it — never inside
 * it — and hides rather than guess when that gutter is too narrow for even one bead.
 */
function updateCenterMark(){
  const mark=document.getElementById('centermark');
  const mushafEl=document.getElementById('mushaf');
  if(!mark||!mushafEl)return;
  const beadSize=eink?6:5,safety=3;
  const minGutter=beadSize+2*safety;
  const viewportWidth=document.documentElement.clientWidth||window.innerWidth||0;
  const rect=mushafEl.getBoundingClientRect();
  const rightGutter=viewportWidth-rect.right;
  const leftGutter=rect.left;
  const onOuterRight=currentPage%2===1;
  const gutter=onOuterRight?leftGutter:rightGutter;
  if(!(gutter>=minGutter)){mark.classList.remove('show');while(mark.children.length>1)mark.removeChild(mark.lastChild);return}
  const inset=(gutter-beadSize)/2;
  mark.style.left=onOuterRight?inset+'px':'auto';
  mark.style.right=onOuterRight?'auto':inset+'px';
  mark.style.top=rect.top+'px';
  mark.style.height=rect.height+'px';
  const spacing=28;
  const count=Math.max(1,Math.round(rect.height/spacing));
  while(mark.children.length>1+count)mark.removeChild(mark.lastChild);
  while(mark.children.length<1+count)mark.appendChild(document.createElement('div'));
  for(let i=0;i<count;i++){
    const bead=mark.children[i+1];
    bead.className='bead';
    bead.style.top=(((i+0.5)/count)*100)+'%';
  }
  mark.classList.add('show');
}
window.addEventListener('resize',()=>requestAnimationFrame(updateCenterMark));

/*
 * Universal page-number badge: a round token, bottom of the Mushaf, in the same outer-margin
 * gutter #sidemarks uses (odd page = right, even page = left) — never inside #mushaf itself,
 * hidden entirely when that gutter is too narrow for even the small reserved footprint, exactly
 * like the other two cues. Centralized here so every Activity that shows a MushafView gets the
 * identical badge for free, instead of each one drawing its own.
 */
function updatePageBadge(){
  const badge=document.getElementById('pagebadge');
  const mushafEl=document.getElementById('mushaf');
  if(!badge||!mushafEl)return;
  // Diameter and font both adapt to whatever gutter this screen actually has, shrinking to a
  // still-legible floor instead of a fixed size that can exceed the narrowest guaranteed gutter
  // (#mushaf's width breakpoint reserves 72px total, split evenly to 36px per side) and hide the
  // badge entirely there — real captures showed exactly that. The ideal (larger) size is kept
  // whenever the actual gutter has room for it.
  const idealD=eink?50:46,floorD=32,safety=1;
  const idealFont=eink?16:15,floorFont=eink?11:10;
  const minGutter=floorD+2*safety;
  const viewportWidth=document.documentElement.clientWidth||window.innerWidth||0;
  const rect=mushafEl.getBoundingClientRect();
  const rightGutter=viewportWidth-rect.right;
  const leftGutter=rect.left;
  const onOuterRight=currentPage%2===1;
  const gutter=onOuterRight?rightGutter:leftGutter;
  if(!(gutter>=minGutter)){badge.classList.remove('show');return}
  const d=Math.min(idealD,gutter-2*safety);
  const font=floorFont+(idealFont-floorFont)*(d-floorD)/(idealD-floorD);
  const inset=(gutter-d)/2;
  badge.style.left=onOuterRight?'auto':inset+'px';
  badge.style.right=onOuterRight?inset+'px':'auto';
  badge.style.top=(rect.bottom-d)+'px';
  badge.style.width=d+'px';
  badge.style.height=d+'px';
  badge.style.fontSize=font+'px';
  badge.textContent=String(currentPage);
  badge.classList.toggle('odd',onOuterRight);
  badge.classList.toggle('even',!onOuterRight);
  badge.classList.add('show');
}
window.addEventListener('resize',()=>requestAnimationFrame(updatePageBadge));

function prepare(){
  const svg=currentSvg();
  if(!svg){N?.error('SVG Mushaf absent');return}
  svg.querySelectorAll('.ayahPolygon').forEach(p=>{
    const k=verseOf(p);p.dataset.verse=k;
    p.removeAttribute('tabindex');
    p.onclick=e=>{
      e.stopPropagation();
      const [s,a]=k.split(':').map(Number);N?.verseTap(s,a);
    };
  });
  render();
  requestAnimationFrame(updateSideMarks);
  requestAnimationFrame(updateCenterMark);
  requestAnimationFrame(updatePageBadge);
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

function semanticVisibleCellKeys(){
  const keys=new Set();
  if(!semanticAnchorMaskMode)return keys;
  (semanticCues||[]).forEach(cue=>{
    (cue.ranges||[]).forEach(range=>{
      const lineId=String(range.lineId||'');
      const from=Math.max(0,Number(range.fromCell)||0),to=Math.max(from,Number(range.toCell)||0);
      for(let i=from;i<to;i++)keys.add(lineId+':'+i);
    });
  });
  return keys;
}

function maskCandidates(lines,polys){
  const out=[],semanticVisible=semanticVisibleCellKeys();
  lines.forEach(line=>{
    const top=Number(line.top),bottom=Number(line.bottom);
    const cells=line.cells||[];
    const lineId=String(line.id);
    const role=lineId===landmarkStart?'start':(lineId===landmarkEnd?'end':null);
    const range=role?landmarkCellIndices(cells.length,role):null;
    cells.forEach((cell,ci)=>{
      if(range&&(ci<range.from||ci>=range.to))return;
      const key=lineId+':'+ci;
      if(semanticVisible.has(key))return;
      const x0=Number(cell[0]),x1=Number(cell[1]);
      if(polys.length&&!insideSelection(polys,(x0+x1)/2,(top+bottom)/2))return;
      out.push({key,lineId,index:ci,x0,x1,top,bottom});
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

function semanticCueLayer(svg){
  const g=document.createElementNS(NS,'g');
  g.setAttribute('class','semanticcuelayer');
  if(!pageGeo||!Array.isArray(semanticCues)||!semanticCues.length)return g;
  const allLines=pageGeo.lines||[],vb=svg.viewBox&&svg.viewBox.baseVal;
  semanticCues.forEach(cue=>{
    const index=Math.max(1,Number(cue.index)||1);
    const lineIndex=Math.max(0,(Number(cue.startLine)||1)-1);
    const line=allLines[lineIndex];if(!line)return;

    // Exact phrase outline is drawn only from audited visual ranges; never infer word boxes.
    (cue.ranges||[]).forEach(range=>{
      const rline=allLines.find(x=>String(x.id)===String(range.lineId));if(!rline)return;
      const cells=rline.cells||[],from=Math.max(0,Number(range.fromCell)||0),to=Math.min(cells.length,Math.max(from,Number(range.toCell)||0));
      if(to<=from)return;
      let x0=Infinity,x1=-Infinity;
      for(let i=from;i<to;i++){x0=Math.min(x0,Number(cells[i][0]));x1=Math.max(x1,Number(cells[i][1]));}
      if(!Number.isFinite(x0)||!Number.isFinite(x1)||x1<=x0)return;
      const rect=document.createElementNS(NS,'rect');
      rect.setAttribute('x',x0-0.8);rect.setAttribute('y',Number(rline.top)+0.5);
      rect.setAttribute('width',(x1-x0)+1.6);rect.setAttribute('height',Math.max(1,Number(rline.bottom)-Number(rline.top)-1));
      rect.setAttribute('rx','1.6');rect.setAttribute('ry','1.6');
      rect.setAttribute('fill','none');rect.setAttribute('stroke','#524f49');
      rect.setAttribute('stroke-width',eink?'1.0':'0.75');rect.setAttribute('opacity',eink?'0.82':'0.62');
      g.appendChild(rect);
    });

    const cells=line.cells||[];if(!cells.length||!vb)return;
    let minX=Infinity,maxX=-Infinity;
    cells.forEach(cell=>{minX=Math.min(minX,Number(cell[0]));maxX=Math.max(maxX,Number(cell[1]));});
    if(!Number.isFinite(minX)||!Number.isFinite(maxX))return;
    const outerRight=currentPage%2===1;
    const edge=outerRight?vb.x+vb.width:vb.x;
    const gap=outerRight?edge-maxX:minX-edge;
    if(gap<6)return; // never place a control on Quran ink when the physical margin is too thin
    const x=outerRight?maxX+gap*.52:minX-gap*.52;
    const y=(Number(line.top)+Number(line.bottom))/2;
    const marker=document.createElementNS(NS,'g');
    marker.setAttribute('role','button');marker.setAttribute('tabindex','0');
    marker.setAttribute('aria-label','Repère '+index+(cue.title?' · '+cue.title:''));
    const circle=document.createElementNS(NS,'circle');
    circle.setAttribute('cx',x);circle.setAttribute('cy',y);circle.setAttribute('r',eink?'5.2':'4.6');
    circle.setAttribute('fill','#faf8f0');circle.setAttribute('stroke','#524f49');circle.setAttribute('stroke-width',eink?'1':'0.8');
    const label=document.createElementNS(NS,'text');
    label.setAttribute('x',x);label.setAttribute('y',y+(eink?1.8:1.6));
    label.setAttribute('text-anchor','middle');label.setAttribute('font-size',eink?'5.5':'5');
    label.setAttribute('font-family','sans-serif');label.setAttribute('fill','#121211');label.textContent=String(index);
    marker.appendChild(circle);marker.appendChild(label);
    const activate=e=>{e.preventDefault();e.stopPropagation();N?.semanticCueTap?.(String(cue.id||''));};
    marker.addEventListener('click',activate);
    marker.addEventListener('keydown',e=>{if(e.key==='Enter'||e.key===' '){activate(e)}});
    g.appendChild(marker);
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
  svg.querySelectorAll('.masklayer,.linefocuslayer,.weaklayer,.semanticcuelayer').forEach(n=>n.remove());

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

  // Semantic markers/phrase outlines are always a final, non-destructive overlay.
  if(semanticCues.length){
    const cues=semanticCueLayer(svg);
    if(cues.childNodes.length)svg.appendChild(cues);
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
    updateSideMarks();
    updateCenterMark();
    updatePageBadge();
  });
}
function clearReveal(){document.documentElement.style.setProperty('--reveal-shift','0px');updateSideMarks();updateCenterMark();updatePageBadge()}

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
  setSemanticCues(cues,anchorMaskMode){semanticCues=Array.isArray(cues)?cues:[];semanticAnchorMaskMode=!!anchorMaskMode;render()},
  setMaskFollowsSelection(value){maskFollowsSelection=!!value;render()},
  setEink(value){eink=!!value;render();updateSideMarks();updateCenterMark();updatePageBadge()},
  revealSelection(visibleFraction){revealSelection(visibleFraction)},
  clearReveal(){clearReveal()},
  page(){return currentPage}
};

if(typeof module!=='undefined'&&module.exports)module.exports={randomOrderKeys,randomSegmentsForCells,seededRandom,lineFocusLayer,landmarkCellIndices,maskCandidates,semanticVisibleCellKeys};
prepare();
N?.ready();