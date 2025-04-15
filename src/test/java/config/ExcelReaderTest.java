package config;

import com.automationanywhere.botcommand.actions.config.ExcelReader;
import com.automationanywhere.botcommand.data.impl.DictionaryValue;
import com.automationanywhere.botcommand.exception.BotCommandException;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Sumit Kumar
 */

public class ExcelReaderTest {

    private static final String SAMPLE_EXCEL_PATH = "src/test/sample/test.xlsx";
    private static final String SHEET_NAME_1 = "Sheet1";
    private static final String SHEET_NAME_2 = "Sheet2";
    private static final String COLUMN_NAME = "Name";
    private static final String COLUMN_AGE = "Age";
    private static final String COLUMN_INDEX = "INDEX";
    private static final String COLUMN_HEADER = "HEADER";
    private static final String SELECT_BY_NAME = "name";
    private static final String SELECT_BY_INDEX = "index";
    private static final int INDEX_NAME = 0; // assuming 'Name' is in the first column
    private static final int INDEX_AGE = 1; // assuming 'Age' is in the second column
    private static final int SHEET_INDEX_0 = 0; // Sheet1
    private static final int SHEET_INDEX_1 = 1; // Sheet2
    private ExcelReader excelReader;

    @BeforeMethod
    public void setUp() {
        excelReader = new ExcelReader();
    }

    @AfterMethod
    public void tearDown() {
        excelReader = null;
    }

    @Test
    public void testReadExcelByHeader() {
        DictionaryValue result = excelReader.action(
                SAMPLE_EXCEL_PATH,
                SELECT_BY_NAME,    // select sheet by name
                SHEET_NAME_1,      // sheet name
                null,              // sheet index (not used when selecting by name)
                false,
                null,              // no password needed for this test
                COLUMN_HEADER,
                null,              // not using index for header parsing method
                null,              // not using index for header parsing method
                COLUMN_NAME,
                COLUMN_AGE,
                false              // assuming we do not want to trim values for this test
        );

        Assert.assertNotNull(result, "The result should not be null.");
        Assert.assertEquals(result.get().size(), 5, "Dictionary should be same size as non empty rows");
    }

    @Test
    public void testReadExcelByIndex() {
        DictionaryValue result = excelReader.action(
                SAMPLE_EXCEL_PATH,
                SELECT_BY_NAME,    // select sheet by name
                SHEET_NAME_1,      // sheet name
                null,              // sheet index (not used when selecting by name)
                false,
                null,              // no password needed for this test
                COLUMN_INDEX,
                (double) INDEX_NAME,
                (double) INDEX_AGE,
                null,              // header name not needed for index parsing method
                null,              // header name not needed for index parsing method
                false              // assuming we do not want to trim values for this test
        );

        Assert.assertNotNull(result, "The result should not be null.");
        Assert.assertEquals(result.get().size(), 6, "Dictionary should be same size as non empty rows");
    }

    @Test
    public void testReadExcelByIndexDifferentSheet() {
        DictionaryValue result = excelReader.action(
                SAMPLE_EXCEL_PATH,
                SELECT_BY_NAME,    // select sheet by name
                SHEET_NAME_2,      // sheet name
                null,              // sheet index (not used when selecting by name)
                false,
                null,              // no password needed for this test
                COLUMN_INDEX,
                (double) INDEX_NAME,
                (double) INDEX_AGE,
                null,              // header name not needed for index parsing method
                null,              // header name not needed for index parsing method
                false              // assuming we do not want to trim values for this test
        );

        Assert.assertNotNull(result, "The result should not be null.");
        Assert.assertEquals(result.get().size(), 5, "Dictionary should be same size as non empty rows");
    }

    @Test
    public void testReadExcelBySheetIndex() {
        DictionaryValue result = excelReader.action(
                SAMPLE_EXCEL_PATH,
                SELECT_BY_INDEX,    // select sheet by index
                null,               // sheet name (not used when selecting by index)
                (double) SHEET_INDEX_0, // sheet index for Sheet1
                false,
                null,               // no password needed for this test
                COLUMN_INDEX,
                (double) INDEX_NAME,
                (double) INDEX_AGE,
                null,               // header name not needed for index parsing method
                null,               // header name not needed for index parsing method
                false               // assuming we do not want to trim values for this test
        );

        Assert.assertNotNull(result, "The result should not be null.");
        Assert.assertEquals(result.get().size(), 6, "Dictionary should be same size as non empty rows");
    }

    @Test
    public void testReadExcelBySheetIndexDifferentSheet() {
        DictionaryValue result = excelReader.action(
                SAMPLE_EXCEL_PATH,
                SELECT_BY_INDEX,    // select sheet by index
                null,               // sheet name (not used when selecting by index)
                (double) SHEET_INDEX_1, // sheet index for Sheet2
                false,
                null,               // no password needed for this test
                COLUMN_INDEX,
                (double) INDEX_NAME,
                (double) INDEX_AGE,
                null,               // header name not needed for index parsing method
                null,               // header name not needed for index parsing method
                false               // assuming we do not want to trim values for this test
        );

        Assert.assertNotNull(result, "The result should not be null.");
        Assert.assertEquals(result.get().size(), 5, "Dictionary should be same size as non empty rows");
    }

    @Test
    public void testReadExcelBySheetIndexWithHeaderParsing() {
        DictionaryValue result = excelReader.action(
                SAMPLE_EXCEL_PATH,
                SELECT_BY_INDEX,    // select sheet by index
                null,               // sheet name (not used when selecting by index)
                (double) SHEET_INDEX_0, // sheet index for Sheet1
                false,
                null,               // no password needed for this test
                COLUMN_HEADER,
                null,               // not using index for header parsing method
                null,               // not using index for header parsing method
                COLUMN_NAME,
                COLUMN_AGE,
                false               // assuming we do not want to trim values for this test
        );

        Assert.assertNotNull(result, "The result should not be null.");
        Assert.assertEquals(result.get().size(), 5, "Dictionary should be same size as non empty rows");
    }

    @Test(expectedExceptions = BotCommandException.class)
    public void testReadExcelWithInvalidPath() {
        excelReader.action(
                "invalid/path/to/excel.xlsx",
                SELECT_BY_NAME,     // select sheet by name
                SHEET_NAME_1,       // sheet name
                null,               // sheet index (not used when selecting by name)
                false,
                null,
                COLUMN_HEADER,
                null,
                null,
                COLUMN_NAME,
                COLUMN_AGE,
                false
        );
    }

    @Test(expectedExceptions = BotCommandException.class)
    public void testReadExcelWithInvalidSheetName() {
        excelReader.action(
                SAMPLE_EXCEL_PATH,
                SELECT_BY_NAME,     // select sheet by name
                "NonExistentSheet", // invalid sheet name
                null,               // sheet index (not used when selecting by name)
                false,
                null,
                COLUMN_HEADER,
                null,
                null,
                COLUMN_NAME,
                COLUMN_AGE,
                false
        );
    }

    @Test(expectedExceptions = BotCommandException.class)
    public void testReadExcelWithInvalidSheetIndex() {
        excelReader.action(
                SAMPLE_EXCEL_PATH,
                SELECT_BY_INDEX,    // select sheet by index
                null,               // sheet name (not used when selecting by index)
                99.0,               // invalid sheet index
                false,
                null,
                COLUMN_HEADER,
                null,
                null,
                COLUMN_NAME,
                COLUMN_AGE,
                false
        );
    }
}