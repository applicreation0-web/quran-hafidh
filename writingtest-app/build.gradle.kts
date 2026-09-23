// Standalone writing-exercise test app — NOT part of Quran Hifz. Lets the writing-exercise
// mechanics (real letterform trajectory scoring, ayah-end markers, word-group selection) be
// installed and tested independently of the real app, across ANY line in the whole Mushaf corpus,
// without waiting on a hifz-app release. Deliberately isolated in its own module, same pattern as
// inktest-app: it never touches hifz-app's offline-first boundary contract.
plugins {
    id("com.android.application")
}

val generatedWritingTestAssetsDir = layout.buildDirectory.dir("generated/writingTestAssets").get().asFile
val generatedWordShapesDir = layout.buildDirectory.dir("generated/writingTestWordShapes").get().asFile
val generatedAyahMarkersDir = layout.buildDirectory.dir("generated/writingTestAyahMarkers").get().asFile
val svgBrSourceDir = rootProject.file("app/src/main/assets/mushaf/hafs/kfqc/svg-br")
val geometrySourceFile = rootProject.file("app/src/main/assets/reader109/geometry.json")

val generateWordShapes by tasks.registering(Exec::class) {
    inputs.dir(svgBrSourceDir)
    inputs.file(geometrySourceFile)
    inputs.file(rootProject.file("scripts/generate_hifz_word_shapes.py"))
    outputs.dir(generatedWordShapesDir)
    commandLine(
        "python3",
        rootProject.file("scripts/generate_hifz_word_shapes.py").absolutePath,
        svgBrSourceDir.absolutePath,
        geometrySourceFile.absolutePath,
        generatedWordShapesDir.absolutePath
    )
}

val generateAyahMarkers by tasks.registering(Exec::class) {
    inputs.dir(svgBrSourceDir)
    inputs.file(rootProject.file("scripts/generate_hifz_ayah_markers.py"))
    outputs.dir(generatedAyahMarkersDir)
    commandLine(
        "python3",
        rootProject.file("scripts/generate_hifz_ayah_markers.py").absolutePath,
        svgBrSourceDir.absolutePath,
        generatedAyahMarkersDir.absolutePath
    )
}

val prepareWritingTestAssets by tasks.registering(Sync::class) {
    dependsOn(generateWordShapes, generateAyahMarkers)
    into(generatedWritingTestAssetsDir)
    from(rootProject.file("app/src/main/assets/mushaf")) { into("mushaf") }
    from(rootProject.file("app/src/main/assets/reader109/geometry.json")) { into("reader109") }
    from(rootProject.file("app/src/main/assets/reader109/verses_text.json")) { into("reader109") }
    from(generatedWordShapesDir) { into("wordshapes") }
    from(generatedAyahMarkersDir) { into("ayahmarkers") }
}

android {
    namespace = "com.quransafeguard.writingtest"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.quransafeguard.writingtest"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1-writingtest"
    }

    sourceSets.getByName("main").assets.srcDir(generatedWritingTestAssetsDir)

    buildTypes {
        getByName("debug") {
            isDebuggable = true
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn(prepareWritingTestAssets)
}

dependencies {
    implementation(project(":hifz-core"))
}
