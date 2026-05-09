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
import org.apache.commons.io.FileUtils;
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

    // Default log level constants
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

    // Paths for different folder location tests
    private String nestedFolderPath;
    private String differentDrivePath;
    private String absolutePathLog;

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

        // Create nested folder structure for different location tests
        nestedFolderPath = baseTestPath + "nested/subfolder/";
        Files.createDirectories(Paths.get(nestedFolderPath));

        // For cross-drive testing, use a relative path that simulates a different location
        // (In a real environment, this might be a different drive like "D:/logs/")
        differentDrivePath = baseTestPath + "different_drive/logs/";
        Files.createDirectories(Paths.get(differentDrivePath));

        // Set up an absolute path log file
        absolutePathLog = baseTestPath + "absolute/path/log.html";
        Files.createDirectories(Paths.get(baseTestPath + "absolute/path/"));

        // Prepare test data
        setupTestData();
    }

    private void setupTestData() {
        // Create a source map with some variables representing variables sent via common variables
        sourceMap = new HashMap<>();
        sourceMap.put("string", new StringValue("  Hello, World! with spaces at end and front  "));
        sourceMap.put("number", new NumberValue(100));
        sourceMap.put("boolean", new BooleanValue(true));
        sourceMap.put("date", new DateTimeValue(ZonedDateTime.now()));

        // Create a list of dictionary values representing variables sent via entry list
        entryList = new ArrayList<>();

        // String variable
        Map<String, Value> variable1 = new HashMap<>();
        variable1.put("NAME", new StringValue("entrylist string"));
        variable1.put("VALUE", new StringValue("my string variable value from entry list"));
        entryList.add(new DictionaryValue(variable1));

        // Number variable
        Map<String, Value> variable2 = new HashMap<>();
        variable2.put("NAME", new StringValue("entrylist number"));
        variable2.put("VALUE", new NumberValue(456));
        entryList.add(new DictionaryValue(variable2));

        // List variable with nested dictionary
        Map<String, Value> variable3 = new HashMap<>();
        List<Value> listValue = new ArrayList<>();
        Map<String, Value> dictValue = new HashMap<>();
        dictValue.put("entrylist string in dictionary", new StringValue("dict string value"));
        dictValue.put("entrylist Date in dictionary", new DateTimeValue(ZonedDateTime.now()));
        dictValue.put("entrylist Number in dictionary", new NumberValue(100));
        ListValue lv = new ListValue();
        listValue.add(new StringValue("Item 1"));
        listValue.add(new StringValue("Item 2"));
        listValue.add(new StringValue("Item 3"));
        listValue.add(new DictionaryValue(dictValue));
        lv.set(listValue);
        variable3.put("NAME", new StringValue("entrylist list"));
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

        variable4.put("NAME", new StringValue("entrylist table variable"));
        variable4.put("VALUE", tv);
        entryList.add(new DictionaryValue(variable4));
    }

    @AfterClass
    public void tearDown() {
        // Add any cleanup code if needed
        System.out.println("Tests completed. Log files available at: " + baseTestPath);
    }

    /**
     * Returns the file count of a directory, treating non-existent directories
     * and I/O failures (where {@code listFiles()} returns null) as zero.
     */
    private static int countFiles(File dir) {
        if (dir == null || !dir.exists()) {
            return 0;
        }
        File[] files = dir.listFiles();
        return files != null ? files.length : 0;
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

        // Verify variables directory is created
        String variablesDir = Paths.get(baseTestPath, "variables").toString();
        Assert.assertTrue(Files.exists(Paths.get(variablesDir)), "Variables directory should be created");

        // Log messages of different levels
        logMessage.action(logger, LEVEL_INFO, "Common logger: INFO message", true, LOG_VARIABLE, entryList, sourceMap);
        logMessage.action(logger, LEVEL_WARN, "Common logger: WARN message", true, LOG_VARIABLE, entryList, sourceMap);
        logMessage.action(logger, LEVEL_ERROR, "Common logger: ERROR message", true, LOG_VARIABLE, entryList,
                sourceMap);

        // Ensure log file is created
        Assert.assertTrue(Files.exists(Paths.get(commonLogPath)), "Log file should be created");

        // Read the log file content
        String content = new String(Files.readAllBytes(Paths.get(commonLogPath)));

        // Verify screenshots were created
        File screenshotDir = new File(screenshotsDir);
        Assert.assertTrue(screenshotDir.exists() && screenshotDir.isDirectory());
        Assert.assertTrue(countFiles(screenshotDir) >= 3, "Should have at least 3 screenshots");

        // Verify variable files were created
        File variablesDirectory = new File(variablesDir);
        Assert.assertTrue(variablesDirectory.exists() && variablesDirectory.isDirectory());
        Assert.assertTrue(countFiles(variablesDirectory) >= 3, "Should have at least 3 variable files");

        // Check for log messages
        Assert.assertTrue(content.contains("Common logger: INFO message"), "Log should contain INFO message");
        Assert.assertTrue(content.contains("Common logger: WARN message"), "Log should contain WARN message");
        Assert.assertTrue(content.contains("Common logger: ERROR message"), "Log should contain ERROR message");

        // Check if relative screenshot paths with appropriate level prefixes are used in HTML
        Assert.assertTrue(content.contains("screenshots/info_"), "Log should reference INFO screenshots with relative" +
                " paths");
        Assert.assertTrue(content.contains("screenshots/warn_"), "Log should reference WARN screenshots with relative" +
                " paths");
        Assert.assertTrue(content.contains("screenshots/error_"), "Log should reference ERROR screenshots with " +
                "relative paths");

        // Check if variable links are used in HTML instead of embedding content
        Assert.assertTrue(content.contains("variables/variables_"), "Log should contain links to variable HTML files");

        // Verify that the log file doesn't contain embedded variable data
        // Updated to check for patterns based on new HTMLGenerator implementation
        Assert.assertFalse(content.contains("<details class='full-width'>"),
                "Log should not contain embedded table details (should be in separate files)");

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

        // Verify screenshots and variables directories are created for each log file location
        String infoScreenshotsDir = Paths.get(baseTestPath, "screenshots").toString();
        Assert.assertTrue(Files.exists(Paths.get(infoScreenshotsDir)), "Screenshots directory for INFO should be " +
                "created");

        String infoVariablesDir = Paths.get(baseTestPath, "variables").toString();
        Assert.assertTrue(Files.exists(Paths.get(infoVariablesDir)), "Variables directory for INFO should be created");

        // Log messages of different levels
        logMessage.action(logger, LEVEL_INFO, "Separate loggers: INFO message", true, LOG_VARIABLE, entryList,
                sourceMap);
        logMessage.action(logger, LEVEL_WARN, "Separate loggers: WARN message", true, LOG_VARIABLE, entryList,
                sourceMap);
        logMessage.action(logger, LEVEL_ERROR, "Separate loggers: ERROR message", true, LOG_VARIABLE, entryList,
                sourceMap);

        // Ensure all log files are created
        Assert.assertTrue(Files.exists(Paths.get(infoLogPath)), "INFO log file should be created");
        Assert.assertTrue(Files.exists(Paths.get(warnLogPath)), "WARN log file should be created");
        Assert.assertTrue(Files.exists(Paths.get(errorLogPath)), "ERROR log file should be created");

        // Wait a moment for file writing to complete
        Thread.sleep(500);

        // Verify the INFO log file contains only INFO messages
        String infoContent = new String(Files.readAllBytes(Paths.get(infoLogPath)));
        Assert.assertTrue(infoContent.contains("Separate loggers: INFO message"), "INFO log should contain INFO " +
                "message");
        Assert.assertFalse(infoContent.contains("Separate loggers: WARN message"), "INFO log should not contain WARN " +
                "message");
        Assert.assertFalse(infoContent.contains("Separate loggers: ERROR message"), "INFO log should not contain " +
                "ERROR message");

        // Verify the WARN log file contains only WARN messages
        String warnContent = new String(Files.readAllBytes(Paths.get(warnLogPath)));
        Assert.assertFalse(warnContent.contains("Separate loggers: INFO message"), "WARN log should not contain INFO " +
                "message");
        Assert.assertTrue(warnContent.contains("Separate loggers: WARN message"), "WARN log should contain WARN " +
                "message");
        Assert.assertFalse(warnContent.contains("Separate loggers: ERROR message"), "WARN log should not contain " +
                "ERROR message");

        // Verify the ERROR log file contains only ERROR messages
        String errorContent = new String(Files.readAllBytes(Paths.get(errorLogPath)));
        Assert.assertFalse(errorContent.contains("Separate loggers: INFO message"), "ERROR log should not contain " +
                "INFO message");
        Assert.assertFalse(errorContent.contains("Separate loggers: WARN message"), "ERROR log should not contain " +
                "WARN message");
        Assert.assertTrue(errorContent.contains("Separate loggers: ERROR message"), "ERROR log should contain ERROR " +
                "message");

        // Verify variables links in each file
        Assert.assertTrue(infoContent.contains("variables/variables_"), "INFO log should contain links to variable " +
                "files");
        Assert.assertTrue(warnContent.contains("variables/variables_"), "WARN log should contain links to variable " +
                "files");
        Assert.assertTrue(errorContent.contains("variables/variables_"), "ERROR log should contain links to variable " +
                "files");

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

        // Verify content of the first logger's file
        Assert.assertTrue(content1.contains("Logger 1: First message"), "Logger 1 should contain its first message");
        Assert.assertTrue(content1.contains("Logger 1: Second message"), "Logger 1 should contain its second message");
        Assert.assertFalse(content1.contains("Logger 2: First message"), "Logger 1 should not contain Logger 2's " +
                "messages");
        Assert.assertFalse(content1.contains("Logger 2: Second message"), "Logger 1 should not contain Logger 2's " +
                "messages");

        // Verify content of the second logger's file
        Assert.assertTrue(content2.contains("Logger 2: First message"), "Logger 2 should contain its first message");
        Assert.assertTrue(content2.contains("Logger 2: Second message"), "Logger 2 should contain its second message");
        Assert.assertFalse(content2.contains("Logger 1: First message"), "Logger 2 should not contain Logger 1's " +
                "messages");
        Assert.assertFalse(content2.contains("Logger 1: Second message"), "Logger 2 should not contain Logger 1's " +
                "messages");

        // Verify variable links use the correct logger-specific base path
        Assert.assertTrue(content1.contains("variables/variables_"), "Logger 1 should contain links to its variables " +
                "directory");
        Assert.assertTrue(content2.contains("variables/variables_"), "Logger 2 should contain links to its variables " +
                "directory");

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
        int initialScreenshotCount = countFiles(screenshotDir);

        // Log without taking screenshots
        logMessage.action(logger, LEVEL_INFO, "No screenshot: INFO message", false, LOG_VARIABLE, entryList, sourceMap);
        logMessage.action(logger, LEVEL_WARN, "No screenshot: WARN message", false, LOG_VARIABLE, entryList, sourceMap);
        logMessage.action(logger, LEVEL_ERROR, "No screenshot: ERROR message", false, LOG_VARIABLE, entryList,
                sourceMap);

        // Wait a moment for file writing to complete
        Thread.sleep(500);

        // Verify no new screenshots were created
        int newScreenshotCount = countFiles(screenshotDir);
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
        // Verifies that LogMessage emits a variables-link in the log file when
        // logVariable=YES and omits it when logVariable=NO. Variable file
        // content rendering is covered by RenderingShowcaseTest.
        SessionValue sessionValue = LoggerSession.start(COMMON_FILE_ALL_LEVEL, commonLogPath, null, null, null, 10);

        CustomLogger logger = (CustomLogger) sessionValue.getSession();

        String variablesDir = Paths.get(baseTestPath, "variables").toString();
        File variablesDirectory = new File(variablesDir);
        int initialVariablesCount = countFiles(variablesDirectory);

        logMessage.action(logger, LEVEL_INFO, "With variables", false, LOG_VARIABLE, entryList, sourceMap);

        int afterVariablesCount = countFiles(variablesDirectory);
        Assert.assertTrue(afterVariablesCount > initialVariablesCount, "New variable files should be created");

        logMessage.action(logger, LEVEL_INFO, "Without variables", false, DO_NOT_LOG_VARIABLE, entryList, sourceMap);

        Thread.sleep(500);

        String content = new String(Files.readAllBytes(Paths.get(commonLogPath)));
        Assert.assertTrue(content.contains("With variables"), "Log should contain the message with variables");
        Assert.assertTrue(content.contains("Without variables"), "Log should contain the message without variables");
        Assert.assertTrue(content.contains("<a href='variables/variables_"),
                "Log should contain an anchor link to the per-event variables file");

        stopLoggerSession.stop(logger);
    }

    @Test
    public void testLogFileSize() throws Exception {
        // Test to verify that the log file size is reduced by using separate variable files

        // Create a session specifically for this test
        SessionValue sessionValue = LoggerSession.start(COMMON_FILE_ALL_LEVEL,
                baseTestPath + "size_test.html", null, null, null, 10);

        CustomLogger logger = (CustomLogger) sessionValue.getSession();

        // Log a message with complex variables
        logMessage.action(logger, LEVEL_INFO, "Log with complex variables", false, LOG_VARIABLE, entryList, sourceMap);

        // Wait for file writing to complete
        Thread.sleep(500);

        // Get the log file size
        long logFileSize = Files.size(Paths.get(baseTestPath + "size_test.html"));

        // Get the size of the variable file
        String variablesDir = Paths.get(baseTestPath, "variables").toString();
        File variablesDirectory = new File(variablesDir);
        File[] variableFiles =
                variablesDirectory.listFiles((dir, name) -> name.startsWith("variables_") && name.endsWith(".html"));

        if (variableFiles != null && variableFiles.length > 0) {
            long variableFileSize = Files.size(variableFiles[ 0 ].toPath());

            // Log the sizes for information
            System.out.println("Log file size: " + logFileSize + " bytes");
            System.out.println("Variable file size: " + variableFileSize + " bytes");

            // The log file should be significantly smaller than the variable file
            // since the variable data is now stored separately
            Assert.assertTrue(logFileSize < variableFileSize * 2,
                    "Log file should be smaller than would be needed to store all variable data");
        }

        // Stop the logger
        stopLoggerSession.stop(logger);
    }

    @Test
    public void testLoggerInNestedFolder() throws Exception {
        // Test logger in a nested subfolder location
        String nestedLogFile = nestedFolderPath + "nested_log.html";

        // Create the logger session
        SessionValue sessionValue = LoggerSession.start(
                COMMON_FILE_ALL_LEVEL,
                nestedLogFile,
                null,
                null,
                null,
                10
        );

        CustomLogger logger = (CustomLogger) sessionValue.getSession();
        Assert.assertNotNull(logger, "Logger should be initialized properly");

        // Log messages with screenshots and variables
        logMessage.action(logger, LEVEL_INFO, "Nested folder INFO message", true, LOG_VARIABLE, entryList, sourceMap);
        logMessage.action(logger, LEVEL_ERROR, "Nested folder ERROR message", true, LOG_VARIABLE, entryList, sourceMap);

        // Wait for file operations to complete
        Thread.sleep(500);

        // Verify log file was created
        Assert.assertTrue(Files.exists(Paths.get(nestedLogFile)), "Log file should be created in nested folder");

        // Check that screenshots and variables folders were created in the correct relative location
        Path screenshotsPath = Paths.get(nestedFolderPath, "screenshots");
        Path variablesPath = Paths.get(nestedFolderPath, "variables");

        Assert.assertTrue(Files.exists(screenshotsPath), "Screenshots folder should be created relative to log file");
        Assert.assertTrue(Files.exists(variablesPath), "Variables folder should be created relative to log file");

        // Check that screenshots and variable files were created
        File screenshotsDir = screenshotsPath.toFile();
        File variablesDir = variablesPath.toFile();

        Assert.assertTrue(countFiles(screenshotsDir) >= 2, "Should have at least 2 screenshots");
        Assert.assertTrue(countFiles(variablesDir) >= 2, "Should have at least 2 variable files");

        // Verify log content contains correct relative paths
        String logContent = new String(Files.readAllBytes(Paths.get(nestedLogFile)));

        // Paths should be relative to the log file location
        Assert.assertTrue(logContent.contains("screenshots/"), "Log should reference screenshots with relative paths");
        Assert.assertTrue(logContent.contains("variables/variables_"), "Log should contain links to variable HTML " +
                "files");

        // Make sure there are no absolute paths
        Assert.assertFalse(logContent.contains(baseTestPath), "Log should not contain absolute paths");

        // Stop the logger
        stopLoggerSession.stop(logger);
    }

    @Test
    public void testLoggerInDifferentDrive() throws Exception {
        // Test logger in a location that simulates a different drive
        String differentDriveLogFile = differentDrivePath + "different_drive_log.html";

        // Create the logger session
        SessionValue sessionValue = LoggerSession.start(
                COMMON_FILE_ALL_LEVEL,
                differentDriveLogFile,
                null,
                null,
                null,
                10
        );

        CustomLogger logger = (CustomLogger) sessionValue.getSession();
        Assert.assertNotNull(logger, "Logger should be initialized properly");

        // Log messages with screenshots and variables
        logMessage.action(logger, LEVEL_INFO, "Different drive INFO message", true, LOG_VARIABLE, entryList, sourceMap);
        logMessage.action(logger, LEVEL_WARN, "Different drive WARN message", true, DO_NOT_LOG_VARIABLE, null, null);

        // Wait for file operations to complete
        Thread.sleep(500);

        // Verify log file was created
        Assert.assertTrue(Files.exists(Paths.get(differentDriveLogFile)), "Log file should be created in different " +
                "drive folder");

        // Check that screenshots and variables folders were created in the correct relative location
        Path screenshotsPath = Paths.get(differentDrivePath, "screenshots");
        Path variablesPath = Paths.get(differentDrivePath, "variables");

        Assert.assertTrue(Files.exists(screenshotsPath), "Screenshots folder should be created relative to log file");
        Assert.assertTrue(Files.exists(variablesPath), "Variables folder should be created relative to log file");

        // Check that screenshots and variable files were created
        File screenshotsDir = screenshotsPath.toFile();
        File variablesDir = variablesPath.toFile();

        Assert.assertTrue(countFiles(screenshotsDir) >= 2, "Should have at least 2 screenshots");
        Assert.assertTrue(countFiles(variablesDir) >= 1, "Should have at least 1 variable file");

        // Verify log content contains correct relative paths
        String logContent = new String(Files.readAllBytes(Paths.get(differentDriveLogFile)));

        // Paths should be relative to the log file location
        Assert.assertTrue(logContent.contains("screenshots/"), "Log should reference screenshots with relative paths");
        Assert.assertTrue(logContent.contains("variables/variables_"), "Log should contain links to variable HTML " +
                "files");

        // Make sure there are no absolute paths
        Assert.assertFalse(logContent.contains(baseTestPath), "Log should not contain absolute paths");

        // Stop the logger
        stopLoggerSession.stop(logger);
    }

    @Test
    public void testLoggerWithAbsolutePath() throws Exception {
        // Test logger with an absolute path

        // Create the logger session
        SessionValue sessionValue = LoggerSession.start(
                COMMON_FILE_ALL_LEVEL,
                absolutePathLog,
                null,
                null,
                null,
                10
        );

        CustomLogger logger = (CustomLogger) sessionValue.getSession();
        Assert.assertNotNull(logger, "Logger should be initialized properly");

        // Log messages with screenshots and variables
        logMessage.action(logger, LEVEL_INFO, "Absolute path INFO message", true, LOG_VARIABLE, entryList, sourceMap);

        // Wait for file operations to complete
        Thread.sleep(500);

        // Verify log file was created
        Assert.assertTrue(Files.exists(Paths.get(absolutePathLog)), "Log file should be created at absolute path");

        // Check that screenshots and variables folders were created in the correct relative location
        Path logFolder = Paths.get(absolutePathLog).getParent();
        Path screenshotsPath = logFolder.resolve("screenshots");
        Path variablesPath = logFolder.resolve("variables");

        Assert.assertTrue(Files.exists(screenshotsPath), "Screenshots folder should be created relative to log file");
        Assert.assertTrue(Files.exists(variablesPath), "Variables folder should be created relative to log file");

        // Check that screenshots and variable files were created
        File screenshotsDir = screenshotsPath.toFile();
        File variablesDir = variablesPath.toFile();

        Assert.assertTrue(countFiles(screenshotsDir) >= 1, "Should have at least 1 screenshot");
        Assert.assertTrue(countFiles(variablesDir) >= 1, "Should have at least 1 variable file");

        // Verify log content contains correct relative paths
        String logContent = new String(Files.readAllBytes(Paths.get(absolutePathLog)));

        // Paths should be relative to the log file location
        Assert.assertTrue(logContent.contains("screenshots/"), "Log should reference screenshots with relative paths");
        Assert.assertTrue(logContent.contains("variables/variables_"), "Log should contain links to variable HTML " +
                "files");

        // Make sure there are no absolute paths in the HTML links
        Assert.assertFalse(logContent.contains(baseTestPath), "Log should not contain absolute paths");

        // Stop the logger
        stopLoggerSession.stop(logger);
    }

    @Test
    public void testLoggerMovedToNewLocation() throws Exception {
        // Test that a logger still works when the log file is moved to a new location
        String originalLogPath = baseTestPath + "original_location.html";
        String newLocationPath = baseTestPath + "moved/new_location.html";

        // Create directory for the new location
        Files.createDirectories(Paths.get(baseTestPath + "moved/"));

        // Create the logger session at the original location
        SessionValue sessionValue = LoggerSession.start(
                COMMON_FILE_ALL_LEVEL,
                originalLogPath,
                null,
                null,
                null,
                10
        );

        CustomLogger logger = (CustomLogger) sessionValue.getSession();
        Assert.assertNotNull(logger, "Logger should be initialized properly");

        // Log a message with screenshot and variables
        logMessage.action(logger, LEVEL_INFO, "Original location message", true, LOG_VARIABLE, entryList, sourceMap);

        // Wait for file operations to complete
        Thread.sleep(500);

        // Verify original folders were created
        String originalScreenshotsDir = Paths.get(baseTestPath, "screenshots").toString();
        String originalVariablesDir = Paths.get(baseTestPath, "variables").toString();

        Assert.assertTrue(Files.exists(Paths.get(originalScreenshotsDir)), "Original screenshots directory should " +
                "exist");
        Assert.assertTrue(Files.exists(Paths.get(originalVariablesDir)), "Original variables directory should exist");

        // Stop the logger
        stopLoggerSession.stop(logger);

        // Move the log file and create new directories for the moved file
        Files.createDirectories(Paths.get(baseTestPath + "moved/screenshots"));
        Files.createDirectories(Paths.get(baseTestPath + "moved/variables"));

        // Copy the log file to the new location
        FileUtils.copyFile(new File(originalLogPath), new File(newLocationPath));

        // Copy the screenshots and variables to the new location
        File[] screenshots = new File(originalScreenshotsDir).listFiles();
        if (screenshots != null) {
            for (File screenshot : screenshots) {
                FileUtils.copyFile(screenshot, new File(baseTestPath + "moved/screenshots/" + screenshot.getName()));
            }
        }

        File[] variables = new File(originalVariablesDir).listFiles();
        if (variables != null) {
            for (File variable : variables) {
                FileUtils.copyFile(variable, new File(baseTestPath + "moved/variables/" + variable.getName()));
            }
        }

        // Read the log file from the new location
        String movedLogContent = new String(Files.readAllBytes(Paths.get(newLocationPath)));

        // Verify that the log file at the new location can still resolve the relative paths
        Assert.assertTrue(movedLogContent.contains("screenshots/"), "Moved log should reference screenshots with " +
                "relative paths");
        Assert.assertTrue(movedLogContent.contains("variables/variables_"), "Moved log should contain links to " +
                "variable HTML files");

        // Since relative paths are used, the links should still work in the new location
        Assert.assertTrue(Files.exists(Paths.get(baseTestPath + "moved/screenshots")), "Screenshots should exist in " +
                "new location");
        Assert.assertTrue(Files.exists(Paths.get(baseTestPath + "moved/variables")), "Variables should exist in new " +
                "location");
    }
}