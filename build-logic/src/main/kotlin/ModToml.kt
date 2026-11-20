// Managed by mc-cli: `mod update` rewrites this file.
//
// What the build reads from the repository: mod.toml, the mod's identity and
// contract (owned by the project), and .mod/socle.toml, the versions pinned for
// this branch (owned by the socle).

import java.io.File
import org.gradle.api.GradleException
import org.tomlj.Toml
import org.tomlj.TomlArray
import org.tomlj.TomlTable

/** A loader the socle builds for, as named in mod.toml and in subproject names. */
val LOADERS = listOf("fabric", "neoforge")

/** Values of `side` in mod.toml. */
val SIDES = listOf("client", "server", "both-required", "both-optional")

/** Values of a dependency's `type` in mod.toml. */
val DEPENDENCY_TYPES = listOf("required", "optional", "incompatible")

/** A `[[dependencies]]` entry of mod.toml, with its project slug on each platform. */
data class ModDependency(
    val id: String,
    val type: String,
    val loaders: List<String>,
    val modrinth: String? = null,
    val curseforge: String? = null,
)

/** mod.toml. */
data class ModToml(
    val id: String,
    val name: String,
    val pkg: String,
    val side: String,
    val loaders: List<String>,
    val license: String,
    val dependencies: List<ModDependency>,
) {
    /** Whether the client code has a place in this mod. */
    val hasClient: Boolean get() = side != "server"

    /** The dependencies that apply to `loader`. */
    fun dependenciesOf(loader: String): List<ModDependency> = dependencies.filter { loader in it.loaders }

    companion object {
        fun read(file: File): ModToml {
            val toml = parse(file)
            val where = file.name
            fun string(table: TomlTable, key: String, context: String): String {
                val value: String? = table.getString(key)
                return value?.takeIf { it.isNotBlank() }
                    ?: throw GradleException("$context: `$key` is missing or empty")
            }
            fun optionalString(table: TomlTable, key: String, context: String): String? {
                if (!table.contains(key)) return null
                val value = table.get(key) as? String
                    ?: throw GradleException("$context: `$key` must be a string")
                return value.trim().takeIf { it.isNotEmpty() }
            }
            fun strings(table: TomlTable, key: String, context: String): List<String>? {
                val array: TomlArray = table.getArray(key) ?: return null
                return (0 until array.size()).map {
                    array.get(it) as? String ?: throw GradleException("$context: `$key` must hold strings")
                }
            }
            fun oneOf(value: String, allowed: List<String>, context: String): String {
                if (value !in allowed) {
                    throw GradleException("$context: `$value` is not one of ${allowed.joinToString(", ")}")
                }
                return value
            }

            val loaders = strings(toml, "loaders", where)
                ?: throw GradleException("$where: `loaders` is missing")
            if (loaders.isEmpty()) throw GradleException("$where: `loaders` is empty")
            loaders.forEach { oneOf(it, LOADERS, "$where, loaders") }

            val declared: TomlArray? = toml.getArray("dependencies")
            val dependencies = declared?.let { array ->
                (0 until array.size()).map { index ->
                    val context = "$where, dependencies[$index]"
                    val table = array.get(index) as? TomlTable
                        ?: throw GradleException("$context: must be a table, written as a [[dependencies]] section")
                    ModDependency(
                        id = string(table, "id", context),
                        type = oneOf(string(table, "type", context), DEPENDENCY_TYPES, "$context, type"),
                        loaders = (strings(table, "loaders", context) ?: loaders).onEach {
                            oneOf(it, loaders, "$context, loaders")
                        },
                        modrinth = optionalString(table, "modrinth", context),
                        curseforge = optionalString(table, "curseforge", context),
                    )
                }
            } ?: emptyList()

            return ModToml(
                id = string(toml, "id", where),
                name = string(toml, "name", where),
                pkg = string(toml, "package", where),
                side = oneOf(string(toml, "side", where), SIDES, "$where, side"),
                loaders = loaders,
                license = string(toml, "license", where),
                dependencies = dependencies,
            )
        }
    }
}

/** The versions .mod/socle.toml pins for this branch. */
data class Pinned(
    val minecraft: String,
    val fabricLoader: String,
    val fabricApi: String,
    val neoforge: String,
    val modmenu: String,
) {
    companion object {
        fun read(file: File): Pinned {
            val toml = parse(file)
            fun pinned(key: String): String =
                (toml.getString("init.$key") as String?) ?: throw GradleException("${file.path}: `$key` is missing under [init]")
            return Pinned(
                minecraft = pinned("minecraft"),
                fabricLoader = pinned("fabric-loader"),
                fabricApi = pinned("fabric-api"),
                neoforge = pinned("neoforge"),
                modmenu = pinned("modmenu"),
            )
        }
    }
}

private fun parse(file: File): TomlTable {
    if (!file.isFile) throw GradleException("${file.path} is missing")
    val result = Toml.parse(file.toPath())
    if (result.hasErrors()) {
        throw GradleException("${file.path} is invalid: ${result.errors().joinToString("; ")}")
    }
    return result
}
