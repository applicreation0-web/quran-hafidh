import java.security.KeyStore
import java.security.MessageDigest
plugins {
    id("com.android.application")
}

// Separate intermediates prevent private/public incremental asset contamination.
val private114 = System.getenv("AYAH_THEMES_PRIVATE_114") == "1"
// Only the user-approved 1.17.5 isolated BOOX test build uses this opt-in.
val boox1175Preview = private114 && System.getenv("HAAFIDH_1175_PRIVATE_DEBUG") == "1"
// Explicit opt-in: private corpus belongs in an official in-place RELEASE only on a
// trusted offline assembler; ordinary debug previews always keep their suffix.
val privateReleaseUpgrade = private114 && System.getenv("HIFZ_PRIVATE_RELEASE_UPGRADE") == "1"
if (private114) layout.buildDirectory.set(file("build-private114"))

val generatedHifzAssetsDir = layout.buildDirectory.dir("generated/hifzAssets").get().asFile
val generatedHifzTafsirDir = layout.buildDirectory.dir("generated/hifzTafsir").get().asFile
val hifzTafsirSourceDir = rootProject.file("app/src/plus/assets/tafsir")
val generatedSemanticAssetsDir = layout.buildDirectory.dir("generated/semanticAssets").get().asFile
// New coordinate-only asset; generated side-by-side until API compatibility is proven.
val generatedQuranicCueAssetsDir = layout.buildDirectory.dir("generated/quranicCueAssets").get().asFile
val quranicCueOutput = generatedQuranicCueAssetsDir.resolve("quranic/quranic_cues_v1.json")
// Frozen audited semantic corpus (V2.1 boundaries + V2.3 integrity overlay) behind the Al-Munīr
// amorces; SemanticPassageRepository re-checks both SHA-256 values at runtime and fails closed.
val semanticV21SourceDir = file("src/main/semantic-source/v2_1")
val semanticV21Output = generatedSemanticAssetsDir.resolve("semantic/semantic_passages_v2_1.json")
val semanticV23TitleSourceDir = file("src/main/semantic-source/v2_3_titles")
val semanticV23TitleOutput = generatedSemanticAssetsDir.resolve("semantic/semantic_titles_v2_3.json")
// Exact Quran-word boxes (quran-ws/quran-svg-elements v1.1.2) used only by the Quiz prompt.
val quranWordGeometrySourceDir = file("src/main/word-source/quran-ws-v1.1.2")
val hasReleaseSigning = !System.getenv("HIFZ_KEYSTORE_PATH").isNullOrBlank()
// Only the CP-08P staging workflow opts into the separate personal phone-pilot ID.
val cp08BaqaraPilot = System.getenv("CP08_BAQARAH_PILOT") == "1"
// Isolated 1.17.9 user test: never changes the official installed package or signing.
val haafidh1179IsolatedDebug = !private114 && System.getenv("HAAFIDH_1179_ISOLATED_DEBUG") == "1"

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

val prepareSemanticV21 by tasks.registering(Exec::class) {
    inputs.dir(semanticV21SourceDir)
    inputs.file(rootProject.file("scripts/materialize_semantic_v2_1.py"))
    outputs.file(semanticV21Output)
    commandLine(
        "python3",
        rootProject.file("scripts/materialize_semantic_v2_1.py").absolutePath,
        semanticV21SourceDir.absolutePath,
        semanticV21Output.absolutePath
    )
}

val prepareSemanticV23Titles by tasks.registering(Exec::class) {
    dependsOn(prepareSemanticV21)
    inputs.file(semanticV21Output)
    inputs.dir(semanticV23TitleSourceDir)
    inputs.file(rootProject.file("scripts/materialize_semantic_v2_3_titles.py"))
    outputs.file(semanticV23TitleOutput)
    commandLine(
        "python3",
        rootProject.file("scripts/materialize_semantic_v2_3_titles.py").absolutePath,
        semanticV21Output.absolutePath,
        semanticV23TitleSourceDir.absolutePath,
        semanticV23TitleOutput.absolutePath
    )
}

val verifyQuranWordGeometry by tasks.registering(Exec::class) {
    inputs.dir(quranWordGeometrySourceDir)
    inputs.file(rootProject.file("scripts/verify_quran_word_boxes.py"))
    commandLine(
        "python3",
        rootProject.file("scripts/verify_quran_word_boxes.py").absolutePath,
        quranWordGeometrySourceDir.absolutePath
    )
}

val prepareQuranicCueCoordinates by tasks.registering(Exec::class) {
    dependsOn(prepareSemanticV21, verifyQuranWordGeometry)
    inputs.file(semanticV21Output)
    inputs.dir(quranWordGeometrySourceDir)
    inputs.file(rootProject.file("scripts/extract_quranic_cues_v1.py"))
    outputs.file(quranicCueOutput)
    commandLine(
        "python3",
        rootProject.file("scripts/extract_quranic_cues_v1.py").absolutePath,
        semanticV21Output.absolutePath,
        quranWordGeometrySourceDir.absolutePath,
        quranicCueOutput.absolutePath
    )
}

