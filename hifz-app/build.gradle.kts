plugins {
    id("com.android.application")
}

val generatedHifzAssetsDir = layout.buildDirectory.dir("generated/hifzAssets").get().asFile
val generatedHifzTafsirDir = layout.buildDirectory.dir("generated/hifzTafsir").get().asFile
val hifzTafsirSourceDir = rootProject.file("app/src/plus/assets/tafsir")
val hasReleaseSigning = !System.getenv("HIFZ_KEYSTORE_PATH").isNullOrBlank()

val prepareHifzTafsirRelease by tasks.registering(Exec::class) {
    inputs.dir(hifzTafsirSourceDir)
    inputs.file(rootProject.file("scripts/prepare_hifz_tafsir_release.py"))
    outputs.dir(generatedHifzTafsirDir)
    commandLine(
        "python3",
        rootProject.file("scripts/prepare_hifz_tafsir_release.py").absolutePath,
        hifzTafsirSourceDir.absolutePath,
        generatedHifzTafsirDir.absolutePath
    )
}

val prepareHifzAssets by tasks.registering(Sync::class) {
    dependsOn(prepareHifzTafsirRelease)
    into(generatedHifzAssetsDir)
    from(rootProject.file("app/src/main/assets/mushaf")) { into("mushaf") }
    from(rootProject.file("app/src/main/assets/reader109/geometry.json")) { into("reader109") }
    from(rootProject.file("app/src/main/assets/reader109/waqf.json")) { into("reader109") }
    from(generatedHifzTafsirDir) { into("tafsir") }
}

val verifyHifzProductBoundary by tasks.registering {
    doLast {
        val manifest = file("src/main/AndroidManifest.xml").readText()
        check(!manifest.contains("AccessibilityService", ignoreCase = true)) { "Quran Hifz must not package an Accessibility service." }
        check(!manifest.contains("<queries>")) { "Quran Hifz must not query or enumerate external applications." }
        check(!manifest.contains("QUERY_ALL_PACKAGES")) { "Quran Hifz must never request broad package visibility." }
        check(!manifest.contains("BIND_ACCESSIBILITY_SERVICE")) { "Quran Hifz must never request Safeguard blocking privileges." }
        check(!manifest.contains("android.permission.INTERNET")) { "Quran Hifz must remain offline-first and must not request INTERNET." }
        val sourceText = fileTree("src/main") { include("**/*.java", "**/*.kt", "**/*.xml") }.files.joinToString("\n") { it.readText() }
        listOf("QuranAccessibilityService","ProtectedApps","GuardPrefs.protectedPackages","UsageCyclePolicy","UnlockBudgetIntegrity").forEach { forbidden ->
            check(!sourceText.contains(forbidden)) { "Safeguard-only symbol leaked into Quran Hifz: $forbidden" }
        }
    }
}

val verifyHifzCosmeticContract by tasks.registering {
    doLast {
        val audio = file("src/main/java/com/quransafeguard/hifz/preview/HifzAudioDialog.java").readText()
        val session = file("src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java").readText()
        val study = file("src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java").readText()
        val settings = file("src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java").readText()
        val main = file("src/main/java/com/quransafeguard/hifz/preview/MainActivity.java").readText()
        val ui = file("src/main/java/com/quransafeguard/hifz/preview/Ui.java").readText()

        check(!audio.contains("android.app.Dialog") && !audio.contains("new Dialog(")) {
            "BOOX audio must be inline, never a modal Dialog."
        }
        check(audio.contains("attachInline") && audio.contains("detachInline")) {
            "Audio controller must expose explicit inline attach/detach lifecycle."
        }
        check(session.contains("audioHost")
                && !study.contains("audioHost")
                && !study.contains("HifzAudioDialog")
                && !study.contains("openAudio()")) {
            "Structured Hifz sessions must keep inline audio, while Lecture/Study must remain audio-free."
        }
        check(ui.contains("iconButton") && ui.contains("settingRow")) {
            "Compact icon hit-targets and settings rows are required by the BOOX cosmetic contract."
        }
        check(settings.contains("Ui.settingRow")) {
            "Settings must be predominantly row-based instead of button-card based."
        }
        check(!main.contains("Ui.panel(todayAction)")) {
            "Home Today launcher must stay lightweight rather than render as a dashboard card."
        }
        check(study.contains("readerActions") && study.contains("pageRail")) {
            "Reader actions and page slider must be separate compact surfaces."
        }
        check(study.contains("title.setSingleLine(true)")
                && study.contains("tafsirTextControl")
                && study.contains("PopupMenu")
                && study.contains("showTafsirEditionMenu")
                && study.contains("available.size() <= 1")
                && !study.contains("tafsirEditionButton")) {
            "Tafsir source selection must stay compact, hide unavailable authors, and use one dropdown only when needed."
        }
    }
}

