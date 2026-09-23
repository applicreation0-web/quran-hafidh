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
    // Higher-fidelity sampling than hifz-app's copy (which uses the script's coverage-check
    // defaults): this test app exists to validate whether more shape detail makes the Palier 3
    // trajectory score more discriminating, so only it opts into the denser sampling for now.
    commandLine(
        "python3",
        rootProject.file("scripts/generate_hifz_word_shapes.py").absolutePath,
        svgBrSourceDir.absolutePath,
        geometrySourceFile.absolutePath,
        generatedWordShapesDir.absolutePath,
        "--min-samples", "16",
        "--max-samples", "48",
        "--samples-per-unit-length", "2.5"
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
    // Content verification (which word was actually written), complementing the geometric
    // Palier 3 trajectory score above, which is shape-only and blind to word identity/order.
    implementation("com.google.mlkit:digital-ink-recognition:18.1.0")
}
