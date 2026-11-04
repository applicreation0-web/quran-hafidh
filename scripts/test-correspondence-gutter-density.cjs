const assert = require('node:assert/strict');
const fs = require('node:fs');
const [source] = process.argv.slice(2);
const input = fs.readFileSync(source,'utf8').split('String js=')[1].split('view.evaluateJavascript')[0];
const script = input.split('\n').filter(line=>!line.trimStart().startsWith('//')).join('\n')
  .replace('+links.toString()+', '+ JSON.stringify(refs) +')
  .replace('+parallels.toString()+', '+ JSON.stringify(refs) +');
const refs=Array.from({length:31},(_,i)=>'55:'+(i+1));
const pending=[];
class Element {
  constructor(tag){this.tag=tag;this.style={};this.children=[];this.attrs={};this.events={};}
  appendChild(x){this.children.push(x);return x;}
  setAttribute(k,v){this.attrs[k]=v;}
  getAttribute(k){return this.attrs[k];}
  addEventListener(k,v){this.events[k]=v;}
  remove(){this.removed=true;}
  showModal(){this.open=true;}
  close(){this.open=false;}
}
const host=new Element('div');
host.clientHeight=224;host.getBoundingClientRect=()=>({left:0,right:360,top:0});
const m=new Element('div');m.getBoundingClientRect=()=>({left:48,right:312});
m.querySelectorAll=()=>refs.map((ref,i)=>({getAttribute:k=>k==='data-verse'?ref:null,getBoundingClientRect:()=>({top:i*7})}));
const document={getElementById:k=>k==='box'?host:k==='mushaf'?m:null,createElement:t=>new Element(t)};
const window={dispatchEvent(){},addEventListener(){},removeEventListener(){}};
const selected=[];
let dialogOpen=false;
const HifzNative={setMarkerDialogOpen:value=>{dialogOpen=value;},comparisonTap:(s,a)=>selected.push(['similarity',s,a]),correspondenceTap:(s,a)=>selected.push(['correspondence',s,a])};
const requestAnimationFrame=f=>pending.push(f);
eval(eval(script));
while(pending.length)pending.shift()();
const root=host.children[0];
const group=root.children.filter(x=>x.attrs['aria-haspopup']==='dialog'&&x.style.visibility==='visible');
assert.equal(group.length,2,'each lane must expose its overflow');
const event={stopPropagation(){}};
for(const button of group){
  button.events.click(event);
  const dialog=root.children.find(x=>x.tag==='dialog'&&x.open);
  assert.ok(dialog);
  assert.equal(dialogOpen,true,'native swipe interception must be suspended');
  for(const x of dialog.children)if(x.attrs['data-verse'])x.events.click(event);
}
assert.equal(dialogOpen,false,'native swipe interception must resume');
for(const btn of root.children.filter(x=>x.tag==='button'&&x.style.visibility==='visible'&&!x.attrs['aria-haspopup']))btn.events.click(event);
assert.equal(new Set(selected.map(JSON.stringify)).size,62);
assert.equal(selected.length,62);
