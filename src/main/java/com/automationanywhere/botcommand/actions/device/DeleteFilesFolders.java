package com.automationanywhere.botcommand.actions.device;

import com.automationanywhere.botcommand.exception.BotCommandException;
import com.automationanywhere.commandsdk.annotations.*;
import com.automationanywhere.commandsdk.annotations.rules.*;
import com.automationanywhere.commandsdk.model.AttributeType;
import com.automationanywhere.commandsdk.model.DataType;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;
import java.util.logging.Logger;

@BotCommand
@CommandPkg(
        label = "Clean Directory",
        node_label = "{{inputFolderPath}} by deleting {{selectMethod}} {{recursive}}",
        description = "Remove files/folders based on rule set",
        icon = "delete_folders.svg",
        name = "device_delete_files_folders",
        group_label = "Device",
        documentation_url = "https://github.com/A360-Tools/Bot-Framework/blob/main/docs/device/DeleteFilesFolders.md"
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
    private static final Logger LOGGER = Logger.getLogger(DeleteFilesFolders.class.getName());

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
            if (!Files.exists(basePath)) {
                throw new BotCommandException("Base folder path does not exist: " + inputFolderPath);
            }

            LOGGER.info("Starting deletion process for base path: " + basePath);

            // Phase 1: Collect data - files/directories to delete and skip
            FileCollector collector = new FileCollector(
                    basePath, recursive, thresholdCriteria,
                    calculateAgeThreshold(thresholdNumber.longValue(), thresholdUnit),
                    skipFiles, skipFilePathPattern,
                    skipFolders, skipFolderPathPattern
            );
            Files.walkFileTree(basePath, collector);

            // Phase 2: Process the data - resolve conflicts between delete and skip lists
            DeletionProcessor processor = new DeletionProcessor(
                    basePath,
                    collector.getFilesToDelete(),
                    collector.getDirectoriesToDelete(),
                    collector.getFilesToSkip(),
                    collector.getDirectoriesToSkip()
            );

            // Phase 3: Execute deletions
            if (selectMethod.equalsIgnoreCase(PROCESS_ONLY_FILE_TYPE) ||
                    selectMethod.equalsIgnoreCase(PROCESS_ALL_TYPES)) {
                delete(processor.getFilesToDelete(), unableToDeleteBehavior);
            }

            if (selectMethod.equalsIgnoreCase(PROCESS_ALL_TYPES)) {
                delete(processor.getSortedDirectoriesToDelete(), unableToDeleteBehavior);
            }

            LOGGER.info("Deletion process completed successfully");

        } catch (IOException e) {
            LOGGER.severe("IO error occurred: " + e.getMessage());
            if (unableToDeleteBehavior.equalsIgnoreCase(ERROR_THROW)) {
                throw new BotCommandException("IO error: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            LOGGER.severe("Unexpected error: " + e.getMessage());
            throw new BotCommandException("Error: " + e.getMessage(), e);
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

    private void delete(List<Path> pathsToDelete, String unableToDeleteBehavior) {
        for (Path path : pathsToDelete) {
            try {
                LOGGER.info("Deleting: " + path);
                FileUtils.forceDelete(path.toFile());
            } catch (IOException e) {
                LOGGER.warning("Failed to delete " + path + ": " + e.getMessage());
                if (unableToDeleteBehavior.equalsIgnoreCase(ERROR_THROW)) {
                    throw new BotCommandException("Failed to delete " + path + ": " + e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Helper class that collects files and directories during file tree traversal.
     */
    private static class FileCollector extends SimpleFileVisitor<Path> {
        private final Path basePath;
        private final boolean recursive;
        private final String thresholdCriteria;
        private final Instant deletionThresholdInstant;
        private final boolean skipFiles;
        private final String skipFilePathPattern;
        private final boolean skipFolders;
        private final String skipFolderPathPattern;

        private final Set<Path> filesToDelete = new HashSet<>();
        private final Set<Path> directoriesToDelete = new HashSet<>();
        private final Set<Path> filesToSkip = new HashSet<>();
        private final Set<Path> directoriesToSkip = new HashSet<>();

        public FileCollector(
                Path basePath,
                boolean recursive,
                String thresholdCriteria,
                Instant deletionThresholdInstant,
                boolean skipFiles,
                String skipFilePathPattern,
                boolean skipFolders,
                String skipFolderPathPattern) {
            this.basePath = basePath;
            this.recursive = recursive;
            this.thresholdCriteria = thresholdCriteria;
            this.deletionThresholdInstant = deletionThresholdInstant;
            this.skipFiles = skipFiles;
            this.skipFilePathPattern = skipFilePathPattern;
            this.skipFolders = skipFolders;
            this.skipFolderPathPattern = skipFolderPathPattern;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            // Skip processing of the base path for deletion (never delete the base path)
            if (dir.equals(basePath)) {
                return FileVisitResult.CONTINUE;
            }

            // Check if this directory should be skipped based on pattern
            if (skipFolders && matchesPattern(dir.toFile().getAbsolutePath(), skipFolderPathPattern)) {
                LOGGER.info("Skipping directory based on pattern: " + dir);
                directoriesToSkip.add(dir);
                return FileVisitResult.SKIP_SUBTREE; // Don't process contents of skipped directories
            }

            // Check if directory meets age criteria for deletion regardless of recursive setting
            // The actual deletion will be controlled by selectMethod parameter
            if (meetsDeletionCriteria(attrs)) {
                LOGGER.info("Marking directory for potential deletion: " + dir);
                directoriesToDelete.add(dir);
            }

            // If not recursive, skip traversing into subdirectories after processing current directory
            if (!recursive) {
                return FileVisitResult.SKIP_SUBTREE;
            }

            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            // Check if this file should be skipped based on pattern
            if (skipFiles && matchesPattern(file.toFile().getAbsolutePath(), skipFilePathPattern)) {
                LOGGER.info("Skipping file based on pattern: " + file);
                filesToSkip.add(file);
                return FileVisitResult.CONTINUE;
            }

            // Check if this file meets the age criteria for deletion
            if (meetsDeletionCriteria(attrs)) {
                LOGGER.info("Marking file for deletion: " + file);
                filesToDelete.add(file);
            }

            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            LOGGER.warning("Failed to visit file: " + file + " - " + exc.getMessage());
            return FileVisitResult.CONTINUE;
        }

        private boolean meetsDeletionCriteria(BasicFileAttributes attrs) {
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

            return fileTime.isBefore(deletionThresholdInstant); // older than threshold
        }

        private boolean matchesPattern(String pathString, String pattern) {
            return Pattern.matches(pattern, pathString);
        }

        public Set<Path> getFilesToDelete() {
            return filesToDelete;
        }

        public Set<Path> getDirectoriesToDelete() {
            return directoriesToDelete;
        }

        public Set<Path> getFilesToSkip() {
            return filesToSkip;
        }

        public Set<Path> getDirectoriesToSkip() {
            return directoriesToSkip;
        }
    }

    /**
     * Helper class that processes file and directory lists to resolve conflicts
     * and prepare for deletion.
     */
    private static class DeletionProcessor {
        private final Path basePath;
        private final Set<Path> filesToDelete;
        private final Set<Path> directoriesToDelete;
        private final Set<Path> directoriesToPreserve = new HashSet<>();

        public DeletionProcessor(
                Path basePath,
                Set<Path> filesToDelete,
                Set<Path> directoriesToDelete,
                Set<Path> filesToSkip,
                Set<Path> directoriesToSkip) {
            this.basePath = basePath;
            this.filesToDelete = new HashSet<>(filesToDelete);
            this.directoriesToDelete = new HashSet<>(directoriesToDelete);

            // Always preserve the base path
            directoriesToPreserve.add(basePath);

            // Process skipped files - preserve their parent directories
            for (Path skippedFile : filesToSkip) {
                addParentsToPreserveList(skippedFile);
            }

            // Process skipped directories - preserve them and their parent directories
            for (Path skippedDir : directoriesToSkip) {
                directoriesToPreserve.add(skippedDir);
                addParentsToPreserveList(skippedDir);
            }

            // Remove preserved directories from deletion list
            this.directoriesToDelete.removeAll(directoriesToPreserve);

            LOGGER.info("Files to delete after processing: " + this.filesToDelete.size());
            LOGGER.info("Directories to delete after processing: " + this.directoriesToDelete.size());
            LOGGER.info("Directories preserved for containing skipped items: " +
                    (directoriesToPreserve.size() - 1)); // -1 for basePath
        }

        private void addParentsToPreserveList(Path path) {
            Path parent = path.getParent();
            while (parent != null && !parent.equals(basePath)) {
                directoriesToPreserve.add(parent);
                parent = parent.getParent();
            }
        }

        public List<Path> getFilesToDelete() {
            return new ArrayList<>(filesToDelete);
        }

        public List<Path> getSortedDirectoriesToDelete() {
            List<Path> sortedDirectories = new ArrayList<>(directoriesToDelete);
            // Sort by depth (descending) - delete deepest directories first
            sortedDirectories.sort((p1, p2) -> Integer.compare(p2.getNameCount(), p1.getNameCount()));
            return sortedDirectories;
        }
    }
}