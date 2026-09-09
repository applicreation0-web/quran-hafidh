#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace_once(path: str, old: str, new: str, marker: str) -> None:
    p = ROOT / path
    text = p.read_text(encoding="utf-8")
    if new in text:
        return
    if old not in text:
        raise SystemExit(f"{path}: expected {marker} source block not found")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")


replace_once(
    "app/src/main/java/com/quranunlock/guard/FreeQuranReaderActivity.kt",
    """        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)\n        ReaderComfortPrefs.applyBrightness(window, ReaderComfortPrefs.brightness(this))""",
    """        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)\n        Reader109StateSanitizer.migrateOnReaderEntry(this)\n        ReaderComfortPrefs.applyBrightness(window, ReaderComfortPrefs.brightness(this))""",
    "reader109 migration hook",
)

replace_once(
    "app/src/main/assets/reader109/reader.js",
    "Audio Al-Husary indisponible : droits et hébergement à valider.",
    "Audio Al-Husary indisponible actuellement — source de diffusion non encore validée.",
    "explicit audio unavailable copy",
)

mushaf = "app/src/main/java/com/quranunlock/guard/MushafReaderActivity.kt"
replace_once(
    mushaf,
    "settings.javaScriptEnabled = false",
    """// JavaScript is enabled only for a local DOM visibility probe.\n                // No JavaScript interface is exposed and network loads remain blocked.\n                settings.javaScriptEnabled = true""",
    "timed reader DOM probe JavaScript",
)

replace_once(
    mushaf,
    """                            val svgContent = remember(pageNumber) {\n                                loadMushafPage(pageNumber)\n                            }\n                            var loadFailed by remember(pageNumber) {\n                                mutableStateOf(svgContent.isNullOrBlank())\n                            }""",
    """                            var retryGeneration by remember(pageNumber) {\n                                mutableIntStateOf(0)\n                            }\n                            val svgContent = remember(pageNumber, retryGeneration) {\n                                loadMushafPage(pageNumber)\n                            }\n                            var loadFailed by remember(pageNumber, retryGeneration) {\n                                mutableStateOf(svgContent.isNullOrBlank())\n                            }""",
    "challenge retry generation",
)

replace_once(
    mushaf,
    """                            } else {\n                                Text(\n                                    \"La page du Mushaf n’est pas disponible. \" +\n                                        \"La lecture n’est pas comptabilisée.\",\n                                    modifier = Modifier.padding(18.dp)\n                                )\n                            }""",
    """                            } else {\n                                Column(\n                                    modifier = Modifier\n                                        .fillMaxSize()\n                                        .padding(18.dp),\n                                    verticalArrangement = Arrangement.Center,\n                                    horizontalAlignment = Alignment.CenterHorizontally\n                                ) {\n                                    Text(\n                                        \"La page du Mushaf n’est pas visible. \" +\n                                            \"La lecture n’est pas comptabilisée.\"\n                                    )\n                                    Spacer(Modifier.height(12.dp))\n                                    SafeguardOutlinedButton(\n                                        onClick = {\n                                            markPageUnavailable(pageNumber)\n                                            loadFailed = false\n                                            retryGeneration += 1\n                                        }\n                                    ) {\n                                        Text(\"Réessayer\")\n                                    }\n                                }\n                            }""",
    "challenge retry UI",
)

replace_once(
    mushaf,
    """                    override fun onPageFinished(\n                        view: WebView?,\n                        url: String?\n                    ) {\n                        currentOnReady.value()\n                        view?.post {\n                            val contentHeightPx =\n                                (view.contentHeight * view.scale).toInt()\n                            if (contentHeightPx > 0 &&\n                                view.height > 0 &&\n                                !ReadingValidationPolicy.requiresScroll(\n                                    contentHeightPx = contentHeightPx,\n                                    viewportHeightPx = view.height\n                                )\n                            ) {\n                                currentOnBottomReached.value()\n                            }\n                        }\n                    }""",
    """                    override fun onPageFinished(\n                        view: WebView?,\n                        url: String?\n                    ) {\n                        view ?: run {\n                            currentOnFailure.value()\n                            return\n                        }\n                        view.post {\n                            view.evaluateJavascript(\n                                MushafRuntimeVisibilityPolicy.probeJavascript()\n                            ) { result ->\n                                if (!MushafRuntimeVisibilityPolicy\n                                        .probeResultIsReady(result)\n                                ) {\n                                    currentOnFailure.value()\n                                    return@evaluateJavascript\n                                }\n                                currentOnReady.value()\n                                view.post {\n                                    val contentHeightPx =\n                                        (view.contentHeight * view.scale).toInt()\n                                    if (contentHeightPx > 0 &&\n                                        view.height > 0 &&\n                                        !ReadingValidationPolicy.requiresScroll(\n                                            contentHeightPx = contentHeightPx,\n                                            viewportHeightPx = view.height\n                                        )\n                                    ) {\n                                        currentOnBottomReached.value()\n                                    }\n                                }\n                            }\n                        }\n                    }""",
    "timed reader DOM readiness assertion",
)

replace_once(
    mushaf,
    """        return runCatching {\n            assets.open(assetPath).use { compressed ->\n                BrotliInputStream(compressed)\n                    .bufferedReader(Charsets.UTF_8)\n                    .use { it.readText() }\n            }\n        }.getOrNull()""",
    """        return runCatching {\n            assets.open(assetPath).use { compressed ->\n                BrotliInputStream(compressed)\n                    .bufferedReader(Charsets.UTF_8)\n                    .use { it.readText() }\n            }\n        }.getOrNull()\n            ?.takeIf(MushafRuntimeVisibilityPolicy::sourceLooksRenderable)""",
    "timed reader SVG source validation",
)

build = "app/build.gradle.kts"
replace_once(
    build,
    "compileSdk = 36",
    "compileSdk = 37",
    "proven 0.10.8 Android compile SDK",
)
replace_once(
    build,
    """        val preparedReleaseMetadata =\n            buildFile.contains(\"versionCode = 28\") &&\n                buildFile.contains(\"versionName = \\\"0.10.9\\\"\")\n        check(auditedBaselineMetadata || preparedReleaseMetadata) {\n            \"Expected either the audited 0.10.3 baseline metadata or prepared 0.10.9 release metadata.\"\n        }""",
    """        val preparedReleaseMetadata =\n            (buildFile.contains(\"versionCode = 28\") &&\n                buildFile.contains(\"versionName = \\\"0.10.9\\\"\")) ||\n            (buildFile.contains(\"versionCode = 29\") &&\n                buildFile.contains(\"versionName = \\\"0.10.10\\\"\"))\n        check(auditedBaselineMetadata || preparedReleaseMetadata) {\n            \"Expected the audited baseline or an explicitly prepared 0.10.9/0.10.10 release metadata set.\"\n        }""",
    "10.10 migration metadata verifier",
)
replace_once(
    build,
    """        versionCode = 28\n        versionName = \"0.10.9\"""",
    """        versionCode = 29\n        versionName = \"0.10.10\"""",
    "0.10.10 version metadata",
)

print("0.10.10 runtime hardening is applied")
