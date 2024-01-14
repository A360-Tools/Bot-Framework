package config;

import com.automationanywhere.botcommand.actions.config.ExcelReader;
import com.automationanywhere.botcommand.data.impl.DictionaryValue;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class ExcelReaderTest {

    private static final String TEST_EXCEL_PATH = "src/test/sample/test.xlsx";

    @Test
    public void testExcelReaderActionByIndex() {
        String sheetName = "sheet1";
        String parsingMethod = "index";
        Boolean passwordProtected = false;
        Double keyIndex = 0.0;
        Double valueIndex = 1.0;
        Boolean isTrimValues = true;

        ExcelReader excelReader = new ExcelReader();
        DictionaryValue result = excelReader.action(TEST_EXCEL_PATH, sheetName, passwordProtected, null, parsingMethod,
                keyIndex, valueIndex,
                null, null, isTrimValues);

        assertNotNull(result, "Result should not be null");
        result.get().forEach((k, v) -> System.out.println(k + " | " + v));
        assertEquals(result.get().size(), 6, "Dictionary should be same size as non empty csv rows");
    }

    @Test
    public void testExcelReaderActionByColumnName() {
        String sheetName = "sheet2";
        Boolean passwordProtected = false;
        String parsingMethod = "header";
        String keyColumnName = "Name";
        String valueColumnName = "Age";
        Boolean isTrimValues = true;

        ExcelReader excelReader = new ExcelReader();
        DictionaryValue result = excelReader.action(TEST_EXCEL_PATH, sheetName, passwordProtected, null, parsingMethod,
                null, null,
                keyColumnName, valueColumnName, isTrimValues);

        assertNotNull(result, "Result should not be null");
        result.get().forEach((k, v) -> System.out.println(k + " | " + v));
        assertEquals(result.get().size(), 4, "Dictionary should be same size as non empty data rows");
    }
}