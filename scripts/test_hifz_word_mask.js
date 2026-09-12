'use strict';
const assert=require('assert');

global.window={HifzNative:null,HIFZ_BOOT:{}};
global.document={
  getElementById(){return {querySelector(){return null;}};},
  addEventListener(){},
  body:{classList:{toggle(){}}},
  documentElement:{style:{setProperty(){}}},
  createElementNS(){return {setAttribute(){},appendChild(){},removeAttribute(){},cloneNode(){return this;}};}
};
global.requestAnimationFrame=fn=>fn();

const reader=require('../hifz-app/src/main/assets/hifzreader/reader.js');
assert.strictEqual(typeof reader.maskedWordIds,'function','reader must expose maskedWordIds for deterministic tests');
assert.strictEqual(typeof reader.maskBoxesForPage,'function','reader must expose page word boxes for deterministic tests');

const words=[
  {id:'2:75:1',verse:'2:75',x:250,y:100,w:30,h:20},
  {id:'2:75:2',verse:'2:75',x:210,y:100,w:30,h:20},
  {id:'2:75:3',verse:'2:75',x:170,y:100,w:30,h:20},
  {id:'2:75:4',verse:'2:75',x:130,y:100,w:30,h:20},
  {id:'2:76:1',verse:'2:76',x:90,y:100,w:30,h:20},
  {id:'2:76:2',verse:'2:76',x:50,y:100,w:30,h:20},
  {id:'2:76:3',verse:'2:76',x:250,y:130,w:30,h:20},
  {id:'2:76:4',verse:'2:76',x:210,y:130,w:30,h:20},
];
const seed='SABQI|2:75,2:76|48:10,48:11';
function set(percent){return new Set(reader.maskedWordIds(words,percent,seed));}
const p0=set(0),p25=set(25),p50=set(50),p75=set(75),p100=set(100);
assert.strictEqual(p0.size,0);
assert.strictEqual(p25.size,2);
assert.strictEqual(p50.size,4);
assert.strictEqual(p75.size,6);
assert.strictEqual(p100.size,8);
for(const id of p25) assert.ok(p50.has(id),'25% must be a subset of 50%');
for(const id of p50) assert.ok(p75.has(id),'50% must be a subset of 75%');
for(const id of p75) assert.ok(p100.has(id),'75% must be a subset of 100%');
assert.deepStrictEqual([...p50],[...set(50)],'same passage/cycle must keep the same random draw');
assert.notDeepStrictEqual([...p50],[...new Set(reader.maskedWordIds(words,50,seed+'|next'))], 'new cycle seed must reshuffle words');

const pageGeo={lines:[{id:'48:10',words:[
  {id:'2:75:1',verse:'2:75',x:250,y:100,w:30,h:20},
  {id:'marker:75',verse:'',kind:'marker',x:200,y:100,w:20,h:20},
  {id:'2:75:2',verse:'2:75',x:160,y:100,w:30,h:20}
]}]};
const boxes=reader.maskBoxesForPage(pageGeo,new Set(['2:75:1','marker:75','2:75:2']));
assert.deepStrictEqual(boxes.map(x=>x.id).sort(),['2:75:1','2:75:2'], 'verse markers/decorations must never be masked');
assert.ok(boxes.every(b=>b.w>0&&b.h>0&&b.w<100),'mask must stay word-sized, not line-sized');

console.log('HIFZ_WORD_MASK_RANDOM_CUMULATIVE_OK');
