package com.automationanywhere.botcommand.actions.documentation;

import com.automationanywhere.botcommand.data.Value;
import com.automationanywhere.botcommand.data.model.image.Image;
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
        label = "Steps",
        node_label = "{{sequenceTitle}}",
        description = "Runs a sequence of commands, allows documentation and screenshots",
        icon = "sequence.svg",
        name = "documentation_sequence",
        group_label = "Documentation",
        text_color = "#4285f4",
        background_color = "#4285f4",
        allowed_agent_targets = AllowedTarget.HEADLESS,
        nestable = true
)
public class Sequence {

    @Idx(index = "1", type = AttributeType.TEXT)
    @Pkg(label = "Title")
    @Inject
    @NotEmpty
    String sequenceTitle;
    @Idx(index = "2", type = AttributeType.HTML)
    @Pkg(label = "Documentation", description = "Documentation of actions being performed")
    @Inject
    Map<String, Value> sequenceDescription;
    @Idx(index = "3", type = AttributeType.CHECKBOX)
    @Pkg(label = "Capture image")
    @Inject
    Boolean sequenceCheckBox1;
    @Idx(index = "3.1", type = AttributeType.TEXTAREA)
    @Pkg(label = "Detail about captured image")
    @Inject
    Image sequenceImageText1;
    @Idx(index = "3.2", type = AttributeType.IMAGE)
    @Pkg(label = "")
    @Inject
    Image sequenceImage1;
    @Idx(index = "4", type = AttributeType.CHECKBOX)
    @Pkg(label = "Capture image")
    @Inject
    Boolean sequenceCheckBox2;
    @Idx(index = "4.1", type = AttributeType.TEXTAREA)
    @Pkg(label = "Detail about captured image")
    @Inject
    Image sequenceImageText2;
    @Idx(index = "4.2", type = AttributeType.IMAGE)
    @Pkg(label = "")
    @Inject
    Image sequenceImage2;
    @Idx(index = "5", type = AttributeType.CHECKBOX)
    @Pkg(label = "Capture image")
    @Inject
    Boolean sequenceCheckBox3;
    @Idx(index = "5.1", type = AttributeType.TEXTAREA)
    @Pkg(label = "Detail about captured image")
    @Inject
    Image sequenceImageText3;
    @Idx(index = "5.2", type = AttributeType.IMAGE)
    @Pkg(label = "")
    @Inject
    Image sequenceImage3;

    public void setSequenceCheckBox1(Boolean sequenceCheckBox1) {
        this.sequenceCheckBox1 = sequenceCheckBox1;
    }

    public void setSequenceCheckBox2(Boolean sequenceCheckBox2) {
        this.sequenceCheckBox2 = sequenceCheckBox2;
    }

    public void setSequenceCheckBox3(Boolean sequenceCheckBox3) {
        this.sequenceCheckBox3 = sequenceCheckBox3;
    }

    public void setSequenceTitle(String sequenceTitle) {
        this.sequenceTitle = sequenceTitle;
    }

    public void setSequenceDescription(Map<String, Value> sequenceDescription) {
        this.sequenceDescription = sequenceDescription;
    }

    public void setSequenceImageText1(Image sequenceImageText1) {
        this.sequenceImageText1 = sequenceImageText1;
    }

    public void setSequenceImage1(Image sequenceImage1) {
        this.sequenceImage1 = sequenceImage1;
    }

    public void setSequenceImageText2(Image sequenceImageText2) {
        this.sequenceImageText2 = sequenceImageText2;
    }

    public void setSequenceImage2(Image sequenceImage2) {
        this.sequenceImage2 = sequenceImage2;
    }

    public void setSequenceImageText3(Image sequenceImageText3) {
        this.sequenceImageText3 = sequenceImageText3;
    }

    public void setSequenceImage3(Image sequenceImage3) {
        this.sequenceImage3 = sequenceImage3;
    }

    @Execute
    public void action(


    ) {

    }
}
