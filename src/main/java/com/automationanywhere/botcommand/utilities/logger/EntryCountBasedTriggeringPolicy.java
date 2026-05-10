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
     * Counts existing log entries in the file by counting &lt;tr&gt;&lt;td&gt;
     * patterns. This pattern matches only data rows, excluding header rows
     * which use &lt;th&gt; instead.
     *
     * <p>Pure read-only: footer stripping happens earlier in
     * {@code CustomLogger.stripTrailingFooterIfPresent} before Log4j2 opens
     * the file. By the time this method runs, RollingFileManager already
     * holds an append-mode handle on the file, which on Windows blocks any
     * truncate/rename from this thread. Reading is unaffected because the
     * manager's share mode grants read access.
     *
     * @param filePath Path to the log file
     * @return Number of existing log entries in the file, or 0 if file
     *         doesn't exist or can't be read
     */
    private int countExistingEntries(String filePath) {
        File file = new File(filePath);
        if (!file.exists() || file.length() == 0) {
            return 0;
        }

        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String lowerLine = line.toLowerCase();
                int index = 0;
                while ((index = lowerLine.indexOf("<tr><td>", index)) != -1) {
                    count++;
                    index += 8;
                }
            }
        } catch (IOException e) {
            // Safe fallback: 0 means "treat as fresh file", so we may
            // rollover slightly early on a transient read failure, never
            // overshoot the maxEntries cap.
            return 0;
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