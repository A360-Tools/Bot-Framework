package com.automationanywhere.botcommand.actions.logs;

import com.automationanywhere.botcommand.data.Value;
import com.automationanywhere.botcommand.exception.BotCommandException;
import com.automationanywhere.botcommand.utilities.logger.CustomHTMLLayout;
import com.automationanywhere.botcommand.utilities.logger.CustomLogger;
import com.automationanywhere.botcommand.utilities.logger.DataConversion;
import com.automationanywhere.botcommand.utilities.screen.CaptureScreen;
import com.automationanywhere.commandsdk.annotations.*;
import com.automationanywhere.commandsdk.annotations.rules.EntryList.EntryListAddButtonLabel;
import com.automationanywhere.commandsdk.annotations.rules.EntryList.EntryListEmptyLabel;
import com.automationanywhere.commandsdk.annotations.rules.EntryList.EntryListEntryUnique;
import com.automationanywhere.commandsdk.annotations.rules.EntryList.EntryListLabel;
import com.automationanywhere.commandsdk.annotations.rules.NotEmpty;
import com.automationanywhere.commandsdk.annotations.rules.SelectModes;
import com.automationanywhere.commandsdk.annotations.rules.SessionObject;
import com.automationanywhere.commandsdk.model.AllowedTarget;
import com.automationanywhere.commandsdk.model.AttributeType;
import com.automationanywhere.commandsdk.model.DataType;
import org.apache.logging.log4j.Logger;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Sumit Kumar
 */

@BotCommand
@CommandPkg(label = "Log Message",
        node_label = "{{logLevel}} : {{logMessage}}",
        description = "Logs message to session log file , optionally captures a screenshot",
        icon = "log_message.svg",
        name = "log_message",
        group_label = "Logs",
        text_color = "#4285f4",
        allowed_agent_targets = AllowedTarget.HEADLESS
)
public class LogMessage {
    private static final String LEVEL_INFO = "INFO";
    private static final String LEVEL_ERROR = "ERROR";
    private static final String LEVEL_WARN = "WARN";
    private static final String LOG_VARIABLE = "YES";
    private static final String DO_NOT_LOG_VARIABLE = "NO";
    @Idx(index = "5.2.1.3", type = AttributeType.TEXT, name = "NAME")
    @Pkg(label = "Name", default_value_type = DataType.STRING)
    @NotEmpty
    private String colName;
    @Idx(index = "5.2.1.4", type = AttributeType.VARIABLE, name = "VALUE")
    @Pkg(label = "Value", default_value_type = DataType.ANY)
    private Value colValue;
    @GlobalSessionContext
    private com.automationanywhere.bot.service.GlobalSessionContext globalSessionContext;
    private String testBotUri; // For testing purposes

    public static String generateRandomScreenshotPath(String folderPath) {
        String fileName = "screenshot_" + UUID.randomUUID() + ".png";
        Path path = Paths.get(folderPath, fileName);
        return path.toAbsolutePath().toString();
    }

    public void setTestBotUri(String testBotUri) {
        this.testBotUri = testBotUri;
    }

    public String getFormattedBotUri() {
        if (this.testBotUri != null) {
            return this.testBotUri; // Return the test URI if set
        }

        // Original implementation
        String botUri = this.globalSessionContext.getBotUri();
        botUri = URLDecoder.decode(botUri, StandardCharsets.UTF_8);
        botUri = botUri.substring(botUri.indexOf("Automation Anywhere") + "Automation Anywhere".length(), botUri.indexOf(63));
        botUri = botUri.replace("/", "\\");
        return botUri;
    }

    public void setGlobalSessionContext(com.automationanywhere.bot.service.GlobalSessionContext globalSessionContext) {
        this.globalSessionContext = globalSessionContext;
    }

