package screen.process;

import com.automationanywhere.botcommand.utilities.screen.recorder.EncodingMode;
import com.automationanywhere.botcommand.utilities.screen.recorder.ScreenRecorder;
import org.apache.logging.log4j.Level;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Verifies that {@code snapshotForError} dedups calls inside the same
 * 1-second chunk and emits a fresh path only once the chunk boundary is
 * crossed. The dedup is what keeps tight loop-warning workloads bounded -
 * if it regresses, the encoder pool starts piling up duplicate work.
 */
public class ChunkDedupTest {

    private Path logDir;
    private ScreenRecorder recorder;

    @BeforeClass
    public void requireDesktop() {
        DesktopAvailability.requireDesktop();
    }

    @BeforeMethod
    public void setUp() throws IOException {
        logDir = Files.createTempDirectory("chunkdedup-");
    }

    @AfterMethod
    public void tearDown() {
        if (recorder != null && !recorder.isClosed()) {
            recorder.close();
        }
        deleteRecursivelyQuietly(logDir);
    }

    @Test
    public void sameChunkReturnsSamePathDifferentChunkReturnsNewPath() throws Exception {
        Set<Level> levels = new HashSet<>();
        levels.add(Level.ERROR);
        String sessionId = UUID.randomUUID().toString();
        recorder = ScreenRecorder.start(sessionId, logDir, 5, levels, EncodingMode.COMPACT);
        Assert.assertNotSame(recorder, ScreenRecorder.DISABLED);

        // Wait for the ring to populate; otherwise the first snapshot returns null
        // (and a subsequent one in the same chunk would also return null, making
        // the dedup observation meaningless).
        RecorderTestSupport.awaitSessionMovSegments(sessionId, 2, Duration.ofSeconds(15));

        // Align to the start of a fresh 1-second chunk so the two calls below
        // land inside the same chunk with high confidence. Without this we
        // could call near a chunk boundary and get spuriously distinct paths.
        sleepUntilChunkStart();

        Path first = recorder.snapshotForError("first-" + UUID.randomUUID());
        Path second = recorder.snapshotForError("second-" + UUID.randomUUID());
        Assert.assertNotNull(first, "first snapshot should produce a path");
        Assert.assertNotNull(second, "second snapshot should produce a path");
        Assert.assertEquals(second, first,
                "two snapshots inside the same 1-second chunk must share a clip path");

        // Cross the chunk boundary and the next snapshot should return a fresh path.
        Thread.sleep(1100);
        Path third = recorder.snapshotForError("third-" + UUID.randomUUID());
        Assert.assertNotNull(third, "snapshot after chunk boundary should produce a path");
        Assert.assertNotEquals(third, first,
                "after crossing the 1-second chunk boundary the path must change");
    }

    private static void sleepUntilChunkStart() throws InterruptedException {
        long now = System.currentTimeMillis();
        long nextChunkStart = ((now / 1000L) + 1L) * 1000L;
        long pause = nextChunkStart - now;
        if (pause > 0 && pause < 1000) {
            Thread.sleep(pause + 5);   // small slack to land just after the boundary
        }
    }

    private static void deleteRecursivelyQuietly(Path root) {
        if (root == null || !Files.exists(root)) return;
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }
}
