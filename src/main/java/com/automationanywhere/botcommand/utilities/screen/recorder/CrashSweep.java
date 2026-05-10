package com.automationanywhere.botcommand.utilities.screen.recorder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * Once-per-JVM background sweep that recovers recordings left behind by
 * sessions whose JVM died, or whose {@link ScreenRecorder#close()} drain
 * timed out, before the encoder pool finished all queued encodes.
 *
 * <p>For each {@code sessions\<id>} folder under
 * {@code %LOCALAPPDATA%\A360-BotFramework\}:
 * <ol>
 *   <li>If the folder name ends in {@code .salvaging} (leftover from a prior
 *       interrupted sweep), delete it unconditionally.</li>
 *   <li>Otherwise atomically rename it to {@code <id>.salvaging} to claim it.
 *       If the rename fails, another JVM has it - skip.</li>
 *   <li>If {@code session.active} exists in the claimed folder, salvage in
 *       this order:
 *       <ul>
 *         <li>Encode any pending {@code scratch/<errorUuid>/} subdirs into
 *             {@code <logDir>/clips/<errorUuid>.mp4} using the encoding
 *             mode persisted in {@link SessionMeta}. The HTML log already
 *             references those exact paths, so previously-broken video
 *             links self-heal once the next bot run completes the sweep.</li>
 *         <li>If the ring has any complete segments, encode them into a
 *             {@code crash-recording-<id>.mp4} alongside the per-error
 *             clips.</li>
 *         <li>Append one summary line to {@code crash-log.txt}.</li>
 *       </ul></li>
 *   <li>Delete the claimed folder.</li>
 * </ol>
 *
 * <p>Runs on a daemon thread at {@link Thread#MIN_PRIORITY} so it yields to
 * the active session's actual work.
 *
 * @author Sumit Kumar
 */
final class CrashSweep {

    private static final Logger LOGGER = LogManager.getLogger(CrashSweep.class);

    private static final AtomicBoolean SCHEDULED = new AtomicBoolean(false);
    private static final String SALVAGING_SUFFIX = ".salvaging";
    private static final String SCRATCH_DIR = "scratch";
    private static final String RING_DIR = "ring";
    private static final String SESSION_ACTIVE_MARKER = "session.active";
    private static final String SESSION_META = "session.meta";
    private static final String CLIPS_DIR = "clips";
    private static final String CRASH_LOG = "crash-log.txt";
    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss");
    private static final DateTimeFormatter LOG_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private CrashSweep() {
    }

    /** Schedules at most one sweep across the entire JVM lifetime. Returns immediately. */
    static void scheduleOnce(Path appDataRoot, Path ffmpegExe) {
        if (!SCHEDULED.compareAndSet(false, true)) {
            return;
        }
        Thread t = new Thread(() -> sweep(appDataRoot, ffmpegExe), "a360-recorder-sweep");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    /**
     * Synchronous entry point used by tests. Bypasses the SCHEDULED guard so
     * tests can drive the sweep deterministically without spinning up a
     * daemon thread.
     */
    static void sweepForTest(Path appDataRoot, Path ffmpegExe) {
        sweep(appDataRoot, ffmpegExe);
    }

    private static void sweep(Path appDataRoot, Path ffmpegExe) {
        Path sessionsRoot = appDataRoot.resolve("sessions");
        if (!Files.isDirectory(sessionsRoot)) {
            return;
        }
        try (Stream<Path> dirs = Files.list(sessionsRoot)) {
            dirs.forEach(folder -> processOne(folder, appDataRoot, ffmpegExe));
        } catch (IOException e) {
            LOGGER.debug("Sweep listing failed at {}: {}", sessionsRoot, e.toString());
        }
    }

    private static void processOne(Path folder, Path appDataRoot, Path ffmpegExe) {
        String name = folder.getFileName().toString();
        if (name.endsWith(SALVAGING_SUFFIX)) {
            ScreenRecorder.deleteRecursivelyQuietly(folder);
            return;
        }
        // Skip folders that belong to the running JVM. The sweep daemon races
        // with sibling sessions starting up in the same JVM: without this
        // guard the daemon could claim a freshly-created session folder
        // before its constructor finishes writing markers and end up deleting
        // a live session's ring/scratch dirs. If the meta is missing or
        // unreadable, the folder is mid-construction or corrupt - either way
        // skip and let the next JVM start handle it.
        if (belongsToCurrentJvm(folder)) {
            return;
        }
        Path claimed = folder.resolveSibling(name + SALVAGING_SUFFIX);
        try {
            Files.move(folder, claimed, StandardCopyOption.ATOMIC_MOVE);
        } catch (NoSuchFileException e) {
            return;   // racing JVM already moved it
        } catch (IOException e) {
            LOGGER.debug("Could not claim {}: {}", folder, e.toString());
            return;
        }

        try {
            Path marker = claimed.resolve(SESSION_ACTIVE_MARKER);
            if (Files.exists(marker)) {
                trySalvage(claimed, appDataRoot, ffmpegExe);
            }
        } finally {
            ScreenRecorder.deleteRecursivelyQuietly(claimed);
        }
    }

    private static boolean belongsToCurrentJvm(Path folder) {
        Path metaFile = folder.resolve(SESSION_META);
        if (!Files.exists(metaFile)) {
            return true;
        }
        try {
            SessionMeta meta = SessionMeta.read(metaFile);
            return meta.jvmPid == ProcessHandle.current().pid();
        } catch (Exception e) {
            return true;
        }
    }

    private static void trySalvage(Path claimedFolder, Path appDataRoot, Path ffmpegExe) {
        String folderName = claimedFolder.getFileName().toString();
        String sessionId = folderName.endsWith(SALVAGING_SUFFIX)
                ? folderName.substring(0, folderName.length() - SALVAGING_SUFFIX.length())
                : folderName;
        Path metaFile = claimedFolder.resolve(SESSION_META);

        SessionMeta meta;
        try {
            meta = SessionMeta.read(metaFile);
        } catch (Exception e) {
            LOGGER.debug("Skipping salvage of {}: meta unreadable ({})", sessionId, e.toString());
            return;
        }

        EncodingMode mode = meta.encodingMode != null ? meta.encodingMode : EncodingMode.FAST;
        Path logDir = resolveLogDir(meta);

        // 1. Per-error scratches: re-encode each into the exact path the HTML
        //    log already references. This is the load-bearing recovery for
        //    drain-timeout / kill-mid-encode scenarios.
        List<Path> recoveredClips = recoverPendingScratches(claimedFolder, ffmpegExe, mode,
                logDir, sessionId, appDataRoot);

        // 2. Ring buffer: produce one crash-recording-<id>.mp4 alongside the
        //    per-error clips. Useful for hard-kill cases where the ring still
        //    holds the last bufferSeconds of footage; mostly noise for clean
        //    drain-timeout cases (ring content is "moment of close") but
        //    cheap and consistent.
        Path crashRecording = trySalvageRing(claimedFolder, ffmpegExe, mode,
                logDir, sessionId, appDataRoot);

        if (!recoveredClips.isEmpty() || crashRecording != null) {
            Path summaryDir = crashRecording != null ? crashRecording.getParent()
                    : recoveredClips.get(0).getParent();
            appendCrashLog(summaryDir, meta, crashRecording, recoveredClips);
        }
    }

    /**
     * Resolves the original logDir from {@link SessionMeta} if it still
     * exists on disk, otherwise returns null. The {@code salvaged/} fallback
     * for the ring crash-recording is computed separately because per-error
     * clips have no useful fallback when the logDir is gone (the HTML log
     * referencing them is also gone).
     */
    private static Path resolveLogDir(SessionMeta meta) {
        if (meta.logDir == null || meta.logDir.isEmpty()) {
            return null;
        }
        Path logDir = Path.of(meta.logDir);
        return Files.isDirectory(logDir) ? logDir : null;
    }

    /**
     * Walks {@code claimedFolder/scratch/<errorUuid>/} subdirs and re-encodes
     * each into {@code <logDir>/clips/<errorUuid>.mp4}. Each subdir is
     * processed independently: a corrupt or empty scratch fails its own
     * encode and the loop continues. Already-existing target mp4s are
     * skipped (idempotent: a partially-completed prior sweep can resume).
     *
     * <p>If the original logDir is gone, the per-error clips are dropped on
     * the floor: the HTML log they would have populated is also gone, so
     * orphan mp4s under {@code salvaged/} would be unreachable and just leak
     * disk. Skip and let the scratches be cleaned along with the rest of
     * the session folder.
     *
     * @return the absolute paths of clips that were successfully encoded in
     *         this pass (empty when there are no scratches, when logDir is
     *         missing, or when every scratch failed to encode).
     */
    private static List<Path> recoverPendingScratches(Path claimedFolder, Path ffmpegExe,
                                                      EncodingMode mode, Path logDir,
                                                      String sessionId, Path appDataRoot) {
        Path scratchRoot = claimedFolder.resolve(SCRATCH_DIR);
        if (!Files.isDirectory(scratchRoot)) {
            return List.of();
        }
        if (logDir == null) {
            LOGGER.info("Session {} has pending scratches but original logDir is gone; "
                    + "nothing useful to recover (HTML log is also missing)", sessionId);
            return List.of();
        }
        Path clipsDir = logDir.resolve(CLIPS_DIR);
        try {
            Files.createDirectories(clipsDir);
        } catch (IOException e) {
            LOGGER.warn("Could not create clips dir {} for session {}: {}",
                    clipsDir, sessionId, e.toString());
            return List.of();
        }

        List<Path> recovered = new ArrayList<>();
        try (Stream<Path> dirs = Files.list(scratchRoot)) {
            dirs.filter(Files::isDirectory).forEach(scratchDir -> {
                String errorUuid = scratchDir.getFileName().toString();
                Path target = clipsDir.resolve(errorUuid + ".mp4");
                if (Files.exists(target)) {
                    LOGGER.debug("Skipping already-recovered clip {} for session {}",
                            target.getFileName(), sessionId);
                    return;
                }
                try {
                    ClipFinalizer.encodeSegmentsToMp4(ffmpegExe, scratchDir, target, mode);
                    recovered.add(target);
                    LOGGER.info("Recovered pending clip for session {} -> {}", sessionId, target);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOGGER.debug("Recovery interrupted for {}/{}", sessionId, errorUuid);
                } catch (IOException e) {
                    LOGGER.debug("Could not recover scratch {}/{} for session {}: {}",
                            sessionId, errorUuid, mode, e.toString());
                }
            });
        } catch (IOException e) {
            LOGGER.debug("Could not list scratches under {}: {}", scratchRoot, e.toString());
        }
        return recovered;
    }

    /**
     * Encodes the ring buffer to {@code <logDir>/clips/crash-recording-<id>.mp4}
     * (or {@code salvaged/<stamp>-<id>.mp4} if the original logDir is gone).
     * Returns the encoded path, or null when the ring is empty or the encode
     * fails.
     */
    private static Path trySalvageRing(Path claimedFolder, Path ffmpegExe, EncodingMode mode,
                                       Path logDir, String sessionId, Path appDataRoot) {
        Path ringDir = claimedFolder.resolve(RING_DIR);
        if (!Files.isDirectory(ringDir)) {
            LOGGER.debug("No ring/ in session {}; skipping ring salvage", sessionId);
            return null;
        }
        Path target = resolveRingTarget(logDir, sessionId, appDataRoot);
        try {
            Files.createDirectories(target.getParent());
            ClipFinalizer.encodeSegmentsToMp4(ffmpegExe, ringDir, target, mode);
            LOGGER.info("Salvaged ring buffer for session {} -> {}", sessionId, target);
            return target;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.debug("Ring salvage interrupted for {}", sessionId);
            return null;
        } catch (Exception e) {
            // Empty rings are normal when stage-1 produced nothing usable; log
            // at debug to avoid alarming users with a stack-y warning.
            LOGGER.debug("Ring salvage skipped for session {}: {}", sessionId, e.toString());
            return null;
        }
    }

    private static Path resolveRingTarget(Path logDir, String sessionId, Path appDataRoot) {
        if (logDir != null) {
            return logDir.resolve(CLIPS_DIR).resolve("crash-recording-" + sessionId + ".mp4");
        }
        String stamp = TIMESTAMP.format(Instant.now().atZone(java.time.ZoneId.systemDefault()));
        return appDataRoot.resolve("salvaged").resolve(stamp + "-" + sessionId + ".mp4");
    }

    private static void appendCrashLog(Path clipsDir, SessionMeta meta, Path crashRecording,
                                       List<Path> recoveredClips) {
        Path log = clipsDir.resolve(CRASH_LOG);
        StringBuilder line = new StringBuilder(160)
                .append(LOG_TIMESTAMP.format(Instant.now().atZone(java.time.ZoneId.systemDefault())))
                .append("  session ").append(meta.sessionId)
                .append(" crashed (started ").append(meta.startedAt)
                .append(", encoder ").append(meta.encodingMode).append(")");
        if (crashRecording != null) {
            line.append(" - ring salvaged to ").append(crashRecording.getFileName());
        }
        if (!recoveredClips.isEmpty()) {
            line.append(" - ").append(recoveredClips.size()).append(" pending clip(s) recovered");
        }
        line.append(System.lineSeparator());
        try {
            Files.write(log, line.toString().getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOGGER.debug("Could not append to crash log {}: {}", log, e.toString());
        }
    }
}
