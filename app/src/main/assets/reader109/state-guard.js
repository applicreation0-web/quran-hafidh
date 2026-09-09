'use strict';
// 0.10.9 migration guard: a historical reader state may keep schema=1 while
// missing a valid page. reader.js starts its async boot before this file runs;
// normalize the persisted page before the geometry fetch continuation renders.
(()=>{
  const requested=Number(initial&&initial.page);
  const fallback=Number.isInteger(requested)&&requested>=1&&requested<=604?requested:1;
  const restored=Number(page);
  if(!Number.isInteger(restored)||restored<1||restored>604){
    page=fallback;
    state.page=fallback;
    try{save()}catch(_){/* rendering must remain available even if persistence fails */}
  }
})();
