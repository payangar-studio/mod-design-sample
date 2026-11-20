// Managed by mc-cli: `mod update` rewrites this file.

import net.fabricmc.loom.task.prod.ClientProductionRunTask
import org.gradle.process.ExecSpec

/**
 * Loom's production client, kept on the virtual display of xvfb-run on a
 * desktop with a Wayland session, as it is in CI.
 */
abstract class HeadlessClientProductionRunTask : ClientProductionRunTask() {
    override fun configureCommand(exec: ExecSpec) {
        super.configureCommand(exec)
        if (useXVFB.get()) exec.environment(X11_ONLY)
    }
}
