const assert=require('node:assert/strict');const P=require('../app/src/main/assets/reader109/protocol.js');let checks=0;
for(const n of [1,2,3,4,5,6,7,8,10,15,31]){
 const lines=Array.from({length:n},(_,i)=>'12:'+i);const s=P.create(lines,false),steps=P.plan(lines,false);assert(steps.every(p=>p.lines.every(l=>lines.includes(l))));
 while(P.current(s)){const p=P.current(s);assert(!P.validate(s)||p.min===0);if(p.kind==='confirm')continue;for(let i=0;i<Math.max(p.min,3);i++)P.record(s,true);assert(P.canValidate(s));const before=[...s.milestones];P.aid(s);if(p.success){assert(!P.canValidate(s));for(let i=0;i<3;i++)P.record(s,true)}assert.deepEqual(s.milestones,before);assert(P.validate(s));Object.assign(s,JSON.parse(JSON.stringify(s)));checks++;}assert(s.complete);assert.equal(s.status,'Acquis');assert.equal(s.milestones.length,steps.length);
}
const s=P.create(['1:0'],true);assert(!P.record(s,true,'audio'));P.record(s);P.record(s);P.validate(s);assert.equal(P.current(s).kind,'passive');assert(!P.record(s));assert(P.record(s,true,'audio'));assert(!P.canValidate(s));P.record(s,true,'audio');assert(P.canValidate(s));
for(let i=0;i<100;i++){const rank=P.rank(42,'line:'+i);assert.equal(rank,P.rank(42,'line:'+i));assert(rank>=0&&rank<1)}
console.log('PASS',checks,'milestone transitions; short, normal, long, restart, aid, audio isolation');
