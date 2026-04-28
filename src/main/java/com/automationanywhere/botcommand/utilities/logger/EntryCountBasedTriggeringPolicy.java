package com.automationanywhere.botcommand.utilities.logger;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.rolling.AbstractTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.RollingFileManager;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;

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
        // Count existing entries in the file if it exists
        String fileName = manager.getFileName();
        int existingEntries = countExistingEntries(fileName);

        // Initialize counter to existing entry count
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
     * Reset the counter after a successful rollover
     */
    public void clearCounter() {
        currentEntryCount.set(0);
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
        try {
            return HtmlLogFileSupport.prepareForAppend(filePath);
        } catch (IOException e) {
            // If we can't read the file, assume no entries
            // This is a safe default as it's better to potentially rollover early
            // than to exceed the max entry limit
            return 0;
        }
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
