plugins {
    id("com.android.application")
}

android {
    namespace = "com.quransafeguard.hifz"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.quransafeguard.hifz"
        minSdk = 23
        targetSdk = 35
        versionCode = 10
        versionName = "0.7.3-boox"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":hifz-core"))
    implementation("org.brotli:dec:0.1.2")
    testImplementation("junit:junit:4.13.2")
}

val prepareHifzTafsirRelease by tasks.registering {
    group = "verification"
    description = "Normalize audited Tafsir assets for the standalone Hifz APK."
    doLast {
        val sourceRoot = rootProject.file("app/src/plus/assets/tafsir")
        val targetRoot = file("src/main/assets/tafsir")
        check(sourceRoot.isDirectory) { "Missing audited Tafsir source directory: $sourceRoot" }
        targetRoot.deleteRecursively()
        targetRoot.mkdirs()

        data class Corpus(val stem: String, val sourcePattern: Regex, val expectedParts: Int)
        val corpora = listOf(
            Corpus("al_jalalayn_en.sqlite", Regex("al_jalalayn_en\\.sqlite\\.gz\\.part\\d+"), 4),
            Corpus("qurtubi_en.sqlite", Regex("qurtubi_en\\.sqlite\\.gz\\.b64\\.part\\d+"), 4),
            Corpus("qushayri_en.sqlite", Regex("qushayri_en\\.sqlite\\.gz\\.b64\\.part\\d+"), 2)
        )

        corpora.forEach { corpus ->
            val parts = sourceRoot.listFiles().orEmpty()
                .filter { corpus.sourcePattern.matches(it.name) }
                .sortedBy { it.name }
            check(parts.size == corpus.expectedParts) {
                "${corpus.stem}: expected ${corpus.expectedParts} source parts, found ${parts.size}"
            }
            val payload = parts.flatMap { it.readBytes().asIterable() }.toByteArray()
            val decoded = if (parts.first().name.contains(".b64.")) {
                java.util.Base64.getMimeDecoder().decode(payload)
            } else payload
            val chunkSize = kotlin.math.ceil(decoded.size / corpus.expectedParts.toDouble()).toInt()
            repeat(corpus.expectedParts) { index ->
                val start = index * chunkSize
                val end = kotlin.math.min(decoded.size, start + chunkSize)
                check(start < end) { "${corpus.stem}: empty normalized part $index" }
                file("src/main/assets/tafsir/${corpus.stem}.gz.part${index.toString().padStart(2, '0')}")
                    .writeBytes(decoded.copyOfRange(start, end))
            }
        }
        println("HIFZ_TAFSIR_RELEASE_ASSETS_OK")
    }
}

val prepareHifzAssets by tasks.registering {
    group = "verification"
    description = "Prepare standalone Quran Hifz runtime assets from validated local sources."
    dependsOn(prepareHifzTafsirRelease)
    doLast {
        val sourceMushaf = rootProject.file("app/src/main/assets/mushaf/hafs/kfqc/svg-br")
        val sourceGeometry = rootProject.file("app/src/main/assets/reader109/geometry.json")
        val targetMushaf = file("src/main/assets/mushaf/hafs/kfqc/svg-br")
        val targetGeometry = file("src/main/assets/reader109/geometry.json")
        check(sourceMushaf.isDirectory) { "Missing canonical Mushaf source directory: $sourceMushaf" }
        check(sourceGeometry.isFile) { "Missing generated Hifz geometry: $sourceGeometry" }
        targetMushaf.deleteRecursively()
        targetMushaf.mkdirs()
        sourceMushaf.listFiles().orEmpty().filter { it.name.matches(Regex("\\d{3}\\.svg\\.br")) }.forEach { source ->
            source.copyTo(targetMushaf.resolve(source.name), overwrite = true)
        }
        check(targetMushaf.listFiles().orEmpty().count { it.name.matches(Regex("\\d{3}\\.svg\\.br")) } == 604) {
            "Standalone Hifz APK must package exactly 604 canonical Mushaf pages."
        }
        targetGeometry.parentFile.mkdirs()
        sourceGeometry.copyTo(targetGeometry, overwrite = true)
    }
}

tasks.named("preBuild").configure { dependsOn(prepareHifzAssets) }

