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
    }
}

// A14 excludes gradle.properties; AndroidX is used only by instrumentation tests.
gradle.beforeProject {
    extensions.extraProperties.set("android.useAndroidX", true)
}

rootProject.name = "OsakaConnect"
include(":app")
