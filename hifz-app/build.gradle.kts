plugins {
    id("com.android.application")
}

val generatedHifzAssetsDir = layout.buildDirectory.dir("generated/hifzAssets").get().asFile
val personalHusaryDir = rootProject.file("private/hifz-audio/husary-muallim")

val verifyPersonalEmbeddedAudio by tasks.registering {
    doLast {
        if (!personalHusaryDir.exists()) return@doLast
        val mp3 = personalHusaryDir.listFiles { file -> file.isFile && Regex("[0-9]{6}\\.mp3").matches(file.name) }?.toList().orEmpty()
        check(mp3.size == 6236) { "Personal Husary embedded corpus must contain exactly 6,236 ayah MP3s, found ${mp3.size}." }
        check(personalHusaryDir.resolve("001001.mp3").isFile) { "Missing embedded 001001.mp3" }
        check(personalHusaryDir.resolve("114006.mp3").isFile) { "Missing embedded 114006.mp3" }
        check(personalHusaryDir.resolve("source.json").isFile) { "Missing embedded audio source.json evidence" }
        check(personalHusaryDir.resolve("sha256.txt").isFile) { "Missing embedded audio sha256.txt evidence" }
        check(personalHusaryDir.resolve("upstream_checksum.md5").isFile) { "Missing preserved EveryAyah checksum evidence" }
        val source = personalHusaryDir.resolve("source.json").readText()
        check(source.contains("https://everyayah.com/data/Husary_Muallim_128kbps/")) { "Embedded audio source directory is not explicit." }
        check(source.contains("000_checksum.md5")) { "Embedded audio checksum source is not explicit." }
        check(source.contains("\"fileCount\": 6236")) { "Embedded audio source evidence has wrong file count." }
        check(source.contains("\"delivery\": \"embedded-in-personal-apk\"")) { "Embedded audio delivery evidence missing." }
    }
}

val prepareHifzAssets by tasks.registering(Sync::class) {
    dependsOn(verifyPersonalEmbeddedAudio)
    into(generatedHifzAssetsDir)
    from(rootProject.file("app/src/main/assets/mushaf")) { into("mushaf") }
    from(rootProject.file("app/src/main/assets/reader109/geometry.json")) { into("reader109") }
    from(rootProject.file("app/src/main/assets/reader109/audio.json")) { into("reader109") }
    from(rootProject.file("app/src/plus/assets/tafsir")) { into("tafsir") }
    if (personalHusaryDir.exists()) {
        from(personalHusaryDir) { into("audio/husary-muallim") }
    }
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

val verifyHifzConvergenceRules by tasks.registering {
    doLast {
        val config = file("src/main/java/com/quransafeguard/hifz/preview/PreviewConfig.java").readText()
        val session = file("src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java").readText()
        val settings = file("src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java").readText()
        val study = file("src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java").readText()
        val reader = file("src/main/assets/hifzreader/reader.js").readText()
        val prefs = file("src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java").readText()
        val audio = file("src/main/java/com/quransafeguard/hifz/preview/HifzAudioDialog.java").readText()
        val mushaf = file("src/main/java/com/quransafeguard/hifz/preview/MushafView.java").readText()
        val eink = file("src/main/java/com/quransafeguard/hifz/preview/EinkController.java").readText()
        check(config.contains("MURAJAAH_RECENT_SABQI_MINUTES_WORKING = 30"))
        check(config.contains("MURAJAAH_ITQAN_MINUTES_WORKING = 30"))
        check(config.contains("MURAJAAH_MINUTES_WORKING = 60"))
        check(config.contains("EINK_AUDIO_CHANGES_BEFORE_FULL_CLEAN_WORKING = 6")) { "Audio needs its own anti-ghosting cleanup cadence." }
        check(!session.contains("Faite avec aide")) { "Old ambiguous assisted button must not return." }
        check(!session.contains("Stable sans aide")) { "A fault-free recent review must not trigger promotion." }
        check(!session.contains("markFirstRecentStable")) { "Recent Sabqi must stay recent until capacity pressure." }
        check(!session.contains("reconcileStablePromotions")) { "Review quality must not directly promote to Itqan." }
        check(session.contains("rebalanceRecentWindow")) { "New Sabqi must enforce the sliding 30-minute recent window." }
        check(session.contains("À renforcer"))
        check(session.contains("révélations") && session.contains("Révéler"))
        check(session.contains("prêt à valider") && session.contains("validateSabqi") && session.contains("validateItqan")) {
            "Sabqi/Itqan completion must require explicit persisted validation."
        }
        check(settings.contains("Ajouter plage") && settings.contains("Début de rotation Itqān"))
        check(prefs.contains("itqanRanges") && prefs.contains("promotedRanges"))
        check(study.contains("LAYOUT_DIRECTION_RTL")) { "Arabic-book page slider must be RTL." }
        check(reader.contains("hiddenBandForLine") && reader.contains("cells.slice(n-take)")) {
            "Mask must be a continuous nested RTL band."
        }
        check(reader.contains("setAudioVerse") && reader.contains("clearReveal") && reader.contains("revealSelection"))
        check(audio.indexOf("player.start()") < audio.indexOf("mushaf.setAudioVerse(verse)")) {
            "Audio highlight must switch only after playback starts."
        }
        check(mushaf.contains("eink.audio(this)")) { "Audio highlight must use the dedicated BOOX refresh path." }
        check(eink.contains("REGAL") && eink.contains("GU") && eink.contains("GC")) { "BOOX partial/full refresh preference missing." }
    }
}

android {
    namespace = "com.quransafeguard.hifz.preview"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.quransafeguard.hifz.installtest1"
        minSdk = 26
        targetSdk = 36
        versionCode = 7
        versionName = "0.7-boox-polish-test"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    sourceSets.getByName("main").assets.srcDir(generatedHifzAssetsDir)
    androidResources {
        noCompress += "mp3"
    }

    buildTypes {
        getByName("debug") {
            // Exact personal test package; no suffix so this signed test candidate updates the prior test package.
        }
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn(prepareHifzAssets)
    dependsOn(verifyHifzProductBoundary)
    dependsOn(verifyHifzConvergenceRules)
}

dependencies {
    implementation(project(":hifz-core"))
    implementation("org.brotli:dec:0.1.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("junit:junit:4.13.2")
}
