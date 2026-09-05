#!/usr/bin/env python3
"""Fail-closed documentary gate for the Quran Safeguard Plus 0.10.5 Sharh corpus.

A release is eligible only when every one of the frozen 264 Hikam has two
separate, fully verified classical commentaries: al-Sharnubi and Ibn Abbad.
Research candidates, OCR, inferred boundaries and draft translations never pass.
"""
from __future__ import annotations

import hashlib
import json
import re
from collections import Counter, defaultdict
from pathlib import Path
from urllib.parse import urlparse

ROOT = Path(__file__).resolve().parents[1]
CANONICAL = ROOT / "app/src/main/assets/hikam/al_hikam_verified.json"
SHARH = ROOT / "app/src/plus/assets/hikam/hikam_sharh_dual.json"

COMMENTATORS = {"sharnubi", "ibn_abbad"}
EXPECTED_WORK = {
    "sharnubi": "شرح الحكم العطائية",
    "ibn_abbad": "غيث المواهب العلية في شرح الحكم العطائية",
}
APPROVED_SOURCE_HOSTS = {
    "sharnubi": {"archive.org", "waqfeya.net", "waqfeya.com"},
    "ibn_abbad": {"www.nli.org.il", "nli.org.il"},
}
REQUIRED_STATUS = "verified"
ARABIC = re.compile(r"[\u0621-\u063a\u0641-\u064a]")
PLACEHOLDER = re.compile(
    r"(?:todo|tbd|placeholder|à\s+vérifier|a\s+verifier|draft|candidate|ocr[-_ ]only|"
    r"not\s+verified|non\s+vérifié|non\s+verifie)",
    re.IGNORECASE,
)
TRUNCATION = re.compile(r"(?:\[\s*…\s*\]|\[\.\.\.\]|…\s*$)")


