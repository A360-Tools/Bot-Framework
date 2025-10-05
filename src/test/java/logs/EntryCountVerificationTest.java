package logs;

import com.automationanywhere.botcommand.utilities.logger.CustomHTMLLayout;
import com.automationanywhere.botcommand.utilities.logger.CustomLogger;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.Test;
import org.testng.Assert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Test to verify exact entry count before rotation
 */
public class EntryCountVerificationTest {

    @Test
    public void testExactEntryCount() throws IOException {
        // Create test directory
        String testPath = "src/test/target/test-artifacts/entry-count-verify-" + System.currentTimeMillis() + "/";
        Files.createDirectories(Paths.get(testPath));

        String logFilePath = testPath + "exact-count.html";
        int maxEntries = 5;

        CustomLogger customLogger = new CustomLogger("ExactCountLogger_" + UUID.randomUUID(),
                                                      logFilePath, maxEntries);
        Logger logger = customLogger.getLogger();

        // Log exactly maxEntries
        for (int i = 1; i <= maxEntries; i++) {
            Map<String, Object> message = new HashMap<>();
            message.put(CustomHTMLLayout.Columns.MESSAGE, "Entry " + i + " of " + maxEntries);
            message.put(CustomHTMLLayout.Columns.SOURCE, "Test/ExactCount");
            message.put(CustomHTMLLayout.Columns.SCREENSHOT, "");
            message.put(CustomHTMLLayout.Columns.VARIABLES, null);
            logger.info(message);
        }

        // At this point, file should NOT have rotated yet
        File originalFile = new File(logFilePath);
        // New pattern: 1_basename.html
        String baseName = FilenameUtils.getBaseName(logFilePath);
        String ext = FilenameUtils.getExtension(logFilePath);
        String rotatedFilePath = testPath + "1_" + baseName + "." + ext;
        File rotatedFile = new File(rotatedFilePath);

        Assert.assertTrue(originalFile.exists(), "Original file should exist");
        Assert.assertFalse(rotatedFile.exists(),
            "Rotation should NOT occur yet - we've logged exactly " + maxEntries + " entries");

        // Log one more entry to trigger rotation
        Map<String, Object> triggerMessage = new HashMap<>();
        triggerMessage.put(CustomHTMLLayout.Columns.MESSAGE, "Entry " + (maxEntries + 1) + " - This triggers rotation");
        triggerMessage.put(CustomHTMLLayout.Columns.SOURCE, "Test/ExactCount");
        triggerMessage.put(CustomHTMLLayout.Columns.SCREENSHOT, "");
        triggerMessage.put(CustomHTMLLayout.Columns.VARIABLES, null);
        logger.info(triggerMessage);

        customLogger.close();

        // Now rotation should have occurred
        Assert.assertTrue(originalFile.exists(), "Original file should still exist");
        Assert.assertTrue(rotatedFile.exists(),
            "Rotation SHOULD occur after " + (maxEntries + 1) + " entries");

        // Verify the rotated file has exactly maxEntries entries
        String rotatedContent = new String(Files.readAllBytes(rotatedFile.toPath()));

        // Count the number of log entries in the rotated file
        int entryCount = 0;
        for (int i = 1; i <= maxEntries; i++) {
            if (rotatedContent.contains("Entry " + i + " of " + maxEntries)) {
                entryCount++;
            }
        }

        Assert.assertEquals(entryCount, maxEntries,
            "Rotated file should contain exactly " + maxEntries + " entries");

        // The new file should contain the trigger entry
        String currentContent = new String(Files.readAllBytes(originalFile.toPath()));
        Assert.assertTrue(currentContent.contains("This triggers rotation"),
            "Current file should contain the entry that triggered rotation");

        System.out.println("Entry count verification test completed. Logs at: " + testPath);
    }

    @Test
    public void testScreenshotPathInHTML() throws IOException {
        // Create test directory
        String testPath = "src/test/target/test-artifacts/screenshot-path-" + System.currentTimeMillis() + "/";
        Files.createDirectories(Paths.get(testPath));

        String logFilePath = testPath + "screenshot-test.html";
        CustomLogger customLogger = new CustomLogger("ScreenshotLogger_" + UUID.randomUUID(),
                                                      logFilePath, 10);
        Logger logger = customLogger.getLogger();

        String screenshotsDir = testPath + "screenshots";

        // Log entry with screenshot path
        Map<String, Object> message = new HashMap<>();
        message.put(CustomHTMLLayout.Columns.MESSAGE, "Test message with screenshot");
        message.put(CustomHTMLLayout.Columns.SOURCE, "Test/Screenshot");
        message.put(CustomHTMLLayout.Columns.SCREENSHOT, screenshotsDir + "/test_image.png");
        message.put(CustomHTMLLayout.Columns.VARIABLES, null);
        logger.info(message);

        customLogger.close();

        // Check the HTML content
        String htmlContent = new String(Files.readAllBytes(Paths.get(logFilePath)));

        // The HTMLGenerator creates relative paths like "screenshots/filename.png"
        Assert.assertTrue(htmlContent.contains("screenshots/test_image.png") ||
                         htmlContent.contains("View Screenshot"),
            "HTML should contain screenshot reference");

        System.out.println("Screenshot path test completed. Log at: " + logFilePath);
    }
}