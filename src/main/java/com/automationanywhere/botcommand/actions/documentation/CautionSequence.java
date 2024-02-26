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
        label = "Caution Steps",
        node_label = "{{cautionSequenceTitle}}",
        description = "Highlights caution and runs sequence of commands, allows documentation and screenshots",
        icon = "caution.svg",
        name = "documentation_caution_sequence",
        group_label = "Documentation",
        documentation_url = "https://github.com/A360-Tools/Bot-Framework/blob/main/docs/documentation/CautionSequence" +
                ".md",
        text_color = "#ffa500",
        allowed_agent_targets = AllowedTarget.HEADLESS,
        nestable = true
)
public class CautionSequence {

    @Idx(index = "1", type = AttributeType.TEXT)
    @Pkg(label = "Title")
    @Inject
    @NotEmpty
    String cautionSequenceTitle;
    @Idx(index = "2", type = AttributeType.HTML)
    @Pkg(label = "Documentation", description = "Documentation of actions being performed which needs caution")
    @Inject
    Map<String, Value> cautionSequenceDescription;
    @Idx(index = "3", type = AttributeType.CHECKBOX)
    @Pkg(label = "Capture image")
    @Inject
    Boolean cautionSequenceCheckBox1;
    @Idx(index = "3.1", type = AttributeType.TEXTAREA)
    @Pkg(label = "Detail about captured image")
    @Inject
    Image cautionSequenceText1;
    @Idx(index = "3.2", type = AttributeType.IMAGE)
    @Pkg(label = "")
    @Inject
    Image cautionSequenceImage1;
    @Idx(index = "4", type = AttributeType.CHECKBOX)
    @Pkg(label = "Capture image")
    @Inject
    Boolean cautionSequenceCheckBox2;
    @Idx(index = "4.1", type = AttributeType.TEXTAREA)
    @Pkg(label = "Detail about captured image")
    @Inject
    Image cautionSequenceText2;
    @Idx(index = "4.2", type = AttributeType.IMAGE)
    @Pkg(label = "")
    @Inject
    Image cautionSequenceImage2;
    @Idx(index = "5", type = AttributeType.CHECKBOX)
    @Pkg(label = "Captured image")
    @Inject
    Boolean cautionSequenceCheckBox3;
    @Idx(index = "5.1", type = AttributeType.TEXTAREA)
    @Pkg(label = "Detail about captured image")
    @Inject
    Image cautionSequenceText3;
    @Idx(index = "5.2", type = AttributeType.IMAGE)
    @Pkg(label = "")
    @Inject
    Image cautionSequenceImage3;

    public void setCautionSequenceCheckBox1(Boolean cautionSequenceCheckBox1) {
        this.cautionSequenceCheckBox1 = cautionSequenceCheckBox1;
    }

    public void setCautionSequenceCheckBox2(Boolean cautionSequenceCheckBox2) {
        this.cautionSequenceCheckBox2 = cautionSequenceCheckBox2;
    }

    public void setCautionSequenceCheckBox3(Boolean cautionSequenceCheckBox3) {
        this.cautionSequenceCheckBox3 = cautionSequenceCheckBox3;
    }

    public void setCautionSequenceTitle(String cautionSequenceTitle) {
        this.cautionSequenceTitle = cautionSequenceTitle;
    }

    public void setCautionSequenceDescription(Map<String, Value> cautionSequenceDescription) {
        this.cautionSequenceDescription = cautionSequenceDescription;
    }

    public void setCautionSequenceText1(Image cautionSequenceText1) {
        this.cautionSequenceText1 = cautionSequenceText1;
    }

    public void setCautionSequenceImage1(Image cautionSequenceImage1) {
        this.cautionSequenceImage1 = cautionSequenceImage1;
    }

    public void setCautionSequenceText2(Image cautionSequenceText2) {
        this.cautionSequenceText2 = cautionSequenceText2;
    }

    public void setCautionSequenceImage2(Image cautionSequenceImage2) {
        this.cautionSequenceImage2 = cautionSequenceImage2;
    }

    public void setCautionSequenceText3(Image cautionSequenceText3) {
        this.cautionSequenceText3 = cautionSequenceText3;
    }

    public void setCautionSequenceImage3(Image cautionSequenceImage3) {
        this.cautionSequenceImage3 = cautionSequenceImage3;
    }

    @Execute
    public void action(


    ) {

    }
}
