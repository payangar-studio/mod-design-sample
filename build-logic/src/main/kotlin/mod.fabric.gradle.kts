// Managed by mc-cli: `mod update` rewrites this file.
//
// The Fabric jar, through Loom without remapping. The client code lives in the
// `client` source set. fabric.mod.json is generated from mod.toml.
//
// The tests: JUnit in src/test on Fabric Loader (fabric-loader-junit), and the
// gametests of src/gametest, a test mod `<id>_gametest` with its own
// fabric.mod.json, run three ways:
// - `runGameTest`, server gametests on a headless server, part of `check`;
// - `runClientGameTest`, client gametests on the dev client;
// - `runProductionClientGameTest`, the same on a client launched as in
//   production, from the built jars.
// The client runs use a virtual display (xvfb-run) when there is one. Each
// fails when no test ran; `clientCheck` runs both client runs.

import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.api.fabricapi.FabricApiExtension
import net.fabricmc.loom.task.RunGameTask

plugins {
    id("mod.java")
    id("net.fabricmc.fabric-loom")
    id("me.modmuss50.mod-publish-plugin")
}

val mod = ModToml.read(rootDir.resolve("mod.toml"))
val sourceSets = the<SourceSetContainer>()
val pinned = Pinned.read(rootDir.resolve(".mod/socle.toml"))

extensions.configure<BasePluginExtension> {
    archivesName.set("${mod.id}-fabric")
}

extensions.configure<LoomGradleExtensionAPI> {
    splitEnvironmentSourceSets()
    mods.register(mod.id) {
        sourceSet(sourceSets["main"])
        sourceSet(sourceSets["client"])
        // common's classes, on the runtime classpath below, belong to the mod
        // in dev runs too.
        if (hasCommon) {
            sourceSet("main", project(":common"))
            sourceSet("client", project(":common"))
        }
    }
}

dependencies {
    "minecraft"("com.mojang:minecraft:${pinned.minecraft}")
    "implementation"("net.fabricmc:fabric-loader:${pinned.fabricLoader}")
    if (mod.dependenciesOf("fabric").any { it.id == "fabric-api" }) {
        "implementation"("net.fabricmc.fabric-api:fabric-api:${pinned.fabricApi}")
    }
}

// The shared library opens its config screen from Mod Menu when the player
// has it; the mod does not depend on it. Mod Menu is compiled against, and its
// entrypoint declared, once the library's class is there.
val modMenuEntrypoint = file("src/client/java/${mod.pkg.replace('.', '/')}/vendor/fabric/ModMenuEntrypoint.java")
if (modMenuEntrypoint.isFile) {
    repositories {
        maven("https://maven.terraformersmc.com/releases/") {
            content { includeGroup("com.terraformersmc") }
        }
    }
    dependencies {
        "clientCompileOnly"("com.terraformersmc:modmenu:${pinned.modmenu}")
    }
}

if (hasCommon) {
    val mainClasses = fromCommon("commonMainClasses")
    val clientClasses = fromCommon("commonClientClasses")
    sourceSets["main"].compileClasspath += mainClasses
    sourceSets["main"].runtimeClasspath += mainClasses
    sourceSets["client"].compileClasspath += mainClasses + clientClasses
    sourceSets["client"].runtimeClasspath += mainClasses + clientClasses
    tasks.named<Jar>("jar") {
        from(mainClasses, clientClasses)
    }
    tasks.named<ProcessResources>("processResources") {
        from(fromCommon("commonMainResources"))
    }
    tasks.named<ProcessResources>("processClientResources") {
        from(fromCommon("commonClientResources"))
    }
}

val generateModMetadata = tasks.register<Copy>("generateModMetadata") {
    val values = ModMetadata.fabric(mod, pinned, project.version.toString(), modMenuEntrypoint.isFile)
    inputs.properties(values)
    from(rootDir.resolve("build-logic/templates/fabric.mod.json"))
    expand(values)
    into(layout.buildDirectory.dir("generated/modMetadata"))
}
sourceSets["main"].resources.srcDir(generateModMetadata)
mergeVendorLang(mod.id, listOf("processResources", "processClientResources"), home = "processResources")

// The tests.

val gametestModId = "${mod.id}_gametest"
val fabricApi = "net.fabricmc.fabric-api:fabric-api:${pinned.fabricApi}"

extensions.configure<FabricApiExtension> {
    configureTests {
        createSourceSet.set(true)
        modId.set(gametestModId)
        // The dedicated servers of client gametests need it.
        eula.set(true)
    }
}
val gametest = sourceSets["gametest"]
if (hasCommon) {
    val classes = configurations["commonMainClasses"] + configurations["commonClientClasses"]
    gametest.compileClasspath += classes
    gametest.runtimeClasspath += classes
    sourceSets["test"].compileClasspath += classes
    sourceSets["test"].runtimeClasspath += classes
}

