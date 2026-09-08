/* Quran Safeguard 0.10.9: pure, persisted pedagogical state machine. */
(function(root){
'use strict';
function balancedBlocks(lines){
 /* Keep blocks close to five real Mushaf lines without pathological tails (5+5+1). */
 const blockCount=lines.length<=6?1:Math.ceil(lines.length/6),base=Math.floor(lines.length/blockCount),extra=lines.length%blockCount;
 const blocks=[];for(let i=0,offset=0;i<blockCount;i++){const n=base+(i<extra?1:0);blocks.push(lines.slice(offset,offset+n));offset+=n;}
 return blocks;
}
function plan(lines,audio){
 if(!Array.isArray(lines)||!lines.length)throw Error('Aucune ligne sélectionnée');
 const blocks=balancedBlocks(lines);
 const steps=[],add=(id,label,ls,min,mask,kind='personal',success=false)=>steps.push({id,label,lines:ls,min,mask,kind,success});
 blocks.forEach((b,bi)=>{
  const prefix='B'+bi,offset=lines.indexOf(b[0]);
  b.forEach((line,li)=>{
   const lid=prefix+'L'+li, label='L'+(offset+li+1);
   if(audio){add(lid+'passive',label+' · Écoute passive',[line],2,0,'passive');add(lid+'active',label+' · Écoute active',[line],3,0,'active');}
   [0,25,50,75,100].forEach((mask,mi)=>add(lid+'M'+mask,'Valider — '+(mask?mask+' %':'Visible')+' · '+label,[line],[10,5,5,5,7][mi],mask,'personal',mask===100));
   add(lid+'confirm','Valider '+label,[line],0,100,'confirm');
   if(li>0)add(prefix+'chain'+li,li===b.length-1?'Valider le bloc de '+b.length+' lignes':'Valider '+('L'+(offset+1))+'–'+label,b.slice(0,li+1),li===b.length-1?7:5,100,'personal',false);
  });
  if(bi>0)add('blocks'+bi,'Enchaîner les blocs',blocks.slice(0,bi+1).flat(),bi===blocks.length-1?7:5,100);
 });
 add('final','Valider le bloc mémorisé',lines,3,100,'personal',true);
 return steps;
}
function create(lines,audio=false){return {schema:1,lines,withAudio:audio,step:0,counts:{},wins:{},milestones:[],status:'À apprendre',due:Date.now(),seed:Math.floor(Math.random()*2147483647),complete:false};}
function current(s){return plan(s.lines,s.withAudio)[s.step]||null;}
function assistanceActive(s){const p=current(s);return !!p&&s.assistance?.stepId===p.id;}
function canValidate(s){const p=current(s);return !!p&&!assistanceActive(s)&&(s.counts[p.id]||0)>=p.min&&(!p.success||(s.wins[p.id]||0)>=3);}
function record(s,correct=true,source='personal'){
 const p=current(s);if(!p||p.kind==='confirm')return false;
 if(assistanceActive(s))return false;
 if((p.kind==='passive'||p.kind==='active')&&source!=='audio')return false;
 if(p.kind!=='passive'&&p.kind!=='active'&&source==='audio')return false;
 s.counts[p.id]=(s.counts[p.id]||0)+1;s.wins[p.id]=correct?(s.wins[p.id]||0)+1:0;return true;
}
function aid(s,kind='help',hintWords=0){const p=current(s);if(!p)return false;s.wins[p.id]=0;s.assistance={stepId:p.id,kind,hintWords};return true;}
function clearAid(s){delete s.assistance;}
function validate(s){if(!canValidate(s))return false;const p=current(s);if(!s.milestones.includes(p.id))s.milestones.push(p.id);clearAid(s);s.step++;if(!current(s)){s.complete=true;s.status='Acquis';s.due=Date.now()+86400000;}return true;}
function redo(s){const p=current(s);if(p){s.counts[p.id]=0;s.wins[p.id]=0;}clearAid(s);}
function rank(seed,id){let h=(seed|0)^2166136261;for(let i=0;i<id.length;i++){h^=id.charCodeAt(i);h=Math.imul(h,16777619);}return (h>>>0)/4294967296;}
const api={balancedBlocks,plan,create,current,assistanceActive,canValidate,record,aid,clearAid,validate,redo,rank};if(typeof module!=='undefined')module.exports=api;root.QsgProtocol=api;
})(typeof globalThis!=='undefined'?globalThis:this);
