#!/usr/bin/env python3
"""Fail-closed offline 1.17.1 -> 1.17.2 identity upgrade contract.

Synthetic APKs and stubbed aapt/apksigner output; NEVER accesses private keys
or private editorial content. Real signed-APK/BOOX verification is separate.
"""
import importlib.util
from pathlib import Path
import tempfile
from unittest import TestCase, main
from unittest.mock import patch
import zipfile

_SCRIPT = Path(__file__).with_name("assemble_1172_private_candidate_offline.py")
_SPEC = importlib.util.spec_from_file_location("haafidh1172_offline_finalizer", _SCRIPT)
finalizer = importlib.util.module_from_spec(_SPEC)
_SPEC.loader.exec_module(finalizer)


def mk_apk(path):
    with zipfile.ZipFile(path, "w") as out:
        out.writestr("AndroidManifest.xml", b"binary manifest fixture")
        out.writestr("classes.dex", b"DEX fixture")


class InPlaceUpdateIdentityTests(TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        root = Path(self.tmp.name)
        self.old = root / "private-1171.apk"
        self.new = root / "private-1172-staging.apk"
        mk_apk(self.old)
        mk_apk(self.new)
        self.old_package = finalizer.APP_ID
        self.old_code = 39
        self.old_name = finalizer.PREVIOUS_VERSION_NAME
        self.new_package = finalizer.APP_ID
        self.new_code = 40
        self.new_name = finalizer.VERSION_NAME
        self.cert = finalizer.EXPECTED_CERT

    def stub_tool(self, argv, **ignored):
        name = Path(argv[0]).name
        apk = Path(argv[-1])
        if name == "aapt":
            if apk == self.old:
                return (f"package: name='{self.old_package}' versionCode='{self.old_code}' "
                        f"versionName='{self.old_name}'\n")
            return (f"package: name='{self.new_package}' versionCode='{self.new_code}' "
                    f"versionName='{self.new_name}'\n")
        if name == "apksigner":
            return "Signer #1 certificate SHA-256 digest: " + self.cert + "\n"
        raise AssertionError("unexpected tool " + name)

    def verify(self):
        with patch.object(finalizer, "run", side_effect=self.stub_tool):
            return finalizer.verify_upgrade_path(
                self.old, self.new, Path("/tools/aapt"), Path("/tools/apksigner"))

    def test_accepts_same_package_same_certificate_and_monotonic_code(self):
        self.assertEqual(self.verify(), finalizer.sha(self.old.read_bytes()))

    def test_rejects_other_signing_certificate(self):
        self.cert = "0" * 64
        with self.assertRaisesRegex(SystemExit, "NO-GO: old APK signing certificate"):
            self.verify()

    def test_rejects_different_installed_package(self):
        self.old_package += ".anotherapp"
        with self.assertRaisesRegex(SystemExit, "NO-GO: reference APK is not"):
            self.verify()

    def test_rejects_different_staging_package(self):
        self.new_package += ".test"
        with self.assertRaisesRegex(SystemExit, "NO-GO: staging APK package/version"):
            self.verify()

    def test_rejects_equal_or_lower_version_code(self):
        self.new_code = 39
        with self.assertRaisesRegex(SystemExit, "NO-GO: staging APK package/version"):
            self.verify()

    def test_rejects_wrong_installed_version(self):
        self.old_name = "1.17.0-AyahThemes114-PRIVATE"
        with self.assertRaisesRegex(SystemExit, "NO-GO: reference APK is not"):
            self.verify()

    def test_rejects_missing_reference(self):
        self.old.unlink()
        with self.assertRaisesRegex(SystemExit, "NO-GO: real signed 1.17.1 reference"):
            self.verify()


if __name__ == "__main__":
    main()
