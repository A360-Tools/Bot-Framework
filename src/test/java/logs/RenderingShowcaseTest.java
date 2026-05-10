package logs;

import com.automationanywhere.botcommand.actions.logs.LogMessage;
import com.automationanywhere.botcommand.actions.logs.StopLoggerSession;
import com.automationanywhere.botcommand.data.Value;
import com.automationanywhere.botcommand.data.impl.BooleanValue;
import com.automationanywhere.botcommand.data.impl.DateTimeValue;
import com.automationanywhere.botcommand.data.impl.DictionaryValue;
import com.automationanywhere.botcommand.data.impl.ListValue;
import com.automationanywhere.botcommand.data.impl.NumberValue;
import com.automationanywhere.botcommand.data.impl.StringValue;
import com.automationanywhere.botcommand.data.impl.TableValue;
import com.automationanywhere.botcommand.data.model.Schema;
import com.automationanywhere.botcommand.data.model.table.Row;
import com.automationanywhere.botcommand.data.model.table.Table;
import com.automationanywhere.botcommand.utilities.logger.CustomLogger;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * End-to-end rendering showcase tests. These produce variable and log
 * HTML artifacts that exercise every branch in HTMLGenerator and the
 * matching templates: short and long scalars, long unbroken strings,
 * multi-line text, JSON / code blocks, HTML-special characters, Unicode,
 * primitive types, simple and nested lists, dictionaries, and small /
 * multi-line-cell / wide tables. Run them and open the printed paths
 * to visually validate the templates under realistic data shapes.
 */
@SuppressWarnings({"SpellCheckingInspection", "GrazieInspection"})
public class RenderingShowcaseTest {

    private static final String LEVEL_INFO = "INFO";
    private static final String LEVEL_WARN = "WARN";
    private static final String LEVEL_ERROR = "ERROR";
    private static final String LOG_VARIABLE = "YES";
    private static final String DO_NOT_LOG_VARIABLE = "NO";

