'use strict';
const assert=require('assert');
const fs=require('fs');
const path=require('path');

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
const seed='SABQI|2:75,2:76|48:10,48:11|rep:16';
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
assert.deepStrictEqual([...p50],[...set(50)],'same repetition seed must keep the same random draw');
assert.notDeepStrictEqual([...p50],[...new Set(reader.maskedWordIds(words,50,seed.replace('rep:16','rep:17')))], 'next repetition seed must reshuffle words');

const pageGeo={lines:[{id:'48:10',words:[
  {id:'2:75:1',verse:'2:75',kind:'word',x:250,y:100,w:30,h:20},
  {id:'marker:75',verse:'',kind:'marker',x:200,y:100,w:20,h:20},
  {id:'2:75:2',verse:'2:75',kind:'word',x:160,y:100,w:30,h:20}
]}]};
const boxes=reader.maskBoxesForPage(pageGeo,new Set(['2:75:1','marker:75','2:75:2']));
assert.deepStrictEqual(boxes.map(x=>x.id).sort(),['2:75:1','2:75:2'], 'verse markers/decorations must never be masked');
assert.ok(boxes.every(b=>b.w>0&&b.h>0&&b.w<100),'mask must stay word-sized, not line-sized');

// Native contract: the repetition number must reach the JS seed, including repeated reps at the same percentage.
const session=fs.readFileSync(path.join(__dirname,'../hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java'),'utf8');
const mushaf=fs.readFileSync(path.join(__dirname,'../hifz-app/src/main/java/com/quransafeguard/hifz/preview/MushafView.java'),'utf8');
assert.ok(session.includes('maskDrawKey()'),'structured sessions must derive a repetition-specific mask draw key');
assert.ok(session.includes('mushaf.setMask(currentMask, maskDrawKey())'),'every completed repetition must push the new random draw key');
assert.ok(session.includes('mushaf.show(currentPage,currentSelection,currentLineIds,currentMask,maskDrawKey())'),'first render must use the same repetition-specific draw key');
assert.ok(mushaf.includes('setMask(int maskPercent, String drawKey)'),'native reader bridge must accept an explicit random draw key');
assert.ok(mushaf.includes('show(int page, List<VerseRef> selection, List<String> lineIds, int maskPercent, String drawKey)'),'initial WebView boot must accept the draw key');
assert.ok(mushaf.includes('.put("maskSeed", drawKey'),'draw key must be injected into the offline WebView boot payload');
assert.ok(reader.toString || true);
const readerSource=fs.readFileSync(path.join(__dirname,'../hifz-app/src/main/assets/hifzreader/reader.js'),'utf8');
assert.ok(readerSource.includes('let drawKey=String(boot.maskSeed||\'\')'),'JS reader must retain the native repetition draw key');
assert.ok(readerSource.includes('setMask(hidden,key)'),'runtime mask update must accept a new repetition draw key');

console.log('HIFZ_WORD_MASK_RANDOM_CUMULATIVE_OK');
