package com.automationanywhere.botcommand.actions.device;

import com.automationanywhere.botcommand.data.Value;
import com.automationanywhere.botcommand.exception.BotCommandException;
import com.automationanywhere.botcommand.utilities.process.TaskKiller;
import com.automationanywhere.commandsdk.annotations.*;
import com.automationanywhere.commandsdk.annotations.rules.ListType;
import com.automationanywhere.commandsdk.annotations.rules.NotEmpty;
import com.automationanywhere.commandsdk.model.AllowedTarget;
import com.automationanywhere.commandsdk.model.AttributeType;
import com.automationanywhere.commandsdk.model.DataType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sumit Kumar
 */
@BotCommand
@CommandPkg(label = "Close Applications",
        node_label = "in list {{killTaskList}}",
        description = "Sends close and then terminate request to running applications in list",
        icon = "close.svg", name = "device_close_application",
        group_label = "Device",
        allowed_agent_targets = AllowedTarget.HEADLESS,
        documentation_url = "https://github.com/A360-Tools/Bot-Framework/blob/main/docs/device/CloseApplications.md",
        text_color = "#e04f5f"
)
public class CloseApplications {
    @Execute
    public void action(

            @Idx(index = "1", type = AttributeType.LIST)
            @Pkg(label = "List of process names to close", description = "Sample list value: excel.exe, excel, " +
                    "chrome.exe, chrome, notepad",
                    default_value_type = DataType.STRING)
            @NotEmpty
            @ListType(DataType.STRING)
            List<Value> killTaskList

    ) {
        try {
            if (killTaskList == null) {
                return;
            }
            List<String> processes = new ArrayList<>();
            for (Value value : killTaskList) {
                processes.add(value.get().toString().trim());
            }
            TaskKiller.killProcesses(processes, false);
            TaskKiller.killProcesses(processes, true);
        } catch (Exception e) {
            throw new BotCommandException("Error occurred while terminating applications: " + e.getMessage());
        }
    }
}
