#!/usr/bin/env python3
"""Update the legacy Tafsir alignment assertion after semantic block rendering.

The old assertion looked for one unconditional `textAlign = TextAlign.Justify`.
After poetry becomes a separate non-justified block, prose justification is expressed
as the `else TextAlign.Justify` branch instead. Keep the gate strict on that exact
conditional rather than weakening it.
"""
from pathlib import Path

p = Path(__file__).resolve().parents[1] / "scripts/verify_0105_tafsir_typography_contract.py"
text = p.read_text(encoding="utf-8")
old = 'require("textAlign = TextAlign.Justify" in renderer, "commentary/note prose must remain justified")'
new = '''require(
    "textAlign = if (isPoetry) TextAlign.Start else TextAlign.Justify" in renderer,
    "commentary/note prose must remain justified while poetry stays Start-aligned",
)'''
if text.count(old) != 1:
    raise SystemExit(f"expected one legacy alignment assertion, found {text.count(old)}")
p.write_text(text.replace(old, new, 1), encoding="utf-8")
print("Tafsir typography gate updated for explicit prose/poetry alignment")
