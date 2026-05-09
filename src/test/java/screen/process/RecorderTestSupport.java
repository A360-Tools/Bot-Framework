package screen.process;

import com.automationanywhere.botcommand.utilities.screen.recorder.FfmpegBinary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.stream.Stream;

/**
 * Shared helpers for tests that need to wait on real ffmpeg side effects.
 * Polls for the condition rather than using fixed Thread.sleep windows so
 * tests stay deterministic even on slow runners.
 */
public final class RecorderTestSupport {

    private RecorderTestSupport() {
    }

    /**
     * Returns the ring directory under
     * {@code %LOCALAPPDATA%\A360-BotFramework\sessions\<sessionId>\ring}.
     * Throws if the folder doesn't exist (e.g. recorder failed to start).
     */
    public static Path sessionRing(String sessionId) throws IOException {
        Path appData = FfmpegBinary.appDataRoot();
        Path ring = appData.resolve("sessions").resolve(sessionId).resolve("ring");
        if (!Files.isDirectory(ring)) {
            throw new IOException("session ring directory not found at " + ring);
        }
        return ring;
    }

    /** Counts {@code .mov} files in {@code dir}; returns 0 if the dir is gone. */
    public static long countMov(Path dir) {
        if (!Files.isDirectory(dir)) {
            return 0;
        }
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".mov"))
                    .count();
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * Polls {@code ringDir} until at least {@code minSegments} {@code .mov}
     * files exist, or {@code timeout} elapses. Throws AssertionError on
     * timeout so the caller's stack trace points at the wait that failed.
     *
     * <p>The caller usually wants {@code minSegments == 2}: that means at
     * least one segment is fully written (stable) plus one in-flight, which
     * is exactly what {@link com.automationanywhere.botcommand.utilities.screen.recorder.ScreenRecorder}
     * snapshots from on a level-trigger.
     */
    public static void awaitMovSegments(Path ringDir, int minSegments, Duration timeout)
            throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        long count;
        while (true) {
            count = countMov(ringDir);
            if (count >= minSegments) {
                return;
            }
            if (System.nanoTime() >= deadline) {
                break;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("timed out after " + timeout + " waiting for >="
                + minSegments + " .mov segments in " + ringDir
                + " (last seen: " + count + ")");
    }

    /**
     * Convenience: poll the ring of the supplied {@code sessionId} until
     * at least {@code minSegments} .mov files exist, or {@code timeout}
     * elapses. Throws AssertionError on timeout.
     */
    public static void awaitSessionMovSegments(String sessionId, int minSegments,
                                                Duration timeout)
            throws IOException, InterruptedException {
        awaitMovSegments(sessionRing(sessionId), minSegments, timeout);
    }
}