    @Test
    public void testExhaustiveVariablesShowcase() throws Exception {
        String testPath = "src/test/target/test-artifacts/showcase-variables-" + System.currentTimeMillis() + "/";
        Files.createDirectories(Paths.get(testPath));

        String logFilePath = testPath + "showcase.html";
        CustomLogger customLogger = new CustomLogger("ShowcaseVariables_" + UUID.randomUUID(),
                                                      logFilePath, 10);

        LogMessage logMessage = new LogMessage();
        logMessage.setTestBotUri("Automation Anywhere/bots/test/showcase-variables");

        Map<String, Value> sourceMap = new LinkedHashMap<>();

        // Short scalars
        sourceMap.put("Short String", new StringValue("Hello"));
        sourceMap.put("Empty String", new StringValue(""));
        sourceMap.put("Number Zero", new NumberValue(0));
        sourceMap.put("Number Large", new NumberValue(123456789.987654));
        sourceMap.put("Number Negative", new NumberValue(-2.5));
        sourceMap.put("Boolean True", new BooleanValue(true));
        sourceMap.put("Boolean False", new BooleanValue(false));
        sourceMap.put("DateTime Now", new DateTimeValue(ZonedDateTime.now()));
        sourceMap.put("Null Value", null);

        // Long single-line text (no newlines, normal word breaks)
        sourceMap.put("Long Single Line", new StringValue(
            "This is a single long line of text without any newlines. It is intentionally long enough to stress how the renderer handles wrapping versus explicit line breaks. " +
            "It continues with more content to ensure the line is well over two hundred characters and forces the layout engine to make decisions about where to wrap inside its own column."));

        // Very long URL (long but with break points at /, ., -)
        sourceMap.put("Long URL", new StringValue(
            "https://very-long-subdomain.example.company.org/path/with/many/segments/" +
            "and-very-long-identifier-1234567890123456789012345/profile/details.html" +
            "?query=value&another=value&yet-another=value&final-token=abcdef0123456789"));

        // Unbreakable long token (no spaces, no break opportunities)
        sourceMap.put("Unbreakable Token", new StringValue(
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        // Multiline JSON-like content
        sourceMap.put("JSON Multiline", new StringValue(
            "{\n" +
            "  \"id\": 42,\n" +
            "  \"name\": \"Sample Record\",\n" +
            "  \"tags\": [\n" +
            "    \"alpha\",\n" +
            "    \"beta\",\n" +
            "    \"gamma\"\n" +
            "  ],\n" +
            "  \"nested\": {\n" +
            "    \"key\": \"value\",\n" +
            "    \"count\": 7\n" +
            "  }\n" +
            "}"));

        // Multi-paragraph text (newlines + blank lines)
        sourceMap.put("Multi Paragraph", new StringValue(
            "First paragraph describes the situation. It can be a long sentence that wraps around when displayed in a narrow column and demonstrates how soft wrapping interacts with explicit newlines.\n" +
            "\n" +
            "Second paragraph follows after a blank line. This pattern is common in formatted text and the renderer should preserve the blank line as visible vertical space.\n" +
            "\n" +
            "Final paragraph concludes the message and demonstrates that triple-newline handling works correctly for longer content."));

        // Code block with indentation
        sourceMap.put("Code Block", new StringValue(
            "function processData(input) {\n" +
            "    if (!input || input.length === 0) {\n" +
            "        return null;\n" +
            "    }\n" +
            "    return input\n" +
            "        .map(item => item.value)\n" +
            "        .filter(value => value != null);\n" +
            "}"));

        // HTML special characters that must be escaped
        sourceMap.put("HTML Special Chars", new StringValue(
            "Tags <script>alert('xss')</script> & ampersands & quotes \"double\" 'single' should all render as escaped text rather than markup"));

        // Unicode and emoji
        sourceMap.put("Unicode Mixed", new StringValue(
            "ASCII / 日本語 / العربية / Ελληνικά / 🎉 emoji / mathematical symbols ∑ ∫ ∂ √ ∞"));

        // Mixed: long line followed by newline followed by more text
        sourceMap.put("Mixed Long And Newlines", new StringValue(
            "First a long line of text with no breaks until we deliberately hit a newline character right after this final word.\n" +
            "Then a second line that is also reasonably long and should wrap independently of the first line above.\n" +
            "And finally a short third line."));

        // Simple flat list of strings
        ListValue flatStringList = new ListValue();
        flatStringList.set(Arrays.asList(
            new StringValue("apple"),
            new StringValue("banana"),
            new StringValue("cherry"),
            new StringValue("date with a much longer name that exercises wrapping inside a list item"),
            new StringValue("elderberry")
        ));
        sourceMap.put("Flat String List", flatStringList);

        // Mixed-type list
        ListValue mixedList = new ListValue();
        mixedList.set(Arrays.asList(
            new StringValue("first string"),
            new NumberValue(42),
            new BooleanValue(true),
            new StringValue("a multi-line entry\nwith its own newline\nspanning three lines")
        ));
        sourceMap.put("Mixed-Type List", mixedList);

        // Nested list (list of dictionaries)
        ListValue nestedList = new ListValue();
        Map<String, Value> dict1 = new LinkedHashMap<>();
        dict1.put("name", new StringValue("Alice"));
        dict1.put("score", new NumberValue(85));
        dict1.put("active", new BooleanValue(true));
        Map<String, Value> dict2 = new LinkedHashMap<>();
        dict2.put("name", new StringValue("Bob"));
        dict2.put("score", new NumberValue(92));
        dict2.put("active", new BooleanValue(false));
        Map<String, Value> dict3 = new LinkedHashMap<>();
        dict3.put("name", new StringValue("Charlie"));
        dict3.put("score", new NumberValue(78));
        dict3.put("active", new BooleanValue(true));
        nestedList.set(Arrays.asList(
            new DictionaryValue(dict1),
            new DictionaryValue(dict2),
            new DictionaryValue(dict3)
        ));
        sourceMap.put("List Of Dictionaries", nestedList);

        // Dictionary with mixed types
        Map<String, Value> configDict = new LinkedHashMap<>();
        configDict.put("retries", new NumberValue(3));
        configDict.put("timeout_seconds", new NumberValue(30));
        configDict.put("debug", new BooleanValue(true));
        configDict.put("endpoint", new StringValue("https://api.example.com/v2/resource"));
        configDict.put("description", new StringValue("Multi-line dictionary value:\n  - first line\n  - second line"));
        sourceMap.put("Configuration Dictionary", new DictionaryValue(configDict));

        // Small 2-column table
        TableValue smallTable = new TableValue();
        smallTable.set(new Table(
            Arrays.asList(new Schema("Field"), new Schema("Value")),
            Arrays.asList(
                new Row(new StringValue("API Endpoint"), new StringValue("/api/v1/users")),
                new Row(new StringValue("HTTP Method"), new StringValue("GET")),
                new Row(new StringValue("Auth Type"), new StringValue("Bearer Token")),
                new Row(new StringValue("Timeout"), new StringValue("30s"))
            )
        ));
        sourceMap.put("Small Table", smallTable);

        // Table with multi-line cell values (newlines preserved per cell)
        TableValue multilineTable = new TableValue();
        multilineTable.set(new Table(
            Arrays.asList(new Schema("Step"), new Schema("Input"), new Schema("Output")),
            Arrays.asList(
                new Row(
                    new StringValue("Parse JSON"),
                    new StringValue("{\n  \"a\": 1,\n  \"b\": 2\n}"),
                    new StringValue("Map<String, Number> {\n  a -> 1,\n  b -> 2\n}")
                ),
                new Row(
                    new StringValue("Validate"),
                    new StringValue("Required fields:\n  - name\n  - email\n  - phone"),
                    new StringValue("Result: PASS\nWarnings: 0\nErrors: 0")
                ),
                new Row(
                    new StringValue("Transform"),
                    new StringValue("uppercase all string fields\ntrim whitespace\nconvert dates to ISO 8601"),
                    new StringValue("12 fields modified")
                )
            )
        ));
        sourceMap.put("Multi-Line Cells Table", multilineTable);

        // Wide table with 10 columns
        List<Schema> wideSchemas = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            wideSchemas.add(new Schema("Column " + i));
        }
        List<Row> wideRows = new ArrayList<>();
        for (int r = 1; r <= 5; r++) {
            Value[] cells = new Value[10];
            for (int c = 0; c < 10; c++) {
                cells[c] = new StringValue("R" + r + "C" + (c + 1));
            }
            wideRows.add(new Row(cells));
        }
        TableValue wideTable = new TableValue();
        wideTable.set(new Table(wideSchemas, wideRows));
        sourceMap.put("Wide Table", wideTable);

        // Tall table to exercise sticky thead during page scroll
        List<Row> tallRows = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            tallRows.add(new Row(
                new StringValue("ROW-" + String.format("%03d", i)),
                new StringValue("Item " + i),
                new StringValue(i % 2 == 0 ? "Active" : "Pending"),
                new StringValue("Description for item " + i + " with some additional context.")
            ));
        }
        TableValue tallTable = new TableValue();
        tallTable.set(new Table(
            Arrays.asList(new Schema("ID"), new Schema("Name"), new Schema("Status"), new Schema("Description")),
            tallRows
        ));
        sourceMap.put("Tall Table (30 rows)", tallTable);

        // Table with nested complex values inside cells
        ListValue nestedCellList = new ListValue();
        nestedCellList.set(Arrays.asList(new StringValue("alpha"), new StringValue("beta"), new StringValue("gamma")));
        Map<String, Value> nestedCellDict = new LinkedHashMap<>();
        nestedCellDict.put("score", new NumberValue(95));
        nestedCellDict.put("rank", new StringValue("A+"));
        TableValue nestedCellsTable = new TableValue();
        nestedCellsTable.set(new Table(
            Arrays.asList(new Schema("Identifier"), new Schema("Tags"), new Schema("Grade")),
            Arrays.asList(
                new Row(new StringValue("rec-001"), nestedCellList, new DictionaryValue(nestedCellDict)),
                new Row(new StringValue("rec-002"), new StringValue("simple"), new NumberValue(42))
            )
        ));
        sourceMap.put("Table With Nested Cells", nestedCellsTable);

        // Build entryList (LogMessage variant) with one richly nested entry
        List<Value> entryList = new ArrayList<>();
        Map<String, Value> richEntry = new LinkedHashMap<>();
        richEntry.put("NAME", new StringValue("Pipeline Configuration"));
        Map<String, Value> richDict = new LinkedHashMap<>();
        richDict.put("source", new StringValue("s3://bucket/raw-data/2026/"));
        richDict.put("destination", new StringValue("s3://bucket/processed/2026/"));
        richDict.put("steps", flatStringList);
        richDict.put("retry_policy", new DictionaryValue(configDict));
        richEntry.put("VALUE", new DictionaryValue(richDict));
        entryList.add(new DictionaryValue(richEntry));

        logMessage.action(
            customLogger,
            LEVEL_INFO,
            "Exhaustive variables showcase entry",
            false,
            LOG_VARIABLE,
            entryList,
            sourceMap
        );

        new StopLoggerSession().stop(customLogger);

        String variablesDir = testPath + "variables";
        File[] variableFiles = new File(variablesDir).listFiles((dir, name) -> name.endsWith(".html"));
        Assert.assertNotNull(variableFiles, "Variables directory should contain files");
        Assert.assertEquals(variableFiles.length, 1, "Exactly one variables file should be created");

        String variablesHtml = new String(Files.readAllBytes(variableFiles[0].toPath()));
        // Spot-check that representative variables, types, and rare characters survive
        Assert.assertTrue(variablesHtml.contains("Long URL"), "should include 'Long URL' name");
        Assert.assertTrue(variablesHtml.contains("JSON Multiline"), "should include 'JSON Multiline' name");
        Assert.assertTrue(variablesHtml.contains("Wide Table"), "should include 'Wide Table' name");
        Assert.assertTrue(variablesHtml.contains("Tall Table (30 rows)"), "should include 'Tall Table' name");
        Assert.assertTrue(variablesHtml.contains("Multi-Line Cells Table"), "should include multi-line cells table name");
        Assert.assertTrue(variablesHtml.contains("Pipeline Configuration"), "should include entryList variable name");
        Assert.assertTrue(variablesHtml.contains("&lt;script&gt;"), "HTML special characters must be escaped");
        Assert.assertTrue(variablesHtml.contains("日本語"), "Unicode characters should pass through");
        Assert.assertTrue(variablesHtml.contains("AAAAAAAAAAAAAAAAAAAAAAAAA"), "Unbreakable token should survive verbatim");
        Assert.assertTrue(variablesHtml.contains("ROW-030"), "Tall table should include all 30 rows");

        System.out.println("Variables showcase artifact: " + variableFiles[0].getAbsolutePath());
    }