val verifyHifzConvergenceRules by tasks.registering {
    doLast {
        val config = file("src/main/java/com/quransafeguard/hifz/preview/PreviewConfig.java").readText()
        val core = rootProject.file("hifz-core/src/main/kotlin/com/quransafeguard/hifz/core/HifzCore.kt").readText()
        val session = file("src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java").readText()
        val settings = file("src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java").readText()
        val main = file("src/main/java/com/quransafeguard/hifz/preview/MainActivity.java").readText()
        val ui = file("src/main/java/com/quransafeguard/hifz/preview/Ui.java").readText()
        val study = file("src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java").readText()
        val reader = file("src/main/assets/hifzreader/reader.js").readText()
        val prefs = file("src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java").readText()
        val audioPack = file("src/main/java/com/quransafeguard/hifz/preview/HifzAudioPack.java").readText()
        val audio = file("src/main/java/com/quransafeguard/hifz/preview/HifzAudioDialog.java").readText()
        val mushaf = file("src/main/java/com/quransafeguard/hifz/preview/MushafView.java").readText()
        val eink = file("src/main/java/com/quransafeguard/hifz/preview/EinkController.java").readText()
        val manifest = file("src/main/AndroidManifest.xml").readText()

        check(core.contains("EVENING_REVIEW_MINUTES = 30")
                && core.contains("CONSOLIDATION_MINUTES = 30")
                && core.contains("ANCHORING_ENVELOPE_MINUTES = 60")
                && core.contains("MAINTENANCE_MINUTES = 45")
                && core.contains("ACTIVE_REVIEW_MINUTES = 15")) {
            "Fixed timed sessions must be defined by HifzSchedule, not duplicate PreviewConfig constants."
        }
        check(session.contains("MURAJAAH_ACTIVE") && session.contains("renderMurajaahActive")) {
            "The daily masked-recall Révision active must run before the passive Entretien."
        }
        check(config.contains("ITQAN_VISIBLE_REPS_WORKING = 15"))
        check(config.contains("ITQAN_100_REPS_WORKING = 10"))
        check(config.contains("ITQAN_TOTAL_REPS = 40")) { "Full promoted-page Ancrage must keep the ×40 protocol." }
        check(config.contains("ITQAN_LIGHT_VISIBLE_REPS = 20")
                && config.contains("ITQAN_LIGHT_25_REPS = 0")
                && config.contains("ITQAN_LIGHT_50_REPS = 5")
                && config.contains("ITQAN_LIGHT_75_REPS = 5")
                && config.contains("ITQAN_LIGHT_100_REPS = 5")
                && config.contains("ITQAN_LIGHT_TOTAL_REPS = 35")) {
            "Reconstruction Ancrage must stay on 35 repetitions: 20 visible + 5/5/5 masked."
        }
        check(config.contains("EINK_AUDIO_CHANGES_BEFORE_FULL_CLEAN_WORKING = 6")) { "Audio needs its own anti-ghosting cleanup cadence." }
        check(config.contains("fractionatedBlockSizes")
                && config.contains("fractionatedBlockStart")
                && config.contains("fractionatedBlockLength")) {
            "Le découpage de l'Ancrage fractionné doit rester une fonction pure et testable."
        }
        check(session.contains("Stabilisation") && !session.contains("Ancrage fractionné")) {
            "Le libellé visible du mode structuré doit rester Stabilisation."
        }
        check(!session.contains("Consolidation fractionnée")) {
            "Consolidation désigne RECENT_SABQI_REVIEW et ne doit pas nommer la Stabilisation."
        }
        check(settings.contains("Sourates difficiles à stabiliser")) {
            "Le réglage durable du protocole fractionné doit rester accessible dans les Paramètres."
        }

        check(!session.contains("Faite avec aide")) { "Old ambiguous assisted button must not return." }
        check(!session.contains("Stable sans aide")) { "A fault-free Consolidation must not directly promote a block." }
        check(!session.contains("markFirstRecentStable")) { "Consolidation actions must target the displayed block." }
        check(!session.contains("reconcileStablePromotions")) { "Review quality must not directly promote to Ancrage." }
        check(session.contains("validateConsolidationFinalReview") && session.contains("validateLearningFinalReview")) {
            "Recent work must only reach Acquis through the weekly snowball's Sunday ×5 final review."
        }
        check(!session.contains("À renforcer")) {
            "The canonical schema6 flow must not expose the obsolete reinforcement action."
        }
        check(session.contains("révélations") && session.contains("Révéler"))
        check(session.contains("prêt à valider") && session.contains("validateSabqi") && session.contains("validateItqan")) {
            "Leçon neuve and Ancrage completion must require explicit persisted validation."
        }
        check(!session.contains("Page suivante") && !session.contains("Page précédente")) {
            "Tablet Hifz sessions must use swipe/hardware page turns, not permanent page buttons."
        }
        check(!session.contains("heading = Ui.bookText")) {
            "Structured Hifz modes must not waste Mushaf height on a separate mode title."
        }
        check(session.contains("LinearLayout controlBar = Ui.row(this)")) {
            "Session actions and offline audio must share one compact bottom control row."
        }
        check(session.contains("Écouter") && session.contains("gate.installed()")) {
            "Audio control must stay visible and route to Settings until the local pack is installed."
        }

        check(settings.contains("Ajouter") && settings.contains("Début de rotation de stabilisation"))
        check(settings.contains("FLAG_GRANT_PERSISTABLE_URI_PERMISSION")) { "Audio picker should retain read permission for a long import." }
        check(prefs.contains("itqanRanges") && prefs.contains("promotedRanges"))
        check(main.contains("todayAction.setOnClickListener") && !main.contains("\"Séance\", v -> openToday")) {
            "Today card should be the single scheduled-session entry point."
        }
        check(ui.contains("ic_hifz_new_lesson")
                && ui.contains("ic_hifz_anchor")
                && ui.contains("ic_hifz_maintenance")
                && ui.contains("ic_hifz_consolidation")) {
            "Hifz pictograms must use the explicit monochrome semantic icon family."
        }
        check(manifest.contains("ic_quran_hifz_logo")) { "Quran Hifz launcher icon must use the Mushaf/rehal identity." }

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
        check(mushaf.contains("eink.audio(this, prefs)")) { "Audio highlight must use the dedicated BOOX refresh path." }
        check(eink.contains("REGAL") && eink.contains("GU") && eink.contains("GC")) { "BOOX partial/full refresh preference missing." }

        check(settings.contains("HifzAudioPack.PACK_FILE_NAME") && settings.contains("Choisir le pack")
                && audioPack.contains("Quran-Hifz-Husary-Muallim.zip") && audioPack.contains("Téléchargements/QuranHifz/")) {
            "The durable local audio import path must remain explicit."
        }
        check(audioPack.contains("VERIFIED_MARKER") && audioPack.contains("sha256.txt")) {
            "Local audio packs must be structurally and cryptographically verified before activation."
        }
        check(audioPack.contains("MIN_IMPORT_FREE_BYTES") && audioPack.contains("MAX_TOTAL_EXTRACTED_BYTES")
                && audioPack.contains("recoverInterruptedActivation") && audioPack.contains("activateVerifiedPack")) {
            "Audio import must guard storage, bound extraction, and recover interrupted activation."
        }
    }
}

