package com.automationanywhere.botcommand.utilities.screen.recorder;

import org.apache.logging.log4j.Level;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Sumit Kumar
 */
public final class SessionMeta {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    public final int schemaVersion;
    public final String sessionId;
    public final String logDir;
    public final int bufferSeconds;
    public final Set<Level> recordingLevels;
    public final Instant startedAt;
    public final String packageVersion;
    public final long jvmPid;

    public SessionMeta(String sessionId, String logDir, int bufferSeconds,
                       Set<Level> recordingLevels, Instant startedAt,
                       String packageVersion, long jvmPid) {
        this.schemaVersion = CURRENT_SCHEMA_VERSION;
        this.sessionId = sessionId;
        this.logDir = logDir;
        this.bufferSeconds = bufferSeconds;
        this.recordingLevels = new HashSet<>(recordingLevels);
        this.startedAt = startedAt;
        this.packageVersion = packageVersion;
        this.jvmPid = jvmPid;
    }

    private SessionMeta(JSONObject json) {
        this.schemaVersion = json.getInt("schemaVersion");
        this.sessionId = json.getString("sessionId");
        this.logDir = json.getString("logDir");
        this.bufferSeconds = json.getInt("bufferSeconds");
        this.recordingLevels = parseLevels(json.getJSONArray("recordingLevels"));
        this.startedAt = Instant.parse(json.getString("startedAt"));
        this.packageVersion = json.optString("packageVersion", "");
        this.jvmPid = json.optLong("jvmPid", -1L);
    }

    public static SessionMeta read(Path file) throws IOException {
        String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        return new SessionMeta(new JSONObject(text));
    }

    public void write(Path file) throws IOException {
        JSONObject json = new JSONObject();
        json.put("schemaVersion", schemaVersion);
        json.put("sessionId", sessionId);
        json.put("logDir", logDir);
        json.put("bufferSeconds", bufferSeconds);
        json.put("recordingLevels", new JSONArray(levelsToStrings()));
        json.put("startedAt", startedAt.toString());
        json.put("packageVersion", packageVersion);
        json.put("jvmPid", jvmPid);

        Files.createDirectories(file.getParent());
        Files.write(file, json.toString(2).getBytes(StandardCharsets.UTF_8));
    }

    private String[] levelsToStrings() {
        return recordingLevels.stream().map(Level::toString).toArray(String[]::new);
    }

    private static Set<Level> parseLevels(JSONArray array) {
        Set<Level> result = new HashSet<>();
        for (int i = 0; i < array.length(); i++) {
            Level level = Level.toLevel(array.getString(i), null);
            if (level != null) {
                result.add(level);
            }
        }
        return result;
    }
}
