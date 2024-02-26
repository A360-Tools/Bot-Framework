package com.automationanywhere.botcommand.actions.logs;

import com.automationanywhere.botcommand.data.impl.SessionValue;
import com.automationanywhere.botcommand.exception.BotCommandException;
import com.automationanywhere.botcommand.utilities.logger.CustomLogger;
import com.automationanywhere.commandsdk.annotations.*;
import com.automationanywhere.commandsdk.annotations.rules.*;
import com.automationanywhere.commandsdk.model.AllowedTarget;
import com.automationanywhere.commandsdk.model.AttributeType;
import com.automationanywhere.commandsdk.model.DataType;
import com.automationanywhere.commandsdk.model.ReturnSettingsType;
import org.apache.logging.log4j.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.automationanywhere.commandsdk.model.AttributeType.FILE;


/**
 * @author Sumit Kumar
 */

@BotCommand
@CommandPkg(label = "Start Logger Session",
        name = "logs_start_session",
        description = "Create new logger session",
        comment = true,
        group_label = "Logs",
        text_color = "#2F4F4F",
        background_color = "#2F4F4F",
        icon = "log_session.svg", node_label = "{{returnTo}}",
        return_settings = {ReturnSettingsType.SESSION_TARGET},
        return_type = DataType.SESSION,
        return_name = "Logger",
        documentation_url = "https://github.com/A360-Tools/Bot-Framework/blob/main/docs/logs/StartLoggerSession.md",
        allowed_agent_targets = AllowedTarget.HEADLESS,
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

            @Idx(index = "2", type = FILE)
            @Pkg(label = "Screenshot folder path")
            @FileFolder
            @NotEmpty
            String screenshotFolderPath,

            @Idx(index = "3", type = AttributeType.NUMBER)
            @Pkg(label = "Rollover file size in MB", default_value_type = DataType.NUMBER, default_value = "10")
            @GreaterThan("0")
            Number rollingFileSizeMB

    ) {
        try {
            if (screenshotFolderPath == null || screenshotFolderPath.isEmpty()) {
                throw new BotCommandException("Invalid screenshot folder path");
            }

            CustomLogger customLogger;
            switch (logLevelsAndFileOption) {
                case COMMON_FILE_ALL_LEVEL:
                    customLogger = new CustomLogger("CustomLogger_" + UUID.randomUUID(), logFilePath,
                            rollingFileSizeMB.longValue(), screenshotFolderPath);
                    break;
                case CONFIGURABLE_FILE_ALL_LEVEL:
                    Map<Level, String> levelFilePathMap = new HashMap<>();
                    levelFilePathMap.put(Level.INFO, infoLogFilePath);
                    levelFilePathMap.put(Level.WARN, warnLogFilePath);
                    levelFilePathMap.put(Level.ERROR, errorLogFilePath);
                    customLogger = new CustomLogger("CustomLogger_" + UUID.randomUUID(), levelFilePathMap,
                            rollingFileSizeMB.longValue(), screenshotFolderPath);
                    break;
                default:
                    throw new BotCommandException("Invalid log level and file option");
            }

            return SessionValue
                    .builder()
                    .withSessionObject(customLogger)
                    .build();
        } catch (Exception e) {
            throw new BotCommandException("Error occurred while creating new session: " + e.getMessage());
        }
    }
}
