import org.gradle.api.GradleException
import org.gradle.api.tasks.Sync
import java.io.ByteArrayOutputStream
import java.util.Locale

plugins {
    id("com.android.application")
}

val pinnedQuranSvgCommit = "1b427fab77aae1403fe7e1f0b8c794a5384d5605"
val quranSvgSubmodule = rootProject.file("third_party/quran-svg")
val canonicalMushafSource = quranSvgSubmodule.resolve("mushafs/hafs/kfqc/svg-br")
val canonicalGeometrySource = quranSvgSubmodule.resolve("mushafs/hafs/kfqc/json")
val generatedMushafAssets = layout.buildDirectory.dir("generated/hifzMushafAssets")
val smokeMushaf = providers.gradleProperty("hifzSmokeMushaf")
    .orNull
    ?.toBooleanStrictOrNull()
    ?: false

fun checkedGitOutput(vararg args: String): String {
    val output = ByteArrayOutputStream()
    val process = ProcessBuilder(
        listOf("git", "-C", quranSvgSubmodule.absolutePath) + args
    )
        .redirectErrorStream(true)
        .start()
    process.inputStream.copyTo(output)
    val exit = process.waitFor()
    val text = output.toString(Charsets.UTF_8).trim()
    if (exit != 0) {
        throw GradleException("git ${args.joinToString(" ")} failed for pinned quran-svg: $text")
    }
    return text
}

val verifyHifzMushafSource by tasks.registering {
    group = "verification"
    description = "Verifies the pinned, clean, exact 604-page local KFQC Mushaf and geometry source."

    doLast {
        if (!quranSvgSubmodule.isDirectory || !canonicalMushafSource.isDirectory || !canonicalGeometrySource.isDirectory) {
            throw GradleException(
                "Pinned quran-svg submodule is missing or incomplete. Run: git submodule update --init --recursive"
            )
        }

        val actualCommit = checkedGitOutput("rev-parse", "HEAD")
        if (actualCommit != pinnedQuranSvgCommit) {
            throw GradleException(
                "Unexpected quran-svg commit: $actualCommit; expected $pinnedQuranSvgCommit"
            )
        }

        val trackedChanges = checkedGitOutput("status", "--porcelain", "--untracked-files=no")
        if (trackedChanges.isNotEmpty()) {
            throw GradleException("Pinned quran-svg source has tracked local modifications; refusing to build")
        }

        val expectedSvgNames = (1..604).map {
            String.format(Locale.ROOT, "%03d.svg.br", it)
        }
        val actualSvgNames = canonicalMushafSource
            .listFiles { file -> file.isFile && file.name.endsWith(".svg.br") }
            ?.map { it.name }
            ?.sorted()
            ?: emptyList()
        if (actualSvgNames != expectedSvgNames) {
            throw GradleException(
                "Expected canonical KFQC files 001.svg.br..604.svg.br; found ${actualSvgNames.size} pages"
            )
        }

        val expectedGeometryNames = (1..604).map {
            String.format(Locale.ROOT, "%03d.json", it)
        }
        val actualGeometryNames = canonicalGeometrySource
            .listFiles { file -> file.isFile && Regex("\\d{3}\\.json").matches(file.name) }
            ?.map { it.name }
            ?.sorted()
            ?: emptyList()
        if (actualGeometryNames != expectedGeometryNames) {
            throw GradleException(
                "Expected canonical KFQC geometry 001.json..604.json; found ${actualGeometryNames.size} pages"
            )
        }
    }
}

val prepareHifzMushafAssets by tasks.registering(Sync::class) {
    dependsOn(verifyHifzMushafSource)

    from(canonicalMushafSource) {
        if (smokeMushaf) {
            include("001.svg.br", "002.svg.br")
        } else {
            include("*.svg.br")
        }
        into("mushaf/hafs/kfqc/svg-br")
    }
    from(canonicalGeometrySource) {
        if (smokeMushaf) {
            include("001.json", "002.json")
        } else {
            include(Regex("\\d{3}\\.json"))
        }
        into("geometry/hafs/kfqc")
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