    @Test
    public void testExhaustiveLogShowcase() throws Exception {
        String testPath = "src/test/target/test-artifacts/showcase-log-" + System.currentTimeMillis() + "/";
        Files.createDirectories(Paths.get(testPath));

        String logFilePath = testPath + "showcase.html";
        CustomLogger customLogger = new CustomLogger("ShowcaseLog_" + UUID.randomUUID(),
                                                      logFilePath, 200);

        LogMessage logMessage = new LogMessage();
        logMessage.setTestBotUri("Automation Anywhere/bots/test/log-showcase");

        // 1. Short INFO, no extras
        logMessage.action(customLogger, LEVEL_INFO,
            "Bot started",
            false, DO_NOT_LOG_VARIABLE, null, null);

        // 2. INFO with screenshot
        logMessage.action(customLogger, LEVEL_INFO,
            "Initial screen captured for run-time baseline",
            true, DO_NOT_LOG_VARIABLE, null, null);

        // 3. Long single-line INFO
        logMessage.action(customLogger, LEVEL_INFO,
            "Processed records sequentially: starting from the first record in the input file, applying validation, then mapping each field through the transformation pipeline, and finally writing each result to the output destination, as one continuous operation across all rows in the source.",
            false, DO_NOT_LOG_VARIABLE, null, null);

        // 4. Multi-line INFO message (newlines preserved on render? Depends on layout's escaping; this test produces the raw entry)
        logMessage.action(customLogger, LEVEL_INFO,
            "Step 1: Read input\nStep 2: Validate fields\nStep 3: Transform values\nStep 4: Write output",
            false, DO_NOT_LOG_VARIABLE, null, null);

        // 5. INFO with HTML special chars
        logMessage.action(customLogger, LEVEL_INFO,
            "Source file <input.csv> & destination <output.json> set; \"verbose\" mode = 'true'",
            false, DO_NOT_LOG_VARIABLE, null, null);

        // 6. INFO with unicode + emoji
        logMessage.action(customLogger, LEVEL_INFO,
            "Run completed: 日本語 OK, العربية OK, 🎉 success",
            false, DO_NOT_LOG_VARIABLE, null, null);

        // 7. INFO with very long unbroken token
        logMessage.action(customLogger, LEVEL_INFO,
            "Trace: AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA-token-end",
            false, DO_NOT_LOG_VARIABLE, null, null);

        // 8. INFO with variables
        Map<String, Value> infoVars = new LinkedHashMap<>();
        infoVars.put("recordCount", new NumberValue(1247));
        infoVars.put("startTime", new DateTimeValue(ZonedDateTime.now().minusMinutes(5)));
        infoVars.put("inputFile", new StringValue("data/customers-2026-Q1.csv"));
        logMessage.action(customLogger, LEVEL_INFO,
            "Processing batch",
            false, LOG_VARIABLE, null, infoVars);

        // 9. INFO with screenshot AND variables
        Map<String, Value> screenVars = new LinkedHashMap<>();
        screenVars.put("page", new StringValue("Dashboard"));
        screenVars.put("loggedInUser", new StringValue("automation@example.com"));
        logMessage.action(customLogger, LEVEL_INFO,
            "Captured dashboard state",
            true, LOG_VARIABLE, null, screenVars);

        // 10. WARN, short
        logMessage.action(customLogger, LEVEL_WARN,
            "Optional field missing in record 42; using default value",
            false, DO_NOT_LOG_VARIABLE, null, null);

        // 11. WARN with screenshot
        logMessage.action(customLogger, LEVEL_WARN,
            "UI element not found at expected location; capturing state for diagnosis",
            true, DO_NOT_LOG_VARIABLE, null, null);

        // 12. WARN with multi-line message + variables
        Map<String, Value> warnVars = new LinkedHashMap<>();
        warnVars.put("missingFields", new StringValue("phoneNumber, secondaryEmail"));
        warnVars.put("recordsAffected", new NumberValue(17));
        logMessage.action(customLogger, LEVEL_WARN,
            "Multiple records have missing optional fields:\n" +
            "  - phoneNumber: 12 records\n" +
            "  - secondaryEmail: 5 records\n" +
            "Defaults will be applied where applicable.",
            false, LOG_VARIABLE, null, warnVars);

        // 13. ERROR, short
        logMessage.action(customLogger, LEVEL_ERROR,
            "Connection refused: unable to reach upstream service",
            false, DO_NOT_LOG_VARIABLE, null, null);

        // 14. ERROR with screenshot
        logMessage.action(customLogger, LEVEL_ERROR,
            "Authentication failed at login screen",
            true, DO_NOT_LOG_VARIABLE, null, null);

        // 15. ERROR with stack-trace-like multi-line message + variables + screenshot
        Map<String, Value> errorVars = new LinkedHashMap<>();
        errorVars.put("errorCode", new StringValue("E_NETWORK_TIMEOUT"));
        errorVars.put("retryCount", new NumberValue(3));
        errorVars.put("lastAttempt", new DateTimeValue(ZonedDateTime.now()));
        errorVars.put("endpoint", new StringValue("https://api.example.com/v2/resource"));
        logMessage.action(customLogger, LEVEL_ERROR,
            "Operation failed after 3 retries:\n" +
            "java.net.SocketTimeoutException: connect timed out\n" +
            "    at java.net.PlainSocketImpl.socketConnect(Native Method)\n" +
            "    at java.net.AbstractPlainSocketImpl.doConnect(AbstractPlainSocketImpl.java:350)\n" +
            "    at java.net.AbstractPlainSocketImpl.connectToAddress(AbstractPlainSocketImpl.java:206)\n" +
            "    at java.net.Socket.connect(Socket.java:606)\n" +
            "    at sun.security.ssl.SSLSocketImpl.connect(SSLSocketImpl.java:288)",
            true, LOG_VARIABLE, null, errorVars);

        // 16-25. Filler entries to make the log long enough to exercise scrolling and the sticky log-table thead
        for (int i = 1; i <= 10; i++) {
            String level = (i % 5 == 0) ? LEVEL_ERROR : (i % 3 == 0) ? LEVEL_WARN : LEVEL_INFO;
            logMessage.action(customLogger, level,
                "Pipeline iteration " + i + " of 10 completed successfully with all expected validations passing",
                false, DO_NOT_LOG_VARIABLE, null, null);
        }

        // 26. Final INFO summary with rich variables
        Map<String, Value> summaryVars = new LinkedHashMap<>();
        summaryVars.put("totalRecords", new NumberValue(1247));
        summaryVars.put("successful", new NumberValue(1230));
        summaryVars.put("warnings", new NumberValue(12));
        summaryVars.put("errors", new NumberValue(5));
        summaryVars.put("durationSeconds", new NumberValue(327.8));
        summaryVars.put("endTime", new DateTimeValue(ZonedDateTime.now()));
        logMessage.action(customLogger, LEVEL_INFO,
            "Run summary",
            false, LOG_VARIABLE, null, summaryVars);

        new StopLoggerSession().stop(customLogger);

        File logFile = new File(logFilePath);
        Assert.assertTrue(logFile.exists(), "Log file should exist");
        Assert.assertTrue(logFile.length() > 0, "Log file should have content");

        String logHtml = new String(Files.readAllBytes(logFile.toPath()));
        // Verify all three levels rendered, screenshots and variables were emitted,
        // and HTML escaping happened where needed.
        Assert.assertTrue(logHtml.contains("level-INFO"), "INFO level should be rendered");
        Assert.assertTrue(logHtml.contains("level-WARN"), "WARN level should be rendered");
        Assert.assertTrue(logHtml.contains("level-ERROR"), "ERROR level should be rendered");
        Assert.assertTrue(logHtml.contains("class='img-link'"), "Screenshot links should be rendered");
        Assert.assertTrue(logHtml.contains("vars-link"), "Variable links should be rendered");
        Assert.assertTrue(logHtml.contains("&lt;input.csv&gt;"), "HTML special characters in messages must be escaped");

        File screenshotsDir = new File(testPath + "screenshots");
        File[] screenshots = screenshotsDir.listFiles((dir, name) -> name.endsWith(".png"));
        Assert.assertNotNull(screenshots, "Screenshots directory should exist");
        Assert.assertTrue(screenshots.length >= 4, "Should have captured screenshots for the entries that requested them");

        File variablesDir = new File(testPath + "variables");
        File[] variableFiles = variablesDir.listFiles((dir, name) -> name.endsWith(".html"));
        Assert.assertNotNull(variableFiles, "Variables directory should exist");
        Assert.assertTrue(variableFiles.length >= 4, "Should have generated variables files for entries that logged variables");

        System.out.println("Log showcase artifact: " + logFilePath);
    }

