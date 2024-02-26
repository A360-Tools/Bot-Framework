package config;

/**
 * @author Sumit Kumar
 */

import com.automationanywhere.botcommand.actions.config.JSONReader;
import com.automationanywhere.botcommand.data.Value;
import com.automationanywhere.botcommand.data.impl.DictionaryValue;
import com.automationanywhere.botcommand.data.impl.StringValue;
import com.automationanywhere.botcommand.exception.BotCommandException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Sumit Kumar
 */

public class JSONReaderTest {

    private static final String JSON_FILE_PATH = "src/test/sample/test.json";
    private static final String CHARSET_UTF_8 = "UTF-8";
    private static final String JSON_TEXT = "{\n" +
            "  \"person\": {\n" +
            "    \"name\": \"John\",\n" +
            "    \"age\": 30,\n" +
            "    \"address\": {\n" +
            "      \"city\": \"New York\",\n" +
            "      \"zip\": \"10001\",\n" +
            "      \"coordinates\": {\n" +
            "        \"latitude\": 40.7128,\n" +
            "        \"longitude\": -74.0060\n" +
            "      }\n" +
            "    },\n" +
            "    \"phoneNumbers\": [\"123-456-7890\", \"987-654-3210\"],\n" +
            "    \"friends\": [\n" +
            "      {\n" +
            "        \"friendName\": \"Alice\",\n" +
            "        \"friendAge\": 28,\n" +
            "        \"interests\": [\"Reading\", \"Hiking\"]\n" +
            "      },\n" +
            "      {\n" +
            "        \"friendName\": \"Bob\",\n" +
            "        \"friendAge\": 32,\n" +
            "        \"interests\": [\"Cooking\", \"Gaming\"]\n" +
            "      }\n" +
            "    ]\n" +
            "  },\n" +
            "  \"company\": {\n" +
            "    \"name\": \"TechCorp\",\n" +
            "    \"location\": \"Silicon Valley\",\n" +
            "    \"employees\": [\n" +
            "      {\n" +
            "        \"employeeName\": \"Mary\",\n" +
            "        \"employeeAge\": 25\n" +
            "      },\n" +
            "      {\n" +
            "        \"employeeName\": \"James\",\n" +
            "        \"employeeAge\": 35\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}";
    private static final String INVALID_METHOD = "invalid_method";
    private static final String INVALID_PATH = "";
    private JSONReader jsonReader;

    @BeforeClass
    public void setUp() {
        jsonReader = new JSONReader();
    }

    @Test
    public void testJsonFileParsingAllObjectNode() {
        DictionaryValue result = jsonReader.action(
                "FILE",
                JSON_FILE_PATH,
                CHARSET_UTF_8,
                null,
                "ALL",
                null,
                true
        );

        assertResult(result, 22);
        Map<String, Value> resultMap = result.get();
        Assert.assertEquals(resultMap.size(), 22);
        Assert.assertTrue(resultMap.containsKey("person.address.city"));
        Assert.assertTrue(resultMap.containsKey("person.address.zip"));
        Assert.assertTrue(resultMap.containsKey("person.address.coordinates.longitude"));
        Assert.assertTrue(resultMap.containsKey("person.address.coordinates.latitude"));
        Assert.assertEquals(resultMap.get("person.address.city").toString(), "New York");
        Assert.assertEquals(resultMap.get("person.address.zip").toString(), "10001");
        Assert.assertEquals(resultMap.get("person.address.coordinates.longitude").toString(), "-74.0060");
        Assert.assertEquals(resultMap.get("person.address.coordinates.latitude").toString(), "40.7128");
    }

    private static void assertResult(DictionaryValue result, int expectedSize) {
        Assert.assertNotNull(result, "Result should not be null");
        Map<String, Value> resultMap = result.get();
        resultMap.forEach((k, v) -> System.out.println(k + " | " + v)); // For debug purposes
        Assert.assertEquals(resultMap.size(), expectedSize);
    }

