'use strict';
const assert=require('assert');

global.window={HifzNative:null,HIFZ_BOOT:{}};
global.document={
  getElementById(){return {querySelector(){return null;}};},
  addEventListener(){},
  body:{classList:{toggle(){}}},
  documentElement:{style:{setProperty(){}}}
};
global.requestAnimationFrame=fn=>fn();

const {randomOrderKeys,randomSegmentsForCells,seededRandom}=require('../hifz-app/src/main/assets/hifzreader/reader.js');
const cells=[
  {key:'L1:0',x0:0,x1:10,top:100,bottom:120},
  {key:'L1:1',x0:20,x1:40,top:100,bottom:120},
  {key:'L1:2',x0:50,x1:80,top:100,bottom:120},
  {key:'L2:0',x0:5,x1:25,top:130,bottom:150}
];

function rng(values){let i=0;return()=>values[i++%values.length];}
function width(xs){return xs.reduce((sum,s)=>sum+s.width,0);}
function keys(xs){return new Set(xs.map(s=>s.key));}
function subset(a,b){for(const k of a)assert.ok(b.has(k),`${k} must remain masked`);}

const orderA=randomOrderKeys(cells,rng([0.01,0.15,0.33,0.61]));
const orderB=randomOrderKeys(cells,rng([0.91,0.72,0.54,0.38]));
assert.notDeepStrictEqual(orderA,orderB,'different entropy must be able to produce a different draw');
assert.deepStrictEqual([...orderA].sort(),cells.map(c=>c.key).sort());

assert.strictEqual(typeof seededRandom,'function','reader must expose a seeded PRNG so a random draw survives a WebView page reload');
const pageA1=randomOrderKeys(cells,seededRandom('session-entropy|page-A'));
const pageA2=randomOrderKeys(cells,seededRandom('session-entropy|page-A'));
const pageB=randomOrderKeys(cells,seededRandom('different-entropy|page-A'));
assert.deepStrictEqual(pageA1,pageA2,'same session entropy and page must restore exactly the same random mask order');
assert.notDeepStrictEqual(pageA1,pageB,'a fresh session entropy must be able to produce a fresh random draw');

const s25=randomSegmentsForCells(cells,25,orderA);
const s50=randomSegmentsForCells(cells,50,orderA);
const s75=randomSegmentsForCells(cells,75,orderA);
const s100=randomSegmentsForCells(cells,100,orderA);
subset(keys(s25),keys(s50));subset(keys(s50),keys(s75));subset(keys(s75),keys(s100));
assert.strictEqual(Math.round(width(s25)*1000),20000);
assert.strictEqual(Math.round(width(s50)*1000),40000);
assert.strictEqual(Math.round(width(s75)*1000),60000);
assert.strictEqual(Math.round(width(s100)*1000),80000);
assert.strictEqual(keys(s100).size,4);

for(const segment of s100){
  const cell=cells.find(c=>c.key===segment.key);
  assert.ok(cell);
  assert.ok(segment.x>=cell.x0-1e-6 && segment.x+segment.width<=cell.x1+1e-6);
  assert.strictEqual(segment.y,cell.top+0.6);
  assert.strictEqual(Math.round(segment.height*10)/10,18.8);
}

const source=require('fs').readFileSync(require('path').join(__dirname,'../hifz-app/src/main/assets/hifzreader/reader.js'),'utf8');
assert.ok(source.includes('markerLayer(svg,polys,lines)'),'verse-number rosette layer must receive masked-line geometry');
assert.ok(source.includes('layer.appendChild(markerLayer(svg,polys,lines))'),'verse-number rosettes must always be redrawn above masks');
assert.ok(source.includes('Verse-number rosettes are deliberately redrawn above the random masks'));
assert.ok(source.includes('maskEntropy'),'native session entropy must participate in the random draw');
assert.ok(!source.includes('line.words'),'random masking must not depend on linguistic word geometry');
assert.ok(!source.includes('augment_hifz_word_geometry'),'word-mapping experiment must not leak into runtime');

console.log('HIFZ_RANDOM_SOURCE_INK_MASK_OK');