    @Test
    public void testWideTableVariableRendering() throws Exception {
        // Stress-tests the variables.html template with a wide table containing
        // long cell values. Exercises every layer end-to-end: schema/row
        // construction, LogMessage.action with logVariable=YES,
        // HTMLGenerator.appendTableValue, and the rendered template.
        // A human can open the generated artifact to visually verify wide-table
        // rendering (sticky summary + thead, plain cells instead of monospace
        // code-boxes, overflow-wrap on long unbroken strings).
        String testPath = "src/test/target/test-artifacts/wide-table-" + System.currentTimeMillis() + "/";
        Files.createDirectories(Paths.get(testPath));

        String logFilePath = testPath + "wide-table.html";
        CustomLogger customLogger = new CustomLogger("WideTableLogger_" + UUID.randomUUID(),
                                                      logFilePath, 10);
        LogMessage logMessage = new LogMessage();
        logMessage.setTestBotUri("Automation Anywhere/bots/test/wide-table");

        String[] columns = {
            "Employee ID", "Full Name", "Email Address", "Department",
            "Job Title", "Office Location", "Direct Manager", "Project Code",
            "Status", "Tech Skills", "Last Review Notes", "Profile URL"
        };

        List<Schema> schemas = new ArrayList<>();
        for (String column : columns) {
            schemas.add(new Schema(column));
        }

        String longSkills = "JavaScript, TypeScript, React, Node.js, Python, Django, PostgreSQL, AWS, Docker, Kubernetes";
        String longNotes = "Excellent team player with strong technical leadership skills. Has consistently delivered high-quality work and mentored junior engineers across multiple teams over the past three years.";
        String longUrl = "https://company.example.com/employees/profiles/department/engineering/team-platform-tools/very-long-unbroken-identifier-1234567890123456789012345.html";
        String unbreakableToken = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

        List<Row> rows = new ArrayList<>();
        rows.add(new Row(
            new StringValue("E-1042"),
            new StringValue("Sumit Kumar"),
            new StringValue("sumit.kumar@company.example.com"),
            new StringValue("Engineering"),
            new StringValue("Senior Software Engineer"),
            new StringValue("Bangalore, India"),
            new StringValue("Priya Sharma"),
            new StringValue("PRJ-INFRA-2026-Q2"),
            new StringValue("Active"),
            new StringValue(longSkills),
            new StringValue(longNotes),
            new StringValue(longUrl)
        ));
        rows.add(new Row(
            new StringValue("E-1187"),
            new StringValue("Maria Garcia Rodriguez"),
            new StringValue("maria.garcia.rodriguez@company.example.com"),
            new StringValue("Product Engineering"),
            new StringValue("Staff Software Engineer, Platform Tooling"),
            new StringValue("Mexico City, Mexico"),
            new StringValue("Alejandro Hernandez"),
            new StringValue("PRJ-PLATFORM-2026"),
            new StringValue("Active"),
            new StringValue("Go, Rust, Python, Kubernetes, Terraform, GCP, BigQuery, gRPC"),
            new StringValue("Drove the migration of three legacy services to a new shared infrastructure with zero downtime over a six-month period."),
            new StringValue("https://company.example.com/employees/profiles/department/product-eng/team-platform/m-garcia-rodriguez")
        ));
        rows.add(new Row(
            new StringValue("E-1255"),
            new StringValue("Wei Zhang"),
            new StringValue("wei.zhang@company.example.com"),
            new StringValue("Data Science"),
            new StringValue("Principal Data Scientist"),
            new StringValue("Singapore"),
            new StringValue("Hiroshi Tanaka"),
            new StringValue("PRJ-ML-RECOMMENDATIONS"),
            new StringValue("On Leave"),
            new StringValue("Python, R, PyTorch, TensorFlow, Spark, Kafka, Airflow, Snowflake"),
            new StringValue("On parental leave through Q3 2026. Returning to lead the recommendation system v3 rollout."),
            new StringValue("https://company.example.com/employees/profiles/department/data-science/w-zhang")
        ));
        rows.add(new Row(
            new StringValue("E-9876"),
            new StringValue("Test_User_Name_" + unbreakableToken),
            new StringValue("very.long.email.address.with.many.dots@subdomain.example.company.org"),
            new StringValue("QA"),
            new StringValue("Engineering Lead, QA Automation Platform"),
            new StringValue("Remote / Distributed"),
            new StringValue("Jordan Smith"),
            new StringValue("PRJ-QA-AUTOMATION-2026"),
            new StringValue("Active"),
            new StringValue("Selenium, Cypress, Playwright, JUnit, TestNG, Cucumber, Postman, Charles Proxy"),
            new StringValue("Edge case verification: " + unbreakableToken),
            new StringValue(longUrl)
        ));

        TableValue tableValue = new TableValue();
        tableValue.set(new Table(schemas, rows));

        Map<String, Value> tableVariable = new java.util.HashMap<>();
        tableVariable.put("NAME", new StringValue("Employee Roster"));
        tableVariable.put("VALUE", tableValue);

        List<Value> wideEntryList = new ArrayList<>();
        wideEntryList.add(new DictionaryValue(tableVariable));

        logMessage.action(
            customLogger,
            LEVEL_INFO,
            "Wide table variable rendering test",
            false,
            LOG_VARIABLE,
            wideEntryList,
            null
        );

        new StopLoggerSession().stop(customLogger);

        String variablesDir = testPath + "variables";
        File[] variableFiles = new File(variablesDir).listFiles((dir, name) -> name.endsWith(".html"));
        Assert.assertNotNull(variableFiles, "Variables directory should contain files");
        Assert.assertEquals(variableFiles.length, 1, "Exactly one variables file should be created");

        String variablesHtml = new String(Files.readAllBytes(variableFiles[0].toPath()));
        Assert.assertTrue(variablesHtml.contains("class='table-container'"),
            "Variables HTML should wrap the table in .table-container");
        Assert.assertTrue(variablesHtml.contains("Employee Roster"),
            "Variables HTML should include the variable name");
        Assert.assertTrue(variablesHtml.contains(rows.size() + " rows x " + columns.length + " columns"),
            "Variables HTML should include the table summary line");
        for (String column : columns) {
            Assert.assertTrue(variablesHtml.contains(">" + column + "<"),
                "Variables HTML should contain column header: " + column);
        }
        Assert.assertTrue(variablesHtml.contains(longSkills),
            "Variables HTML should preserve long skills value verbatim");
        Assert.assertTrue(variablesHtml.contains(longUrl),
            "Variables HTML should preserve long URL value verbatim");
        Assert.assertTrue(variablesHtml.contains(unbreakableToken),
            "Variables HTML should preserve unbreakable token (overflow-wrap stress)");

        System.out.println("Wide table variable artifact: " + variableFiles[0].getAbsolutePath());
    }

