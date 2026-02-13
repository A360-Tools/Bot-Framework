package com.automationanywhere.botcommand.utilities.process;

import com.automationanywhere.botcommand.exception.BotCommandException;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Sumit Kumar
 */

public class TaskKiller {
    private static final Logger LOGGER = LogManager.getLogger(TaskKiller.class);
    private static final long PROCESS_EXIT_TIMEOUT_SECONDS = 30;

    public static void killProcesses(List<String> processNames, boolean forceKill) {
        try {
            for (String processName : processNames) {
                List<ProcessHandle> matchingProcesses = findAllByCommand(processName);
                for (ProcessHandle processHandle : matchingProcesses) {
                    try {
                        if (forceKill) {
                            processHandle.destroyForcibly();
                        } else {
                            processHandle.destroy();
                        }
                        processHandle.onExit().get(PROCESS_EXIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    } catch (Exception ex) {
                        LOGGER.warn("Failed to terminate process '{}': {}", processName, ex.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            throw new BotCommandException("Error killing processes: " + e.getMessage(), e);
        }
    }

    private static List<ProcessHandle> findAllByCommand(String processName) {
        String baseProcessName = FilenameUtils.getBaseName(processName);
        return ProcessHandle.allProcesses()
                .filter(process -> {
                    Optional<String> cmd = process.info().command();
                    return cmd.isPresent() &&
                            FilenameUtils.getBaseName(cmd.get())
                                    .equalsIgnoreCase(baseProcessName);
                })
                .collect(Collectors.toList());
    }
}
