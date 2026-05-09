package com.automationanywhere.botcommand.utilities.logger;

import com.automationanywhere.botcommand.data.Value;
import com.automationanywhere.botcommand.data.impl.*;
import com.automationanywhere.botcommand.data.model.record.Record;
import com.automationanywhere.botcommand.data.model.table.Table;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Sumit Kumar
 */

public class HTMLGenerator {
    private static final Logger LOGGER = LogManager.getLogger(HTMLGenerator.class);
    private static final String NULL = "NULL";
    private static final String TEMPLATE_PATH = "/templates/variables.html";
    private static final Pattern TRAILING_SPACES = Pattern.compile("( +)(?=\\n|$)", Pattern.MULTILINE);
    private static final String NL_MARKER = "<span class='ws-marker ws-nl' aria-hidden='true'>↵</span>\n";
    private static final String TAB_MARKER = "<span class='ws-marker ws-tab' aria-hidden='true'>→</span>\t";

    /**
     * HTML-escapes the input and inserts visible glyphs for newlines, tabs,
     * and trailing spaces so users can spot whitespace bugs in rendered
     * logs. Markers carry aria-hidden and rely on user-select: none in CSS
     * so they do not leak into copy/paste.
     */
    public static String escapeWithWhitespaceMarkers(String s) {
        if (s == null || s.isEmpty()) {
            return s == null ? "" : s;
        }
        String escaped = StringEscapeUtils.escapeHtml4(s);

        Matcher trailing = TRAILING_SPACES.matcher(escaped);
        if (trailing.find()) {
            StringBuilder buf = new StringBuilder(escaped.length() + 32);
            int last = 0;
            do {
                buf.append(escaped, last, trailing.start());
                int n = trailing.end() - trailing.start();
                buf.append("<span class='ws-marker ws-trail' aria-hidden='true'>")
                   .append("·".repeat(n))
                   .append("</span>");
                last = trailing.end();
            } while (trailing.find());
            buf.append(escaped, last, escaped.length());
            escaped = buf.toString();
        }

        if (escaped.indexOf('\t') >= 0) {
            escaped = escaped.replace("\t", TAB_MARKER);
        }
        if (escaped.indexOf('\n') >= 0) {
            escaped = escaped.replace("\n", NL_MARKER);
        }
        return escaped;
    }

    public static String generateHTML(Map<String, Value> valueMap) {
        if (valueMap == null || valueMap.isEmpty()) {
            return "";
        }
        int variableCount = valueMap.size();
        return "<span class='variable-count'>" + variableCount + " variable" + (variableCount > 1 ? "s" : "") +
                "</span>";
    }