    @Test
    public void testNewlineGlyphInLogMessage() throws Exception {
        // Multi-line messages must render as visible breaks with a dim glyph
        // marker so users can spot embedded newlines instead of seeing them
        // collapse into a single line.
        String testPath = "src/test/target/test-artifacts/newline-log-" + System.currentTimeMillis() + "/";
        Files.createDirectories(Paths.get(testPath));

        String logFilePath = testPath + "newline.html";
        CustomLogger customLogger = new CustomLogger("NewlineLogger_" + UUID.randomUUID(),
                                                      logFilePath, 10);
        LogMessage logMessage = new LogMessage();
        logMessage.setTestBotUri("Automation Anywhere/bots/test/newline-log");

        logMessage.action(customLogger, LEVEL_INFO,
            "Step 1: Read input\nStep 2: Validate\nStep 3: Done",
            false, DO_NOT_LOG_VARIABLE, null, null);

        new StopLoggerSession().stop(customLogger);

        String logHtml = new String(Files.readAllBytes(Paths.get(logFilePath)));
        Assert.assertTrue(logHtml.contains("class='message-cell'"),
            "Message cell should carry the message-cell class so pre-wrap can target it");
        Assert.assertTrue(logHtml.contains("class='ws-marker ws-nl'"),
            "Multi-line message should emit newline whitespace markers");
        Assert.assertTrue(logHtml.contains("↵"),
            "Newline glyph should be present in the rendered message");
    }

    @Test
    public void testTabGlyphInVariableValue() throws Exception {
        // Tab characters in variable values should render with a visible
        // arrow glyph followed by the actual tab whitespace.
        String testPath = "src/test/target/test-artifacts/tab-glyph-" + System.currentTimeMillis() + "/";
        Files.createDirectories(Paths.get(testPath));

        String logFilePath = testPath + "tab.html";
        CustomLogger customLogger = new CustomLogger("TabLogger_" + UUID.randomUUID(),
                                                      logFilePath, 10);
        LogMessage logMessage = new LogMessage();
        logMessage.setTestBotUri("Automation Anywhere/bots/test/tab-glyph");

        Map<String, Value> vars = new java.util.LinkedHashMap<>();
        vars.put("indented", new StringValue("col1\tcol2\tcol3"));
        logMessage.action(customLogger, LEVEL_INFO, "Tab in variable",
            false, LOG_VARIABLE, null, vars);

        new StopLoggerSession().stop(customLogger);

        File[] variableFiles = new File(testPath + "variables")
                .listFiles((dir, name) -> name.endsWith(".html"));
        Assert.assertNotNull(variableFiles, "Variables directory should contain files");
        Assert.assertEquals(variableFiles.length, 1);

        String variablesHtml = new String(Files.readAllBytes(variableFiles[0].toPath()));
        Assert.assertTrue(variablesHtml.contains("class='ws-marker ws-tab'"),
            "Tab in variable value should emit tab whitespace markers");
        Assert.assertTrue(variablesHtml.contains("→"),
            "Tab glyph should be present in the rendered variable");
    }

    @Test
    public void testTrailingSpaceMarkerInVariableValue() throws Exception {
        // Trailing spaces on a line are bug-prone. The renderer should
        // substitute middle-dot glyphs equal to the trailing-space count so
        // they are visible.
        String testPath = "src/test/target/test-artifacts/trail-space-" + System.currentTimeMillis() + "/";
        Files.createDirectories(Paths.get(testPath));

        String logFilePath = testPath + "trail.html";
        CustomLogger customLogger = new CustomLogger("TrailLogger_" + UUID.randomUUID(),
                                                      logFilePath, 10);
        LogMessage logMessage = new LogMessage();
        logMessage.setTestBotUri("Automation Anywhere/bots/test/trail-space");

        Map<String, Value> vars = new java.util.LinkedHashMap<>();
        vars.put("padded", new StringValue("value with trailing   "));
        logMessage.action(customLogger, LEVEL_INFO, "Trailing space",
            false, LOG_VARIABLE, null, vars);

        new StopLoggerSession().stop(customLogger);

        File[] variableFiles = new File(testPath + "variables")
                .listFiles((dir, name) -> name.endsWith(".html"));
        Assert.assertNotNull(variableFiles);
        Assert.assertEquals(variableFiles.length, 1);

        String variablesHtml = new String(Files.readAllBytes(variableFiles[0].toPath()));
        Assert.assertTrue(variablesHtml.contains("class='ws-marker ws-trail'"),
            "Trailing spaces should emit ws-trail marker spans");
        Assert.assertTrue(variablesHtml.contains("···"),
            "Three trailing spaces should render as three middle-dot glyphs");
    }

