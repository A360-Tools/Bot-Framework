package screen.process;

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
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

public class StageOneCaptureTest {

    private Path logDir;
    private ScreenRecorder recorder;

    @BeforeClass
    public void requireDesktop() {
        DesktopAvailability.requireDesktop();
    }

    @BeforeMethod
    public void setUp() throws IOException {
        logDir = Files.createTempDirectory("stage1test-");
    }

    @AfterMethod
    public void tearDown() {
        if (recorder != null && !recorder.isClosed()) {
            recorder.close();
        }
        deleteRecursivelyQuietly(logDir);
    }

    @Test
    public void capturesSegmentsIntoRingFolder() throws Exception {
        Set<Level> levels = new HashSet<>();
        levels.add(Level.ERROR);

        String sessionId = UUID.randomUUID().toString();
        recorder = ScreenRecorder.start(sessionId, logDir, 5, levels);
        Assert.assertNotSame(recorder, ScreenRecorder.DISABLED,
                "real recorder should be returned for valid arguments");
        Assert.assertTrue(recorder.shouldRecordFor(Level.ERROR),
                "shouldRecordFor must reflect the configured level");

        // Poll up to 8s for at least 2 segments. Throws AssertionError on timeout.
        RecorderTestSupport.awaitSessionMovSegments(sessionId, 2,
                java.time.Duration.ofSeconds(8));
    }

    @Test
    public void closeShutsDownAndCleansSessionFolder() throws Exception {
        Set<Level> levels = new HashSet<>();
        levels.add(Level.ERROR);

        String sessionId = UUID.randomUUID().toString();
        recorder = ScreenRecorder.start(sessionId, logDir, 5, levels);
        Path ringDir = RecorderTestSupport.sessionRing(sessionId);
        Path sessionFolder = ringDir.getParent();

        // Wait until stage-1 has actually started writing - poll for the first segment.
        RecorderTestSupport.awaitMovSegments(ringDir, 1,
                java.time.Duration.ofSeconds(8));

        recorder.close();
        Assert.assertTrue(recorder.isClosed());

        Assert.assertFalse(Files.exists(sessionFolder),
                "session folder must be removed after close(); still exists at "
                        + sessionFolder);
    }

    @Test
    public void disabledSentinelOnEmptyLevelSet() {
        recorder = ScreenRecorder.start(UUID.randomUUID().toString(),
                logDir, 30, new HashSet<>());
        Assert.assertSame(recorder, ScreenRecorder.DISABLED,
                "empty level set should yield the DISABLED sentinel");
        Assert.assertFalse(recorder.shouldRecordFor(Level.ERROR));
        Assert.assertNull(recorder.snapshotForError("any"));
    }

    @Test
    public void disabledSentinelOnZeroBuffer() {
        Set<Level> levels = new HashSet<>();
        levels.add(Level.ERROR);
        recorder = ScreenRecorder.start(UUID.randomUUID().toString(),
                logDir, 0, levels);
        Assert.assertSame(recorder, ScreenRecorder.DISABLED);
    }

    private static void deleteRecursivelyQuietly(Path root) {
        if (root == null || !Files.exists(root)) return;
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (IOException ignored) {}
            });
        } catch (IOException ignored) {
        }
    }
}
