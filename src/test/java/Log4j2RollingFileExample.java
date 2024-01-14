import com.automationanywhere.botcommand.data.Value;
import com.automationanywhere.botcommand.data.impl.*;
import com.automationanywhere.botcommand.data.model.Schema;
import com.automationanywhere.botcommand.data.model.table.Row;
import com.automationanywhere.botcommand.data.model.table.Table;
import com.automationanywhere.botcommand.utilities.logger.CustomHTMLLayout;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.config.builder.impl.DefaultConfigurationBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.logging.log4j.core.config.Configurator.initialize;

public class Log4j2RollingFileExample {
    public static void main(String[] args) {
        // Create a custom configuration
        ConfigurationBuilder<BuiltConfiguration> builder = new DefaultConfigurationBuilder<>();
        builder.setConfigurationName("RollingBuilder");
        builder.setPackages("com.automationanywhere.botcommand.utilities.logger");

        // Create a rolling file appender
        LayoutComponentBuilder layoutBuilder = builder.newLayout("CustomHTMLLayout")
                .addAttribute("charset", "UTF-8");

        ComponentBuilder<?> triggeringPolicy = builder
                .newComponent("Policies")
                .addComponent(builder.newComponent("SizeBasedTriggeringPolicy")
                        .addAttribute("size", "10M"));

        // Create the DefaultRolloverStrategy separately
        ComponentBuilder<?> rolloverStrategy = builder.newComponent("DefaultRolloverStrategy")
                .addAttribute("fileIndex", "nomax");

        AppenderComponentBuilder appenderBuilder = builder.newAppender("rolling", "RollingFile")
                .addAttribute("fileName", "target/rolling.html")
                .addAttribute("append", "true")
                .addAttribute("filePattern", "target/%d{yyyy-MMM-dd}_%i.html")
                .add(layoutBuilder)
                .addComponent(rolloverStrategy)
                .addComponent(triggeringPolicy);

        builder.add(appenderBuilder);

        // Create the new logger
        builder.add(builder.newLogger("TestLogger", Level.DEBUG)
                .add(builder.newAppenderRef("rolling")));

        // Initialize the LoggerContext and get the logger
        try (LoggerContext ctx = initialize(builder.build())) {
            Logger logger = LogManager.getLogger("TestLogger");

            // Log some messages
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
            //variables.put("my credential", new CredentialObject("my13$<)!-234<>"));

//        List<Schema> schemaList = new ArrayList<>();
//        schemaList.add(new Schema("Name"));
//        schemaList.add(new Schema("Age"));
//        schemaList.add(new Schema("company"));
//        List<Value> valuesList = new ArrayList<>();
//        valuesList.add(new StringValue("Sumit"));
//        valuesList.add(new StringValue("29"));
//        valuesList.add(new StringValue("Holcim"));
//        RecordValue rv = new RecordValue();
//        rv.set(new Record(schemaList, valuesList));
//        variables.put("recordValue", rv);

            // Test HTML generation
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

            Map<String, Object> infoObject = new HashMap<>();
            infoObject.put(CustomHTMLLayout.Columns.MESSAGE, "sample info message");
            infoObject.put(CustomHTMLLayout.Columns.SCREENSHOT, "C:\\Users\\Scream\\Pictures\\Screenshots\\Screenshot_20230301_131150.png");
            infoObject.put(CustomHTMLLayout.Columns.VARIABLES, variables);
            infoObject.put(CustomHTMLLayout.Columns.SOURCE, "\\Bots\\Departement\\Project\\Subtask");
            int i = 0;
            while (i < 1000) {
                logger.debug(debugObject);
                logger.info(infoObject);
                ++i;
            }

            // Shut down the logger context
            ctx.stop();
            logger.info("after stop");
        }
    }
}
