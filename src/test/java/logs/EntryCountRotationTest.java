package logs;

import com.automationanywhere.botcommand.actions.logs.LogMessage;
import com.automationanywhere.botcommand.actions.logs.StartLoggerSession;
import com.automationanywhere.botcommand.actions.logs.StopLoggerSession;
import com.automationanywhere.botcommand.data.Value;
import com.automationanywhere.botcommand.data.impl.*;
import com.automationanywhere.botcommand.data.model.Schema;
import com.automationanywhere.botcommand.data.model.table.Row;
import com.automationanywhere.botcommand.data.model.table.Table;
import com.automationanywhere.botcommand.utilities.logger.CustomHTMLLayout;
import com.automationanywhere.botcommand.utilities.logger.CustomLogger;
import com.automationanywhere.botcommand.utilities.logger.EntryCountBasedTriggeringPolicy;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.Assert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Test class to verify entry-count based log rotation
 */
public class EntryCountRotationTest {

    private static final String LEVEL_INFO = "INFO";
    private static final String LEVEL_ERROR = "ERROR";
    private static final String LEVEL_WARN = "WARN";
    private static final String LOG_VARIABLE = "YES";
    private static final String DO_NOT_LOG_VARIABLE = "NO";

    private String baseTestPath;
    private Map<String, Value> sourceMap;
    private List<Value> entryList;
    private LogMessage logMessage;
    private StopLoggerSession stopLoggerSession;

    @BeforeClass
    public void setUp() throws IOException {
        // Create test directory in target folder with timestamp to avoid conflicts
        baseTestPath = "src/test/target/test-artifacts/entry-rotation-" + System.currentTimeMillis() + "/";
        Files.createDirectories(Paths.get(baseTestPath));

        // Initialize LogMessage action
        logMessage = new LogMessage();
        stopLoggerSession = new StopLoggerSession();
        logMessage.setTestBotUri("Automation Anywhere/bots/test/entry-rotation-bot");

        // Setup test variables similar to LoggerTest
        setupTestVariables();
    }

    private void setupTestVariables() {
        // Create source map for common variables
        sourceMap = new HashMap<>();
        sourceMap.put("stringVar", new StringValue("Entry-based rotation test"));
        sourceMap.put("numberVar", new NumberValue(42));
        sourceMap.put("booleanVar", new BooleanValue(true));
        sourceMap.put("dateVar", new DateTimeValue(ZonedDateTime.now()));

        // Create entry list for more complex variables
        entryList = new ArrayList<>();

        // String variable entry
        Map<String, Value> variable1 = new HashMap<>();
        variable1.put("NAME", new StringValue("Test String"));
        variable1.put("VALUE", new StringValue("Entry rotation variable"));
        entryList.add(new DictionaryValue(variable1));

        // Number variable entry
        Map<String, Value> variable2 = new HashMap<>();
        variable2.put("NAME", new StringValue("Entry Count"));
        variable2.put("VALUE", new NumberValue(5));
        entryList.add(new DictionaryValue(variable2));

        // List variable with nested values
        Map<String, Value> variable3 = new HashMap<>();
        List<Value> listValues = new ArrayList<>();
        listValues.add(new StringValue("First"));
        listValues.add(new StringValue("Second"));
        listValues.add(new NumberValue(100));
        ListValue listValue = new ListValue();
        listValue.set(listValues);
        variable3.put("NAME", new StringValue("List Data"));
        variable3.put("VALUE", listValue);
        entryList.add(new DictionaryValue(variable3));

        // Table variable
        Map<String, Value> variable4 = new HashMap<>();
        List<Schema> schemas = new ArrayList<>();
        schemas.add(new Schema("ID"));
        schemas.add(new Schema("Type"));
        schemas.add(new Schema("Status"));

        List<Row> rows = new ArrayList<>();
        rows.add(new Row(new StringValue("1"), new StringValue("Entry"), new StringValue("Active")));
        rows.add(new Row(new StringValue("2"), new StringValue("Size"), new StringValue("Deprecated")));

        TableValue tableValue = new TableValue();
        tableValue.set(new Table(schemas, rows));
        variable4.put("NAME", new StringValue("Rotation Types"));
        variable4.put("VALUE", tableValue);
        entryList.add(new DictionaryValue(variable4));
    }

    @AfterClass
    public void tearDown() {
        // Log location for debugging if needed
        System.out.println("Entry rotation test logs available at: " + baseTestPath);
    }

