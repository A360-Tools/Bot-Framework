package device;

import com.automationanywhere.botcommand.actions.device.CloseApplications;
import com.automationanywhere.botcommand.data.Value;
import com.automationanywhere.botcommand.data.impl.StringValue;

import java.util.Arrays;
import java.util.List;

/**
 * @author Sumit Kumar
 */
public class CloseApplicationsTest {

    //Disabled to avoid data loss, should be mocked or triggered manually
    //@Test
    public void action_ClosesProcessesSuccessfully() {
        CloseApplications closeApplications = new CloseApplications();
        List<Value> taskList = Arrays.asList(new StringValue("Excel"), new StringValue("Notepad"));
        closeApplications.action(taskList);
    }
}