    @Test
    public void testStickyTableSummaryStyleEmitted() throws Exception {
        // The variables.html template carries the CSS rule that pins the
        // <summary> of details.full-width while the table is in view. We
        // assert the rule is present in the rendered output.
        String testPath = "src/test/target/test-artifacts/sticky-summary-" + System.currentTimeMillis() + "/";
        Files.createDirectories(Paths.get(testPath));

        String logFilePath = testPath + "sticky.html";
        CustomLogger customLogger = new CustomLogger("StickyLogger_" + UUID.randomUUID(),
                                                      logFilePath, 10);
        LogMessage logMessage = new LogMessage();
        logMessage.setTestBotUri("Automation Anywhere/bots/test/sticky-summary");

        TableValue table = new TableValue();
        table.set(new Table(
            Arrays.asList(new Schema("A"), new Schema("B")),
            java.util.Collections.singletonList(new Row(new StringValue("1"), new StringValue("2")))
        ));
        Map<String, Value> vars = new java.util.LinkedHashMap<>();
        vars.put("smallTable", table);
        logMessage.action(customLogger, LEVEL_INFO, "Sticky summary check",
            false, LOG_VARIABLE, null, vars);

        new StopLoggerSession().stop(customLogger);

        File[] variableFiles = new File(testPath + "variables")
                .listFiles((dir, name) -> name.endsWith(".html"));
        Assert.assertNotNull(variableFiles);
        Assert.assertEquals(variableFiles.length, 1);

        String variablesHtml = new String(Files.readAllBytes(variableFiles[0].toPath()));
        Assert.assertTrue(variablesHtml.contains("details.full-width > summary"),
            "Sticky summary CSS rule should be present in the variables template");
        Assert.assertTrue(variablesHtml.contains("position: sticky"),
            "Sticky positioning should be declared in the variables template");
        Assert.assertTrue(variablesHtml.contains("--summary-h"),
            "Summary height variable should be declared so the th can offset by it");
        Assert.assertTrue(variablesHtml.contains("<details class='full-width'>"),
            "Tables should be wrapped in details.full-width to receive sticky behavior");
    }

    @Test
    public void testDeeplyNestedStructureRenders() throws Exception {
        // 4-level nesting: top-level dict -> list -> dict -> table -> list-in-cell.
        // Verifies recursion through every container type at depth.
        String testPath = "src/test/target/test-artifacts/deep-nested-" + System.currentTimeMillis() + "/";
        Files.createDirectories(Paths.get(testPath));

        String logFilePath = testPath + "deep.html";
        CustomLogger customLogger = new CustomLogger("DeepNested_" + UUID.randomUUID(),
                                                      logFilePath, 10);
        LogMessage logMessage = new LogMessage();
        logMessage.setTestBotUri("Automation Anywhere/bots/test/deep-nested");

        // Innermost: list of strings, lives inside a table cell.
        ListValue innerList = new ListValue();
        innerList.set(Arrays.asList(new StringValue("alpha"), new StringValue("beta")));

        // Level 4 wrap: a table whose first cell IS the inner list.
        TableValue innerTable = new TableValue();
        innerTable.set(new Table(
            Arrays.asList(new Schema("ListCell"), new Schema("Marker")),
            java.util.Collections.singletonList(new Row(innerList, new StringValue("level-4-marker")))
        ));

        // Level 3: dict containing the table plus a marker scalar.
        Map<String, Value> level3Dict = new LinkedHashMap<>();
        level3Dict.put("nestedTable", innerTable);
        level3Dict.put("level3Marker", new StringValue("level-3-marker"));

        // Level 2: list of one dict + a scalar to verify mixed siblings.
        ListValue level2List = new ListValue();
        level2List.set(Arrays.asList(new DictionaryValue(level3Dict), new StringValue("level-2-marker")));

        // Level 1: top dict that becomes the variable value.
        Map<String, Value> topDict = new LinkedHashMap<>();
        topDict.put("nestedStructure", level2List);
        topDict.put("level1Marker", new StringValue("level-1-marker"));

        Map<String, Value> vars = new LinkedHashMap<>();
        vars.put("deepNested", new DictionaryValue(topDict));
        logMessage.action(customLogger, LEVEL_INFO, "Deep nesting test",
            false, LOG_VARIABLE, null, vars);

        new StopLoggerSession().stop(customLogger);

        File[] variableFiles = new File(testPath + "variables")
                .listFiles((dir, name) -> name.endsWith(".html"));
        Assert.assertNotNull(variableFiles);
        Assert.assertEquals(variableFiles.length, 1);

        String html = new String(Files.readAllBytes(variableFiles[0].toPath()));
        Assert.assertTrue(html.contains("level-1-marker"), "Level 1 dict marker should render");
        Assert.assertTrue(html.contains("level-2-marker"), "Level 2 list scalar should render");
        Assert.assertTrue(html.contains("level-3-marker"), "Level 3 dict marker should render");
        Assert.assertTrue(html.contains("level-4-marker"), "Level 4 table cell should render");
        Assert.assertTrue(html.contains("alpha"), "Innermost list element (level 5) should render");
        Assert.assertTrue(html.contains("class='table-container'"),
            "Table at depth should render with table-container class");
        int detailsCount = html.split("<details").length - 1;
        Assert.assertTrue(detailsCount >= 4,
            "Expected at least 4 <details> elements for the nesting; got " + detailsCount);

        System.out.println("Deeply-nested artifact: " + variableFiles[0].getAbsolutePath());
    }

