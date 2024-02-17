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
            header = IOUtils.toByteArray(Objects.requireNonNull(CustomHTMLLayout.class.getResourceAsStream(templatePath)));
            machine = InetAddress.getLocalHost().getHostName();
            user = System.getProperty("user.name");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss Z"); // Define your desired date and time
    // format

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
        String htmlFormattedVariables = "";
        String screenshotPath = "";

        //parse the message and extract column details for current row
        Object[] parameters = event.getMessage().getParameters();
        if (parameters != null && parameters.length > 0 && parameters[0] instanceof Map) {
            Map<String, Object> messageObject = (Map<String, Object>) parameters[0];
            message = Optional.ofNullable(messageObject.get(Columns.MESSAGE)).map(Object::toString).orElse("");
            sourceBotPath = Optional.ofNullable(messageObject.get(Columns.SOURCE)).map(Object::toString).orElse("");
            screenshotPath = Optional.ofNullable(messageObject.get(Columns.SCREENSHOT)).map(Object::toString).orElse("");

            if (messageObject.get(Columns.VARIABLES) != null && messageObject.get(Columns.VARIABLES) instanceof Map) {
                Map<String, Value> variableMap = (Map<String, Value>) messageObject.get(Columns.VARIABLES);
                htmlFormattedVariables = HTMLGenerator.generateHTML(variableMap);
            }

        } else {
            message = event.getMessage().getFormattedMessage();
        }

        formattedContent = String.format(
                "<tr>" +
                        "<td>%s</td>" +
                        "<td>%s</td>" +
                        "<td>%s</td>" +
                        "<td>%s</td>" +
                        "<td>%s</td>" +
                        "<td>%s</td>" +
                        "<td>%s</td>" +
                        "<td>%s</td>" +
                        "</tr>",
                StringEscapeUtils.escapeHtml4(dateFormat.format(event.getTimeMillis())),
                StringEscapeUtils.escapeHtml4(event.getLevel().toString()),
                StringEscapeUtils.escapeHtml4(sourceBotPath),
                StringEscapeUtils.escapeHtml4(machine),
                StringEscapeUtils.escapeHtml4(user),
                StringEscapeUtils.escapeHtml4(message),
                htmlFormattedVariables,
                HTMLGenerator.getScreenshotHTML(screenshotPath)
        );


        return formattedContent;
    }

    @Override
    public byte[] getHeader() {
        // Add header content similar to Log4j2's HtmlLayout
        return header;
    }

    @Override
    public byte[] getFooter() {
        // Add footer content similar to Log4j2's HtmlLayout
        return footer;
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
    }
}
