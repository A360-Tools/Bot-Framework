package config;

/**
 * @author Sumit Kumar
 */

import com.automationanywhere.botcommand.actions.config.XMLReader;
import com.automationanywhere.botcommand.data.Value;
import com.automationanywhere.botcommand.data.impl.DictionaryValue;
import com.automationanywhere.botcommand.exception.BotCommandException;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

public class XMLReaderTest {

    private static final String SAMPLE_XML_FILE_PATH = "src/test/sample/test.xml";
    private static final String SAMPLE_XML_CONTENT = "<root><config><dev>" +
            "<item id=\"1\" value=\"value1\">text1</item>" +
            "<item id=\"2\" value=\"value2\">text2</item>" +
            "<item id=\"3\" value=\"value3\">text3</item>" +
            "</dev></config></root>";
    private static final String INVALID_FILE_PATH = "invalid/path/to/xml.xml";
    private static final String XPATH_TO_NODES = "//dev/item";
    private static final String CHARSET_UTF_8 = "UTF-8";
    private static final String ID_ATTRIBUTE = "id";
    private static final String INPUT_TYPE_FILE = "FILE";
    private static final String INPUT_TYPE_TEXT = "TEXT";
    private static final String KEY_OPTION_TAG_ATTRIBUTE = "TAG_ATTRIBUTE";
    private static final String VALUE_OPTION_TAG_TEXT = "TAG_TEXT";
    private XMLReader xmlReader;

    @BeforeMethod
    public void setUp() {
        xmlReader = new XMLReader();
    }

    @AfterMethod
    public void tearDown() {
        xmlReader = null;
    }

    @Test
    public void testReadFromXmlFile() throws Exception {
        DictionaryValue result = xmlReader.action(
                INPUT_TYPE_FILE,
                SAMPLE_XML_FILE_PATH,
                CHARSET_UTF_8,
                null,
                XPATH_TO_NODES,
                KEY_OPTION_TAG_ATTRIBUTE,
                ID_ATTRIBUTE,
                VALUE_OPTION_TAG_TEXT,
                null,
                false
        );

        Map<String, Value> resultMap = result.get();
        Assert.assertNotNull(resultMap);
        Assert.assertTrue(result.get().containsKey("1"));
        Assert.assertTrue(result.get().containsKey("2"));
        Assert.assertTrue(result.get().containsKey("3"));
        Assert.assertEquals("text1", result.get().get("1").get().toString());
        Assert.assertEquals("text2", result.get().get("2").get().toString());
        Assert.assertEquals("text3", result.get().get("3").get().toString());
    }

    @Test
    public void testReadFromXmlText() {
        DictionaryValue result = xmlReader.action(
                INPUT_TYPE_TEXT,
                null,
                null,
                SAMPLE_XML_CONTENT,
                XPATH_TO_NODES,
                KEY_OPTION_TAG_ATTRIBUTE,
                ID_ATTRIBUTE,
                VALUE_OPTION_TAG_TEXT,
                null,
                false
        );

        Map<String, Value> resultMap = result.get();
        Assert.assertNotNull(resultMap);
        Assert.assertTrue(result.get().containsKey("1"));
        Assert.assertTrue(result.get().containsKey("2"));
        Assert.assertTrue(result.get().containsKey("3"));
        Assert.assertEquals("text1", result.get().get("1").get().toString());
        Assert.assertEquals("text2", result.get().get("2").get().toString());
        Assert.assertEquals("text3", result.get().get("3").get().toString());
    }

    @Test(expectedExceptions = BotCommandException.class)
    public void testInvalidXmlFilePath() {
        xmlReader.action(
                INPUT_TYPE_FILE,
                INVALID_FILE_PATH,
                CHARSET_UTF_8,
                null,
                XPATH_TO_NODES,
                KEY_OPTION_TAG_ATTRIBUTE,
                ID_ATTRIBUTE,
                VALUE_OPTION_TAG_TEXT,
                null,
                false
        );
    }

}
