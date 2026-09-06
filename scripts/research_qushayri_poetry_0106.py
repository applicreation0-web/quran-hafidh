#!/usr/bin/env python3
from __future__ import annotations
import argparse, hashlib, json, re
from pathlib import Path
import fitz

EXPECTED_SOURCE_SHA256 = "f5b064cfe8ece67a89aaeea04540a7d79c8ef5a531ce2aff880608621e3e1bd3"
TARGETS = [
    "We have not been firm",
    "but justice will be firm with us without bending",
    "If we had been sincere",
]


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as fh:
        for chunk in iter(lambda: fh.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def clean(s: str) -> str:
    return re.sub(r"\s+", " ", s.replace("\u00a0", " ")).strip()


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("pdf", type=Path)
    ap.add_argument("--report", type=Path, required=True)
    args = ap.parse_args()
    actual = sha256(args.pdf)
    if actual != EXPECTED_SOURCE_SHA256:
        raise SystemExit(f"Qushayri source SHA mismatch: {actual}")
    doc = fitz.open(args.pdf)
    report: dict[str, object] = {"pages": len(doc), "poetry_index_pages": [], "target_hits": []}

    for pi, page in enumerate(doc):
        text = page.get_text("text")
        if re.search(r"\bpoetry\s+index\b", text, re.I):
            report["poetry_index_pages"].append({"pdf_page": pi + 1, "text": text})

    for pi, page in enumerate(doc):
        pd = page.get_text("dict")
        flat = clean(page.get_text("text"))
        if not any(t.lower() in flat.lower() for t in TARGETS):
            continue
        lines = []
        for bi, block in enumerate(pd.get("blocks", [])):
            for li, line in enumerate(block.get("lines", [])):
                spans = [s for s in line.get("spans", []) if s.get("text", "").strip()]
                if not spans:
                    continue
                text = "".join(s.get("text", "") for s in spans).strip()
                if not text:
                    continue
                lines.append({
                    "block": bi,
                    "line": li,
                    "text": text,
                    "bbox": [round(float(v), 2) for v in line.get("bbox", (0, 0, 0, 0))],
                    "fonts": [s.get("font", "") for s in spans],
                    "sizes": [round(float(s.get("size", 0.0)), 2) for s in spans],
                })
        report["target_hits"].append({"pdf_page": pi + 1, "lines": lines})

    args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({
        "pages": report["pages"],
        "poetry_index_pages": [x["pdf_page"] for x in report["poetry_index_pages"]],
        "target_pages": [x["pdf_page"] for x in report["target_hits"]],
    }, ensure_ascii=False))
    for entry in report["poetry_index_pages"]:
        print(f"\n--- POETRY INDEX PDF PAGE {entry['pdf_page']} ---\n")
        print(entry["text"][:12000])
    for hit in report["target_hits"]:
        print(f"\n--- TARGET PDF PAGE {hit['pdf_page']} ---")
        for line in hit["lines"]:
            if any(t.lower() in clean(line["text"]).lower() for t in TARGETS) or line["block"] in {
                l["block"] for l in hit["lines"] if any(t.lower() in clean(l["text"]).lower() for t in TARGETS)
            }:
                print(line)

if __name__ == "__main__":
    main()