    @Test
    public void testEntryBasedRotation() throws Exception {
        // Use the target directory for test logs
        String logFilePath = baseTestPath + "rotation-test.html";

        // Create logger with a small entry limit for testing
        int maxEntries = 5;
        CustomLogger customLogger = new CustomLogger("TestLogger_" + UUID.randomUUID(),
                                                      logFilePath, maxEntries);

        // Verify screenshots and variables directories are created
        String screenshotsDir = baseTestPath + "screenshots";
        String variablesDir = baseTestPath + "variables";
        Assert.assertTrue(Files.exists(Paths.get(screenshotsDir)), "Screenshots directory should be created");
        Assert.assertTrue(Files.exists(Paths.get(variablesDir)), "Variables directory should be created");

        // Log more entries than the limit to trigger rotation
        for (int i = 1; i <= 10; i++) {
            // Capture screenshot on even entries
            boolean captureScreenshot = (i % 2 == 0);

            // Log variables on every 3rd entry
            String logVariables = (i % 3 == 0) ? LOG_VARIABLE : DO_NOT_LOG_VARIABLE;

            // Use LogMessage.action() to properly test all features
            logMessage.action(
                customLogger,
                LEVEL_INFO,
                "Test log entry " + i + " with entry-based rotation",
                captureScreenshot,
                logVariables,
                entryList,
                sourceMap
            );
        }

        // Close the logger
        stopLoggerSession.stop(customLogger);

        // Check if rotation occurred - should have created backup file(s)
        File originalFile = new File(logFilePath);

        // Check for rotated files with pattern: 1_log.html, 2_log.html, etc.
        String baseName = FilenameUtils.getBaseName(logFilePath);
        String ext = FilenameUtils.getExtension(logFilePath);
        File rotatedFile = new File(baseTestPath + "1_" + baseName + "." + ext);

        Assert.assertTrue(originalFile.exists(), "Original log file should exist");
        Assert.assertTrue(rotatedFile.exists(), "First rotated log file (1_" + baseName + "." + ext + ") should exist");
    }

    @Test
    public void testMultipleLevelRotation() throws Exception {
        // Use subdirectory in target for multi-level test
        String multiLevelPath = baseTestPath + "multi-level/";
        Files.createDirectories(Paths.get(multiLevelPath));

        Map<Level, String> levelFilePathMap = new HashMap<>();
        levelFilePathMap.put(Level.INFO, multiLevelPath + "info.html");
        levelFilePathMap.put(Level.WARN, multiLevelPath + "warn.html");
        levelFilePathMap.put(Level.ERROR, multiLevelPath + "error.html");

        // Create logger with entry-based rotation
        int maxEntries = 3;
        CustomLogger customLogger = new CustomLogger("TestLogger_" + UUID.randomUUID(),
                                                      levelFilePathMap, maxEntries);

        // Verify directories are created
        String screenshotsDir = multiLevelPath + "screenshots";
        String variablesDir = multiLevelPath + "variables";
        Assert.assertTrue(Files.exists(Paths.get(screenshotsDir)), "Screenshots directory should be created for multi-level");
        Assert.assertTrue(Files.exists(Paths.get(variablesDir)), "Variables directory should be created for multi-level");

        // Log to different levels with proper LogMessage.action()
        for (int i = 1; i <= 5; i++) {
            // INFO message - screenshot on iteration 2, variables on iteration 3
            logMessage.action(
                customLogger,
                LEVEL_INFO,
                "INFO: Application started with entry-based rotation - iteration " + i,
                i == 2,  // capture screenshot on iteration 2
                i == 3 ? LOG_VARIABLE : DO_NOT_LOG_VARIABLE,
                entryList,
                sourceMap
            );

            // WARN message - always with screenshot, no variables
            logMessage.action(
                customLogger,
                LEVEL_WARN,
                "WARN: Memory usage above threshold - iteration " + i,
                true,  // always capture screenshot for warnings
                DO_NOT_LOG_VARIABLE,
                null,
                null
            );

            // ERROR message - screenshot on even iterations, variables on iteration 4
            logMessage.action(
                customLogger,
                LEVEL_ERROR,
                "ERROR: Failed to process request - iteration " + i,
                i % 2 == 0,  // screenshot on even iterations
                i == 4 ? LOG_VARIABLE : DO_NOT_LOG_VARIABLE,
                entryList,
                sourceMap
            );
        }

        // Close the logger
        stopLoggerSession.stop(customLogger);

        // Verify rotation for each level
        for (Level level : levelFilePathMap.keySet()) {
            String filePath = levelFilePathMap.get(level);
            File originalFile = new File(filePath);

            // Check for rotated files with pattern: 1_info.html, 2_info.html, etc.
            String baseName = FilenameUtils.getBaseName(filePath);
            String ext = FilenameUtils.getExtension(filePath);
            File rotatedFile = new File(multiLevelPath + "1_" + baseName + "." + ext);

            Assert.assertTrue(originalFile.exists(),
                            level + " log file should exist");
            Assert.assertTrue(rotatedFile.exists(),
                            level + " should have at least one rotated file (1_" + baseName + "." + ext + ")");
        }
    }