val prepareHifzAssets by tasks.registering(Sync::class) {
    dependsOn(prepareHifzTafsirRelease, prepareSemanticV21, prepareSemanticV23Titles,
              prepareQuranicCueCoordinates, verifyQuranWordGeometry)
    into(generatedHifzAssetsDir)
    from(rootProject.file("app/src/main/assets/mushaf")) { into("mushaf") }
    from(rootProject.file("app/src/main/assets/reader109/geometry.json")) { into("reader109") }
    from(rootProject.file("app/src/main/assets/reader109/waqf.json")) { into("reader109") }
    from(quranWordGeometrySourceDir) { into("reader109/word-boxes") }
    from(generatedHifzTafsirDir) { into("tafsir") }
    from(generatedSemanticAssetsDir)
    // Staged but NOT consumed by Hifz engines until the facade passes parity tests.
    from(generatedQuranicCueAssetsDir)
}

val verifyHifzProductBoundary by tasks.registering {
    doLast {
        val manifest = file("src/main/AndroidManifest.xml").readText()
        check(!manifest.contains("AccessibilityService", ignoreCase = true)) { "Quran Hifz must not package an Accessibility service." }
        check(!manifest.contains("<queries>")) { "Quran Hifz must not query or enumerate external applications." }
        check(!manifest.contains("QUERY_ALL_PACKAGES")) { "Quran Hifz must never request broad package visibility." }
        check(!manifest.contains("BIND_ACCESSIBILITY_SERVICE")) { "Quran Hifz must never request Safeguard blocking privileges." }

        // Quran Hifz is fully offline: no network-capable API may be referenced anywhere in this
        // module, and the manifest must never declare INTERNET/ACCESS_NETWORK_STATE.
        check(!manifest.contains("android.permission.INTERNET")) { "Quran Hifz must stay offline: no INTERNET permission." }
        val sourceFiles = fileTree("src/main") { include("**/*.java", "**/*.kt", "**/*.xml") }.files
        val networkMarkers = listOf(
            "com.google.mlkit", "RemoteModelManager", "DigitalInkRecognition",
            "java.net.HttpURLConnection", "java.net.URLConnection", "okhttp", "Retrofit", "WebSocket"
        )
        val filesWithNetworkMarkers = sourceFiles.filter { f -> networkMarkers.any { f.readText().contains(it) } }
        check(filesWithNetworkMarkers.isEmpty()) {
            "No network-capable code may exist in an offline app: found in ${filesWithNetworkMarkers.map { it.name }}"
        }

        val sourceText = sourceFiles.joinToString("\n") { it.readText() }
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
        check(main.contains("todayAction = todayLine()")
                && main.contains("line.setOnClickListener(v -> openToday())")
                && main.contains("if(sessions.isEmpty()){")
                && main.contains("showTodayWindow(sessions);")
                && main.contains("VOIR LES SÉANCES  ›")
                && !main.contains("if(sessions.size()<=1){ openMode(mode,scheduled); return; }")
                && !main.contains("\"Séance\", v -> openToday")) {
            "The approved BOOX Today hero must remain the single scheduled-session entry point."
        }
        val palaceHifz = listOf("door", "column", "buttress", "keystone", "torch", "mirror")
        check(palaceHifz.all { icon ->
            ui.contains("R.drawable.ic_palace_" + icon)
                && file("src/main/res/drawable/ic_palace_" + icon + ".xml").exists()
        }) {
            "Six unique BOOX memory-palace Hifz vectors must be mapped and bundled."
        }
        check(listOf("home", "route", "compass").all { icon ->
            ui.contains("R.drawable.ic_palace_" + icon)
                && file("src/main/res/drawable/ic_palace_" + icon + ".xml").exists()
        }) {
            "Grand Palais, Parcours and Atlas boussole must be distinct real vectors."
        }
        check(manifest.contains("android:icon=\"@drawable/ic_quran_hifz_header\"")
                && manifest.contains("android:roundIcon=\"@drawable/ic_quran_hifz_header\"")
                && file("src/main/res/drawable/ic_quran_hifz_header.xml").exists()
                && main.contains("appEmblem.setImageResource(R.drawable.ic_quran_hifz_header)")) {
            "Quran Hifz launcher and Home must share the clean Mushaf/rehal vector identity."
        }

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
        versionCode = 46
        versionName = if (haafidh1179IsolatedDebug) "1.17.9" else "1.17.8"
        if (boox1175Preview) versionName = "1.17.8"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    sourceSets.getByName("main").assets.srcDir(generatedHifzAssetsDir)
    if (private114) {
        // Isolated debug stays unchanged for parallel BOOX tests.
        sourceSets.getByName("debug").assets.srcDir("src/private114/assets")
        sourceSets.getByName("debug").manifest.srcFile("src/private114/AndroidManifest.xml")
        // An official upgrade must be a RELEASE, keeping defaultConfig.applicationId
        // without a suffix. Private assets are staged OFFLINE before packaging.
        // Do not overlay the private debug manifest/launcher label on release.
        if (privateReleaseUpgrade) sourceSets.getByName("release").assets.srcDir("src/private114/assets")
    }

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
            buildConfigField("boolean", "AYAH_THEMES_FINAL_PRIVATE", private114.toString())
            // Private BOOX preview: display user-approved wording without granting source/rights certification.
            buildConfigField("boolean", "ATLAS_PRIVATE_EDITORIAL_PREVIEW", private114.toString())
            isDebuggable = false
            // Isolated personal test install; never overwrites the official 1.17.1 package.
            applicationIdSuffix = if (private114) ".ayahthemes114private" else if (cp08BaqaraPilot) ".mawdudibaqara" else ".ibnkathirtest"
            if (boox1175Preview) applicationIdSuffix = ".boox1175debug"
            if (haafidh1179IsolatedDebug) applicationIdSuffix = ".haafidh1179debug"
            versionNameSuffix = if (haafidh1179IsolatedDebug) "-BOOX-TEST" else if (boox1175Preview) "-BOOX-HOME-PRIVATE-DEBUG" else if (private114) "-AyahThemes114-PRIVATE" else if (cp08BaqaraPilot) "-Mawdudi-Baqara-PILOT" else "-IbnKathir-DEBUG"
        }
        getByName("release") {
            // Private Atlas content is enabled only by an explicit local build flag.
            // The official package identity and launcher label remain unchanged.
            buildConfigField("boolean", "AYAH_THEMES_FINAL_PRIVATE", privateReleaseUpgrade.toString())
            buildConfigField("boolean", "ATLAS_PRIVATE_EDITORIAL_PREVIEW", privateReleaseUpgrade.toString())
            isMinifyEnabled = false
            isDebuggable = false
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
        }
    }
}

