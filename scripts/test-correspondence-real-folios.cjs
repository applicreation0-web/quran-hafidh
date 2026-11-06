// Execute the production gutter script against EVERY concerned folio's real SVG
// polygons, with both catalogues. Geometry harness, not an Android/E-Ink claim.
const assert=require('node:assert/strict'),fs=require('node:fs'),z=require('node:zlib');
const base='hifz-app/src/main/java/com/quransafeguard/hifz/preview/';
const data=JSON.parse(fs.readFileSync('docs/correspondances/catalogue-62.json'));
const geometry=JSON.parse(fs.readFileSync('app/src/main/assets/reader109/geometry.json'));
const corr=new Map(),sim=new Map();
function add(map,page,ref){if(!map.has(page))map.set(page,new Set());map.get(page).add(ref);}
for(const d of data.dossiers.filter(d=>d.statut==='ADMIS'))for(const a of d.ancresQcf)add(corr,a.folio,a.verset);
const additions=JSON.parse(fs.readFileSync('docs/correspondances/additions-11712.json'));
for(const d of additions.dossiers.filter(d=>d.statut==='ADMIS'))for(const ref of d.reperes){
  const folios=geometry.verses[ref]||[];
  assert.ok(folios.includes(d.folioQcfAttendu),'Unverified real QCF folio for '+d.id+' '+ref);
  for(const page of folios)add(corr,page,ref);
}
for(const match of fs.readFileSync(base+'AtlasDiscriminantCases.java','utf8').matchAll(/new Entry\((\d+),(\d+),(\d+),(\d+),/g)){
  for(const ref of [match[1]+':'+match[2],match[3]+':'+match[4]])
    for(const p of geometry.verses[ref])add(sim,p,ref);
}
const java=fs.readFileSync(base+'CorrespondenceGutterMarkers.java','utf8');
const input=java.split('String js=')[1].split('view.evaluateJavascript')[0];
const script=input.split('\n').filter(l=>!l.trimStart().startsWith('//')).join('\n')
 .replace('+links.toString()+','+ JSON.stringify(links) +')
 .replace('+parallels.toString()+','+ JSON.stringify(parallels) +');
class Element{
 constructor(tag){this.tag=tag;this.style={};this.children=[];this.attrs={};this.events={};}
 appendChild(x){this.children.push(x);return x;} setAttribute(k,v){this.attrs[k]=v;}
 getAttribute(k){return this.attrs[k];} addEventListener(k,v){this.events[k]=v;}
 remove(){this.removed=true;}showModal(){this.open=true;}close(){this.open=false;}
}
let scenarios=0,checked=0;
const pageSet=new Set([...corr.keys(),...sim.keys()]);
for(const page of [...pageSet].sort((a,b)=>a-b)){
 const svg=z.brotliDecompressSync(fs.readFileSync('app/src/main/assets/mushaf/hafs/kfqc/svg-br/'+String(page).padStart(3,'0')+'.svg.br')).toString();
 const polys=[];
 for(const tag of svg.match(/<path\b[^>]*class="ayahPolygon"[^>]*>/g)||[]){
   const attr=k=>(tag.match(new RegExp('(?:^|\\s)'+k+'="([^"]*)"'))||[])[1];
   const nums=attr('d').match(/-?\d+(?:\.\d+)?/g).map(Number);
   polys.push({ref:attr('surah')+':'+attr('ayah'),top:Math.min(...nums.filter((_,i)=>i%2))});
 }
 for(const [width,height] of [[411,620],[994,1100],[360,224]]){
  const links=[...(sim.get(page)||[])],parallels=[...(corr.get(page)||[])];
  for(const ref of [...links,...parallels])assert.ok(polys.some(p=>p.ref===ref),'Missing actual polygon '+page+' '+ref);
  const qwidth=Math.min(width-96,(height-4)*345/550),left=(width-qwidth)/2,scale=qwidth/345;
  const host=new Element('div');host.clientHeight=height;host.getBoundingClientRect=()=>({left:0,right:width,top:0});
  const m=new Element('div');m.getBoundingClientRect=()=>({left,right:left+qwidth});
  m.querySelectorAll=()=>polys.map(p=>({getAttribute:k=>k==='data-verse'?p.ref:null,getBoundingClientRect:()=>({top:p.top*scale})}));
  const document={getElementById:k=>k==='box'?host:k==='mushaf'?m:null,createElement:t=>new Element(t)};
  const window={dispatchEvent(){},addEventListener(){},removeEventListener(){}};
  const selected=[];let dialogOpen=false;
  const HifzNative={setMarkerDialogOpen:v=>dialogOpen=v,comparisonTap:(s,a)=>selected.push('similarity '+s+':'+a),correspondenceTap:(s,a)=>selected.push('correspondence '+s+':'+a)};
  const pending=[],requestAnimationFrame=f=>pending.push(f);
  eval(eval(script));while(pending.length)pending.shift()();
  const root=host.children[0],buttons=root.children.filter(b=>b.tag==='button'&&b.style.visibility==='visible');
  const rects=buttons.map(b=>({x:parseFloat(b.style.left),y:parseFloat(b.style.top)}));
  for(let i=0;i<rects.length;i++){
    const r=rects[i];assert.ok(r.x>=0&&r.y>=0&&r.x+44<=width&&r.y+44<=height,'Offscreen '+page);
    assert.ok(r.x+44<=left+.01||r.x>=left+qwidth-.01,'QCF overlap '+page);
    for(let j=0;j<i;j++)assert.ok(Math.abs(r.x-rects[j].x)>=44||Math.abs(r.y-rects[j].y)>=44,'Button collision '+page);
  }
  const event={stopPropagation(){}};
  for(const b of buttons){
    b.events.click(event);
    if(b.attrs['aria-haspopup']){
      const d=root.children.find(d=>d.tag==='dialog'&&d.open&&!d.removed);
      assert.ok(d&&dialogOpen,'Overflow must open');
      for(const item of d.children.filter(x=>x.attrs['data-verse']))item.events.click(event);
      assert.equal(dialogOpen,false);
    }
  }
  const expected=[...links.map(r=>'similarity '+r),...parallels.map(r=>'correspondence '+r)].sort();
  assert.deepEqual(selected.sort(),expected,'Lost/wrong/duplicate marker '+page);
  window.__haafidhAtlasMarkerDispose();assert.equal(dialogOpen,false);
  scenarios++;checked+=selected.length;
 }
}
console.log(JSON.stringify({folios:pageSet.size,viewportScenarios:scenarios,markerSelections:checked,collisions:0,lostMarkers:0,scope:'production JS / real SVG polygon geometry; not Android runtime'}));
