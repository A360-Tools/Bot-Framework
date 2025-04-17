package device;

import com.automationanywhere.botcommand.actions.device.DeleteFilesFolders;
import com.automationanywhere.botcommand.exception.BotCommandException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive test suite for the DeleteFilesFolders class
 */
public class DeleteFilesFoldersTest {
    private static final String TEST_DIRECTORY_PATH = "src/test/target/temp/deleteFolderTest/";
    private static final String PROCESS_ALL_TYPES = "ALL";
    private static final String PROCESS_ONLY_FILE_TYPE = "FILE";
    private static final String THRESHOLD_UNIT_DAY = "DAY";
    private static final String THRESHOLD_UNIT_HOUR = "HOUR";
    private static final String THRESHOLD_UNIT_MINUTE = "MINUTE";
    private static final String THRESHOLD_UNIT_SECOND = "SECOND";
    private static final String ERROR_IGNORE = "IGNORE";
    private static final String ERROR_THROW = "THROW";
    private static final String THRESHOLD_CRITERIA_CREATION = "CREATION";
    private static final String THRESHOLD_CRITERIA_MODIFICATION = "MODIFICATION";

    private DeleteFilesFolders deleteFilesFolders;

    @BeforeMethod
    public void setUp() throws IOException {
        deleteFilesFolders = new DeleteFilesFolders();
        FileUtils.forceMkdir(new File(TEST_DIRECTORY_PATH));
        FileUtils.cleanDirectory(new File(TEST_DIRECTORY_PATH));
    }

    @AfterMethod
    public void tearDown() throws IOException {
        cleanupTestEnvironment();
    }

    private void cleanupTestEnvironment() throws IOException {
        FileUtils.deleteDirectory(new File(TEST_DIRECTORY_PATH));
    }

    // Helper method to count files and directories in the test directory
    private int countFilesAndDirs(File directory) {
        Collection<File> filesAndDirs = FileUtils.listFilesAndDirs(
                directory, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        filesAndDirs.remove(directory); // Remove the root directory from the count
        return filesAndDirs.size();
    }

    // Helper method to check if specific paths exist
    private void assertPathsExist(String... relativePaths) {
        for (String relativePath : relativePaths) {
            File file = new File(TEST_DIRECTORY_PATH + relativePath);
            Assert.assertTrue(file.exists(), "Expected path does not exist: " + relativePath);
        }
    }

    // Helper method to check if specific paths don't exist
    private void assertPathsDoNotExist(String... relativePaths) {
        for (String relativePath : relativePaths) {
            File file = new File(TEST_DIRECTORY_PATH + relativePath);
            Assert.assertFalse(file.exists(), "Path should not exist: " + relativePath);
        }
    }

    // Helper method to create a file with specific creation and modification times
    private void createFileWithTimes(String relativePath, Instant creationTime, Instant modificationTime) throws IOException {
        Path path = Paths.get(TEST_DIRECTORY_PATH, relativePath);
        Files.createDirectories(path.getParent());
        Files.createFile(path);

        if (creationTime != null || modificationTime != null) {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);

            if (creationTime != null) {
                Files.setAttribute(path, "creationTime", FileTime.from(creationTime));
            }

            if (modificationTime != null) {
                Files.setLastModifiedTime(path, FileTime.from(modificationTime));
            }
        }
    }

