// Managed by mc-cli: `mod update` rewrites this file.
//
// The strings of the shared library (the `vendor` package): `mod update`
// writes them to .mod/vendor/lang/<locale>.json, keys without prefix. The game
// reads one language file per namespace and several mods carry the library,
// so they are merged into the mod's own language file,
// assets/<modid>/lang/<locale>.json, under `<modid>.vendor.`. A key the mod's
// file defines itself is kept.
//
// The merge rewrites that file in the output of every resource task holding
// it, in `common` too: a dev run loads the mod from each of these folders, and
// the first one holding the file wins. A locale the mod has no file for gets
// one with the library's strings alone, written once per loader.

import groovy.json.JsonGenerator
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.io.File
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.kotlin.dsl.named
import org.gradle.language.jvm.tasks.ProcessResources

/**
 * Merges the library's strings into the mod's language files that the
 * resource tasks `processes` write. With `home`, one of them, a locale none
 * of them holds gets its file from `home`, after the others ran.
 */
fun Project.mergeVendorLang(modId: String, processes: List<String>, home: String? = null) {
    val library = rootDir.resolve(".mod/vendor/lang")
    val langDir = "assets/$modId/lang"
    val others = processes.filter { it != home }.map { tasks.named<ProcessResources>(it) }
    processes.forEach { process ->
        tasks.named<ProcessResources>(process) {
            inputs.files(fileTree(library)).withPropertyName("vendorLang")
            inputs.property("vendorLangModId", modId)
            val isHome = process == home
            val elsewhere = files()
            if (isHome) {
                dependsOn(others)
                elsewhere.from(others.map { other -> other.map { it.source.asFileTree.matching { include("$langDir/*.json") } } })
                inputs.files(elsewhere).withPropertyName("vendorLangElsewhere")
            }
            doLast {
                val held = elsewhere.files.map { it.name }.toSet()
                val lang = destinationDir.resolve(langDir)
                library.listFiles { f -> f.isFile && f.extension == "json" }.orEmpty().sorted().forEach { file ->
                    val target = lang.resolve(file.name)
                    if (!target.isFile && !(isHome && file.name !in held)) return@forEach
                    val merged = LinkedHashMap(if (target.isFile) readLang(target) else emptyMap())
                    readLang(file).forEach { (key, value) -> merged.putIfAbsent("$modId.vendor.$key", value) }
                    target.parentFile.mkdirs()
                    val json = JsonGenerator.Options().disableUnicodeEscaping().build().toJson(merged)
                    target.writeText(JsonOutput.prettyPrint(json, true) + "\n")
                }
            }
        }
    }
}

/** A language file: a JSON object of strings. */
private fun readLang(file: File): Map<String, String> {
    val parsed = try {
        JsonSlurper().parse(file, "UTF-8")
    } catch (e: Exception) {
        throw GradleException("${file.path} is not valid JSON: ${e.message}", e)
    }
    val map = parsed as? Map<*, *> ?: throw GradleException("${file.path} must hold a JSON object")
    return map.entries.associate { (key, value) ->
        key as String to (value as? String ?: throw GradleException("${file.path}: `$key` must be a string"))
    }
}
