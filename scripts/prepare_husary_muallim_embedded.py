#!/usr/bin/env python3
"""Prepare a strictly personal embedded Al-Husary Muallim audio corpus.

Runtime Android code never downloads audio. This build-time helper fetches the original
per-surah EveryAyah ZIPs, extracts only canonical ayah MP3s, verifies the exact 6,236
Hafs keys, and writes source + SHA-256 evidence next to the audio.

Canonical upstream source used by this personal build:
  directory: https://everyayah.com/data/Husary_Muallim_128kbps/
  surah zips: https://everyayah.com/data/Husary_Muallim_128kbps/zips/SSS.zip
  checksum:  https://everyayah.com/data/Husary_Muallim_128kbps/000_checksum.md5
  full zip:  https://everyayah.com/data/Husary_Muallim_128kbps/000_versebyverse.zip
"""

from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import json
import shutil
import tempfile
import urllib.request
import zipfile
from pathlib import Path

BASE = "https://everyayah.com/data/Husary_Muallim_128kbps"
DIRECTORY_URL = BASE + "/"
ZIP_BASE = BASE + "/zips"
ZIP_TEMPLATE = ZIP_BASE + "/{surah:03d}.zip"
UPSTREAM_CHECKSUM_URL = BASE + "/000_checksum.md5"
FULL_ARCHIVE_URL = BASE + "/000_versebyverse.zip"
LANDING_PAGE = "https://everyayah.com/recitations_ayat.html"
DEFAULT_OUTPUT = Path("private/hifz-audio/husary-muallim")
AYAHS = [
    7,286,200,176,120,165,206,75,129,109,123,111,43,52,99,128,111,110,98,135,
    112,78,118,64,77,227,93,88,69,60,34,30,73,54,45,83,182,88,75,85,54,53,
    89,59,37,35,38,29,18,45,60,49,62,55,78,96,29,22,24,13,14,11,11,18,12,
    12,30,52,52,44,28,28,20,56,40,31,50,40,46,42,29,19,36,25,22,17,19,26,
    30,20,15,21,11,8,8,19,5,8,8,11,11,8,3,9,5,4,7,3,6,3,5,4,5,6,
]
EXPECTED_COUNT = sum(AYAHS)


def canonical_names() -> list[str]:
    return [f"{s:03d}{a:03d}.mp3" for s, count in enumerate(AYAHS, start=1) for a in range(1, count + 1)]


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def download(url: str, target: Path) -> None:
    req = urllib.request.Request(url, headers={"User-Agent": "Quran-Hifz-personal-build/0.7"})
    with urllib.request.urlopen(req, timeout=180) as src, target.open("wb") as out:
        shutil.copyfileobj(src, out, length=1024 * 1024)


def fetch_surah(surah: int, output: Path, tmp: Path) -> None:
    expected = {f"{surah:03d}{ayah:03d}.mp3" for ayah in range(1, AYAHS[surah - 1] + 1)}
    archive = tmp / f"{surah:03d}.zip"
    url = ZIP_TEMPLATE.format(surah=surah)
    print(f"[{surah:03d}/114] {url}")
    download(url, archive)
    found: set[str] = set()
    with zipfile.ZipFile(archive) as zf:
        for info in zf.infolist():
            if info.is_dir():
                continue
            name = Path(info.filename).name
            if name not in expected:
                continue
            if name in found:
                raise RuntimeError(f"duplicate ayah in {archive.name}: {name}")
            target = output / name
            with zf.open(info) as src, target.open("wb") as out:
                shutil.copyfileobj(src, out, length=1024 * 1024)
            if target.stat().st_size <= 0:
                raise RuntimeError(f"empty audio file: {name}")
            found.add(name)
    missing = sorted(expected - found)
    if missing:
        raise RuntimeError(f"surah {surah}: missing {len(missing)} canonical files; first={missing[0]}")
    archive.unlink(missing_ok=True)


