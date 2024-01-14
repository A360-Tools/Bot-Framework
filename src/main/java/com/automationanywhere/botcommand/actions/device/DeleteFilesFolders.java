package com.automationanywhere.botcommand.actions.device;

import com.automationanywhere.botcommand.exception.BotCommandException;
import com.automationanywhere.botcommand.utilities.file.FileValidator;
import com.automationanywhere.commandsdk.annotations.*;
import com.automationanywhere.commandsdk.annotations.rules.*;
import com.automationanywhere.commandsdk.model.AllowedTarget;
import com.automationanywhere.commandsdk.model.AttributeType;
import com.automationanywhere.commandsdk.model.DataType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


/**
 * @author Sumit Kumar
 */
@BotCommand
@CommandPkg(label = "Clean Directory",
        node_label = "{{inputFolderPath}} by deleting {{selectMethod}} {{recursive}}",
        description = "Remove files/folders based on rule set",
        icon = "delete_folders.svg", name = "device_delete_files_folders",
        group_label = "Device",
        text_color = "#e04f5f",
        allowed_agent_targets = AllowedTarget.HEADLESS
)
public class DeleteFilesFolders {

    private static final String THRESHOLD_UNIT_DAY = "DAY";
    private static final String THRESHOLD_UNIT_HOUR = "HOUR";
    private static final String THRESHOLD_UNIT_MINUTE = "MINUTE";
    private static final String THRESHOLD_UNIT_SECOND = "SECOND";
    private static final String PROCESS_ONLY_FILE_TYPE = "FILE";
    private static final String PROCESS_ALL_TYPES = "ALL";
    private static final String ERROR_THROW = "THROW";
    private static final String ERROR_IGNORE = "IGNORE";

    private static boolean isImmediateChild(File file, File directory) {
        return file.getParentFile() != null && file.getParentFile().equals(directory);
    }

    @Execute
    public void action(
            @Idx(index = "1", type = AttributeType.TEXT)
            @Pkg(label = "Enter base folder path", description = "Files/Folders will be scanned within this folder " +
                    "for deletion")
            @NotEmpty
            @FileFolder
            String inputFolderPath,

            @Idx(index = "2", type = AttributeType.SELECT, options = {
                    @Idx.Option(index = "2.1", pkg = @Pkg(label = "Directories and Files", value = PROCESS_ALL_TYPES,
                            node_label = "it's directories and files")),
                    @Idx.Option(index = "2.2", pkg = @Pkg(label = "Files Only", value = PROCESS_ONLY_FILE_TYPE,
                            node_label = "it's files"))})
            @Pkg(label = "Deletion option", default_value = PROCESS_ALL_TYPES,
                    default_value_type = DataType.STRING)
            @NotEmpty
            @SelectModes
            String selectMethod,

            @Idx(index = "3", type = AttributeType.CHECKBOX)
            @Pkg(label = "All subdirectories are searched as well", default_value = "true",
                    node_label = "Action all subdirectories",
                    default_value_type = DataType.BOOLEAN)
            Boolean recursive,

            @Idx(index = "4", type = AttributeType.NUMBER)
            @Pkg(label = "Enter threshold number", default_value_type = DataType.NUMBER, default_value = "30",
                    description = "any file/folder with modified datetime older than this value will be deleted")
            @NotEmpty
            @GreaterThanEqualTo("0")
            @NumberInteger
            Number thresholdNumber,

            @Idx(index = "5", type = AttributeType.SELECT, options = {
                    @Idx.Option(index = "5.1", pkg = @Pkg(label = "DAY", value = THRESHOLD_UNIT_DAY)),
                    @Idx.Option(index = "5.2", pkg = @Pkg(label = "HOUR", value = THRESHOLD_UNIT_HOUR)),
                    @Idx.Option(index = "5.3", pkg = @Pkg(label = "MINUTE", value = THRESHOLD_UNIT_MINUTE)),
                    @Idx.Option(index = "5.4", pkg = @Pkg(label = "SECOND", value = THRESHOLD_UNIT_SECOND))})
            @Pkg(label = "Threshold Unit", default_value = THRESHOLD_UNIT_DAY,
                    default_value_type = DataType.STRING)
            @NotEmpty
            @SelectModes
            String thresholdUnit,

            @Idx(index = "6", type = AttributeType.CHECKBOX)
            @Pkg(label = "Ignore specific folder paths", default_value = "false", default_value_type =
                    DataType.BOOLEAN)
            Boolean skipFolders,

            @Idx(index = "6.1", type = AttributeType.TEXT)
            @Pkg(label = "Folder path matching this pattern will be skipped during scanning", description = "Any " +
                    "sub folder or file within this folder will also be ignored")
            @NotEmpty
            String skipFolderPathPattern,

            @Idx(index = "7", type = AttributeType.CHECKBOX)
            @Pkg(label = "Ignore specific file paths", default_value = "false", default_value_type =
                    DataType.BOOLEAN)
            Boolean skipFiles,

            @Idx(index = "7.1", type = AttributeType.TEXT)
            @Pkg(label = "File path matching this pattern will be skipped during deletion")
            @NotEmpty
            String skipFilePathPattern,

            @Idx(index = "8", type = AttributeType.RADIO, options = {
                    @Idx.Option(index = "8.1", pkg = @Pkg(label = "Throw error", value = ERROR_THROW)),
                    @Idx.Option(index = "8.2", pkg = @Pkg(label = "Ignore", value = ERROR_IGNORE))
            })
            @Pkg(label = "If certain files/folders cannot be deleted", default_value_type = DataType.STRING, description =
                    "Behavior in case a file is locked/missing permission", default_value = ERROR_IGNORE)
            @NotEmpty
            String unableToDeleteBehavior
    ) {
        try {
            validateInput(inputFolderPath);

            Path basePath = Paths.get(inputFolderPath);
            Set<File> directoriesToPreserve = new HashSet<>();
            if (skipFolders) {
                directoriesToPreserve = getDirectoriesToPreserve(basePath, recursive, skipFolderPathPattern);
            }

            long thresholdMilliseconds = calculateThresholdMilliseconds(thresholdUnit, thresholdNumber);
            //get all possible files and folders with optional sub folders/files
            Collection<File> filesAndDirs = FileUtils.listFilesAndDirs(basePath.toFile(),
                    TrueFileFilter.INSTANCE, getFilter(recursive));

            processFilesAndDirs(filesAndDirs, directoriesToPreserve, thresholdMilliseconds,
                    selectMethod, skipFiles, skipFilePathPattern, unableToDeleteBehavior);

        } catch (Exception exception) {
            throw new BotCommandException(exception.getMessage());
        }
    }

