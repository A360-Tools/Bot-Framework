package com.automationanywhere.botcommand.actions.logs;

import com.automationanywhere.botcommand.utilities.logger.CustomLogger;
import com.automationanywhere.commandsdk.annotations.*;
import com.automationanywhere.commandsdk.annotations.rules.NotEmpty;
import com.automationanywhere.commandsdk.annotations.rules.SessionObject;
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
        icon = "log_session.svg",
        node_label = "{{session}}",
        documentation_url = "https://github.com/A360-Tools/Bot-Framework/blob/main/docs/logs/StopLoggerSession.md"
)
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
            // Idempotent: this action is documented to be called from a Finally
            // block, which means the happy-path Stop and the Finally Stop both
            // run. Throwing here would mask whatever exception was already
            // unwinding the bot.
            return;
        }
        session.close();
    }


}