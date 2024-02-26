package device;

import com.automationanywhere.botcommand.actions.device.CreateFolders;
import com.automationanywhere.botcommand.data.Value;
import com.automationanywhere.botcommand.data.impl.DictionaryValue;
import com.automationanywhere.botcommand.data.impl.StringValue;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Sumit Kumar
 */

public class CreateFoldersTest {
    private static final String RELATIVE_PATH_TYPE = "RELATIVE";
    private static final String ABSOLUTE_PATH_TYPE = "ABSOLUTE";
    private static final String TEST_BASE_PATH = "src/test/target/temp/createFolderTest/";

    private CreateFolders createFolders;

    @BeforeMethod
    public void setUp() throws Exception {
        createFolders = new CreateFolders();
        // Ensuring the test base path is clean before each test
        FileUtils.forceMkdir(new File(TEST_BASE_PATH));
        FileUtils.cleanDirectory(new File(TEST_BASE_PATH));
    }

    @AfterMethod
    public void tearDown() throws Exception {
        // Clean up test artifacts after each test
        FileUtils.cleanDirectory(new File(TEST_BASE_PATH));
    }

    @Test
    public void testCreateFoldersRelativePath() {
        String pathType = RELATIVE_PATH_TYPE;
        String baseFolderPath = TEST_BASE_PATH + "/base";
        List<Value> folderPaths = List.of(
                new StringValue("folder1"),
                new StringValue("folder2/subfolder"),
                new StringValue("folder3/subfolder1/subfolder2/subfolder3")
        );
        Boolean overwriteExisting = false;
        Boolean appendToExistingDictionary = false;
        Map<String, Value> configDictionary = new HashMap<>();
        String suffix = "_path";
        String prefix = "test_";

        DictionaryValue output = createFolders.action(pathType, baseFolderPath, folderPaths, overwriteExisting,
                appendToExistingDictionary, configDictionary, suffix, prefix);

        assertFolderCreation(output, prefix, suffix);
    }

    private void assertFolderCreation(DictionaryValue output, String prefix, String suffix) {
        Map<String, Value> dictionary = output.get();
        for (Map.Entry<String, Value> entry : dictionary.entrySet()) {
            String folderName = entry.getKey().replace(prefix, "").replace(suffix, "");
            Path folderPath = Path.of(entry.getValue().get().toString());
            Assert.assertTrue(Files.exists(folderPath), "Folder does not exist: " + folderName);
        }
    }

    @Test
    public void testCreateFoldersAbsolutePath() {
        String pathType = ABSOLUTE_PATH_TYPE;
        String baseFolderPath = ""; // Not used in ABSOLUTE path mode
        List<Value> folderPaths = List.of(
                new StringValue(TEST_BASE_PATH + "/base/folder1"),
                new StringValue(TEST_BASE_PATH + "/base/folder2/subfolder"),
                new StringValue(TEST_BASE_PATH + "/base/folder3/subfolder1/subfolder2/subfolder3")
        );
        Boolean overwriteExisting = true;
        Boolean appendToExistingDictionary = false;
        Map<String, Value> configDictionary = new HashMap<>();
        String suffix = "_path";
        String prefix = "test_";

        DictionaryValue output = createFolders.action(pathType, baseFolderPath, folderPaths, overwriteExisting,
                appendToExistingDictionary, configDictionary, suffix, prefix);

        assertFolderCreation(output, prefix, suffix);
    }
}
