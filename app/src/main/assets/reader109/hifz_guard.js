/* Quran Safeguard 0.10.10 — structured Hifz reader isolation. */
'use strict';
(function(){
 if(!initial?.hifz)return;
 const targetStart=String(initial.hifzStart||'');
 const targetEnd=String(initial.hifzEnd||'');
 if(!targetStart||!targetEnd){N?.exit();return;}

 // Structured Hifz has one native source of truth. Never reuse free-memorisation
 // protocol sessions, counters or reveal state.
 state.sessions=[];
 state.active=null;
 selectStart=null;
 selectEnd=null;
 memory=true;
 N?.setMode(true);
 let nativeRevealGeneration=0,nativeRevealTimer=null,nativeRevealVisible=false;

 const targetKeys=()=>geo?rangeKeys(targetStart,targetEnd):[];
 const status=()=>{
  try{return JSON.parse(N?.hifzStatus?.()||'{"valid":false}')}catch(_){return {valid:false}}
 };
 const trackLabel=t=>t==='SABQI'?'Sabqi':t==='ITQAN'?'Itqān':t==='MURAJAAH'?'Murājaʿah':'Hifz';
 const targetPages=()=>{
  if(!geo)return [];
  const keys=targetKeys();
  return [...new Set(keys.flatMap(k=>geo.verses[k]||[]))].sort((a,b)=>a-b);
 };

 const normalShowPage=showPage;
 showPage=async function(p,focus=null){
  if(geo){
   const pages=targetPages();
   if(pages.length)p=Math.max(pages[0],Math.min(pages.at(-1),p));
  }
  return normalShowPage(p,focus);
 };

 const normalSelectionKeys=selectionKeys;
 selectionKeys=function(){
  if(!initial.hifz)return normalSelectionKeys();
  return targetKeys();
 };

 verseTap=function(k){
  chrome();
  if(targetKeys().includes(k))focusVerse(k);
 };

 const normalRenderMasks=renderMasks;
 renderMasks=function(){
  if(!initial.hifz)return normalRenderMasks();
  const svg=$('mushaf').querySelector('svg');if(!svg)return;
  svg.querySelectorAll('.masklayer').forEach(x=>x.remove());
  const st=status(),mask=nativeRevealVisible?0:Number(st.maskPercent||0);
  if(!st.valid||st.completed||mask<=0)return;
  const keys=targetKeys();if(!keys.length)return;
  const NS='http://www.w3.org/2000/svg',group=document.createElementNS(NS,'g');group.classList.add('masklayer');
  const defs=document.createElementNS(NS,'defs');group.append(defs);
  const clip=document.createElementNS(NS,'clipPath');clip.id='hifz-selection-clip';clip.setAttribute('clipPathUnits','userSpaceOnUse');
  svg.querySelectorAll('.ayahPolygon').forEach(p=>{if(keys.includes(p.dataset.verse)){const q=p.cloneNode();q.removeAttribute('id');q.removeAttribute('class');q.removeAttribute('tabindex');q.setAttribute('fill-opacity','1');clip.append(q)}});
  defs.append(clip);
  const masked=document.createElementNS(NS,'g');masked.setAttribute('clip-path','url(#hifz-selection-clip)');group.append(masked);
  const lines=selectedLines(keys).map(lineById).filter(l=>l&&l.page===page);
  for(const line of lines){line.cells.forEach((cell,i)=>{if(mask===100||P.rank(initial.hifzTaskId||'hifz',line.id+':'+i)<mask/100){const r=document.createElementNS(NS,'rect');r.setAttribute('x',cell[0]);r.setAttribute('y',line.top);r.setAttribute('width',cell[1]-cell[0]);r.setAttribute('height',line.bottom-line.top);r.setAttribute('fill','#F7F2E8');masked.append(r)}})}
  svg.append(group);
 };

 function refreshNative(){
  renderMemory();renderMarks();save();
 }
 function nativeAttempt(correct){
  if(N?.hifzAttempt?.(!!correct)!==true){notice('Progression Hifz non enregistrée');return}
  refreshNative();
 }
 function nativeAdvance(){
  const before=status();if(!before.valid||!before.canAdvance)return;
  if(N?.hifzAdvance?.()!==true){notice('Étape Hifz non validée');return}
  nativeRevealVisible=false;refreshNative();visual('MILESTONE');
 }
 function briefNativeReveal(){
  const st=status();if(!st.valid||st.completed||Number(st.maskPercent||0)<=0)return;
  if(N?.hifzReveal?.()!==true){notice('Aide Hifz non enregistrée');return}
  const generation=++nativeRevealGeneration;
  if(nativeRevealTimer!==null)clearTimeout(nativeRevealTimer);
  nativeRevealVisible=true;renderMasks();renderMemory();visual('REVEAL');
  nativeRevealTimer=setTimeout(()=>{
   if(generation!==nativeRevealGeneration)return;
   nativeRevealTimer=null;nativeRevealVisible=false;renderMasks();renderMemory();visual('REVEAL_RETURN');
  },eink?900:1500);
 }
 function playTarget(count=1){
  const keys=targetKeys();
  if(!localAudioReady(keys)){notice('Téléchargez d’abord l’audio Al-Husary du passage');audioOptions();return}
  audioQueue=[];audioCountsProgress=false;
  for(let i=0;i<count;i++)audioQueue.push(...keys);
  nextAudio();
 }

 renderMemory=function(){
  memory=true;
  const mem=$('mem');mem.hidden=false;
  const body=$('memcontrols');body.replaceChildren();
  const st=status(),keys=targetKeys(),ls=geo?selectedLines(keys):[];
  if(!st.valid){
   $('stepLabel').textContent='Séance Hifz indisponible';
   $('memCount').textContent='Aucune progression n’est enregistrée.';
   body.append(button('Retour au parcours',()=>N?.exit(),'capsule'));
   return;
  }
  const phase=st.segmentIndex>=st.segmentCount?'Assemblage':('Segment '+(Number(st.segmentIndex)+1)+' / '+st.segmentCount);
  $('stepLabel').textContent=trackLabel(st.track)+' · '+(st.completed?'Terminé':(st.stepLabel||'Étape'));
  $('memCount').textContent=st.completed
   ? targetStart+' → '+targetEnd+' · progression prête à clôturer'
   : phase+' · '+st.repetitions+' / '+st.requiredRepetitions+' répétition(s)'+
     (st.requiredConsecutiveSuccesses?(' · '+st.consecutiveSuccesses+' / '+st.requiredConsecutiveSuccesses+' correctes consécutives'):'')+
     ' · aides '+st.totalRevealCount+' · erreurs '+st.totalIncorrectAttempts;

  if(st.completed){
   body.append(button('Retour au parcours',()=>N?.exit(),'capsule'));
   return;
  }

  if(st.kind==='AUDIO_PASSIVE'||st.kind==='AUDIO_ACTIVE'){
   body.append(button('▶ Al-Husary',()=>playTarget(Math.max(1,Number(st.requiredRepetitions)||1)),'capsule'));
  }else{
   body.append(button('Correct',()=>nativeAttempt(true),'capsule'));
   body.append(button('À refaire',()=>nativeAttempt(false),'capsule'));
  }
  if(Number(st.maskPercent||0)>0){
   const eye=button('◉',briefNativeReveal,'');eye.setAttribute('aria-label','Afficher brièvement');body.append(eye);
  }
  body.append(button('Valider',nativeAdvance,'capsule',!st.canAdvance));
  if(initial.audio){
   body.append(button(localAudioReady(keys)?'▶ Audio':'Audio',()=>localAudioReady(keys)?playTarget(repeats):audioOptions(),'text'));
  }
  renderMasks();
 };

 memoryDetails=function(){
  const b=sheet('Séance Hifz'),st=status();
  addText(b,trackLabel(st.track)+' · '+targetStart+' → '+targetEnd);
  if(st.valid){
   addText(b,'Temps actif enregistré : '+Math.floor(Number(st.activeSeconds||0)/60)+' min '+(Number(st.activeSeconds||0)%60)+' s','small');
   addText(b,'Aides : '+Number(st.totalRevealCount||0)+' · erreurs : '+Number(st.totalIncorrectAttempts||0),'small muted');
  }
  if(initial.audio)b.append(button('Audio Al-Husary Muʿallim',audioOptions));
  b.append(button('Retour au parcours',()=>N?.exit(),'text'));
 };

 exitMemory=function(){
  nativeRevealGeneration++;
  if(nativeRevealTimer!==null){clearTimeout(nativeRevealTimer);nativeRevealTimer=null}
  nativeRevealVisible=false;save();N?.exit();
 };

 options=function(){
  const b=sheet('Séance Hifz'),st=status();
  addText(b,trackLabel(st.track)+' · '+targetStart+' → '+targetEnd,'small');
  if(initial.audio)b.append(button('Audio Al-Husary Muʿallim',audioOptions));
  b.append(button('Retour au parcours',()=>N?.exit(),'text'));
 };

 const constrainUi=()=>{
  if(!geo){setTimeout(constrainUi,40);return;}
  const pages=targetPages();
  if(pages.length){$('progress').min=String(pages[0]);$('progress').max=String(pages.at(-1))}
  renderMarks();renderMemory();
 };
 constrainUi();
})();
