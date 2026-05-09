package screen.process;

import com.automationanywhere.botcommand.utilities.screen.recorder.FfmpegBinary;
import com.automationanywhere.botcommand.utilities.screen.recorder.ProcessGuard;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class ProcessGuardTest {

    @Test
    public void spawnReturnsAliveProcess() throws IOException, InterruptedException {
        ProcessGuard guard = new ProcessGuard();
        try {
            Process p = spawnLongRunningFfmpeg(guard);
            Assert.assertTrue(p.isAlive(), "spawned process should be alive");
            // Cleanup
            p.destroyForcibly();
            p.waitFor(3, TimeUnit.SECONDS);
        } finally {
            guard.close();
        }
    }

    @Test
    public void closeKillsStragglers() throws IOException, InterruptedException {
        ProcessGuard guard = new ProcessGuard();
        Process p = spawnLongRunningFfmpeg(guard);
        Assert.assertTrue(p.isAlive(), "spawned process should start alive");

        guard.close();

        // close() does destroyForcibly() on any tracked-and-alive child
        boolean exited = p.waitFor(3, TimeUnit.SECONDS);
        Assert.assertTrue(exited, "child should be dead within 3s of guard.close()");
        Assert.assertFalse(p.isAlive());
    }

    @Test(expectedExceptions = IOException.class)
    public void spawnAfterCloseThrows() throws IOException {
        ProcessGuard guard = new ProcessGuard();
        guard.close();
        // Should fail-fast - guard is no longer accepting new spawns
        guard.spawn(buildLongRunningFfmpegArgs(), null);
    }

    @Test
    public void multipleGuardsAreIndependent() throws IOException, InterruptedException {
        ProcessGuard a = new ProcessGuard();
        ProcessGuard b = new ProcessGuard();
        Process pa = spawnLongRunningFfmpeg(a);
        Process pb = spawnLongRunningFfmpeg(b);
        try {
            Assert.assertTrue(pa.isAlive());
            Assert.assertTrue(pb.isAlive());

            a.close();
            Assert.assertTrue(pa.waitFor(3, TimeUnit.SECONDS),
                    "guard A's child should die when A closes");
            Assert.assertTrue(pb.isAlive(),
                    "guard B's child must remain alive when only A closes");
        } finally {
            b.close();
            pb.waitFor(3, TimeUnit.SECONDS);
        }
    }

    private static Process spawnLongRunningFfmpeg(ProcessGuard guard) throws IOException {
        return guard.spawn(buildLongRunningFfmpegArgs(), null);
    }

    /**
     * 30-second lavfi-driven ffmpeg invocation - works without a desktop and
     * produces no output files. Long enough for any guard test to assert
     * "still alive" before forcing termination.
     */
    private static String[] buildLongRunningFfmpegArgs() throws IOException {
        Path ffmpeg = FfmpegBinary.locate();
        return new String[]{
                ffmpeg.toAbsolutePath().toString(),
                "-hide_banner",
                "-loglevel", "error",
                "-f", "lavfi",
                "-i", "testsrc=size=160x120:rate=5:duration=30",
                "-f", "null",
                "-"
        };
    }
}
