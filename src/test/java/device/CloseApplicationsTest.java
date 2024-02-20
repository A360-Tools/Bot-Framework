package device;

import com.automationanywhere.botcommand.actions.device.CloseApplications;
import com.automationanywhere.botcommand.data.Value;
import com.automationanywhere.botcommand.data.impl.StringValue;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

/**
 * @author Sumit Kumar
 */
@Test
public class CloseApplicationsTest {

    @Test
    public void action_ClosesProcessesSuccessfully() {
        // Arrange
        CloseApplications closeApplications = new CloseApplications();
        List<Value> taskList = Arrays.asList(new StringValue("Excel"), new StringValue("Notepad"));
        closeApplications.action(taskList);
    }
}