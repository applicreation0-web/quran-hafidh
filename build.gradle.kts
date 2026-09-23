buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21")
    }
}

plugins {
    id("com.android.application") version "9.3.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
}

// JVM unit tests must execute the same real org.json semantics used by Android.
// Without this test-only implementation, android.jar exposes throwing stubs and
// migration/state-sanitizer tests fail before exercising application logic.
subprojects {
    plugins.withId("com.android.application") {
        dependencies.add("testImplementation", "org.json:json:20240303")
    }
}
