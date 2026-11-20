// Managed by mc-cli: `mod update` rewrites this file.
//
// The values that fill the loader metadata templates (build-logic/templates/),
// derived from mod.toml and the pinned versions. The entrypoint classes are
// those `mod init` writes: `<package>.fabric.FabricEntrypoint` and
// `<package>.fabric.client.FabricClientEntrypoint`; and Mod Menu's, from the
// shared library: `<package>.vendor.fabric.ModMenuEntrypoint`.

object ModMetadata {
    /**
     * Values of build-logic/templates/fabric.mod.json. `modMenu`: whether the
     * sources hold the library's Mod Menu entrypoint.
     */
    fun fabric(mod: ModToml, pinned: Pinned, version: String, modMenu: Boolean): Map<String, String> {
        val entrypoints = buildList {
            add("\"main\": [${json("${mod.pkg}.fabric.FabricEntrypoint")}]")
            if (mod.hasClient) add("\"client\": [${json("${mod.pkg}.fabric.client.FabricClientEntrypoint")}]")
            if (mod.hasClient && modMenu) add("\"modmenu\": [${json("${mod.pkg}.vendor.fabric.ModMenuEntrypoint")}]")
        }
        val depends = linkedMapOf(
            "fabricloader" to ">=${pinned.fabricLoader}",
            "minecraft" to "~${pinned.minecraft}",
            "java" to ">=25",
        )
        val deps = mod.dependenciesOf("fabric")
        deps.filter { it.type == "required" }.forEach { depends[it.id] = "*" }
        return mapOf(
            "id" to escapeJson(mod.id),
            "name" to escapeJson(mod.name),
            "version" to escapeJson(version),
            "license" to escapeJson(mod.license),
            "environment" to when (mod.side) {
                "client" -> "client"
                "server" -> "server"
                else -> "*"
            },
            "entrypoints" to "{ ${entrypoints.joinToString(", ")} }",
            "depends" to jsonObject(depends),
            "suggests" to jsonObject(deps.filter { it.type == "optional" }.associate { it.id to "*" }),
            "breaks" to jsonObject(deps.filter { it.type == "incompatible" }.associate { it.id to "*" }),
        )
    }

    /** Values of build-logic/templates/neoforge.mods.toml. */
    fun neoforge(mod: ModToml, pinned: Pinned, version: String): Map<String, String> {
        val side = when (mod.side) {
            "client" -> "CLIENT"
            "server" -> "SERVER"
            else -> "BOTH"
        }
        fun dependency(modId: String, type: String, versionRange: String?): String = buildString {
            append("[[dependencies.${mod.id}]]\n")
            append("modId = ${toml(modId)}\n")
            append("type = ${toml(type)}\n")
            if (versionRange != null) append("versionRange = ${toml(versionRange)}\n")
            append("side = ${toml(side)}\n")
        }
        val dependencies = buildList {
            add(dependency("neoforge", "required", "[${pinned.neoforge},)"))
            add(dependency("minecraft", "required", minecraftRange(pinned.minecraft)))
            mod.dependenciesOf("neoforge").forEach { add(dependency(it.id, it.type, null)) }
        }
        return mapOf(
            "id" to escapeToml(mod.id),
            "name" to escapeToml(mod.name),
            "version" to escapeToml(version),
            "license" to escapeToml(mod.license),
            "dependencies" to dependencies.joinToString("\n"),
        )
    }

    /** `[26.3,26.4)`: the Minecraft version and its later patches. */
    private fun minecraftRange(minecraft: String): String {
        val parts = minecraft.split(".")
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: return "[$minecraft]"
        return "[$minecraft,${parts[0]}.${minor + 1})"
    }

    private fun json(value: String) = "\"${escapeJson(value)}\""

    private fun jsonObject(entries: Map<String, String>): String =
        if (entries.isEmpty()) "{}"
        else entries.entries.joinToString(", ", "{ ", " }") { "${json(it.key)}: ${json(it.value)}" }

    private fun toml(value: String) = "\"${escapeToml(value)}\""

    private fun escapeJson(value: String): String = buildString {
        for (c in value) {
            when {
                c == '"' -> append("\\\"")
                c == '\\' -> append("\\\\")
                c < ' ' -> append("\\u%04x".format(c.code))
                else -> append(c)
            }
        }
    }

    // TOML basic strings take the same escapes as JSON ones, and DEL.
    private fun escapeToml(value: String): String =
        escapeJson(value).replace("\u007f", "\\u007f")
}
