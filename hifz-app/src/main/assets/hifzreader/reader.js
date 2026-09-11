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
 lines.forEach(line=>{
   const cells=line.cells||[];
   cells.forEach((cell,i)=>{
     const rank=((i*37 + line.id.length*17)%100);
     if(mask===100||rank<mask){const r=document.createElementNS(NS,'rect');r.setAttribute('x',cell[0]);r.setAttribute('y',line.top);r.setAttribute('width',Math.max(0.5,cell[1]-cell[0]));r.setAttribute('height',line.bottom-line.top);r.setAttribute('fill','#fff');group.appendChild(r)}
   });
 });
 layer.appendChild(group);svg.appendChild(layer);
}

window.HifzReader={
 setMask(hidden){mask=Number(hidden||0);render()},
 setSelection(selection,lines){selected=selection||[];lineIds=lines||[];render()},
 page(){return currentPage}
};

N?.ready();
prepare();
