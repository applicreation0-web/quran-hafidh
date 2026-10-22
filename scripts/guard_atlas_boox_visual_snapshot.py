#!/usr/bin/env python3
"""Fail-closed visual freeze of Atlas/Mushaf/Jalalayn BOOX screenshot-approved sources.

The only 1.17.2 departures from the 22 October BOOX visual snapshot are\nthe exact QCF empty-lines fail-open protection in Lecture and the byte-pinned\nMushafView JavaScript load-generation fix. All other Atlas/Jalalayn sources\nremain frozen. Never bundle private material here.
"""
import hashlib
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "hifz-app/src/main/java/com/quransafeguard/hifz/preview"
PINNED = {
    "MawdudiMapScreen.java": "95d74a4df66374d258f10c47e01996e89d37f392",
    "StudyReaderActivity.java": "cc4e078af15a54b12951a3eedcfa9283054cc669",
    "MapJalalaynDialog.java": "e2f82338c1feb0673b51e9a6f9375e4be37861b9",
    "MapQcfThumbnails.java": "750013d368854082d6dc6d1094d9c472ed962cc7",
    "MapUnitMarkers.java": "deaf4f97a08a771c88ba21048a60419323b613ab",
    "MushafView.java": "8ac1afcb199ef7539dd34b320156e4e64741f644"
}

def git_blob(raw: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(raw)).encode() + b"\0" + raw).hexdigest()

def verify_deltas() -> None:
    """Two and ONLY two reviewed departures from the October 22 screenshot baseline.

    The shared MushafView is separately guarded by an exact pinned hash AND
    the authorized renderer verifier, preserving the immutable Hifz engines.
    The Lecture change is reconstructed from the October 22 Git blob, and
    may never silently alter the approved Atlas/Jalalayn presentation.
    """
    baseline="040af230d35f148ac323155a8ae6868e89094aef"
    source="hifz-app/src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java"
    before=subprocess.check_output(["git","show",f"{baseline}:{source}"],cwd=ROOT)
    expected_old=b"                if(landmarks.length()==6) {"
    expected_new=(b"                // Do not report a ready mask if WebView cannot mask any physical QCF line.\n"
                  b"                if(!GeometryRepository.get(this).lineIdsOnPage(page).isEmpty() && landmarks.length()==6) {")
    if before.count(expected_old)!=1:
        raise SystemExit("ATLAS FREEZE FAIL: baseline Lecture mask check no longer unique")
    expected=before.replace(expected_old,expected_new)
    if (ROOT/source).read_bytes()!=expected:
        raise SystemExit("ATLAS FREEZE FAIL: Lecture changed beyond QCF empty-lines guard")
    subprocess.run(["python3","scripts/verify_authorized_mushaf_reader_delta.py"],
                   cwd=ROOT,check=True)


def main() -> None:
    verify_deltas()
    failed = []
    for filename, expected in PINNED.items():
        p = SOURCE / filename
        if not p.is_file():
            failed.append(filename + ": missing")
            continue
        actual = git_blob(p.read_bytes())
        if actual != expected:
            failed.append(filename + ": unexpected source delta " + actual)
    if failed:
        raise SystemExit("ATLAS BOOX VISUAL FREEZE FAILED\n" + "\n".join(failed))
    print("PASS: Atlas dual rail, reader/Jalalayn, QCF and markers exact Git blobs (" +
          str(len(PINNED)) + " files)")

if __name__ == "__main__":
    main()
