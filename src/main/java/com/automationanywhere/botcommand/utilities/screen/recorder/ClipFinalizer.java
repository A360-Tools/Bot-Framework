package com.automationanywhere.botcommand.utilities.screen.recorder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Stage-2 encoder helper. Takes a directory of ffvhuff .mov segments produced
 * by ScreenRecorder's stage-1 ring buffer, runs ffmpeg with the concat demuxer
 * + libaom-av1, and lands a browser-playable mp4 at the target path. Stateless
 * and synchronous - the caller owns threading.
 *
 * <p>Used both by {@code RealScreenRecorder}'s per-error encoder pool and by
 * {@link CrashSweep} for orphan ring salvage outside any session.
 *
 * <p>Codec choice is dictated by the bundled ffmpeg, which is the AA-style
 * LGPL stripped 6.0 build with only {@code libaom_av1} and {@code ffvhuff}
 * encoders compiled in. The {@code -cpu-used 8} preset is libaom's fastest;
 * a typical 30 s clip encodes in ~5-15 s. The wall-time cost lives entirely
 * in the per-session encoder pool and never blocks the bot.
 *
 * @author Sumit Kumar
 */
public final class ClipFinalizer {

    private static final Logger LOGGER = LogManager.getLogger(ClipFinalizer.class);

    private static final int ENCODE_TIMEOUT_SECONDS = 120;

    private ClipFinalizer() {
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
}
