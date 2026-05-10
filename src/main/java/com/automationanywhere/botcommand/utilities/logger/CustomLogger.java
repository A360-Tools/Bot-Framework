package com.automationanywhere.botcommand.utilities.logger;

import com.automationanywhere.botcommand.utilities.screen.recorder.EncodingMode;
import com.automationanywhere.botcommand.utilities.screen.recorder.ScreenRecorder;
import com.automationanywhere.toolchain.runtime.session.CloseableSessionObject;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.config.builder.impl.DefaultConfigurationBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author Sumit Kumar
 */
public class CustomLogger implements CloseableSessionObject {

    private final Logger logger;
    private final String loggerId;
    private final LoggerContext loggerContext;
    private final Map<Level, String> screenshotFolderPaths;
    private final Map<Level, String> variablesFolderPaths;
    private final ScreenRecorder recorder;

    // Constructor for a single log file for all levels (no video recording)
    public CustomLogger(String loggerName, String logFilePath, int maxLogEntries) throws IOException {
        this(loggerName, logFilePath, maxLogEntries, 0, new HashSet<>(), EncodingMode.FAST);
    }

    /**
     * Constructor for a single log file for all levels, with optional screen recording.
     *
     * @param bufferSeconds   rolling-buffer length in seconds; ignored if {@code recordingLevels} is empty
     * @param recordingLevels levels at which video clips should be saved; empty disables recording
     * @param encodingMode    encoder used when finalizing per-error clips; ignored when recording is off
     */
    public CustomLogger(String loggerName, String logFilePath, int maxLogEntries,
                        int bufferSeconds, Set<Level> recordingLevels,
                        EncodingMode encodingMode) throws IOException {
        this.loggerId = UUID.randomUUID().toString();

        // Create screenshot folder at the same location as log file
        String baseDir = FilenameUtils.getFullPath(logFilePath);
        String screenshotFolder = baseDir + "screenshots";
        String variablesFolder = baseDir + "variables";

        this.screenshotFolderPaths = new HashMap<>();
        this.variablesFolderPaths = new HashMap<>();

        // Use the same folder for all levels when using a combined log file
        this.screenshotFolderPaths.put(Level.INFO, screenshotFolder);
        this.screenshotFolderPaths.put(Level.WARN, screenshotFolder);
        this.screenshotFolderPaths.put(Level.ERROR, screenshotFolder);

        this.variablesFolderPaths.put(Level.INFO, variablesFolder);
        this.variablesFolderPaths.put(Level.WARN, variablesFolder);
        this.variablesFolderPaths.put(Level.ERROR, variablesFolder);

        // Create directories
        createDirectories();

        // Create a unique logger context for this instance
        LoggerContext context = createNewLoggerContext();

        // Setup logger configuration
        ConfigurationBuilder<BuiltConfiguration> builder = new DefaultConfigurationBuilder();
        setupLoggerConfiguration(builder);

        AppenderComponentBuilder appenderBuilder = getCustomAppenderBuilder(builder, "COMBINED_" + loggerId,
                logFilePath,
                maxLogEntries);
        builder.add(appenderBuilder);
        builder.add(builder.newLogger(loggerName, Level.INFO)
                .add(builder.newAppenderRef("COMBINED_" + loggerId)));

        builder.add(builder.newRootLogger(Level.INFO));

        // Initialize the context with the configuration
        BuiltConfiguration config = builder.build();
        context.start(config);
        this.loggerContext = context;

        // Get logger from the new context
        this.logger = context.getLogger(loggerName);

        // Optionally start the screen recorder. Returns DISABLED on any failure.
        Path logDir = Paths.get(FilenameUtils.getFullPath(logFilePath));
        this.recorder = ScreenRecorder.start(loggerId, logDir, bufferSeconds, recordingLevels,
                encodingMode);
    }

    private void createDirectories() throws IOException {
        // Create screenshot directories
        for (String path : screenshotFolderPaths.values()) {
            Path directoryPath = Paths.get(path);
            Files.createDirectories(directoryPath);
        }

        // Create variables directories
        for (String path : variablesFolderPaths.values()) {
            Path directoryPath = Paths.get(path);
            Files.createDirectories(directoryPath);
        }
    }

    private LoggerContext createNewLoggerContext() {
        // Creating a completely separate LoggerContext
        return new LoggerContext("Context-" + loggerId);
    }

    private void setupLoggerConfiguration(ConfigurationBuilder<BuiltConfiguration> builder) {
        builder.setConfigurationName("CustomLogger-" + loggerId);
        builder.setPackages("com.automationanywhere.botcommand.utilities.logger");
        builder.setMonitorInterval("30");
        builder.setStatusLevel(Level.ERROR);
    }

    private AppenderComponentBuilder getCustomAppenderBuilder(ConfigurationBuilder<BuiltConfiguration> builder,
                                                              String appenderName,
                                                              String filePath, int maxLogEntries) {
        LayoutComponentBuilder layoutBuilder = builder.newLayout("CustomHTMLLayout")
                .addAttribute("charset", "UTF-8");

        // Simple sequential numbering: 1_log.html, 2_log.html, etc.
        // Active file remains as log.html
        String baseName = FilenameUtils.getBaseName(filePath);
        String baseDir = FilenameUtils.getFullPath(filePath);
        String ext = FilenameUtils.getExtension(filePath);

        // Pattern: %i_basename.extension (e.g., 1_log.html, 2_log.html)
        String filePattern = baseDir + "%i_" + baseName + "." + ext;

        return builder.newAppender(appenderName, "RollingFile")
                .addAttribute("fileName", filePath)
                .addAttribute("filePattern", filePattern)
                .addAttribute("immediateFlush", true)
                .addAttribute("append", true)
                .addComponent(layoutBuilder)
                .addComponent(builder.newComponent("Policies")
                        .addComponent(builder.newComponent("EntryCountBasedTriggeringPolicy")
                                .addAttribute("maxEntries", maxLogEntries)))
                .addComponent(builder.newComponent("DefaultRolloverStrategy")
                    .addAttribute("fileIndex", "nomax")
            );
    }

