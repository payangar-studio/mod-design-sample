// Managed by mc-cli: `mod update` rewrites this file.
//
// Names the build after the mod and includes one subproject per loader of
// mod.toml, plus `common` when the mod has more than one loader.

val mod = ModToml.read(rootDir.resolve("mod.toml"))

rootProject.name = mod.id
if (mod.loaders.size > 1) {
    include("common")
}
mod.loaders.forEach { include(it) }