    // Helper method to create a directory with specific creation and modification times
    private void createDirWithTimes(String relativePath, Instant creationTime, Instant modificationTime) throws IOException {
        Path path = Paths.get(TEST_DIRECTORY_PATH, relativePath);
        Files.createDirectories(path);

        if (creationTime != null || modificationTime != null) {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);

            if (creationTime != null) {
                Files.setAttribute(path, "creationTime", FileTime.from(creationTime));
            }

            if (modificationTime != null) {
                Files.setLastModifiedTime(path, FileTime.from(modificationTime));
            }
        }
    }

    @Test
    public void testBasicDeletion() throws IOException {
        // Create a simple structure
        createFileWithTimes("file1.txt", null, null);
        createFileWithTimes("file2.log", null, null);
        createDirWithTimes("dir1", null, null);
        createFileWithTimes("dir1/file3.txt", null, null);

        // Delete everything
        deleteFilesFolders.action(
                TEST_DIRECTORY_PATH, PROCESS_ALL_TYPES, true, 0,
                THRESHOLD_UNIT_DAY, THRESHOLD_CRITERIA_CREATION,
                false, "", false, "", ERROR_IGNORE);

        // Base directory should still exist, but be empty
        File testDirectory = new File(TEST_DIRECTORY_PATH);
        Assert.assertTrue(testDirectory.exists(), "Base directory should still exist");
        Assert.assertEquals(countFilesAndDirs(testDirectory), 0, "Base directory should be empty");
    }

    @Test
    public void testThresholdAgeFiltering() throws IOException {
        // Current time
        Instant now = Instant.now();

        // Create files with different ages
        createFileWithTimes("recent.txt", now, now);
        createFileWithTimes("old.txt", now.minus(3, ChronoUnit.DAYS), now.minus(3, ChronoUnit.DAYS));
        createDirWithTimes("recentDir", now, now);
        createDirWithTimes("oldDir", now.minus(3, ChronoUnit.DAYS), now.minus(3, ChronoUnit.DAYS));
        createFileWithTimes("recentDir/file.txt", now, now);
        createFileWithTimes("oldDir/file.txt", now.minus(3, ChronoUnit.DAYS), now.minus(3, ChronoUnit.DAYS));

        // Delete files older than 2 days
        deleteFilesFolders.action(
                TEST_DIRECTORY_PATH, PROCESS_ALL_TYPES, true, 2,
                THRESHOLD_UNIT_DAY, THRESHOLD_CRITERIA_CREATION,
                false, "", false, "", ERROR_IGNORE);

        // Check what remains
        assertPathsExist("recent.txt", "recentDir", "recentDir/file.txt");
        assertPathsDoNotExist("old.txt", "oldDir", "oldDir/file.txt");
    }

    @Test
    public void testModificationTimeFiltering() throws IOException {
        // Current time
        Instant now = Instant.now();
        Instant oldCreation = now.minus(5, ChronoUnit.DAYS);
        Instant oldModification = now.minus(3, ChronoUnit.DAYS);
        Instant recentModification = now.minus(1, ChronoUnit.DAYS);

        // Create files with different creation and modification times
        createFileWithTimes("recentlyModified.txt", oldCreation, recentModification);
        createFileWithTimes("oldModified.txt", oldCreation, oldModification);

        // Delete files whose modification time is older than 2 days
        deleteFilesFolders.action(
                TEST_DIRECTORY_PATH, PROCESS_ALL_TYPES, true, 2,
                THRESHOLD_UNIT_DAY, THRESHOLD_CRITERIA_MODIFICATION,
                false, "", false, "", ERROR_IGNORE);

        // Check what remains (creation time is not considered here)
        assertPathsExist("recentlyModified.txt");
        assertPathsDoNotExist("oldModified.txt");
    }

    @Test
    public void testDifferentTimeUnits() throws IOException {
        // Current time
        Instant now = Instant.now();

        // Create files with different ages in minutes
        createFileWithTimes("recent.txt", now.minus(30, ChronoUnit.MINUTES), null);
        createFileWithTimes("old.txt", now.minus(90, ChronoUnit.MINUTES), null);

        // Delete files older than 60 minutes
        deleteFilesFolders.action(
                TEST_DIRECTORY_PATH, PROCESS_ALL_TYPES, true, 60,
                THRESHOLD_UNIT_MINUTE, THRESHOLD_CRITERIA_CREATION,
                false, "", false, "", ERROR_IGNORE);

        // Check what remains
        assertPathsExist("recent.txt");
        assertPathsDoNotExist("old.txt");
    }

    @Test
    public void testRecursiveVsNonRecursive() throws IOException {
        // Create nested structure
        createFileWithTimes("file1.txt", null, null);
        createDirWithTimes("dir1", null, null);
        createFileWithTimes("dir1/file2.txt", null, null);
        createDirWithTimes("dir1/subdir", null, null);
        createFileWithTimes("dir1/subdir/file3.txt", null, null);

        // Delete with non-recursive option
        deleteFilesFolders.action(
                TEST_DIRECTORY_PATH, PROCESS_ONLY_FILE_TYPE, false, 0,
                THRESHOLD_UNIT_DAY, THRESHOLD_CRITERIA_CREATION,
                false, "", false, "", ERROR_IGNORE);

        // Only top-level files should be deleted
        assertPathsDoNotExist("file1.txt");
        assertPathsExist("dir1", "dir1/file2.txt", "dir1/subdir", "dir1/subdir/file3.txt");
    }

    @Test
    public void testFilesOnlyOption() throws IOException {
        // Create structure
        createFileWithTimes("file1.txt", null, null);
        createDirWithTimes("dir1", null, null);
        createFileWithTimes("dir1/file2.txt", null, null);

        // Delete files only
        deleteFilesFolders.action(
                TEST_DIRECTORY_PATH, PROCESS_ONLY_FILE_TYPE, true, 0,
                THRESHOLD_UNIT_DAY, THRESHOLD_CRITERIA_CREATION,
                false, "", false, "", ERROR_IGNORE);

        // Files should be deleted, directories preserved
        assertPathsDoNotExist("file1.txt", "dir1/file2.txt");
        assertPathsExist("dir1");
    }

    @Test
    public void testSkipFilePattern() throws IOException {
        // Create structure with different file types
        createFileWithTimes("doc1.txt", null, null);
        createFileWithTimes("doc2.pdf", null, null);
        createFileWithTimes("image.jpg", null, null);
        createDirWithTimes("dir1", null, null);
        createFileWithTimes("dir1/doc3.txt", null, null);
        createFileWithTimes("dir1/image2.jpg", null, null);

        // Delete everything except .jpg files
        deleteFilesFolders.action(
                TEST_DIRECTORY_PATH, PROCESS_ALL_TYPES, true, 0,
                THRESHOLD_UNIT_DAY, THRESHOLD_CRITERIA_CREATION,
                false, "", true, ".*\\.jpg$", ERROR_IGNORE);

        // Check what remains
        assertPathsExist("image.jpg", "dir1", "dir1/image2.jpg");
        assertPathsDoNotExist("doc1.txt", "doc2.pdf", "dir1/doc3.txt");
    }

    @Test
    public void testSkipFolderPattern() throws IOException {
        // Create structure
        createFileWithTimes("file1.txt", null, null);
        createDirWithTimes("backups", null, null);
        createFileWithTimes("backups/backup1.txt", null, null);
        createDirWithTimes("data", null, null);
        createFileWithTimes("data/file2.txt", null, null);

        // Skip the "backups" directory
        String skipPattern = ".*[/\\\\]backups$"; // Works on both Windows and Unix

        deleteFilesFolders.action(
                TEST_DIRECTORY_PATH, PROCESS_ALL_TYPES, true, 0,
                THRESHOLD_UNIT_DAY, THRESHOLD_CRITERIA_CREATION,
                true, skipPattern, false, "", ERROR_IGNORE);

        // Check what remains
        assertPathsExist("backups", "backups/backup1.txt");
        assertPathsDoNotExist("file1.txt", "data", "data/file2.txt");
    }

    @Test
    public void testParentPreservation() throws IOException {
        // Create nested structure
        createDirWithTimes("dir1", null, null);
        createDirWithTimes("dir1/dir2", null, null);
        createDirWithTimes("dir1/dir2/dir3", null, null);
        createFileWithTimes("dir1/dir2/dir3/important.txt", null, null);
        createFileWithTimes("dir1/file1.txt", null, null);
        createFileWithTimes("dir1/dir2/file2.txt", null, null);
        createFileWithTimes("file0.txt", null, null);

        // Skip the important.txt file, which should preserve its parent directories
        deleteFilesFolders.action(
                TEST_DIRECTORY_PATH, PROCESS_ALL_TYPES, true, 0,
                THRESHOLD_UNIT_DAY, THRESHOLD_CRITERIA_CREATION,
                false, "", true, ".*important\\.txt$", ERROR_IGNORE);

        // Parent directories of important.txt should be preserved, but other files can be deleted
        assertPathsExist("dir1", "dir1/dir2", "dir1/dir2/dir3", "dir1/dir2/dir3/important.txt");
        assertPathsDoNotExist("file0.txt", "dir1/file1.txt", "dir1/dir2/file2.txt");
    }

    @Test
    public void testComplexNestedSkipping() throws IOException {
        // Create complex structure
        createDirWithTimes("project", null, null);
        createDirWithTimes("project/src", null, null);
        createDirWithTimes("project/build", null, null);
        createDirWithTimes("project/docs", null, null);
        createFileWithTimes("project/src/main.java", null, null);
        createFileWithTimes("project/src/utils.java", null, null);
        createFileWithTimes("project/build/output.log", null, null);
        createFileWithTimes("project/docs/manual.pdf", null, null);
        createFileWithTimes("project/docs/guide.txt", null, null);

        // Skip the src directory and PDF files
        deleteFilesFolders.action(
                TEST_DIRECTORY_PATH, PROCESS_ALL_TYPES, true, 0,
                THRESHOLD_UNIT_DAY, THRESHOLD_CRITERIA_CREATION,
                true, ".*[/\\\\]src$", true, ".*\\.pdf$", ERROR_IGNORE);

        // Check what remains
        assertPathsExist(
                "project", "project/src", "project/src/main.java", "project/src/utils.java",
                "project/docs", "project/docs/manual.pdf");
        assertPathsDoNotExist(
                "project/build", "project/build/output.log", "project/docs/guide.txt");
    }

    @Test
    public void testEdgeCaseEmptyDirectory() throws IOException {
        // Create empty base directory
        File testDirectory = new File(TEST_DIRECTORY_PATH);

        // Try to delete
        deleteFilesFolders.action(
                TEST_DIRECTORY_PATH, PROCESS_ALL_TYPES, true, 0,
                THRESHOLD_UNIT_DAY, THRESHOLD_CRITERIA_CREATION,
                false, "", false, "", ERROR_IGNORE);

        // Base directory should still exist
        Assert.assertTrue(testDirectory.exists(), "Base directory should still exist");
    }


    @Test
    public void testDeepNestedStructure() throws IOException {
        // Create a deeply nested structure
        for (int i = 1; i <= 10; i++) {
            String path = "level1";
            for (int j = 2; j <= i; j++) {
                path += "/level" + j;
            }
            createDirWithTimes(path, null, null);
            createFileWithTimes(path + "/file.txt", null, null);
        }

        // Create a file to skip in the deepest directory
        createFileWithTimes("level1/level2/level3/level4/level5/level6/level7/level8/level9/level10/important.dat", null, null);

        // Skip .dat files
        deleteFilesFolders.action(
                TEST_DIRECTORY_PATH, PROCESS_ALL_TYPES, true, 0,
                THRESHOLD_UNIT_DAY, THRESHOLD_CRITERIA_CREATION,
                false, "", true, ".*\\.dat$", ERROR_IGNORE);

        // Check the deepest file and its parent structure
        assertPathsExist(
                "level1", "level1/level2", "level1/level2/level3", "level1/level2/level3/level4",
                "level1/level2/level3/level4/level5", "level1/level2/level3/level4/level5/level6",
                "level1/level2/level3/level4/level5/level6/level7",
                "level1/level2/level3/level4/level5/level6/level7/level8",
                "level1/level2/level3/level4/level5/level6/level7/level8/level9",
                "level1/level2/level3/level4/level5/level6/level7/level8/level9/level10",
                "level1/level2/level3/level4/level5/level6/level7/level8/level9/level10/important.dat");

        // Other files should be deleted
        assertPathsDoNotExist(
                "level1/file.txt", "level1/level2/file.txt", "level1/level2/level3/file.txt",
                "level1/level2/level3/level4/file.txt", "level1/level2/level3/level4/level5/file.txt");
    }

    @Test
    public void testSiblingsWithSkippedItems() throws IOException {
        // Create structure with siblings
        createDirWithTimes("parent", null, null);
        createDirWithTimes("parent/child1", null, null);
        createDirWithTimes("parent/child2", null, null);
        createDirWithTimes("parent/child3", null, null);

        createFileWithTimes("parent/child1/normal1.txt", null, null);
        createFileWithTimes("parent/child1/important.dat", null, null);
        createFileWithTimes("parent/child2/normal2.txt", null, null);
        createFileWithTimes("parent/child3/normal3.txt", null, null);

        // Skip .dat files
        deleteFilesFolders.action(
                TEST_DIRECTORY_PATH, PROCESS_ALL_TYPES, true, 0,
                THRESHOLD_UNIT_DAY, THRESHOLD_CRITERIA_CREATION,
                false, "", true, ".*\\.dat$", ERROR_IGNORE);

        // Parent of skipped file should remain, but siblings can be deleted
        assertPathsExist(
                "parent", "parent/child1", "parent/child1/important.dat");

        assertPathsDoNotExist(
                "parent/child1/normal1.txt", "parent/child2", "parent/child2/normal2.txt",
                "parent/child3", "parent/child3/normal3.txt");
    }

    @Test
    public void testSpecialPathCharacters() throws IOException {
        // Create paths with special characters
        createDirWithTimes("folder with spaces", null, null);
        createFileWithTimes("folder with spaces/file-with-dashes.txt", null, null);
        createDirWithTimes("folder_with_underscores", null, null);
        createFileWithTimes("folder_with_underscores/file.txt", null, null);

        // Test special character pattern matching
        deleteFilesFolders.action(
                TEST_DIRECTORY_PATH, PROCESS_ALL_TYPES, true, 0,
                THRESHOLD_UNIT_DAY, THRESHOLD_CRITERIA_CREATION,
                true, ".*with spaces$", false, "", ERROR_IGNORE);

        // Check results
        assertPathsExist("folder with spaces", "folder with spaces/file-with-dashes.txt");
        assertPathsDoNotExist("folder_with_underscores", "folder_with_underscores/file.txt");
    }

    @Test
    public void testNonExistentBaseDirectory() {
        // Try to delete from a non-existent directory
        try {
            deleteFilesFolders.action(
                    TEST_DIRECTORY_PATH + "nonexistent/", PROCESS_ALL_TYPES, true, 0,
                    THRESHOLD_UNIT_DAY, THRESHOLD_CRITERIA_CREATION,
                    false, "", false, "", ERROR_THROW);
            Assert.fail("Expected BotCommandException was not thrown");
        } catch (BotCommandException e) {
            // Expected
            Assert.assertTrue(e.getMessage().contains("does not exist"));
        }
    }

    @Test
    public void testInvalidPattern() throws IOException {
        // Create file structure
        createFileWithTimes("file1.txt", null, null);

        // Try to use invalid regex pattern
        try {
            deleteFilesFolders.action(
                    TEST_DIRECTORY_PATH, PROCESS_ALL_TYPES, true, 0,
                    THRESHOLD_UNIT_DAY, THRESHOLD_CRITERIA_CREATION,
                    false, "", true, "[[invalid", ERROR_THROW);
            Assert.fail("Expected exception was not thrown");
        } catch (Exception e) {
            // Expected
        }
    }

    @Test
    public void testSymbolicLinks() throws IOException {
        // Only test on systems that support symbolic links
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            // Create structure
            createDirWithTimes("original", null, null);
            createFileWithTimes("original/file.txt", null, null);

            // Create symbolic link
            Path original = Paths.get(TEST_DIRECTORY_PATH, "original");
            Path link = Paths.get(TEST_DIRECTORY_PATH, "link");
            Files.createSymbolicLink(link, original);

            // Test deletion with symlinks
            deleteFilesFolders.action(
                    TEST_DIRECTORY_PATH, PROCESS_ALL_TYPES, true, 0,
                    THRESHOLD_UNIT_DAY, THRESHOLD_CRITERIA_CREATION,
                    false, "", false, "", ERROR_IGNORE);

            // Check results - original content should be deleted, but not symlink itself
            assertPathsDoNotExist("original", "original/file.txt");
            // The symlink might still exist but point to nothing
        }
    }

    @Test
    public void testFileLocking() throws IOException {
        // Create files
        createFileWithTimes("file1.txt", null, null);

        // This is a simplified test as actual locking depends on OS
        // In a real scenario, we would open the file with exclusive lock

        deleteFilesFolders.action(
                TEST_DIRECTORY_PATH, PROCESS_ALL_TYPES, true, 0,
                THRESHOLD_UNIT_DAY, THRESHOLD_CRITERIA_CREATION,
                false, "", false, "", ERROR_IGNORE);

        // Should still work with ERROR_IGNORE
        assertPathsDoNotExist("file1.txt");
    }

    @Test
    public void testAbsolutePathsInSkipPatterns() throws IOException {
        // Create structure
        String filePath = "skipMe.txt";
        createFileWithTimes(filePath, null, null);

        // Get absolute path
        File file = new File(TEST_DIRECTORY_PATH + filePath);
        String absolutePath = file.getAbsolutePath();
        String escapedPath = absolutePath.replace("\\", "\\\\"); // Escape backslashes for regex

        // Skip using absolute path pattern
        deleteFilesFolders.action(
                TEST_DIRECTORY_PATH, PROCESS_ALL_TYPES, true, 0,
                THRESHOLD_UNIT_DAY, THRESHOLD_CRITERIA_CREATION,
                false, "", true, escapedPath, ERROR_IGNORE);

        // File should be skipped
        assertPathsExist(filePath);
    }

    @Test
    public void testLargeNumberOfFiles() throws IOException {
        // Create a large number of files
        for (int i = 0; i < 100; i++) {
            createFileWithTimes("file" + i + ".txt", null, null);
        }

        // Create one special file to skip
        createFileWithTimes("special.dat", null, null);

        // Skip .dat files
        deleteFilesFolders.action(
                TEST_DIRECTORY_PATH, PROCESS_ALL_TYPES, true, 0,
                THRESHOLD_UNIT_DAY, THRESHOLD_CRITERIA_CREATION,
                false, "", true, ".*\\.dat$", ERROR_IGNORE);

        // Special file should remain, all others should be deleted
        assertPathsExist("special.dat");

        // Check a sample of the other files
        assertPathsDoNotExist("file0.txt", "file50.txt", "file99.txt");
    }

    @Test
    public void testMultipleSkipPatternsScenario() throws IOException {
        // Create structure
        createFileWithTimes("doc.txt", null, null);
        createFileWithTimes("image.jpg", null, null);
        createFileWithTimes("data.csv", null, null);
        createDirWithTimes("logs", null, null);
        createFileWithTimes("logs/server.log", null, null);

        // We would need multiple patterns - demonstrate with combined pattern
        String filePattern = ".*(\\.(jpg|csv)$|logs.*)";

        deleteFilesFolders.action(
                TEST_DIRECTORY_PATH, PROCESS_ALL_TYPES, true, 0,
                THRESHOLD_UNIT_DAY, THRESHOLD_CRITERIA_CREATION,
                false, "", true, filePattern, ERROR_IGNORE);

        // Check results
        assertPathsExist("image.jpg", "data.csv", "logs", "logs/server.log");
        assertPathsDoNotExist("doc.txt");
    }
}