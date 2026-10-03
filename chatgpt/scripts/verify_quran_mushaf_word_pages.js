'use strict';
const fs=require('fs'),path=require('path'),zlib=require('zlib'),assert=require('assert');
const root=path.resolve(__dirname,'..');
const geometry=JSON.parse(fs.readFileSync(path.join(root,'app/src/main/assets/reader109/geometry.json')));
const sidecars=path.join(root,'hifz-app/src/main/word-source/quran-ws-v1.1.2');
let count=0,words=0,shiftedPages=[];
for(const file of fs.readdirSync(sidecars).filter(f=>f.endsWith('.json')).sort()){
  const data=JSON.parse(fs.readFileSync(path.join(sidecars,file)));
  for(const [page,rows] of Object.entries(data.pages)){
    const svg=zlib.brotliDecompressSync(fs.readFileSync(path.join(root,'app/src/main/assets/mushaf/hafs/kfqc/svg-br',page.padStart(3,'0')+'.svg.br'))).toString();
    const viewBox=svg.match(/viewBox="([^"]+)"/)[1].split(/\s+/).map(Number);
    assert.deepStrictEqual(viewBox,geometry.pages[page].viewBox,'SVG and reader geometry must share the exact viewBox on page '+page);
    assert.deepStrictEqual(viewBox.slice(2),[345,550]);
    if(viewBox[0]||viewBox[1])shiftedPages.push(Number(page));
    const verses=new Set([...svg.matchAll(/<path\b[^>]*class="ayahPolygon"[^>]*>/g)].map(m=>{
      const s=m[0].match(/\bsurah="(\d+)"/),a=m[0].match(/\bayah="(\d+)"/);return s[1]+':'+a[1];
    }));
    for(const row of rows){
      const [s,a]=row[0].split(':');
      assert.ok(geometry.verses[s+':'+a].includes(Number(page)),row[0]+' must belong to page '+page);
      assert.ok(verses.has(s+':'+a),row[0]+' must exist in the shipped SVG');words++;
    }
    assert.strictEqual(rows.slice(0,3).length,3);assert.strictEqual(rows.slice(-3).length,3);count++;
  }
}
assert.strictEqual(count,604);assert.strictEqual(words,77432);
assert.deepStrictEqual(shiftedPages,[1,2]);
console.log('SHIPPED_MUSHAF_WORD_PAGES_OK pages='+count+' words='+words+' shifted_origins='+shiftedPages.join(','));
