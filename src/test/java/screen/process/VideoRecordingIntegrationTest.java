package screen.process;

import com.automationanywhere.botcommand.actions.logs.LogMessage;
import com.automationanywhere.botcommand.actions.logs.StartLoggerSession;
import com.automationanywhere.botcommand.actions.logs.StopLoggerSession;
import com.automationanywhere.botcommand.data.impl.SessionValue;
import com.automationanywhere.botcommand.utilities.logger.CustomLogger;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public class VideoRecordingIntegrationTest {

    private static final String COMMON_FILE_ALL_LEVEL = "COMMON_FILE";
    private static final String LEVEL_ERROR = "ERROR";
    private static final String LEVEL_INFO = "INFO";
    private static final String DO_NOT_LOG_VARIABLE = "NO";

    private StartLoggerSession session;
    private LogMessage logMessage;
    private StopLoggerSession stopSession;
    private CustomLogger logger;
    private Path baseDir;
    private Path logFile;

    @BeforeClass
    public void requireDesktop() {
        DesktopAvailability.requireDesktop();
    }

    @BeforeMethod
    public void setUp() throws IOException {
        session = new StartLoggerSession();
        logMessage = new LogMessage();
        logMessage.setTestBotUri("Automation Anywhere/bots/integration/video-test");
        stopSession = new StopLoggerSession();

        baseDir = Files.createTempDirectory("video-integ-");
        logFile = baseDir.resolve("log.html");
    }

    @AfterMethod
    public void tearDown() {
        if (logger != null && !logger.isClosed()) {
            try {
                stopSession.stop(logger);
            } catch (Throwable ignored) {
            }
        }
        deleteRecursivelyQuietly(baseDir);
    }

    @Test
    public void errorEntryProducesMp4AndPosterAndHtmlLinks() throws Exception {
        // Start logger with video on, ERROR-only, 5s buffer
        SessionValue sv = session.start(
                COMMON_FILE_ALL_LEVEL,
                logFile.toString(),
                null, null, null,
                100,
                /* captureScreenRecording  */ "VIDEO_ENABLED",
                /* videoOnInfo             */ false,
                /* videoOnWarn             */ false,
                /* videoOnError            */ true,
                /* videoBufferSeconds      */ 5);
        logger = (CustomLogger) sv.getSession();
        Assert.assertTrue(logger.shouldRecordVideoFor(org.apache.logging.log4j.Level.ERROR),
                "logger must report video-on for ERROR after start()");

        // Wait for the ring to have at least 2 .mov files (1 stable + 1 in-flight)
        // before logging anything - that's the minimum for snapshotForError to succeed.
        RecorderTestSupport.awaitSessionMovSegments(logger.getLoggerId(), 2,
                java.time.Duration.ofSeconds(10));

        // INFO message: no video, just a normal log entry
        logMessage.action(logger, LEVEL_INFO, "info before failure",
                false, DO_NOT_LOG_VARIABLE, null, null);

        // ERROR message: triggers poster + async stage-2 encode
        logMessage.action(logger, LEVEL_ERROR, "the actual failure",
                false, DO_NOT_LOG_VARIABLE, null, null);

        // Stop the logger. close() blocks until the encoder pool drains
        // (typically ~5-15 s with libaom-av1) and the session folder is
        // deleted. AA invokes this same path automatically via the
        // CloseableSessionObject contract when no Stop Logger Session is
        // present in the bot.
        stopSession.stop(logger);

        Path clipsDir = baseDir.resolve("clips");
        Path screenshotsDir = baseDir.resolve("screenshots");

        Assert.assertTrue(Files.isDirectory(clipsDir),
                "clips/ directory should exist next to log.html");
        long mp4Count = countByExt(clipsDir, ".mp4");
        Assert.assertEquals(mp4Count, 1L,
                "exactly one .mp4 should be in clips/; found " + mp4Count);

        long mp4Size = sumSizeByExt(clipsDir, ".mp4");
        Assert.assertTrue(mp4Size > 1000,
                "the produced mp4 should be at least ~1 KB; got " + mp4Size + " bytes");

        long posterCount = countByExt(screenshotsDir, ".png");
        Assert.assertTrue(posterCount >= 1,
                "at least one error_*.png poster should exist; found " + posterCount);

        // HTML file references the video link via the new CSS class
        String html = new String(Files.readAllBytes(logFile), StandardCharsets.UTF_8);
        Assert.assertTrue(html.contains("video-link"),
                "rendered log.html must contain 'video-link' class for the error row");
        Assert.assertTrue(html.contains("clips/") && html.contains(".mp4"),
                "rendered log.html must reference the mp4 under clips/");
        Assert.assertTrue(html.contains("<th>Screen</th>"),
                "the Screen column header must be present");

        // The session-private workspace under %LOCALAPPDATA% is cleaned by
        // close() above as part of its drain.
        Path appData = com.automationanywhere.botcommand.utilities.screen.recorder
                .FfmpegBinary.appDataRoot();
        Path sessionFolder = appData.resolve("sessions").resolve(logger.getLoggerId());
        Assert.assertFalse(Files.exists(sessionFolder),
                "session folder must be removed after close(); still at " + sessionFolder);
    }

    @Test
    public void nonRecordingLevelDoesNotProduceMp4() throws Exception {
        // Start with video on, but ONLY for ERROR; log an INFO and a WARN.
        SessionValue sv = session.start(
                COMMON_FILE_ALL_LEVEL,
                logFile.toString(),
                null, null, null,
                100,
                "VIDEO_ENABLED", false, false, true, 5);
        logger = (CustomLogger) sv.getSession();

        // No need to wait for the buffer here - INFO is not in recordingLevels,
        // so snapshotForError will not be invoked and the encoder is never engaged.
        logMessage.action(logger, LEVEL_INFO, "info, no recording",
                false, DO_NOT_LOG_VARIABLE, null, null);

        stopSession.stop(logger);

        Path clipsDir = baseDir.resolve("clips");
        long mp4Count = Files.isDirectory(clipsDir) ? countByExt(clipsDir, ".mp4") : 0;
        Assert.assertEquals(mp4Count, 0L,
                "INFO-only session must not produce any .mp4 (video off for INFO)");
    }

    private static long countByExt(Path dir, String ext) throws IOException {
        if (!Files.isDirectory(dir)) return 0;
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(p -> p.toString().toLowerCase().endsWith(ext.toLowerCase()))
                    .count();
        }
    }

    private static long sumSizeByExt(Path dir, String ext) throws IOException {
        if (!Files.isDirectory(dir)) return 0;
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(p -> p.toString().toLowerCase().endsWith(ext.toLowerCase()))
                    .mapToLong(p -> {
                        try { return Files.size(p); } catch (IOException e) { return 0; }
                    })
                    .sum();
        }
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
