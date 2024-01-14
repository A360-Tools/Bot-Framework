package com.automationanywhere.botcommand.utilities.file;

/**
 * @author Sumit Kumar
 */

import org.apache.commons.io.FilenameUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class FileValidator {

    private final Path path;

    public FileValidator(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }
        this.path = Paths.get(filePath);
    }

    public void validateFile(String[] allowedExtensions) {
        validateCommonConditions();

        if (Files.isDirectory(path)) {
            throw new IllegalArgumentException("Specified path is a directory, not a file: " + path);
        }

        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Specified path does not point to a valid file: " + path);
        }

        String fileExtension = FilenameUtils.getExtension(path.getFileName().toString());
        String lowerCaseFileExtension = fileExtension.toLowerCase();

        boolean isExtensionValid = Arrays.stream(allowedExtensions)
                .map(String::toLowerCase)
                .anyMatch(lowerCaseFileExtension::equals);

        if (!isExtensionValid) {
            throw new IllegalArgumentException("Invalid file extension. Expected: " +
                    Arrays.toString(allowedExtensions) + ", Actual: " + lowerCaseFileExtension);
        }
    }

    public void validateDirectory() {
        validateCommonConditions();

        // Additional directory-specific validations
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("Specified path is not a directory: " + path);
        }

    }

    private void validateCommonConditions() {
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("File does not exist at the specified path: " + path);
        }

        if (!Files.isReadable(path)) {
            throw new IllegalArgumentException("Cannot read the specified file: " + path);
        }
    }
}


