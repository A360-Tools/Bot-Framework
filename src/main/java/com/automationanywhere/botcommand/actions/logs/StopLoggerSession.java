package com.automationanywhere.botcommand.actions.logs;

import com.automationanywhere.botcommand.exception.BotCommandException;
import com.automationanywhere.botcommand.utilities.logger.CustomLogger;
import com.automationanywhere.commandsdk.annotations.*;
import com.automationanywhere.commandsdk.annotations.rules.NotEmpty;
import com.automationanywhere.commandsdk.annotations.rules.SessionObject;
import com.automationanywhere.commandsdk.model.AllowedTarget;
import com.automationanywhere.commandsdk.model.AttributeType;
import com.automationanywhere.commandsdk.model.DataType;

/**
 * @author Sumit Kumar
 */
@BotCommand
@CommandPkg(label = "Stop Logger Session",
        name = "logs_stop_session",
        group_label = "Logs",
        description = "Stops a logger session",
        text_color = "#2F4F4F",
        icon = "log_session.svg",
        node_label = "{{session}}",
        documentation_url = "https://github.com/A360-Tools/Bot-Framework/blob/main/docs/logs/StopLoggerSession.md",
        allowed_agent_targets = AllowedTarget.HEADLESS)
public class StopLoggerSession {

    @Execute
    public void stop(
            @Idx(index = "1", type = AttributeType.SESSION)
            @Pkg(label = "Logger Session", description = "Logger session to stop logging",
                    default_value_type = DataType.SESSION, default_value = "Default")
            @NotEmpty
            @SessionObject
            CustomLogger session) {
        if (session.isClosed()) {
            throw new BotCommandException("Logger session not found");
        } else {
            session.close();
        }
    }


}