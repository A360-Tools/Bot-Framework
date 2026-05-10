package com.automationanywhere.botcommand.utilities.logger;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.rolling.AbstractTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.RollingFileManager;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A Log4j2 triggering policy that triggers rollover based on the number of log entries
 * rather than file size.
 *
 * @author Sumit Kumar
 */
@Plugin(name = "EntryCountBasedTriggeringPolicy", category = "Core", printObject = true)
public class EntryCountBasedTriggeringPolicy extends AbstractTriggeringPolicy {

    private final int maxEntries;
    private final AtomicInteger currentEntryCount = new AtomicInteger(0);

    private EntryCountBasedTriggeringPolicy(int maxEntries) {
        this.maxEntries = maxEntries;
    }

    /**
     * Initializes this triggering policy.
     * This method is called when the policy is first attached to a RollingFileManager.
     * Counts existing entries in the file to properly handle appends across program restarts.
     */
    @Override
    public void initialize(final RollingFileManager manager) {
        int existingEntries = countExistingEntries(manager.getFileName());
        currentEntryCount.set(existingEntries);
    }

    /**
     * Checks if rollover should be triggered based on the entry count.
     *
     * @param logEvent The log event
     * @return true if rollover should occur, false otherwise
     */
    @Override
    public boolean isTriggeringEvent(final LogEvent logEvent) {
        int count = currentEntryCount.incrementAndGet();

        // Trigger rollover AFTER we exceed the max entries
        // This ensures we write exactly maxEntries before rotating
        if (count > maxEntries) {
            // Reset the counter for the next cycle
            currentEntryCount.set(1); // Set to 1 because current entry will be first in new file
            return true;
        }

        return false;
    }

    /**
     * Get the current entry count
     */
    public int getCurrentEntryCount() {
        return currentEntryCount.get();
    }

    /**
     * Counts existing log entries in the file by counting &lt;tr&gt;&lt;td&gt; patterns.
     * This pattern matches only data rows, excluding header rows which use &lt;th&gt; instead.
     * Also removes the HTML footer if present to allow proper appending.
     * This enables proper entry counting across program restarts when appending to existing files.
     *
     * @param filePath Path to the log file
     * @return Number of existing log entries in the file, or 0 if file doesn't exist or can't be read
     */
    private int countExistingEntries(String filePath) {
        File file = new File(filePath);

        // If file doesn't exist or is empty, no entries exist
        if (!file.exists() || file.length() == 0) {
            return 0;
        }

        int count = 0;
        boolean footerFound = false;
        List<String> allLines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                allLines.add(line);
                String lowerLine = line.toLowerCase();

                // Count occurrences of <tr><td> to count only data rows (excludes header rows with <th>)
                int index = 0;
                while ((index = lowerLine.indexOf("<tr><td>", index)) != -1) {
                    count++;
                    index += 8; // Move past "<tr><td>" (8 characters) to find next occurrence
                }

                // Check if this line contains the footer
                if (!footerFound && (lowerLine.contains("</tbody>") || lowerLine.contains("</table>"))) {
                    footerFound = true;
                }
            }
        } catch (IOException e) {
            // If we can't read the file, assume no entries
            // This is a safe default as it's better to potentially rollover early
            // than to exceed the max entry limit
            return 0;
        }

        // If footer was found, remove it from the file. Write to a sibling
        // temp file and atomically rename over the original: a crash mid-write
        // leaves the original intact, whereas an in-place truncate-then-rewrite
        // would lose every prior entry on power loss.
        if (footerFound && !allLines.isEmpty()) {
            Path original = file.toPath();
            Path tmp = original.resolveSibling(file.getName() + ".rewrite-tmp");
            try {
                StringBuilder rebuilt = new StringBuilder(allLines.size() * 64);
                for (int i = 0; i < allLines.size(); i++) {
                    String line = allLines.get(i);
                    String lowerLine = line.toLowerCase();
                    int footerIndex = lowerLine.indexOf("</tbody>");
                    if (footerIndex == -1) {
                        footerIndex = lowerLine.indexOf("</table>");
                    }
                    if (footerIndex >= 0) {
                        line = line.substring(0, footerIndex);
                    }
                    rebuilt.append(line);
                    if (i < allLines.size() - 1) {
                        rebuilt.append(System.lineSeparator());
                    }
                }
                Files.write(tmp, rebuilt.toString().getBytes(StandardCharsets.UTF_8));
                try {
                    Files.move(tmp, original,
                            StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(tmp, original, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                // Best-effort cleanup of the temp file; original is untouched.
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException ignored) {
                }
                // If we can't rewrite, log file will continue to work but footer will be duplicated
                // This is not critical, just a warning condition
            }
        }

        return count;
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<EntryCountBasedTriggeringPolicy> {

        @PluginBuilderAttribute
        private int maxEntries = 1000; // Default to 1000 entries

        public Builder withMaxEntries(int maxEntries) {
            this.maxEntries = maxEntries;
            return this;
        }

        @Override
        public EntryCountBasedTriggeringPolicy build() {
            return new EntryCountBasedTriggeringPolicy(maxEntries);
        }
    }
}