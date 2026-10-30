#!/usr/bin/env python3
"""Fail closed on 1.17.9 worktree changes outside reviewed 1.17.8 UI adapters.

Historical 1171/1176/1178 guards remain unchanged. This is a distinct, current
baseline check, not an exemption in those older guards and not a runtime test.
"""
from pathlib import Path
import subprocess
import sys

BASE = "ae68ee1a46e195e9da1068aed981f3363869a4e0"
ROOT = Path(__file__).resolve().parents[1]
JAVA = "hifz-app/src/main/java/com/quransafeguard/hifz/preview/"
# Reviewed surface only: all other production files, including persistence,
# migrations, initializers, schedulers, geometry and Quran assets, stay byte exact.
ADAPTERS = {
    JAVA + name + ".java" for name in (
        "ActiveReviewCorrectionPlan", "ActiveReviewCorrectionStore",
        "AtlasDiscriminantActivity", "AtlasDiscriminantCases", "AtlasDiscriminantMarkers",
        "HifzSessionActivity", "MapQcfThumbnails", "MawdudiMapScreen", "MushafView",
        "QcfAtlasVerseStrips", "QcfExcerptWindow", "RevisionAtlasAccess",
        "SettingsActivity", "StudyReaderActivity", "Ui",
    )
}
RESOURCES = {
    "hifz-app/src/main/AndroidManifest.xml",
    "hifz-app/src/main/res/drawable/ic_ui_atlas_similar.xml",
}
SCOPES = ("hifz-core/src", "hifz-app/src/main", "hifz-app/src/private114")
BUILD = "hifz-app/build.gradle.kts"


def git(*args):
    return subprocess.check_output(["git", *args], cwd=ROOT)


def verify(root=ROOT):
    """Compare file contents, including untracked additions, to the pinned base."""
    baseline = {
        p for p in git("ls-tree", "-r", "--name-only", BASE, "--", *SCOPES)
        .decode().splitlines()
    }
    actual = {
        str(p.relative_to(root))
        for scope in SCOPES
        for p in (root / scope).rglob("*")
        if p.is_file()
    }
    errors = []
    changed = []
    for path in sorted(baseline | actual):
        old = git("show", BASE + ":" + path) if path in baseline else None
        new = (root / path).read_bytes() if path in actual else None
        if old == new:
            continue
        changed.append(path)
        if path not in ADAPTERS | RESOURCES:
            errors.append("Protected production file differs from 1.17.8: " + path)
    # Official identity/signing/private SHA guards cannot change under a version bump.
    old_build = git("show", BASE + ":" + BUILD)
    expected = old_build.replace(b'versionCode = 46', b'versionCode = 47').replace(
        b'versionName = "1.17.8"', b'versionName = "1.17.9"'
    )
    if (root / BUILD).read_bytes() != expected:
        errors.append("Build differs beyond exact 1.17.9/code47 metadata update")
    return changed, errors


def main():
    changed, errors = verify()
    for error in errors:
        print("FAIL: " + error, file=sys.stderr)
    if errors:
        return 1
    print("PASS: all production files outside the explicit 1.17.9 adapter/resource list are byte-identical to " + BASE)
    print("PASS: exact version47/1.17.9 metadata; official package, signer and seven private asset SHA guards unchanged")
    print("REVIEWED ADAPTER/RESOURCE DELTAS: " + ", ".join(changed))
    print("LIMIT: adapter allowlisting is not runtime or signed-upgrade evidence; run fresh JVM/Android and physical upgrade checks")
    return 0


if __name__ == "__main__":
    sys.exit(main())
