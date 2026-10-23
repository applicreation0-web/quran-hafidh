#!/usr/bin/env python3
"""Fail-closed preflight for updating the OFFICIAL 1.17.1 release in-place.

No private keystore, password, APK or copyrighted private assets are uploaded by
this script. It only inspects the two local files.
"""
import argparse
import hashlib
import re
import subprocess
import sys
import zipfile

OFFICIAL_PACKAGE = "com.quransafeguard.hifz"
OFFICIAL_1171_CERT_SHA256 = "66adc4f0dcf70d716a8050fd4b0195266307576aa2f9bd54c9cbf2851a330e14"
PRIVATE_ASSETS = {
    "assets/atlas/atlas_display_catalog_v1.json": "065ec0dd68c488efacbd9e77635bf9d531b4cbb2b1bc82db7853ad4da0c02c9c",
    "assets/atlas_private/atlas_366_comments_offline_reference_v1.json": "8f514ffa367d67a49198799be279691ba0555363fac0b030f09fbf39fe3c3484",
    "assets/atlas_private/atlas_approved_unit_titles_366_v1.json": "421277c59519f2b904de23bbc1e0eee6faeb569e33ed6b425b4b9235cf82ff79",
    "assets/atlas_private/maududi_original.pdf": "1d207bb3dd58d45e7a77b0b5942f88a5c216b3a4273ea5a4474fc9e140b0d62e",
    "assets/ayah_themes_private/macros_114.json": "3589af556f82e55e1619fc0f3ae16b7f21b4609f156a8ad808a3a0bbe6d3c09a",
    "assets/ayah_themes_private/special_27.json": "d3fb2a7cc23f84851f83caa14ab23a549b8f410529930131dd2c2560d7450404",
    "assets/ayah_themes_private/titles_114.json": "a42914ae968c77bc7224f5126a41cc3e92099fbc797a91b01d74405d242eb209",
}
def execute(*args):
    return subprocess.check_output(args, text=True, stderr=subprocess.STDOUT)

def metadata(apk, aapt):
    text = execute(aapt, "dump", "badging", apk)
    m = re.search(r"^package: name='([^']+)' versionCode='(\\d+)' versionName='([^']+)'", text, re.M)
    if not m:
        raise ValueError("Cannot read Android package identity: " + apk)
    return m.group(1), int(m.group(2)), m.group(3)

def signature(apk, apksigner_jar):
    out = execute("java", "-jar", apksigner_jar, "verify", "--verbose", "--print-certs", apk)
    m = re.search(r"Signer #1 certificate SHA-256 digest: ([0-9a-f]{64})", out)
    if not m:
        raise ValueError("No verified APK certificate: " + apk)
    if "Verified using v2 scheme (APK Signature Scheme v2): true" not in out:
        raise ValueError("APK v2 signature missing")
    if "Verified using v3 scheme (APK Signature Scheme v3): true" not in out:
        raise ValueError("APK v3 signature missing")
    return m.group(1)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--installed", required=True, help="Exact OFFICIAL 1.17.1 release APK, never a debug preview")
    parser.add_argument("--candidate", required=True, help="Signed 1.17.4 official upgrade APK")
    parser.add_argument("--aapt", required=True)
    parser.add_argument("--apksigner-jar", required=True)
    a = parser.parse_args()
    old_package, old_version, old_name = metadata(a.installed, a.aapt)
    new_package, new_version, new_name = metadata(a.candidate, a.aapt)
    old_cert = signature(a.installed, a.apksigner_jar)
    new_cert = signature(a.candidate, a.apksigner_jar)
    checks = {
        "official baseline package": old_package == OFFICIAL_PACKAGE,
        "official baseline signing cert": old_cert == OFFICIAL_1171_CERT_SHA256,
        "same package for in-place install": new_package == old_package == OFFICIAL_PACKAGE,
        "same signer for in-place install": new_cert == old_cert == OFFICIAL_1171_CERT_SHA256,
        "version code increases": new_version > old_version,
        "expected target version": new_name == "1.17.4",
    }
    with zipfile.ZipFile(a.candidate) as archive:
        for path, expected in PRIVATE_ASSETS.items():
            try:
                actual = hashlib.sha256(archive.read(path)).hexdigest()
                checks["private asset: " + path] = actual == expected
            except KeyError:
                checks["private asset: " + path] = False
    for label, valid in checks.items():
        print(("PASS" if valid else "NO-GO") + ": " + label)
    print("Baseline:", old_name, old_version)
    print("Candidate:", new_name, new_version)
    if not all(checks.values()):
        print("DO NOT INSTALL. Do not uninstall the original app.")
        return 1
    print("APPROVED FOR IN-PLACE BOOX INSTALLATION TEST (physical-device backup still required)")
    return 0

if __name__ == "__main__":
    try:
        sys.exit(main())
    except (ValueError, subprocess.CalledProcessError, zipfile.BadZipFile, OSError) as e:
        print("NO-GO:", str(e), file=sys.stderr)
        sys.exit(1)