    @Test
    public void testEmptyContainersRender() throws Exception {
        // Empty list, empty dict, and zero-row table should render their
        // summary lines without throwing or producing malformed HTML.
        String testPath = "src/test/target/test-artifacts/empty-containers-" + System.currentTimeMillis() + "/";
        Files.createDirectories(Paths.get(testPath));

        String logFilePath = testPath + "empty.html";
        CustomLogger customLogger = new CustomLogger("EmptyContainers_" + UUID.randomUUID(),
                                                      logFilePath, 10);
        LogMessage logMessage = new LogMessage();
        logMessage.setTestBotUri("Automation Anywhere/bots/test/empty-containers");

        ListValue emptyList = new ListValue();
        emptyList.set(new ArrayList<>());

        DictionaryValue emptyDict = new DictionaryValue(new LinkedHashMap<>());

        TableValue emptyTable = new TableValue();
        emptyTable.set(new Table(
            Arrays.asList(new Schema("A"), new Schema("B")),
            new ArrayList<>()
        ));

        Map<String, Value> vars = new LinkedHashMap<>();
        vars.put("emptyList", emptyList);
        vars.put("emptyDict", emptyDict);
        vars.put("emptyTable", emptyTable);
        logMessage.action(customLogger, LEVEL_INFO, "Empty containers test",
            false, LOG_VARIABLE, null, vars);

        new StopLoggerSession().stop(customLogger);

        File[] variableFiles = new File(testPath + "variables")
                .listFiles((dir, name) -> name.endsWith(".html"));
        Assert.assertNotNull(variableFiles);
        Assert.assertEquals(variableFiles.length, 1);

        String html = new String(Files.readAllBytes(variableFiles[0].toPath()));
        Assert.assertTrue(html.contains("List: 0 items"), "Empty list summary line");
        Assert.assertTrue(html.contains("Dictionary: 0 entries"), "Empty dict summary line");
        Assert.assertTrue(html.contains("Table: 0 rows x 2 columns"), "Empty table summary line");
        Assert.assertTrue(html.contains(">A<"), "Empty table column headers should still render");
        Assert.assertTrue(html.contains(">B<"), "Empty table column headers should still render");

        System.out.println("Empty-containers artifact: " + variableFiles[0].getAbsolutePath());
    }

    @Test
    public void testHtmlSpecialCharsInVariableName() throws Exception {
        // Variable names that contain HTML-special characters must be escaped
        // before rendering. Otherwise a malicious or accidental name could
        // inject markup into the generated file.
        String testPath = "src/test/target/test-artifacts/html-name-" + System.currentTimeMillis() + "/";
        Files.createDirectories(Paths.get(testPath));

        String logFilePath = testPath + "name.html";
        CustomLogger customLogger = new CustomLogger("HtmlName_" + UUID.randomUUID(),
                                                      logFilePath, 10);
        LogMessage logMessage = new LogMessage();
        logMessage.setTestBotUri("Automation Anywhere/bots/test/html-name");

        Map<String, Value> vars = new LinkedHashMap<>();
        vars.put("<script>alert(1)</script>", new StringValue("payload"));
        vars.put("name & label", new StringValue("ampersand"));
        vars.put("has \"quotes\"", new StringValue("quoted"));

        Map<String, Value> innerDict = new LinkedHashMap<>();
        innerDict.put("<inner-key>", new StringValue("inner-value"));
        vars.put("dictWithWeirdKey", new DictionaryValue(innerDict));

        logMessage.action(customLogger, LEVEL_INFO, "HTML special chars in names",
            false, LOG_VARIABLE, null, vars);

        new StopLoggerSession().stop(customLogger);

        File[] variableFiles = new File(testPath + "variables")
                .listFiles((dir, name) -> name.endsWith(".html"));
        Assert.assertNotNull(variableFiles);
        Assert.assertEquals(variableFiles.length, 1);

        String html = new String(Files.readAllBytes(variableFiles[0].toPath()));
        Assert.assertFalse(html.contains("<script>alert(1)</script>"),
            "Raw script tag in name must NOT survive into the rendered HTML (XSS)");
        Assert.assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt;"),
            "Variable name with script tag must be HTML-escaped");
        Assert.assertTrue(html.contains("name &amp; label"),
            "Ampersand in variable name must be HTML-escaped");
        Assert.assertTrue(html.contains("&lt;inner-key&gt;"),
            "Nested dict key with angle brackets must be HTML-escaped");

