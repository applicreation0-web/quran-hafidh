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
val auditedTafsirSource = rootProject.file("app/src/plus/assets/tafsir")
val generatedHifzAssets = layout.buildDirectory.dir("generated/hifzAssets").get().asFile
val canonicalPageNames = (1..604).map { String.format(Locale.ROOT, "%03d.svg.br", it) }
val canonicalGeometryNames = (1..604).map { String.format(Locale.ROOT, "%03d.json", it) }
val jalalaynTafsirNames = (0..3).map { String.format(Locale.ROOT, "al_jalalayn_en.sqlite.gz.part%02d", it) }
val qurtubiTafsirNames = (0..3).map { String.format(Locale.ROOT, "qurtubi_en.sqlite.gz.b64.part%02d", it) }
val qushayriTafsirNames = (0..1).map { String.format(Locale.ROOT, "qushayri_en.sqlite.gz.b64.part%02d", it) }
val auditedTafsirNames = jalalaynTafsirNames + qurtubiTafsirNames + qushayriTafsirNames
val smokeMushaf = providers.gradleProperty("hifzSmokeMushaf")
    .orNull
    ?.toBooleanStrictOrNull()
    ?: false

fun checkedGitOutput(vararg args: String): String {
    val output = ByteArrayOutputStream()
    val process = ProcessBuilder(listOf("git", "-C", quranSvgSubmodule.absolutePath) + args)
        .redirectErrorStream(true)
        .start()
    process.inputStream.copyTo(output)
    val exit = process.waitFor()
    val text = output.toString(Charsets.UTF_8).trim()
    if (exit != 0) throw GradleException("git ${args.joinToString(" ")} failed for pinned quran-svg: $text")
    return text
}

val verifyHifzMushafSource by tasks.registering {
    group = "verification"
    description = "Verifies the pinned exact 604-page KFQC Mushaf, geometry and audited three-Tafsir inputs."
    doLast {
        if (!quranSvgSubmodule.isDirectory || !canonicalMushafSource.isDirectory || !canonicalGeometrySource.isDirectory) {
            throw GradleException("Pinned quran-svg submodule is missing or incomplete. Run: git submodule update --init --recursive")
        }
        val actualCommit = checkedGitOutput("rev-parse", "HEAD")
        if (actualCommit != pinnedQuranSvgCommit) {
            throw GradleException("Unexpected quran-svg commit: $actualCommit; expected $pinnedQuranSvgCommit")
        }
        if (checkedGitOutput("status", "--porcelain", "--untracked-files=no").isNotEmpty()) {
            throw GradleException("Pinned quran-svg source has tracked local modifications; refusing to build")
        }

        val actualSvg = canonicalMushafSource
            .listFiles { f -> f.isFile && Regex("\\d{3}\\.svg\\.br").matches(f.name) }
            ?.map { it.name }?.sorted() ?: emptyList()
        if (actualSvg != canonicalPageNames) {
            throw GradleException("Expected exact canonical 001.svg.br..604.svg.br; found ${actualSvg.size} numbered pages")
        }

        val actualGeometry = canonicalGeometrySource
            .listFiles { f -> f.isFile && Regex("\\d{3}\\.json").matches(f.name) }
            ?.map { it.name }?.sorted() ?: emptyList()
        if (actualGeometry != canonicalGeometryNames) {
            throw GradleException("Expected exact geometry 001.json..604.json; found ${actualGeometry.size} numbered pages")
        }

        val missingTafsir = auditedTafsirNames.filterNot { auditedTafsirSource.resolve(it).isFile }
        if (missingTafsir.isNotEmpty()) {
            throw GradleException("Audited three-Tafsir asset set is incomplete: ${missingTafsir.joinToString()}")
        }
    }
}

val prepareHifzAssets by tasks.registering(Sync::class) {
    dependsOn(verifyHifzMushafSource)
    from(canonicalMushafSource) {
        if (smokeMushaf) {
            include("001.svg.br", "002.svg.br")
        } else {
            include(*canonicalPageNames.toTypedArray())
        }
        into("mushaf/hafs/kfqc/svg-br")
    }
    from(canonicalGeometrySource) {
        if (smokeMushaf) {
            include("001.json", "002.json")
        } else {
            include(*canonicalGeometryNames.toTypedArray())
        }
        into("geometry/hafs/kfqc")
    }
    from(auditedTafsirSource) {
        include(*auditedTafsirNames.toTypedArray())
        into("tafsir")
    }
    into(generatedHifzAssets)
}

android {
    namespace = "com.quransafeguard.hifz"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.quransafeguard.hifz"
        minSdk = 26
        targetSdk = 36
        versionCode = 10
        versionName = "0.10.10-hifz"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    sourceSets.getByName("main").assets.srcDir(generatedHifzAssets)
    buildTypes { getByName("release") { isMinifyEnabled = false } }
}

tasks.named("preBuild").configure { dependsOn(prepareHifzAssets) }

dependencies {
    implementation(project(":hifz-core"))
    implementation("org.brotli:dec:0.1.2")
    implementation("com.caverock:androidsvg-aar:1.4")
    implementation("com.onyx.android.sdk:onyxsdk-device:1.3.5")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("junit:junit:4.13.2")
}
