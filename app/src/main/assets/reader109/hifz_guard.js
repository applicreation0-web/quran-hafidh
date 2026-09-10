/* Quran Safeguard 0.10.10 — structured Hifz reader isolation. */
'use strict';
(function(){
 if(!initial?.hifz)return;
 const targetStart=String(initial.hifzStart||'');
 const targetEnd=String(initial.hifzEnd||'');
 if(!targetStart||!targetEnd){N?.exit();return;}

 memory=true;
 N?.setMode(true);

 const targetKeys=()=>geo?rangeKeys(targetStart,targetEnd):[];
 const activeSessionIsTarget=()=>{
  const s=currentSession();
  return !!s&&Array.isArray(s.verses)&&s.verses.length>0&&
    s.verses[0]===targetStart&&s.verses.at(-1)===targetEnd;
 };
 if(currentSession()&&!activeSessionIsTarget()){
  state.active=null;
 }
 if(!currentSession()){
  selectStart=targetStart;
  selectEnd=targetEnd;
 }

 const normalShowPage=showPage;
 showPage=async function(p,focus=null){
  if(geo){
   const startPages=geo.verses[targetStart]||[];
   const endPages=geo.verses[targetEnd]||[];
   const minPage=Math.min(...startPages,...endPages);
   const maxPage=Math.max(...startPages,...endPages);
   if(Number.isFinite(minPage)&&Number.isFinite(maxPage))p=Math.max(minPage,Math.min(maxPage,p));
  }
  return normalShowPage(p,focus);
 };

 const normalSelectionKeys=selectionKeys;
 selectionKeys=function(){
  if(!initial.hifz)return normalSelectionKeys();
  const s=currentSession();
  return s?.verses?.length?s.verses:targetKeys();
 };

 verseTap=function(k){
  chrome();
  if(targetKeys().includes(k))focusVerse(k);
 };

 const normalRenderMemory=renderMemory;
 renderMemory=function(){
  if(!initial.hifz)return normalRenderMemory();
  memory=true;
  const mem=$('mem');mem.hidden=false;
  const s=currentSession();
  if(s){
   normalRenderMemory();
   return;
  }
  const body=$('memcontrols');body.replaceChildren();
  const keys=targetKeys(),ls=selectedLines(keys);
  $('stepLabel').textContent=(initial.hifzTrack||'HIFZ')+' · '+targetStart+' → '+targetEnd;
  $('memCount').textContent=ls.length+' ligne(s) du Muṣḥaf · passage imposé par le parcours';
  const audioReady=localAudioReady(keys);
  body.append(button(
    'Commencer'+(audioReady?' · audio local':''),
    ()=>begin(keys,ls),
    'capsule',
    !keys.length||!ls.length
  ));
  if(initial.audio&&!audioReady){
   addText(body,'Al-Husary reste disponible après téléchargement local des versets.','small muted');
  }
 };

 const normalMemoryDetails=memoryDetails;
 memoryDetails=function(){
  if(!initial.hifz)return normalMemoryDetails();
  const b=sheet('Séance Hifz');
  addText(b,(initial.hifzTrack||'Hifz')+' · '+targetStart+' → '+targetEnd);
  const s=currentSession();
  if(!s){addText(b,'Le passage est fixé par le Parcours Hifz.','small muted');return;}
  addText(b,'La progression de cette séance reste séparée de Mémorisation libre.','small muted');
  b.append(button('Recommencer ce passage',()=>{closeSheet();restart(s)}));
  if(s.withAudio)b.append(button('Réécouter',()=>{closeSheet();playLines(P.current(s)?.lines||s.lines,1,false)}));
 };

 exitMemory=function(){
  invalidateBriefReveal();
  save();
  N?.exit();
 };

 options=function(){
  const b=sheet('Séance Hifz');
  addText(b,(initial.hifzTrack||'Hifz')+' · '+targetStart+' → '+targetEnd,'small');
  if(initial.audio)b.append(button('Audio Al-Husary Muʿallim',audioOptions));
  b.append(button('Retour au parcours',()=>N?.exit(),'text'));
 };

 const constrainUi=()=>{
  if(!geo){setTimeout(constrainUi,40);return;}
  const pages=[...(geo.verses[targetStart]||[]),...(geo.verses[targetEnd]||[])];
  if(pages.length){
   const minPage=Math.min(...pages),maxPage=Math.max(...pages);
   $('progress').min=String(minPage);$('progress').max=String(maxPage);
  }
  renderMarks();renderMemory();
 };
 constrainUi();
})();
