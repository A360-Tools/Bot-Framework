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
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertNotNull;

public class JSONReaderTest {
    @Test
    public void testJsonFileParsingAllObjectNode() {
        // Given
        JSONReader jsonReader = new JSONReader();
        String inputMethod = "FILE";
        String jsonFilePath = "src/test/sample/test.json";
        String charsetName = "UTF-8";
        String parsingMethod = "ALL";
        Boolean isTrimValues = true;

        // When
        DictionaryValue result = jsonReader.action(inputMethod, jsonFilePath,
                charsetName, null, parsingMethod, null, isTrimValues);

        // Then
        Map<String, Value> resultMap = result.get();
        assertNotNull(result, "Result should not be null");
        result.get().forEach((k, v) -> System.out.println(k + " | " + v));
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

    @Test
    public void testJsonFileParsingSpecificObjectNode() {
        // Given
        JSONReader jsonReader = new JSONReader();
        String inputMethod = "FILE";
        String jsonFilePath = "src/test/sample/test.json";
        String charsetName = "UTF-8";
        String parsingMethod = "SPECIFIC";
        List<Value> jsonPathSegments = new ArrayList<>();
        jsonPathSegments.add(new StringValue("person"));
        jsonPathSegments.add(new StringValue("address"));
        Boolean isTrimValues = true;


        // When
        DictionaryValue result = jsonReader.action(inputMethod, jsonFilePath,
                charsetName, null, parsingMethod, jsonPathSegments, isTrimValues);

        // Then
        Map<String, Value> resultMap = result.get();
        assertNotNull(result, "Result should not be null");
        result.get().forEach((k, v) -> System.out.println(k + " | " + v));
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
        // Given
        JSONReader jsonReader = new JSONReader();
        String inputMethod = "FILE";
        String jsonFilePath = "src/test/sample/test.json";
        String charsetName = "UTF-8";
        String parsingMethod = "SPECIFIC";
        List<Value> jsonPathSegments = new ArrayList<>();
        jsonPathSegments.add(new StringValue("person"));
        jsonPathSegments.add(new StringValue("friends"));
        Boolean isTrimValues = true;
        // When
        DictionaryValue result = jsonReader.action(inputMethod, jsonFilePath,
                charsetName, null, parsingMethod, jsonPathSegments, isTrimValues);

        // Then
        Map<String, Value> resultMap = result.get();
        assertNotNull(result, "Result should not be null");
        result.get().forEach((k, v) -> System.out.println(k + " | " + v));
        Assert.assertEquals(resultMap.size(), 8);
        Assert.assertTrue(resultMap.containsKey("[0].friendName"));
        Assert.assertTrue(resultMap.containsKey("[0].friendAge"));
        Assert.assertEquals(resultMap.get("[0].friendName").toString(), "Alice");
        Assert.assertEquals(resultMap.get("[0].friendAge").toString(), "28");
    }

    @Test
    public void testJsonTextParsingSpecificObjectNode() {
        // Given
        JSONReader jsonReader = new JSONReader();
        String inputMethod = "TEXT";
        String parsingMethod = "SPECIFIC";
        List<Value> jsonPathSegments = new ArrayList<>();
        jsonPathSegments.add(new StringValue("person"));
        jsonPathSegments.add(new StringValue("address"));
        Boolean isTrimValues = true;
        String jsonText = "{\n" +
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


        // When
        DictionaryValue result = jsonReader.action(inputMethod, null,
                null, jsonText, parsingMethod, jsonPathSegments, isTrimValues);

        // Then
        assertNotNull(result, "Result should not be null");
        result.get().forEach((k, v) -> System.out.println(k + " | " + v));
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
        // Given
        JSONReader jsonReader = new JSONReader();

        // When
        jsonReader.action("invalid_method", "", "UTF-8", "", "invalid", null, false);

        // Then expect BotCommandException
    }

    @Test(expectedExceptions = BotCommandException.class)
    public void testJsonPathSegmentNotFound() {
        // Given
        JSONReader jsonReader = new JSONReader();
        String jsonText = "{ \"name\": \"Bob\", \"age\": 40 }";
        String charsetName = "UTF-8";
        List<Value> jsonPathSegments = List.of(new StringValue("invalidSegment"));
        Boolean isTrimValues = true;

        // When
        jsonReader.action("text", "", charsetName, jsonText, "SPECIFIC", jsonPathSegments, isTrimValues);
        // Then expect BotCommandException
    }
}
