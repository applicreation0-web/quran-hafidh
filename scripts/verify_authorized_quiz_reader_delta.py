#!/usr/bin/env python3
"""Proof that the 1.17.1 Mushaf reader is unchanged except the EXACT off-unit focus veil.

The retired Quiz eraser has been removed, not hidden. The remainder of the
reader, including Sabqi/Itqan mask algorithms, is reconstructed byte-for-byte
from the official 1.17.1 blob before asserting equality with the checked source.
"""
import pathlib,subprocess
ROOT=pathlib.Path(__file__).resolve().parents[1]
FILE="hifz-app/src/main/assets/hifzreader/reader.js"
BASE="7fd45bbe3d2e67a2e21c6f8ca1d5ae935d1f0d21"
OFFICIAL_BLOB="8a9611a8bbd0e5983f0f9ee4e98ddfd9484efa44"
CURRENT_BLOB="cd1c922e1525c088f3ea672a8549294e643746ac"
original=subprocess.check_output(["git","show",f"{BASE}:{FILE}"],cwd=ROOT).decode("utf-8")
assert subprocess.check_output(["git","rev-parse",f"{BASE}:{FILE}"],cwd=ROOT,text=True).strip()==OFFICIAL_BLOB
helper="/* Off-unit page: the due physical lines are on another Mushaf page.\n * Keep all Quranic ink legible but faint. Never expose off-unit text at full\n * contrast or invent a mask hole when this page has zero due-line geometry.\n * No Hifz session/cursor/credit is touched: display-only SVG overlay. */\nfunction fadedOffUnitPage(svg){\n  const g=document.createElementNS(NS,'g');\n  g.setAttribute('class','focuscontextlayer');\n  g.setAttribute('pointer-events','none');\n  const vb=svg.viewBox&&svg.viewBox.baseVal;\n  if(!vb)return g;\n  const paper=document.createElementNS(NS,'rect');\n  paper.setAttribute('x',vb.x);paper.setAttribute('y',vb.y);\n  paper.setAttribute('width',vb.width);paper.setAttribute('height',vb.height);\n  paper.setAttribute('fill','var(--sheet)');\n  paper.setAttribute('fill-opacity',eink?'0.72':'0.65');\n  g.appendChild(paper);\n  return g;\n}\n\n"
old="  if(contextFocus&&lines.length){\n    const context=focusContextLayer(svg,lines,maskFollowsSelection?selectedPolygons(svg):[]);\n    if(context.childNodes.length)svg.appendChild(context);\n  }\n"
new="  if(contextFocus&&lines.length){\n    const context=focusContextLayer(svg,lines,maskFollowsSelection?selectedPolygons(svg):[]);\n    if(context.childNodes.length)svg.appendChild(context);\n  }else if(contextFocus&&lineIds.length&&pageGeo&&(pageGeo.lines||[]).length){\n    // The selected unit is on a different page: fade this entire context page.\n    // Original logic fell through here and displayed unrelated lines at 100%.\n    const context=fadedOffUnitPage(svg);\n    if(context.childNodes.length)svg.appendChild(context);\n  }\n\n"
assert original.count("const AMORCE_HIGHLIGHT_HEIGHT=31;")==1
assert original.count(old)==1
expected=original.replace("const AMORCE_HIGHLIGHT_HEIGHT=31;",helper+"const AMORCE_HIGHLIGHT_HEIGHT=31;").replace(old,new)
# Only the Lecture-only tapped cue decoration changes. Sabqi, Itqan,
# masks and Quranic SVG geometry must remain byte-for-byte frozen.
AMORCE_BEFORE="function semanticCueRect(rect,cue){\n  const el=document.createElementNS(NS,'rect');\n  el.setAttribute('x',rect.x);el.setAttribute('y',rect.y);\n  el.setAttribute('width',rect.width);el.setAttribute('height',rect.height);\n  el.setAttribute('rx','1.35');el.setAttribute('ry','1.35');\n  // User decision: a light grey highlight behind the amorce words — never over the glyphs as a\n  // pattern. Lighter than any reading-mask tile, and only ever drawn in Lecture (Révision\n  // active and post-An-Nās Itqān use the amorces as mask holes with highlighting disabled).\n  el.setAttribute('fill','var(--sel)');\n  el.setAttribute('fill-opacity',eink?'0.16':'0.10');\n  el.setAttribute('stroke','none');el.setAttribute('pointer-events','all');\n  el.onclick=event=>{event.stopPropagation();if(cue.id)N?.semanticCueTap?.(String(cue.id));};\n  return el;\n}\n\n"
AMORCE_AFTER="function semanticCueRect(rect,cue){\n  const el=document.createElementNS(NS,'rect');\n  el.setAttribute('x',rect.x);el.setAttribute('y',rect.y);\n  el.setAttribute('width',rect.width);el.setAttribute('height',rect.height);\n  el.setAttribute('rx','1.35');el.setAttribute('ry','1.35');\n  // User decision: a light grey highlight behind the amorce words — never over the glyphs as a\n  // pattern. Lighter than any reading-mask tile, and only ever drawn in Lecture (Révision\n  // active and post-An-Nās Itqān use the amorces as mask holes with highlighting disabled).\n  el.setAttribute('fill','var(--sel)');\n  el.setAttribute('fill-opacity',eink?'0.16':'0.10');\n  el.setAttribute('stroke','none');el.setAttribute('pointer-events','all');\n  el.onclick=event=>{event.stopPropagation();if(cue.id)N?.semanticCueTap?.(String(cue.id));};\n  return el;\n}\n\n"
assert original.count(AMORCE_BEFORE)==1, "Official amorce decoration changed"
expected=expected.replace(AMORCE_BEFORE,AMORCE_AFTER)
# The official 1.17.2 fix does not touch Quranic SVG, masking geometry,
# Hifz selection, or randomized erasure: only the 2 native bridge callbacks
# gain a load-generation parameter. The substitutions are independently
# counted and the resulting blob is pinned; all other changes fail closed.
GENERATION_BLOB="7961f9a8601cd456275d9acad86ccf46a05d2806"
ready_before="N?.ready();"
shown_before="N?.pageShown(currentPage);"
assert expected.count(ready_before)==1 and expected.count(shown_before)==1
expected=expected.replace(ready_before,"N?.ready(Number(boot.renderGeneration||0));")
expected=expected.replace(shown_before,"N?.pageShown(currentPage,Number(boot.renderGeneration||0));")
# The 1.17.4 full-mask fix is reconstructed from the exact 1.17.3
# bridge-approved JS. Partial 25/50/75% coverage must be byte-identical.
previous=subprocess.check_output(
    ["git","show","6831e1c670a431c5deff09642434521b644bcebb:"+FILE],
    cwd=ROOT).decode("utf-8")
