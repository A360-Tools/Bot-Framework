package com.automationanywhere.botcommand.actions.logs;

import com.automationanywhere.botcommand.data.impl.SessionValue;
import com.automationanywhere.botcommand.exception.BotCommandException;
import com.automationanywhere.botcommand.utilities.logger.CustomLogger;
import com.automationanywhere.commandsdk.annotations.*;
import com.automationanywhere.commandsdk.annotations.rules.*;
import com.automationanywhere.commandsdk.model.AttributeType;
import com.automationanywhere.commandsdk.model.DataType;
import com.automationanywhere.commandsdk.model.ReturnSettingsType;
import org.apache.logging.log4j.Level;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.automationanywhere.commandsdk.model.AttributeType.FILE;


/**
 * @author Sumit Kumar
 */

@BotCommand
@CommandPkg(label = "Start Logger Session",
        name = "logs_start_session",
        description = "Create new logger session",
        group_label = "Logs",
        icon = "log_session.svg", node_label = "{{returnTo}}",
        return_settings = {ReturnSettingsType.SESSION_TARGET},
        return_type = DataType.SESSION,
        return_name = "Logger",
        documentation_url = "https://github.com/A360-Tools/Bot-Framework/blob/main/docs/logs/StartLoggerSession.md",
        return_required = true)
public class StartLoggerSession {
    private static final String COMMON_FILE_ALL_LEVEL = "COMMON_FILE";
    private static final String CONFIGURABLE_FILE_ALL_LEVEL = "CONFIGURABLE_FILE";


    @Execute
    public SessionValue start(

            @Idx(index = "1", type = AttributeType.SELECT, options = {
                    @Idx.Option(index = "1.1", pkg = @Pkg(label = "Same File", value = COMMON_FILE_ALL_LEVEL)),
                    @Idx.Option(index = "1.2", pkg = @Pkg(label = "Custom Configuration",
                            value = CONFIGURABLE_FILE_ALL_LEVEL))})
            @Pkg(label = "Append option for different levels of log", default_value = COMMON_FILE_ALL_LEVEL,
                    default_value_type = DataType.STRING)
            @SelectModes
            @NotEmpty
            String logLevelsAndFileOption,

            @Idx(index = "1.1.1", type = FILE)
            @Pkg(label = "Log file path")
            @NotEmpty
            @LocalFile
            @FileExtension("html")
            String logFilePath,

            @Idx(index = "1.2.1", type = FILE)
            @Pkg(label = "INFO logs file path")
            @NotEmpty
            @LocalFile
            @FileExtension("html")
            String infoLogFilePath,

            @Idx(index = "1.2.2", type = FILE)
            @Pkg(label = "WARN logs file path")
            @NotEmpty
            @LocalFile
            @FileExtension("html")
            String warnLogFilePath,

            @Idx(index = "1.2.3", type = FILE)
            @Pkg(label = "ERROR logs file path")
            @NotEmpty
            @LocalFile
            @FileExtension("html")
            String errorLogFilePath,

            @Idx(index = "2", type = AttributeType.NUMBER)
            @Pkg(label = "Maximum log entries per file (default 1000)", default_value_type = DataType.NUMBER, default_value = "1000")
            @GreaterThan("0")
            Number maxLogEntries,

            @Idx(index = "3", type = AttributeType.BOOLEAN)
            @Pkg(label = "Capture screen recording on log entries",
                    description = "When true, records the desktop continuously into a rolling buffer; "
                            + "on selected log levels, the last N seconds are saved as an MP4 next to the log.",
                    default_value = "false", default_value_type = DataType.BOOLEAN)
            @NotEmpty
            Boolean captureScreenRecording,

            @Idx(index = "4", type = AttributeType.NUMBER)
            @Pkg(label = "Buffer seconds (only used when capture is on)",
                    default_value = "30", default_value_type = DataType.NUMBER)
            @NumberInteger
            @GreaterThanEqualTo("5")
            @LessThanEqualTo("300")
            Number videoBufferSeconds,

            @Idx(index = "5", type = AttributeType.BOOLEAN)
            @Pkg(label = "Record video on INFO entries",
                    default_value = "false", default_value_type = DataType.BOOLEAN)
            Boolean videoOnInfo,

            @Idx(index = "6", type = AttributeType.BOOLEAN)
            @Pkg(label = "Record video on WARN entries",
                    default_value = "false", default_value_type = DataType.BOOLEAN)
            Boolean videoOnWarn,

            @Idx(index = "7", type = AttributeType.BOOLEAN)
            @Pkg(label = "Record video on ERROR entries",
                    default_value = "true", default_value_type = DataType.BOOLEAN)
            Boolean videoOnError

    ) {
        try {
            boolean videoEnabled = Boolean.TRUE.equals(captureScreenRecording);
            Set<Level> recordingLevels = new HashSet<>();
            if (videoEnabled) {
                if (Boolean.TRUE.equals(videoOnInfo))  recordingLevels.add(Level.INFO);
                if (Boolean.TRUE.equals(videoOnWarn))  recordingLevels.add(Level.WARN);
                if (Boolean.TRUE.equals(videoOnError)) recordingLevels.add(Level.ERROR);
            }
            int bufferSec = videoEnabled && videoBufferSeconds != null
                    ? videoBufferSeconds.intValue() : 0;

            CustomLogger customLogger;
            switch (logLevelsAndFileOption) {
                case COMMON_FILE_ALL_LEVEL:
                    customLogger = new CustomLogger("CustomLogger_" + UUID.randomUUID(), logFilePath,
                            maxLogEntries.intValue(), bufferSec, recordingLevels);
                    break;
                case CONFIGURABLE_FILE_ALL_LEVEL:
                    Map<Level, String> levelFilePathMap = new HashMap<>();
                    levelFilePathMap.put(Level.INFO, infoLogFilePath);
                    levelFilePathMap.put(Level.WARN, warnLogFilePath);
                    levelFilePathMap.put(Level.ERROR, errorLogFilePath);
                    customLogger = new CustomLogger("CustomLogger_" + UUID.randomUUID(), levelFilePathMap,
                            maxLogEntries.intValue(), bufferSec, recordingLevels);
                    break;
                default:
                    throw new BotCommandException("Invalid log level and file option");
            }

            return SessionValue
                    .builder()
                    .withSessionObject(customLogger)
                    .build();
        } catch (Exception e) {
            throw new BotCommandException("Error occurred while creating new session: " + e.getMessage(), e);
        }
    }
}