// A private official upgrade must contain the exact rights-scoped offline corpus.
// Refuse packaging when using the public CI checkout or the wrong signing setup.
val privateReleaseAssets = mapOf(
    "atlas/atlas_display_catalog_v1.json" to "065ec0dd68c488efacbd9e77635bf9d531b4cbb2b1bc82db7853ad4da0c02c9c",
    "atlas_private/atlas_366_comments_offline_reference_v1.json" to "8f514ffa367d67a49198799be279691ba0555363fac0b030f09fbf39fe3c3484",
    "atlas_private/atlas_approved_unit_titles_366_v1.json" to "421277c59519f2b904de23bbc1e0eee6faeb569e33ed6b425b4b9235cf82ff79",
    "atlas_private/maududi_original.pdf" to "1d207bb3dd58d45e7a77b0b5942f88a5c216b3a4273ea5a4474fc9e140b0d62e",
    "ayah_themes_private/macros_114.json" to "3589af556f82e55e1619fc0f3ae16b7f21b4609f156a8ad808a3a0bbe6d3c09a",
    "ayah_themes_private/special_27.json" to "d3fb2a7cc23f84851f83caa14ab23a549b8f410529930131dd2c2560d7450404",
    "ayah_themes_private/titles_114.json" to "a42914ae968c77bc7224f5126a41cc3e92099fbc797a91b01d74405d242eb209"
)
val verifyPrivateReleaseInputs by tasks.registering {
    doLast {
        if (!privateReleaseUpgrade) return@doLast
        check(hasReleaseSigning) { "NO-GO: official upgrade requires the official release signing keystore (never the private debug key)." }
        val storePath = file(System.getenv("HIFZ_KEYSTORE_PATH"))
        val storePassword = System.getenv("HIFZ_KEYSTORE_PASSWORD") ?: error("NO-GO: missing release keystore password")
        val alias = System.getenv("HIFZ_KEY_ALIAS") ?: error("NO-GO: missing release signing alias")
        check(storePath.isFile) { "NO-GO: missing release keystore file" }
        val certificate = KeyStore.getInstance(storePath, storePassword.toCharArray())
            .getCertificate(alias) ?: error("NO-GO: release keystore alias unavailable")
        val certSha256 = MessageDigest.getInstance("SHA-256").digest(certificate.encoded)
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
        // Fingerprint from the actual signed official 1.17.1 GitHub Actions run #37824956514.
        check(certSha256 == "66adc4f0dcf70d716a8050fd4b0195266307576aa2f9bd54c9cbf2851a330e14") {
            "NO-GO: signing certificate differs from official Quran Haafidh 1.17.1"
        }
        privateReleaseAssets.forEach { (relative, expected) ->
            val asset = file("src/private114/assets/$relative")
            check(asset.isFile) { "NO-GO: missing private Atlas asset: $relative" }
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(asset.readBytes()).joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
            check(digest == expected) { "NO-GO: private Atlas asset checksum mismatch: $relative" }
        }
        println("PASS: 7/7 private Atlas resources match audited SHA-256; official release keystore path configured")
    }
}
tasks.matching { it.name == "packageRelease" }.configureEach {
    dependsOn(verifyPrivateReleaseInputs)
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
    // JVM tests load the real KFQC geometry and V6 JSON state; android.jar's org.json is a stub.
    testImplementation("org.json:json:20240303")
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
