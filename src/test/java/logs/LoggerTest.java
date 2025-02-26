package logs;

import com.automationanywhere.botcommand.actions.logs.LogMessage;
import com.automationanywhere.botcommand.actions.logs.StartLoggerSession;
import com.automationanywhere.botcommand.actions.logs.StopLoggerSession;
import com.automationanywhere.botcommand.data.Value;
import com.automationanywhere.botcommand.data.impl.*;
import com.automationanywhere.botcommand.data.model.Schema;
import com.automationanywhere.botcommand.data.model.table.Row;
import com.automationanywhere.botcommand.data.model.table.Table;
import com.automationanywhere.botcommand.utilities.logger.CustomLogger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Sumit Kumar
 */

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
    private StopLoggerSession stopLoggerSession;

    // Base paths for test artifacts
    private String baseTestPath;
    private String commonLogPath;
    private String infoLogPath;
    private String warnLogPath;
    private String errorLogPath;

    // Paths for multiple logger tests
    private String instance1Path;
    private String instance2Path;

    private Map<String, Value> sourceMap;
    private List<Value> entryList;

    @BeforeClass
    public void setUp() throws Exception {
        // Initialize test classes
        LoggerSession = new StartLoggerSession();
        logMessage = new LogMessage();
        stopLoggerSession = new StopLoggerSession();
        logMessage.setTestBotUri("Automation Anywhere/bots/path/to/test bot");

        // Create unique test directory with timestamp to avoid conflicts
        baseTestPath = "src/test/target/test-artifacts/logs-" + System.currentTimeMillis() + "/";

        // Ensure base directory exists
        Files.createDirectories(Paths.get(baseTestPath));

        // Set up file paths for testing
        commonLogPath = baseTestPath + "common_log.html";
        infoLogPath = baseTestPath + "info_log.html";
        warnLogPath = baseTestPath + "warn_log.html";
        errorLogPath = baseTestPath + "error_log.html";

        // Set up paths for multiple logger tests
        instance1Path = baseTestPath + "instance1.html";
        instance2Path = baseTestPath + "instance2.html";

        // Prepare test data
        setupTestData();
    }

    @AfterClass
    public void tearDown() {
        // Add any cleanup code if needed
        System.out.println("Tests completed. Log files available at: " + baseTestPath);
    }

    private void setupTestData() {
        // Create a source map with some variables representing variables sent via common variables
        sourceMap = new HashMap<>();
        sourceMap.put("my string", new StringValue("Hello, World!"));
        sourceMap.put("my number", new NumberValue(100));
        sourceMap.put("my boolean", new BooleanValue(true));
        sourceMap.put("my date", new DateTimeValue(ZonedDateTime.now()));

        // Create a list of dictionary values representing variables sent via entry list
        entryList = new ArrayList<>();

        // String variable
        Map<String, Value> variable1 = new HashMap<>();
        variable1.put("NAME", new StringValue("my string variable"));
        variable1.put("VALUE", new StringValue("my string variable value"));
        entryList.add(new DictionaryValue(variable1));

        // Number variable
        Map<String, Value> variable2 = new HashMap<>();
        variable2.put("NAME", new StringValue("my number variable"));
        variable2.put("VALUE", new NumberValue(456));
        entryList.add(new DictionaryValue(variable2));

        // List variable with nested dictionary
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

        // Table variable
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
    public void testCommonLoggerForAllLevels() throws Exception {
        // Test case: Common logger for all levels
        SessionValue sessionValue = LoggerSession.start(
                COMMON_FILE_ALL_LEVEL,
                commonLogPath,
                null, // These are not used for COMMON_FILE_ALL_LEVEL
                null,
                null,
                10
        );

        // Ensure the session was initialized properly
        Assert.assertNotNull(sessionValue);
        CustomLogger logger = (CustomLogger) sessionValue.getSession();
        Assert.assertNotNull(logger);

        // Verify screenshots directory is created
        String screenshotsDir = Paths.get(baseTestPath, "screenshots").toString();
        Assert.assertTrue(Files.exists(Paths.get(screenshotsDir)), "Screenshots directory should be created");

        // Log messages of different levels
        logMessage.action(logger, LEVEL_INFO, "Common logger: INFO message", true, LOG_VARIABLE, entryList, sourceMap);
        logMessage.action(logger, LEVEL_WARN, "Common logger: WARN message", true, LOG_VARIABLE, entryList, sourceMap);
        logMessage.action(logger, LEVEL_ERROR, "Common logger: ERROR message", true, LOG_VARIABLE, entryList, sourceMap);

        // Ensure log file is created
        Assert.assertTrue(Files.exists(Paths.get(commonLogPath)), "Log file should be created");

        // Read the log file content
        String content = new String(Files.readAllBytes(Paths.get(commonLogPath)));

        // Verify screenshots were created
        File screenshotDir = new File(screenshotsDir);
        Assert.assertTrue(screenshotDir.exists() && screenshotDir.isDirectory());
        Assert.assertTrue(screenshotDir.listFiles().length >= 3, "Should have at least 3 screenshots");

        // Check for log messages
        Assert.assertTrue(content.contains("Common logger: INFO message"), "Log should contain INFO message");
        Assert.assertTrue(content.contains("Common logger: WARN message"), "Log should contain WARN message");
        Assert.assertTrue(content.contains("Common logger: ERROR message"), "Log should contain ERROR message");

        // Check if relative screenshot paths with appropriate level prefixes are used in HTML
        Assert.assertTrue(content.contains("screenshots/info_"), "Log should reference INFO screenshots with relative paths");
        Assert.assertTrue(content.contains("screenshots/warn_"), "Log should reference WARN screenshots with relative paths");
        Assert.assertTrue(content.contains("screenshots/error_"), "Log should reference ERROR screenshots with relative paths");

        // Stop the logger
        stopLoggerSession.stop(logger);
    }

    @Test
    public void testSeparateLoggersForDifferentLevels() throws Exception {
        // Test case: Separate loggers for different levels
        SessionValue sessionValue = LoggerSession.start(
                CONFIGURABLE_FILE_ALL_LEVEL,
                null,
                infoLogPath,
                warnLogPath,
                errorLogPath,
                10
        );

        // Ensure the session was initialized properly
        Assert.assertNotNull(sessionValue);
        CustomLogger logger = (CustomLogger) sessionValue.getSession();
        Assert.assertNotNull(logger);

        // Verify screenshots directories are created for each log file location
        String infoScreenshotsDir = Paths.get(baseTestPath, "screenshots").toString();
        Assert.assertTrue(Files.exists(Paths.get(infoScreenshotsDir)), "Screenshots directory for INFO should be created");

        // Log messages of different levels
        logMessage.action(logger, LEVEL_INFO, "Separate loggers: INFO message", true, LOG_VARIABLE, entryList, sourceMap);
        logMessage.action(logger, LEVEL_WARN, "Separate loggers: WARN message", true, LOG_VARIABLE, entryList, sourceMap);
        logMessage.action(logger, LEVEL_ERROR, "Separate loggers: ERROR message", true, LOG_VARIABLE, entryList, sourceMap);

        // Ensure all log files are created
        Assert.assertTrue(Files.exists(Paths.get(infoLogPath)), "INFO log file should be created");
        Assert.assertTrue(Files.exists(Paths.get(warnLogPath)), "WARN log file should be created");
        Assert.assertTrue(Files.exists(Paths.get(errorLogPath)), "ERROR log file should be created");

        // Wait a moment for file writing to complete
        Thread.sleep(500);

        // Verify the INFO log file contains only INFO messages
        String infoContent = new String(Files.readAllBytes(Paths.get(infoLogPath)));
        Assert.assertTrue(infoContent.contains("Separate loggers: INFO message"), "INFO log should contain INFO message");
        Assert.assertFalse(infoContent.contains("Separate loggers: WARN message"), "INFO log should not contain WARN message");
        Assert.assertFalse(infoContent.contains("Separate loggers: ERROR message"), "INFO log should not contain ERROR message");

        // Verify the WARN log file contains only WARN messages
        String warnContent = new String(Files.readAllBytes(Paths.get(warnLogPath)));
        Assert.assertFalse(warnContent.contains("Separate loggers: INFO message"), "WARN log should not contain INFO message");
        Assert.assertTrue(warnContent.contains("Separate loggers: WARN message"), "WARN log should contain WARN message");
        Assert.assertFalse(warnContent.contains("Separate loggers: ERROR message"), "WARN log should not contain ERROR message");

        // Verify the ERROR log file contains only ERROR messages
        String errorContent = new String(Files.readAllBytes(Paths.get(errorLogPath)));
        Assert.assertFalse(errorContent.contains("Separate loggers: INFO message"), "ERROR log should not contain INFO message");
        Assert.assertFalse(errorContent.contains("Separate loggers: WARN message"), "ERROR log should not contain WARN message");
        Assert.assertTrue(errorContent.contains("Separate loggers: ERROR message"), "ERROR log should contain ERROR message");

        // Stop the logger
        stopLoggerSession.stop(logger);
    }

    @Test
    public void testMultipleLoggerInstances() throws Exception {
        // Create two separate logger instances
        SessionValue session1 = LoggerSession.start(COMMON_FILE_ALL_LEVEL, instance1Path, null, null, null, 10);
        SessionValue session2 = LoggerSession.start(COMMON_FILE_ALL_LEVEL, instance2Path, null, null, null, 10);

        // Ensure both sessions were initialized properly
        Assert.assertNotNull(session1);
        Assert.assertNotNull(session2);
        CustomLogger logger1 = (CustomLogger) session1.getSession();
        CustomLogger logger2 = (CustomLogger) session2.getSession();
        Assert.assertNotNull(logger1);
        Assert.assertNotNull(logger2);

        // Log to the first logger instance
        logMessage.action(logger1, LEVEL_INFO, "Logger 1: First message", true, LOG_VARIABLE, entryList, sourceMap);
        logMessage.action(logger1, LEVEL_WARN, "Logger 1: Second message", true, LOG_VARIABLE, entryList, sourceMap);

        // Log to the second logger instance
        logMessage.action(logger2, LEVEL_INFO, "Logger 2: First message", true, LOG_VARIABLE, entryList, sourceMap);
        logMessage.action(logger2, LEVEL_ERROR, "Logger 2: Second message", true, LOG_VARIABLE, entryList, sourceMap);

        // Wait a moment for file writing to complete
        Thread.sleep(500);

        // Ensure log files are created
        Assert.assertTrue(Files.exists(Paths.get(instance1Path)), "Logger 1 file should be created");
        Assert.assertTrue(Files.exists(Paths.get(instance2Path)), "Logger 2 file should be created");

        // Read file contents
        String content1 = new String(Files.readAllBytes(Paths.get(instance1Path)));
        String content2 = new String(Files.readAllBytes(Paths.get(instance2Path)));

        // Print the file contents for debugging
        System.out.println("Logger 1 content:\n" + content1);
        System.out.println("Logger 2 content:\n" + content2);

        // Verify content of the first logger's file
        Assert.assertTrue(content1.contains("Logger 1: First message"), "Logger 1 should contain its first message");
        Assert.assertTrue(content1.contains("Logger 1: Second message"), "Logger 1 should contain its second message");
        Assert.assertFalse(content1.contains("Logger 2: First message"), "Logger 1 should not contain Logger 2's messages");
        Assert.assertFalse(content1.contains("Logger 2: Second message"), "Logger 1 should not contain Logger 2's messages");

        // Verify content of the second logger's file
        Assert.assertTrue(content2.contains("Logger 2: First message"), "Logger 2 should contain its first message");
        Assert.assertTrue(content2.contains("Logger 2: Second message"), "Logger 2 should contain its second message");
        Assert.assertFalse(content2.contains("Logger 1: First message"), "Logger 2 should not contain Logger 1's messages");
        Assert.assertFalse(content2.contains("Logger 1: Second message"), "Logger 2 should not contain Logger 1's messages");

        // Stop both loggers
        stopLoggerSession.stop(logger1);
        stopLoggerSession.stop(logger2);
    }

    @Test
    public void testLogMessageWithoutScreenshot() throws Exception {
        // Test logging without capturing screenshots
        SessionValue sessionValue = LoggerSession.start(COMMON_FILE_ALL_LEVEL, commonLogPath, null, null, null, 10);

        CustomLogger logger = (CustomLogger) sessionValue.getSession();

        // Get initial count of screenshots
        String screenshotsDir = Paths.get(baseTestPath, "screenshots").toString();
        File screenshotDir = new File(screenshotsDir);
        int initialScreenshotCount = screenshotDir.exists() ? screenshotDir.listFiles().length : 0;

        // Log without taking screenshots
        logMessage.action(logger, LEVEL_INFO, "No screenshot: INFO message", false, LOG_VARIABLE, entryList, sourceMap);
        logMessage.action(logger, LEVEL_WARN, "No screenshot: WARN message", false, LOG_VARIABLE, entryList, sourceMap);
        logMessage.action(logger, LEVEL_ERROR, "No screenshot: ERROR message", false, LOG_VARIABLE, entryList, sourceMap);

        // Wait a moment for file writing to complete
        Thread.sleep(500);

        // Verify no new screenshots were created
        int newScreenshotCount = screenshotDir.exists() ? screenshotDir.listFiles().length : 0;
        Assert.assertEquals(newScreenshotCount, initialScreenshotCount, "No new screenshots should be created");

        // Verify log content contains the messages
        String content = new String(Files.readAllBytes(Paths.get(commonLogPath)));
        Assert.assertTrue(content.contains("No screenshot: INFO message"), "Log should contain INFO message");
        Assert.assertTrue(content.contains("No screenshot: WARN message"), "Log should contain WARN message");
        Assert.assertTrue(content.contains("No screenshot: ERROR message"), "Log should contain ERROR message");

        // Stop the logger
        stopLoggerSession.stop(logger);
    }

    @Test
    public void testLogVariableHandling() throws Exception {
        // Test variable logging capabilities
        SessionValue sessionValue = LoggerSession.start(COMMON_FILE_ALL_LEVEL, commonLogPath, null, null, null, 10);

        CustomLogger logger = (CustomLogger) sessionValue.getSession();

        // Test logging with variables
        logMessage.action(logger, LEVEL_INFO, "With variables", false, LOG_VARIABLE, entryList, sourceMap);

        // Test logging without variables
        logMessage.action(logger, LEVEL_INFO, "Without variables", false, DO_NOT_LOG_VARIABLE, entryList, sourceMap);

        // Wait a moment for file writing to complete
        Thread.sleep(500);

        // Verify log content
        String content = new String(Files.readAllBytes(Paths.get(commonLogPath)));
        Assert.assertTrue(content.contains("With variables"), "Log should contain the message with variables");
        Assert.assertTrue(content.contains("Without variables"), "Log should contain the message without variables");

        // When logging with variables, verify some of the variable information is in the HTML
        Assert.assertTrue(content.contains("my string variable"), "Log should contain variable name");
        Assert.assertTrue(content.contains("my string variable value"), "Log should contain variable value");
        Assert.assertTrue(content.contains("my number variable"), "Log should contain number variable name");

        // Stop the logger
        stopLoggerSession.stop(logger);
    }
}