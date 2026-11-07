#!/usr/bin/env python3
"""Synthetic, public-safe tests of the 1.17.14 offline private delivery guard.

No original Quran private assets, keystore, signed APK or user data are needed.
"""
import importlib.util
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import zipfile

SCRIPT = Path(__file__).with_name("build_11714_private_offline.py")
spec = importlib.util.spec_from_file_location("haafidh11714_offline", SCRIPT)
builder = importlib.util.module_from_spec(spec)
spec.loader.exec_module(builder)


def zip_fixture(path: Path) -> None:
    with zipfile.ZipFile(path, "w") as z:
        z.writestr("AndroidManifest.xml", "fake manifest for parser-stub test")
        z.writestr("classes.dex", b"fake dex")


class PrivateCandidateDeliveryGuardTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.original = self.root / "previous.apk"
        zip_fixture(self.original)

    def test_accepts_only_authentic_old_version_and_matching_signer(self):
        for version in ((42, "1.17.4"), (46, "1.17.8"), (47, "1.17.9"), (48, "1.17.10"), (49, "1.17.11"), (50, "1.17.12"), (51, "1.17.13")):
            with self.subTest(version=version):
                with patch.object(builder, "package_identity",
                                  return_value=(builder.PACKAGE, *version)), \
                     patch.object(builder, "package_certificate",
                                  return_value=builder.HISTORICAL_CERT):
                    info = builder.check_reference(
                        Path("/aapt"), Path("/apksigner"), self.original
                    )
                    self.assertEqual(info["versionCode"], version[0])

    def test_rejects_wrong_reference_version(self):
        for version in ((45, "1.17.7"), (50, "1.17.11"), (42, "1.17.8")):
            with patch.object(builder, "package_identity",
                              return_value=(builder.PACKAGE, *version)):
                with self.assertRaisesRegex(SystemExit, "ni 1.17.4"):
                    builder.check_reference(
                        Path("/aapt"), Path("/apksigner"), self.original
                    )

    def test_rejects_wrong_reference_package(self):
        with patch.object(builder, "package_identity",
                          return_value=(builder.PACKAGE + ".debug", 42, "1.17.4")):
            with self.assertRaisesRegex(SystemExit, "APK historique"):
                builder.check_reference(Path("/aapt"), Path("/apksigner"), self.original)

    def test_rejects_wrong_historical_signer(self):
        with patch.object(builder, "package_identity",
                          return_value=(builder.PACKAGE, 42, "1.17.4")), \
             patch.object(builder, "package_certificate", return_value="0" * 64):
            with self.assertRaisesRegex(SystemExit, "signature de l'APK historique"):
                builder.check_reference(Path("/aapt"), Path("/apksigner"), self.original)

    def test_rejects_private_apk_output_inside_public_git_checkout(self):
        with self.assertRaisesRegex(SystemExit, "dépôt Git public"):
            builder.assert_outside_public_checkout(
                self.root, self.root / "signed-release.apk"
            )

    def test_rejects_existing_private_output(self):
        candidate = self.root.parent / "existing-private-test.apk"
        # Create a separate nested directory not under the checked source.
        source = self.root / "source"
        source.mkdir()
        candidate = self.root / "signed-private.apk"
        candidate.write_bytes(b"do not overwrite")
        with self.assertRaisesRegex(SystemExit, "aucun écrasement"):
            builder.assert_outside_public_checkout(source, candidate)

    def test_rejects_non_v2_cert_signature(self):
        with patch.object(builder, "command", return_value=(
            "Signer #1 certificate SHA-256 digest: " + builder.HISTORICAL_CERT + "\n"
            "Verified using v2 scheme (APK Signature Scheme v2): false\n"
        )):
            with self.assertRaisesRegex(SystemExit, "signature APK v2 absente"):
                builder.package_certificate(Path("/apksigner"), self.original)

    def test_rejects_missing_source_sha(self):
        with self.assertRaisesRegex(SystemExit, "SHA du commit certifié invalide"):
            builder.check_source(self.root, "not-a-real-sha")

    def test_exact_upgraded_identity(self):
        self.assertEqual((builder.PACKAGE,builder.VERSION_CODE,builder.VERSION),
                         ("com.quransafeguard.hifz",52,"1.17.14"))
        self.assertEqual(len(builder.RESOURCES),7)
        self.assertEqual(len(builder.HISTORICAL_CERT),64)

    def test_public_script_never_performs_install_or_public_upload(self):
        text = SCRIPT.read_text(encoding="utf-8")
        self.assertIn("GITHUB_ACTIONS", text)
        self.assertIn("HIFZ_PRIVATE_RELEASE_UPGRADE", text)
        self.assertIn("check_candidate", text)
        self.assertNotIn('["adb", "install"', text)
        self.assertNotIn("gh release", text)


if __name__ == "__main__":
    unittest.main()
