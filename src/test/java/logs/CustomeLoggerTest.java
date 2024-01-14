package logs;

import com.automationanywhere.botcommand.data.Value;
import com.automationanywhere.botcommand.data.impl.*;
import com.automationanywhere.botcommand.data.model.Schema;
import com.automationanywhere.botcommand.data.model.table.Row;
import com.automationanywhere.botcommand.data.model.table.Table;
import com.automationanywhere.botcommand.utilities.logger.CustomHTMLLayout;
import com.automationanywhere.botcommand.utilities.logger.CustomLogger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Sumit Kumar
 */
public class CustomeLoggerTest {
    @Test
    public void testCommonLogger() throws IOException {
        Logger logger;
        CustomLogger customLogger = new CustomLogger("CustomLogger", "D:\\test\\logs\\common.html",
                10, "D:\\test\\logs\\screenshot");
        logger = customLogger.getLogger();

        Map<String, Value> variables = new HashMap<>();

        variables.put("my string", new StringValue("Hello, World!"));
        variables.put("my number", new NumberValue(42));
        variables.put("my boolean", new BooleanValue(true));

        List<Value> listValue = new ArrayList<>();
        ListValue lv = new ListValue();
        listValue.add(new StringValue("Item 1"));
        listValue.add(new StringValue("Item 2"));
        listValue.add(new StringValue("Item 3"));
        lv.set(listValue);
        variables.put("my list", lv);

        Map<String, Value> innerMap = new HashMap<>();
        innerMap.put("my dic string", new StringValue("Inner Value"));
        innerMap.put("my list", lv);
        variables.put("my dictionary", new DictionaryValue(innerMap));
        List<Schema> schemalist = new ArrayList<>();
        List<Row> rowList = new ArrayList<>();
        schemalist.add(new Schema("col1 "));
        schemalist.add(new Schema("col2"));
        schemalist.add(new Schema("col 3"));

        rowList.add(new Row(new StringValue("r1c1"), new StringValue("r1c2"), new StringValue("r1c3")));
        rowList.add(new Row(new StringValue("r2c1"), new StringValue("r2c2"), new StringValue("r2c3")));
        rowList.add(new Row(new StringValue("r3c1"), new StringValue("r3c2"), new StringValue("r3c3")));
        rowList.add(new Row(new StringValue("r4c1"), new StringValue("r4c2"), new StringValue("r4c3")));
        rowList.add(new Row(new StringValue("r5c1"), new StringValue("r5c2"), new StringValue("r5c3")));
        rowList.add(new Row(new StringValue("r6c1"), new StringValue("r6c2"),
                new StringValue("r63            longer value with space at end    ")));
        rowList.add(new Row(new StringValue("r7c1"), new StringValue("r7c2"), new StringValue("r7c3")));
        TableValue tv = new TableValue();
        tv.set(new Table(schemalist, rowList));
        variables.put("my table", tv);
        
        Map<String, Object> debugObject = new HashMap<>();
        debugObject.put(CustomHTMLLayout.Columns.MESSAGE, "sample debug message");
        debugObject.put(CustomHTMLLayout.Columns.SCREENSHOT, "C:\\Users\\Scream\\Pictures\\Screenshots\\Screenshot 2023-03-07 142137.png");
        debugObject.put(CustomHTMLLayout.Columns.VARIABLES, variables);
        debugObject.put(CustomHTMLLayout.Columns.SOURCE, "\\Bots\\Departement\\Project\\Main");
        logger.info(debugObject);
        logger.warn(debugObject);
        logger.error(debugObject);
    }

    @Test
    public void testIndividualLogger() throws IOException {
        Map<Level, String> levelFilePathMap = new HashMap<>();
        levelFilePathMap.put(Level.INFO, "D:\\test\\logs\\info.html");
        levelFilePathMap.put(Level.ERROR, "D:\\test\\logs\\error.html");
        levelFilePathMap.put(Level.WARN, "D:\\test\\logs\\warn.html");
        Logger logger;
        CustomLogger customLogger = new CustomLogger("CustomLogger", levelFilePathMap,
                1, "D:\\test\\logs\\screenshot");

        logger = customLogger.getLogger();

        Map<String, Value> variables = new HashMap<>();

        variables.put("my string", new StringValue("Hello, World!"));
        variables.put("my number", new NumberValue(42));
        variables.put("my boolean", new BooleanValue(true));
        Map<String, Object> debugObject = new HashMap<>();
        debugObject.put(CustomHTMLLayout.Columns.MESSAGE, "sample debug message");
        debugObject.put(CustomHTMLLayout.Columns.SCREENSHOT, "C:\\Users\\Scream\\Pictures\\Screenshots\\Screenshot 2023-03-07 142137.png");
        debugObject.put(CustomHTMLLayout.Columns.VARIABLES, variables);
        debugObject.put(CustomHTMLLayout.Columns.SOURCE, "\\Bots\\Departement\\Project\\Main");

        int i = 0;
        while (i < 1000) {
            logger.warn(debugObject);
            logger.info(debugObject);
            logger.error(debugObject);
            ++i;
        }
    }
}
