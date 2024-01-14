package device;

/**
 * @author Sumit Kumar
 */

import com.automationanywhere.botcommand.actions.device.CreateFolders;
import com.automationanywhere.botcommand.data.Value;
import com.automationanywhere.botcommand.data.impl.DictionaryValue;
import com.automationanywhere.botcommand.data.impl.StringValue;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateFoldersTest {

    @Test
    public void testCreateFoldersRelativePath() {
        CreateFolders createFolders = new CreateFolders();
        String pathType = "relative";
        String baseFolderPath = "D:\\test\\base";
        List<Value> folderPaths = List.of(
                new StringValue("folder1"),
                new StringValue("folder2/subfolder"),
                new StringValue("folder3/subfolder1/subfolder2/subfolder/3")
        );
        Boolean overwriteExisting = false;
        Boolean appendToExistingDictionary = true;
        Map<String, Value> configDictionary = null;
        String suffix = "_path";
        String prefix = "";

        // Act
        DictionaryValue output = createFolders.action(pathType, baseFolderPath, folderPaths, overwriteExisting,
                appendToExistingDictionary,
                configDictionary, suffix, prefix);

        Map<String, Value> dictionary = output.get();
        for (String key : dictionary.keySet()) {
            System.out.println("key : " + key + " | value: " + dictionary.get(key).get());
        }
        Assert.assertTrue(output.get().containsKey(prefix + "folder1" + suffix));
        Assert.assertTrue(output.get().containsKey(prefix + "subfolder" + suffix));
        Assert.assertTrue(output.get().containsKey(prefix + "3" + suffix));
    }

    @Test
    public void testCreateFoldersAbsolutePath() {
        CreateFolders createFolders = new CreateFolders();
        String pathType = "absolute";
        String baseFolderPath = "";
        List<Value> folderPaths = List.of(
                new StringValue("D:\\test\\base\\folder1"),
                new StringValue("D:\\test\\base\\folder2\\subfolder"),
                new StringValue("D:\\test\\base\\folder3\\subfolder1\\subfolder2\\subfolder\\3")
        );
        Boolean overwriteExisting = true;
        Boolean appendToExistingDictionary = true;
        Map<String, Value> configDictionary = new HashMap<>();
        String suffix = "_path";
        String prefix = "";

        // Act
        DictionaryValue output = createFolders.action(pathType, baseFolderPath, folderPaths, overwriteExisting,
                appendToExistingDictionary,
                configDictionary, suffix, prefix);

        Map<String, Value> dictionary = output.get();
        for (String key : dictionary.keySet()) {
            System.out.println("key : " + key + " | value: " + dictionary.get(key).get());
        }
        Assert.assertTrue(output.get().containsKey(prefix + "folder1" + suffix));
        Assert.assertTrue(output.get().containsKey(prefix + "subfolder" + suffix));
        Assert.assertTrue(output.get().containsKey(prefix + "3" + suffix));
    }
}