val verifyHifzProductBoundary by tasks.registering {
    group = "verification"
    description = "Verify standalone Quran Hifz stays offline and contains no Safeguard blocking surface."
    doLast {
        val root = file("src/main")
        val text = root.walkTopDown().filter { it.isFile && it.extension in setOf("java", "kt", "xml", "html", "js", "json") }
            .joinToString("\n") { it.readText() }
        val forbidden = listOf(
            "AccessibilityService", "BIND_ACCESSIBILITY_SERVICE", "QUERY_ALL_PACKAGES", "QuranAccessibilityService",
            "UsageStatsManager", "getInstalledPackages", "allowlist", "blocklist"
        )
        forbidden.forEach { token -> check(!text.contains(token)) { "Forbidden Safeguard surface in Hifz app: $token" } }
        val manifest = file("src/main/AndroidManifest.xml").readText()
        check(!manifest.contains("android.permission.INTERNET")) { "Quran Hifz must stay offline-first." }
    }
}

val verifyHifzCosmeticContract by tasks.registering {
    group = "verification"
    description = "Guard compact BOOX reader/dashboard/settings presentation without changing Hifz semantics."
    doLast {
        val session = file("src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java").readText()
        val settings = file("src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java").readText()
        val main = file("src/main/java/com/quransafeguard/hifz/preview/MainActivity.java").readText()
        val ui = file("src/main/java/com/quransafeguard/hifz/preview/Ui.java").readText()
        val manifest = file("src/main/AndroidManifest.xml").readText()

        check(!session.contains("Page suivante") && !session.contains("Page précédente")) {
            "Tablet Hifz sessions must use swipe/hardware page turns, not permanent page buttons."
        }
        check(!session.contains("heading = Ui.bookText")) {
            "Sabqi, Itqan and Murajaah must not waste Mushaf height on a separate mode title."
        }
        check(session.contains("LinearLayout controlBar = Ui.row(this)")) {
            "Session actions and offline audio must share one compact bottom control row."
        }
        check(session.contains("Écouter") && session.contains("gate.installed()")) {
            "Audio control must stay visible and route to Settings until the local pack is installed."
        }
        check(settings.contains("Ajouter") && settings.contains("Début de rotation Itqān"))
        check(settings.contains("FLAG_GRANT_PERSISTABLE_URI_PERMISSION")) { "Audio picker should retain read permission for a long import." }
        check(main.contains("todayAction.setOnClickListener") && !main.contains("\"Séance\", v -> openToday")) {
            "Today card should be the single scheduled-session entry point."
        }
        check(ui.contains("ic_ui_sabqi") && ui.contains("ic_ui_itqan") && ui.contains("ic_ui_murajaah")) {
            "Hifz pictograms must use the uniform semantic icon family."
        }
        check(manifest.contains("ic_quran_hifz_logo")) { "Quran Hifz launcher icon must use the Mushaf/rehal identity." }
    }
}

