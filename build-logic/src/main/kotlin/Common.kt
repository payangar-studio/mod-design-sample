// Managed by mc-cli: `mod update` rewrites this file.
//
// How the loader subprojects take in `common`: its compiled classes and
// processed resources, source set by source set. Minecraft is not remapped in
// 26.x, so classes compiled against vanilla run as they are on every loader.

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.named
import org.gradle.language.jvm.tasks.ProcessResources

/** Whether the build has a `common` subproject: more than one loader. */
val Project.hasCommon: Boolean get() = rootProject.findProject(":common") != null

/** Exposes the classes and resources of `sourceSet` of `common` to the loader subprojects. */
fun Project.exportCommon(sourceSet: SourceSet) {
    val name = "common" + sourceSet.name.replaceFirstChar { it.uppercase() }
    val compile = tasks.named<JavaCompile>(sourceSet.compileJavaTaskName)
    val classes = configurations.create("${name}Classes") {
        isCanBeResolved = false
        isCanBeConsumed = true
    }
    artifacts.add(classes.name, compile.flatMap { it.destinationDirectory }) { builtBy(compile) }
    val process = tasks.named<ProcessResources>(sourceSet.processResourcesTaskName)
    val resources = configurations.create("${name}Resources") {
        isCanBeResolved = false
        isCanBeConsumed = true
    }
    artifacts.add(resources.name, process.map { it.destinationDir }) { builtBy(process) }
}

/** What `common` exports as `name` (`commonMainClasses`), resolved in this subproject. */
fun Project.fromCommon(name: String): Configuration {
    val configuration = configurations.create(name) {
        isCanBeResolved = true
        isCanBeConsumed = false
    }
    dependencies.add(name, dependencies.project(mapOf("path" to ":common", "configuration" to name)))
    return configuration
}
