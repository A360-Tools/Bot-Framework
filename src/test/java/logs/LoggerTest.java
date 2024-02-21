package logs;

/**
 * @author Sumit Kumar
 */

import com.automationanywhere.botcommand.actions.logs.LogMessage;
import com.automationanywhere.botcommand.actions.logs.StartLoggerSession;
import com.automationanywhere.botcommand.data.Value;
import com.automationanywhere.botcommand.data.impl.*;
import com.automationanywhere.botcommand.data.model.Schema;
import com.automationanywhere.botcommand.data.model.table.Row;
import com.automationanywhere.botcommand.data.model.table.Table;
import com.automationanywhere.botcommand.utilities.logger.CustomLogger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoggerTest {

    private static final String COMMON_FILE_ALL_LEVEL = "COMMON_FILE";
    private static final String CONFIGURABLE_FILE_ALL_LEVEL = "CONFIGURABLE_FILE";
    private static final String LEVEL_INFO = "INFO";
    private static final String LEVEL_ERROR = "ERROR";
    private static final String LEVEL_WARN = "WARN";
    private static final String LOG_VARIABLE = "YES";
    private static final String DO_NOT_LOG_VARIABLE = "NO";
    private StartLoggerSession LoggerSession;
    private LogMessage logMessage;
    private String logFilePath;
    private String screenshotFolderPath;
    private Map<String, Value> sourceMap;
    private List<Value> entryList;

    @BeforeClass
    public void setUp() {
        // Initialize your classes
        LoggerSession = new StartLoggerSession();
        logMessage = new LogMessage();
        logMessage.setTestBotUri("Automation Anywhere/bots/path/to/test bot");

        // Set up file paths for testing
        logFilePath = "src/test/target/test-artifacts/logs/log.html";
        screenshotFolderPath = "src/test/target/test-artifacts/logs/screenshot";
        // Create a source map with some variables representing variables sent via common variables(number,string,boolean,datetime)
        sourceMap = new HashMap<>();
        sourceMap.put("my string", new StringValue("Hello, World!"));
        sourceMap.put("my number", new NumberValue(100));
        sourceMap.put("my boolean", new BooleanValue(true));
        sourceMap.put("my date", new DateTimeValue(ZonedDateTime.now()));

        // Create a list of dictionary values representing variables sent via entry list
        entryList = new ArrayList<>();

        Map<String, Value> variable1 = new HashMap<>();
        variable1.put("NAME", new StringValue("my string variable"));
        variable1.put("VALUE", new StringValue("my string variable value"));
        entryList.add(new DictionaryValue(variable1));

        Map<String, Value> variable2 = new HashMap<>();
        variable2.put("NAME", new StringValue("my number variable"));
        variable2.put("VALUE", new NumberValue(456));
        entryList.add(new DictionaryValue(variable2));

        Map<String, Value> variable3 = new HashMap<>();
        List<Value> listValue = new ArrayList<>();
        Map<String, Value> dictValue = new HashMap<>();
        dictValue.put("my key", new StringValue("my value"));
        dictValue.put("my Date", new DateTimeValue(ZonedDateTime.now()));
        dictValue.put("my Number", new NumberValue(100));
        ListValue lv = new ListValue();
        listValue.add(new StringValue("Item 1"));
        listValue.add(new StringValue("Item 2"));
        listValue.add(new StringValue("Item 3"));
        listValue.add(new DictionaryValue(dictValue));
        lv.set(listValue);
        variable3.put("NAME", new StringValue("my list variable"));
        variable3.put("VALUE", lv);
        entryList.add(new DictionaryValue(variable3));

        Map<String, Value> variable4 = new HashMap<>();
        List<Schema> schemalist = new ArrayList<>();
        List<Row> rowList = new ArrayList<>();
        schemalist.add(new Schema("col1 "));
        schemalist.add(new Schema("col2"));
        schemalist.add(new Schema("col 3"));

        rowList.add(new Row(new StringValue("r1c1"), new StringValue("r1c2"), new StringValue("r1c3")));
        rowList.add(new Row(new StringValue("r2c1"), new StringValue("r2c2"), new StringValue("r2c3")));
        rowList.add(new Row(new StringValue("r3c1"), new StringValue("r3c2"), new StringValue("r3c3")));
        rowList.add(new Row(new StringValue("r4c1"), new StringValue("r4c2"), new StringValue("r4c3")));
        rowList.add(new Row(new StringValue("r5c1"), new StringValue("r5c2"), new StringValue("r5c3")));
        rowList.add(new Row(new StringValue("r6c1"), new StringValue("r6c2"),
                new StringValue("r63            longer value with space at end    ")));
        rowList.add(new Row(new StringValue("r7c1"), new StringValue("r7c2"), new StringValue("r7c3")));
        TableValue tv = new TableValue();
        tv.set(new Table(schemalist, rowList));

        variable4.put("NAME", new StringValue("my table variable"));
        variable4.put("VALUE", tv);
        entryList.add(new DictionaryValue(variable4));
    }

    @Test
    public void testLogging() throws Exception {
        // Initialize logger session with test-specific configurations
        SessionValue sessionValue = LoggerSession.start(COMMON_FILE_ALL_LEVEL, logFilePath,
                logFilePath, logFilePath, logFilePath, screenshotFolderPath, 10);
        // Ensure the session was initialized properly
        Assert.assertNotNull(sessionValue);

        // Log test message
        int i = 0;
        while (i < 10) {
            logMessage.action((CustomLogger) sessionValue.getSession(), LEVEL_INFO, "This is a test info message", true,
                    LOG_VARIABLE,
                    entryList, sourceMap);
            logMessage.action((CustomLogger) sessionValue.getSession(), LEVEL_WARN, "This is a test warn message", true,
                    LOG_VARIABLE,
                    entryList, sourceMap);
            logMessage.action((CustomLogger) sessionValue.getSession(), LEVEL_ERROR, "This is a test error message",
                    true,
                    LOG_VARIABLE,
                    entryList, sourceMap);
            ++i;
        }


        // Verify the log file contains the expected output
        String content = new String(Files.readAllBytes(Paths.get(logFilePath)));
        Assert.assertTrue(content.contains("This is a test info message"));
        Assert.assertTrue(content.contains("This is a test warn message"));
        Assert.assertTrue(content.contains("This is a test error message"));
    }
}
