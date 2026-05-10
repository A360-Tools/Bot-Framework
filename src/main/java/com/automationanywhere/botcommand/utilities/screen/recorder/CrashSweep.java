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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * Once-per-JVM background sweep that recovers recordings from sessions whose
 * JVM died before {@link ScreenRecorder#close()} could run.
 *
 * <p>For each {@code sessions\<id>} folder under
 * {@code %LOCALAPPDATA%\A360-BotFramework\}:
 * <ol>
 *   <li>If the folder name ends in {@code .salvaging} (leftover from a prior
 *       interrupted sweep), delete it unconditionally.</li>
 *   <li>Otherwise atomically rename it to {@code <id>.salvaging} to claim it.
 *       If the rename fails, another JVM has it - skip.</li>
 *   <li>If {@code session.active} exists in the claimed folder, encode the
 *       ring into a {@code crash-recording-<id>.mp4} placed under the original
 *       logDir's {@code clips/} folder (or the {@code salvaged/} fallback if
 *       the logDir is gone). Append one line to {@code crash-log.txt}.</li>
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
    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss");
    private static final DateTimeFormatter LOG_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private CrashSweep() {
    }

    /** Schedules at most one sweep across the entire JVM lifetime. Returns immediately. */
    static void scheduleOnce(Path appDataRoot, Path ffmpegExe, EncodingMode mode) {
        if (!SCHEDULED.compareAndSet(false, true)) {
            return;
        }
        EncodingMode salvageMode = mode != null ? mode : EncodingMode.FAST;
        Thread t = new Thread(() -> sweep(appDataRoot, ffmpegExe, salvageMode),
                "a360-recorder-sweep");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    private static void sweep(Path appDataRoot, Path ffmpegExe, EncodingMode mode) {
        Path sessionsRoot = appDataRoot.resolve("sessions");
        if (!Files.isDirectory(sessionsRoot)) {
            return;
        }
        try (Stream<Path> dirs = Files.list(sessionsRoot)) {
            dirs.forEach(folder -> processOne(folder, appDataRoot, ffmpegExe, mode));
        } catch (IOException e) {
            LOGGER.debug("Sweep listing failed at {}: {}", sessionsRoot, e.toString());
        }
    }

    private static void processOne(Path folder, Path appDataRoot, Path ffmpegExe,
                                   EncodingMode mode) {
        String name = folder.getFileName().toString();
        if (name.endsWith(SALVAGING_SUFFIX)) {
            ScreenRecorder.deleteRecursivelyQuietly(folder);
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
            Path marker = claimed.resolve("session.active");
            if (Files.exists(marker)) {
                trySalvage(claimed, appDataRoot, ffmpegExe, mode);
            }
        } finally {
            ScreenRecorder.deleteRecursivelyQuietly(claimed);
        }
    }

    private static void trySalvage(Path claimedFolder, Path appDataRoot, Path ffmpegExe,
                                   EncodingMode mode) {
        String folderName = claimedFolder.getFileName().toString();
        String sessionId = folderName.endsWith(SALVAGING_SUFFIX)
                ? folderName.substring(0, folderName.length() - SALVAGING_SUFFIX.length())
                : folderName;
        Path metaFile = claimedFolder.resolve("session.meta");
        Path ringDir = claimedFolder.resolve("ring");

        if (!Files.isDirectory(ringDir)) {
            LOGGER.debug("No ring/ in crashed session {}; nothing to salvage", sessionId);
            return;
        }

        SessionMeta meta;
        try {
            meta = SessionMeta.read(metaFile);
        } catch (Exception e) {
            LOGGER.debug("Skipping salvage of {}: meta unreadable ({})", sessionId, e.toString());
            return;
        }

        Path target = resolveTarget(meta, sessionId, appDataRoot);
        try {
            Files.createDirectories(target.getParent());
            ClipFinalizer.encodeSegmentsToMp4(ffmpegExe, ringDir, target, mode);
            appendCrashLog(target.getParent(), meta, target);
            LOGGER.info("Salvaged crash recording for session {} -> {}", sessionId, target);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.debug("Salvage interrupted for {}", sessionId);
        } catch (Exception e) {
            LOGGER.warn("Salvage failed for session {}: {}", sessionId, e.toString());
        }
    }

    private static Path resolveTarget(SessionMeta meta, String sessionId, Path appDataRoot) {
        if (meta.logDir != null && !meta.logDir.isEmpty()) {
            Path logDir = Path.of(meta.logDir);
            if (Files.isDirectory(logDir)) {
                return logDir.resolve("clips").resolve("crash-recording-" + sessionId + ".mp4");
            }
        }
        // Fallback: the original logDir is gone, write to salvaged/
        String stamp = TIMESTAMP.format(Instant.now().atZone(java.time.ZoneId.systemDefault()));
        return appDataRoot.resolve("salvaged").resolve(stamp + "-" + sessionId + ".mp4");
    }

    private static void appendCrashLog(Path clipsDir, SessionMeta meta, Path mp4) {
        Path log = clipsDir.resolve("crash-log.txt");
        String line = String.format(
                "%s  session %s crashed (started %s) - salvaged to %s%n",
                LOG_TIMESTAMP.format(Instant.now().atZone(java.time.ZoneId.systemDefault())),
                meta.sessionId,
                meta.startedAt,
                mp4.getFileName());
        try {
            Files.write(log, line.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOGGER.debug("Could not append to crash log {}: {}", log, e.toString());
        }
    }

}
