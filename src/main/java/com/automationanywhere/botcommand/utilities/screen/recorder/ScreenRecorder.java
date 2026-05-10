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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
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

    /**
     * Maximum time {@link RealScreenRecorder#close()} waits for in-flight
     * encodes before forcibly interrupting them. Sized for libaom-av1 with
     * {@link #ENCODER_PARALLELISM} workers: realistic bots logging a handful
     * of errors drain in ~5-30 s; 300 s is the safety ceiling for pathological
     * bursts.
     *
     * <p>The drain runs inside {@code CustomLogger.close()} (the
     * {@code CloseableSessionObject.close()} contract). AA's session container
     * invokes this reliably whether or not the bot calls
     * {@code Stop Logger Session} explicitly. We tried registering a
     * {@code BotShutdownHook} as a faster alternative, but AA's bot runner
     * only honours hooks discovered at code-gen time inside its own first-party
     * packages - {@code addBotShutdownHook(...)} from a custom package is
     * accepted but never invoked (verified empirically: every run of our bot
     * reports "total hooks: 0" in {@code Bot_*.executeBotShutdownHooks}).
     */
    private static final Duration CLOSE_DRAIN_TIMEOUT = Duration.ofSeconds(300);

    /** Sentinel used when video is disabled or setup failed. Always returned, never null. */
    public static final ScreenRecorder DISABLED = new Disabled();

    static final int CAPTURE_FRAMERATE = 5;
    static final int SEGMENT_TIME_SECONDS = 1;

    private static final Duration GRACEFUL_STOP = Duration.ofSeconds(3);

    /**
     * Maximum number of concurrent stage-2 encodes per session. The bundled
     * ffmpeg only ships with libaom-av1 (-cpu-used 8) so each encode is
     * ~5-15 s and CPU-bound; 4 workers gives parallelism on multi-core hosts
     * without oversubscribing typical 4-core bot runners. Excess submissions
     * queue and run as workers free up. The bot thread never waits on a
     * worker - it submits and returns immediately.
     */
    private static final int ENCODER_PARALLELISM = 4;

    /**
     * Upper bound on the synchronous wait inside {@code start()} for stage-1 to
     * produce its first two stable segments. ffmpeg gdigrab needs roughly one
     * {@code SEGMENT_TIME_SECONDS} interval plus startup overhead before the
     * first .mov is flushed; two segments are needed because
     * {@link RealScreenRecorder#stableSegments()} drops the in-flight one.
     * Past this deadline we give up waiting and proceed: the recorder stays
     * attached and will produce clips once segments materialise, but errors
     * fired before that happens will be silent (no clip).
     */
    private static final Duration WARMUP_TIMEOUT = Duration.ofSeconds(10);

    private static final Duration WARMUP_POLL_INTERVAL = Duration.ofMillis(100);

    /**
     * Encoder pool's bounded backlog. With 1-second chunk dedup at most one
     * encode is submitted per second on a tight loop, and a fast encoder
     * drains 4-8/sec, so a queue depth of 16 is comfortably above steady
     * state. Submissions beyond this rejecting is the desired behavior:
     * {@link RealScreenRecorder#snapshotForError} returns null on rejection
     * and the HTML log surfaces a "video unavailable" badge for that row.
     */
    private static final int ENCODER_QUEUE_CAPACITY = 16;

    /**
     * Granularity at which {@link RealScreenRecorder#snapshotForError(String)}
     * dedups across log entries: every entry whose timestamp falls in the same
     * 1-second chunk shares a single clip. Aligned with
     * {@link #SEGMENT_TIME_SECONDS} so two snapshots within the same chunk
     * would have produced byte-identical clips anyway (no new ring segment
     * has flushed in between). Drops loop-warning workloads from N clips to
     * roughly N / chunks-touched without sacrificing visual fidelity.
     */
    private static final long DEDUP_CHUNK_MILLIS = 1000L;

    /**
     * Starts a recorder for the given session. Returns {@link #DISABLED} if
     * {@code recordingLevels} is empty, {@code bufferSeconds} is non-positive,
     * or any setup step fails. Triggers a one-time crash-orphan sweep on the
     * first call per JVM lifetime.
     */
    public static ScreenRecorder start(String sessionId,
                                       Path logDir,
                                       int bufferSeconds,
                                       Set<Level> recordingLevels,
                                       EncodingMode encodingMode) {
        if (recordingLevels == null || recordingLevels.isEmpty() || bufferSeconds <= 0) {
            return DISABLED;
        }
        EncodingMode mode = encodingMode != null ? encodingMode : EncodingMode.FAST;
        try {
            Path appDataRoot = FfmpegBinary.appDataRoot();
            Path ffmpegExe = FfmpegBinary.locate();
            CrashSweep.scheduleOnce(appDataRoot, ffmpegExe, mode);
            return new RealScreenRecorder(sessionId, logDir, bufferSeconds,
                    new HashSet<>(recordingLevels), appDataRoot, ffmpegExe, mode);
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

    static final class RealScreenRecorder extends ScreenRecorder {
        private final String sessionId;
        private final Path logDir;
        private final int bufferSeconds;
        private final Set<Level> recordingLevels;
        private final EncodingMode encodingMode;
        private final Path sessionFolder;
        private final Path ringDir;
        private final Path scratchRoot;
        private final Path activeMarker;
        private final Path ffmpegExe;
        private final ProcessGuard guard;
        private final Process stage1Process;
        private final ThreadPoolExecutor encoderPool;
        private final AtomicBoolean stopping = new AtomicBoolean(false);
        private final AtomicBoolean failed = new AtomicBoolean(false);
        private final AtomicBoolean closed = new AtomicBoolean(false);
        private final AtomicLong lastChunkId = new AtomicLong(Long.MIN_VALUE);
        private final AtomicReference<Path> lastClipPath = new AtomicReference<>();

        RealScreenRecorder(String sessionId,
                           Path logDir,
                           int bufferSeconds,
                           Set<Level> recordingLevels,
                           Path appDataRoot,
                           Path ffmpegExe,
                           EncodingMode encodingMode) throws IOException {
            this.sessionId = sessionId;
            this.logDir = logDir;
            this.bufferSeconds = bufferSeconds;
            this.recordingLevels = recordingLevels;
            this.encodingMode = encodingMode;
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
            this.encoderPool = newEncoderPool(sessionId);
            awaitWarmup();
        }

        /**
         * Per-session daemon worker pool for stage-2 encodes. Daemon threads
         * because their lifecycle is bounded by {@link #close()}, which drains
         * the pool synchronously before returning. JVM-wide hooks cannot be
         * relied on in the AA Bot Runner host - the JVM is terminated after
         * the bot's last action regardless of thread daemon-ness or registered
         * shutdown hooks.
         */
        private static ThreadPoolExecutor newEncoderPool(String sessionId) {
            return new ThreadPoolExecutor(
                    ENCODER_PARALLELISM, ENCODER_PARALLELISM,
                    0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(ENCODER_QUEUE_CAPACITY),
                    r -> {
                        Thread t = new Thread(r, "a360-clip-encoder-" + shortId(sessionId));
                        t.setDaemon(true);
                        return t;
                    });
        }

        /**
         * Blocks until stage-1 has flushed at least two segments to the ring
         * (so {@link #stableSegments()} can return non-empty), or until the
         * stage-1 process dies, or until {@link #WARMUP_TIMEOUT} elapses.
         * Never throws: if warmup fails we still return a working recorder
         * that callers can use; subsequent {@code shouldRecordFor()} calls
         * reflect actual stage-1 health.
         */
        private void awaitWarmup() {
            Instant start = Instant.now();
            Instant deadline = start.plus(WARMUP_TIMEOUT);
            long pollMs = WARMUP_POLL_INTERVAL.toMillis();
            while (true) {
                if (!stage1Process.isAlive()) {
                    failed.set(true);
                    LOGGER.warn("ScreenRecorder session {} stage-1 died during warmup (exit={})",
                            sessionId, safeExitValue());
                    return;
                }
                if (countMovSegments() >= 2) {
                    long ms = Duration.between(start, Instant.now()).toMillis();
                    LOGGER.info("ScreenRecorder session {} warm in {} ms", sessionId, ms);
                    return;
                }
                if (!Instant.now().isBefore(deadline)) {
                    LOGGER.warn("ScreenRecorder session {} did not produce 2 stable segments "
                                    + "within {} ms; proceeding without warm buffer",
                            sessionId, WARMUP_TIMEOUT.toMillis());
                    return;
                }
                try {
                    Thread.sleep(pollMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOGGER.warn("ScreenRecorder session {} warmup interrupted; proceeding",
                            sessionId);
                    return;
                }
            }
        }

        private long countMovSegments() {
            try (Stream<Path> stream = Files.list(ringDir)) {
                return stream
                        .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".mov"))
                        .count();
            } catch (IOException e) {
                return 0L;
            }
        }

        private int safeExitValue() {
            try {
                return stage1Process.exitValue();
            } catch (IllegalThreadStateException e) {
                return -1;
            }
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
            // Dedup at 1-second granularity (DEDUP_CHUNK_MILLIS, aligned with
            // SEGMENT_TIME_SECONDS). Two warnings inside the same chunk would
            // produce byte-identical clips - same stable ring segments - so
            // returning the cached path here saves a ring->scratch copy on the
            // bot thread and keeps the encoder queue from filling on tight
            // loops. Cross-level: any prior call within the chunk wins
            // regardless of which level fired it.
            long chunkId = System.currentTimeMillis() / DEDUP_CHUNK_MILLIS;
            if (lastChunkId.get() == chunkId) {
                Path cached = lastClipPath.get();
                if (cached != null) {
                    return cached;
                }
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
                    encoderPool.execute(() -> runEncode(scratchDir, targetMp4));
                    lastChunkId.set(chunkId);
                    lastClipPath.set(targetMp4);
                    return targetMp4;
                } catch (RejectedExecutionException rejected) {
                    // Queue is at capacity (ENCODER_QUEUE_CAPACITY) or the pool
                    // was shut down between our closed-check and submit. Either
                    // way the snapshot won't encode; clean up scratch and let
                    // the caller render a "video unavailable" indicator.
                    deleteRecursivelyQuietly(scratchDir);
                    LOGGER.debug("Encoder pool rejected task for session {} ({})",
                            sessionId, errorUuid);
                    return null;
                }
            } catch (IOException e) {
                LOGGER.warn("snapshotForError({}) failed for session {}: {}",
                        errorUuid, sessionId, e.toString());
                return null;
            }
        }

        /**
         * Encoder task body. Runs on a worker thread of {@link #encoderPool}.
         * Always cleans up its scratch dir; if interrupted via shutdownNow
         * the scratch dir is left behind for next-startup CrashSweep.
         */
        private void runEncode(Path scratchDir, Path targetMp4) {
            try {
                ClipFinalizer.encodeSegmentsToMp4(ffmpegExe, scratchDir, targetMp4, encodingMode);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.debug("Encode interrupted for {}; scratch left for next-startup sweep",
                        targetMp4);
                return;   // skip scratch cleanup; CrashSweep will do it
            } catch (Throwable t) {
                LOGGER.warn("Failed to finalize clip {}: {}", targetMp4, t.toString());
            }
            deleteRecursivelyQuietly(scratchDir);
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

        /**
         * Drains the encoder pool synchronously and deletes the session
         * folder. AA invokes this via {@code CloseableSessionObject.close()}
         * either when the bot reaches {@code Stop Logger Session} or when
         * the session container is cleaned up at bot end. Either way, this
         * is the only callback we can rely on, so the wait happens here.
         */
        @Override
        public void close() {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            stopping.set(true);
            stopStage1();
            encoderPool.shutdown();
            boolean drained = false;
            try {
                drained = encoderPool.awaitTermination(
                        CLOSE_DRAIN_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (!drained) {
                encoderPool.shutdownNow();
                LOGGER.warn("Encoder pool did not drain within {} for session {}; "
                                + "interrupting in-flight encodes",
                        CLOSE_DRAIN_TIMEOUT, sessionId);
            }
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
