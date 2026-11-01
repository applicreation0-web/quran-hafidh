#!/usr/bin/env python3
"""Fail-closed LOCAL/OFFLINE private 1.17.9 signed candidate builder.

Run on a trusted machine with the original seven rights-scoped files, the
historical signing key, and a signed reference APK. NEVER run on public GitHub
Actions, upload an APK or key, install on the BOOX, or erase application data.
This script is an offline delivery gate, not a claim that a candidate exists.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import zipfile

PACKAGE = "com.quransafeguard.hifz"
VERSION = "1.17.9"
VERSION_CODE = 47
HISTORICAL_CERT = "66adc4f0dcf70d716a8050fd4b0195266307576aa2f9bd54c9cbf2851a330e14"
ALLOWED_INSTALLED = {(42, "1.17.4"), (46, "1.17.8")}
RESOURCES = {
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


def nogo(reason: str) -> None:
    raise SystemExit("NO-GO (aucun APK livré) : " + reason)


def digest(raw: bytes) -> str:
    return hashlib.sha256(raw).hexdigest()


def command(args: list[str], *, env: dict[str, str] | None = None) -> str:
    result = subprocess.run(args, text=True, capture_output=True, env=env, check=False)
    if result.returncode != 0:
        # Never print external command output: paths, signing data or assets may
        # be embedded in Gradle/aapt stderr in the trusted private environment.
        nogo("commande locale échouée : " + Path(args[0]).name
             + " (code " + str(result.returncode) + ")")
    return result.stdout + result.stderr


def assert_outside_public_checkout(source: Path, output: Path) -> None:
    candidate = output.resolve()
    try:
        candidate.relative_to(source.resolve())
        nogo("le fichier de sortie est dans le dépôt Git public")
    except ValueError:
        pass
    if output.exists() or output.with_suffix(".verification.json").exists():
        nogo("sortie ou attestation existante : aucun écrasement autorisé")


def check_source(source: Path, expected_sha: str) -> None:
    if not re.fullmatch(r"[0-9a-f]{40}", expected_sha):
        nogo("SHA du commit certifié invalide")
    if command(["git", "-C", str(source), "rev-parse", "HEAD"]).strip() != expected_sha:
        nogo("le checkout ne correspond pas exactement au commit certifié")
    if command(["git", "-C", str(source), "status", "--porcelain"]).strip():
        nogo("checkout modifié ou fichiers non versionnés : refus de signer")
    build = (source / "hifz-app/build.gradle.kts").read_text(encoding="utf-8")
    if ('applicationId = "' + PACKAGE + '"') not in build or (
        "versionCode = 47" not in build or 'versionName = "1.17.9"' not in build
    ):
        nogo("package ou version Android non conformes")


def check_assets(source: Path) -> None:
    folder = source / "hifz-app/src/private114/assets"
    for name, expected in RESOURCES.items():
        path = folder / name
        if not path.is_file() or path.is_symlink():
            nogo("ressource privée manquante ou lien symbolique : " + name)
        raw = path.read_bytes()
        if digest(raw) != expected:
            nogo("SHA-256 incorrect : " + name)
        if name.endswith(".json"):
            try:
                json.loads(raw.decode("utf-8"))
            except (UnicodeError, ValueError):
                nogo("JSON privé invalide : " + name)
        elif not raw.startswith(b"%PDF-"):
            nogo("PDF privé invalide : " + name)
    # Public source plus local audited assets: never send this directory to CI.


def package_identity(aapt: Path, apk: Path) -> tuple[str, int, str]:
    out = command([str(aapt), "dump", "badging", str(apk)])
    found = re.search(
        r"^package: name='([^']+)' versionCode='([0-9]+)' versionName='([^']+)'",
        out, re.MULTILINE
    )
    if found is None:
        nogo("identité de l'APK illisible")
    return found.group(1), int(found.group(2)), found.group(3)


def package_certificate(apksigner: Path, apk: Path) -> str:
    out = command([str(apksigner), "verify", "--verbose", "--print-certs", str(apk)])
    signer = re.search(r"Signer #1 certificate SHA-256 digest:\s*([0-9a-fA-F:]+)", out)
    if signer is None:
        nogo("certificat APK absent")
    if re.search(r"Verified using v2 scheme \(APK Signature Scheme v2\): true", out) is None:
        nogo("signature APK v2 absente")
    return signer.group(1).replace(":", "").lower()


def check_reference(aapt: Path, apksigner: Path, reference: Path) -> dict:
    if not reference.is_file() or not zipfile.is_zipfile(reference):
        nogo("un APK historique réellement signé est obligatoire")
    app, version_code, version = package_identity(aapt, reference)
    if app != PACKAGE or (version_code, version) not in ALLOWED_INSTALLED:
        nogo("l'APK historique n'est ni 1.17.4/code42 ni 1.17.8/code46")
    if package_certificate(apksigner, reference) != HISTORICAL_CERT:
        nogo("signature de l'APK historique différente")
    return {"versionCode": version_code, "versionName": version,
            "reference_apk_sha256": digest(reference.read_bytes())}


def check_candidate(aapt: Path, apksigner: Path, apk: Path) -> None:
    if package_identity(aapt, apk) != (PACKAGE, VERSION_CODE, VERSION):
        nogo("l'APK construit n'est pas 1.17.9/code47, package officiel")
    if package_certificate(apksigner, apk) != HISTORICAL_CERT:
        nogo("l'APK construit utilise un autre certificat")
    if not zipfile.is_zipfile(apk):
        nogo("APK invalide")
    with zipfile.ZipFile(apk) as z:
        if z.testzip() is not None:
            nogo("APK endommagé")
        for name, expected in RESOURCES.items():
            try:
                actual = digest(z.read("assets/" + name))
            except KeyError:
                nogo("ressource absente de l'APK signé : " + name)
            if actual != expected:
                nogo("ressource privée altérée dans l'APK signé : " + name)


def main() -> None:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--source", type=Path, required=True,
                   help="checkout Git certifié, avec sept fichiers privés git-ignored")
    p.add_argument("--expected-sha", required=True,
                   help="SHA 40 caractères obtenu après CI finale PASS")
    p.add_argument("--historical-apk", type=Path, required=True,
                   help="APK officiel antérieur réellement installé (1.17.4/1.17.8)")
    p.add_argument("--build-tools", type=Path, required=True,
                   help="répertoire Android SDK build-tools/36.0.0")
    p.add_argument("--output", type=Path, required=True,
                   help="chemin local PRIVÉ hors du dépôt public")
    a = p.parse_args()
    os.umask(0o077)

    if os.environ.get("GITHUB_ACTIONS", "").lower() == "true" or os.environ.get("CI", "").lower() == "true":
        nogo("assemblage/signature interdits sur CI et sur dépôt public")
    for var in ("HIFZ_KEYSTORE_PATH", "HIFZ_KEYSTORE_PASSWORD",
                "HIFZ_KEY_PASSWORD", "HIFZ_KEY_ALIAS"):
        if not os.environ.get(var):
            nogo("variable de signature historique absente : " + var)
    source = a.source.resolve()
    assert_outside_public_checkout(source, a.output)
    check_source(source, a.expected_sha)
    check_assets(source)
    qcf_folder = source / "app/src/main/assets/mushaf/hafs/kfqc/svg-br"
    if not all((qcf_folder / (f"{i:03d}.svg.br")).is_file() for i in range(1, 605)):
        nogo("604 pages vectorielles QCF originales non disponibles localement")
    tools = {name: a.build_tools / name for name in ("aapt", "apksigner")}
    if not all(t.is_file() for t in tools.values()):
        nogo("Android build-tools 36.0.0 incomplets")
    previous = check_reference(tools["aapt"], tools["apksigner"], a.historical_apk)
    keystore = Path(os.environ["HIFZ_KEYSTORE_PATH"])
    if not keystore.is_file():
        nogo("keystore historique absent")
    environment = os.environ.copy()
    environment["AYAH_THEMES_PRIVATE_114"] = "1"
    environment["HIFZ_PRIVATE_RELEASE_UPGRADE"] = "1"
    # Gradle's verifyPrivateReleaseInputs checks the official signer and all
    # seven SHA-256s BEFORE its guarded :hifz-app:packageRelease task.
    command(["gradle", "--offline", "--no-daemon", ":hifz-app:assembleRelease"],
            env=environment)
    output_folder = source / "hifz-app/build-private114/outputs/apk/release"
    built = [x for x in output_folder.glob("*-release.apk") if x.is_file()]
    if len(built) != 1:
        nogo("un unique APK release signé n'a pas été construit")
    check_candidate(tools["aapt"], tools["apksigner"], built[0])
    a.output.parent.mkdir(parents=True, exist_ok=True)
    if a.output.exists():
        nogo("interdiction d'écraser un APK")
    shutil.copyfile(built[0], a.output)
    a.output.chmod(0o600)
    result = {"status": "PRIVATE_SIGNED_CANDIDATE__PHYSICAL_BOOX_NO_GO",
              "source_sha": a.expected_sha, "package": PACKAGE,
              "versionName": VERSION, "versionCode": VERSION_CODE,
              "signer_sha256": HISTORICAL_CERT,
              "candidate_sha256": digest(a.output.read_bytes()),
              "historical_reference": previous,
              "private_resources_sha256": RESOURCES,
              "physical_boox_upgrade_test": "NOT_EXECUTED",
              "user_progression_backup_comparison": "NOT_EXECUTED",
              "publication_authorized": False}
    a.output.with_suffix(".verification.json").write_text(
        json.dumps(result, indent=2, ensure_ascii=False) + "\n", encoding="utf-8"
    )
    print("PASS : APK privé signé, contenu et identité vérifiés LOCAL/OUTIL HORS LIGNE.")
    print("SHA256 : " + result["candidate_sha256"])
    print("NO-GO : installation et contrôle des données réelles BOOX non réalisés.")
    print("NE JAMAIS PUBLIER l'APK ni les sept ressources dans GitHub public.")


if __name__ == "__main__":
    main()
