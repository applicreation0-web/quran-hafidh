plugins {
    id("com.android.application")
}

val generatedHifzAssetsDir = layout.buildDirectory.dir("generated/hifzAssets").get().asFile

val prepareHifzAssets by tasks.registering(Sync::class) {
    into(generatedHifzAssetsDir)
    from(rootProject.file("app/src/main/assets/mushaf")) { into("mushaf") }
    from(rootProject.file("app/src/main/assets/reader109/geometry.json")) { into("reader109") }
    from(rootProject.file("app/src/main/assets/reader109/hifz-index.json")) { into("reader109") }
    from(rootProject.file("app/src/main/assets/reader109/hifz-pages")) { into("reader109/hifz-pages") }
    from(rootProject.file("app/src/main/assets/reader109/audio.json")) { into("reader109") }
    from(rootProject.file("app/src/plus/assets/tafsir")) { into("tafsir") }
}

android {
    namespace = "com.quransafeguard.hifz.preview"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.quransafeguard.hifz.preview"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.2-full-scope-preview"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // AGP 9 rejects Provider<Directory> through the legacy SourceSet API.
    // Resolve the build directory eagerly here; preBuild carries the task dependency explicitly.
    sourceSets.getByName("main").assets.srcDir(generatedHifzAssetsDir)

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}

tasks.named("preBuild").configure { dependsOn(prepareHifzAssets) }

dependencies {
    implementation(project(":hifz-core"))
    implementation("org.brotli:dec:0.1.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("junit:junit:4.13.2")
}