    private void validateInput(String inputFolderPath) {
        FileValidator fileValidator = new FileValidator(inputFolderPath);
        fileValidator.validateDirectory();
    }

    private Set<File> getDirectoriesToPreserve(Path basePath, boolean recursive, String skipFolderPathPattern) {
        Set<File> directoriesToPreserve = new HashSet<>();
        IOFileFilter filter = getFilter(recursive);
        //only find directories and optionally subdirectories
        Collection<File> dirs = FileUtils.listFilesAndDirs(basePath.toFile(), FalseFileFilter.INSTANCE, filter);
        for (File dir : dirs) {
            if (dir.getAbsolutePath().matches(skipFolderPathPattern)) {
                if (recursive) {
                    //get all sub directories
                    Collection<File> subDirs = FileUtils.listFilesAndDirs(dir, FalseFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
                    directoriesToPreserve.addAll(subDirs);
                } else {
                    directoriesToPreserve.add(dir);
                }
            }
        }
        return directoriesToPreserve;
    }

    private long calculateThresholdMilliseconds(String thresholdUnit, Number thresholdNumber) {
        long thresholdMilliseconds;
        long systemMilliseconds = System.currentTimeMillis();
        switch (thresholdUnit.toUpperCase()) {
            case THRESHOLD_UNIT_DAY:
                thresholdMilliseconds = systemMilliseconds - thresholdNumber.longValue() * 24 * 60 * 60 * 1000;
                break;
            case THRESHOLD_UNIT_HOUR:
                thresholdMilliseconds = systemMilliseconds - thresholdNumber.longValue() * 60 * 60 * 1000;
                break;
            case THRESHOLD_UNIT_MINUTE:
                thresholdMilliseconds = systemMilliseconds - thresholdNumber.longValue() * 60 * 1000;
                break;
            case THRESHOLD_UNIT_SECOND:
                thresholdMilliseconds = systemMilliseconds - thresholdNumber.longValue() * 1000;
                break;
            default:
                throw new IllegalArgumentException("Invalid threshold unit: " + thresholdUnit);
        }

        return thresholdMilliseconds;
    }

    private IOFileFilter getFilter(boolean recursive) {
        return recursive ? TrueFileFilter.INSTANCE : FalseFileFilter.INSTANCE;
    }

    private void processFilesAndDirs(Collection<File> filesAndDirs, Set<File> directoriesToPreserve,
                                     long thresholdMilliseconds, String selectMethod, boolean skipFiles,
                                     String skipFilePathPattern, String unableToDeleteBehavior) {
        for (File file : filesAndDirs) {
            if (shouldSkipFile(file, directoriesToPreserve, skipFiles, skipFilePathPattern)) {
                continue;
            }

            try {
                processFile(file, thresholdMilliseconds, selectMethod);
            } catch (FileNotFoundException ignored) {
            } catch (IOException e) {
                handleUnableToDeleteBehavior(unableToDeleteBehavior, e);
            }
        }
    }

    private void processFile(File file, long thresholdMilliseconds, String selectMethod) throws IOException {
        if (FileUtils.isFileOlder(file, thresholdMilliseconds)) {
            deleteFile(file, selectMethod);
        }
    }

    private void deleteFile(File file, String selectMethod) throws IOException {
        if (PROCESS_ALL_TYPES.equalsIgnoreCase(selectMethod) || (file.isFile() && PROCESS_ONLY_FILE_TYPE.equalsIgnoreCase(selectMethod))) {
            FileUtils.forceDelete(file);
        }
    }

    private boolean shouldSkipFile(File file, Set<File> directoriesToPreserve, boolean skipFiles, String skipFilePathPattern) {
        if (directoriesToPreserve.contains(file)) {
            return true;
        }

        if (directoriesToPreserve.stream().anyMatch(directory -> isImmediateChild(file, directory))) {
            return true;
        }

        return skipFiles && file.isFile() && file.getAbsolutePath().matches(skipFilePathPattern);
    }


    private void handleUnableToDeleteBehavior(String unableToDeleteBehavior, IOException e) throws BotCommandException {
        if (ERROR_THROW.equalsIgnoreCase(unableToDeleteBehavior)) {
            throw new BotCommandException(e.getMessage());
        }
    }
}