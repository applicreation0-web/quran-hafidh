const assert=require('node:assert/strict');
const {mountTitleTargets}=require('../hifz-app/src/main/assets/ayah_themes_display/review_titles.js');
class Element {
 constructor(name){this.name=name;this.attrs={};this.children=[];this.viewBox={baseVal:{x:0,y:0}};}
 setAttribute(k,v){this.attrs[k]=String(v);}
 appendChild(n){this.children=this.children.filter(x=>x!==n);this.children.push(n);return n;}
}
const document={createElementNS:(_,name)=>new Element(name)};
const svg=new Element('svg');svg.ownerDocument=document;
const mask=new Element('masklayer');mask.attrs.path='EXACT_OLD_MASK_WITH_HISTORICAL_HOLES';
svg.appendChild(mask);const before=JSON.stringify(mask);const taps=[];
const cues=[{id:'s002a006b007',surah:2,start:6,boxes:[[12,22,32,42]]}];
const verseTaps=[];
const layer=mountTitleTargets(svg,cues,id=>taps.push(id),(s,a)=>verseTaps.push([s,a]));
assert.equal(svg.children[0],mask);assert.equal(JSON.stringify(mask),before);
assert.equal(layer.children.length,1);const target=layer.children[0];
assert.equal(target.attrs.x,'12');assert.equal(target.attrs.y,'22');
assert.equal(target.attrs.width,'20');assert.equal(target.attrs.height,'20');
target.onclick({stopPropagation(){}});assert.deepEqual(taps,['AT:s002a006b007']);assert.deepEqual(verseTaps,[[2,6]]);
// Invalid/fabricated rectangles are ignored without changing the existing mask.
const rejected=mountTitleTargets(svg,[{id:'bad',boxes:[[32,22,12,42]]}],()=>assert.fail());
assert.equal(rejected.children.length,0);assert.equal(JSON.stringify(mask),before);
svg.viewBox.baseVal={x:3,y:5};
const shifted=mountTitleTargets(svg,cues,()=>{});
assert.equal(shifted.children[0].attrs.x,'15');assert.equal(shifted.children[0].attrs.y,'27');
assert.equal(JSON.stringify(mask),before);
console.log('PASS: exact independent title targets, native IDs, invalid-box rejection and SVG offsets; existing mask nodes untouched');