val verifyHifzConvergenceRules by tasks.registering {
    group = "verification"
    description = "Verify reader, mask, audio, dashboard and settings still converge on the final 0.7.3 contract."
    doLast {
        val config = file("src/main/java/com/quransafeguard/hifz/preview/PreviewConfig.java").readText()
        val core = rootProject.file("hifz-core/src/main/kotlin/com/quransafeguard/hifz/core/HifzCore.kt").readText()
        val session = file("src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java").readText()
        val settings = file("src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java").readText()
        val prefs = file("src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java").readText()
        val main = file("src/main/java/com/quransafeguard/hifz/preview/MainActivity.java").readText()
        val ui = file("src/main/java/com/quransafeguard/hifz/preview/Ui.java").readText()
        val manifest = file("src/main/AndroidManifest.xml").readText()
        val study = file("src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java").readText()
        val reader = file("src/main/assets/hifzreader/reader.js").readText()
        val audio = file("src/main/java/com/quransafeguard/hifz/preview/HifzAudioDialog.java").readText()
        val mushaf = file("src/main/java/com/quransafeguard/hifz/preview/MushafView.java").readText()
        val eink = file("src/main/java/com/quransafeguard/hifz/preview/EinkController.java").readText()

        check(config.contains("SCHEMA_VERSION = 3")) { "Hifz state must remain on schema v3." }
        check(core.contains("PlannedSession(SessionKind.SABQI_TODAY_REVIEW, 30)")
                && core.contains("PlannedSession(SessionKind.OLD_ITQAN_MURAJAAH, 60)")
                && core.contains("PlannedSession(SessionKind.RECENT_SABQI_REVIEW, 30)")
                && core.contains("PlannedSession(SessionKind.OLD_ITQAN_MURAJAAH, 30)")) {
            "Fixed timed sessions must be defined by HifzSchedule, not duplicate PreviewConfig constants."
        }
        check(config.contains("SABQI_TOTAL_REPS = 37"))
        check(config.contains("ITQAN_TOTAL_REPS = 40"))
        check(!prefs.contains("pendingPromotedItqan")) { "Priority promoted Itqan queue is forbidden by cycle philosophy." }
        check(prefs.contains("unconsolidatedPromotedRanges") && prefs.contains("legacyMurajaahPromotedRanges"))
        check(prefs.contains("itqanWorkCorpus()") && prefs.contains("murajaahCorpus()"))
        check(session.contains("SABQI_TODAY_REVIEW") && session.contains("RECENT_SABQI_REVIEW"))
        check(session.contains("completeExpiredTimedSession") && session.contains("completeEmptyRecentSabqiSession"))
        check(session.contains("HifzSchedule") && session.contains("planFor")) { "Runtime timed sessions must share HifzSchedule with Today/dashboard." }
        check(session.contains("prefs.maskEntropyFor(mode)") && session.contains("prefs.clearMaskEntropy(mode)")) { "Mask entropy must persist for the logical Hifz session and reset only after completion." }
        check(!session.contains("murajaahBlockB") && !session.contains("transitionToBlockB") && !session.contains("unusedA") && !session.contains("availableB")) {
            "Legacy A/B allocation and unused-time transfer must stay removed."
        }
        check(!session.contains("Page suivante") && !session.contains("Page précédente")) {
            "Tablet Hifz sessions must use swipe/hardware page turns, not permanent page buttons."
        }
        check(!session.contains("heading = Ui.bookText")) {
            "Sabqi, Itqan and Murajaah must not waste Mushaf height on a separate mode title."
        }
        check(session.contains("LinearLayout controlBar = Ui.row(this)")) {
            "Session actions and offline audio must share one compact bottom control row."
        }
        check(session.contains("Écouter") && session.contains("gate.installed()")) {
            "Audio control must stay visible and route to Settings until the local pack is installed."
        }

        check(settings.contains("Ajouter") && settings.contains("Début de rotation Itqān"))
        check(settings.contains("FLAG_GRANT_PERSISTABLE_URI_PERMISSION")) { "Audio picker should retain read permission for a long import." }
        check(prefs.contains("itqanRanges") && prefs.contains("promotedRanges"))
        check(main.contains("todayAction.setOnClickListener") && !main.contains("\"Séance\", v -> openToday")) {
            "Today card should be the single scheduled-session entry point."
        }
        check(ui.contains("ic_ui_sabqi") && ui.contains("ic_ui_itqan") && ui.contains("ic_ui_murajaah")) {
            "Hifz pictograms must use the uniform semantic icon family."
        }
        check(manifest.contains("ic_quran_hifz_logo")) { "Quran Hifz launcher icon must use the Mushaf/rehal identity." }

        check(study.contains("LAYOUT_DIRECTION_RTL")) { "Arabic-book page slider must be RTL." }
        check(reader.contains("randomOrderKeys") && reader.contains("randomSegmentsForCells")
                && reader.contains("totalWidth*fraction") && reader.contains("Math.min(cellWidth,remaining)")
                && reader.contains("markerLayer(svg,polys,lines)")
                && reader.contains("layer.appendChild(markerLayer(svg,polys,lines))")
                && reader.contains("seededRandom") && reader.contains("maskEntropy")
                && !reader.contains("line.words") && !reader.contains("function hiddenBandForLine")) {
            "Mask must randomize existing source-ink groups, preserve cumulative percentages, and keep verse markers above masks."
        }
        check(reader.contains("setAudioVerse") && reader.contains("setEink(value)") && reader.contains("clearReveal") && reader.contains("revealSelection"))
        check(!reader.contains("window.scrollBy") && !reader.contains("--reveal-pad")) { "Tafsir reveal must never scroll or pre-shift the whole reader." }
        check(audio.contains("prepareAsync()")) { "Audio prepare must not block the UI thread." }
        check(audio.indexOf("player.start()") < audio.indexOf("mushaf.setAudioVerse(verse)")) {
            "Audio highlight must switch only after playback starts."
        }
        check(mushaf.contains("eink.audio(this)")) { "Audio highlight must use the dedicated BOOX refresh path." }
        check(eink.contains("REGAL") && eink.contains("GU") && eink.contains("GC")) { "BOOX partial/full refresh preference missing." }

        check(settings.contains("HifzAudioPack.PACK_FILE_NAME") && settings.contains("Choisir le pack"))
    }
}