    @Test
    public void testJsonFileParsingSpecificObjectNode() {
        List<Value> jsonPathSegments = new ArrayList<>();
        jsonPathSegments.add(new StringValue("person"));
        jsonPathSegments.add(new StringValue("address"));

        DictionaryValue result = jsonReader.action(
                "FILE",
                JSON_FILE_PATH,
                CHARSET_UTF_8,
                null,
                "SPECIFIC",
                jsonPathSegments,
                true
        );

        assertResult(result, 4);
        Map<String, Value> resultMap = result.get();
        Assert.assertEquals(resultMap.size(), 4);
        Assert.assertTrue(resultMap.containsKey("city"));
        Assert.assertTrue(resultMap.containsKey("zip"));
        Assert.assertTrue(resultMap.containsKey("coordinates.longitude"));
        Assert.assertTrue(resultMap.containsKey("coordinates.latitude"));
        Assert.assertEquals(resultMap.get("city").toString(), "New York");
        Assert.assertEquals(resultMap.get("zip").toString(), "10001");
        Assert.assertEquals(resultMap.get("coordinates.longitude").toString(), "-74.0060");
        Assert.assertEquals(resultMap.get("coordinates.latitude").toString(), "40.7128");
    }

    @Test
    public void testJsonFileParsingSpecificArrayNode() {
        List<Value> jsonPathSegments = new ArrayList<>();
        jsonPathSegments.add(new StringValue("person"));
        jsonPathSegments.add(new StringValue("friends"));

        DictionaryValue result = jsonReader.action(
                "FILE",
                JSON_FILE_PATH,
                CHARSET_UTF_8,
                null,
                "SPECIFIC",
                jsonPathSegments,
                true
        );

        assertResult(result, 8);
        Map<String, Value> resultMap = result.get();
        Assert.assertEquals(resultMap.size(), 8);
        Assert.assertTrue(resultMap.containsKey("[0].friendName"));
        Assert.assertTrue(resultMap.containsKey("[0].friendAge"));
        Assert.assertEquals(resultMap.get("[0].friendName").toString(), "Alice");
        Assert.assertEquals(resultMap.get("[0].friendAge").toString(), "28");
    }

    @Test
    public void testJsonTextParsingSpecificObjectNode() {
        List<Value> jsonPathSegments = new ArrayList<>();
        jsonPathSegments.add(new StringValue("person"));
        jsonPathSegments.add(new StringValue("address"));

        DictionaryValue result = jsonReader.action(
                "TEXT",
                null,
                null,
                JSON_TEXT,
                "SPECIFIC",
                jsonPathSegments,
                true
        );

        assertResult(result, 4);
        Map<String, Value> resultMap = result.get();
        Assert.assertEquals(resultMap.size(), 4);
        Assert.assertTrue(resultMap.containsKey("city"));
        Assert.assertTrue(resultMap.containsKey("zip"));
        Assert.assertTrue(resultMap.containsKey("coordinates.longitude"));
        Assert.assertTrue(resultMap.containsKey("coordinates.latitude"));
        Assert.assertEquals(resultMap.get("city").toString(), "New York");
        Assert.assertEquals(resultMap.get("zip").toString(), "10001");
        Assert.assertEquals(resultMap.get("coordinates.longitude").toString(), "-74.0060");
        Assert.assertEquals(resultMap.get("coordinates.latitude").toString(), "40.7128");
    }

    @Test(expectedExceptions = BotCommandException.class)
    public void testInvalidParsingMethod() {
        jsonReader.action(
                INVALID_METHOD,
                INVALID_PATH,
                CHARSET_UTF_8,
                JSON_TEXT,
                "ALL",
                null,
                false
        );
    }

    @Test(expectedExceptions = BotCommandException.class)
    public void testJsonPathSegmentNotFound() {
        List<Value> jsonPathSegments = List.of(new StringValue("invalidSegment"));

        jsonReader.action(
                "TEXT",
                INVALID_PATH,
                CHARSET_UTF_8,
                JSON_TEXT,
                "SPECIFIC",
                jsonPathSegments,
                true
        );
    }
}
