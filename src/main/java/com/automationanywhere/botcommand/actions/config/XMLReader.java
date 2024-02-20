package com.automationanywhere.botcommand.actions.config;

import com.automationanywhere.botcommand.data.Value;
import com.automationanywhere.botcommand.data.impl.DictionaryValue;
import com.automationanywhere.botcommand.data.impl.StringValue;
import com.automationanywhere.botcommand.exception.BotCommandException;
import com.automationanywhere.commandsdk.annotations.*;
import com.automationanywhere.commandsdk.annotations.rules.FileExtension;
import com.automationanywhere.commandsdk.annotations.rules.NotEmpty;
import com.automationanywhere.commandsdk.annotations.rules.SelectModes;
import com.automationanywhere.commandsdk.model.AllowedTarget;
import com.automationanywhere.commandsdk.model.AttributeType;
import com.automationanywhere.commandsdk.model.DataType;
import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

@BotCommand
@CommandPkg(label = "Read XML",
        node_label = "{{inputMethod}}, parse {{xPathToNodes}} and save to {{returnTo}}",
        description = "Read values from XML file and save to dictionary",
        icon = "xml.svg", name = "config_read_xml",
        group_label = "Config",
        text_color = "#f1662a",
        return_label = "Output: config dictionary", return_type = DataType.DICTIONARY, return_sub_type =
        DataType.STRING, return_name = "Config", return_Direct = true,
        allowed_agent_targets = AllowedTarget.HEADLESS,
        return_required = true
)
public class XMLReader {
    private static final String INPUT_TYPE_FILE = "FILE";
    private static final String INPUT_TYPE_TEXT = "TEXT";
    private static final String KEY_OPTION_TAG_NAME = "TAG_NAME";
    private static final String KEY_OPTION_TAG_ATTRIBUTE = "TAG_ATTRIBUTE";
    private static final String VALUE_OPTION_TAG_TEXT = "TAG_TEXT";
    private static final String VALUE_OPTION_TAG_ATTRIBUTE_VALUE = "TAG_ATTRIBUTE_VALUE";

    @Execute
    public DictionaryValue action(
            @Idx(index = "1", type = AttributeType.SELECT, options = {
                    @Idx.Option(index = "1.1", pkg = @Pkg(label = "XML File", value = INPUT_TYPE_FILE)),
                    @Idx.Option(index = "1.2", pkg = @Pkg(label = "XML Text", value = INPUT_TYPE_TEXT))
            })
            @Pkg(label = "Choose Parsing Method", default_value = INPUT_TYPE_FILE, default_value_type = DataType.STRING)
            @SelectModes
            String inputMethod,

            @Idx(index = "1.1.1", type = AttributeType.FILE)
            @Pkg(label = "XML File Path")
            @FileExtension("xml")
            @NotEmpty
            String xmlFilePath,

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
            @Pkg(label = "XML Text Value", description = "Provide XML content as text.")
            @NotEmpty
            String xmlText,

            @Idx(index = "2", type = AttributeType.TEXT)
            @Pkg(label = "Xpath to target elements", description = "Xpath to parse matching elements, e.g.: " +
                    "//config/dev/*")
            @NotEmpty
            String xPathToNodes,

            @Idx(index = "3", type = AttributeType.SELECT, options = {
                    @Idx.Option(index = "3.1", pkg = @Pkg(label = "Element's tag", value = KEY_OPTION_TAG_NAME)),
                    @Idx.Option(index = "3.2", pkg = @Pkg(label = "Element's attribute", value =
                            KEY_OPTION_TAG_ATTRIBUTE))
            })
            @Pkg(label = "Populate Dictionary keys from", default_value = KEY_OPTION_TAG_NAME, default_value_type = DataType.STRING)
            @SelectModes
            @NotEmpty
            String dictionaryKeys,

            @Idx(index = "3.2.1", type = AttributeType.TEXT)
            @Pkg(label = "Attribute name, whose value is to be saved as key")
            @NotEmpty
            String dictionaryKeysAttributeName,

            @Idx(index = "4", type = AttributeType.SELECT, options = {
                    @Idx.Option(index = "4.1", pkg = @Pkg(label = "Element's text", value = VALUE_OPTION_TAG_TEXT)),
                    @Idx.Option(index = "4.2", pkg = @Pkg(label = "Element's attribute value", value = VALUE_OPTION_TAG_ATTRIBUTE_VALUE))
            })
            @Pkg(label = "Populate Dictionary values from", default_value = VALUE_OPTION_TAG_TEXT, default_value_type =
                    DataType.STRING)
            @SelectModes
            @NotEmpty
            String dictionaryValues,

            @Idx(index = "4.2.1", type = AttributeType.TEXT)
            @Pkg(label = "Attribute name, whose value is to be saved as value")
            @NotEmpty
            String dictionaryValuesAttributeName,

            @Idx(index = "5", type = AttributeType.BOOLEAN)
            @Pkg(label = "Trim values", default_value_type = DataType.BOOLEAN, default_value = "false", node_label =
                    "Trim values in resulting dictionary")
            @NotEmpty
            Boolean isTrimValues


    ) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document;
            switch (inputMethod.toUpperCase()) {
                case INPUT_TYPE_FILE:
                    Charset charset = Charset.forName(charsetName);
                    String XmlFileText = FileUtils.readFileToString(new File(xmlFilePath), charset);
                    document = builder.parse(new InputSource(new StringReader(XmlFileText)));
                    break;
                case INPUT_TYPE_TEXT:
                    document = builder.parse(new InputSource(new StringReader(xmlText)));
                    break;
                default:
                    throw new BotCommandException("Invalid parsing method." + inputMethod);
            }

