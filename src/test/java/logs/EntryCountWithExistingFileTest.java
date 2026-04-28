package logs;

import com.automationanywhere.botcommand.utilities.logger.CustomHTMLLayout;
import com.automationanywhere.botcommand.utilities.logger.CustomLogger;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests that entry counting works correctly across logger sessions when appending to existing files.
 * Verifies that rollover happens at the correct count when combining existing + new entries.
 *
 * @author Sumit Kumar
 */
public class EntryCountWithExistingFileTest {

    private String baseTestPath;

    @BeforeClass
    public void setup() throws IOException {
        // Create test directory in target folder with timestamp to avoid conflicts
        baseTestPath = "src/test/target/test-artifacts/existing-file-" + System.currentTimeMillis() + "/";
        Files.createDirectories(Paths.get(baseTestPath));
        System.out.println("Test log directory: " + baseTestPath);
    }

    @AfterClass
    public void cleanup() {
        // Log location for debugging if needed
        System.out.println("Entry count with existing file test logs available at: " + baseTestPath);
    }

    @Test(priority = 1, description = "Create initial log file with entries below limit")
    public void testCreateInitialLogFile() throws Exception {
        int maxEntries = 10;
        int entriesToWrite = 5; // Less than limit
        String logFilePath = baseTestPath + "session1.html";

        CustomLogger customLogger = new CustomLogger(
                "test-logger-session1",
                logFilePath,
                maxEntries
        );

        Logger logger = customLogger.getLogger();

        // Write entries
        for (int i = 1; i <= entriesToWrite; i++) {
            Map<String, Object> message = new HashMap<>();
            message.put(CustomHTMLLayout.Columns.MESSAGE, "Entry " + i + " from session 1");
            message.put(CustomHTMLLayout.Columns.SOURCE, "Test/Session1");
            message.put(CustomHTMLLayout.Columns.SCREENSHOT, "");
            message.put(CustomHTMLLayout.Columns.VARIABLES, null);
            logger.info(message);
        }

        customLogger.close();

        // Verify file exists and has correct entry count
        File logFile = new File(logFilePath);
        Assert.assertTrue(logFile.exists(), "Log file should exist");

        int entryCount = countLogEntries(logFilePath);
        Assert.assertEquals(entryCount, entriesToWrite, "Should have exactly " + entriesToWrite + " entries");

        // Verify footer is present
        Assert.assertTrue(hasFooter(logFilePath), "File should have HTML footer after close");

        System.out.println("✓ Session 1: Created log with " + entryCount + " entries");
    }

    @Test(priority = 2, description = "Append to existing file - should count existing entries")
    public void testAppendToExistingFile() throws Exception {
        int maxEntries = 10;
        int existingEntries = 5; // From previous test
        int newEntries = 3;
        String logFilePath = baseTestPath + "session1.html";

        // Verify file exists from previous test
        Assert.assertTrue(new File(logFilePath).exists(), "Log file from previous test should exist");

        CustomLogger customLogger = new CustomLogger(
                "test-logger-session2",
                logFilePath,
                maxEntries
        );

        Logger logger = customLogger.getLogger();

        // Write more entries
        for (int i = 1; i <= newEntries; i++) {
            Map<String, Object> message = new HashMap<>();
            message.put(CustomHTMLLayout.Columns.MESSAGE, "Entry " + i + " from session 2");
            message.put(CustomHTMLLayout.Columns.SOURCE, "Test/Session2");
            message.put(CustomHTMLLayout.Columns.SCREENSHOT, "");
            message.put(CustomHTMLLayout.Columns.VARIABLES, null);
            logger.info(message);
        }

        customLogger.close();

        // Verify total entry count
        int totalEntries = countLogEntries(logFilePath);
        Assert.assertEquals(totalEntries, existingEntries + newEntries,
                "Should have " + existingEntries + " existing + " + newEntries + " new = " + (existingEntries + newEntries) + " total entries");

        // Verify no duplicate headers
        int headerCount = countHeaders(logFilePath);
        Assert.assertEquals(headerCount, 1, "Should have exactly one header row");

        // Verify footer is present
        Assert.assertTrue(hasFooter(logFilePath), "File should have HTML footer after close");

        System.out.println("✓ Session 2: Appended " + newEntries + " entries, total now " + totalEntries);
    }

