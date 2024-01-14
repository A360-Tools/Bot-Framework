package com.automationanywhere.botcommand.utilities.logger;

import com.automationanywhere.botcommand.data.Value;
import com.automationanywhere.botcommand.data.impl.*;
import com.automationanywhere.botcommand.data.model.record.Record;
import com.automationanywhere.botcommand.data.model.table.Table;
import org.apache.commons.text.StringEscapeUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HTMLGenerator {

    private static final String NULL = "NULL";

    public static String generateHTML(Map<String, Value> valueMap) {
        if (valueMap.entrySet().isEmpty())
            return "";
        String uninqueID = UUID.randomUUID().toString();
        StringBuilder htmlBuilder = new StringBuilder();
        htmlBuilder
                .append("<label class='btn' for='")
                .append(uninqueID)
                .append("'>")
                .append("Show Details [")
                .append(valueMap.entrySet().size())
                .append("]</label>");

        htmlBuilder.append("<input class='modal-state' id='")
                .append(uninqueID)
                .append("' type='checkbox' />");

        htmlBuilder
                .append("<div class='modal'>")
                .append("<label class='modal__bg' for='")
                .append(uninqueID)
                .append("'></label>")
                .append("<div class='modal__inner'>");

        for (Map.Entry<String, Value> entry : valueMap.entrySet()) {
            generateVariableHTML(entry.getKey(), entry.getValue(), htmlBuilder);
        }
        htmlBuilder.append("</div>")
                .append("</div>");
        return htmlBuilder.toString();
    }

    private static void generateVariableHTML(String name, Value value, StringBuilder htmlBuilder) {
        name = name != null ? name : NULL;
        htmlBuilder
                .append("<div class='card'>")
                .append("<label>")
                .append(StringEscapeUtils.escapeHtml4(name))
                .append("</label>");

        if (value == null) {
            htmlBuilder.append("<label>")
                    .append(NULL)
                    .append("</label>");
        } else {
            String classname = value.getClass().getSimpleName();
            String variableType = classname.toLowerCase()
                    .replace("object", "")
                    .replace("value", "").toUpperCase();

            htmlBuilder.append("<label>")
                    .append(StringEscapeUtils.escapeHtml4(variableType))
                    .append("</label>");

            switch (variableType) {
                case "LIST":
                    visitListValue((ListValue) value, htmlBuilder);
                    break;
                case "RECORD":
                    visitRecordValue((RecordValue) value, htmlBuilder);
                    break;
                case "TABLE":
                    visitTableValue((TableValue) value, htmlBuilder);
                    break;
                case "DICTIONARY":
                    visitDictionaryValue((DictionaryValue) value, htmlBuilder);
                    break;
                default:
                    visitScalarValue(value, htmlBuilder);
                    break;
            }
        }

        htmlBuilder.append("</div>");
    }

    private static void visitListValue(ListValue listValue, StringBuilder htmlBuilder) {
        List<Value> list = listValue.get();
        htmlBuilder.append("<details>")
                .append("<summary> Value [")
                .append(list.size())
                .append(" items]</summary>");

        if (!list.isEmpty()) {
            for (int i = 0; i < list.size(); i++) {
                generateVariableHTML("At index [" + i + "]", list.get(i), htmlBuilder);
            }
        }

        htmlBuilder.append("</details>");
    }

    private static void visitRecordValue(RecordValue recordValue, StringBuilder htmlBuilder) {
        Record record = recordValue.get();
        htmlBuilder.append("<details>")
                .append("<summary> Value [")
                .append(record.getSchema().size())
                .append(" Columns]</summary>")
                .append("<table>")
                .append("<tr>");

        for (int i = 0; i < record.getSchema().size(); i++) {
            htmlBuilder.append("<th>")
                    .append("<span title='Table Header' type='text' disabled value='")
                    .append(StringEscapeUtils.escapeHtml4(record.getSchema().get(i).getName()))
                    .append("'></th>");
        }

        htmlBuilder.append("</tr>");

        int j = 0;
        for (Value v : record.getValues()) {
            htmlBuilder.append("<td>");
            generateVariableHTML("Col" + (++j), v, htmlBuilder);
            htmlBuilder.append("</td>");
        }

        htmlBuilder.append("</tr>")
                .append("</table>")
                .append("</details>");
    }

    private static void visitTableValue(TableValue tableValue, StringBuilder htmlBuilder) {
        Table table = tableValue.get();
        htmlBuilder.append("<details>")
                .append("<summary> Value [")
                .append(table.getRows().size())
                .append(" Rows x ")
                .append(table.getSchema().size())
                .append(" Columns]</summary>")
                .append("<table>")
                .append("<tr>");

        for (int i = 0; i < table.getSchema().size(); i++) {
            htmlBuilder.append("<th>")
                    .append("<span>")
                    .append(StringEscapeUtils.escapeHtml4(table.getSchema().get(i).getName()))
                    .append("</span>")
                    .append("</th>");
        }

        htmlBuilder.append("</tr>");

        for (int i = 0; i < table.getRows().size(); i++) {
            htmlBuilder.append("<tr>");
            for (int j = 0; j < table.getRows().get(i).getValues().size(); j++) {
                htmlBuilder.append("<td>");
                generateVariableHTML("R" + (i + 1) + "C" + (j + 1),
                        table.getRows().get(i).getValues().get(j),
                        htmlBuilder);

                htmlBuilder.append("</td>");
            }
            htmlBuilder.append("</tr>");
        }

        htmlBuilder.append("</table>")
                .append("</details>");
    }

    private static void visitDictionaryValue(DictionaryValue dictionaryValue, StringBuilder htmlBuilder) {
        Map<String, Value> dictionaryMap = dictionaryValue.get();
        htmlBuilder.append("<details>")
                .append("<summary> Value [").append(dictionaryMap.size()).append(" pairs] </summary>");

        for (Map.Entry<String, Value> entry : dictionaryMap.entrySet()) {
            generateVariableHTML("At Key: " + entry.getKey(), entry.getValue(), htmlBuilder);
        }

        htmlBuilder.append("</details>");
    }

    private static void visitScalarValue(Value scalarValue, StringBuilder htmlBuilder) {
        String objectStringvalue = "";
        if (scalarValue instanceof CredentialObject)
            objectStringvalue = ((CredentialObject) scalarValue).get().getInsecureString();
        else if (scalarValue instanceof NumberValue
                || scalarValue instanceof StringValue
                || scalarValue instanceof DateTimeValue
                || scalarValue instanceof BooleanValue)
            objectStringvalue = scalarValue.get().toString();
        htmlBuilder.append("<textarea disabled>")
                .append(StringEscapeUtils.escapeHtml4(objectStringvalue))
                .append("</textarea>");
    }

    public static String getScreenshotHTML(String screenshotPath) {
        if (screenshotPath.isEmpty())
            return "";
        String uninqueID = UUID.randomUUID().toString();

        String htmlBuilder = "<label class='btn' for='" +
                uninqueID +
                "'>" +
                "Show Image" +
                "</label>" +
                "<input class='modal-state' id='" +
                uninqueID +
                "' type='checkbox' />" +
                "<div class='modal'>" +
                "<label class='modal__bg' for='" +
                uninqueID +
                "'></label>" +
                "<div class='modal__inner__img'>" +
                "<img src='" +
                screenshotPath +
                "'</img>";
        return htmlBuilder;
    }
}
