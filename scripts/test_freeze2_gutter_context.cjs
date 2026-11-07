// Actual generated WebView JavaScript: verify one-family/two-family QCF gutter
// reservations without changing the original 345:550 Medina pagination.
const assert=require('node:assert/strict');
const fs=require('node:fs');
const source=fs.readFileSync('hifz-app/src/main/java/com/quransafeguard/hifz/preview/CorrespondenceGutterMarkers.java','utf8');
const raw=source.split('String js=')[1].split('view.evaluateJavascript')[0];
for(const [links,parallels,marginLeft,marginRight,reserve] of [
  [['55:13'],[], '46px','0px',52],
  [[],['55:13'], '0px','46px',52],
  [['55:13'],['55:13'], '0px','0px',96],
  [[],[], '0px','0px',96],
]){
  const script=raw.split('\n').filter(line=>!line.trimStart().startsWith('//')).join('\n')
    .replace('+links.toString()+',JSON.stringify(links))
    .replace('+parallels.toString()+',JSON.stringify(parallels));
  class Element {
    constructor(){this.style={};this.children=[];this.attrs={};this.events={};this.clientHeight=600;}
    appendChild(e){this.children.push(e);return e;}
    remove(){} setAttribute(k,v){this.attrs[k]=v;}
    getAttribute(k){return this.attrs[k];}
    addEventListener(k,v){this.events[k]=v;}
    getBoundingClientRect(){return {top:0,left:0,right:350};}
    querySelectorAll(){return [];}
  }
  const host=new Element(),m=new Element();
  const document={getElementById:id=>id==='box'?host:id==='mushaf'?m:null,
    createElement:()=>new Element()};
  const window={dispatchEvent(){},addEventListener(){},removeEventListener(){}};
  const HifzNative={setMarkerDialogOpen(){},comparisonTap(){},correspondenceTap(){}};
  const requestAnimationFrame=()=>{};
  eval(eval(script));
  assert.equal(m.style.marginLeft,marginLeft);
  assert.equal(m.style.marginRight,marginRight);
  if(links.length||parallels.length){
    assert.ok(m.style.width.includes('100vw - '+reserve+'px'),m.style.width);
    assert.ok(m.style.width.includes('345 / 550'),'QCF ratio must be unchanged');
  }else assert.equal(m.style.width,'');
}
console.log('PASS BOOX/phone exact QCF single/double gutter geometry contract');
