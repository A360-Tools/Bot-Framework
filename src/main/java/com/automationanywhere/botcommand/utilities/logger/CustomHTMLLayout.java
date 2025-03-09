package com.automationanywhere.botcommand.utilities.logger;

import com.automationanywhere.botcommand.data.Value;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Sumit Kumar
 */
@Plugin(name = "CustomHTMLLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
public class CustomHTMLLayout extends AbstractStringLayout {
    private static final byte[] footer = ("").getBytes();
    private static final String templatePath = "/templates/log.html";
    private static final byte[] header;
    private static final String machine;
    private static final String user;

    static {
        try {
            header =
                    IOUtils.toByteArray(Objects.requireNonNull(CustomHTMLLayout.class.getResourceAsStream(templatePath)));
            machine = InetAddress.getLocalHost().getHostName();
            user = System.getProperty("user.name");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss Z"); // Define your desired date and
    // time format

    public CustomHTMLLayout(Charset charset) {
        super(charset);
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public String toSerializable(LogEvent event) {
        String formattedContent;
        String message;
        String sourceBotPath = "";
        String variablesLink = "";
        String screenshotLink = "";

        // Parse the message and extract column details for current row
        Object[] parameters = event.getMessage().getParameters();
        if (parameters != null && parameters.length > 0 && parameters[ 0 ] instanceof Map) {
            Map<String, Object> messageObject = (Map<String, Object>) parameters[ 0 ];
            message = Optional.ofNullable(messageObject.get(Columns.MESSAGE)).map(Object::toString).orElse("");
            sourceBotPath = Optional.ofNullable(messageObject.get(Columns.SOURCE)).map(Object::toString).orElse("");
            String screenshotPath =
                    Optional.ofNullable(messageObject.get(Columns.SCREENSHOT)).map(Object::toString).orElse("");

            // Process variables if present
            if (messageObject.get(Columns.VARIABLES) != null && messageObject.get(Columns.VARIABLES) instanceof Map) {
                Map<String, Value> variableMap = (Map<String, Value>) messageObject.get(Columns.VARIABLES);

                // Generate count display for the main log
                String variablesCount = HTMLGenerator.generateHTML(variableMap);

                // If the variables folder path is provided, generate a separate file
                if (messageObject.get("variablesFolderPath") != null) {
                    String variablesFolderPath = messageObject.get("variablesFolderPath").toString();
                    String logEventId = UUID.randomUUID().toString();

                    // Generate variable file and get the link
                    String variablesFilePath = HTMLGenerator.generateVariableFile(variableMap, variablesFolderPath,
                            logEventId);

                    if (!variablesFilePath.isEmpty()) {
                        variablesLink =
                                "<a href='" + variablesFilePath + "' target='_blank' class='vars-link'>" + variablesCount + "</a>";
                    } else {
                        variablesLink = variablesCount;
                    }
                } else {
                    variablesLink = variablesCount;
                }
            }

            // Get screenshot link if screenshot exists
            screenshotLink = HTMLGenerator.getScreenshotHTML(screenshotPath);
        } else {
            message = event.getMessage().getFormattedMessage();
        }

        // Get the level as a CSS class for styling
        String levelClass = "level-" + event.getLevel().toString();

        formattedContent = String.format(
                "<tr>" +
                        "<td>%s</td>" +
                        "<td class='%s'>%s</td>" +
                        "<td>%s</td>" +
                        "<td class='responsive-hide'>%s</td>" +
                        "<td class='responsive-hide'>%s</td>" +
                        "<td>%s</td>" +
                        "<td>%s</td>" +
                        "<td>%s</td>" +
                        "</tr>",
                StringEscapeUtils.escapeHtml4(dateFormat.format(event.getTimeMillis())),
                levelClass,
                StringEscapeUtils.escapeHtml4(event.getLevel().toString()),
                StringEscapeUtils.escapeHtml4(sourceBotPath),
                StringEscapeUtils.escapeHtml4(machine),
                StringEscapeUtils.escapeHtml4(user),
                StringEscapeUtils.escapeHtml4(message),
                variablesLink,
                screenshotLink
        );

        return formattedContent;
    }

    @Override
    public byte[] getFooter() {
        return footer;
    }

    @Override
    public byte[] getHeader() {
        return header;
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<CustomHTMLLayout> {

        @PluginBuilderAttribute
        private Charset charset;

        private Builder() {
        }

        public Builder withCharset(Charset charset) {
            this.charset = charset;
            return this;
        }

        @Override
        public CustomHTMLLayout build() {
            return new CustomHTMLLayout(charset);
        }
    }

    public static class Columns {
        public static final String SOURCE = "Source";
        public static final String MESSAGE = "Message";
        public static final String VARIABLES = "Variables";
        public static final String SCREENSHOT = "Screenshot";
        public static final String VARIABLES_FOLDER_PATH = "variablesFolderPath";
    }
}