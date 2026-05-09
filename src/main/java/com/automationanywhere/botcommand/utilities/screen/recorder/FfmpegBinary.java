package com.automationanywhere.botcommand.utilities.screen.recorder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Objects;

/**
 * Locates and extracts the bundled ffmpeg.exe to a per-user, content-addressed
 * cache under %LOCALAPPDATA%\A360-BotFramework\ffmpeg\&lt;hash&gt;\ffmpeg.exe.
 *
 * @author Sumit Kumar
 */
public final class FfmpegBinary {

    private static final Logger LOGGER = LogManager.getLogger(FfmpegBinary.class);
    private static final String JAR_RESOURCE_PATH = "/ffmpeg/ffmpeg.exe";
    private static final String APP_FOLDER_NAME = "A360-BotFramework";

    /**
     * Full SHA-256 of the bundled ffmpeg.exe. Compile-time constant; the build
     * verifies the bundled resource hashes to this value via FfmpegBinaryTest.
     */
    public static final String BUNDLED_SHA256 =
            "7dae68267fe9190e9bd5e2f58df89681a4126dc7e91235aaeb224ce2a53bb754";

    /** First 12 hex chars of {@link #BUNDLED_SHA256} - used as the cache subfolder. */
    public static final String BUNDLED_HASH_PREFIX = BUNDLED_SHA256.substring(0, 12);

    private FfmpegBinary() {
    }

    /**
     * Returns the path to a working ffmpeg.exe, extracting it from the package jar
     * to %LOCALAPPDATA%\A360-BotFramework\ffmpeg\&lt;hash&gt;\ on first call per-user.
     *
     * @throws IOException if the binary cannot be extracted to any writable location
     */
    public static Path locate() throws IOException {
        IOException firstFailure = null;
        for (Path root : candidateRoots()) {
            try {
                return locateAt(root);
            } catch (IOException e) {
                LOGGER.debug("ffmpeg cache root {} unusable: {}", root, e.getMessage());
                if (firstFailure == null) {
                    firstFailure = e;
                }
            }
        }
        throw new IOException("Could not extract bundled ffmpeg.exe to any cache root",
                firstFailure);
    }

    /**
     * Returns the directory under which session.meta + session.active + ring/ for
     * each ScreenRecorder session live. Same root as {@link #locate()} cache.
     */
    public static Path appDataRoot() throws IOException {
        for (Path root : candidateRoots()) {
            try {
                Files.createDirectories(root);
                return root;
            } catch (IOException e) {
                LOGGER.debug("appData root {} unusable: {}", root, e.getMessage());
            }
        }
        throw new IOException("No writable application-data root for screen recorder");
    }

    private static Path locateAt(Path root) throws IOException {
        Path target = root.resolve("ffmpeg")
                .resolve(BUNDLED_HASH_PREFIX)
                .resolve("ffmpeg.exe");

        if (Files.exists(target)) {
            String existing = sha256Hex(target);
            if (BUNDLED_SHA256.equalsIgnoreCase(existing)) {
                return target;
            }
            LOGGER.warn("Cached ffmpeg.exe at {} has hash {} (expected {}); re-extracting",
                    target, existing, BUNDLED_SHA256);
            Files.delete(target);
        }

        Files.createDirectories(target.getParent());
        Path tmp = target.resolveSibling("ffmpeg.exe.tmp");
        Files.deleteIfExists(tmp);

        try (InputStream in = Objects.requireNonNull(
                FfmpegBinary.class.getResourceAsStream(JAR_RESOURCE_PATH),
                "bundled ffmpeg.exe resource not found at " + JAR_RESOURCE_PATH)) {
            Files.copy(in, tmp);
        }

        try {
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            // Concurrent extractor may have won the race; fall back to non-atomic
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    private static Iterable<Path> candidateRoots() {
        java.util.List<Path> roots = new java.util.ArrayList<>(3);
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isEmpty()) {
            roots.add(Paths.get(localAppData, APP_FOLDER_NAME));
        }
        String userHome = System.getProperty("user.home");
        if (userHome != null && !userHome.isEmpty()) {
            roots.add(Paths.get(userHome, "AppData", "Local", APP_FOLDER_NAME));
        }
        String tmp = System.getProperty("java.io.tmpdir");
        if (tmp != null && !tmp.isEmpty()) {
            roots.add(Paths.get(tmp, APP_FOLDER_NAME));
        }
        return roots;
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
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 not available in this JVM", e);
        }
    }
}
