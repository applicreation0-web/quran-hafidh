'use strict';
(function(){
  const MIN_PAGE=1,MAX_PAGE=604,SAFE_PAGE=1;
  const native=window.QsgNative;
  const strictPage=v=>Number.isInteger(v)&&v>=MIN_PAGE&&v<=MAX_PAGE?v:null;
  const cloneObject=v=>v&&typeof v==='object'&&!Array.isArray(v)?v:{};
  function sanitizeState(raw,fallback){
    let source={};
    try{source=raw?JSON.parse(raw):{}}catch(_){source={}}
    if(!source||typeof source!=='object'||Array.isArray(source)||source.schema!==1)source={};
    const page=strictPage(source.page)??strictPage(fallback)??SAFE_PAGE;
    const zoom=typeof source.zoom==='number'&&Number.isFinite(source.zoom)&&source.zoom>=1&&source.zoom<=3?source.zoom:1;
    const marks=Array.isArray(source.marks)?source.marks.filter(m=>m&&typeof m==='object'&&strictPage(m.page)!==null):[];
    const sessions=Array.isArray(source.sessions)?source.sessions.filter(s=>s&&typeof s==='object'&&!Array.isArray(s)):[];
    const ui=cloneObject(source.ui);
    const mode=ui.mode==='MEMORIZATION'?'MEMORIZATION':'READING';
    const active=typeof source.active==='string'&&sessions.some(s=>s.id===source.active)?source.active:null;
    return JSON.stringify({...source,schema:1,page,zoom,marks,sessions,active,ui:{...ui,mode,selectStart:typeof ui.selectStart==='string'?ui.selectStart:null,selectEnd:typeof ui.selectEnd==='string'?ui.selectEnd:null}});
  }
  function sanitizeInitial(raw){
    let initial={};
    try{initial=raw?JSON.parse(raw):{}}catch(_){initial={}}
    if(!initial||typeof initial!=='object'||Array.isArray(initial))initial={};
    const page=strictPage(initial.page)??SAFE_PAGE;
    const legacy=Array.isArray(initial.legacyBookmarks)?initial.legacyBookmarks.filter(p=>strictPage(p)!==null):[];
    return JSON.stringify({...initial,page,legacyBookmarks:legacy,state:sanitizeState(initial.state,page)});
  }
  if(native&&typeof native.initial==='function'){
    const originalInitial=native.initial.bind(native);
    native.initial=function(){
      let raw='{}';
      try{raw=originalInitial()}catch(e){console.error('QSG initial state failed',e)}
      return sanitizeInitial(raw);
    };
  }

  let pendingPageTimer=null;
  let lastFailure='';
  const clearPending=()=>{if(pendingPageTimer!==null){clearTimeout(pendingPageTimer);pendingPageTimer=null}};
  function showFailure(message,error){
    clearPending();
    lastFailure=message||'Le Muṣḥaf n’a pas pu être affiché.';
    console.error(lastFailure,error||'');
    document.body.classList.remove('booting','hidden','runtime-ready');
    const box=document.getElementById('readerError'),text=document.getElementById('readerErrorText');
    if(text)text.textContent=lastFailure+' La lecture n’est pas validée. Vous pouvez réessayer ou revenir.';
    if(box)box.hidden=false;
    const mushaf=document.getElementById('mushaf');
    if(mushaf)mushaf.setAttribute('aria-busy','false');
  }
  function beginPageLoad(){
    clearPending();
    document.body.classList.remove('runtime-ready');
    document.body.classList.add('booting');
    const box=document.getElementById('readerError');if(box)box.hidden=true;
    const mushaf=document.getElementById('mushaf');if(mushaf)mushaf.setAttribute('aria-busy','true');
    pendingPageTimer=setTimeout(()=>showFailure('Le chargement de la page du Muṣḥaf a expiré.'),12000);
  }
  function substantiveSvg(svg){
    if(!(svg instanceof SVGElement)||svg.localName.toLowerCase()!=='svg')return false;
    if(svg.querySelector('parsererror'))return false;
    const nodes=svg.querySelectorAll('path,text,use,polygon,polyline,line,circle,ellipse,rect');
    if(nodes.length<8)return false;
    const r=svg.getBoundingClientRect();
    if(!(r.width>1&&r.height>1))return false;
    let box=null;try{box=svg.getBBox()}catch(_){}
    if(box&&!(box.width>1&&box.height>1))return false;
    return true;
  }
  function verifyRenderedPage(){
    const mushaf=document.getElementById('mushaf'),svg=mushaf?.querySelector('svg');
    if(!substantiveSvg(svg)){
      showFailure('La page du Muṣḥaf est vide ou invalide.');return false;
    }
    document.body.classList.add('runtime-ready');
    document.body.classList.remove('booting');
    requestAnimationFrame(()=>{
      const r=svg.getBoundingClientRect(),style=getComputedStyle(svg),host=getComputedStyle(mushaf);
      const visible=r.width>1&&r.height>1&&style.display!=='none'&&style.visibility!=='hidden'&&style.opacity!=='0'&&host.display!=='none'&&host.visibility!=='hidden';
      if(!visible){showFailure('La page du Muṣḥaf est chargée mais n’est pas visible.');return}
      clearPending();
      lastFailure='';
      mushaf.setAttribute('aria-busy','false');
      mushaf.dataset.runtimeReady='true';
      const box=document.getElementById('readerError');if(box)box.hidden=true;
    });
    return true;
  }

  const originalFetch=window.fetch.bind(window);
  window.fetch=async function(input,init){
    const url=String(typeof input==='string'?input:input?.url||'');
    const pageRequest=/(^|\/)page\/\d{1,3}(?:$|[?#])/.test(url)||url.includes('../page/');
    if(pageRequest)beginPageLoad();
    try{
      const response=await originalFetch(input,init);
      if(pageRequest&&!response.ok)showFailure('La page du Muṣḥaf est indisponible.');
      return response;
    }catch(e){if(pageRequest)showFailure('Impossible de charger la page du Muṣḥaf.',e);throw e}
  };

  window.addEventListener('error',e=>{if(document.body.classList.contains('booting'))showFailure('Erreur pendant le rendu du Muṣḥaf.',e.error)});
  window.addEventListener('unhandledrejection',e=>{showFailure('Erreur pendant le chargement du Muṣḥaf.',e.reason)});
  document.addEventListener('DOMContentLoaded',()=>{
    const retry=document.getElementById('readerRetry');
    if(retry)retry.addEventListener('click',()=>location.reload());
    const mushaf=document.getElementById('mushaf');
    if(mushaf){
      new MutationObserver(()=>{
        mushaf.dataset.runtimeReady='false';
        document.body.classList.remove('runtime-ready');
        requestAnimationFrame(()=>requestAnimationFrame(verifyRenderedPage));
      }).observe(mushaf,{childList:true});
    }
    setTimeout(()=>{if(document.body.classList.contains('booting'))showFailure('Le lecteur n’a pas terminé son démarrage.')},12000);
  });
  window.QsgRuntimeGuard={sanitizeInitial,sanitizeState,strictPage,verifyRenderedPage,fail:showFailure,get lastFailure(){return lastFailure}};
})();