    @Test(priority = 3, description = "Rollover when existing + new entries exceed limit")
    public void testRolloverWithExistingEntries() throws Exception {
        int maxEntries = 10;
        int existingEntries = 8; // From previous tests (5 + 3)
        int newEntriesToTriggerRollover = 5; // 8 + 5 = 13 > 10, should rollover
        String logFilePath = baseTestPath + "session1.html";

        CustomLogger customLogger = new CustomLogger(
                "test-logger-session3",
                logFilePath,
                maxEntries
        );

        Logger logger = customLogger.getLogger();

        // Write entries that should trigger rollover
        for (int i = 1; i <= newEntriesToTriggerRollover; i++) {
            Map<String, Object> message = new HashMap<>();
            message.put(CustomHTMLLayout.Columns.MESSAGE, "Entry " + i + " from session 3");
            message.put(CustomHTMLLayout.Columns.SOURCE, "Test/Session3");
            message.put(CustomHTMLLayout.Columns.SCREENSHOT, "");
            message.put(CustomHTMLLayout.Columns.VARIABLES, null);
            logger.info(message);
        }

        customLogger.close();

        // Check if rollover occurred
        String baseDir = FilenameUtils.getFullPath(logFilePath);
        String baseName = FilenameUtils.getBaseName(logFilePath);
        String ext = FilenameUtils.getExtension(logFilePath);

        File originalFile = new File(logFilePath);
        File rolledFile = new File(baseDir + "1_" + baseName + "." + ext);

        boolean rolloverOccurred = rolledFile.exists();
        Assert.assertTrue(rolloverOccurred, "Rollover should have occurred when total entries exceeded " + maxEntries);

        if (rolloverOccurred) {
            // Verify rolled file has entries up to maxEntries
            int rolledFileEntries = countLogEntries(rolledFile.getAbsolutePath());
            Assert.assertTrue(rolledFileEntries <= maxEntries,
                    "Rolled file should have at most " + maxEntries + " entries, found " + rolledFileEntries);

            // Verify new file has remaining entries
            int newFileEntries = countLogEntries(logFilePath);
            int expectedNewFileEntries = (existingEntries + newEntriesToTriggerRollover) - rolledFileEntries;
            Assert.assertEquals(newFileEntries, expectedNewFileEntries,
                    "New file should have remaining entries");

            // Verify both files have footers
            Assert.assertTrue(hasFooter(rolledFile.getAbsolutePath()), "Rolled file should have footer");
            Assert.assertTrue(hasFooter(logFilePath), "New file should have footer");

            System.out.println("✓ Session 3: Rollover occurred correctly");
            System.out.println("  - Rolled file (1_session1.html): " + rolledFileEntries + " entries");
            System.out.println("  - New file (session1.html): " + newFileEntries + " entries");
        }
    }

    @Test(priority = 4, description = "Fresh start - no existing file")
    public void testFreshStart() throws Exception {
        int maxEntries = 5;
        int entriesToWrite = 3;
        String logFilePath = baseTestPath + "fresh_start.html";

        // Ensure file doesn't exist
        File logFile = new File(logFilePath);
        if (logFile.exists()) {
            logFile.delete();
        }

        CustomLogger customLogger = new CustomLogger(
                "test-logger-fresh",
                logFilePath,
                maxEntries
        );

        Logger logger = customLogger.getLogger();

        // Write entries
        for (int i = 1; i <= entriesToWrite; i++) {
            Map<String, Object> message = new HashMap<>();
            message.put(CustomHTMLLayout.Columns.MESSAGE, "Fresh entry " + i);
            message.put(CustomHTMLLayout.Columns.SOURCE, "Test/Fresh");
            message.put(CustomHTMLLayout.Columns.SCREENSHOT, "");
            message.put(CustomHTMLLayout.Columns.VARIABLES, null);
            logger.info(message);
        }

        customLogger.close();

        // Verify
        int entryCount = countLogEntries(logFilePath);
        Assert.assertEquals(entryCount, entriesToWrite, "Fresh file should have exactly " + entriesToWrite + " entries");

        Assert.assertTrue(hasFooter(logFilePath), "Fresh file should have footer");

        System.out.println("✓ Fresh start: Created log with " + entryCount + " entries");
    }

