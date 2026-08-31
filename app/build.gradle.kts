import java.net.URI
import java.security.MessageDigest
import java.util.zip.ZipFile

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val mushafSourceUrl =
    "https://download.qurancomplex.gov.sa/resources_dev/UthmanicHafs_v2-0.zip"
val mushafExpectedSha1 = "36EA5AB0D7EA1702F17FF43F9B50924CCCD77EBF"
val generatedMushafDir = layout.buildDirectory.dir("generated/mushafAssets")

val prepareMushafData by tasks.registering {
    val outputFile = generatedMushafDir.map {
        it.file("mushaf/hafsData_v2-0.json")
    }
    outputs.file(outputFile)

    doLast {
        val output = outputFile.get().asFile
        val download = layout.buildDirectory
            .file("downloads/UthmanicHafs_v2-0.zip")
            .get()
            .asFile

        if (!download.exists()) {
            download.parentFile.mkdirs()
            val connection = URI.create(mushafSourceUrl).toURL().openConnection().apply {
                connectTimeout = 30_000
                readTimeout = 120_000
            }
            connection.getInputStream().use { input ->
                download.outputStream().use { outputStream ->
                    input.copyTo(outputStream)
                }
            }
        }

        val sha1 = MessageDigest.getInstance("SHA-1")
            .digest(download.readBytes())
            .joinToString("") { "%02X".format(it) }

        check(sha1.equals(mushafExpectedSha1, ignoreCase = true)) {
            "Official Mushaf archive integrity check failed. Expected " +
                mushafExpectedSha1 + " but got " + sha1
        }

        output.parentFile.mkdirs()
        ZipFile(download).use { archive ->
            val entries = archive.entries().asSequence().toList()
            val entry = entries.firstOrNull {
                it.name.endsWith("hafsData_v2-0.json", ignoreCase = true)
            } ?: error("Official archive does not contain hafsData_v2-0.json")

            archive.getInputStream(entry).use { input ->
                output.outputStream().use { outputStream ->
                    input.copyTo(outputStream)
                }
            }
        }

        val text = output.readText(Charsets.UTF_8)
        check(Regex("\"page\"\\s*:\\s*1").containsMatchIn(text)) {
            "Mushaf page 1 was not found in official data."
        }
        check(Regex("\"page\"\\s*:\\s*604").containsMatchIn(text)) {
            "Mushaf page 604 was not found in official data."
        }
        check(text.contains("\"aya_text\"")) {
            "Quranic text field is missing from official data."
        }
        check(Regex("\"aya_no\"").findAll(text).count() >= 6236) {
            "Official Mushaf data appears incomplete."
        }
    }
}

android {
    namespace = "com.quranunlock.guard"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.quranunlock.guard"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.2.0"
    }

    buildFeatures {
        compose = true
    }

    sourceSets.getByName("main").assets.srcDir(generatedMushafDir)
}

tasks.named("preBuild").configure {
    dependsOn(prepareMushafData)
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
