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
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

@BotCommand
@CommandPkg(
        label = "Clean Directory",
        node_label = "{{inputFolderPath}} by deleting {{selectMethod}} {{recursive}}",
        description = "Remove files/folders based on rule set",
        icon = "delete_folders.svg",
        name = "device_delete_files_folders",
        group_label = "Device",
        documentation_url = "https://github.com/A360-Tools/Bot-Framework/blob/main/docs/device/DeleteFilesFolders.md",
        text_color = "#e04f5f",
        allowed_agent_targets = AllowedTarget.HEADLESS
)
public class DeleteFilesFolders {
    private static final String THRESHOLD_UNIT_DAY = "DAY";
    private static final String THRESHOLD_UNIT_HOUR = "HOUR";
    private static final String THRESHOLD_UNIT_MINUTE = "MINUTE";
    private static final String THRESHOLD_UNIT_SECOND = "SECOND";
    private static final Map<String, Long> TIME_UNIT_CONVERSION = Map.of(
            THRESHOLD_UNIT_DAY, TimeUnit.DAYS.toMillis(1),
            THRESHOLD_UNIT_HOUR, TimeUnit.HOURS.toMillis(1),
            THRESHOLD_UNIT_MINUTE, TimeUnit.MINUTES.toMillis(1),
            THRESHOLD_UNIT_SECOND, TimeUnit.SECONDS.toMillis(1)
    );
    private static final String PROCESS_ONLY_FILE_TYPE = "FILE";
    private static final String PROCESS_ALL_TYPES = "ALL";
    private static final String ERROR_THROW = "THROW";
    private static final String ERROR_IGNORE = "IGNORE";

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
            @Pkg(label = "Folder path matching this regex pattern will be skipped during scanning",
                    description =
                            "Matching will be done on absolute path in OS file separator format.Any sub folder or " +
                                    "file within this folder will also be ignored")
            @NotEmpty
            String skipFolderPathPattern,

            @Idx(index = "7", type = AttributeType.CHECKBOX)
            @Pkg(label = "Ignore specific file paths", default_value = "false", default_value_type =
                    DataType.BOOLEAN)
            Boolean skipFiles,

            @Idx(index = "7.1", type = AttributeType.TEXT)
            @Pkg(label = "File path matching this regex pattern will be skipped during deletion", description =
                    "^.+\\.txt$ to skip all text files. Matching will be done on absolute path in OS file separator " +
                            "format.")
            @NotEmpty
            String skipFilePathPattern,

            @Idx(index = "8", type = AttributeType.RADIO, options = {
                    @Idx.Option(index = "8.1", pkg = @Pkg(label = "Throw error", value = ERROR_THROW)),
                    @Idx.Option(index = "8.2", pkg = @Pkg(label = "Ignore", value = ERROR_IGNORE))
            })
            @Pkg(label = "If certain files/folders cannot be deleted", default_value_type = DataType.STRING,
                    description =
                            "Behavior in case a file is locked/missing permission", default_value = ERROR_IGNORE)
            @NotEmpty
            String unableToDeleteBehavior
    ) {
        try {
            new FileValidator(inputFolderPath).validateDirectory();
            Long thresholdMillis = TIME_UNIT_CONVERSION.getOrDefault(thresholdUnit.toUpperCase(),
                    TimeUnit.DAYS.toMillis(30));
            long thresholdMilliseconds =
                    System.currentTimeMillis() - thresholdNumber.longValue() * thresholdMillis;

            File basePath = Paths.get(inputFolderPath).toFile();
            IOFileFilter filter = recursive ? TrueFileFilter.INSTANCE : FalseFileFilter.INSTANCE;
            Collection<File> filesAndDirs = FileUtils.listFilesAndDirs(basePath, TrueFileFilter.INSTANCE, filter);
            filesAndDirs.remove(basePath);
            //create a mapping of file/directories and their eligibility to delete based on last modified date
            //this is needed to ensure deleting of file does not impact program as folder modified date will get updated
            Map<File, Boolean> matchesDeleteThreshold = new HashMap<>();
            for (File file : filesAndDirs) {
                // Check if the file or directory is older(modified) than the threshold and populate the map
                boolean isOlder = FileUtils.isFileOlder(file, thresholdMilliseconds);
                matchesDeleteThreshold.put(file, isOlder);
            }
            Set<File> directoriesToPreserve = skipFolders ? getDirectoriesToPreserve(basePath, recursive,
                    skipFolderPathPattern) : Collections.emptySet();

            for (File file : filesAndDirs) {
                if (!directoriesToPreserve.contains(file)
                        && matchesDeleteThreshold.getOrDefault(file, false)
                        && !shouldSkipFile(file, skipFiles, skipFilePathPattern)) {
                    try {
                        if (PROCESS_ALL_TYPES.equalsIgnoreCase(selectMethod) || (file.isFile() && PROCESS_ONLY_FILE_TYPE.equalsIgnoreCase(selectMethod))) {
                            FileUtils.forceDelete(file);
                        }
                    } catch (FileNotFoundException ignored) {
                    } catch (IOException e) {
                        if (ERROR_THROW.equalsIgnoreCase(unableToDeleteBehavior)) {
                            throw new BotCommandException(e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception exception) {
            throw new BotCommandException(exception.getMessage());
        }
    }

    private Set<File> getDirectoriesToPreserve(File basePath, boolean recursive, String skipFolderPathPattern) {
        Set<File> directoriesToPreserve = new HashSet<>();
        IOFileFilter filter = recursive ? TrueFileFilter.INSTANCE : FalseFileFilter.INSTANCE;
        Collection<File> dirs = FileUtils.listFilesAndDirs(basePath, FalseFileFilter.INSTANCE, filter);
        dirs.stream()
                .filter(dir -> dir.getAbsolutePath().matches(skipFolderPathPattern))
                .forEach(directoriesToPreserve::add);
        return directoriesToPreserve;
    }

    private boolean shouldSkipFile(File file, boolean skipFiles, String skipFilePathPattern) {
        return skipFiles && file.isFile() && file.getAbsolutePath().matches(skipFilePathPattern);
    }
}
