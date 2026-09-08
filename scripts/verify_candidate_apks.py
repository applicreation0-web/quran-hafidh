#!/usr/bin/env python3
"""Behavior-independent evidence extracted from the actual candidate APK bytes."""
import json
import re
import subprocess
import sys
import zipfile
from pathlib import Path

if len(sys.argv) != 3:
    raise SystemExit("usage: verify_candidate_apks.py LIGHT.apk PLUS.apk")

apks = {"light": Path(sys.argv[1]), "plus": Path(sys.argv[2])}
expected_ids = {
    "light": "com.applicreation0.quransafeguard",
    "plus": "com.applicreation0.quransafeguard.plus",
}
forbidden_permissions = {
    "android.permission.INTERNET",
    "android.permission.RECORD_AUDIO",
    "android.permission.BLUETOOTH",
    "android.permission.BLUETOOTH_CONNECT",
    "android.permission.GET_ACCOUNTS",
    "android.permission.READ_CALL_LOG",
    "android.permission.READ_PHONE_STATE",
}
report = {}

for edition, apk in apks.items():
    assert apk.is_file() and apk.stat().st_size > 0, apk
    badging = subprocess.check_output(["aapt", "dump", "badging", str(apk)], text=True)
    permissions_dump = subprocess.check_output(
        ["aapt", "dump", "permissions", str(apk)], text=True
    )
    match = re.search(
        r"package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'",
        badging,
    )
    assert match, "package metadata missing"
    app_id, version_code, version_name = match.groups()
    assert app_id == expected_ids[edition]
    assert version_code == "28"
    assert version_name.startswith("0.10.9")
    present_permissions = set(re.findall(r"name='([^']+)'", permissions_dump))
    assert not (present_permissions & forbidden_permissions), (
        edition, present_permissions & forbidden_permissions
    )

    with zipfile.ZipFile(apk) as archive:
        names = set(archive.namelist())
        pages = [
            n for n in names
            if re.fullmatch(r"assets/mushaf/hafs/kfqc/svg-br/\d{3}\.svg\.br", n)
        ]
        assert len(pages) == 604
        for required in (
            "assets/reader109/index.html",
            "assets/reader109/protocol.js",
            "assets/reader109/reader.js",
            "assets/reader109/geometry.json",
            "assets/reader109/audio.json",
        ):
            assert required in names, (edition, required)
        tafsir_assets = [n for n in names if n.startswith("assets/tafsir/")]
        if edition == "light":
            assert not tafsir_assets
        else:
            assert tafsir_assets
        dex = b"".join(
            archive.read(n) for n in sorted(names)
            if re.fullmatch(r"classes\d*\.dex", n)
        )
        assert b"DisplayProfile" in dex
        assert b"EInkRefreshController" in dex
        audio_gate = json.loads(archive.read("assets/reader109/audio.json"))
        assert audio_gate.get("redistributionApproved") is False

    report[edition] = {
        "apk": str(apk),
        "applicationId": app_id,
        "versionCode": int(version_code),
        "versionName": version_name,
        "permissions": sorted(present_permissions),
        "mushafPages": len(pages),
        "tafsirAssets": len(tafsir_assets),
        "audioRedistributionApproved": False,
    }

print(json.dumps(report, indent=2, ensure_ascii=False))
