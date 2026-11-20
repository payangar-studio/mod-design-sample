// Managed by mc-cli: `mod update` rewrites this file.
//
// The convention plugins of the mod's build. The versions of the Gradle
// plugins come from .mod/socle.toml, pinned for this branch's Minecraft
// version.
import org.tomlj.Toml

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.tomlj:tomlj:2.2.0")
    }
}

plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(25)
}

val socle = Toml.parse(rootDir.resolve("../.mod/socle.toml").toPath())
check(!socle.hasErrors()) { "../.mod/socle.toml is invalid: ${socle.errors()}" }

fun pinned(key: String): String =
    socle.getString("init.$key") ?: error("../.mod/socle.toml has no `$key` under [init]")

dependencies {
    implementation("org.tomlj:tomlj:2.2.0")
    implementation("net.fabricmc.fabric-loom:net.fabricmc.fabric-loom.gradle.plugin:${pinned("loom")}")
    implementation("net.neoforged.moddev:net.neoforged.moddev.gradle.plugin:${pinned("moddevgradle")}")
    implementation("me.modmuss50.mod-publish-plugin:me.modmuss50.mod-publish-plugin.gradle.plugin:${pinned("mod-publish-plugin")}")
}
