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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

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
    private static final String THRESHOLD_CRITERIA_CREATION = "CREATION";
    private static final String THRESHOLD_CRITERIA_MODIFICATION = "MODIFICATION";
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
            @Pkg(label = "Threshold number", default_value_type = DataType.NUMBER, default_value = "30",
                    description = "any file/folder with threshold age value older than this value will be " +
                            "deleted")
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

            @Idx(index = "6", type = AttributeType.SELECT, options = {
                    @Idx.Option(index = "6.1", pkg = @Pkg(label = "CREATION", value = THRESHOLD_CRITERIA_CREATION)),
                    @Idx.Option(index = "6.2", pkg = @Pkg(label = "LAST MODIFICATION", value =
                            THRESHOLD_CRITERIA_MODIFICATION))})
            @Pkg(label = "Threshold age type", default_value = THRESHOLD_CRITERIA_CREATION,
                    default_value_type = DataType.STRING)
            @NotEmpty
            @SelectModes
            String thresholdCriteria,

            @Idx(index = "7", type = AttributeType.CHECKBOX)
            @Pkg(label = "Ignore specific folder paths", default_value = "false", default_value_type =
                    DataType.BOOLEAN)
            Boolean skipFolders,

            @Idx(index = "7.1", type = AttributeType.TEXT)
            @Pkg(label = "Folder path matching this regex pattern will be skipped during scanning",
                    description =
                            "Matching will be done on absolute path in OS file separator format.Any sub folder or " +
                                    "file within this folder will also be ignored")
            @NotEmpty
            String skipFolderPathPattern,

            @Idx(index = "8", type = AttributeType.CHECKBOX)
            @Pkg(label = "Ignore specific file paths", default_value = "false", default_value_type =
                    DataType.BOOLEAN)
            Boolean skipFiles,

            @Idx(index = "8.1", type = AttributeType.TEXT)
            @Pkg(label = "File path matching this regex pattern will be skipped during deletion", description =
                    "^.+\\.txt$ to skip all text files. Matching will be done on absolute path in OS file separator " +
                            "format.")
            @NotEmpty
            String skipFilePathPattern,

            @Idx(index = "9", type = AttributeType.RADIO, options = {
                    @Idx.Option(index = "9.1", pkg = @Pkg(label = "Throw error", value = ERROR_THROW)),
                    @Idx.Option(index = "9.2", pkg = @Pkg(label = "Ignore", value = ERROR_IGNORE))
            })
            @Pkg(label = "If certain files/folders cannot be deleted", default_value_type = DataType.STRING,
                    description =
                            "Behavior in case a file is locked/missing permission", default_value = ERROR_IGNORE)
            @NotEmpty
            String unableToDeleteBehavior
    ) {
        try {
            new FileValidator(inputFolderPath).validateDirectory();

            Instant thresholdInstant = calculateThresholdInstant(thresholdNumber.longValue(), thresholdUnit);

            File basePath = Paths.get(inputFolderPath).toFile();
            IOFileFilter filter = recursive ? TrueFileFilter.INSTANCE : FalseFileFilter.INSTANCE;
            Collection<File> filesAndDirs = FileUtils.listFilesAndDirs(basePath, TrueFileFilter.INSTANCE, filter);
            filesAndDirs.remove(basePath);
            //create a mapping of file/directories and their eligibility to delete based on last modified date
            //this is needed to ensure deleting of file does not impact program as folder modified date will get updated
            Map<File, Boolean> matchesDeleteThreshold = new HashMap<>();
            for (File file : filesAndDirs) {
                Path path = file.toPath();
                BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
                Instant timeToCompare;
                switch (thresholdCriteria) {
                    case THRESHOLD_CRITERIA_CREATION:
                        timeToCompare = attributes.creationTime().toInstant();
                        break;
                    case THRESHOLD_CRITERIA_MODIFICATION:
                        timeToCompare = attributes.lastModifiedTime().toInstant();
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported threshold criteria: " + thresholdCriteria);
                }

                boolean isEligibleForDeletion = timeToCompare.isBefore(thresholdInstant);
                matchesDeleteThreshold.put(file, isEligibleForDeletion);
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

    private Instant calculateThresholdInstant(long thresholdNumber, String thresholdUnit) {
        Duration duration;
        switch (thresholdUnit.toUpperCase()) {
            case THRESHOLD_UNIT_DAY:
                duration = Duration.ofDays(thresholdNumber);
                break;
            case THRESHOLD_UNIT_HOUR:
                duration = Duration.ofHours(thresholdNumber);
                break;
            case THRESHOLD_UNIT_MINUTE:
                duration = Duration.ofMinutes(thresholdNumber);
                break;
            case THRESHOLD_UNIT_SECOND:
                duration = Duration.ofSeconds(thresholdNumber);
                break;
            default:
                throw new IllegalArgumentException("Unsupported time unit: " + thresholdUnit);
        }
        return Instant.now().minus(duration);
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