def digest(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def fail(message: str) -> None:
    raise SystemExit("HIKAM SHARH 0.10.5 FAILURE: " + message)


def require_text(entry: dict, key: str, label: str) -> str:
    value = entry.get(key)
    if not isinstance(value, str) or not value.strip():
        fail(f"{label}: missing/non-text {key}")
    value = value.strip()
    if PLACEHOLDER.search(value):
        fail(f"{label}: placeholder/draft marker in {key}")
    return value


def main() -> None:
    if not SHARH.is_file():
        fail(
            "Plus asset app/src/plus/assets/hikam/hikam_sharh_dual.json is absent; "
            "research infrastructure is not publishable content"
        )

    canonical = json.loads(CANONICAL.read_text(encoding="utf-8"))
    if len(canonical) != 264:
        fail(f"canonical corpus changed: {len(canonical)} Hikam")
    canonical_by_number = {int(item["source_number"]): item for item in canonical}
    if set(canonical_by_number) != set(range(1, 265)):
        fail("canonical numbering is not exactly 1..264")

    entries = json.loads(SHARH.read_text(encoding="utf-8"))
    if not isinstance(entries, list):
        fail("Sharh asset root must be an array")
    if len(entries) != 528:
        fail(f"expected 528 entries (264 x 2), got {len(entries)}")

    by_hikma: dict[int, list[dict]] = defaultdict(list)
    seen = set()
    documentary_keys = set()
    arabic_digests = set()
    french_digests = set()
    counts = Counter()

    for index, entry in enumerate(entries):
        if not isinstance(entry, dict):
            fail(f"entry {index}: object required")
        try:
            number = int(entry["source_number"])
        except Exception as exc:
            fail(f"entry {index}: invalid source_number ({exc})")
        commentator = require_text(entry, "commentator_id", f"entry {index}")
        label = f"Hikma {number} / {commentator}"
        if number not in canonical_by_number:
            fail(f"{label}: number outside 1..264")
        if commentator not in COMMENTATORS:
            fail(f"{label}: unsupported commentator")
        key = (number, commentator)
        if key in seen:
            fail(f"{label}: duplicate entry")
        seen.add(key)
        counts[commentator] += 1

        work = require_text(entry, "commentary_work", label)
        if work != EXPECTED_WORK[commentator]:
            fail(f"{label}: unexpected work title {work!r}")
        edition = require_text(entry, "commentary_source_edition", label)
        arabic = require_text(entry, "commentary_arabic", label)
        french = require_text(entry, "commentary_french", label)
        credit = require_text(entry, "commentary_translation_credit", label)
        url = require_text(entry, "commentary_source_url", label)
        locator = require_text(entry, "commentary_print_locator", label)
        boundary = require_text(entry, "commentary_boundary_locator", label)

        if not ARABIC.search(arabic):
            fail(f"{label}: commentary_arabic contains no Arabic letters")
        if ARABIC.search(french):
            fail(f"{label}: French translation contains an unreviewed Arabic-script block")
        if TRUNCATION.search(arabic) or TRUNCATION.search(french):
            fail(f"{label}: truncated excerpt marker; full approved commentary required")
        if len(arabic) < 40:
            fail(f"{label}: Arabic commentary is implausibly short ({len(arabic)} chars)")
        if len(french) < 40:
            fail(f"{label}: French commentary translation is implausibly short ({len(french)} chars)")
        if len(credit) < 3:
            fail(f"{label}: translation credit is not explicit")

        parsed = urlparse(url)
        if parsed.scheme != "https" or parsed.hostname not in APPROVED_SOURCE_HOSTS[commentator]:
            fail(f"{label}: source URL host is not approved: {url}")
        documentary_key = (commentator, edition, locator, boundary)
        if documentary_key in documentary_keys:
            fail(f"{label}: duplicate documentary locator/boundary")
        documentary_keys.add(documentary_key)

        for status_key in (
            "hikma_alignment_status",
            "commentary_status",
            "translation_status",
            "source_boundary_status",
        ):
            if entry.get(status_key) != REQUIRED_STATUS:
                fail(f"{label}: {status_key} must be {REQUIRED_STATUS!r}")

        canonical_arabic = canonical_by_number[number].get("arabic", "").strip()
        expected_matn_hash = digest(canonical_arabic)
        if entry.get("canonical_hikma_arabic_sha256") != expected_matn_hash:
            fail(f"{label}: canonical Hikma hash mismatch")
        if entry.get("commentary_arabic_sha256") != digest(arabic):
            fail(f"{label}: commentary Arabic hash mismatch")
        if entry.get("commentary_french_sha256") != digest(french):
            fail(f"{label}: French translation hash mismatch")

        verification = entry.get("verification")
        if not isinstance(verification, dict):
            fail(f"{label}: verification object missing")
        if verification.get("independent_boundary_check") is not True:
            fail(f"{label}: independent boundary check missing")
        if verification.get("arabic_source_check") is not True:
            fail(f"{label}: Arabic source check missing")
        if verification.get("french_translation_check") is not True:
            fail(f"{label}: French translation check missing")
        reviewer = verification.get("reviewer")
        if not isinstance(reviewer, str) or not reviewer.strip():
            fail(f"{label}: reviewer identity/role missing")
        checked_at = verification.get("checked_at")
        if not isinstance(checked_at, str) or not checked_at.strip():
            fail(f"{label}: verification timestamp missing")

        by_hikma[number].append(entry)
        arabic_hash = digest(arabic)
        french_hash = digest(french)
        if arabic_hash in arabic_digests:
            fail(f"{label}: duplicate Arabic commentary text reused across entries")
        if french_hash in french_digests:
            fail(f"{label}: duplicate French commentary text reused across entries")
        arabic_digests.add(arabic_hash)
        french_digests.add(french_hash)

    if counts != Counter({"sharnubi": 264, "ibn_abbad": 264}):
        fail(f"commentator counts incorrect: {dict(counts)}")

    for number in range(1, 265):
        commentators = {entry["commentator_id"] for entry in by_hikma[number]}
        if commentators != COMMENTATORS:
            fail(f"Hikma {number}: expected both commentators, got {sorted(commentators)}")

    print("Hikam Sharh 0.10.5 documentary gate PASS")
    print("- 264/264 Hikam have al-Sharnubi commentary")
    print("- 264/264 Hikam have Ibn Abbad commentary")
    print("- 528/528 alignments, Arabic texts, translations and source boundaries are independently verified")
    print("- every entry carries explicit work, edition, translation credit, source URL, page locator and boundary locator")
    print("- no placeholders, research candidates, truncated excerpts or duplicated commentary payloads")


if __name__ == "__main__":
    main()