    /**
     * Generates a separate HTML file for variable data and returns a link to it
     *
     * @param valueMap   Map of variable names to values
     * @param folderPath Folder where the HTML file will be stored
     * @param logEventId Unique identifier for the log event
     *
     * @return Relative path to the HTML file
     */
    public static String generateVariableFile(Map<String, Value> valueMap, String folderPath, String logEventId) {
        if (valueMap == null || valueMap.isEmpty()) {
            return "";
        }

        String filename = "variables_" + logEventId + ".html";
        Path filePath = Paths.get(folderPath, filename);

        // Create HTML content for variables
        StringBuilder htmlBuilder = new StringBuilder();

        try (InputStream templateStream = HTMLGenerator.class.getResourceAsStream(TEMPLATE_PATH)) {
            // Read the header template from the resource file
            byte[] templateBytes = IOUtils.toByteArray(Objects.requireNonNull(templateStream));
            String headerTemplate = new String(templateBytes, StandardCharsets.UTF_8);

            // Append the template header
            htmlBuilder.append(headerTemplate);

        } catch (IOException e) {
            throw new RuntimeException("Failed to read variables template: " + e.getMessage(), e);
        }

        // Continue with the container div
        if (!htmlBuilder.toString().contains("<div class='container'>")) {
            htmlBuilder.append("<div class='container'>\n");
        }

        // Add each variable to the HTML
        for (Map.Entry<String, Value> entry : valueMap.entrySet()) {
            appendVariableToHTML(entry.getKey(), entry.getValue(), htmlBuilder);
        }

        htmlBuilder.append("</div>\n")
                .append("</body>\n")
                .append("</html>");

        LOGGER.debug("Generated HTML file: {}", filePath);

        // Write the HTML to a file
        try {
            Files.write(filePath, htmlBuilder.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOGGER.error("Error writing variables HTML file: {}", e.getMessage());
            return "";
        }

        // Return the relative path to the file
        return "variables/" + filename;
    }

    private static void appendVariableToHTML(String name, Value value, StringBuilder htmlBuilder) {
        name = name != null ? name : NULL;

        // Only create a variable card if a name is provided (for top-level variables)
        boolean isTopLevel = !name.startsWith("Index ");

        if (isTopLevel) {
            htmlBuilder.append("<div class='variable-card'>\n")
                    .append("<div class='variable-name'>")
                    .append(StringEscapeUtils.escapeHtml4(name))
                    .append("</div>\n");
        }

        if (value == null) {
            htmlBuilder.append("<div class='variable-type'>NULL</div>\n")
                    .append("<div class='variable-value'>NULL</div>\n");
        } else {
            String classname = value.getClass().getSimpleName();
            String variableType = classname.toLowerCase()
                    .replace("object", "")
                    .replace("value", "").toUpperCase();

            if (isTopLevel) {
                htmlBuilder.append("<div class='variable-type'>")
                        .append(StringEscapeUtils.escapeHtml4(variableType))
                        .append("</div>\n");
            }

            switch (variableType) {
                case "LIST":
                    appendListValue((ListValue) value, htmlBuilder);
                    break;
                case "RECORD":
                    appendRecordValue((RecordValue) value, htmlBuilder);
                    break;
                case "TABLE":
                    appendTableValue((TableValue) value, htmlBuilder);
                    break;
                case "DICTIONARY":
                    appendDictionaryValue((DictionaryValue) value, htmlBuilder);
                    break;
                default:
                    appendScalarValue(value, htmlBuilder);
                    break;
            }
        }

        if (isTopLevel) {
            htmlBuilder.append("</div>\n");
        }
    }

    private static void appendListValue(ListValue listValue, StringBuilder htmlBuilder) {
        List<Value> list = listValue.get();

        htmlBuilder.append("<details>\n")
                .append("<summary>List: ")
                .append(list.size())
                .append(" items</summary>\n");

        if (!list.isEmpty()) {
            // Wrap children so a single continuous thread-line on the left
            // (rendered via summary::after) sits next to them with padding,
            // and the children push to the right of the line.
            htmlBuilder.append("<div class='nested-content'>\n");
            for (int i = 0; i < list.size(); i++) {
                Value itemValue = list.get(i);
                String indexName = "Index " + i;

                htmlBuilder.append("<div class='variable-card'>\n")
                        .append("<div class='variable-name'>")
                        .append(indexName)
                        .append("</div>\n");

                if (itemValue == null) {
                    htmlBuilder.append("<div class='variable-value'>NULL</div>\n");
                } else {
                    String itemType = itemValue.getClass().getSimpleName().toLowerCase()
                            .replace("object", "")
                            .replace("value", "").toUpperCase();

                    htmlBuilder.append("<div class='variable-type'>")
                            .append(StringEscapeUtils.escapeHtml4(itemType))
                            .append("</div>\n");

                    // Use appendVariableToHTML instead of separate switch cases
                    appendVariableValueWithoutCard(itemValue, htmlBuilder);
                }

                htmlBuilder.append("</div>\n");
            }
            htmlBuilder.append("</div>\n");
        }

        htmlBuilder.append("</details>\n");
    }

    private static void appendVariableValueWithoutCard(Value value, StringBuilder htmlBuilder) {
        if (value == null) {
            htmlBuilder.append("<div class='variable-value'>NULL</div>\n");
            return;
        }

        String variableType = value.getClass().getSimpleName().toLowerCase()
                .replace("object", "")
                .replace("value", "").toUpperCase();

        switch (variableType) {
            case "LIST":
                appendListValue((ListValue) value, htmlBuilder);
                break;
            case "RECORD":
                appendRecordValue((RecordValue) value, htmlBuilder);
                break;
            case "TABLE":
                appendTableValue((TableValue) value, htmlBuilder);
                break;
            case "DICTIONARY":
                appendDictionaryValue((DictionaryValue) value, htmlBuilder);
                break;
            default:
                appendScalarValue(value, htmlBuilder);
                break;
        }
    }

    private static void appendDictionaryValue(DictionaryValue dictionaryValue, StringBuilder htmlBuilder) {
        Map<String, Value> dictionaryMap = dictionaryValue.get();

        htmlBuilder.append("<details>\n")
                .append("<summary>Dictionary: ")
                .append(dictionaryMap.size())
                .append(" entries</summary>\n");

        if (!dictionaryMap.isEmpty()) {
            htmlBuilder.append("<div class='nested-content'>\n");
            for (Map.Entry<String, Value> entry : dictionaryMap.entrySet()) {
                String key = entry.getKey();
                Value val = entry.getValue();

                htmlBuilder.append("<div class='variable-card'>\n")
                        .append("<div class='variable-name'>")
                        .append(StringEscapeUtils.escapeHtml4(key))
                        .append("</div>\n");

                if (val == null) {
                    htmlBuilder.append("<div class='variable-value'>NULL</div>\n");
                } else {
                    String valType = val.getClass().getSimpleName().toLowerCase()
                            .replace("object", "")
                            .replace("value", "").toUpperCase();

                    htmlBuilder.append("<div class='variable-type'>")
                            .append(StringEscapeUtils.escapeHtml4(valType))
                            .append("</div>\n");

                    // Use appendVariableValueWithoutCard instead of separate switch cases
                    appendVariableValueWithoutCard(val, htmlBuilder);
                }

                htmlBuilder.append("</div>\n");
            }
            htmlBuilder.append("</div>\n");
        }

        htmlBuilder.append("</details>\n");
    }

    private static void appendRecordValue(RecordValue recordValue, StringBuilder htmlBuilder) {
        Record record = recordValue.get();

        htmlBuilder.append("<details class='full-width'>\n")
                .append("<summary>Record: ")
                .append(record.getSchema().size())
                .append(" columns</summary>\n")
                .append("<div class='table-container'>\n")
                .append("<table>\n")
                .append("<tr>\n");

        // Add header row
        for (int i = 0; i < record.getSchema().size(); i++) {
            htmlBuilder.append("<th>")
                    .append(StringEscapeUtils.escapeHtml4(record.getSchema().get(i).getName()))
                    .append("</th>\n");
        }

        htmlBuilder.append("</tr>\n");
        htmlBuilder.append("<tr>\n");

        // Add data row with proper nested handling
        for (Value v : record.getValues()) {
            htmlBuilder.append("<td>\n");

            if (v == null) {
                htmlBuilder.append("NULL");
            } else {
                // Use appendVariableValueWithoutCard for consistent rendering
                appendValueInCell(v, htmlBuilder);
            }

            htmlBuilder.append("</td>\n");
        }

        htmlBuilder.append("</tr>\n")
                .append("</table>\n")
                .append("</div>\n")
                .append("</details>\n");
    }

    private static void appendTableValue(TableValue tableValue, StringBuilder htmlBuilder) {
        Table table = tableValue.get();

        htmlBuilder.append("<details class='full-width'>\n")
                .append("<summary>Table: ")
                .append(table.getRows().size())
                .append(" rows x ")
                .append(table.getSchema().size())
                .append(" columns</summary>\n")
                .append("<div class='table-container'>\n")
                .append("<table>\n")
                .append("<tr>\n");

        // Add header row
        for (int i = 0; i < table.getSchema().size(); i++) {
            htmlBuilder.append("<th>")
                    .append(StringEscapeUtils.escapeHtml4(table.getSchema().get(i).getName()))
                    .append("</th>\n");
        }

        htmlBuilder.append("</tr>\n");

        // Add data rows with proper nested handling
        for (int i = 0; i < table.getRows().size(); i++) {
            htmlBuilder.append("<tr>\n");

            for (int j = 0; j < table.getRows().get(i).getValues().size(); j++) {
                Value cellValue = table.getRows().get(i).getValues().get(j);

                htmlBuilder.append("<td>\n");

                if (cellValue == null) {
                    htmlBuilder.append("NULL");
                } else {
                    // Use appendVariableValueWithoutCard for consistent rendering
                    appendValueInCell(cellValue, htmlBuilder);
                }

                htmlBuilder.append("</td>\n");
            }

            htmlBuilder.append("</tr>\n");
        }

        htmlBuilder.append("</table>\n")
                .append("</div>\n")
                .append("</details>\n");
    }

    // Helper method for table and record cells
    private static void appendValueInCell(Value value, StringBuilder htmlBuilder) {
        if (value == null) {
            htmlBuilder.append("NULL");
            return;
        }

        if (value instanceof ListValue) {
            appendListValue((ListValue) value, htmlBuilder);
        } else if (value instanceof DictionaryValue) {
            appendDictionaryValue((DictionaryValue) value, htmlBuilder);
        } else if (value instanceof RecordValue) {
            appendRecordValue((RecordValue) value, htmlBuilder);
        } else if (value instanceof TableValue) {
            appendTableValue((TableValue) value, htmlBuilder);
        } else {
            appendScalarValue(value, htmlBuilder);
        }
    }

    private static void appendScalarValue(Value scalarValue, StringBuilder htmlBuilder) {
        String objectStringValue = "";
        if (scalarValue instanceof CredentialObject) {
            objectStringValue = ((CredentialObject) scalarValue).get().getInsecureString();
        } else if (scalarValue instanceof NumberValue
                || scalarValue instanceof StringValue
                || scalarValue instanceof DateTimeValue
                || scalarValue instanceof BooleanValue) {
            objectStringValue = scalarValue.get().toString();
        }

        htmlBuilder.append("<div class='variable-value'>")
                .append(escapeWithWhitespaceMarkers(objectStringValue))
                .append("</div>\n");
    }

    public static String getScreenshotHTML(String screenshotPath) {
        if (screenshotPath == null || screenshotPath.isEmpty()) {
            return "";
        }

        // Create a relative path to the screenshot
        String relativePath = "screenshots/" + FilenameUtils.getName(screenshotPath);
        String escapedRelativePath = StringEscapeUtils.escapeHtml4(relativePath);

        // Return a link to the screenshot
        return "<a href='" + escapedRelativePath + "' target='_blank' class='img-link' " +
                "aria-label='Screenshot' title='Screenshot' " +
                "style='--screenshot-preview: url(\"" + escapedRelativePath + "\")'>" +
                "<img src='" + escapedRelativePath + "' loading='lazy' decoding='async' " +
                "width='88' height='50' alt='' />" +
                "</a>";
    }
}
