package com.automationanywhere.botcommand.actions.config;

import com.automationanywhere.botcommand.data.Value;
import com.automationanywhere.botcommand.data.impl.DictionaryValue;
import com.automationanywhere.botcommand.data.impl.StringValue;
import com.automationanywhere.botcommand.exception.BotCommandException;
import com.automationanywhere.botcommand.utilities.file.FileValidator;
import com.automationanywhere.commandsdk.annotations.*;
import com.automationanywhere.commandsdk.annotations.rules.FileExtension;
import com.automationanywhere.commandsdk.annotations.rules.ListType;
import com.automationanywhere.commandsdk.annotations.rules.NotEmpty;
import com.automationanywhere.commandsdk.annotations.rules.SelectModes;
import com.automationanywhere.commandsdk.model.AttributeType;
import com.automationanywhere.commandsdk.model.DataType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * @author Sumit Kumar
 */

@BotCommand
@CommandPkg(label = "Read JSON",
        node_label = "{{inputMethod}}, parse {{parsingMethod}} and save to {{returnTo}}",
        description = "Read values from JSON file and save to dictionary",
        icon = "json.svg", name = "config_read_json",
        group_label = "Config",
        text_color = "#fbc02d",
        return_label = "Output: config dictionary", return_type = DataType.DICTIONARY, return_sub_type =
        DataType.STRING, return_name = "Config", return_Direct = true,
        documentation_url = "https://github.com/A360-Tools/Bot-Framework/blob/main/docs/config/JSONReader.md",
//        allowed_agent_targets = AllowedTarget.HEADLESS,
        return_required = true
)
public class JSONReader {
    private static final String INPUT_TYPE_FILE = "FILE";
    private static final String INPUT_TYPE_TEXT = "TEXT";
    private static final String PARSING_TYPE_ALL = "ALL";
    private static final String PARSING_TYPE_SPECIFIC = "SPECIFIC";

    public static Object getJSON(String jsonString) {
        try {
            return new JSONObject(jsonString);
        } catch (JSONException eObject) {
            try {
                return new JSONArray(jsonString);
            } catch (JSONException eArray) {
                throw new BotCommandException("Not a valid JSON object or array.");
            }
        }
    }