    @Test
    public void testRotationWithRichContent() throws Exception {
        // Test that rotation preserves screenshots and variables references correctly
        String richContentPath = baseTestPath + "rich-content/";
        Files.createDirectories(Paths.get(richContentPath));

        String logFilePath = richContentPath + "rich.html";

        // Very small limit to ensure rotation happens multiple times
        int maxEntries = 2;
        CustomLogger customLogger = new CustomLogger("RichLogger_" + UUID.randomUUID(),
                                                      logFilePath, maxEntries);

        // Verify directories
        String screenshotsDir = richContentPath + "screenshots";
        String variablesDir = richContentPath + "variables";
        Assert.assertTrue(Files.exists(Paths.get(screenshotsDir)), "Screenshots directory should exist");
        Assert.assertTrue(Files.exists(Paths.get(variablesDir)), "Variables directory should exist");

        // Log entries with rich content - all with screenshots and variables
        for (int i = 1; i <= 6; i++) {
            logMessage.action(
                customLogger,
                LEVEL_INFO,
                "Rich content entry " + i + " - Testing rotation with screenshots and variables",
                true,  // always capture screenshot
                LOG_VARIABLE,  // always log variables
                entryList,
                sourceMap
            );
        }

        stopLoggerSession.stop(customLogger);

        // Verify multiple rotation files were created
        File originalFile = new File(logFilePath);

        String baseName = FilenameUtils.getBaseName(logFilePath);
        String ext = FilenameUtils.getExtension(logFilePath);
        File rotation1 = new File(richContentPath + "1_" + baseName + "." + ext);
        File rotation2 = new File(richContentPath + "2_" + baseName + "." + ext);

        Assert.assertTrue(originalFile.exists(), "Original file should exist");
        Assert.assertTrue(rotation1.exists(), "First rotation file (1_" + baseName + "." + ext + ") should exist");
        Assert.assertTrue(rotation2.exists(), "Second rotation file (2_" + baseName + "." + ext + ") should exist");

        // Verify HTML content structure is maintained
        String originalContent = new String(Files.readAllBytes(originalFile.toPath()));
        Assert.assertTrue(originalContent.contains("View Screenshot") || originalContent.contains(".png"),
            "Screenshot references should be preserved");
        Assert.assertTrue(originalContent.contains("Variables") || originalContent.contains("vars-link"),
            "Variable references should be preserved");

        // Check that actual screenshot files were created
        File screenshotDirectory = new File(screenshotsDir);
        File[] screenshots = screenshotDirectory.listFiles((dir, name) -> name.endsWith(".png"));
        Assert.assertNotNull(screenshots, "Screenshot directory should contain files");
        Assert.assertTrue(screenshots.length > 0, "At least one screenshot should have been captured");

        // Check that variable files were created
        File variableDirectory = new File(variablesDir);
        File[] variableFiles = variableDirectory.listFiles((dir, name) -> name.endsWith(".html"));
        Assert.assertNotNull(variableFiles, "Variables directory should contain files");
        Assert.assertTrue(variableFiles.length > 0, "At least one variable file should have been created");
    }

    @Test
    public void testScreenshotAndVariableCapture() throws Exception {
        // Specific test to verify screenshots and variables are properly captured
        String testPath = baseTestPath + "capture-test/";
        Files.createDirectories(Paths.get(testPath));

        String logFilePath = testPath + "capture.html";
        CustomLogger customLogger = new CustomLogger("CaptureLogger_" + UUID.randomUUID(),
                                                      logFilePath, 10);

        // Log entry with screenshot and variables
        logMessage.action(
            customLogger,
            LEVEL_INFO,
            "Testing screenshot and variable capture",
            true,  // capture screenshot
            LOG_VARIABLE,  // log variables
            entryList,
            sourceMap
        );

        stopLoggerSession.stop(customLogger);

        // Verify screenshot was created
        String screenshotsDir = testPath + "screenshots";
        File screenshotDir = new File(screenshotsDir);
        File[] screenshots = screenshotDir.listFiles((dir, name) -> name.endsWith(".png"));
        Assert.assertNotNull(screenshots, "Screenshots should be created");
        Assert.assertEquals(screenshots.length, 1, "Exactly one screenshot should be created");

        // Verify the screenshot file exists and has content
        File screenshot = screenshots[0];
        Assert.assertTrue(screenshot.exists(), "Screenshot file should exist");
        Assert.assertTrue(screenshot.length() > 0, "Screenshot file should have content");

        // Verify variable file was created
        String variablesDir = testPath + "variables";
        File variableDir = new File(variablesDir);
        File[] variableFiles = variableDir.listFiles((dir, name) -> name.endsWith(".html"));
        Assert.assertNotNull(variableFiles, "Variable files should be created");
        Assert.assertTrue(variableFiles.length > 0, "At least one variable file should be created");

        // Verify HTML contains links to both
        String htmlContent = new String(Files.readAllBytes(Paths.get(logFilePath)));
        Assert.assertTrue(htmlContent.contains("View Screenshot"), "HTML should contain screenshot link");
        Assert.assertTrue(htmlContent.contains("vars-link"), "HTML should contain variable link");
    }

    @Test
    public void testEntryCountPolicyCreation() {
        // Test the EntryCountBasedTriggeringPolicy builder
        EntryCountBasedTriggeringPolicy.Builder builder = EntryCountBasedTriggeringPolicy.newBuilder();
        builder.withMaxEntries(100);

        EntryCountBasedTriggeringPolicy policy = builder.build();

        Assert.assertNotNull(policy, "Policy should be created successfully");
        Assert.assertEquals(policy.getCurrentEntryCount(), 0, "Initial count should be 0");
    }
}