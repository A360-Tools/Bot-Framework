package device;

import com.automationanywhere.botcommand.actions.device.DeleteFilesFolders;
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
import java.util.Collection;

/**
 * @author Sumit Kumar
 */

public class DeleteFilesFoldersTest {
    private static final String TEST_DIRECTORY_PATH = "src/test/target/temp/deleteFolderTest/";
    private static final String PROCESS_ALL_TYPES = "ALL";
    private static final String PROCESS_ONLY_FILE_TYPE = "FILE";
    private static final String THRESHOLD_UNIT_DAY = "DAY";
    private static final String ERROR_IGNORE = "IGNORE";
    private static final String THRESHOLD_CRITERIA_CREATION = "CREATION";
    private static final String THRESHOLD_CRITERIA_MODIFICATION = "MODIFICATION";
    private DeleteFilesFolders deleteFilesFolders;

    @BeforeMethod
    public void setUp() throws IOException {
        deleteFilesFolders = new DeleteFilesFolders();
        FileUtils.forceMkdir(new File(TEST_DIRECTORY_PATH));
        FileUtils.cleanDirectory(new File(TEST_DIRECTORY_PATH));
        prepareTestEnvironment();
    }

    private void prepareTestEnvironment() throws IOException {
        Path testDirectory = Path.of(TEST_DIRECTORY_PATH);
        // Create some test files and directories
        Files.createFile(testDirectory.resolve("testFile1.txt"));
        Path subDirectory = Files.createDirectory(testDirectory.resolve("subDirectory"));
        Files.createFile(subDirectory.resolve("testFile2.txt"));
    }

    @AfterMethod
    public void tearDown() throws IOException {
        cleanupTestEnvironment();
    }

    // Additional test methods for other scenarios...

    private void cleanupTestEnvironment() throws IOException {
        FileUtils.deleteDirectory(new File(TEST_DIRECTORY_PATH));
    }

    @Test
    public void testDeleteFilesAndDirectories() {
        Boolean recursive = Boolean.TRUE;
        Number thresholdNumber = 0;//all files and folders will match
        Boolean skipFolders = false;
        String skipFolderPathPattern = "";
        Boolean skipFiles = false;
        String skipFilePathPattern = "";

        deleteFilesFolders.action(TEST_DIRECTORY_PATH, PROCESS_ALL_TYPES, recursive, thresholdNumber,
                THRESHOLD_UNIT_DAY, THRESHOLD_CRITERIA_CREATION,
                skipFolders, skipFolderPathPattern, skipFiles, skipFilePathPattern, ERROR_IGNORE);

        File testDirectory = new File(TEST_DIRECTORY_PATH);
        Assert.assertTrue(testDirectory.exists());
        Collection<File> filesAndDirs = FileUtils.listFilesAndDirs(testDirectory,
                TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        filesAndDirs.remove(testDirectory);
        Assert.assertEquals(filesAndDirs.size(), 0, "Test directory should be " +
                "empty after deletion");
    }

    @Test
    public void testNotDeleteFilesAndDirectories() {
        Boolean recursive = Boolean.TRUE;
        Number thresholdNumber = 1;//no files/folders will match as they are created now during testing
        Boolean skipFolders = false;
        String skipFolderPathPattern = "";
        Boolean skipFiles = false;
        String skipFilePathPattern = "";

        deleteFilesFolders.action(TEST_DIRECTORY_PATH, PROCESS_ALL_TYPES, recursive, thresholdNumber,
                THRESHOLD_UNIT_DAY, THRESHOLD_CRITERIA_CREATION,
                skipFolders, skipFolderPathPattern, skipFiles, skipFilePathPattern, ERROR_IGNORE);

        File testDirectory = new File(TEST_DIRECTORY_PATH);
        Assert.assertTrue(testDirectory.exists());
        Collection<File> filesAndDirs = FileUtils.listFilesAndDirs(testDirectory,
                TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        filesAndDirs.remove(testDirectory);
        Assert.assertEquals(filesAndDirs.size(), 3, "Test directory should be " +
                "intact as threshold does not match");
    }

    @Test
    public void testSkipDirectoryDeletion() {
        Boolean recursive = Boolean.TRUE;
        Number thresholdNumber = 0;//all files and folders will match
        Boolean skipFolders = true;
        String skipFolderPathPattern = ".*\\\\subDirectory";
        Boolean skipFiles = false;
        String skipFilePathPattern = "";

        deleteFilesFolders.action(TEST_DIRECTORY_PATH, PROCESS_ALL_TYPES, recursive, thresholdNumber,
                THRESHOLD_UNIT_DAY, THRESHOLD_CRITERIA_CREATION,
                skipFolders, skipFolderPathPattern, skipFiles, skipFilePathPattern, ERROR_IGNORE);

        File testDirectory = new File(TEST_DIRECTORY_PATH);
        Assert.assertTrue(testDirectory.exists());
        Collection<File> filesAndDirs = FileUtils.listFilesAndDirs(testDirectory,
                TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        filesAndDirs.remove(testDirectory);
        Assert.assertEquals(filesAndDirs.size(), 2, "Test directory must contain skipped directory and its children");
    }

    @Test
    public void testSkipFileDeletion() {
        Boolean recursive = Boolean.TRUE;
        Number thresholdNumber = 0;//all files and folders will match
        Boolean skipFolders = false;
        String skipFolderPathPattern = "";
        Boolean skipFiles = true;
        String skipFilePathPattern = ".*\\.txt$";

        deleteFilesFolders.action(TEST_DIRECTORY_PATH, PROCESS_ALL_TYPES, recursive, thresholdNumber,
                THRESHOLD_UNIT_DAY, THRESHOLD_CRITERIA_CREATION,
                skipFolders, skipFolderPathPattern, skipFiles, skipFilePathPattern, ERROR_IGNORE);

        File testDirectory = new File(TEST_DIRECTORY_PATH);
        Assert.assertTrue(testDirectory.exists());
        Collection<File> filesAndDirs = FileUtils.listFilesAndDirs(testDirectory,
                TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        filesAndDirs.remove(testDirectory);
        Assert.assertEquals(filesAndDirs.size(), 3, "Test directory must contain directory and its skipped children");
    }

    @Test
    public void testSkipFileDeletionSingleFolder() {
        Boolean recursive = Boolean.TRUE;
        Number thresholdNumber = 0;//all files and folders will match
        Boolean skipFolders = false;
        String skipFolderPathPattern = "";
        Boolean skipFiles = true;
        String skipFilePathPattern = ".*\\\\subDirectory\\\\.*\\.txt$";

        deleteFilesFolders.action(TEST_DIRECTORY_PATH, PROCESS_ALL_TYPES, recursive, thresholdNumber,
                THRESHOLD_UNIT_DAY, THRESHOLD_CRITERIA_CREATION,
                skipFolders, skipFolderPathPattern, skipFiles, skipFilePathPattern, ERROR_IGNORE);

        File testDirectory = new File(TEST_DIRECTORY_PATH);
        Assert.assertTrue(testDirectory.exists());
        Collection<File> filesAndDirs = FileUtils.listFilesAndDirs(testDirectory,
                TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        filesAndDirs.remove(testDirectory);
        Assert.assertEquals(filesAndDirs.size(), 2, "Test directory must contain directory and its skipped children");
    }
}