            Map<String, Value> xmlDictionary = parseXML(document, xPathToNodes, dictionaryKeys
                    , dictionaryKeysAttributeName, dictionaryValues, dictionaryValuesAttributeName, isTrimValues);

            return new DictionaryValue(xmlDictionary);
        } catch (Exception e) {
            throw new BotCommandException("Error occurred: " + e.getMessage());
        }
    }

    private Map<String, Value> parseXML(
            Document document,
            String xPathToNodes,
            String dictionaryKeys,
            String dictionaryKeysAttributeName,
            String dictionaryValues,
            String dictionaryValuesAttributeName,
            Boolean isTrimValues
    ) throws Exception {

        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();
        XPathExpression expression = xpath.compile(xPathToNodes);

        NodeList nodeList = (NodeList) expression.evaluate(document, XPathConstants.NODESET);
        Map<String, Value> xmlDictionary = new LinkedHashMap<>();

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            String key;
            String value;

            switch (dictionaryKeys.toUpperCase()) {
                case KEY_OPTION_TAG_NAME:
                    key = node.getNodeName();
                    break;
                case KEY_OPTION_TAG_ATTRIBUTE:
                    key = node.getAttributes().getNamedItem(dictionaryKeysAttributeName).getNodeValue();
                    break;
                default:
                    throw new BotCommandException("Invalid key parsing method: " + dictionaryKeys);
            }
            if (key == null || key.isEmpty())
                continue;

            switch (dictionaryValues.toUpperCase()) {
                case VALUE_OPTION_TAG_TEXT:
                    value = node.getTextContent();
                    break;
                case VALUE_OPTION_TAG_ATTRIBUTE_VALUE:
                    value = node.getAttributes().getNamedItem(dictionaryValuesAttributeName).getNodeValue();
                    break;
                default:
                    throw new BotCommandException("Invalid value parsing method: " + dictionaryValues);
            }
            value = value == null ? "" : value;
            value = isTrimValues ? value.strip() : value;
            xmlDictionary.put(key, new StringValue(value));
        }

        return xmlDictionary;
    }
}
