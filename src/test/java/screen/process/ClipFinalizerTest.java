package screen.process;

import com.automationanywhere.botcommand.utilities.screen.recorder.ClipFinalizer;
import com.automationanywhere.botcommand.utilities.screen.recorder.FfmpegBinary;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClipFinalizerTest {

    private Path fixtureDir;
    private Path outputDir;
    private Path ffmpegExe;

    @BeforeMethod
    public void setUp() throws IOException {
        DesktopAvailability.requireDesktop();   // bundled ffmpeg has no testsrc/color/null source
        ffmpegExe = FfmpegBinary.locate();      // filters - we use gdigrab to seed fixture .mov files
        fixtureDir = Files.createTempDirectory("cliptest-fixture-");
        outputDir = Files.createTempDirectory("cliptest-output-");
    }

    @AfterMethod
    public void tearDown() {
        deleteRecursivelyQuietly(fixtureDir);
        deleteRecursivelyQuietly(outputDir);
    }

    @Test
    public void encodesLavfiSegmentsToPlayableMp4() throws Exception {
        generateFfvhuffSegments(fixtureDir, 3);   // ~3 segments of 1 s each
        List<Path> segments = listMov(fixtureDir);
        Assert.assertTrue(segments.size() >= 2,
                "fixture should produce at least 2 .mov segments; got " + segments.size());

        Path target = outputDir.resolve("out.mp4");
        ClipFinalizer.encodeSegmentsToMp4(ffmpegExe, fixtureDir, target);

        Assert.assertTrue(Files.exists(target), "output mp4 must exist at " + target);
        long size = Files.size(target);
        Assert.assertTrue(size > 1000, "output mp4 should be at least ~1 KB; got " + size);

        // Sanity check: the mp4 has the standard ftyp box at the start (libaom + faststart).
        try (java.io.InputStream in = Files.newInputStream(target)) {
            byte[] head = new byte[12];
            int n = in.read(head);
            Assert.assertEquals(n, 12);
            // bytes 4-7 should be "ftyp" for any well-formed MP4
            String boxName = new String(head, 4, 4, java.nio.charset.StandardCharsets.US_ASCII);
            Assert.assertEquals(boxName, "ftyp",
                    "first box of mp4 must be 'ftyp'; got '" + boxName + "'");
        }
    }

    @Test(expectedExceptions = IOException.class)
    public void emptyDirectoryThrows() throws Exception {
        Path target = outputDir.resolve("empty.mp4");
        ClipFinalizer.encodeSegmentsToMp4(ffmpegExe, fixtureDir, target);
    }

    /**
     * Spawn a real ffmpeg with gdigrab as input, ffvhuff codec, and the segment
     * muxer to mirror what ScreenRecorder's stage-1 produces. Requires an
     * interactive desktop. Synchronous: returns when ffmpeg exits.
     */
    private void generateFfvhuffSegments(Path outDir, int durationSec)
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
            throw new IOException("fixture generation timed out");
        }
        if (p.exitValue() != 0) {
            String tail = readOutput(p);
            throw new IOException("fixture ffmpeg exit code " + p.exitValue()
                    + (tail.isEmpty() ? "" : "; output: " + tail));
        }
    }

    private static String readOutput(Process p) {
        try (java.io.BufferedReader r = new java.io.BufferedReader(
                new java.io.InputStreamReader(p.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            int n = 0;
            while (n++ < 8 && (line = r.readLine()) != null) {
                sb.append(line).append(" | ");
            }
            return sb.toString();
        } catch (IOException e) {
            return "";
        }
    }

    private static List<Path> listMov(Path dir) throws IOException {
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(p -> p.toString().toLowerCase().endsWith(".mov"))
                    .collect(Collectors.toList());
        }
    }

    private static void deleteRecursivelyQuietly(Path root) {
        if (root == null || !Files.exists(root)) return;
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
