plugins {
    id("com.android.application")
}

val smokeMushaf = providers.gradleProperty("hifzSmokeMushaf")
    .orNull
    ?.toBooleanStrictOrNull()
    ?: false

val canonicalMushafSource = rootProject.file(
    "third_party/quran-svg/mushafs/hafs/kfqc/svg-br"
)
val generatedMushafAssets = layout.buildDirectory.dir("generated/hifzMushafAssets")

val prepareHifzMushafAssets by tasks.registering(Sync::class) {
    val expectedNames = (1..604).map { "%03d.svg.br".format(it) }

    doFirst {
        require(canonicalMushafSource.isDirectory) {
            "Pinned quran-svg submodule is missing. Run: git submodule update --init --recursive"
        }
        val actualNames = canonicalMushafSource
            .listFiles { file -> file.isFile && file.name.endsWith(".svg.br") }
            ?.map { it.name }
            ?.sorted()
            ?: emptyList()
        require(actualNames == expectedNames) {
            "Expected the canonical KFQC 604-page set in third_party/quran-svg; found ${actualNames.size} pages"
        }
    }

    from(canonicalMushafSource) {
        if (smokeMushaf) {
            include("001.svg.br", "002.svg.br")
        } else {
            include("*.svg.br")
        }
        into("mushaf/hafs/kfqc/svg-br")
    }
    into(generatedMushafAssets)
}

android {
    namespace = "com.quransafeguard.hifz"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.quransafeguard.hifz"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1-local-first"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    sourceSets.getByName("main").assets.srcDir(generatedMushafAssets)

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}

tasks.matching { task ->
    task.name.startsWith("merge") && task.name.endsWith("Assets")
}.configureEach {
    dependsOn(prepareHifzMushafAssets)
}

dependencies {
    implementation(project(":hifz-core"))
    implementation("org.brotli:dec:0.1.2")
    implementation("com.caverock:androidsvg-aar:1.4")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("junit:junit:4.13.2")
}
