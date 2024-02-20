package com.automationanywhere.botcommand.utilities.logger;

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
import java.util.Map;

/**
 * @author Sumit Kumar
 */
public class CustomLogger implements CloseableSessionObject {

    private final Logger logger;
    private final LoggerContext loggerContext;
    private final String screenshotFolderPath;

    // Constructor for a single log file for all levels
    public CustomLogger(String loggerName, String logFilePath, long sizeLimitMB, String screenshotFolderPath) throws IOException {
        this.screenshotFolderPath = screenshotFolderPath;
        Path directoryPath = Paths.get(screenshotFolderPath);
        Files.createDirectories(directoryPath);
        ConfigurationBuilder<BuiltConfiguration> builder = new DefaultConfigurationBuilder<>();
        setupLoggerConfiguration(builder);

        AppenderComponentBuilder appenderBuilder = getCustomAppenderBuilder(builder, "COMBINED", logFilePath,
                sizeLimitMB);
        builder.add(appenderBuilder);
        builder.add(builder.newLogger(loggerName, Level.INFO)
                .add(builder.newAppenderRef("COMBINED")));

        this.loggerContext = initializeLoggerContext(builder);
        this.logger = loggerContext.getLogger(loggerName);
    }

    // Constructor for multiple log files based on the level
    public CustomLogger(String loggerName, Map<Level, String> levelFilePathMap, long sizeLimitMB, String screenshotFolderPath) throws IOException {
        this.screenshotFolderPath = screenshotFolderPath;
        Path directoryPath = Paths.get(screenshotFolderPath);
        Files.createDirectories(directoryPath);
        ConfigurationBuilder<BuiltConfiguration> builder = new DefaultConfigurationBuilder<>();
        setupLoggerConfiguration(builder);

        // Creating appender based on provided level-file path map
        for (Map.Entry<Level, String> entry : levelFilePathMap.entrySet()) {
            Level level = entry.getKey();
            String filePath = entry.getValue();
            AppenderComponentBuilder appenderBuilder = getCustomAppenderBuilder(builder, level.name(), filePath,
                    sizeLimitMB);
            appenderBuilder.add(builder.newFilter("LevelMatchFilter", "ACCEPT", "DENY").addAttribute("level", level));
            builder.add(appenderBuilder);
        }
        builder.add(builder.newLogger(loggerName, Level.INFO)
                .add(builder.newAppenderRef(Level.INFO.name()))
                .add(builder.newAppenderRef(Level.WARN.name()))
                .add(builder.newAppenderRef(Level.ERROR.name()))
        );
        this.loggerContext = initializeLoggerContext(builder);
        this.logger = loggerContext.getLogger(loggerName);
    }

    private void setupLoggerConfiguration(ConfigurationBuilder<BuiltConfiguration> builder) {
        builder.setConfigurationName("CustomLogger");
        builder.setPackages("com.automationanywhere.botcommand.utilities.logger");
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
                .addComponent(layoutBuilder)
                .addComponent(builder.newComponent("Policies")
                        .addComponent(builder.newComponent("SizeBasedTriggeringPolicy").addAttribute("size", sizeLimitMB + "MB")))
                .addComponent(builder.newComponent("DefaultRolloverStrategy").addAttribute("fileIndex", "nomax"));
    }

    private LoggerContext initializeLoggerContext(ConfigurationBuilder<BuiltConfiguration> builder) {
        LoggerContext context = LoggerContext.getContext(false);
        context.start(builder.build());
        return context;
    }

    public Logger getLogger() {
        return logger;
    }

    @Override
    public boolean isClosed() {
        return loggerContext.isStopped();
    }

    @Override
    public void close() {
        if (!isClosed()) {
            loggerContext.stop();
        }
    }

    public String getScreenshotFolderPath() {
        return screenshotFolderPath;
    }
}