    @Test(priority = 5, description = "Multiple sessions reaching exact limit")
    public void testMultipleSessionsExactLimit() throws Exception {
        int maxEntries = 10;
        String logFilePath = baseTestPath + "exact_limit.html";

        // Ensure file doesn't exist
        File logFile = new File(logFilePath);
        if (logFile.exists()) {
            logFile.delete();
        }

        // Session 1: Write 4 entries
        writeEntries(logFilePath, maxEntries, 4, "Session1");
        Assert.assertEquals(countLogEntries(logFilePath), 4, "After session 1: should have 4 entries");

        // Session 2: Write 3 entries (total 7)
        writeEntries(logFilePath, maxEntries, 3, "Session2");
        Assert.assertEquals(countLogEntries(logFilePath), 7, "After session 2: should have 7 entries");

        // Session 3: Write 3 entries (total 10 - exactly at limit)
        writeEntries(logFilePath, maxEntries, 3, "Session3");
        Assert.assertEquals(countLogEntries(logFilePath), 10, "After session 3: should have 10 entries");

        // Session 4: Write 1 entry (total 11 - should trigger rollover)
        writeEntries(logFilePath, maxEntries, 1, "Session4");

        // Check rollover
        String baseDir = FilenameUtils.getFullPath(logFilePath);
        String baseName = FilenameUtils.getBaseName(logFilePath);
        String ext = FilenameUtils.getExtension(logFilePath);
        File rolledFile = new File(baseDir + "1_" + baseName + "." + ext);

        Assert.assertTrue(rolledFile.exists(), "Rollover should occur when exceeding limit by 1");

        System.out.println("✓ Multiple sessions: Rollover occurred at correct threshold");
    }

    @Test(priority = 6, description = "Rollover does not append next row after footer")
    public void testRolloverDoesNotAppendAfterFooter() throws Exception {
        int maxEntries = 13;
        String logFilePath = baseTestPath + "rollover_footer_boundary.html";

        CustomLogger customLogger = new CustomLogger(
                "test-logger-rollover-footer-boundary",
                logFilePath,
                maxEntries
        );

        Logger logger = customLogger.getLogger();
        for (int i = 1; i <= 14; i++) {
            Map<String, Object> message = new HashMap<>();
            message.put(CustomHTMLLayout.Columns.MESSAGE, "Boundary entry " + i);
            message.put(CustomHTMLLayout.Columns.SOURCE, "Test/Boundary");
            message.put(CustomHTMLLayout.Columns.SCREENSHOT, "");
            message.put(CustomHTMLLayout.Columns.VARIABLES, null);
            logger.info(message);
        }

        customLogger.close();

        String baseDir = FilenameUtils.getFullPath(logFilePath);
        String baseName = FilenameUtils.getBaseName(logFilePath);
        String ext = FilenameUtils.getExtension(logFilePath);
        File rolledFile = new File(baseDir + "1_" + baseName + "." + ext);

        assertNoRowsAfterFooter(logFilePath);
        assertNoRowsAfterFooter(rolledFile.getAbsolutePath());
        Assert.assertEquals(countLogEntries(rolledFile.getAbsolutePath()), maxEntries,
                "Rolled file should keep first " + maxEntries + " rows");
        Assert.assertEquals(countLogEntries(logFilePath), 1, "Active file should contain row after rollover");
    }