    // Constructor for multiple log files based on the level (no video recording)
    public CustomLogger(String loggerName, Map<Level, String> levelFilePathMap, int maxLogEntries) throws IOException {
        this(loggerName, levelFilePathMap, maxLogEntries, 0, new HashSet<>(), EncodingMode.FAST);
    }

    /**
     * Constructor for multiple log files based on level, with optional screen recording.
     *
     * @param bufferSeconds   rolling-buffer length in seconds; ignored if {@code recordingLevels} is empty
     * @param recordingLevels levels at which video clips should be saved; empty disables recording
     * @param encodingMode    encoder used when finalizing per-error clips; ignored when recording is off
     */
    public CustomLogger(String loggerName, Map<Level, String> levelFilePathMap, int maxLogEntries,
                        int bufferSeconds, Set<Level> recordingLevels,
                        EncodingMode encodingMode) throws IOException {
        this.loggerId = UUID.randomUUID().toString();
        this.screenshotFolderPaths = new HashMap<>();
        this.variablesFolderPaths = new HashMap<>();

        // Create a screenshot and variables folder alongside each log file
        for (Map.Entry<Level, String> entry : levelFilePathMap.entrySet()) {
            Level level = entry.getKey();
            String filePath = entry.getValue();
            String baseDir = FilenameUtils.getFullPath(filePath);

            this.screenshotFolderPaths.put(level, baseDir + "screenshots");
            this.variablesFolderPaths.put(level, baseDir + "variables");
        }

        // Create all directories
        createDirectories();

        // Create a unique logger context for this instance
        LoggerContext context = createNewLoggerContext();

        ConfigurationBuilder<BuiltConfiguration> builder = new DefaultConfigurationBuilder();
        setupLoggerConfiguration(builder);

        // Creating appender based on provided level-file path map
        for (Map.Entry<Level, String> entry : levelFilePathMap.entrySet()) {
            Level level = entry.getKey();
            String filePath = entry.getValue();
            String appenderName = level.name() + "_" + loggerId;

            AppenderComponentBuilder appenderBuilder = getCustomAppenderBuilder(builder, appenderName, filePath,
                    maxLogEntries);
            // Only accept logs for this specific level
            appenderBuilder.add(builder.newFilter("LevelMatchFilter", "ACCEPT", "DENY")
                    .addAttribute("level", level));

            builder.add(appenderBuilder);
        }

        // Configure the logger
        builder.add(builder.newLogger(loggerName, Level.INFO)
                .add(builder.newAppenderRef(Level.INFO.name() + "_" + loggerId))
                .add(builder.newAppenderRef(Level.WARN.name() + "_" + loggerId))
                .add(builder.newAppenderRef(Level.ERROR.name() + "_" + loggerId))
        );

        builder.add(builder.newRootLogger(Level.INFO));

        // Initialize the context with the configuration
        BuiltConfiguration config = builder.build();
        context.start(config);
        this.loggerContext = context;

        // Get logger from the new context
        this.logger = context.getLogger(loggerName);

        // Optionally start the screen recorder. Recording artifacts (clips/, screenshots/posters)
        // live next to the INFO log file because the HTML log uses relative paths.
        Path recorderLogDir = Paths.get(FilenameUtils.getFullPath(
                levelFilePathMap.getOrDefault(Level.INFO, levelFilePathMap.values().iterator().next())));
        this.recorder = ScreenRecorder.start(loggerId, recorderLogDir, bufferSeconds, recordingLevels,
                encodingMode);
    }

    public Logger getLogger() {
        return logger;
    }

    /** True when the supplied level is in the configured recording-levels set and recorder is healthy. */
    public boolean shouldRecordVideoFor(Level level) {
        return recorder.shouldRecordFor(level);
    }

    /**
     * Snapshots the rolling buffer and queues an async stage-2 encode. Returns the
     * eventual mp4 path (which may not exist until the encode finishes), or null
     * if the encode could not be queued (recorder failed, queue full, ring empty).
     */
    public java.nio.file.Path snapshotForError(String errorUuid) {
        return recorder.snapshotForError(errorUuid);
    }


    /** UUID identifying this session; useful for tests and salvage path naming. */
    public String getLoggerId() {
        return loggerId;
    }

    @Override
    public void close() {
        if (!isClosed()) {
            // Stop the screen recorder first (if any). Order matters: stage-1 stops
            // writing to the ring before pending stage-2 encodes drain, and the
            // session folder is deleted only after both are quiescent.
            recorder.close();

            // Shutdown this specific logger context
            loggerContext.stop();
        }
    }

    @Override
    public boolean isClosed() {
        return loggerContext.isStopped();
    }

    public String getScreenshotFolderPath(Level level) {
        return screenshotFolderPaths.getOrDefault(level, screenshotFolderPaths.get(Level.INFO));
    }

    /**
     * Returns the variables folder path for the specified log level
     *
     * @param level Log level
     *
     * @return variables folder path for the specified level
     */
    public String getVariablesFolderPath(Level level) {
        return variablesFolderPaths.getOrDefault(level, variablesFolderPaths.get(Level.INFO));
    }
}