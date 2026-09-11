plugins {
    id("com.android.application")
}

val generatedHifzAssetsDir = layout.buildDirectory.dir("generated/hifzAssets").get().asFile

val prepareHifzAssets by tasks.registering(Sync::class) {
    into(generatedHifzAssetsDir)
    from(rootProject.file("app/src/main/assets/mushaf")) { into("mushaf") }
    from(rootProject.file("app/src/main/assets/reader109/geometry.json")) { into("reader109") }
    from(rootProject.file("app/src/main/assets/reader109/audio.json")) { into("reader109") }
    from(rootProject.file("app/src/plus/assets/tafsir")) { into("tafsir") }
}

val verifyHifzProductBoundary by tasks.registering {
    doLast {
        val manifest = file("src/main/AndroidManifest.xml").readText()
        check(!manifest.contains("AccessibilityService", ignoreCase = true)) { "Quran Hifz must not package an Accessibility service." }
        check(!manifest.contains("<queries>")) { "Quran Hifz must not query or enumerate external applications." }
        check(!manifest.contains("QUERY_ALL_PACKAGES")) { "Quran Hifz must never request broad package visibility." }
        check(!manifest.contains("BIND_ACCESSIBILITY_SERVICE")) { "Quran Hifz must never request Safeguard blocking privileges." }
        val sourceText = fileTree("src/main") { include("**/*.java", "**/*.kt", "**/*.xml") }.files.joinToString("\n") { it.readText() }
        listOf("QuranAccessibilityService","ProtectedApps","GuardPrefs.protectedPackages","UsageCyclePolicy","UnlockBudgetIntegrity").forEach { forbidden ->
            check(!sourceText.contains(forbidden)) { "Safeguard-only symbol leaked into Quran Hifz: $forbidden" }
        }
    }
}

android {
    namespace = "com.quransafeguard.hifz.preview"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.quransafeguard.hifz.installtest1"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "0.5-tablet-tafsir-test"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    sourceSets.getByName("main").assets.srcDir(generatedHifzAssetsDir)

    buildTypes {
        getByName("debug") {
            // Exact personal test package; no suffix so the candidate can replace the prior test when signatures match.
        }
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn(prepareHifzAssets)
    dependsOn(verifyHifzProductBoundary)
}

dependencies {
    implementation(project(":hifz-core"))
    implementation("org.brotli:dec:0.1.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("junit:junit:4.13.2")
}