dependencies {
    "testImplementation"("net.fabricmc:fabric-loader-junit:${pinned.fabricLoader}")
    "gametestImplementation"(fabricApi)
    "productionRuntimeMods"(fabricApi)
}
// fabric-loader-junit starts Fabric Loader as a client unless told otherwise,
// and a server mod does not load on a client.
if (!mod.hasClient) {
    tasks.named<Test>("test") { systemProperty("fabric.side", "server") }
}

// Fabric API reads these properties.
val gametestReport = layout.buildDirectory.file("test-results/runGameTest/report.xml")
val clientGametestProperties = mapOf(
    // Only the test mod's client gametests: none is an error, not a pass.
    "fabric.client.gametest.modid" to gametestModId,
    // Fabric documents it for client gametests on GitHub Actions; a Fabric
    // API that does not read it ignores it.
    "fabric.client.gametest.disableNetworkSynchronizer" to "true",
)
extensions.configure<LoomGradleExtensionAPI> {
    runs.named("gameTest") {
        systemProperties.put("fabric-api.gametest.report-file", gametestReport.get().asFile.absolutePath)
    }
    runs.named("clientGameTest") {
        // Without it, a comparison with a missing reference saves the render
        // as the reference and passes: references come from CI only.
        val properties = systemProperties.get() - "fabric.client.gametest.testModResourcesPath"
        systemProperties.set(properties + clientGametestProperties)
    }
}

val runGameTest = tasks.named<RunGameTask>("runGameTest") {
    doFirst { gametestReport.get().asFile.delete() }
}
requireTestsRan(
    runGameTest,
    gametestReport.map { it.asFile.parentFile },
    "Annotate a method @GameTest in a class listed under `fabric-gametest` in fabric/src/gametest/resources/fabric.mod.json.",
)

/** Writes options.txt in `runDir`, emptied first when `clear`. */
fun Task.writeClientOptions(runDir: File, clear: Boolean) {
    val classpath = gametest.runtimeClasspath
    doFirst {
        if (clear) runDir.deleteRecursively()
        runDir.mkdirs()
        runDir.resolve("options.txt").writeText(clientOptions(classpath))
    }
}

val runClientGameTest = tasks.named<RunGameTask>("runClientGameTest") {
    useXvfb.set(onLinuxPath("xvfb-run"))
    doFirst { if (useXvfb.get()) environment(X11_ONLY) }
    // Loom empties the run directory before.
    writeClientOptions(layout.buildDirectory.dir("run/clientGameTest").get().asFile, clear = false)
}

val gametestJar = tasks.register<Jar>("gametestJar") {
    archiveClassifier.set("gametest")
    from(gametest.output)
}

val productionRunDir = layout.buildDirectory.dir("run/productionClientGameTest")
val runProductionClientGameTest = tasks.register<HeadlessClientProductionRunTask>("runProductionClientGameTest") {
    group = "verification"
    description = "Runs the client gametests on a client launched as in production."
    mods.from(gametestJar)
    jvmArgs.add("-Dfabric.client.gametest")
    jvmArgs.addAll(clientGametestProperties.map { (key, value) -> "-D$key=$value" })
    useXVFB.set(onLinuxPath("xvfb-run"))
    runDir.set(productionRunDir)
    writeClientOptions(productionRunDir.get().asFile, clear = true)
}

tasks.register("clientCheck") {
    group = "verification"
    description = "Runs the client gametests, on the dev client and as in production."
    dependsOn(runClientGameTest, runProductionClientGameTest)
}

// Client gametests write no report: Fabric API fails the run when the filter
// on the test mod finds no test, so the exit code is the guard.

tasks.register("promoteScreenshots") {
    group = "verification"
    description = "Makes the renders of a CI run the references: -Pscreenshots=<downloaded screenshots artifact>."
    val from = providers.gradleProperty("screenshots")
    val templates = file("src/gametest/resources/templates")
    doLast {
        val dir = file(from.orNull ?: throw GradleException("Pass the downloaded screenshots with -Pscreenshots=<dir>."))
        // Renders of takeScreenshot carry a counter prefix: they are no references.
        val renders = dir.listFiles { f -> f.extension == "png" && !Regex("\\d{4}_.*").matches(f.name) }.orEmpty()
        if (renders.isEmpty()) throw GradleException("$dir holds no render to promote.")
        renders.forEach {
            it.copyTo(templates.resolve(it.name), overwrite = true)
            logger.lifecycle("Promoted ${it.name}")
        }
    }
}
// One version of this loader's jar on each platform (Publication.kt).
configurePublication(mod, pinned, "fabric")
