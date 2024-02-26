package config;

import com.automationanywhere.botcommand.actions.config.CSVReader;
import com.automationanywhere.botcommand.data.impl.DictionaryValue;
import com.automationanywhere.botcommand.exception.BotCommandException;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Sumit Kumar
 */

public class CSVReaderTest {
    private static final String COLUMN_INDEX = "INDEX";
    private static final String COLUMN_HEADER = "HEADER";
    private static final String sampleTestFilePath = "src/test/sample/test.csv";
    private static final String semicolonDelimiterFilePath = "src/test/sample/test_semicolon.csv";
    private static final String CHARSET_UTF_8 = "UTF-8";
    private static final String DELIMITER_COMMA = ",";
    private static final String DELIMITER_SEMICOLON = ";";
    private CSVReader csvReader;

    @BeforeMethod
    public void setUp() {
        csvReader = new CSVReader();
    }

    @AfterMethod
    public void tearDown() {
        csvReader = null;
    }

    @Test
    public void testReadCSVWithHeaderSuccessfully() {
        DictionaryValue result = csvReader.action(sampleTestFilePath, COLUMN_HEADER,
                null, null, "Name", "Age",
                CHARSET_UTF_8,
                false, DELIMITER_COMMA);
        Assert.assertNotNull(result, "The result should not be null.");
        Assert.assertEquals(result.get("John Doe").toString(), "25", "The age of John Doe should be 25.");
    }

    @Test
    public void testReadCSVWithIndexSuccessfully() {
        DictionaryValue result = csvReader.action(sampleTestFilePath, COLUMN_INDEX,
                0, 1, null, null,
                CHARSET_UTF_8, false, DELIMITER_COMMA);
        Assert.assertNotNull(result, "The result should not be null.");
        Assert.assertEquals(result.get("John Doe").toString(), "25", "The age of John Doe should be 25.");
    }

    @Test(expectedExceptions = BotCommandException.class)
    public void testReadCSVWithInvalidFilePath() {
        csvReader.action("invalid/path.csv", COLUMN_HEADER,
                null, null, "Name", "Age",
                CHARSET_UTF_8, false, DELIMITER_COMMA);
    }

    @Test
    public void testReadCSVWithCustomDelimiter() {
        DictionaryValue result = csvReader.action(semicolonDelimiterFilePath, COLUMN_HEADER,
                null, null, "Name", "Age",
                CHARSET_UTF_8, false, DELIMITER_SEMICOLON);
        Assert.assertNotNull(result, "The result should not be null.");
        Assert.assertEquals(result.get("John Doe").toString(), "25", "The age of John Doe should be 25.");
    }

    @Test
    public void testReadCSVWithTrimValuesEnabled() {
        DictionaryValue result = csvReader.action(sampleTestFilePath, COLUMN_HEADER,
                null, null, "Name", "Age", CHARSET_UTF_8,
                true, DELIMITER_COMMA);
        Assert.assertNotNull(result, "The result should not be null.");
        Assert.assertEquals(result.get("Alice").toString(), "22", "The age of Alice should be 22, with spaces trimmed" +
                ".");
    }
}
