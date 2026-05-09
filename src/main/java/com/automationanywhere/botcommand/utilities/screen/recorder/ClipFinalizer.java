package com.automationanywhere.botcommand.utilities.screen.recorder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Per-session encoder: takes a directory of ffvhuff .mov segments, runs
 * stage-2 ffmpeg (concat &rarr; libaom-av1 &rarr; mp4), and lands the result at
 * the requested target path.
 *
 * <p>One thread per instance, bounded queue (size 8). Excess submissions
 * throw {@link RejectedExecutionException} so the caller can render a
 * "video unavailable" chip instead of stalling.
 *
 * <p>The static {@link #encodeSegmentsToMp4(Path, Path, Path)} is reused
 * directly by {@link CrashSweep} for orphan salvage outside any session.
 *
 * @author Sumit Kumar
 */
public final class ClipFinalizer {

    private static final Logger LOGGER = LogManager.getLogger(ClipFinalizer.class);

    /**
     * Maximum number of pending finalize tasks per session. Sized in tandem with
     * {@code ScreenRecorder.DRAIN_TIMEOUT}: at session close we wait
     * (QUEUE_CAPACITY * worst-case-encode-time) for everything to clear, so this
     * cap directly bounds the worst-case shutdown latency. With cpu-used 8 AV1,
     * each clip encodes in ~10-30 s, so queue 5 + drain 180 s leaves 30 s of
     * headroom for the in-flight encode at drain start.
     */
    private static final int QUEUE_CAPACITY = 5;
    private static final int ENCODE_TIMEOUT_SECONDS = 120;

    private final Path ffmpegExe;
    private final ThreadPoolExecutor encoder;

    public ClipFinalizer(Path ffmpegExe, String sessionId) {
        this.ffmpegExe = ffmpegExe;
        this.encoder = new ThreadPoolExecutor(
                1, 1,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                r -> {
                    Thread t = new Thread(r, "a360-clip-encoder-" + shortId(sessionId));
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.AbortPolicy());
    }

    /**
     * Submits a finalize task. The task encodes segments in {@code scratchDir}
     * to {@code targetMp4}, then deletes {@code scratchDir} regardless of
     * outcome. Throws {@link RejectedExecutionException} when the queue is full.
     */
    public void enqueue(Path scratchDir, Path targetMp4) {
        encoder.execute(() -> runFinalize(ffmpegExe, scratchDir, targetMp4));
    }

    /**
     * Stops accepting new tasks and waits up to {@code timeout} for in-flight
     * encodes to complete. Anything not done in time is interrupted; its scratch
     * dir is left for next-startup orphan sweep.
     */
    public void drain(Duration timeout) {
        encoder.shutdown();
        try {
            if (!encoder.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                encoder.shutdownNow();
                encoder.awaitTermination(2, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            encoder.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Encodes all .mov files in {@code segmentsDir} into {@code targetMp4}.
     * Synchronous - blocks the caller's thread for the full duration of the
     * stage-2 ffmpeg invocation.
     *
     * @throws IOException          on any io / ffmpeg failure
     * @throws InterruptedException if the calling thread is interrupted while
     *                              waiting for ffmpeg
     */
    public static void encodeSegmentsToMp4(Path ffmpegExe, Path segmentsDir, Path targetMp4)
            throws IOException, InterruptedException {
        List<Path> segments = listMovFilesByMtimeAsc(segmentsDir);
        if (segments.isEmpty()) {
            throw new IOException("No .mov segments to encode in " + segmentsDir);
        }

        Path concatList = segmentsDir.resolve("concat-list.txt");
        writeConcatList(segments, concatList);

        Path tmpOut = segmentsDir.resolve("out.mp4.tmp");
        Files.deleteIfExists(tmpOut);

        String[] cmd = {
                ffmpegExe.toAbsolutePath().toString(),
                "-hide_banner",
                "-loglevel", "error",
                "-f", "concat",
                "-safe", "0",
                "-fflags", "+igndts+genpts",
                "-i", concatList.toAbsolutePath().toString(),
                "-c:v", "libaom-av1",
                "-b:v", "1500k",
                "-cpu-used", "8",
                "-row-mt", "1",
                "-tile-columns", "2",
                "-g", "60",
                "-pix_fmt", "yuv420p",
                "-movflags", "+faststart",
                "-f", "mp4",   // explicit; the .tmp suffix prevents inference from extension
                tmpOut.toAbsolutePath().toString()
        };

        Process process = new ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start();

        int exitCode;
        try {
            boolean done = process.waitFor(ENCODE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!done) {
                process.destroyForcibly();
                throw new IOException("ffmpeg encode timed out after "
                        + ENCODE_TIMEOUT_SECONDS + " s for " + targetMp4);
            }
            exitCode = process.exitValue();
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw e;
        }

        if (exitCode != 0) {
            String tail = readFirstLines(process, 8);
            throw new IOException("ffmpeg encode exit code " + exitCode
                    + " for " + targetMp4 + (tail.isEmpty() ? "" : "; tail: " + tail));
        }

        if (!Files.exists(tmpOut) || Files.size(tmpOut) == 0) {
            throw new IOException("ffmpeg produced empty output at " + tmpOut);
        }

        Files.createDirectories(targetMp4.getParent());
        try {
            Files.move(tmpOut, targetMp4, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            // Cross-volume - the copy is non-atomic at byte level but the destination
            // filename only appears once the copy completes.
            Files.move(tmpOut, targetMp4, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void runFinalize(Path ffmpegExe, Path scratchDir, Path targetMp4) {
        try {
            encodeSegmentsToMp4(ffmpegExe, scratchDir, targetMp4);
        } catch (InterruptedException e) {
            LOGGER.debug("Encode interrupted for {}; scratch left for next-startup sweep",
                    targetMp4);
            Thread.currentThread().interrupt();
            return;
        } catch (Throwable t) {
            LOGGER.warn("Failed to finalize clip {}: {}", targetMp4, t.toString());
        }
        deleteRecursivelyQuietly(scratchDir);
    }

    private static void writeConcatList(List<Path> segments, Path concatList) throws IOException {
        StringBuilder sb = new StringBuilder(segments.size() * 80);
        for (Path seg : segments) {
            String posixPath = seg.toAbsolutePath().toString().replace('\\', '/');
            // Escape single quotes inside the file directive: ' -> '\''
            String escaped = posixPath.replace("'", "'\\''");
            sb.append("file '").append(escaped).append("'\n");
        }
        Files.write(concatList, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static List<Path> listMovFilesByMtimeAsc(Path dir) throws IOException {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".mov"))
                    .sorted(Comparator.comparingLong(ClipFinalizer::mtimeOrZero))
                    .collect(java.util.stream.Collectors.toList());
        }
    }

    private static long mtimeOrZero(Path p) {
        try {
            return Files.getLastModifiedTime(p).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    private static String readFirstLines(Process process, int max) {
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            List<String> lines = new ArrayList<>(max);
            String line;
            while (lines.size() < max && (line = reader.readLine()) != null) {
                lines.add(line);
            }
            return String.join(" | ", lines);
        } catch (IOException e) {
            return "";
        }
    }

    private static void deleteRecursivelyQuietly(Path root) {
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

    private static String shortId(String sessionId) {
        if (sessionId == null) {
            return "x";
        }
        return sessionId.length() > 8 ? sessionId.substring(0, 8) : sessionId;
    }
}