    @Execute
    public void action(
            @Idx(index = "1", type = AttributeType.SESSION)
            @Pkg(label = "Logger session", description = "Set valid logger session", default_value_type =
                    DataType.SESSION, default_value = "Default")
            @NotEmpty
            @SessionObject
            CustomLogger session,
            @Idx(
                    index = "2", type = AttributeType.SELECT, options = {
                    @Idx.Option(index = "2.1", pkg = @Pkg(label = "INFORMATION", value = LEVEL_INFO)),
                    @Idx.Option(index = "2.2", pkg = @Pkg(label = "WARNING", value = LEVEL_WARN)),
                    @Idx.Option(index = "2.3", pkg = @Pkg(label = "ERROR", value = LEVEL_ERROR))
            }
            )
            @Pkg(label = "Select log type", default_value = LEVEL_INFO, default_value_type = DataType.STRING)
            @NotEmpty
            @SelectModes
            String logLevel,

            @Idx(index = "3", type = AttributeType.TEXTAREA)
            @Pkg(label = "Enter message to log", default_value_type = DataType.STRING, default_value = "Sample log message")
            @NotEmpty
            String logMessage,

            @Idx(index = "4", type = AttributeType.BOOLEAN)
            @Pkg(label = "Capture Screenshot", default_value_type = DataType.BOOLEAN, default_value = "false")
            @NotEmpty
            Boolean captureScreenshot,

            @Idx(index = "5", type = AttributeType.SELECT, options = {
                    @Idx.Option(index = "5.1", pkg = @Pkg(label = "No", value = DO_NOT_LOG_VARIABLE)),
                    @Idx.Option(index = "5.2", pkg = @Pkg(label = "Yes", value = LOG_VARIABLE))})
            @Pkg(label = "Log variable values", default_value = DO_NOT_LOG_VARIABLE, default_value_type = DataType.STRING)
            @SelectModes
            String logVariable,

            @Idx(index = "5.2.1", type = AttributeType.ENTRYLIST, options = {
                    @Idx.Option(index = "5.2.1.1", pkg = @Pkg(title = "NAME", label = "Variable name/description", node_label = "{{NAME}}")),
                    @Idx.Option(index = "5.2.1.2", pkg = @Pkg(title = "VALUE", label = "Variable Value", node_label = "{{VALUE}}")),
            })
            //Label you see at the top of the control
            @Pkg(label = "Log following variables")
            //Header of the entry form
            @EntryListLabel(value = "Variable details")
            //Button label which displays the entry form
            @EntryListAddButtonLabel(value = "Add variable")
            //Uniqueness rule for the column, this value is the TITLE of the column requiring uniqueness.
            @EntryListEntryUnique(value = "NAME")
            //Message to display in table when no entries are present.
            @EntryListEmptyLabel(value = "No value to log")
            List<Value> list,

            @Idx(index = "5.2.2", type = AttributeType.VARIABLEMAP)
            @Pkg(label = "Common datatype variables to log")
            Map<String, Value> sourceMap
    ) throws Exception {
        Map<String, Value> variableValues = null;
        String screenshotPath = "";
        if (logVariable.equalsIgnoreCase(LOG_VARIABLE)) {
            if (sourceMap != null && !sourceMap.isEmpty())
                variableValues = DataConversion.getMergedDictionary(list, sourceMap);
            else
                variableValues = DataConversion.getMergedDictionary(list);
        }
        if (captureScreenshot) {
            screenshotPath = generateRandomScreenshotPath(session.getScreenshotFolderPath());
            CaptureScreen.captureDesktop(screenshotPath, true);
        }

        Map<String, Object> message = new HashMap<>();
        message.put(CustomHTMLLayout.Columns.MESSAGE, logMessage);
        message.put(CustomHTMLLayout.Columns.SCREENSHOT, screenshotPath);
        message.put(CustomHTMLLayout.Columns.VARIABLES, variableValues);
        message.put(CustomHTMLLayout.Columns.SOURCE, getFormattedBotUri());
        Logger logger = session.getLogger();
        switch (logLevel) {
            case LEVEL_INFO:
                logger.info(message);
                break;
            case LEVEL_WARN:
                logger.warn(message);
                break;
            case LEVEL_ERROR:
                logger.error(message);
                break;
            default:
                throw new BotCommandException("Invalid log level");
        }
    }

}
