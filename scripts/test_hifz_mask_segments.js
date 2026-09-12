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

const {hiddenSegmentsForLine}=require('../hifz-app/src/main/assets/hifzreader/reader.js');
const line={top:100,bottom:120,cells:[
  {x0:0,x1:10},{x0:20,x1:40},{x0:50,x1:80}
]};

function width(segments){return segments.reduce((sum,s)=>sum+s.width,0);}
function rounded(value){return Math.round(value*1000)/1000;}

let segments=hiddenSegmentsForLine(line,25);
assert.strictEqual(segments.length,1);
assert.strictEqual(rounded(width(segments)),15);
assert.strictEqual(rounded(segments[0].x),65);

segments=hiddenSegmentsForLine(line,50);
assert.strictEqual(segments.length,1);
assert.strictEqual(rounded(width(segments)),30);
assert.strictEqual(rounded(segments[0].x),50);

segments=hiddenSegmentsForLine(line,75);
assert.strictEqual(segments.length,2);
assert.strictEqual(rounded(width(segments)),45);
assert.strictEqual(rounded(segments[1].x),25);
assert.ok(segments.every(s=>s.y===100.6 && rounded(s.height)===18.8));

segments=hiddenSegmentsForLine(line,100);
assert.strictEqual(segments.length,3);
assert.strictEqual(rounded(width(segments)),60);
assert.deepStrictEqual(segments.map(s=>rounded(s.x)),[50,20,0]);

// No segment may bridge a whitespace gap between source-ink groups.
for(const segment of segments){
  const contained=line.cells.some(cell=>segment.x>=cell.x0-1e-6 && segment.x+segment.width<=cell.x1+1e-6);
  assert.ok(contained,'mask segment crossed whitespace');
}

console.log('HIFZ_MASK_SEGMENTS_NUMERIC_OK');
