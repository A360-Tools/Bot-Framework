package com.automationanywhere.botcommand.utilities.process;

import com.automationanywhere.botcommand.exception.BotCommandException;
import org.apache.commons.io.FilenameUtils;

import java.util.List;
import java.util.Optional;

/**
 * @author Sumit Kumar
 */

public class TaskKiller {

    public static void killProcesses(List<String> processNames, boolean forceKill) {
        try {
            for (String processName : processNames) {
                Optional<ProcessHandle> processHandleOptional = findByExactCommand(processName);
                processHandleOptional.ifPresent(processHandle -> {
                    try {
                        if (forceKill) {
                            processHandle.destroyForcibly();
                        } else {
                            processHandle.destroy();
                        }
                        processHandle.onExit().get();
                    } catch (Exception ex) {
                        System.out.println(ex.getMessage());
                        // Move to the next process kill
                    }
                });
            }
        } catch (Exception e) {
            throw new BotCommandException("Error killing processes: " + e.getMessage());
        }
    }

    private static Optional<ProcessHandle> findByExactCommand(String processName) {
        String baseProcessName = FilenameUtils.getBaseName(processName);
        return ProcessHandle.allProcesses()
                .filter(process -> {
                    Optional<String> cmd = process.info().command();
                    return cmd.isPresent() &&
                            FilenameUtils.getBaseName(cmd.get())
                                    .equalsIgnoreCase(baseProcessName);
                })
                .findFirst();
    }
}
