'use strict';
const N=window.HifzNative;
let pageGeo=null,currentPage=1,selected=[],lineIds=[],mask=0,eink=false,loadSeq=0;
const mushaf=document.getElementById('mushaf');

function key(s,a){return s+':'+a}
function parse(k){return k.split(':').map(Number)}
function currentSvg(){return mushaf.querySelector('svg')}

function loadPage(page){
 if(page<1||page>604)return;
 const seq=++loadSeq,current=page;currentPage=page;
 let text='',geometryText='';
 try{
   text=N?.pageSvg(page)||'';
   geometryText=N?.pageGeometry(page)||'';
 }catch(error){N?.error('Page '+page+' indisponible');return}
 if(seq!==loadSeq)return;
 if(!text){N?.error('Page '+page+' indisponible');return}
 try{
   pageGeo=geometryText?JSON.parse(geometryText):null;
 }catch(error){
   pageGeo=null;N?.error('Géométrie page '+page+' invalide');return;
 }
 if(!pageGeo){N?.error('Géométrie page '+page+' indisponible');return}
 const doc=new DOMParser().parseFromString(text,'image/svg+xml');
 if(doc.querySelector('parsererror')){N?.error('SVG page '+page+' invalide');return}
 const svg=document.importNode(doc.documentElement,true);mushaf.replaceChildren(svg);
 const seen=new Set();
 svg.querySelectorAll('.ayahPolygon').forEach(p=>{
   const k=key(p.getAttribute('surah'),p.getAttribute('ayah'));p.dataset.verse=k;
   if(!seen.has(k)){seen.add(k);p.setAttribute('role','button');p.setAttribute('tabindex','0');
     p.onclick=()=>{const [s,a]=parse(k);N?.verseTap(s,a)};
     p.onkeydown=e=>{if(e.key==='Enter'||e.key===' '){e.preventDefault();const [s,a]=parse(k);N?.verseTap(s,a)}};
   }
 });
 render();N?.pageShown(current);
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
 show(page,selection,lines,hidden,einkMode){selected=selection||[];lineIds=lines||[];mask=hidden||0;eink=!!einkMode;if(page!==currentPage||!currentSvg())loadPage(page);else render()},
 setMask(hidden){mask=hidden||0;render()},
 setSelection(selection,lines){selected=selection||[];lineIds=lines||[];render()},
 page(){return currentPage}
};

N?.ready();
