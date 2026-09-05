#!/usr/bin/env python3
"""Update legacy Tafsir alignment assertions after semantic block rendering.

The old assertions looked for one unconditional `textAlign = TextAlign.Justify`.
After poetry becomes a separate non-justified block, prose justification is expressed
as the `else TextAlign.Justify` branch instead. Keep both release gates strict on that
exact conditional rather than weakening them.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace_once(rel: str, old: str, new: str) -> None:
    p = ROOT / rel
    text = p.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{rel}: expected one legacy alignment assertion, found {count}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")


replace_once(
    "scripts/verify_0105_tafsir_typography_contract.py",
    'require("textAlign = TextAlign.Justify" in renderer, "commentary/note prose must remain justified")',
    '''require(
    "textAlign = if (isPoetry) TextAlign.Start else TextAlign.Justify" in renderer,
    "commentary/note prose must remain justified while poetry stays Start-aligned",
)''',
)

replace_once(
    "scripts/verify_0105_tafsir_update.py",
    'require("textAlign = TextAlign.Justify" in renderer, "shared Tafsir prose renderer is not justified")',
    '''require(
    "textAlign = if (isPoetry) TextAlign.Start else TextAlign.Justify" in renderer,
    "shared Tafsir prose renderer must remain justified while explicit poetry is Start-aligned",
)''',
)

replace_once(
    "scripts/verify_0105_tafsir_update.py",
    'print("- one shared justified low-glare renderer serves commentary and source notes")',
    'print("- shared low-glare renderer justifies prose/notes while explicit source-tagged poetry stays Start-aligned")',
)

print("Tafsir release gates updated for explicit prose/poetry alignment")
