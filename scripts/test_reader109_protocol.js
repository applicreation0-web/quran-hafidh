const assert=require('node:assert/strict');const P=require('../app/src/main/assets/reader109/protocol.js');let checks=0;
for(const n of [1,2,3,4,5,6,7,8,10,11,12,15,16,31]){
 const lines=Array.from({length:n},(_,i)=>'12:'+i);const s=P.create(lines,false),steps=P.plan(lines,false);assert(steps.every(p=>p.lines.every(l=>lines.includes(l))));
 const sizes=P.balancedBlocks(lines).map(b=>b.length);assert.deepEqual(P.balancedBlocks(lines).flat(),lines);assert(sizes.every(size=>size<=7));if(sizes.length>1){assert(sizes.every(size=>size>=4));assert(Math.max(...sizes)-Math.min(...sizes)<=1)}
 for(const label of ['Valider — Visible','Valider — 25 %','Valider — 50 %','Valider — 75 %','Valider — 100 %'])assert(steps.some(p=>p.label.startsWith(label)));
 assert.equal(steps.at(-1).label,'Valider le bloc mémorisé');
 while(P.current(s)){const p=P.current(s);assert(!P.validate(s)||p.min===0);if(p.kind==='confirm')continue;for(let i=0;i<Math.max(p.min,3);i++)P.record(s,true);assert(P.canValidate(s));const before=[...s.milestones];P.aid(s);if(p.success){assert(!P.canValidate(s));for(let i=0;i<3;i++)P.record(s,true)}assert.deepEqual(s.milestones,before);assert(P.validate(s));Object.assign(s,JSON.parse(JSON.stringify(s)));checks++;}assert(s.complete);assert.equal(s.status,'Acquis');assert.equal(s.milestones.length,steps.length);
}
const s=P.create(['1:0'],true);assert.equal(P.current(s).kind,'passive');assert(!P.record(s));assert(P.record(s,true,'audio'));assert(!P.canValidate(s));P.record(s,true,'audio');assert(P.canValidate(s));P.validate(s);assert.equal(P.current(s).kind,'active');for(let i=0;i<3;i++)assert(P.record(s,true,'audio'));assert(P.canValidate(s));
const failed=P.create(['1:0'],false);for(let i=0;i<2;i++)P.record(failed);P.record(failed,false);assert.equal(failed.wins[P.current(failed).id],0);assert.equal(failed.counts[P.current(failed).id],3);
const stable=P.create(['1:0'],false);assert.equal(P.rank(stable.seed,'1:0:3'),P.rank(stable.seed,'1:0:3'));const persisted=JSON.parse(JSON.stringify(stable));assert.equal(persisted.seed,stable.seed);
for(let i=0;i<100;i++){const rank=P.rank(42,'line:'+i);assert.equal(rank,P.rank(42,'line:'+i));assert(rank>=0&&rank<1)}
console.log('PASS',checks,'milestone transitions; short, normal, long, restart, aid, audio isolation');
