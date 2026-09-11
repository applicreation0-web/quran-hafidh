import java.io.SequenceInputStream
import java.security.MessageDigest
import java.util.Collections
import java.util.zip.GZIPInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val verifySafeguardProductBoundary by tasks.registering {
    doLast {
        val manifest = file("src/main/AndroidManifest.xml").readText()
        val hub = file("src/main/java/com/quranunlock/guard/QuranHubActivity.kt").readText()
        val study = file("src/main/java/com/quranunlock/guard/SafeguardStudyReaderActivity.kt").readText()
        val accessibility = file("src/plus/res/xml/accessibility_service_config.xml").readText()
        val protectedApps = file("src/main/java/com/quranunlock/guard/ProtectedApps.kt").readText()

        listOf(
            "HifzJourneyActivity",
            "HifzSetupActivity",
            "FreeQuranReaderActivity"
        ).forEach { forbidden ->
            check(!manifest.contains(forbidden)) { "$forbidden must not be packaged in Quran Safeguard TEST." }
            check(!hub.contains(forbidden)) { "$forbidden must not be reachable from the Quran hub." }
        }
        check(!hub.contains("Mémorisation") && !hub.contains("Parcours Hifz")) {
            "Quran Safeguard TEST must expose Study/Tafsir only in its voluntary Quran hub."
        }
        check(!study.contains("Hifz") && !study.contains("Memor") && !study.contains("QuranAudio")) {
            "The Safeguard Study reader must not carry Hifz or memorization state."
        }
        check(!file("src/main/java/com/quranunlock/guard/FreeQuranReaderActivity.kt").exists())
        check(fileTree("src/main/java/com/quranunlock/guard") {
            include("Hifz*.kt")
        }.files.isEmpty()) { "Structured Hifz source files leaked into Safeguard TEST." }
        check(!file("src/main/assets/reader109/hifz_guard.js").exists())

        check(!manifest.contains("android.permission.QUERY_ALL_PACKAGES")) {
            "Broad package visibility is forbidden."
        }
        check(accessibility.contains("android:canRetrieveWindowContent=\"false\""))
        check(accessibility.contains("com.quransafeguard.safeguard.test"))

        val targets = listOf(
            "com.whatsapp",
            "com.twitter.android",
            "com.instagram.android",
            "com.facebook.katana",
            "com.google.android.youtube",
            "com.zhiliaoapp.musically",
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.microsoft.emmx",
            "com.brave.browser",
            "com.opera.browser",
            "com.sec.android.app.sbrowser",
            "com.duckduckgo.mobile.android",
            "com.vivaldi.browser"
        )
        check(targets.size == 14 && targets.toSet().size == 14)
        targets.forEach { packageName ->
            check(protectedApps.contains(packageName)) { "Missing fixed target $packageName" }
            check(manifest.contains("<package android:name=\"$packageName\" />")) {
                "Missing narrow query declaration for $packageName"
            }
            check(accessibility.contains(packageName)) {
                "Missing static Accessibility target $packageName"
            }
        }
    }
}

val verifySafeguardMushafPages by tasks.registering {
    doLast {
        val mushafDir = file("src/main/assets/mushaf/hafs/kfqc/svg-br")
        val pages = mushafDir.listFiles { candidate ->
            candidate.isFile && candidate.name.matches(Regex("\\d{3}\\.svg\\.br"))
        }.orEmpty()
        check(pages.size == 604) { "Quran Safeguard TEST requires exactly 604 canonical Mushaf pages." }
        (1..604).forEach { page ->
            check(mushafDir.resolve("%03d.svg.br".format(page)).isFile) {
                "Missing canonical Mushaf page $page"
            }
        }
        val policy = file("src/main/java/com/quranunlock/guard/ReadingValidationPolicy.kt").readText()
        val reader = file("src/main/java/com/quranunlock/guard/MushafReaderActivity.kt").readText()
        check(policy.contains("MIN_ACTIVE_READING_MS = 60_000L"))
        check(reader.contains("ReadingValidationPolicy.canValidate"))
        check(reader.contains("TargetReturnCoordinator.returnImmediately"))
    }
}

val verifySafeguardTafsirCorpus by tasks.registering {
    doLast {
        val archiveParts = (0..3).map { index ->
            file("src/plus/assets/tafsir/al_jalalayn_en.sqlite.gz.part%02d".format(index))
        }
        check(archiveParts.all { it.isFile && it.length() > 0L }) {
            "Quran Safeguard TEST requires the approved Tafsir corpus."
        }
        val digest = MessageDigest.getInstance("SHA-256")
        val streams = archiveParts.map { it.inputStream() }
        GZIPInputStream(SequenceInputStream(Collections.enumeration(streams))).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        check(actual == "26d8715a9bcecda6cb6397f0d8a530cb9404bb69ba66ed5264ed3f5b16d11a56") {
            "The packaged Safeguard Tafsir corpus is not the approved database."
        }
    }
}

android {
    namespace = "com.applicreation0.quransafeguard"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.quransafeguard.safeguard.test"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "test-1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "edition"
    productFlavors {
        create("light") {
            dimension = "edition"
            applicationIdSuffix = ".light"
        }
        create("plus") {
            dimension = "edition"
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-ktx:1.17.0")
    add("plusImplementation", "androidx.webkit:webkit:1.16.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("org.brotli:dec:0.1.2")
    implementation("com.batoulapps.adhan:adhan2:0.0.7")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:core-ktx:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.2.1")
}

tasks.matching { it.name == "assemblePlusDebug" }.configureEach {
    dependsOn(verifySafeguardProductBoundary)
    dependsOn(verifySafeguardMushafPages)
    dependsOn(verifySafeguardTafsirCorpus)
}