        System.out.println("HTML-name artifact: " + variableFiles[0].getAbsolutePath());
    }

    @Test
    public void testSingleColumnAndAllNullRowTables() throws Exception {
        // Single-column tables and rows where every cell is null are edge
        // shapes the renderer should handle without dropping output.
        String testPath = "src/test/target/test-artifacts/edge-tables-" + System.currentTimeMillis() + "/";
        Files.createDirectories(Paths.get(testPath));

        String logFilePath = testPath + "edge.html";
        CustomLogger customLogger = new CustomLogger("EdgeTables_" + UUID.randomUUID(),
                                                      logFilePath, 10);
        LogMessage logMessage = new LogMessage();
        logMessage.setTestBotUri("Automation Anywhere/bots/test/edge-tables");

        TableValue singleCol = new TableValue();
        singleCol.set(new Table(
            java.util.Collections.singletonList(new Schema("OnlyColumn")),
            Arrays.asList(
                new Row(new StringValue("alpha")),
                new Row(new StringValue("beta")),
                new Row(new StringValue("gamma"))
            )
        ));

        TableValue nullRowTable = new TableValue();
        nullRowTable.set(new Table(
            Arrays.asList(new Schema("X"), new Schema("Y"), new Schema("Z")),
            Arrays.asList(
                new Row(new StringValue("before"), new StringValue("nulls"), new StringValue("here")),
                new Row(null, null, null),
                new Row(new StringValue("after"), new StringValue("nulls"), new StringValue("works"))
            )
        ));

        Map<String, Value> vars = new LinkedHashMap<>();
        vars.put("singleColumn", singleCol);
        vars.put("withAllNullRow", nullRowTable);
        logMessage.action(customLogger, LEVEL_INFO, "Edge table shapes",
            false, LOG_VARIABLE, null, vars);

        new StopLoggerSession().stop(customLogger);

        File[] variableFiles = new File(testPath + "variables")
                .listFiles((dir, name) -> name.endsWith(".html"));
        Assert.assertNotNull(variableFiles);
        Assert.assertEquals(variableFiles.length, 1);

        String html = new String(Files.readAllBytes(variableFiles[0].toPath()));
        Assert.assertTrue(html.contains("Table: 3 rows x 1 columns"),
            "Single-column table summary");
        Assert.assertTrue(html.contains(">OnlyColumn<"), "Single column header");
        Assert.assertTrue(html.contains("alpha"), "Single-column row content");
        Assert.assertTrue(html.contains("Table: 3 rows x 3 columns"),
            "All-null-row table summary");
        Assert.assertTrue(html.contains("before"), "Row before all-null row");
        Assert.assertTrue(html.contains("after"), "Row after all-null row");

        System.out.println("Edge-tables artifact: " + variableFiles[0].getAbsolutePath());
    }

    @Test
    public void testFlatLayoutAppliedNoOuterRadius() throws Exception {
        // The flat layout drops --radius and the body padding. We assert the
        // rendered log and variables files no longer reference var(--radius)
        // and that body uses padding: 0 so content stretches to the viewport.
        String testPath = "src/test/target/test-artifacts/flat-layout-" + System.currentTimeMillis() + "/";
        Files.createDirectories(Paths.get(testPath));

        String logFilePath = testPath + "flat.html";
        CustomLogger customLogger = new CustomLogger("FlatLogger_" + UUID.randomUUID(),
                                                      logFilePath, 10);
        LogMessage logMessage = new LogMessage();
        logMessage.setTestBotUri("Automation Anywhere/bots/test/flat-layout");

        Map<String, Value> vars = new java.util.LinkedHashMap<>();
        vars.put("k", new StringValue("v"));
        logMessage.action(customLogger, LEVEL_INFO, "flat", false, LOG_VARIABLE, null, vars);

        new StopLoggerSession().stop(customLogger);

        String logHtml = new String(Files.readAllBytes(Paths.get(logFilePath)));
        File[] variableFiles = new File(testPath + "variables")
                .listFiles((dir, name) -> name.endsWith(".html"));
        Assert.assertNotNull(variableFiles);
        String variablesHtml = new String(Files.readAllBytes(variableFiles[0].toPath()));

        Assert.assertFalse(logHtml.contains("border-radius: var(--radius)"),
            "log.html should no longer round outer corners with --radius");
        Assert.assertFalse(variablesHtml.contains("border-radius: var(--radius)"),
            "variables.html should no longer round outer corners with --radius");
        Assert.assertTrue(logHtml.contains("padding: 0"),
            "log.html body should use padding: 0 for edge-to-edge layout");
        Assert.assertTrue(variablesHtml.contains("padding: 0"),
            "variables.html body should use padding: 0 for edge-to-edge layout");
    }

    @Test
    public void testVideoRecordingShowcase() throws Exception {
        // Persistent artifact for visual review of the renamed "Screen" column
        // with a real video clip. Skipped on machines without an interactive
        // desktop because the bundled ffmpeg only supports gdigrab capture.
        screen.process.DesktopAvailability.requireDesktop();

        String testPath = "src/test/target/test-artifacts/showcase-video-"
                + System.currentTimeMillis() + "/";
        Files.createDirectories(Paths.get(testPath));

        String logFilePath = testPath + "showcase.html";

        java.util.Set<org.apache.logging.log4j.Level> levels = new java.util.HashSet<>();
        levels.add(org.apache.logging.log4j.Level.WARN);
        levels.add(org.apache.logging.log4j.Level.ERROR);

        CustomLogger customLogger = new CustomLogger("VideoShowcase_" + UUID.randomUUID(),
                logFilePath, 50, /* bufferSeconds */ 10, levels,
                com.automationanywhere.botcommand.utilities.screen.recorder.EncodingMode.COMPACT);

        LogMessage logMessage = new LogMessage();
        logMessage.setTestBotUri("Automation Anywhere/bots/test/video-showcase");

        // 1. INFO before failure - no video, no still
        logMessage.action(customLogger, LEVEL_INFO,
            "Bot started; rolling buffer is filling",
            false, DO_NOT_LOG_VARIABLE, null, null);

        // 2. INFO with manual screenshot - existing path, unchanged
        logMessage.action(customLogger, LEVEL_INFO,
            "Initial state captured for baseline",
            true, DO_NOT_LOG_VARIABLE, null, null);

        // Wait until the ring has 6 segments (5 stable + 1 in-flight) before the
        // first level-trigger. That gives the WARN clip ~5 s of footage rather
        // than just the 1 s that the test-passing minimum would produce.
        screen.process.RecorderTestSupport.awaitSessionMovSegments(
            customLogger.getLoggerId(), 6, java.time.Duration.ofSeconds(15));

        // 3. WARN - triggers video recording (level is in our set)
        Map<String, Value> warnVars = new LinkedHashMap<>();
        warnVars.put("retryCount", new NumberValue(2));
        warnVars.put("lastError", new StringValue("intermittent timeout"));
        logMessage.action(customLogger, LEVEL_WARN,
            "Retrying after timeout - capturing recording for context",
            false, LOG_VARIABLE, null, warnVars);

        // 4. INFO between failures - no video
        logMessage.action(customLogger, LEVEL_INFO,
            "Continuing with retry attempt",
            false, DO_NOT_LOG_VARIABLE, null, null);

        // Wait for the ring to roll forward to 9 segments so the ERROR clip has
        // ~8 s of footage and is visibly different from the WARN clip.
        screen.process.RecorderTestSupport.awaitSessionMovSegments(
            customLogger.getLoggerId(), 9, java.time.Duration.ofSeconds(15));

        // 5. ERROR - triggers another video recording
        Map<String, Value> errVars = new LinkedHashMap<>();
        errVars.put("recordId", new StringValue("CUST-018273"));
        errVars.put("step", new StringValue("submit-form"));
        errVars.put("retryAttempts", new NumberValue(3));
        logMessage.action(customLogger, LEVEL_ERROR,
            "Maximum retries exceeded - aborting record",
            false, LOG_VARIABLE, null, errVars);

        // Stop the session. close() is non-blocking: stage-2 encodes finish on
        // the per-session encoder pool's worker threads after stop returns.
        new StopLoggerSession().stop(customLogger);

        // Wait for at least one mp4 to land. With libaom-av1 -cpu-used 8 each
        // encode is ~5-15 s; two parallel encodes contend for CPU so the first
        // typically finishes first while the second trails. Poll generously.
        java.nio.file.Path clipsDir = Paths.get(testPath, "clips");
        java.nio.file.Path screenshotsDir = Paths.get(testPath, "screenshots");
        screen.process.RecorderTestSupport.awaitMp4Clips(
                clipsDir, 1, java.time.Duration.ofSeconds(60));

        long mp4s = countMatching(clipsDir, ".mp4");
        long pngs = countMatching(screenshotsDir, ".png");
        // The encoder pool may still have the second clip in flight when this
        // assertion runs; the poster PNG is taken synchronously before submit,
        // so the HTML row carries the still even when the mp4 is yet to land.
        Assert.assertTrue(mp4s >= 1L,
                "showcase should produce at least 1 mp4; got " + mp4s);
        Assert.assertTrue(pngs >= 3,
                "showcase should produce at least 3 PNGs (1 manual + 2 video posters); got " + pngs);

        String html = Files.readString(Paths.get(logFilePath),
                java.nio.charset.StandardCharsets.UTF_8);
        Assert.assertTrue(html.contains("video-link"),
                "rendered HTML must contain video-link class");
        Assert.assertTrue(html.contains("<th>Screen</th>"),
                "rendered HTML must contain the renamed Screen header");

        System.out.println("Video showcase artifact: " + new File(logFilePath).getAbsolutePath());
        System.out.println("  -> " + mp4s + " clip(s), " + pngs + " screenshot(s)");
    }

    private static long countMatching(java.nio.file.Path dir, String suffix) throws IOException {
        if (!Files.isDirectory(dir)) {
            return 0L;
        }
        try (java.util.stream.Stream<java.nio.file.Path> stream = Files.list(dir)) {
            return stream.filter(p -> p.toString().endsWith(suffix)).count();
        }
    }
}