    @Execute
    public DictionaryValue action(
            @Idx(index = "1", type = AttributeType.SELECT, options = {
                    @Idx.Option(index = "1.1", pkg = @Pkg(label = "File", value = INPUT_TYPE_FILE)),
                    @Idx.Option(index = "1.2", pkg = @Pkg(label = "Text", value = INPUT_TYPE_TEXT))
            })
            @Pkg(label = "JSON Source", default_value = INPUT_TYPE_FILE, default_value_type = DataType.STRING)
            @SelectModes
            @NotEmpty
            String inputMethod,

            @Idx(index = "1.1.1", type = AttributeType.FILE)
            @Pkg(label = "File Path")
            @FileExtension("json")
            String jsonFilePath,

            @Idx(index = "1.1.2", type = AttributeType.TEXT)
            @Pkg(label = "Character Set", default_value = "UTF-8", default_value_type = DataType.STRING,
                    description = "Either a canonical name or an alias, E.g." +
                            "US-ASCII," +
                            "ISO-8859-1," +
                            "UTF-16," +
                            "UTF-16BE," +
                            "UTF-16LE," +
                            "UTF-32," +
                            "UTF-32BE," +
                            "UTF-32LE etc."
            )
            @NotEmpty
            String charsetName,

            @Idx(index = "1.2.1", type = AttributeType.TEXTAREA)
            @Pkg(label = "Text", description = "JSON content as text")
            @NotEmpty
            String jsonText,

            @Idx(index = "3", type = AttributeType.SELECT, options = {
                    @Idx.Option(index = "3.1", pkg = @Pkg(label = "All Nodes", value = PARSING_TYPE_ALL,
                            node_label = "all nodes")),
                    @Idx.Option(index = "3.2", pkg = @Pkg(label = "Specific Node", value = PARSING_TYPE_SPECIFIC,
                            node_label = "path segment represented by {{jsonPathSegments}}"))
            })
            @Pkg(label = "Parsing Option", default_value = PARSING_TYPE_ALL, default_value_type = DataType.STRING)
            @SelectModes
            @NotEmpty
            String parsingMethod,

            @Idx(index = "3.2.1", type = AttributeType.LIST)
            @Pkg(label = "Parse specific node", description = "Path segment from root (e.g., " +
                    "list: ['config','dev']) to parse entries in config.dev")
            @ListType(DataType.STRING)
            List<Value> jsonPathSegments,

            @Idx(index = "4", type = AttributeType.BOOLEAN)
            @Pkg(label = "Trim values", default_value_type = DataType.BOOLEAN, default_value = "false", node_label =
                    "Trim values in resulting dictionary")
            @NotEmpty
            Boolean isTrimValues


    ) {
        try {
            Map<String, Value> jsonDictionary;
            if (jsonPathSegments == null || parsingMethod.equalsIgnoreCase(PARSING_TYPE_ALL)) {
                jsonPathSegments = List.of();
            }
            switch (inputMethod) {
                case INPUT_TYPE_FILE:
                    FileValidator fileValidator = new FileValidator(jsonFilePath);
                    String[] allowedExtensions = {"json"};
                    fileValidator.validateFile(allowedExtensions);
                    Charset charset = Charset.forName(charsetName);

                    String jsonContent = Files.readString(Path.of(jsonFilePath), charset);
                    jsonDictionary = parseJSON(jsonContent, jsonPathSegments, isTrimValues);
                    break;
                case INPUT_TYPE_TEXT:
                    jsonDictionary = parseJSON(jsonText, jsonPathSegments, isTrimValues);
                    break;
                default:
                    throw new BotCommandException("Invalid parsing method: " + inputMethod);
            }

            return new DictionaryValue(jsonDictionary);

        } catch (Exception e) {
            throw new BotCommandException("Error occurred: " + e.getMessage());
        }
    }

    private Map<String, Value> parseJSON(String jsonString, List<Value> jsonPathSegments, Boolean isTrimValues) {
        try {
            Object currentNode = getJSON(jsonString);
            Map<String, Value> jsonDictionary = new LinkedHashMap<>();
            //traverse to interested json object node
            for (Value pathSegment : jsonPathSegments) {
                String pathSegmentString = pathSegment.get().toString();
                if (currentNode instanceof JSONObject) {
                    currentNode = ((JSONObject) currentNode).get(pathSegmentString);
                } else {
                    break;
                }
            }
            //traverse all available keys within this json node
            traverseJson("", currentNode, jsonDictionary, false, isTrimValues);
            return jsonDictionary;
        } catch (Exception e) {
            throw new BotCommandException("Error parsing JSON: " + e.getMessage());
        }
    }

    private void traverseJson(String prefix, Object jsonNode, Map<String, Value> jsonDictionary,
                              boolean isParentNodeObject, Boolean isTrimValues) {
        if (jsonNode instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) jsonNode;
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = jsonObject.get(key);
                traverseJson(prefix + key + ".", value, jsonDictionary, true, isTrimValues);
            }
        } else if (jsonNode instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) jsonNode;
            for (int i = 0; i < jsonArray.length(); i++) {
                Object arrayElement = jsonArray.get(i);
                if (isParentNodeObject) {
                    prefix = prefix.substring(0, prefix.length() - 1);
                }
                traverseJson(prefix + "[" + i + "].", arrayElement, jsonDictionary, false, isTrimValues);
            }
        } else {
            String currentKey = prefix.substring(0, prefix.length() - 1);
            String value = jsonNode == null ? "" : jsonNode.toString();
            value = isTrimValues ? value.strip() : value;
            jsonDictionary.put(currentKey, new StringValue(value));
        }
    }

}
