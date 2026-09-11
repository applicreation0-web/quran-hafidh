'use strict';
const N=window.HifzNative;
const boot=window.HIFZ_BOOT||{};
let pageGeo=boot.geometry||null;
let currentPage=Number(boot.page||1);
let selected=boot.selection||[];
let lineIds=boot.lines||[];
let mask=Number(boot.mask||0);
let eink=!!boot.eink;
const mushaf=document.getElementById('mushaf');

function key(s,a){return s+':'+a}
function parse(k){return k.split(':').map(Number)}
function currentSvg(){return mushaf.querySelector('svg')}

function prepare(){
 const svg=currentSvg();
 if(!svg){N?.error('SVG Mushaf absent');return}
 const seen=new Set();
 svg.querySelectorAll('.ayahPolygon').forEach(p=>{
   const k=key(p.getAttribute('surah'),p.getAttribute('ayah'));p.dataset.verse=k;
   if(!seen.has(k)){
     seen.add(k);p.setAttribute('role','button');p.setAttribute('tabindex','0');
     p.onclick=e=>{e.stopPropagation();const [s,a]=parse(k);N?.verseTap(s,a)};
     p.onkeydown=e=>{if(e.key==='Enter'||e.key===' '){e.preventDefault();const [s,a]=parse(k);N?.verseTap(s,a)}};
   }
 });
 render();
 N?.pageShown(currentPage);
}

mushaf.onclick=()=>N?.surfaceTap();

function stableHash(text){
 let h=2166136261>>>0;
 for(let i=0;i<text.length;i++){
   h^=text.charCodeAt(i);
   h=Math.imul(h,16777619)>>>0;
 }
 return h;
}

function intersects(a,b){
 return a.x < b.x+b.width && a.x+a.width > b.x &&
        a.y < b.y+b.height && a.y+a.height > b.y;
}

function selectedBoxes(svg){
 const out=[];
 svg.querySelectorAll('.ayahPolygon').forEach(p=>{
   if(!selected.includes(p.dataset.verse))return;
   try{
     const b=p.getBBox();
     if(b && b.width>0 && b.height>0)out.push({x:b.x,y:b.y,width:b.width,height:b.height});
   }catch(_ignored){}
 });
 return out;
}

function maskCandidates(svg,lines){
 const boxes=selectedBoxes(svg);
 const all=[];
 lines.forEach((line,lineIndex)=>{
   (line.cells||[]).forEach((cell,cellIndex)=>{
     const rect={
       x:Number(cell[0]),
       y:Number(line.top),
       width:Math.max(0.5,Number(cell[1])-Number(cell[0])),
       height:Math.max(0.5,Number(line.bottom)-Number(line.top))
     };
     if(boxes.length && !boxes.some(box=>intersects(rect,box)))return;
     all.push({
       ...rect,
       order:stableHash(String(line.id)+'#'+lineIndex+'#'+cellIndex+'#'+cell[0]+'#'+cell[1])
     });
   });
 });
 // If browser geometry cannot expose polygon boxes, retain the historical fallback
 // but still use deterministic nested thresholds below.
 if(!all.length && lines.length){
   lines.forEach((line,lineIndex)=>{
     (line.cells||[]).forEach((cell,cellIndex)=>{
       all.push({
         x:Number(cell[0]),
         y:Number(line.top),
         width:Math.max(0.5,Number(cell[1])-Number(cell[0])),
         height:Math.max(0.5,Number(line.bottom)-Number(line.top)),
         order:stableHash(String(line.id)+'#'+lineIndex+'#'+cellIndex+'#'+cell[0]+'#'+cell[1])
       });
     });
   });
 }
 all.sort((a,b)=>a.order-b.order || a.y-b.y || a.x-b.x);
 return all;
}

function render(){
 document.body.classList.toggle('eink',eink);
 const svg=currentSvg();if(!svg)return;
 svg.querySelectorAll('.ayahPolygon').forEach(p=>p.classList.toggle('selected',selected.includes(p.dataset.verse)));
 svg.querySelectorAll('.masklayer').forEach(n=>n.remove());
 if(!mask||!pageGeo||!lineIds.length)return;
 const lines=(pageGeo.lines||[]).filter(l=>lineIds.includes(l.id));if(!lines.length)return;
 const NS='http://www.w3.org/2000/svg';
 const layer=document.createElementNS(NS,'g');layer.setAttribute('class','masklayer');
 const defs=document.createElementNS(NS,'defs'),clip=document.createElementNS(NS,'clipPath');clip.id='hifz-selection-clip';
 svg.querySelectorAll('.ayahPolygon').forEach(p=>{if(selected.includes(p.dataset.verse)){const q=p.cloneNode();q.removeAttribute('class');q.removeAttribute('tabindex');q.removeAttribute('role');q.setAttribute('fill-opacity','1');clip.appendChild(q)}});
 defs.appendChild(clip);layer.appendChild(defs);
 const group=document.createElementNS(NS,'g');group.setAttribute('clip-path','url(#hifz-selection-clip)');
 const candidates=maskCandidates(svg,lines);
 if(candidates.length){
   const clamped=Math.max(0,Math.min(100,Number(mask)||0));
   const hiddenCount=clamped>=100 ? candidates.length : Math.max(1,Math.ceil(candidates.length*clamped/100));
   candidates.slice(0,hiddenCount).forEach(cell=>{
     const r=document.createElementNS(NS,'rect');
     r.setAttribute('x',cell.x);r.setAttribute('y',cell.y);
     r.setAttribute('width',cell.width);r.setAttribute('height',cell.height);
     r.setAttribute('fill','#fff');group.appendChild(r);
   });
 }
 layer.appendChild(group);svg.appendChild(layer);
}

window.HifzReader={
 setGeometry(geometry){pageGeo=geometry||null;render()},
 setMask(hidden){mask=Number(hidden||0);render()},
 setSelection(selection,lines){selected=selection||[];lineIds=lines||[];render()},
 page(){return currentPage}
};

N?.ready();
prepare();