assert previous==expected, "Reader generation-approved JS baseline unexpectedly changed"
ERASER_BEFORE="""  const full=segments.slice();
  if(!full.length||!pageWordBoxes.length)return [];
  const out=[];"""
ERASER_AFTER="""  const full=segments.slice();
  if(!full.length||!pageWordBoxes.length)return [];
  // The 100% eraser must cover the REAL exact-word ink of every due line.
  // Earlier cell-centre filtering missed overhanging harakat/letters, producing
  // residual straight streaks on physical folios 6/11. Partial masks remain
  // randomized by cell as before; this is strictly a 100% correction.
  const completelyMasked=full.every(s=>s.fullErase);
  const out=[];"""
WORDS_BEFORE="""    if(!full.some(s=>s.lineId===lineId&&cx>=s.x&&cx<=s.x+s.width))return;
    out.push({key:'w'+i,lineId,x:box[0],y:box[1],width:box[2]-box[0],height:box[3]-box[1],fullErase:true});"""
WORDS_AFTER="""    if(!completelyMasked&&!full.some(s=>s.lineId===lineId&&cx>=s.x&&cx<=s.x+s.width))return;
    // The exact-hole mask is applied after the eraser, so all protected words
    // remain visible and rosettes are redrawn in their unchanged QCF positions.
    const pad=completelyMasked?1.15:0;
    out.push({key:'w'+i,lineId,x:box[0]-pad,y:box[1]-pad,
      width:(box[2]-box[0])+2*pad,height:(box[3]-box[1])+2*pad,fullErase:true});"""
for a,b in ((ERASER_BEFORE,ERASER_AFTER),(WORDS_BEFORE,WORDS_AFTER)):
    assert expected.count(a)==1, "Approved 1.17.4 mask source patch no longer unique"
    expected=expected.replace(a,b)
actual=(ROOT/FILE).read_text(encoding="utf-8")
assert actual==expected, "UNAUTHORIZED JS change beyond exact 100% full-word erase"
assert "quizMaskElement" not in actual and "quiz-prompt" not in actual
assert subprocess.check_output(["git","hash-object",str(ROOT/FILE)],cwd=ROOT,text=True).strip()=="3c76e9d7e2133bf3a68f0ff466fbc0fdb2dfb5f2"
print("PASS: byte-reconstructed reader JS, 1.17.4 100% exact-word eraser only; 25/50/75% frozen")
