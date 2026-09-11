plugins {
    id("com.android.application")
}

val generatedHifzAssets = layout.buildDirectory.dir("generated/hifzAssets")

val prepareHifzAssets by tasks.registering(Sync::class) {
    into(generatedHifzAssets)
    from(rootProject.file("app/src/main/assets/mushaf")) { into("mushaf") }
    from(rootProject.file("app/src/main/assets/reader109/geometry.json")) { into("reader109") }
    from(rootProject.file("app/src/plus/assets/tafsir")) { into("tafsir") }
    from("src/main/assets/hifzreader") { into("hifzreader") }
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
    }

    sourceSets.getByName("main").assets.srcDir(generatedHifzAssets)

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
}
