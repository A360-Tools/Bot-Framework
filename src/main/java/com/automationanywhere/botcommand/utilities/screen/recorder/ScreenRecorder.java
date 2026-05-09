package com.automationanywhere.botcommand.utilities.screen.recorder;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Continuous screen recorder built on a child ffmpeg process.
 *
 * <p>Stage-1 (this class): a long-lived ffmpeg captures the desktop with
 * {@code gdigrab} into a ring of ffvhuff .mov segments under
 * {@code %LOCALAPPDATA%\A360-BotFramework\sessions\<sessionId>\ring\}.
 *
 * <p>Stage-2 (delegated to {@link ClipFinalizer}): when an error fires,
 * {@link #snapshotForError(String)} copies the ring slice into a per-error
 * scratch dir and queues a concat &rarr; libaom-av1 &rarr; mp4 finalize that
 * lands under {@code <logDir>/clips/<errorUuid>.mp4}.
 *
 * <p>{@link #start} never throws. If anything goes wrong during setup it
 * returns the {@link #DISABLED} sentinel whose methods all degrade safely,
 * so the surrounding logger session always works.
 *
 * @author Sumit Kumar
 */
public abstract class ScreenRecorder implements AutoCloseable {

    private static final Logger LOGGER = LogManager.getLogger(ScreenRecorder.class);

    /** Sentinel used when video is disabled or setup failed. Always returned, never null. */
    public static final ScreenRecorder DISABLED = new Disabled();

    static final int CAPTURE_FRAMERATE = 5;
    static final int SEGMENT_TIME_SECONDS = 1;

    private static final Duration GRACEFUL_STOP = Duration.ofSeconds(3);

    /**
     * How long {@link #close()} waits for queued stage-2 encodes to finish
     * before cancelling them. Sized in tandem with
     * {@code ClipFinalizer.QUEUE_CAPACITY = 5}: 5 encodes &times; ~30 s each plus
     * 30 s headroom = 180 s. This bounds the worst-case bot-agent shutdown
     * latency at the cost of dropping the tail of pathological error bursts
     * (6th+ rapid errors lose their video; the poster PNG taken before queueing
     * is unaffected).
     */
    private static final Duration DRAIN_TIMEOUT = Duration.ofSeconds(180);

    /**
     * Starts a recorder for the given session. Returns {@link #DISABLED} if
     * {@code recordingLevels} is empty, {@code bufferSeconds} is non-positive,
     * or any setup step fails. Triggers a one-time crash-orphan sweep on the
     * first call per JVM lifetime.
     */
    public static ScreenRecorder start(String sessionId,
                                       Path logDir,
                                       int bufferSeconds,
                                       Set<Level> recordingLevels) {
        if (recordingLevels == null || recordingLevels.isEmpty() || bufferSeconds <= 0) {
            return DISABLED;
        }
        try {
            Path appDataRoot = FfmpegBinary.appDataRoot();
            Path ffmpegExe = FfmpegBinary.locate();
            CrashSweep.scheduleOnce(appDataRoot, ffmpegExe);
            return new RealScreenRecorder(sessionId, logDir, bufferSeconds,
                    new HashSet<>(recordingLevels), appDataRoot, ffmpegExe);
        } catch (Throwable t) {
            LOGGER.warn("Screen recorder disabled for session {}: {}",
                    sessionId, t.toString());
            return DISABLED;
        }
    }

    public abstract boolean shouldRecordFor(Level level);

    /**
     * Snapshots the ring slice covering the last {@code bufferSeconds} ending
     * at "now" and queues an async stage-2 encode. Returns the eventual mp4
     * path. The file may not exist until the encode finishes, which is normal.
     *
     * <p>Returns {@code null} when the recorder is disabled, has failed, the
     * encoder queue is full, or the ring has nothing usable yet (e.g. session
     * just started). Callers render a "video unavailable" chip in that case.
     */
    public abstract Path snapshotForError(String errorUuid);

    @Override
    public abstract void close();

    public abstract boolean isClosed();

    // ===== implementations =====

    private static final class Disabled extends ScreenRecorder {
        @Override public boolean shouldRecordFor(Level level) { return false; }
        @Override public Path snapshotForError(String errorUuid) { return null; }
        @Override public void close() { }
        @Override public boolean isClosed() { return true; }
    }

    private static final class RealScreenRecorder extends ScreenRecorder {
        private final String sessionId;
        private final Path logDir;
        private final int bufferSeconds;
        private final Set<Level> recordingLevels;
        private final Path sessionFolder;
        private final Path ringDir;
        private final Path scratchRoot;
        private final Path activeMarker;
        private final Path ffmpegExe;
        private final ProcessGuard guard;
        private final Process stage1Process;
        private final ClipFinalizer finalizer;
        private final AtomicBoolean stopping = new AtomicBoolean(false);
        private final AtomicBoolean failed = new AtomicBoolean(false);
        private final AtomicBoolean closed = new AtomicBoolean(false);

        RealScreenRecorder(String sessionId,
                           Path logDir,
                           int bufferSeconds,
                           Set<Level> recordingLevels,
                           Path appDataRoot,
                           Path ffmpegExe) throws IOException {
            this.sessionId = sessionId;
            this.logDir = logDir;
            this.bufferSeconds = bufferSeconds;
            this.recordingLevels = recordingLevels;
            this.ffmpegExe = ffmpegExe;

            this.sessionFolder = appDataRoot.resolve("sessions").resolve(sessionId);
            this.ringDir = sessionFolder.resolve("ring");
            this.scratchRoot = sessionFolder.resolve("scratch");
            this.activeMarker = sessionFolder.resolve("session.active");

            Files.createDirectories(ringDir);
            Files.createDirectories(scratchRoot);

            writeMeta();
            createMarker();

            this.guard = new ProcessGuard();
            try {
                this.stage1Process = guard.spawn(buildStage1Command(), null);
            } catch (IOException e) {
                guard.close();
                throw e;
            }
            startStderrWatchdog();
            this.finalizer = new ClipFinalizer(ffmpegExe, sessionId);
        }

        private void writeMeta() throws IOException {
            String packageVersion = readPackageVersion();
            long pid = ProcessHandle.current().pid();
            SessionMeta meta = new SessionMeta(
                    sessionId,
                    logDir.toAbsolutePath().toString(),
                    bufferSeconds,
                    recordingLevels,
                    Instant.now(),
                    packageVersion,
                    pid);
            meta.write(sessionFolder.resolve("session.meta"));
        }

        private void createMarker() throws IOException {
            Files.deleteIfExists(activeMarker);
            Files.createFile(activeMarker);
        }

        private String[] buildStage1Command() {
            int ringSize = bufferSeconds + 1;
            return new String[]{
                    ffmpegExe.toAbsolutePath().toString(),
                    "-hide_banner",
                    "-loglevel", "warning",
                    "-f", "gdigrab",
                    "-framerate", String.valueOf(CAPTURE_FRAMERATE),
                    "-draw_mouse", "1",
                    "-i", "desktop",
                    "-c:v", "ffvhuff",
                    "-pix_fmt", "yuv420p",
                    "-f", "segment",
                    "-segment_time", String.valueOf(SEGMENT_TIME_SECONDS),
                    "-reset_timestamps", "1",
                    "-segment_wrap", String.valueOf(ringSize),
                    "-segment_list", ringDir.resolve("list.txt").toAbsolutePath().toString(),
                    "-segment_list_size", String.valueOf(ringSize),
                    ringDir.resolve("%01d.mov").toAbsolutePath().toString()
            };
        }

        private void startStderrWatchdog() {
            Thread t = new Thread(this::pumpStderr,
                    "a360-recorder-stderr-" + shortId(sessionId));
            t.setDaemon(true);
            t.start();
        }

        private void pumpStderr() {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stage1Process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOGGER.debug("[ffmpeg-{}] {}", shortId(sessionId), line);
                }
            } catch (IOException ignored) {
                // stream closed or interrupted; fall through
            }
            // stderr ended -> the process either exited or its pipes closed.
            // If we're not the ones stopping it, treat as failure.
            if (!stopping.get()) {
                failed.set(true);
                int exit = -1;
                try {
                    exit = stage1Process.exitValue();
                } catch (IllegalThreadStateException ignored) {
                }
                LOGGER.warn("ScreenRecorder session {} stage-1 exited unexpectedly (exit={})",
                        sessionId, exit);
            }
        }

        @Override
        public boolean shouldRecordFor(Level level) {
            return !closed.get() && !failed.get() && recordingLevels.contains(level);
        }

        @Override
        public Path snapshotForError(String errorUuid) {
            if (closed.get() || failed.get()) {
                return null;
            }
            try {
                List<Path> segments = stableSegments();
                if (segments.isEmpty()) {
                    return null;   // session too young, no usable footage yet
                }

                Path scratchDir = scratchRoot.resolve(errorUuid);
                Files.createDirectories(scratchDir);
                for (Path seg : segments) {
                    Files.copy(seg, scratchDir.resolve(seg.getFileName()),
                            StandardCopyOption.COPY_ATTRIBUTES,
                            StandardCopyOption.REPLACE_EXISTING);
                }

                Path targetMp4 = logDir.resolve("clips").resolve(errorUuid + ".mp4");
                try {
                    finalizer.enqueue(scratchDir, targetMp4);
                } catch (RejectedExecutionException rejected) {
                    deleteRecursivelyQuietly(scratchDir);
                    LOGGER.debug("Encoder queue full for session {}; dropping video for {}",
                            sessionId, errorUuid);
                    return null;
                }
                return targetMp4;
            } catch (IOException e) {
                LOGGER.warn("snapshotForError({}) failed for session {}: {}",
                        errorUuid, sessionId, e.toString());
                return null;
            }
        }

        /**
         * Returns the ring's complete segments, oldest first. The most recently
         * written segment is dropped because stage-1 may still be writing to it -
         * including a partial segment with an incomplete moov atom can break
         * stage-2's concat demuxer.
         */
        private List<Path> stableSegments() throws IOException {
            try (Stream<Path> stream = Files.list(ringDir)) {
                List<Path> all = stream
                        .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".mov"))
                        .sorted(Comparator.comparingLong(this::mtimeOrZero))
                        .collect(Collectors.toList());
                if (all.size() <= 1) {
                    return java.util.Collections.emptyList();
                }
                return new java.util.ArrayList<>(all.subList(0, all.size() - 1));
            }
        }

        private long mtimeOrZero(Path p) {
            try {
                return Files.getLastModifiedTime(p).toMillis();
            } catch (IOException e) {
                return 0L;
            }
        }

        @Override
        public void close() {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            stopping.set(true);
            stopStage1();
            finalizer.drain(DRAIN_TIMEOUT);
            deleteMarkerQuietly();
            guard.close();
            deleteRecursivelyQuietly(sessionFolder);
        }

        private void stopStage1() {
            if (stage1Process == null || !stage1Process.isAlive()) {
                return;
            }
            try (OutputStream stdin = stage1Process.getOutputStream()) {
                stdin.write("q\n".getBytes(StandardCharsets.UTF_8));
                stdin.flush();
            } catch (IOException ignored) {
                // process may have already exited
            }
            try {
                if (!stage1Process.waitFor(GRACEFUL_STOP.toMillis(), TimeUnit.MILLISECONDS)) {
                    stage1Process.destroyForcibly();
                    stage1Process.waitFor(2, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                stage1Process.destroyForcibly();
                Thread.currentThread().interrupt();
            }
        }

        private void deleteMarkerQuietly() {
            try {
                Files.deleteIfExists(activeMarker);
            } catch (IOException ignored) {
            }
        }

        @Override
        public boolean isClosed() {
            return closed.get();
        }
    }

    // ===== shared static helpers =====

    static String shortId(String sessionId) {
        if (sessionId == null) return "x";
        return sessionId.length() > 8 ? sessionId.substring(0, 8) : sessionId;
    }

    static String readPackageVersion() {
        Package pkg = ScreenRecorder.class.getPackage();
        String v = pkg != null ? pkg.getImplementationVersion() : null;
        return v != null ? v : UUID.randomUUID().toString().substring(0, 8);
    }

    static void deleteRecursivelyQuietly(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
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
