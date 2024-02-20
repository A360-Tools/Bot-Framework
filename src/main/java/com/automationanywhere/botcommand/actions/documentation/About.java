package com.automationanywhere.botcommand.actions.documentation;

import com.automationanywhere.botcommand.data.Value;
import com.automationanywhere.commandsdk.annotations.*;
import com.automationanywhere.commandsdk.annotations.rules.NotEmpty;
import com.automationanywhere.commandsdk.model.AllowedTarget;
import com.automationanywhere.commandsdk.model.AttributeType;

import java.util.Map;


/**
 * @author Sumit Kumar
 */

@BotCommand
@CommandPkg(
        label = "About",
        node_label = "Task Description",
        description = "Document task functionality, input, output, expected " +
                "application version and application setting to run this task." +
                "Note: Abouts are ignored when the bot runs",
        icon = "about.svg",
        name = "documentation_about",
        group_label = "Documentation",
        documentation_url = "https://github.com/A360-Tools/Bot-Framework/blob/main/docs/documentation/About.md",
        text_color = "#2196f3",

        allowed_agent_targets = AllowedTarget.HEADLESS,
        comment = true

)
public class About {
    @Idx(index = "1", type = AttributeType.HTML)
    @Pkg(label = "Documentation")
    @Inject
    @NotEmpty
    Map<String, Value> aboutDescription;


    public void setAboutDescription(Map<String, Value> aboutDescription) {
        this.aboutDescription = aboutDescription;
    }

    @Execute
    public void action(

    ) {

    }
}
