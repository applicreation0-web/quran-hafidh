#!/usr/bin/env python3
"""Private 1.17.7 BOOX official in-place candidate finalization, ONLY on a trusted offline machine.

Public-safe code: there are NO private assets, keystores, passwords or APK bytes in Git.
Preconditions: GitHub staging APK from the exact 1.17.7 code, seven original private
files with approved content hashes, Android SDK build-tools 36, historical keystore.
Never call on public CI; never publish the resulting candidate automatically.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import zipfile

EXPECTED_CERT = "66adc4f0dcf70d716a8050fd4b0195266307576aa2f9bd54c9cbf2851a330e14"
APP_ID = "com.quransafeguard.hifz"
VERSION_CODE = "45"
PREVIOUS_VERSION_CODE = 44
PREVIOUS_VERSION_NAME = "1.17.6"
VERSION_NAME = "1.17.7"
EXPECTED_ASSETS = {
    "atlas_private/atlas_366_comments_offline_reference_v1.json":
        "8f514ffa367d67a49198799be279691ba0555363fac0b030f09fbf39fe3c3484",
    "atlas_private/maududi_original.pdf":
        "1d207bb3dd58d45e7a77b0b5942f88a5c216b3a4273ea5a4474fc9e140b0d62e",
    "atlas_private/atlas_approved_unit_titles_366_v1.json":
        "421277c59519f2b904de23bbc1e0eee6faeb569e33ed6b425b4b9235cf82ff79",
    "atlas/atlas_display_catalog_v1.json":
        "065ec0dd68c488efacbd9e77635bf9d531b4cbb2b1bc82db7853ad4da0c02c9c",
    "ayah_themes_private/titles_114.json":
        "a42914ae968c77bc7224f5126a41cc3e92099fbc797a91b01d74405d242eb209",
    "ayah_themes_private/macros_114.json":
        "3589af556f82e55e1619fc0f3ae16b7f21b4609f156a8ad808a3a0bbe6d3c09a",
    "ayah_themes_private/special_27.json":
        "d3fb2a7cc23f84851f83caa14ab23a549b8f410529930131dd2c2560d7450404",
}

def die(reason: str) -> None:
    raise SystemExit("NO-GO: " + reason)

def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()

def run(args: list[str], **kwargs) -> str:
    result = subprocess.run(args, text=True, stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE, check=False, **kwargs)
    if result.returncode:
        # Do not echo environment, command-line secret values, or private file contents.
        die("Android signing/tool step failed: " + Path(args[0]).name +
            " (exit=" + str(result.returncode) + ")")
    return result.stdout + result.stderr

def check_assets(folder: Path) -> dict[str, bytes]:
    if not folder.is_dir():
        die("private source directory does not exist")
    found = {}
    for name, digest in EXPECTED_ASSETS.items():
        p = folder / name
        if not p.is_file() or p.is_symlink():
            die("missing/non-regular approved private asset: " + name)
        raw = p.read_bytes()
        if sha(raw) != digest:
            die("private SHA-256 mismatch: " + name)
        if name.endswith(".json"):
            try:
                obj = json.loads(raw.decode("utf-8"))
            except (ValueError, UnicodeError):
                die("invalid private UTF-8 JSON: " + name)
            if name.endswith("atlas_366_comments_offline_reference_v1.json"):
                if obj.get("unit_count") != 366 or len(obj.get("units", [])) != 366:
                    die("Atlas index does not contain 366 units")
            if name.endswith("atlas_display_catalog_v1.json"):
                if not isinstance(obj, dict):
                    die("Atlas editorial catalogue must be a JSON object")
        elif not raw.startswith(b"%PDF-"):
            die("PDF asset does not start with a PDF header")
        found[name] = raw
    return found

def check_staging(p: Path) -> None:
    if not p.is_file() or not zipfile.is_zipfile(p):
        die("no valid staging APK provided")
    with zipfile.ZipFile(p) as z:
        names = set(z.namelist())
        if "AndroidManifest.xml" not in names or "classes.dex" not in names:
            die("staging is not a recognizable Android package")
        if any("assets/" + name in names for name in EXPECTED_ASSETS):
            die("staging already contains private assets; refuse double injection")
        if any(n.startswith(("assets/atlas_private/", "assets/ayah_themes_private/"))
               for n in names):
            die("staging unexpectedly already contains a private asset tree")
        if z.testzip() is not None:
            die("staging APK ZIP CRC check failed")

def parse_apk_package(apk: Path, aapt: Path) -> tuple[str, int, str]:
    """Read the signed/unsigned Android manifest, not a guessed APK filename."""
    out = run([str(aapt), "dump", "badging", str(apk)])
    m = re.search(r"^package: name='([^']+)' versionCode='([0-9]+)' versionName='([^']+)'",
                  out, re.MULTILINE)
    if not m:
        die("cannot read Android package identity: " + apk.name)
    return m.group(1), int(m.group(2)), m.group(3)


def signed_certificate(apk: Path, apksigner: Path) -> str:
    out = run([str(apksigner), "verify", "--verbose", "--print-certs", str(apk)])
    m = re.search(r"Signer #1 certificate SHA-256 digest:\s*([0-9a-fA-F:]+)", out)
    if not m:
        die("cannot verify the existing APK signing certificate")
    return m.group(1).lower().replace(":", "")


def verify_upgrade_path(previous: Path, staging: Path,
                        aapt: Path, apksigner: Path) -> str:
    """Fail before signing unless a real v1.17.1 APK can be upgraded in place.

    A reference APK's signature proves the expected Android identity; the BOOX
    device must still be checked before its actual in-place installation.
    """
    if not previous.is_file() or not zipfile.is_zipfile(previous):
        die("real signed 1.17.1 reference APK required for an in-place update")
    old_id, old_code, old_name = parse_apk_package(previous, aapt)
    if (old_id, old_code, old_name) != (
        APP_ID, PREVIOUS_VERSION_CODE, PREVIOUS_VERSION_NAME
    ):
        die("reference APK is not the expected installed official 1.17.6 app")
    if signed_certificate(previous, apksigner) != EXPECTED_CERT:
        die("old APK signing certificate differs; refusing a destructive reinstall")

    new_id, new_code, new_name = parse_apk_package(staging, aapt)
    if (new_id, new_code, new_name) != (APP_ID, int(VERSION_CODE), VERSION_NAME):
        die("staging APK package/version differs from approved 1.17.7 update")
    if new_code <= old_code:
        die("APK downgrade or same-version update: versionCode must increase")
    return sha(previous.read_bytes())


def build_unsigned(staging: Path, unsigned: Path, assets: dict[str, bytes]) -> None:
    # Rewrite rather than append: old APK signing blocks and JAR signature entries
    # must not survive an asset-level modification.
    with zipfile.ZipFile(staging) as old, zipfile.ZipFile(unsigned, "w", allowZip64=True) as new:
        for info in old.infolist():
            n = info.filename
            if re.match(r"(?i)^META-INF/[^/]+\\.(RSA|DSA|EC|SF|MF)$", n):
                continue
            if n.startswith(("assets/atlas_private/", "assets/ayah_themes_private/")):
                die("unexpected private material in staging")
            new.writestr(info, old.read(info))
        for name, data in assets.items():
            new.writestr("assets/" + name, data, compress_type=zipfile.ZIP_DEFLATED)
    with zipfile.ZipFile(unsigned) as z:
        for name, data in assets.items():
            if sha(z.read("assets/" + name)) != sha(data):
                die("private asset altered during assembly: " + name)

def verify_signed(apk: Path, aapt: Path, apksigner: Path, expected_cert: str) -> None:
    out = run([str(apksigner), "verify", "--verbose", "--print-certs", str(apk)])
    match = re.search(r"Signer #1 certificate SHA-256 digest:\s*([0-9a-fA-F:]+)", out)
    if not match or match.group(1).lower().replace(":", "") != expected_cert:
        die("signer certificate is NOT the installed BOOX identity")
    meta = run([str(aapt), "dump", "badging", str(apk)])
    pkg = re.search(r"^package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'",
                    meta, re.MULTILINE)
    if not pkg or pkg.groups() != (APP_ID, VERSION_CODE, VERSION_NAME):
        die("APK identifier/version differs from the intended in-place BOOX update")
    with zipfile.ZipFile(apk) as z:
        for name, digest in EXPECTED_ASSETS.items():
            if sha(z.read("assets/" + name)) != digest:
                die("signed candidate private data mismatch: " + name)

def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--staging-apk", type=Path, required=True)
    parser.add_argument("--private-assets", type=Path, required=True,
                        help="root containing atlas/, atlas_private/, ayah_themes_private/")
    parser.add_argument("--build-tools", type=Path, required=True,
                        help="Android SDK build-tools/36.0.0 directory")
    parser.add_argument("--reference-1176-apk", type=Path, required=True,
                        help="actual signed private 1.17.1 APK matching the BOOX installation")
    parser.add_argument("--keystore", type=Path, required=True)
    parser.add_argument("--alias", required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--expected-cert", default=EXPECTED_CERT)
    args = parser.parse_args()

    if (len(args.expected_cert) != 64 or
        not re.fullmatch(r"[0-9a-fA-F]{64}", args.expected_cert)):
        die("expected certificate must be a SHA-256 fingerprint")
    if args.expected_cert.lower() != EXPECTED_CERT:
        die("certificate override disallowed for this known in-place update")
    for key in ("HAAFIDH_KS_PASS", "HAAFIDH_KEY_PASS"):
        if not os.environ.get(key):
            die("missing offline signing environment variable " + key)
    if not args.keystore.is_file() or not args.alias:
        die("historical keystore/alias missing")
    for tool in ("zipalign", "apksigner", "aapt"):
        if not (args.build_tools / tool).is_file():
            die("Android build-tool missing: " + tool)
    if args.output.exists() or args.output.name.endswith(".zip"):
        die("output must be a fresh APK path")
    check_staging(args.staging_apk)
    reference_sha = verify_upgrade_path(args.reference_1176_apk, args.staging_apk,
                                        args.build_tools / "aapt",
                                        args.build_tools / "apksigner")
    assets = check_assets(args.private_assets)

    args.output.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="haafidh1177_offline_") as tmp:
        unsigned = Path(tmp) / "candidate-unsigned.apk"
        aligned = Path(tmp) / "candidate-aligned.apk"
        signed = Path(tmp) / "candidate-signed.apk"
        build_unsigned(args.staging_apk, unsigned, assets)
        run([str(args.build_tools / "zipalign"), "-f", "4", str(unsigned), str(aligned)])
        run([str(args.build_tools / "zipalign"), "-c", "4", str(aligned)])
        run([str(args.build_tools / "apksigner"), "sign",
             "--ks", str(args.keystore), "--ks-key-alias", args.alias,
             "--ks-pass", "env:HAAFIDH_KS_PASS",
             "--key-pass", "env:HAAFIDH_KEY_PASS",
             "--out", str(signed), str(aligned)])
        verify_signed(signed, args.build_tools / "aapt",
                      args.build_tools / "apksigner", args.expected_cert.lower())
        final_sha = sha(signed.read_bytes())
        signed.replace(args.output)
    report = {
        "status": "SIGNED_PRIVATE_CANDIDATE_NOT_BOOX_VALIDATED",
        "source_app": APP_ID,
        "versionCode": int(VERSION_CODE),
        "versionName": VERSION_NAME,
        "signer_sha256": EXPECTED_CERT,
        "apk_sha256": final_sha,
        "reference_1176_apk_sha256": reference_sha,
        "reference_1176_versionCode": PREVIOUS_VERSION_CODE,
        "upgrade_package_and_signer_match_reference": True,
        "apk_size_bytes": args.output.stat().st_size,
        "private_asset_sha256": EXPECTED_ASSETS,
        "boox_recette": "NON_TESTEE",
        "in_place_update_and_data_preservation": "NON_TESTEES_SUR_BOOX",
        "installation_instruction": "Sauvegarder; installer avec adb install -r (sans desinstaller); controler acquis",
        "publication_authorized": False,
    }
    report_path = args.output.with_suffix(".verification.json")
    if report_path.exists():
        die("existing verification report; refuse silent overwrite")
    report_path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n",
                           encoding="utf-8")
    print("PASS: signed private candidate verified OFFLINE; no official release.")
    print("APK SHA256: " + final_sha)
    print("APK: " + str(args.output))
    print("Proof: " + str(report_path))

if __name__ == "__main__":
    main()
