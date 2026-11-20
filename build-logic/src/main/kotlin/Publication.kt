// Managed by mc-cli: `mod update` rewrites this file.
//
// The publication of a loader's jar through mod-publish-plugin, fed by the
// files that own each datum: mod.toml (name, side, dependencies),
// page/page.toml (project id on each platform), .mod/socle.toml (Minecraft
// version) and changelogs/<version>.md (notes).
//
// `./gradlew publishMods -Pversion=<version>` publishes one version per loader,
// numbered `<version>+<minecraft>-<loader>`. With `-PdryRun=true` nothing is
// sent: each platform writes what it would publish to
// build/publishMods/<platform>.dry-run.json. The tokens come from the
// environment: MC_MODRINTH_TOKEN, MC_CURSEFORGE_UPLOAD_TOKEN.

import java.io.File
import me.modmuss50.mpp.ModPublishExtension
import me.modmuss50.mpp.PublishModTask
import me.modmuss50.mpp.ReleaseType
import me.modmuss50.mpp.platforms.curseforge.Curseforge
import me.modmuss50.mpp.platforms.modrinth.Modrinth
import me.modmuss50.mpp.platforms.modrinth.ModrinthEnvironment
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType
import org.tomlj.Toml

/** Configures the publication of this loader subproject's jar. */
fun Project.configurePublication(mod: ModToml, pinned: Pinned, loader: String) {
    val modVersion = version.toString()
    val number = "$modVersion+${pinned.minecraft}-$loader"
    val isDryRun = providers.gradleProperty("dryRun").map { it != "false" }.getOrElse(false)
    val changelog = rootDir.resolve("changelogs/$modVersion.md")
    // A build or a test neither warns about a platform left out nor fails on
    // page/page.toml: both wait for publishMods.
    val page = runCatching { platformIds(rootDir.resolve("page/page.toml")) }
    val pageError = page.exceptionOrNull()?.message
    val ids = page.getOrDefault(emptyMap())
    val skipped = mutableListOf<String>()
    val dependencies = mod.dependenciesOf(loader)

    extensions.configure<ModPublishExtension> {
        dryRun.set(isDryRun)
        file.set(tasks.named<Jar>("jar").flatMap { it.archiveFile })
        version.set(number)
        displayName.set("${mod.name} $number")
        type.set(releaseType(modVersion))
        modLoaders.add(loader)
        this.changelog.set(providers.provider { readChangelog(changelog, modVersion, isDryRun) })

        // The plugin fails on a platform without project: one without id in
        // page/page.toml is not declared at all.
        val modrinthId = ids["modrinth"].orEmpty()
        if (modrinthId.isEmpty()) {
            skipped += "$loader: Modrinth skipped, page/page.toml has no [modrinth] project_id. Create the project on Modrinth and write its id there to publish to it."
        } else {
            modrinth {
                projectId.set(modrinthId)
                accessToken.set(providers.environmentVariable("MC_MODRINTH_TOKEN"))
                minecraftVersions.add(pinned.minecraft)
                environment.set(
                    when (mod.side) {
                        "client" -> ModrinthEnvironment.CLIENT_ONLY
                        "server" -> ModrinthEnvironment.SERVER_ONLY
                        "both-optional" -> ModrinthEnvironment.SERVER_ONLY_CLIENT_OPTIONAL
                        else -> ModrinthEnvironment.CLIENT_AND_SERVER
                    },
                )
                for (dependency in dependencies) {
                    val slug = dependency.modrinth ?: continue
                    when (dependency.type) {
                        "required" -> requires(slug)
                        "optional" -> optional(slug)
                        else -> incompatible(slug)
                    }
                }
            }
        }

        val curseforgeId = ids["curseforge"].orEmpty()
        if (curseforgeId.isEmpty()) {
            skipped += "$loader: CurseForge skipped, page/page.toml has no [curseforge] project_id. Create the project on CurseForge and write its id there to publish to it."
        } else {
            curseforge {
                projectId.set(curseforgeId)
                accessToken.set(providers.environmentVariable("MC_CURSEFORGE_UPLOAD_TOKEN"))
                minecraftVersions.add(pinned.minecraft)
                client.set(mod.side != "server")
                server.set(mod.side != "client")
                for (dependency in dependencies) {
                    val slug = dependency.curseforge ?: continue
                    when (dependency.type) {
                        "required" -> requires(slug)
                        "optional" -> optional(slug)
                        else -> incompatible(slug)
                    }
                }
            }
        }
    }

    val warnings = skipped.toList()
    tasks.named("publishMods") {
        doFirst {
            if (pageError != null) throw GradleException(pageError)
            warnings.forEach { logger.warn(it) }
        }
    }

    if (isDryRun) {
        tasks.withType<PublishModTask>().configureEach {
            val out = layout.buildDirectory.file("publishMods/${platform.name}.dry-run.json")
            doLast {
                out.get().asFile.writeText(toJson(dryRunDescription(this as PublishModTask)) + "\n")
            }
        }
    }
}

