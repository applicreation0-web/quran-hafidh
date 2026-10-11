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
actual=(ROOT/FILE).read_text(encoding="utf-8")
assert actual==expected, "Mushaf renderer drift outside exact off-unit fog and original exact-word low-opacity amorce highlight"
assert "quizMaskElement" not in actual and "quiz-prompt" not in actual
assert subprocess.check_output(["git","hash-object",str(ROOT/FILE)],cwd=ROOT,text=True).strip()==CURRENT_BLOB
print("PASS: official Hifz renderer, QCF and masks intact; exact off-unit veil + Lecture-only original low-opacity highlight")
