'use strict';
const assert = require('assert');
const fs = require('fs');
const vm = require('vm');
const source = fs.readFileSync(require('path').join(__dirname, '../hifz-app/src/main/assets/hifzreader/reader.js'), 'utf8');
class Element {
  constructor(tag) { this.tagName=tag; this.childNodes=[]; this.attrs={}; this.dataset={}; this.classList={toggle(){}}; }
  setAttribute(k,v) { this.attrs[k]=String(v); }
  appendChild(n) { n.parent=this; this.childNodes.push(n); return n; }
  insertBefore(n,b) { n.parent=this; this.childNodes.splice(Math.max(0,this.childNodes.indexOf(b)),0,n); }
  remove() { if(this.parent)this.parent.childNodes.splice(this.parent.childNodes.indexOf(this),1); }
  cloneNode() { return new Element(this.tagName); }
}
const identity={a:1,b:0,c:0,d:1,e:0,f:0,inverse(){return this},multiply(){return this}};
const rosette = new Element('g');
rosette.appendChild(new Element('rosette-ink'));
rosette.getScreenCTM=()=>identity;
rosette.getBBox=()=>({x:10,y:100,width:8,height:10});
const svg=new Element('svg');
svg.getScreenCTM=()=>identity;
svg.viewBox={baseVal:{x:0,y:0,width:345,height:550}};
svg.createSVGPoint=()=>({x:0,y:0,matrixTransform(){return {x:this.x,y:this.y}}});
svg.querySelectorAll=selector=>selector==='#ayah_markers > g'?[rosette]:selector.includes('.masklayer')?svg.childNodes.filter(n=>selector.split(',').some(c=>n.attrs.class===c.slice(1))):[];
const document={getElementById(id){return id==='mushaf'?{querySelector(){return svg}}:null},createElementNS(ns,tag){return new Element(tag)},addEventListener(){},body:{classList:{toggle(){}}},documentElement:{style:{setProperty(){}}}};
const window={HIFZ_BOOT:{page:1,geometry:{lines:[{id:'L1',top:100,bottom:120,cells:[[0,30]],verses:[]}]},lines:['L1'],mask:100,maskFollowsSelection:false}};
vm.runInNewContext(source,{window,document,requestAnimationFrame:fn=>fn(),module:{exports:{}}});
function verifyMaskedRosette(){
  const layer=svg.childNodes.find(n=>n.attrs.class==='masklayer');
  assert.ok(layer,'real renderer must create a mask');
  assert.ok(layer.childNodes[0].childNodes.some(n=>n.attrs.class==='maskcell'),'mask must contain source-ink coverage');
  const markers=layer.childNodes.at(-1);
  assert.strictEqual(markers.childNodes.length,1,'rosette intersecting masked line must be redrawn');
  assert.strictEqual(markers.childNodes[0]?.childNodes[0]?.tagName,'rosette-ink','original rosette ink must sit above the mask');
}
verifyMaskedRosette();
window.HifzReader.setMask(0);
assert.ok(!svg.childNodes.some(n=>n.attrs.class==='masklayer'),'reveal must remove masking');
window.HifzReader.setMask(100);
verifyMaskedRosette();
// Prove this test detects a regression in production rendering, independently of comments.
assert.throws(()=>vm.runInNewContext(source.replace('layer.appendChild(markerLayer(svg,polys,lines));',''),{window:{HIFZ_BOOT:window.HIFZ_BOOT},document,requestAnimationFrame:fn=>fn(),module:{exports:{}}}) || verifyMaskedRosette(),/original rosette ink/);
console.log('HIFZ_ROSETTE_RENDER_BEHAVIOR_OK');
// The first two shipped SVGs retain their negative viewBox origin. Word-box data
// is normalized to the page; exact holes and highlights must map into SVG coordinates.
svg.viewBox.baseVal={x:-53.3109,y:-198.4777,width:345,height:550};
window.HifzReader.setPageLandmarkBoxes([[100,220,120,240]]);
window.HifzReader.setSemanticCues([{boxes:[[200,220,230,240]]}],true);
const masked=svg.childNodes.find(n=>n.attrs.class==='masklayer');
const holeMask=masked.childNodes.find(n=>n.tagName==='defs').childNodes[0];
const hole=holeMask.childNodes[1];
assert.ok(Math.abs(Number(hole.attrs.x)-(100-53.3109))<1e-8,'page landmarks must account for the actual SVG viewBox origin');
assert.ok(Math.abs(Number(hole.attrs.y)-(220-198.4777))<1e-8,'page landmarks must stay on the real Quran ink');
const cues=svg.childNodes.find(n=>n.attrs.class==='semanticcuelayer');
const cueRect=cues.childNodes.find(n=>n.tagName==='rect');
assert.ok(cueRect,'semantic hatch must retain an exact Quran word-box rectangle');
assert.ok(Math.abs(Number(cueRect.attrs.x)-(200-53.3109))<1e-8,'semantic highlight must share the Quran coordinate system');
assert.ok(Math.abs(Number(cueRect.attrs.y)-(220-198.4777))<1e-8,'semantic highlight must share the Quran coordinate system');
console.log('HIFZ_NEGATIVE_VIEWBOX_WORD_MAPPING_OK');
