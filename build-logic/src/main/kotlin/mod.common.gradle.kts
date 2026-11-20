// Managed by mc-cli: `mod update` rewrites this file.
//
// `common`: the code shared by the loaders, compiled against vanilla Minecraft
// only. Loom splits it into `main` (both sides) and `client`: code in `main`
// that uses a client-only Minecraft class does not compile.

import net.fabricmc.loom.api.LoomGradleExtensionAPI

plugins {
    id("mod.java")
    id("net.fabricmc.fabric-loom")
}

val mod = ModToml.read(rootDir.resolve("mod.toml"))
val sourceSets = the<SourceSetContainer>()
val pinned = Pinned.read(rootDir.resolve(".mod/socle.toml"))

extensions.configure<LoomGradleExtensionAPI> {
    splitEnvironmentSourceSets()
}

dependencies {
    "minecraft"("com.mojang:minecraft:${pinned.minecraft}")
}

exportCommon(sourceSets["main"])
exportCommon(sourceSets["client"])
// Dev runs load the mod from common's resources too; the loaders write the
// library's strings of a locale the mod has no file for.
mergeVendorLang(mod.id, listOf("processResources", "processClientResources"))

// The loaders ship common's classes in their own jars.
tasks.named<Jar>("jar") {
    enabled = false
}
