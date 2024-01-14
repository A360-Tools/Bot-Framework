package config;

import com.automationanywhere.botcommand.actions.config.CSVReader;
import com.automationanywhere.botcommand.data.impl.DictionaryValue;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class CSVReaderTest {

    private static final String TEST_CSV_PATH = "src/test/sample/test.csv";

    @Test
    public void testCSVReaderActionByIndex() {
        String parsingMethod = "INDEX";
        Double keyIndex = 0.0;
        Double valueIndex = 1.0;
        String charsetName = "UTF-8";
        Boolean isTrimValues = true;
        String delimiter = ",";

        CSVReader csvReader = new CSVReader();
        DictionaryValue result = csvReader.action(TEST_CSV_PATH, parsingMethod, keyIndex, valueIndex,
                null, null, charsetName, isTrimValues, delimiter);

        assertNotNull(result, "Result should not be null");
        result.get().forEach((k, v) -> System.out.println(k + " | " + v));
        assertEquals(result.get().size(), 5, "Dictionary should be same size as non empty csv rows");

    }

    @Test
    public void testCSVReaderActionByColumnName() {
        String parsingMethod = "HEADER";
        String keyColumnName = "Name";
        String valueColumnName = "Age";
        String charsetName = "UTF-8";
        Boolean isTrimValues = true;
        String delimiter = ",";

        CSVReader csvReader = new CSVReader();
        DictionaryValue result = csvReader.action(TEST_CSV_PATH, parsingMethod, null, null,
                keyColumnName, valueColumnName, charsetName, isTrimValues, delimiter);

        assertNotNull(result, "Result should not be null");
        result.get().forEach((k, v) -> System.out.println(k + " | " + v));
        assertEquals(result.get().size(), 4, "Dictionary should be same size as non empty data row");

    }
}