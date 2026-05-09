package screen;

import com.automationanywhere.botcommand.utilities.screen.recorder.FfmpegBinary;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Locale;

public class FfmpegBinaryTest {

    @Test
    public void locateExtractsBinaryOnFirstCall() throws IOException {
        Path resolved = FfmpegBinary.locate();

        Assert.assertNotNull(resolved, "locate() must return a non-null path");
        Assert.assertTrue(Files.exists(resolved),
                "extracted ffmpeg.exe should exist at " + resolved);
        Assert.assertTrue(Files.size(resolved) > 1_000_000,
                "binary should be at least ~1 MB; got " + Files.size(resolved));
    }

    @Test
    public void locateReturnsSamePathOnSecondCall() throws IOException {
        Path first = FfmpegBinary.locate();
        Path second = FfmpegBinary.locate();
        Assert.assertEquals(first, second,
                "subsequent locate() calls should return the cached path");
    }

    @Test
    public void extractedBinaryMatchesBundledHash() throws IOException {
        Path resolved = FfmpegBinary.locate();
        String actual = sha256Hex(resolved);
        Assert.assertEquals(actual.toLowerCase(Locale.ROOT),
                FfmpegBinary.BUNDLED_SHA256.toLowerCase(Locale.ROOT),
                "extracted binary's SHA-256 must equal the compile-time constant");
    }

    @Test
    public void cacheSubfolderUsesBundledHashPrefix() throws IOException {
        Path resolved = FfmpegBinary.locate();
        Assert.assertTrue(resolved.toString().contains(FfmpegBinary.BUNDLED_HASH_PREFIX),
                "cache path should include the hash-prefix subfolder; got " + resolved);
    }

    @Test
    public void appDataRootIsResolvable() throws IOException {
        Path root = FfmpegBinary.appDataRoot();
        Assert.assertTrue(Files.isDirectory(root),
                "appDataRoot() must return an existing directory; got " + root);
        Assert.assertTrue(root.toString().contains("A360-BotFramework"),
                "appDataRoot should be namespaced under A360-BotFramework; got " + root);
    }

    @Test
    public void bundledHashPrefixIsExactly12Chars() {
        Assert.assertEquals(FfmpegBinary.BUNDLED_HASH_PREFIX.length(), 12,
                "hash prefix must be exactly 12 characters for a stable cache path");
    }

    @Test
    public void resourceIsPresentInJarOrClassesDir() throws IOException {
        // Sanity check: the ffmpeg resource is reachable via the classloader.
        // If this fails, the resource path or build packaging is broken.
        try (InputStream in = FfmpegBinary.class.getResourceAsStream("/ffmpeg/ffmpeg.exe")) {
            Assert.assertNotNull(in, "/ffmpeg/ffmpeg.exe must be on the classpath");
            // Read at least the PE magic to confirm it's actually a binary
            byte[] magic = new byte[2];
            int read = in.read(magic);
            Assert.assertEquals(read, 2);
            Assert.assertEquals(magic[0], (byte) 'M', "first byte of PE binary should be 'M'");
            Assert.assertEquals(magic[1], (byte) 'Z', "second byte of PE binary should be 'Z'");
        }
    }

    private static String sha256Hex(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(file)) {
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = in.read(buf)) != -1) {
                    digest.update(buf, 0, n);
                }
            }
            byte[] hash = digest.digest();
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format(Locale.ROOT, "%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }
}
