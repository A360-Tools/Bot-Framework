package com.automationanywhere.botcommand.actions.config;

import com.automationanywhere.botcommand.data.Value;
import com.automationanywhere.botcommand.data.impl.DictionaryValue;
import com.automationanywhere.botcommand.data.impl.StringValue;
import com.automationanywhere.botcommand.exception.BotCommandException;
import com.automationanywhere.botcommand.utilities.file.FileValidator;
import com.automationanywhere.commandsdk.annotations.*;
import com.automationanywhere.commandsdk.annotations.rules.*;
import com.automationanywhere.commandsdk.model.AttributeType;
import com.automationanywhere.commandsdk.model.DataType;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Sumit Kumar
 */
@BotCommand
@CommandPkg(label = "Read CSV",
        node_label = "file: {{inputFilePath}} with {{parsingMethod}} and save to {{returnTo}}",
        description = "Read values from CSV file and save to dictionary",
        icon = "csv.svg", name = "config_read_csv",
        group_label = "Config",
        text_color = "#1f9c61",
        return_label = "Output: config dictionary", return_type = DataType.DICTIONARY, return_sub_type =
        DataType.STRING, return_name = "Config", return_Direct = true,
//        allowed_agent_targets = AllowedTarget.HEADLESS,
        documentation_url = "https://github.com/A360-Tools/Bot-Framework/blob/main/docs/config/CSVReader.md",
        return_required = true
)
public class CSVReader {
    private static final String COLUMN_INDEX = "INDEX";
    private static final String COLUMN_HEADER = "HEADER";

