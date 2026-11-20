// Managed by mc-cli: `mod update` rewrites this file.
//
// The NeoForge jar, through ModDevGradle. NeoForge has no client source set:
// the client code of a NeoForge-only mod lives in the `client` packages, and
// NeoForge hides client classes from a dedicated server in development.
// META-INF/neoforge.mods.toml is generated from mod.toml.
//
// The tests:
// - `test`: JUnit in src/test with the game and the mod loaded (MDG's unit
//   tests), an ephemeral server at hand (NeoForge's testframework);
// - `runGameTestServer`: the server gametests of src/gametest, a test mod
//   `<id>_gametest` with its own neoforge.mods.toml, on a headless server;
// - `runServerSelfTest` and `runClientSelfTest`: NeoForge's boot self-tests,
//   a dedicated server and a client that start with the mod and stop.
// `check` runs the first three, `clientCheck` the client self-test, on a
// virtual display (Xvfb) when there is one. Each fails when no test ran.

import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import net.neoforged.moddevgradle.dsl.RunModel

plugins {
    id("mod.java")
    id("net.neoforged.moddev")
    id("me.modmuss50.mod-publish-plugin")
}

val mod = ModToml.read(rootDir.resolve("mod.toml"))
val sourceSets = the<SourceSetContainer>()
val pinned = Pinned.read(rootDir.resolve(".mod/socle.toml"))

extensions.configure<BasePluginExtension> {
    archivesName.set("${mod.id}-neoforge")
}

// common's source sets are read below, once it is configured.
if (hasCommon) {
    evaluationDependsOn(":common")
}

extensions.configure<NeoForgeExtension> {
    setVersion(pinned.neoforge)
    mods.register(mod.id) {
        sourceSet(sourceSets["main"])
        // common's classes, on the runtime classpath below, belong to the mod
        // in dev runs too, so that they load with the game.
        if (hasCommon) {
            val common = project(":common").the<SourceSetContainer>()
            sourceSet(common["main"])
            sourceSet(common["client"])
        }
    }
}

if (hasCommon) {
    val classes = fromCommon("commonMainClasses")
    val clientClasses = fromCommon("commonClientClasses")
    sourceSets["main"].compileClasspath += classes + clientClasses
    sourceSets["main"].runtimeClasspath += classes + clientClasses
    tasks.named<Jar>("jar") {
        from(classes, clientClasses)
    }
    tasks.named<ProcessResources>("processResources") {
        from(fromCommon("commonMainResources"), fromCommon("commonClientResources"))
    }
}

val generateModMetadata = tasks.register<Copy>("generateModMetadata") {
    val values = ModMetadata.neoforge(mod, pinned, project.version.toString())
    inputs.properties(values)
    from(rootDir.resolve("build-logic/templates/neoforge.mods.toml"))
    expand(values)
    into(layout.buildDirectory.dir("generated/modMetadata/META-INF"))
}
sourceSets["main"].resources.srcDir(layout.buildDirectory.dir("generated/modMetadata"))
tasks.named<ProcessResources>("processResources") {
    dependsOn(generateModMetadata)
}
mergeVendorLang(mod.id, listOf("processResources"), home = "processResources")

// The tests.

val gametestReport = layout.buildDirectory.file("test-results/runGameTestServer/report.xml")
val serverSelfTestReport = layout.buildDirectory.file("selftest/server")
val clientSelfTestReport = layout.buildDirectory.file("selftest/client")
val gametestModId = "${mod.id}_gametest"
val gametest = sourceSets.create("gametest")

/** The game directory of the self-test run `run`. */
fun selfTestDirectory(run: String) = layout.buildDirectory.dir("run/$run")

extensions.configure<NeoForgeExtension> {
    addModdingDependenciesTo(gametest)
    val main = mods[mod.id]
    val testMod = mods.register(gametestModId) { sourceSet(gametest) }
    unitTest {
        enable()
        testedMod.set(main)
        loadedMods.set(listOf(main))
    }
    runs.register("gameTestServer") {
        type.set("gameTestServer")
        sourceSet.set(gametest)
        loadedMods.set(listOf(main, testMod.get()))
        programArgument("--report")
        programArgument(gametestReport.get().asFile.absolutePath)
    }
    fun RunModel.selfTest(variable: String, report: Provider<RegularFile>) {
        environment(variable, report.get().asFile.absolutePath)
        loadedMods.set(listOf(main))
        gameDirectory.set(selfTestDirectory(name))
    }
    runs.register("serverSelfTest") {
        server()
        selfTest("NEOFORGE_DEDICATED_SERVER_SELFTEST", serverSelfTestReport)
    }
    runs.register("clientSelfTest") {
        client()
        selfTest("NEOFORGE_CLIENT_SELFTEST", clientSelfTestReport)
    }
}

gametest.compileClasspath += sourceSets["main"].output
gametest.runtimeClasspath += sourceSets["main"].output
if (hasCommon) {
    val classes = configurations["commonMainClasses"] + configurations["commonClientClasses"]
    gametest.compileClasspath += classes
    gametest.runtimeClasspath += classes
    sourceSets["test"].compileClasspath += classes
    sourceSets["test"].runtimeClasspath += classes
}

dependencies {
    "testImplementation"("net.neoforged:testframework:${pinned.neoforge}")
}

val runGameTestServer = tasks.named<JavaExec>("runGameTestServer") {
    group = "verification"
    // The game does not create the report's directory.
    doFirst { gametestReport.get().asFile.apply { delete(); parentFile.mkdirs() } }
}
requireTestsRan(
    runGameTestServer,
    gametestReport.map { it.asFile.parentFile },
    "Register a test through RegisterGameTestsEvent in the test mod of neoforge/src/gametest.",
)

/**
 * Fails the task of the self-test run `run` when NeoForge did not write its
 * self-test `report`: the game did not finish starting.
 */
fun selfTest(run: String, report: Provider<RegularFile>, what: String) = tasks.named<JavaExec>("run" + run.replaceFirstChar { it.uppercase() }) {
    group = "verification"
    val gameDirectory = selfTestDirectory(run)
    // The dedicated server stops at once without it.
    doFirst {
        val file = report.get().asFile
        file.delete()
        file.parentFile.mkdirs()
        gameDirectory.get().asFile.resolve("eula.txt").apply { parentFile.mkdirs() }.writeText("eula=true\n")
    }
    doLast {
        if (!report.get().asFile.isFile) throw GradleException("$path: the $what did not finish starting.")
    }
}

val runServerSelfTest = selfTest("serverSelfTest", serverSelfTestReport, "dedicated server")
val runClientSelfTest = selfTest("clientSelfTest", clientSelfTestReport, "client")
onVirtualDisplay(runClientSelfTest)

tasks.named("check") {
    dependsOn(runGameTestServer, runServerSelfTest)
}
tasks.register("clientCheck") {
    group = "verification"
    description = "Runs NeoForge's client self-test."
    dependsOn(runClientSelfTest)
}
// One version of this loader's jar on each platform (Publication.kt).
configurePublication(mod, pinned, "neoforge")
