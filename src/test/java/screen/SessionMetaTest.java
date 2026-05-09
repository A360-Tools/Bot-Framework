package screen;

import com.automationanywhere.botcommand.utilities.screen.recorder.SessionMeta;
import org.apache.logging.log4j.Level;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class SessionMetaTest {

    private Path tempDir;

    @BeforeMethod
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("session-meta-test-");
    }

    @AfterMethod
    public void tearDown() {
        if (tempDir == null) return;
        try (Stream<Path> walk = Files.walk(tempDir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (IOException ignored) {}
            });
        } catch (IOException ignored) {
        }
    }

    @Test
    public void roundTripPreservesAllFields() throws IOException {
        Set<Level> levels = new HashSet<>();
        levels.add(Level.ERROR);
        levels.add(Level.WARN);

        Instant started = Instant.parse("2026-05-10T10:00:00Z");
        SessionMeta original = new SessionMeta(
                "session-uuid-abc",
                "C:\\bots\\bot-a\\logs",
                30,
                levels,
                started,
                "3.7.0",
                14728);

        Path file = tempDir.resolve("session.meta");
        original.write(file);

        SessionMeta loaded = SessionMeta.read(file);
        Assert.assertEquals(loaded.schemaVersion, SessionMeta.CURRENT_SCHEMA_VERSION);
        Assert.assertEquals(loaded.sessionId, "session-uuid-abc");
        Assert.assertEquals(loaded.logDir, "C:\\bots\\bot-a\\logs");
        Assert.assertEquals(loaded.bufferSeconds, 30);
        Assert.assertEquals(loaded.recordingLevels, levels);
        Assert.assertEquals(loaded.startedAt, started);
        Assert.assertEquals(loaded.packageVersion, "3.7.0");
        Assert.assertEquals(loaded.jvmPid, 14728L);
    }

    @Test
    public void emptyLevelSetRoundTrips() throws IOException {
        SessionMeta original = new SessionMeta(
                "session-empty",
                "C:\\logs",
                10,
                new HashSet<>(),
                Instant.now(),
                "test",
                1);
        Path file = tempDir.resolve("empty.meta");
        original.write(file);

        SessionMeta loaded = SessionMeta.read(file);
        Assert.assertTrue(loaded.recordingLevels.isEmpty());
    }

    @Test
    public void writeCreatesParentDirectories() throws IOException {
        SessionMeta meta = new SessionMeta(
                "id",
                "C:\\logs",
                30,
                new HashSet<>(),
                Instant.now(),
                "v",
                1);
        Path nested = tempDir.resolve("a/b/c/session.meta");
        meta.write(nested);

        Assert.assertTrue(Files.exists(nested));
    }

    @Test
    public void copiedSetIsIndependent() {
        Set<Level> source = new HashSet<>();
        source.add(Level.INFO);

        SessionMeta meta = new SessionMeta(
                "id", "C:\\logs", 30, source, Instant.now(), "v", 1);

        // Mutating the source should not affect meta
        source.add(Level.ERROR);
        Assert.assertEquals(meta.recordingLevels.size(), 1);
        Assert.assertTrue(meta.recordingLevels.contains(Level.INFO));
        Assert.assertFalse(meta.recordingLevels.contains(Level.ERROR));
    }
}
