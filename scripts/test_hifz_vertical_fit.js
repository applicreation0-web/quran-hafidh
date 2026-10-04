'use strict';
const assert=require('assert');
const fs=require('fs');
const path=require('path');

const index=fs.readFileSync(path.join(__dirname,'../hifz-app/src/main/assets/hifzreader/index.html'),'utf8');

assert.ok(!index.includes('--mushaf-scale'),'reader must not enlarge the Mushaf beyond the WebView height');
assert.ok(!/scale\s*\(/.test(index),'reader transform must never scale the Mushaf beyond its fitted geometry');
assert.ok(index.includes('calc((100vh - 4px) * 345 / 550)'),'reader must reserve a 2px safety margin above and below the full Mushaf page');
assert.ok(index.includes('align-items:center')&&index.includes('justify-content:center'),'full fitted page must remain centered in the available reader area');
assert.ok(index.includes('#mushaf>svg{display:block;width:100%;height:auto}'),'canonical SVG must preserve its aspect ratio without reflow');

console.log('HIFZ_VERTICAL_FIT_NO_CROP_OK');
