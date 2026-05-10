package com.automationanywhere.botcommand.utilities.screen.recorder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Tracks spawned child processes and ensures they die with the JVM.
 *
 * <p>Strategy: spawn via plain {@link ProcessBuilder}, hold the
 * {@link Process} reference, and rely on a single JVM-wide shutdown hook to
 * {@link Process#destroyForcibly()} every tracked child on graceful exit.
 * Hard JVM kills (taskkill /f, OOM-killer, power loss) skip the hook and
 * leak the child until the next-startup orphan-sweep in {@link CrashSweep}
 * cleans up the abandoned ring folder.
 *
 * <p>This intentionally does not use Windows JobObject. JNA's platform
 * mappings do not expose the JobObject API surface in any current release
 * (verified through 5.18.1), and the kernel-enforced kill-on-close it would
 * provide is a marginal improvement over hook + sweep for the typical Bot
 * Agent deployment under NSSM, where most JVM exits give the hook time to
 * run.
 *
 * @author Sumit Kumar
 */
public final class ProcessGuard implements AutoCloseable {

    private static final Logger LOGGER = LogManager.getLogger(ProcessGuard.class);

    private static final Set<ProcessGuard> ALL = ConcurrentHashMap.newKeySet();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(ProcessGuard::shutdownAll,
                "a360-process-guard-shutdown"));
    }

    private final Set<Process> tracked = ConcurrentHashMap.newKeySet();
    private volatile boolean closed;

    public ProcessGuard() {
        ALL.add(this);
    }

    public Process spawn(String[] command, Path workDir) throws IOException {
        if (closed) {
            throw new IOException("ProcessGuard is closed; cannot spawn new processes");
        }
        ProcessBuilder pb = new ProcessBuilder(command);
        if (workDir != null) {
            pb.directory(workDir.toFile());
        }
        pb.redirectErrorStream(false);
        Process process = pb.start();
        tracked.add(process);
        return process;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        ALL.remove(this);
        // Safety net: force-kill anything our caller did not already stop.
        for (Process p : tracked) {
            try {
                if (p.isAlive()) {
                    p.destroyForcibly();
                }
            } catch (Throwable ignored) {
            }
        }
        tracked.clear();
    }

    /** Invoked by the static shutdown hook. Best-effort kill of every tracked child. */
    private static void shutdownAll() {
        for (ProcessGuard guard : ALL) {
            for (Process p : guard.tracked) {
                try {
                    if (!p.isAlive()) {
                        continue;
                    }
                    p.destroy();
                    if (!p.waitFor(2, TimeUnit.SECONDS)) {
                        p.destroyForcibly();
                    }
                } catch (InterruptedException e) {
                    p.destroyForcibly();
                    Thread.currentThread().interrupt();
                } catch (Throwable t) {
                    LOGGER.debug("ProcessGuard shutdown kill failed: {}", t.toString());
                }
            }
        }
    }
}
