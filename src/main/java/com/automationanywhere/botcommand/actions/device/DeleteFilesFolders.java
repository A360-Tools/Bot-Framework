package com.automationanywhere.botcommand.actions.device;

import com.automationanywhere.botcommand.exception.BotCommandException;
import com.automationanywhere.commandsdk.annotations.*;
import com.automationanywhere.commandsdk.annotations.rules.*;
import com.automationanywhere.commandsdk.model.AllowedTarget;
import com.automationanywhere.commandsdk.model.AttributeType;
import com.automationanywhere.commandsdk.model.DataType;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

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
            @Idx(index = "1", type = AttributeType.FILE)
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
            @Pkg(label = "Regex pattern to match folder paths to ignore",
                    description = ".*\\\\subDirectory" + " to skip folder called subDirectory on windows platform" +
                            "Matching will be done on absolute path in OS file separator format.")
            @NotEmpty
            String skipFolderPathPattern,

            @Idx(index = "8", type = AttributeType.CHECKBOX)
            @Pkg(label = "Ignore specific file paths", default_value = "false", default_value_type =
                    DataType.BOOLEAN)
            Boolean skipFiles,

            @Idx(index = "8.1", type = AttributeType.TEXT)
            @Pkg(label = "Regex pattern to match file paths to ignore", description =
                    ".*\\.txt$" + " to skip all text files on windows platform. Matching will be done on absolute " +
                            "path in OS file separator format.")
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
            Path basePath = Paths.get(inputFolderPath);
            Instant deletionThresholdInstant = calculateAgeThreshold(thresholdNumber.longValue(), thresholdUnit);
            Set<Path> directoriesToSkipDeletion = new HashSet<>();
            Set<Path> filesToSkipDeletion = new HashSet<>();
            Set<Path> directoriesToDelete = new HashSet<>();
            Set<Path> filesToDelete = new HashSet<>();

            directoriesToSkipDeletion.add(basePath);
            Files.walkFileTree(basePath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (!recursive) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }

                    if (skipFolders && shouldSkipDir(dir, skipFolderPathPattern)) {
                        directoriesToSkipDeletion.add(basePath);
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    if (meetsDeletionCriteria(attrs, thresholdCriteria, deletionThresholdInstant)) {
                        directoriesToDelete.add(dir);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (skipFiles && shouldSkipFile(file, skipFilePathPattern)) {
                        filesToSkipDeletion.add(file);
                        return FileVisitResult.CONTINUE;
                    }
                    if (meetsDeletionCriteria(attrs, thresholdCriteria, deletionThresholdInstant)) {
                        filesToDelete.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }

            });
            //add all ancestors to skip to ensure they are not deleted
            for (Path fileToSkip : filesToSkipDeletion) {
                Path parent = fileToSkip.getParent();
                while (parent != null && !parent.equals(basePath)) {
                    directoriesToSkipDeletion.add(parent);
                    parent = parent.getParent();
                }
            }
            directoriesToDelete.removeAll(directoriesToSkipDeletion);
            if (selectMethod.equalsIgnoreCase(PROCESS_ALL_TYPES)) {
                delete(filesToDelete, unableToDeleteBehavior);
                delete(directoriesToDelete, unableToDeleteBehavior);
            } else if (selectMethod.equalsIgnoreCase(PROCESS_ONLY_FILE_TYPE)) {
                delete(filesToDelete, unableToDeleteBehavior);
            }

        } catch (IOException e) {
            if (unableToDeleteBehavior.equalsIgnoreCase(ERROR_THROW)) {
                throw new BotCommandException(e.getMessage());
            }
        } catch (Exception e) {
            throw new BotCommandException(e.getMessage());
        }

    }

    private Instant calculateAgeThreshold(long threshold, String unit) {
        Instant now = Instant.now();
        switch (unit) {
            case THRESHOLD_UNIT_DAY:
                return now.minus(threshold, ChronoUnit.DAYS);
            case THRESHOLD_UNIT_HOUR:
                return now.minus(threshold, ChronoUnit.HOURS);
            case THRESHOLD_UNIT_MINUTE:
                return now.minus(threshold, ChronoUnit.MINUTES);
            case THRESHOLD_UNIT_SECOND:
                return now.minus(threshold, ChronoUnit.SECONDS);
            default:
                throw new IllegalArgumentException("Unsupported time unit: " + unit);
        }
    }

    private boolean shouldSkipDir(Path path, String folderPattern) {
        return Files.isDirectory(path) && Pattern.matches(folderPattern, path.toString());
    }

    private boolean meetsDeletionCriteria(BasicFileAttributes attrs, String thresholdCriteria, Instant ageThreshold) {
        Instant fileTime;
        switch (thresholdCriteria) {
            case THRESHOLD_CRITERIA_CREATION:
                fileTime = attrs.creationTime().toInstant();
                break;
            case THRESHOLD_CRITERIA_MODIFICATION:
                fileTime = attrs.lastModifiedTime().toInstant();
                break;
            default:
                throw new IllegalArgumentException("Unsupported threshold criteria: " + thresholdCriteria);
        }

        return fileTime.isBefore(ageThreshold);//older than threshold deletion date
    }

    private boolean shouldSkipFile(Path path, String filePattern) {
        return Files.isRegularFile(path) && Pattern.matches(filePattern, path.toFile().getAbsolutePath());
    }

    private static void delete(Set<Path> filesToDelete,
                               String unableToDeleteBehavior) {
        for (Path filePath : filesToDelete) {
            try {
                FileUtils.forceDelete(filePath.toFile());
            } catch (IOException e) {
                if (unableToDeleteBehavior.equalsIgnoreCase(ERROR_THROW)) {
                    throw new BotCommandException(e.getMessage());
                }
            }
        }
    }

}