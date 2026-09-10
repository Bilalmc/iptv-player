import org.gradle.api.initialization.resolve.RepositoriesMode

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            name = "OwnTVCore"
            url = uri("https://maven.pkg.github.com/ahXN00/OwnTV_Core")
            credentials {
                username = providers.gradleProperty("gpr.user")
                    .orElse(providers.environmentVariable("GITHUB_ACTOR")).orNull
                password = providers.gradleProperty("gpr.token")
                    .orElse(providers.environmentVariable("GPR_TOKEN")).orNull
            }
            content { includeGroup("tv.own.owntv") }
        }
    }
}

val upstreamCatalog = file("upstream/OwnTV/gradle/libs.versions.toml")
if (upstreamCatalog.isFile) {
    dependencyResolutionManagement.versionCatalogs.maybeCreate("libs").from(files(upstreamCatalog))
}

rootProject.name = "IPTV Player"

include(":product-ui")
include(":app")
project(":app").projectDir = file("upstream/OwnTV/app")

val baselineDir = file("upstream/OwnTV/baselineprofile")
if (baselineDir.isDirectory) {
    include(":baselineprofile")
    project(":baselineprofile").projectDir = baselineDir
}

val localCore = file("upstream/OwnTV_Core")
if (localCore.isDirectory) {
    includeBuild(localCore)
}

providers.gradleProperty("owntv.corePath").orNull
    ?.takeIf { it.isNotBlank() }
    ?.let { includeBuild(it) }