/** The project id of each platform in `page`, when it has one. */
fun platformIds(page: File): Map<String, String> {
    if (!page.isFile) return emptyMap()
    val toml = Toml.parse(page.toPath())
    if (toml.hasErrors()) throw GradleException("${page.path} is invalid: ${toml.errors().joinToString("; ")}")
    return listOf("modrinth", "curseforge").associateWith { toml.getString("$it.project_id").orEmpty().trim() }
}

/** `-alpha` gives an alpha, any other suffix (`-beta.1`, `-rc.1`) a beta. */
fun releaseType(version: String): ReleaseType = when {
    "-alpha" in version -> ReleaseType.ALPHA
    "-" in version -> ReleaseType.BETA
    else -> ReleaseType.STABLE
}

/** The notes of `version`, required to publish; a dry run goes on without. */
private fun readChangelog(file: File, version: String, dryRun: Boolean): String {
    val text = if (file.isFile) file.readText() else ""
    if (text.isNotBlank()) return text
    if (dryRun) return "(dry run: changelogs/${file.name} is missing or empty)"
    throw GradleException("changelogs/${file.name} is missing or empty: write the notes of version $version there, then publish again")
}

/** What `task` would send, read from the plugin's own platform settings. */
private fun dryRunDescription(task: PublishModTask): Map<String, Any?> {
    val platform = task.platform
    val common = linkedMapOf<String, Any?>(
        "platform" to platform.name,
        "version" to platform.version.get(),
        "displayName" to platform.displayName.get(),
        "type" to platform.type.get().name,
        "loaders" to platform.modLoaders.get(),
        "file" to platform.file.get().asFile.name,
        "changelog" to platform.changelog.get(),
    )
    when (platform) {
        is Modrinth -> {
            common["projectId"] = platform.projectId.get()
            common["gameVersions"] = platform.minecraftVersions.get()
            common["environment"] = platform.environment.orNull?.name
            common["dependencies"] = platform.dependencies.get().map {
                mapOf("slug" to (it.slug.orNull ?: it.id.orNull), "type" to it.type.get().name)
            }
        }
        is Curseforge -> {
            common["projectId"] = platform.projectId.get()
            common["gameVersions"] = platform.minecraftVersions.get()
            common["client"] = platform.client.orNull
            common["server"] = platform.server.orNull
            common["dependencies"] = platform.dependencies.get().map {
                mapOf("slug" to it.slug.get(), "type" to it.type.get().name)
            }
        }
    }
    return common
}

private fun toJson(value: Any?): String = when (value) {
    null -> "null"
    is Boolean, is Number -> value.toString()
    is Map<*, *> -> value.entries.joinToString(", ", "{", "}") { "${toJson(it.key.toString())}: ${toJson(it.value)}" }
    is Iterable<*> -> value.joinToString(", ", "[", "]") { toJson(it) }
    else -> buildString {
        append('"')
        for (c in value.toString()) {
            when {
                c == '"' -> append("\\\"")
                c == '\\' -> append("\\\\")
                c < ' ' -> append("\\u%04x".format(c.code))
                else -> append(c)
            }
        }
        append('"')
    }
}
