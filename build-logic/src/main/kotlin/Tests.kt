// Managed by mc-cli: `mod update` rewrites this file.
//
// What the test layers share: the JUnit version, the virtual display of the
// client runs, the game options pinned for screenshots, and the guard that
// fails a test task when no test ran. Without it, such a task passes: a test
// source set left empty, a gametest the loader never registered.

import java.io.File
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.w3c.dom.Element

/** The JUnit Jupiter version of every JUnit test task. */
const val JUNIT = "5.14.4"

/**
 * options.txt of the client runs: OpenGL whatever the Minecraft version's
 * default backend, so that a screenshot does not depend on it. The game drops
 * the options of a file without the data version of the Minecraft jar found
 * on `classpath`.
 */
fun clientOptions(classpath: Iterable<File>): String {
    val version = classpath.asSequence()
        .filter { it.isFile && it.extension == "jar" }
        .mapNotNull { jar ->
            ZipFile(jar).use { zip ->
                zip.getEntry("version.json")?.let { entry ->
                    val text = zip.getInputStream(entry).bufferedReader().readText()
                    Regex("\"world_version\"\\s*:\\s*(\\d+)").find(text)?.groupValues?.get(1)
                }
            }
        }
        .firstOrNull() ?: throw GradleException("No Minecraft jar with a version.json on the run's classpath")
    return "version:$version\npreferredGraphicsBackend:\"opengl\"\n"
}

/** Whether `program` is on the path, on Linux: the client runs then use a virtual display. */
fun onLinuxPath(program: String): Boolean =
    System.getProperty("os.name").lowercase().startsWith("linux") &&
        (System.getenv("PATH") ?: "").split(File.pathSeparator).any { File(it, program).canExecute() }

/**
 * A virtual display (Xvfb) for the client runs Loom does not wrap in
 * xvfb-run: started on first use, stopped at the end of the build.
 */
abstract class VirtualDisplay : BuildService<BuildServiceParameters.None>, AutoCloseable {
    private var process: Process? = null

    /** The `DISPLAY` of the virtual display. */
    val display: String by lazy {
        val started = ProcessBuilder("Xvfb", "-displayfd", "1", "-screen", "0", "1280x720x24", "-nolisten", "tcp")
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
        process = started
        val number = started.inputStream.bufferedReader().readLine()
            ?: throw GradleException("Xvfb did not start")
        ":${number.trim()}"
    }

    override fun close() {
        process?.destroy()
    }
}

/**
 * The environment that keeps a client on the virtual display: on a desktop
 * with a Wayland session, SDL would open its window there, on the GPU.
 */
val X11_ONLY = mapOf("SDL_VIDEO_DRIVER" to "x11")

/** Runs `task` on a virtual display when Xvfb is installed. */
fun Project.onVirtualDisplay(task: TaskProvider<out JavaExec>) {
    if (!onLinuxPath("Xvfb")) return
    val service = gradle.sharedServices.registerIfAbsent("virtualDisplay", VirtualDisplay::class.java) {}
    task.configure {
        usesService(service)
        doFirst {
            environment("DISPLAY", service.get().display)
            environment(X11_ONLY)
        }
    }
}

/**
 * Registers `<task>Ran`, run after `task`, which fails when the JUnit XML
 * reports under `reports` hold no test that ran. Vanilla's own tests
 * (`minecraft:` ids, which every gametest server registers) do not count.
 */
fun Project.requireTestsRan(task: TaskProvider<out Task>, reports: Provider<File>, hint: String) {
    val path = "${project.path}:${task.name}".replace("::", ":")
    val guard = tasks.register("${task.name}Ran") {
        group = "verification"
        description = "Fails when ${task.name} ran no test."
        onlyIf { task.get().state.failure == null }
        doLast {
            val ran = reports.get().walkTopDown()
                .filter { it.isFile && it.extension == "xml" }
                .sumOf { countRan(it) }
            if (ran == 0) {
                throw GradleException("$path ran no test. $hint")
            }
            logger.lifecycle("$path ran $ran test(s).")
        }
    }
    task.configure { finalizedBy(guard) }
}

/** Runs `task` on JUnit, with its guard. */
fun Project.junit(task: TaskProvider<Test>, hint: String) {
    task.configure { useJUnitPlatform() }
    requireTestsRan(task, provider { task.get().reports.junitXml.outputLocation.get().asFile }, hint)
}

/** The test cases of a JUnit XML report that ran: neither skipped nor vanilla's. */
private fun countRan(report: File): Int {
    val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(report)
    val cases = document.getElementsByTagName("testcase")
    return (0 until cases.length).map { cases.item(it) as Element }.count { case ->
        case.getElementsByTagName("skipped").length == 0 && !case.getAttribute("name").startsWith("minecraft:")
    }
}
