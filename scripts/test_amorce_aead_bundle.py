#!/usr/bin/env python3
"""Contract tests for the public authenticated-encrypted Amorces bundle."""

from __future__ import annotations

import hashlib
import json
import subprocess
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
TOOL = ROOT / "tools/amorce-aead/AmorceAeadTool.java"
BUNDLE_DIR = ROOT / "secure-corpus/ibn-kathir-amorces-v1"
CIPHERTEXT = BUNDLE_DIR / "amorces-m7b-1903.qhaead"
MANIFEST = BUNDLE_DIR / "manifest.json"


class AmorceAeadBundleTest(unittest.TestCase):
    def run_tool(self, *args: object, check: bool = True) -> subprocess.CompletedProcess[str]:
        self.assertTrue(TOOL.is_file(), f"missing AEAD tool: {TOOL}")
        return subprocess.run(
            ["java", str(TOOL), *(str(arg) for arg in args)],
            cwd=ROOT,
            check=check,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )

    def test_round_trip_and_tamper_rejection(self) -> None:
        with tempfile.TemporaryDirectory() as raw:
            tmp = Path(raw)
            key = tmp / "private.key"
            source = tmp / "source.zip"
            encrypted = tmp / "source.qhaead"
            restored = tmp / "restored.zip"
            source.write_bytes(b"PK\x03\x04fixture-only-not-private")

            self.run_tool("keygen", key)
            self.assertEqual(32, key.stat().st_size)
            self.run_tool("encrypt", key, source, encrypted)
            original_container = encrypted.read_bytes()
            self.assertEqual(b"QHAEAD01", original_container[:8])
            self.run_tool("decrypt", key, encrypted, restored)
            self.assertEqual(source.read_bytes(), restored.read_bytes())

            restored.unlink()
            tampered = bytearray(original_container)
            tampered[-1] ^= 1
            encrypted.write_bytes(tampered)
            failed = self.run_tool("decrypt", key, encrypted, restored, check=False)
            self.assertNotEqual(0, failed.returncode)
            self.assertIn("authentication failed", failed.stderr)
            self.assertFalse(restored.exists())

            wrong_key = tmp / "wrong.key"
            encrypted.write_bytes(original_container)
            self.run_tool("keygen", wrong_key)
            failed = self.run_tool("decrypt", wrong_key, encrypted, restored, check=False)
            self.assertNotEqual(0, failed.returncode)
            self.assertIn("authentication failed", failed.stderr)
            self.assertFalse(restored.exists())

    def test_repository_bundle_manifest_is_source_safe(self) -> None:
        self.assertTrue(CIPHERTEXT.is_file(), "encrypted corpus is missing")
        self.assertTrue(MANIFEST.is_file(), "public manifest is missing")
        manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
        self.assertEqual("QH_AMORCES_AEAD_V1", manifest["schema"])
        self.assertEqual("AES-256-GCM", manifest["cipher"])
        self.assertEqual(1903, manifest["total_groups"])
        self.assertEqual(1708, manifest["titles"])
        self.assertEqual(195, manifest["untitled"])
        self.assertEqual(8, manifest["chunks"])
        self.assertEqual(
            hashlib.sha256(CIPHERTEXT.read_bytes()).hexdigest(),
            manifest["ciphertext_sha256"],
        )
        self.assertNotIn("key", " ".join(manifest.keys()).lower())
        self.assertGreater(CIPHERTEXT.stat().st_size, 1_000)


if __name__ == "__main__":
    unittest.main()
