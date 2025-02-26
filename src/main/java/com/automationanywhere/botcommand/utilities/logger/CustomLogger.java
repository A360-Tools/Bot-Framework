package com.automationanywhere.botcommand.utilities.logger;

import com.automationanywhere.toolchain.runtime.session.CloseableSessionObject;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Sumit Kumar
 */
public class CustomLogger implements CloseableSessionObject {

    // Static map to track all logger contexts by unique ID
    private static final ConcurrentHashMap<String, LoggerContext> LOGGER_CONTEXTS = new ConcurrentHashMap<>();

    private final Logger logger;
    private final String loggerId;
    private final LoggerContext loggerContext;
    private final Map<Level, String> screenshotFolderPaths;

    // Constructor for a single log file for all levels
    public CustomLogger(String loggerName, String logFilePath, long sizeLimitMB) throws IOException {
        this.loggerId = UUID.randomUUID().toString();

        // Create screenshot folder at the same location as log file
        String baseDir = FilenameUtils.getFullPath(logFilePath);
        String screenshotFolder = baseDir + "screenshots";

        this.screenshotFolderPaths = new HashMap<>();
        // Use the same screenshot folder for all levels when using a combined log file
        this.screenshotFolderPaths.put(Level.INFO, screenshotFolder);
        this.screenshotFolderPaths.put(Level.WARN, screenshotFolder);
        this.screenshotFolderPaths.put(Level.ERROR, screenshotFolder);

        // Create screenshot directory
        createScreenshotDirectories();

        // Create a unique logger context for this instance
        LoggerContext context = createNewLoggerContext();

        // Setup logger configuration
        ConfigurationBuilder<BuiltConfiguration> builder = new DefaultConfigurationBuilder();
        setupLoggerConfiguration(builder);

        AppenderComponentBuilder appenderBuilder = getCustomAppenderBuilder(builder, "COMBINED_" + loggerId, logFilePath,
                sizeLimitMB);
        builder.add(appenderBuilder);
        builder.add(builder.newLogger(loggerName, Level.INFO)
                .add(builder.newAppenderRef("COMBINED_" + loggerId)));

        builder.add(builder.newRootLogger(Level.INFO));

        // Initialize the context with the configuration
        BuiltConfiguration config = builder.build();
        context.start(config);

        // Store the context
        LOGGER_CONTEXTS.put(loggerId, context);
        this.loggerContext = context;

        // Get logger from the new context
        this.logger = context.getLogger(loggerName);
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
                                                              String filePath, long sizeLimitMB) {
        LayoutComponentBuilder layoutBuilder = builder.newLayout("CustomHTMLLayout")
                .addAttribute("charset", "UTF-8");

        String filePattern =
                FilenameUtils.getFullPath(filePath) + FilenameUtils.getBaseName(filePath) + "_%i." +
                        FilenameUtils.getExtension(filePath);

        return builder.newAppender(appenderName, "RollingFile")
                .addAttribute("fileName", filePath)
                .addAttribute("filePattern", filePattern)
                .addAttribute("immediateFlush", true)
                .addAttribute("append", true)
                .addComponent(layoutBuilder)
                .addComponent(builder.newComponent("Policies")
                        .addComponent(builder.newComponent("SizeBasedTriggeringPolicy").addAttribute("size",
                                sizeLimitMB + "MB")))
                .addComponent(builder.newComponent("DefaultRolloverStrategy").addAttribute("fileIndex", "nomax"));
    }

    // Constructor for multiple log files based on the level
    public CustomLogger(String loggerName, Map<Level, String> levelFilePathMap, long sizeLimitMB) throws IOException {
        this.loggerId = UUID.randomUUID().toString();
        this.screenshotFolderPaths = new HashMap<>();

        // Create a screenshot folder alongside each log file
        for (Map.Entry<Level, String> entry : levelFilePathMap.entrySet()) {
            Level level = entry.getKey();
            String filePath = entry.getValue();
            String baseDir = FilenameUtils.getFullPath(filePath);
            this.screenshotFolderPaths.put(level, baseDir + "screenshots");
        }

        // Create all screenshot directories
        createScreenshotDirectories();

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
                    sizeLimitMB);
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

        // Store the context
        LOGGER_CONTEXTS.put(loggerId, context);
        this.loggerContext = context;

        // Get logger from the new context
        this.logger = context.getLogger(loggerName);
    }

    private void createScreenshotDirectories() throws IOException {
        // Create unique directories only (remove duplicates)
        for (String path : screenshotFolderPaths.values()) {
            Path directoryPath = Paths.get(path);
            Files.createDirectories(directoryPath);
        }
    }

    public Logger getLogger() {
        return logger;
    }

    @Override
    public void close() {
        if (!isClosed()) {
            // Shutdown this specific logger context
            loggerContext.stop();

            // Remove from the map of contexts
            LOGGER_CONTEXTS.remove(loggerId);
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
     * Returns the screenshot folder path for the INFO level
     * @return default screenshot folder path (INFO level)
     */
    public String getScreenshotFolderPath() {
        return screenshotFolderPaths.get(Level.INFO);
    }
}