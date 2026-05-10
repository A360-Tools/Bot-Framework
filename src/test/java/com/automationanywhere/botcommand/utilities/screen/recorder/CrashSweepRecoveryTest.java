package com.automationanywhere.botcommand.utilities.screen.recorder;

import org.apache.logging.log4j.Level;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import screen.process.DesktopAvailability;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Verifies that {@link CrashSweep} recovers per-error scratches into the
 * exact {@code <logDir>/clips/<uuid>.mp4} paths the HTML log already
 * references. This is the load-bearing recovery for COMPACT-mode bots
 * that exit before the encoder pool drains: the user sees a play icon
 * over a screenshot, clicks, and would normally 404 — after the next
 * bot run the file appears at that path and the link self-heals.
 *
 * <p>The test sets up the post-kill state directly (real .mov segments
 * copied into a fixture {@code scratch/<uuid>/} layout, plus
 * {@code session.meta} and {@code session.active}) rather than fighting
 * the recorder lifecycle, then drives the sweep synchronously via
 * {@link CrashSweep#sweepForTest(Path, Path)}.
 *
 * <p>Lives in the same package as {@code CrashSweep} so it can call the
 * package-private {@code sweepForTest} entry point without exposing it
 * publicly.
 */
public class CrashSweepRecoveryTest {

    private Path appDataRoot;
    private Path logDir;
    private Path seedSegmentsDir;
    private Path ffmpegExe;

    @BeforeClass
    public void requireDesktop() {
        DesktopAvailability.requireDesktop();
    }

    @BeforeMethod
    public void setUp() throws IOException {
        appDataRoot = Files.createTempDirectory("sweep-appdata-");
        logDir = Files.createTempDirectory("sweep-logdir-");
        seedSegmentsDir = Files.createTempDirectory("sweep-seed-");
        ffmpegExe = FfmpegBinary.locate();
    }

    @AfterMethod
    public void tearDown() {
        deleteRecursivelyQuietly(appDataRoot);
        deleteRecursivelyQuietly(logDir);
        deleteRecursivelyQuietly(seedSegmentsDir);
    }

    @Test
    public void recoversPendingScratchesIntoClipsDir() throws Exception {
        seedFfvhuffSegments(seedSegmentsDir, 3);

        String sessionId = UUID.randomUUID().toString();
        Path sessionFolder = appDataRoot.resolve("sessions").resolve(sessionId);

        String uuid1 = UUID.randomUUID().toString();
        String uuid2 = UUID.randomUUID().toString();
        stageScratch(sessionFolder, uuid1);
        stageScratch(sessionFolder, uuid2);
        stageRing(sessionFolder);
        writeSessionMeta(sessionFolder, sessionId, EncodingMode.COMPACT);
        Files.createFile(sessionFolder.resolve("session.active"));

        Path clipsDir = logDir.resolve("clips");
        Path expectedClip1 = clipsDir.resolve(uuid1 + ".mp4");
        Path expectedClip2 = clipsDir.resolve(uuid2 + ".mp4");
        Assert.assertFalse(Files.exists(expectedClip1), "fixture must start with no recovered clips");
        Assert.assertFalse(Files.exists(expectedClip2), "fixture must start with no recovered clips");

        CrashSweep.sweepForTest(appDataRoot, ffmpegExe);

        Assert.assertTrue(Files.exists(expectedClip1),
                "scratch for uuid1 must be recovered to " + expectedClip1);
        Assert.assertTrue(Files.exists(expectedClip2),
                "scratch for uuid2 must be recovered to " + expectedClip2);
        Assert.assertTrue(Files.size(expectedClip1) > 1000,
                "recovered clip1 should be a real mp4; size=" + Files.size(expectedClip1));
        Assert.assertTrue(Files.size(expectedClip2) > 1000,
                "recovered clip2 should be a real mp4; size=" + Files.size(expectedClip2));

        Path crashLog = clipsDir.resolve("crash-log.txt");
        Assert.assertTrue(Files.exists(crashLog), "crash-log.txt should be written");
        String log = Files.readString(crashLog, StandardCharsets.UTF_8);
        Assert.assertTrue(log.contains(sessionId),
                "crash-log should reference session " + sessionId);
        Assert.assertTrue(log.contains("pending clip(s) recovered"),
                "crash-log should record the pending-clip recovery: " + log);

        Path crashRecording = clipsDir.resolve("crash-recording-" + sessionId + ".mp4");
        Assert.assertTrue(Files.exists(crashRecording),
                "ring should also be salvaged to " + crashRecording);

        Assert.assertFalse(Files.exists(sessionFolder),
                "session folder should be removed after sweep");
        Assert.assertFalse(Files.exists(sessionFolder.resolveSibling(sessionId + ".salvaging")),
                "no .salvaging residue should remain");
    }

    @Test
    public void existingClipIsLeftIntact() throws Exception {
        seedFfvhuffSegments(seedSegmentsDir, 3);

        String sessionId = UUID.randomUUID().toString();
        Path sessionFolder = appDataRoot.resolve("sessions").resolve(sessionId);

        String uuid = UUID.randomUUID().toString();
        stageScratch(sessionFolder, uuid);
        writeSessionMeta(sessionFolder, sessionId, EncodingMode.COMPACT);
        Files.createFile(sessionFolder.resolve("session.active"));

        // Pre-stage a "previously recovered" clip with a sentinel byte stream
        // so we can detect whether the sweep overwrote it.
        Path clipsDir = logDir.resolve("clips");
        Files.createDirectories(clipsDir);
        Path target = clipsDir.resolve(uuid + ".mp4");
        byte[] sentinel = "PREVIOUSLY_RECOVERED".getBytes(StandardCharsets.UTF_8);
        Files.write(target, sentinel);

        CrashSweep.sweepForTest(appDataRoot, ffmpegExe);

        Assert.assertTrue(Files.exists(target));
        Assert.assertEquals(Files.readAllBytes(target), sentinel,
                "sweep must not overwrite an already-recovered clip");
    }

    @Test
    public void missingLogDirSkipsScratchSalvageButFallsBackForRing() throws Exception {
        seedFfvhuffSegments(seedSegmentsDir, 3);

        String sessionId = UUID.randomUUID().toString();
        Path sessionFolder = appDataRoot.resolve("sessions").resolve(sessionId);

        String uuid = UUID.randomUUID().toString();
        stageScratch(sessionFolder, uuid);
        stageRing(sessionFolder);

        // Point logDir at a path that does not exist on disk.
        Path missingLogDir = appDataRoot.resolve("nonexistent-logdir");
        writeSessionMetaWithLogDir(sessionFolder, sessionId, missingLogDir,
                EncodingMode.COMPACT);
        Files.createFile(sessionFolder.resolve("session.active"));

        CrashSweep.sweepForTest(appDataRoot, ffmpegExe);

        // Per-error clip drops on the floor (HTML log is also gone, so an
        // orphan mp4 wouldn't be reachable).
        Assert.assertFalse(Files.exists(missingLogDir.resolve("clips").resolve(uuid + ".mp4")));

        // Ring still gets salvaged to the salvaged/ fallback so the user has
        // some artifact to inspect.
        Path salvagedDir = appDataRoot.resolve("salvaged");
        Assert.assertTrue(Files.isDirectory(salvagedDir),
                "salvaged/ fallback dir should be created");
        long fallbackCount = countByExt(salvagedDir, ".mp4");
        Assert.assertEquals(fallbackCount, 1L,
                "ring should be salvaged to salvaged/<stamp>-<id>.mp4; got "
                        + fallbackCount + " mp4s");
    }

    @Test
    public void emptyScratchSubdirIsSkippedWithoutFailingOthers() throws Exception {
        seedFfvhuffSegments(seedSegmentsDir, 3);

        String sessionId = UUID.randomUUID().toString();
        Path sessionFolder = appDataRoot.resolve("sessions").resolve(sessionId);

        String goodUuid = UUID.randomUUID().toString();
        String emptyUuid = UUID.randomUUID().toString();
        stageScratch(sessionFolder, goodUuid);
        // Empty scratch dir simulates a copy that was interrupted before any
        // .mov files landed - or a stray directory left around.
        Files.createDirectories(sessionFolder.resolve("scratch").resolve(emptyUuid));
        writeSessionMeta(sessionFolder, sessionId, EncodingMode.COMPACT);
        Files.createFile(sessionFolder.resolve("session.active"));

        CrashSweep.sweepForTest(appDataRoot, ffmpegExe);

        Path clipsDir = logDir.resolve("clips");
        Assert.assertTrue(Files.exists(clipsDir.resolve(goodUuid + ".mp4")),
                "valid scratch must still recover even when a sibling is empty");
        Assert.assertFalse(Files.exists(clipsDir.resolve(emptyUuid + ".mp4")),
                "empty scratch must not produce a clip");
    }

    @Test
    public void usesEncodingModeFromSessionMeta() throws Exception {
        seedFfvhuffSegments(seedSegmentsDir, 3);

        String sessionId = UUID.randomUUID().toString();
        Path sessionFolder = appDataRoot.resolve("sessions").resolve(sessionId);

        String uuid = UUID.randomUUID().toString();
        stageScratch(sessionFolder, uuid);
        // Persist FAST so the sweep should use libx264 for recovery.
        writeSessionMeta(sessionFolder, sessionId, EncodingMode.FAST);
        Files.createFile(sessionFolder.resolve("session.active"));

        CrashSweep.sweepForTest(appDataRoot, ffmpegExe);

        Path target = logDir.resolve("clips").resolve(uuid + ".mp4");
        Assert.assertTrue(Files.exists(target));
        // Spot-check the file is a real mp4 (ftyp box at offset 4)
        byte[] head = new byte[12];
        try (java.io.InputStream in = Files.newInputStream(target)) {
            int n = in.read(head);
            Assert.assertEquals(n, 12);
        }
        String boxName = new String(head, 4, 4, StandardCharsets.US_ASCII);
        Assert.assertEquals(boxName, "ftyp",
                "first box of recovered mp4 must be 'ftyp'");
    }

    // ---- fixture helpers ----

    /**
     * Spawns a one-shot ffmpeg that captures the desktop for the given
     * duration and writes ffvhuff segments at 1-second granularity into
     * {@code outDir}. Mirrors what stage-1 of the recorder produces.
     */
    private void seedFfvhuffSegments(Path outDir, int durationSec)
            throws IOException, InterruptedException {
        String[] cmd = {
                ffmpegExe.toAbsolutePath().toString(),
                "-hide_banner",
                "-loglevel", "error",
                "-f", "gdigrab",
                "-framerate", "5",
                "-t", String.valueOf(durationSec),
                "-i", "desktop",
                "-c:v", "ffvhuff",
                "-pix_fmt", "yuv420p",
                "-f", "segment",
                "-segment_time", "1",
                outDir.resolve("%01d.mov").toAbsolutePath().toString()
        };
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        boolean done = p.waitFor(30, TimeUnit.SECONDS);
        if (!done) {
            p.destroyForcibly();
            throw new IOException("seed ffmpeg timed out");
        }
        if (p.exitValue() != 0) {
            throw new IOException("seed ffmpeg exit " + p.exitValue());
        }
    }

    /**
     * Copies the seed segments into {@code <sessionFolder>/scratch/<uuid>/},
     * simulating what {@code snapshotForError} would have left behind when
     * its async encode never finished.
     */
    private void stageScratch(Path sessionFolder, String errorUuid) throws IOException {
        Path target = sessionFolder.resolve("scratch").resolve(errorUuid);
        Files.createDirectories(target);
        try (Stream<Path> seeds = Files.list(seedSegmentsDir)) {
            seeds.filter(p -> p.getFileName().toString().endsWith(".mov"))
                    .forEach(seg -> {
                        try {
                            Files.copy(seg, target.resolve(seg.getFileName()),
                                    StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }

    private void stageRing(Path sessionFolder) throws IOException {
        Path target = sessionFolder.resolve("ring");
        Files.createDirectories(target);
        try (Stream<Path> seeds = Files.list(seedSegmentsDir)) {
            seeds.filter(p -> p.getFileName().toString().endsWith(".mov"))
                    .forEach(seg -> {
                        try {
                            Files.copy(seg, target.resolve(seg.getFileName()),
                                    StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }

    private void writeSessionMeta(Path sessionFolder, String sessionId, EncodingMode mode)
            throws IOException {
        writeSessionMetaWithLogDir(sessionFolder, sessionId, logDir, mode);
    }

    /**
     * Writes a {@code session.meta} pointing at {@code metaLogDir} regardless
     * of whether that directory exists, so tests can exercise the
     * "logDir disappeared" fallback path.
     *
     * <p>The persisted {@code jvmPid} is deliberately set to a value other
     * than the current JVM's so the sweep treats the folder as a crashed
     * prior session (the scenario under test) rather than as a live
     * same-JVM session it should leave alone.
     */
    private void writeSessionMetaWithLogDir(Path sessionFolder, String sessionId,
                                            Path metaLogDir, EncodingMode mode)
            throws IOException {
        Files.createDirectories(sessionFolder);
        JSONObject json = new JSONObject();
        json.put("schemaVersion", SessionMeta.CURRENT_SCHEMA_VERSION);
        json.put("sessionId", sessionId);
        json.put("logDir", metaLogDir.toAbsolutePath().toString());
        json.put("bufferSeconds", 5);
        json.put("recordingLevels", new JSONArray(new String[]{Level.ERROR.toString()}));
        json.put("startedAt", Instant.now().toString());
        json.put("packageVersion", "test");
        json.put("jvmPid", ProcessHandle.current().pid() + 1L);
        json.put("encodingMode", mode.name());
        Files.write(sessionFolder.resolve("session.meta"),
                json.toString(2).getBytes(StandardCharsets.UTF_8));
    }

    private static long countByExt(Path dir, String ext) throws IOException {
        if (!Files.isDirectory(dir)) {
            return 0L;
        }
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(p -> p.toString().toLowerCase().endsWith(ext.toLowerCase()))
                    .count();
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
}
