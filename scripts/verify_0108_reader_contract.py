#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def text(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")

def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit("0.10.8 reader contract: " + message)

free = text("app/src/main/java/com/quranunlock/guard/FreeQuranReaderActivity.kt")
free_html = text("app/src/main/assets/reader109/index.html")
free_js = text("app/src/main/assets/reader109/reader.js")
challenge = text("app/src/main/java/com/quranunlock/guard/MushafReaderActivity.kt")
comfort = text("app/src/main/java/com/quranunlock/guard/ReaderComfortPrefs.kt")
panel = text("app/src/plus/java/com/quranunlock/guard/TafsirPanel.kt")
repo = text("app/src/plus/java/com/quranunlock/guard/TafsirRepository.kt")
honorific = text("app/src/plus/java/com/quranunlock/guard/JalalaynHonorificPresentation.kt")

require('READER_CREAM_HEX = "#F7F2E8"' in comfort, "cream constant missing")
require("ReaderVisualMode.entries.forEach" not in free, "theme choices remain in free reader UI")
require('"Clair"' not in free and '"Sombre"' not in free, "light/dark reader choices remain")
require('#F7F2E8' in free and '--cream:#F7F2E8' in free_html,
        "free Kotlin/HTML reader is not pinned to cream")
require("ReaderComfortPrefs.pageBackground()" in challenge, "challenge reader is not pinned to cream")
require("android.graphics.Color.WHITE" not in challenge, "challenge WebView still forces white")
require("background: #ffffff" not in challenge, "challenge HTML still forces white")
require("color = SafeguardReadingSurface" in challenge, "challenge shell is not cream")

for name, source in (("free", free), ("challenge", challenge)):
    require("FLAG_KEEP_SCREEN_ON" in source, f"{name} reader does not stay awake")
    require("statusBarsPadding()" in source, f"{name} reader does not respect status bar")
    require("navigationBarsPadding()" in source, f"{name} reader does not respect navigation bar")
require("delay(2_500L)" in challenge, "challenge reader lacks calm auto-hide")
require("onReaderTap" in challenge, "challenge reader lacks tap-to-reveal")
require('"☼"' in challenge, "challenge reader lacks brightness control")
require("setTimeout" in free_js and "2500" in free_js and "chrome()" in free_js,
        "free HTML/JS reader lacks calm auto-hide and neutral tap reveal")
require('id="sun"' in free_html and "setBrightness" in free and "setBrightness" in free_js,
        "free HTML/JS reader lacks brightness-only sun control")
require("ReaderComfortPrefs.applyBrightness" in free,
        "free reader does not apply the persisted brightness")

runtime_sources = "\n".join(
    path.read_text(encoding="utf-8")
    for path in (ROOT / "app/src").rglob("*")
    if path.is_file() and path.suffix in {".kt", ".java", ".html", ".js", ".xml"}
)
require("#F4F0E6" not in runtime_sources and "0xFFF4F0E6" not in runtime_sources,
        "obsolete cream remains in runtime sources")

require("Poésie · lignes conservées selon l’édition source" not in panel,
        "application-authored poetry explanation still rendered")
require("JalalaynHonorificPresentation.normalize" in repo,
        "Jalalayn presentation normalization is not wired")
for token in ('"(ṣ)" to "ﷺ"', '"(ʿa)" to "عليه السلام"'):
    require(token in honorific, "missing honorific mapping: " + token)

for token in (
    "SafeguardCyclePrefs.currentPlan",
    "ReadingValidationPolicy.canValidate",
    "GuardPrefs.completeReadingAndUnlock",
    "mayCountActiveReading()",
    "override fun onTopResumedActivityChanged",
    "GuardPrefs.MIN_READING_MS",
    "TargetReturnCoordinator.returnImmediately",
):
    require(token in challenge, "critical reading/unlock contract disappeared: " + token)

print("0.10.8 Quran reader contract PASS")
print("- fixed cream readers; brightness-only sun; safe system insets")
print("- calm immersion + tap reveal; keep-screen-on")
print("- Tafsir editorial poetry label removed")
print("- Jalalayn remains English; honorific shorthand normalized at display time")
print("- 60-second/foreground/unlock structural safeguards retained")

