// Managed by mc-cli: `mod update` rewrites this file. The build logic lives in
// build-logic/; the mod's identity and loaders come from mod.toml.
pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("mod.settings")
}