def verify(output: Path) -> tuple[list[str], str]:
    expected = canonical_names()
    expected_set = set(expected)
    actual = {p.name for p in output.glob("*.mp3")}
    missing = sorted(expected_set - actual)
    extra = sorted(actual - expected_set)
    if missing or extra or len(actual) != EXPECTED_COUNT:
        raise RuntimeError(
            f"audio corpus mismatch: files={len(actual)} expected={EXPECTED_COUNT} "
            f"missing={missing[:3]} extra={extra[:3]}"
        )
    lines: list[str] = []
    manifest_hash = hashlib.sha256()
    for name in expected:
        path = output / name
        if path.stat().st_size <= 0:
            raise RuntimeError(f"empty audio file: {name}")
        digest = sha256(path)
        line = f"{digest}  {name}"
        lines.append(line)
        manifest_hash.update((line + "\n").encode("utf-8"))
    return lines, manifest_hash.hexdigest()


def save_upstream_evidence(output: Path) -> tuple[str, str]:
    """Preserve the provider checksum file byte-for-byte for later provenance audits."""
    target = output / "upstream_checksum.md5"
    download(UPSTREAM_CHECKSUM_URL, target)
    if target.stat().st_size <= 0:
        raise RuntimeError("EveryAyah checksum evidence is empty")
    return target.name, sha256(target)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--verify-only", action="store_true")
    parser.add_argument("--clean", action="store_true", help="delete output before fetching")
    args = parser.parse_args()

    output: Path = args.output
    if args.clean and output.exists() and not args.verify_only:
        shutil.rmtree(output)
    output.mkdir(parents=True, exist_ok=True)

    if not args.verify_only:
        with tempfile.TemporaryDirectory(prefix="quran-hifz-husary-") as temp:
            tmp = Path(temp)
            for surah in range(1, 115):
                names = [output / f"{surah:03d}{ayah:03d}.mp3" for ayah in range(1, AYAHS[surah - 1] + 1)]
                if all(p.is_file() and p.stat().st_size > 0 for p in names):
                    print(f"[{surah:03d}/114] already complete")
                    continue
                fetch_surah(surah, output, tmp)
        checksum_name, checksum_sha256 = save_upstream_evidence(output)
    else:
        checksum_path = output / "upstream_checksum.md5"
        if not checksum_path.is_file() or checksum_path.stat().st_size <= 0:
            raise RuntimeError("missing upstream_checksum.md5 evidence")
        checksum_name, checksum_sha256 = checksum_path.name, sha256(checksum_path)

    lines, aggregate = verify(output)
    (output / "sha256.txt").write_text("\n".join(lines) + "\n", encoding="utf-8")
    source = {
        "schema": 2,
        "reciter": "Mahmoud Khalil Al-Husary — Muʿallim (Hafṣ)",
        "source": "EveryAyah / VerseByVerseQuran · Husary_Muallim_128kbps",
        "landingPage": LANDING_PAGE,
        "sourceDirectory": DIRECTORY_URL,
        "baseUrl": BASE,
        "perSurahZipTemplate": ZIP_TEMPLATE,
        "fullArchiveUrl": FULL_ARCHIVE_URL,
        "upstreamChecksumUrl": UPSTREAM_CHECKSUM_URL,
        "upstreamChecksumFile": checksum_name,
        "upstreamChecksumSha256": checksum_sha256,
        "filePattern": "SSSAAA.mp3",
        "fileCount": EXPECTED_COUNT,
        "sha256Manifest": aggregate,
        "preparedUtc": dt.datetime.now(dt.timezone.utc).replace(microsecond=0).isoformat(),
        "runtimeNetwork": False,
        "delivery": "embedded-in-personal-apk",
        "personalUseOnly": True,
        "redistributionApproved": False,
    }
    (output / "source.json").write_text(json.dumps(source, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"OK: {EXPECTED_COUNT} canonical ayah files")
    print(f"sha256 manifest: {aggregate}")
    print(f"provider checksum evidence sha256: {checksum_sha256}")
    print(f"source directory: {DIRECTORY_URL}")
    print(f"output: {output.resolve()}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
