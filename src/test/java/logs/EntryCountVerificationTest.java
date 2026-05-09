package logs;

import com.automationanywhere.botcommand.actions.logs.LogMessage;
import com.automationanywhere.botcommand.actions.logs.StopLoggerSession;
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
    public void testScreenshotPathInHTML() throws Exception {
        // Drive the real production flow: LogMessage.action(captureScreenshot=true)
        // captures the desktop via CaptureScreen, writes the PNG, and emits the
        // screenshot column. The test asserts the captured file exists and the
        // generated HTML references it via the expected relative path.
        String testPath = "src/test/target/test-artifacts/screenshot-path-" + System.currentTimeMillis() + "/";
        Files.createDirectories(Paths.get(testPath));

        String logFilePath = testPath + "screenshot-test.html";
        CustomLogger customLogger = new CustomLogger("ScreenshotLogger_" + UUID.randomUUID(),
                                                      logFilePath, 10);

        LogMessage logMessage = new LogMessage();
        logMessage.setTestBotUri("Automation Anywhere/bots/test/screenshot-path-bot");

        logMessage.action(
            customLogger,
            "INFO",
            "Test message with screenshot",
            true,
            "NO",
            null,
            null
        );

        new StopLoggerSession().stop(customLogger);

        String screenshotsDir = testPath + "screenshots";
        File[] screenshots = new File(screenshotsDir).listFiles((dir, name) -> name.endsWith(".png"));
        Assert.assertNotNull(screenshots, "Screenshots directory should contain a captured PNG");
        Assert.assertEquals(screenshots.length, 1, "Exactly one screenshot should have been captured");
        Assert.assertTrue(screenshots[0].length() > 0, "Captured screenshot file should have content");

        String capturedFileName = screenshots[0].getName();
        String htmlContent = new String(Files.readAllBytes(Paths.get(logFilePath)));

        Assert.assertTrue(htmlContent.contains("screenshots/" + capturedFileName),
            "HTML should reference the captured screenshot via the relative 'screenshots/' path");
        Assert.assertTrue(htmlContent.contains("class='img-link'"), "HTML should contain screenshot link");
        Assert.assertTrue(htmlContent.contains("<img"), "HTML should contain screenshot thumbnail image");
        Assert.assertTrue(htmlContent.contains("loading='lazy'"), "Screenshot image should lazy load");
        Assert.assertTrue(htmlContent.contains("decoding='async'"), "Screenshot image should decode asynchronously");
        Assert.assertTrue(htmlContent.contains("width='88'"), "Screenshot image should reserve thumbnail width");
        Assert.assertTrue(htmlContent.contains("height='50'"), "Screenshot image should reserve thumbnail height");

        System.out.println("Screenshot path test completed. Log at: " + logFilePath);
    }
}
