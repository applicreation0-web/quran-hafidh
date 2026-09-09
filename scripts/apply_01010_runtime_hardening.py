#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def patch(path: str, old: str, new: str, marker: str) -> None:
    target = ROOT / path
    source = target.read_text(encoding="utf-8")
    if new in source:
        return
    if old not in source:
        raise SystemExit(f"{path}: cannot find old or new block for {marker}")
    target.write_text(source.replace(old, new, 1), encoding="utf-8")


def require(path: str, token: str, marker: str) -> None:
    source = (ROOT / path).read_text(encoding="utf-8")
    if token not in source:
        raise SystemExit(f"{path}: missing {marker}: {token}")


build = "app/build.gradle.kts"

patch(
    build,
    '''        check(!manifest.contains("android.permission.INTERNET")) {
            "Quran Safeguard must remain offline."
        }''',
    '''        check(manifest.contains("android.permission.INTERNET")) {
            "Private local Al-Husary downloads require Android's normal INTERNET permission."
        }
        val audioController = file(
            "src/main/java/com/quranunlock/guard/QuranAudioController.kt"
        ).readText()
        val audioSource = file(
            "src/main/java/com/quranunlock/guard/QuranAudioSource.kt"
        ).readText()
        check(
            audioController.contains("QuranAudioSource.url") &&
                audioController.contains("downloadSurah(") &&
                audioController.contains("looksLikeMp3") &&
                audioSource.contains("Husary_Muallim_128kbps")
        ) {
            "INTERNET may only support the explicit private local Quran-audio path."
        }''',
    "private local audio privacy boundary",
)

patch(
    build,
    '''        check(
            adhkarUi.contains("AdhkarPeriod.MORNING") &&
                adhkarUi.contains("AdhkarPeriod.EVENING") &&
                adhkarUi.contains("Text(\\"Matin\\")") &&
                adhkarUi.contains("Text(\\"Soir\\")")
        ) {
            "Morning and evening Adhkar must both be selectable in-app."
        }
        check(adhkarUi.contains("Crossfade(")) {
            "Morning/evening changes require a calm in-app transition."
        }''',
    '''        check(
            adhkarUi.contains("AdhkarPeriod.MORNING") &&
                adhkarUi.contains("AdhkarPeriod.EVENING") &&
                adhkarUi.contains("FilterChip(") &&
                adhkarUi.contains("✓ Matin") &&
                adhkarUi.contains("✓ Soir")
        ) {
            "Morning and evening Adhkar must both be visible and selectable in-app."
        }''',
    "large-text Adhkar selector contract",
)

patch(
    build,
    '''        listOf("#171715", "#F7F2E8").forEach { brandColor ->
            check(launcherIcon.contains(brandColor)) {
                "Launcher icon lost a required cream/black brand color: " + brandColor
            }
        }''',
    '''        listOf("#1D5B47", "#B48A3C", "#76563C", "#FFF8EA").forEach { brandColor ->
            check(launcherIcon.contains(brandColor)) {
                "Launcher icon lost a required green/gold/brown/ivory brand color: " + brandColor
            }
        }''',
    "0.10.10 launcher visual identity",
)

checks = [
    ("app/src/main/java/com/quranunlock/guard/FreeQuranReaderActivity.kt", "Reader109StateSanitizer.migrateOnReaderEntry(this)", "reader migration"),
    ("app/src/main/java/com/quranunlock/guard/MushafReaderActivity.kt", "MushafRuntimeVisibilityPolicy.probeJavascript()", "Challenge visibility gate"),
    ("app/src/main/assets/reader109/reader.js", "Audio Al-Husary Muʿallim", "audio management UI"),
    ("app/src/main/assets/reader109/protocol.js", "function disableAudio(s)", "non-blocking audio downgrade"),
    ("app/src/main/java/com/quranunlock/guard/QuranAudioSource.kt", "Husary_Muallim_128kbps", "Husary Muallim source"),
    ("app/src/main/AndroidManifest.xml", "android.permission.INTERNET", "audio download permission"),
    (build, "compileSdk = 37", "proven compile SDK"),
    (build, "versionCode = 29", "0.10.10 version code"),
    (build, 'versionName = "0.10.10"', "0.10.10 version name"),
]
for file_name, token, marker in checks:
    require(file_name, token, marker)

print("0.10.10 runtime/audio/UX hardening is applied")