    @Test(priority = 7, description = "Repair existing log where a row was appended after footer")
    public void testAppendRepairsRowAfterFooter() throws Exception {
        int maxEntries = 10;
        String logFilePath = baseTestPath + "row_after_footer.html";
        String footer = "</tbody></table></body></html>";
        String orphanedRow = "<tr><td>Orphaned row</td><td>INFO</td></tr>";

        String malformedContent = "<!DOCTYPE html><html><body><table><tbody>"
                + "<tr><td>Existing row</td><td>INFO</td></tr>"
                + footer
                + orphanedRow
                + footer;

        Files.write(Paths.get(logFilePath), malformedContent.getBytes(StandardCharsets.UTF_8));

        CustomLogger customLogger = new CustomLogger(
                "test-logger-row-after-footer",
                logFilePath,
                maxEntries
        );

        Logger logger = customLogger.getLogger();
        Map<String, Object> message = new HashMap<>();
        message.put(CustomHTMLLayout.Columns.MESSAGE, "Entry after repair");
        message.put(CustomHTMLLayout.Columns.SOURCE, "Test/Repair");
        message.put(CustomHTMLLayout.Columns.SCREENSHOT, "");
        message.put(CustomHTMLLayout.Columns.VARIABLES, null);
        logger.info(message);
        customLogger.close();

        String content = new String(Files.readAllBytes(Paths.get(logFilePath)), StandardCharsets.UTF_8);
        String lowerContent = content.toLowerCase();

        Assert.assertFalse(lowerContent.contains("</html><tr>"), "No row should appear after closing html");
        Assert.assertTrue(content.contains(orphanedRow), "Existing orphaned row should be preserved");
        Assert.assertTrue(content.indexOf(orphanedRow) < lowerContent.indexOf("</tbody>"),
                "Recovered row should be inside tbody");
        Assert.assertEquals(countLogEntries(logFilePath), 3, "All existing and new rows should remain");
        Assert.assertEquals(countFooterOccurrences(content), 1, "Final log should have exactly one footer");
    }

    /**
     * Helper method to write entries to a log file
     */
    private void writeEntries(String logFilePath, int maxEntries, int count, String source) throws Exception {
        CustomLogger customLogger = new CustomLogger(
                "test-logger-" + source,
                logFilePath,
                maxEntries
        );

        Logger logger = customLogger.getLogger();

        for (int i = 1; i <= count; i++) {
            Map<String, Object> message = new HashMap<>();
            message.put(CustomHTMLLayout.Columns.MESSAGE, "Entry " + i + " from " + source);
            message.put(CustomHTMLLayout.Columns.SOURCE, "Test/" + source);
            message.put(CustomHTMLLayout.Columns.SCREENSHOT, "");
            message.put(CustomHTMLLayout.Columns.VARIABLES, null);
            logger.info(message);
        }

        customLogger.close();
    }

    /**
     * Counts log entries (data rows) in the HTML file by counting <tr><td> patterns
     */
    private int countLogEntries(String filePath) throws IOException {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String lowerLine = line.toLowerCase();
                int index = 0;
                while ((index = lowerLine.indexOf("<tr><td>", index)) != -1) {
                    count++;
                    index += 8;
                }
            }
        }
        return count;
    }

    /**
     * Counts header rows in the HTML file by counting <thead> tags
     */
    private int countHeaders(String filePath) throws IOException {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String lowerLine = line.toLowerCase();
                if (lowerLine.contains("<thead>")) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Checks if the HTML file has a proper footer
     */
    private boolean hasFooter(String filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String lowerLine = line.toLowerCase();
                if (lowerLine.contains("</tbody></table></body></html>")) {
                    return true;
                }
            }
        }
        return false;
    }

    private void assertNoRowsAfterFooter(String filePath) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
        Assert.assertFalse(content.toLowerCase().matches("(?s).*</html\\s*>\\s*<tr>.*"),
                "No row should appear after closing html in " + filePath);
        Assert.assertEquals(countFooterOccurrences(content), 1,
                "File should have exactly one footer: " + filePath);
    }

    private int countFooterOccurrences(String content) {
        String lowerContent = content.toLowerCase();
        String footer = "</tbody></table></body></html>";
        int count = 0;
        int index = 0;

        while ((index = lowerContent.indexOf(footer, index)) != -1) {
            count++;
            index += footer.length();
        }

        return count;
    }
}
