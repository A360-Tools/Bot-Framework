package device;

import com.automationanywhere.botcommand.actions.device.DeleteFilesFolders;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * @author Sumit Kumar
 */
public class DeleteFilesFoldersTest {

    @Test
    public void testDeleteFilesFolders() throws IOException {


        // Set up test parameters
        String inputFolderPath = "D:\\test";
        String selectMethod = "FILE";
        Boolean recursive = true;
        Number thresholdNumber = 1;
        String thresholdUnit = "DAY";
        Boolean skipFolders = false;
        String skipFolderPathPattern = ".*copy.*";
        Boolean skipFiles = true;
        String skipFilePathPattern = "^.+\\.txt$";
        String unableToDeleteBehavior = "ignore";

        // Create instance of DeleteFilesFolders
        DeleteFilesFolders deleteFilesFolders = new DeleteFilesFolders();

        // Call the action method with the test parameters
        deleteFilesFolders.action(
                inputFolderPath,
                selectMethod,
                recursive,
                thresholdNumber,
                thresholdUnit,
                skipFolders,
                skipFolderPathPattern,
                skipFiles,
                skipFilePathPattern,
                unableToDeleteBehavior
        );
    }
}