android {
    namespace = "com.quransafeguard.hifz.preview"
    compileSdk = 37

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.quransafeguard.hifz"
        minSdk = 26
        targetSdk = 36
        versionCode = 20
        versionName = "0.7.13-boox"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    sourceSets.getByName("main").assets.srcDir(generatedHifzAssetsDir)

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("HIFZ_KEYSTORE_PATH")
            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("HIFZ_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("HIFZ_KEY_ALIAS")
                keyPassword = System.getenv("HIFZ_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("debug") {
            isDebuggable = false
        }
        getByName("release") {
            isMinifyEnabled = false
            isDebuggable = false
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn(prepareHifzAssets)
    dependsOn(verifyHifzProductBoundary)
    dependsOn(verifyHifzCosmeticContract)
    dependsOn(verifyHifzConvergenceRules)
}

dependencies {
    implementation(project(":hifz-core"))
    implementation("org.brotli:dec:0.1.2")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("junit:junit:4.13.2")
}

// C4: keep source-text contracts, but execute/count them separately from behavioral JVM tests.
tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    if (name == "testDebugUnitTest") {
        exclude("**/*SourceContractTest.class")
        exclude("**/OfficialReleaseContractTest.class")
    }
}

val sourceContractTest = tasks.register<org.gradle.api.tasks.testing.Test>("sourceContractTest") {
    group = "verification"
    description = "Runs source-level anti-regression contracts separately from behavioral JVM tests."
    dependsOn("compileDebugUnitTestJavaWithJavac")
    include("**/*SourceContractTest.class")
    include("**/OfficialReleaseContractTest.class")
}

afterEvaluate {
    val behavioral = tasks.named<org.gradle.api.tasks.testing.Test>("testDebugUnitTest").get()
    sourceContractTest.configure {
        testClassesDirs = behavioral.testClassesDirs
        classpath = behavioral.classpath
    }
}

// CI process-death harness: connectedDebugAndroidTest normally uninstalls both packages.
// Reinstall only on GitHub Actions so a second instrumentation process can cross
// a real `adb shell am force-stop` boundary after the bulk Android suite.
tasks.matching { it.name == "connectedDebugAndroidTest" }.configureEach {
    doLast {
        if (System.getenv("GITHUB_ACTIONS") == "true") {
            val appApk = fileTree(layout.buildDirectory.dir("outputs/apk/debug")) {
                include("**/*.apk")
            }.files.firstOrNull { !it.name.contains("androidTest", ignoreCase = true) }
                ?: error("Debug APK missing after connectedDebugAndroidTest")
            val testApk = fileTree(layout.buildDirectory.dir("outputs/apk/androidTest/debug")) {
                include("**/*.apk")
            }.files.firstOrNull()
                ?: error("AndroidTest APK missing after connectedDebugAndroidTest")

            fun adbInstall(apk: java.io.File) {
                val process = ProcessBuilder("adb", "install", "-r", apk.absolutePath)
                    .inheritIO()
                    .start()
                check(process.waitFor() == 0) { "adb install failed for " + apk.name }
            }
            adbInstall(appApk)
            adbInstall(testApk)
        }
    }
}
