package com.automationanywhere.botcommand.actions.documentation;

import com.automationanywhere.botcommand.data.model.image.Image;
import com.automationanywhere.commandsdk.annotations.*;
import com.automationanywhere.commandsdk.annotations.rules.NotEmpty;
import com.automationanywhere.commandsdk.model.AllowedTarget;
import com.automationanywhere.commandsdk.model.AttributeType;


/**
 * @author Sumit Kumar
 */

@BotCommand
@CommandPkg(
        label = "Comment",
        node_label = "{{commentDescription}}",
        description = "Inserts a comment, allows helper image. Note: Comments are ignored when the bot runs",
        icon = "comment.svg",
        name = "documentation_comment",
        group_label = "Documentation",
        text_color = "#608d56",
        allowed_agent_targets = AllowedTarget.HEADLESS,
        comment = true

)
public class Comment {
    @Idx(index = "1", type = AttributeType.TEXTAREA)
    @Pkg(label = "Comment Text")
    @Inject
    @NotEmpty
    String commentDescription;

    @Idx(index = "2", type = AttributeType.IMAGE)
    @Pkg(label = "Help Image")
    @Inject
    Image commentImage;

    public void setCommentDescription(String commentDescription) {
        this.commentDescription = commentDescription;
    }

    public void setCommentImage(Image commentImage) {
        this.commentImage = commentImage;
    }

    @Execute
    public void action(

    ) {

    }
}
