package config;

/**
 * @author Sumit Kumar
 */

import com.automationanywhere.botcommand.actions.config.XMLReader;
import com.automationanywhere.botcommand.data.impl.DictionaryValue;
import com.automationanywhere.botcommand.exception.BotCommandException;
import org.testng.Assert;
import org.testng.annotations.Test;

public class XMLReaderTest {

    @Test
    public void testXmlFileParsing() {
        XMLReader xmlReader = new XMLReader();

        // Set up input parameters
        String inputMethod = "FILE";
        String xmlFilePath = "src/test/sample/test.xml";
        String charsetName = "UTF-8";
        String xPathToNodes = "//config/dev/*";
        String dictionaryKeys = "TAG_ATTRIBUTE";
        String dictionaryKeysAttributeName = "id";
        String dictionaryValues = "TAG_TEXT";
        Boolean isTrimValues = false;

        // Call the action method
        DictionaryValue result = xmlReader.action(inputMethod, xmlFilePath, charsetName, null, xPathToNodes,
                dictionaryKeys, dictionaryKeysAttributeName, dictionaryValues, null, isTrimValues);

        // Perform assertions on the result
        Assert.assertNotNull(result);
        result.get().forEach((k, v) -> System.out.println(k + " | " + v));
        Assert.assertTrue(result.get().containsKey("1"));
        Assert.assertTrue(result.get().containsKey("2"));
        Assert.assertTrue(result.get().containsKey("3"));
        Assert.assertEquals("text1", result.get().get("1").get().toString());
        Assert.assertEquals("text2", result.get().get("2").get().toString());
        Assert.assertEquals("text3", result.get().get("3").get().toString());
    }

    @Test
    public void testXmlTextParsing() {
        // Create an instance of XMLReader
        XMLReader xmlReader = new XMLReader();

        // Set up input parameters
        String inputMethod = "text";
        String xmlText = "<root>\n" +
                "    <config>\n" +
                "        <dev>\n" +
                "            <db id=\"1\" value=\"value1\">text1</db>\n" +
                "            <auth id=\"2\" value=\"value2 \">text2</auth>\n" +
                "            <url id=\"3\" value=\"value3\">text3</url>\n" +
                "        </dev>\n" +
                "        <prod>\n" +
                "            <db id=\"4\" value=\"value4\">text4</db>\n" +
                "            <url id=\"5\" value=\"value5\">text5</url>\n" +
                "        </prod>\n" +
                "    </config>\n" +
                "</root>";
        String xPathToNodes = "//config/dev/*";
        String dictionaryKeys = "TAG_NAME";
        String dictionaryValues = "TAG_ATTRIBUTE_VALUE";
        String dictionaryValuesAttributeName = "value";
        Boolean isTrimValues = true;

        // Call the action method
        DictionaryValue result = xmlReader.action(inputMethod, null, null, xmlText, xPathToNodes,
                dictionaryKeys, null, dictionaryValues, dictionaryValuesAttributeName, isTrimValues);

        // Perform assertions on the result
        Assert.assertNotNull(result);
        result.get().forEach((k, v) -> System.out.println(k + " | " + v));
        Assert.assertTrue(result.get().containsKey("db"));
        Assert.assertTrue(result.get().containsKey("auth"));
        Assert.assertTrue(result.get().containsKey("url"));
        Assert.assertEquals("value1", result.get().get("db").get().toString());
        Assert.assertEquals("value2", result.get().get("auth").get().toString());
        Assert.assertEquals("value3", result.get().get("url").get().toString());
    }

    @Test(expectedExceptions = BotCommandException.class)
    public void testInvalidParsingMethod() {
        // Create an instance of XMLReader
        XMLReader xmlReader = new XMLReader();

        // Set up input parameters with an invalid parsing method
        String inputMethod = "file";
        String xmlFilePath = "path/to/test.xml";
        String xPathToNodes = "//config/dev/*";
        String dictionaryKeys = "name";
        String dictionaryKeysAttributeName = "id";
        String dictionaryValues = "text";
        String dictionaryValuesAttributeName = "value";
        String charsetName = "UTF-8";
        Boolean isTrimValues = true;

        // Call the action method and expect a BotCommandException
        xmlReader.action(inputMethod, xmlFilePath, charsetName, null, xPathToNodes,
                dictionaryKeys, dictionaryKeysAttributeName, dictionaryValues, dictionaryValuesAttributeName, isTrimValues);
    }
}
