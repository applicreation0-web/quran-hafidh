pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("http://repo.boox.com/repository/maven-public/")
            isAllowInsecureProtocol = true
            content {
                includeGroup("com.onyx.android.sdk")
            }
        }
    }
}

rootProject.name = "QuranSafeguard"
include(":app")
include(":hifz-core")
include(":safeguard-core")
include(":hifz-app")