    @Execute
    public DictionaryValue action(
            @Idx(index = "1", type = AttributeType.FILE)
            @Pkg(label = "CSV file path")
            @NotEmpty
            @FileExtension("csv")
            String inputFilePath,

            @Idx(index = "2", type = AttributeType.SELECT, options = {
                    @Idx.Option(index = "2.1", pkg = @Pkg(label = "Index", value = COLUMN_INDEX, node_label =
                            "Key in column index {{keyIndex}} , value in column index {{valueIndex}}")),
                    @Idx.Option(index = "2.2", pkg = @Pkg(label = "Header", value = COLUMN_HEADER, node_label =
                            "Key in header {{keyColumnName}} , value in header {{valueColumnName}}"))
            })
            @Pkg(label = "Column Parsing Method", default_value = COLUMN_INDEX, default_value_type = DataType.STRING)
            @SelectModes
            @NotEmpty
            String parsingMethod,

            @Idx(index = "2.1.1", type = AttributeType.NUMBER)
            @Pkg(label = "Key column index", default_value_type = DataType.NUMBER, default_value = "0",
                    description = "Index starts at 0. E.g. for column A, index = 0")
            @NotEmpty
            @GreaterThanEqualTo("0")
            @NumberInteger
            Number keyIndex,

            @Idx(index = "2.1.2", type = AttributeType.NUMBER)
            @Pkg(label = "Value column index", default_value_type = DataType.NUMBER, default_value = "1",
                    description = "Index starts at 0. E.g. for column B, index = 1")
            @NotEmpty
            @GreaterThanEqualTo("0")
            @NumberInteger
            Number valueIndex,

            @Idx(index = "2.2.1", type = AttributeType.TEXT)
            @Pkg(label = "Key column header")
            @NotEmpty
            String keyColumnName,

            @Idx(index = "2.2.2", type = AttributeType.TEXT)
            @Pkg(label = "Value column header")
            @NotEmpty
            String valueColumnName,

            @Idx(index = "3", type = AttributeType.TEXT)
            @Pkg(label = "Character Set", default_value = "UTF-8", default_value_type = DataType.STRING,
                    description = "Either a canonical name or an alias, E.g." +
                            "US-ASCII," +
                            "ISO-8859-1," +
                            "UTF-16," +
                            "UTF-16BE," +
                            "UTF-16LE," +
                            "UTF-32," +
                            "UTF-32BE," +
                            "UTF-32LE etc."
            )
            @NotEmpty
            String charsetName,

            @Idx(index = "4", type = AttributeType.BOOLEAN)
            @Pkg(label = "Trim values", default_value_type = DataType.BOOLEAN, default_value = "false", node_label =
                    "Trim values in resulting dictionary")
            @NotEmpty
            Boolean isTrimValues,

            @Idx(index = "5", type = AttributeType.TEXT)
            @Pkg(label = "Delimiter", default_value = ",", default_value_type = DataType.STRING)
            @NotEmpty
            String delimiter

    ) {
        Reader reader = null;
        CSVParser csvParser = null;
        FileInputStream fileInputStream = null;

        try {
            // Validate file exists and has correct extension
            FileValidator fileValidator = new FileValidator(inputFilePath);
            String[] allowedExtensions = {"csv"};
            fileValidator.validateFile(allowedExtensions);

            boolean hasHeader = parsingMethod.equalsIgnoreCase(COLUMN_HEADER);

            // Parse charset safely
            Charset charset;
            try {
                charset = Charset.forName(charsetName);
            } catch (Exception e) {
                // Fallback to UTF-8 if provided charset is invalid
                charset = StandardCharsets.UTF_8;
            }

            // Set up CSV format with proper builder pattern (non-deprecated)
            CSVFormat csvFormat;
            if (hasHeader) {
                csvFormat = CSVFormat.DEFAULT
                        .builder()
                        .setHeader()
                        .setDelimiter(delimiter)
                        .setIgnoreHeaderCase(true)
                        .setIgnoreEmptyLines(true).get();
            } else {
                csvFormat = CSVFormat.DEFAULT
                        .builder()
                        .setDelimiter(delimiter)
                        .setIgnoreEmptyLines(true).get();
            }

            // Create result dictionary
            Map<String, Value> csvDictionary = new LinkedHashMap<>();

            // Open file with explicit resource management
            File file = new File(inputFilePath);
            fileInputStream = new FileInputStream(file);
            reader = new BufferedReader(new InputStreamReader(fileInputStream, charset));

            // Parse CSV content
            csvParser = CSVParser.parse(reader, csvFormat);

            // Process CSV records
            for (CSVRecord csvRecord : csvParser) {
                if (csvRecord.size() == 0) continue;

                String key = null;
                String value = null;

                try {
                    if (hasHeader) {
                        // When using header column names
                        key = csvRecord.get(keyColumnName);
                        value = csvRecord.get(valueColumnName);
                    } else {
                        // When using column indices
                        int keyIdx = keyIndex.intValue();
                        int valueIdx = valueIndex.intValue();

                        // Check bounds before accessing
                        if (keyIdx >= 0 && keyIdx < csvRecord.size() &&
                                valueIdx >= 0 && valueIdx < csvRecord.size()) {
                            key = csvRecord.get(keyIdx);
                            value = csvRecord.get(valueIdx);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    // Skip records where header is not found or index is out of bounds
                    continue;
                }

                // Skip if key is empty
                if (key == null || key.isEmpty()) {
                    continue;
                }

                // Apply trimming if requested
                if (value != null && isTrimValues != null && isTrimValues) {
                    value = value.trim();
                }

                // Add to result dictionary
                csvDictionary.put(key, new StringValue(value != null ? value : ""));
            }

            return new DictionaryValue(csvDictionary);

        } catch (Exception e) {
            throw new BotCommandException("Error reading CSV file: " + e.getMessage(), e);
        } finally {
            // Clean up resources in reverse order of creation
            closeQuietly(csvParser);
            closeQuietly(reader);
            closeQuietly(fileInputStream);
        }
    }

    // Helper methods for resource cleanup
    private void closeQuietly(CSVParser parser) {
        if (parser != null) {
            try {
                parser.close();
            } catch (IOException e) {
                // Swallow exception during cleanup
            }
        }
    }

    private void closeQuietly(Reader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                // Swallow exception during cleanup
            }
        }
    }

    private void closeQuietly(FileInputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                // Swallow exception during cleanup
            }
        }
    }
}