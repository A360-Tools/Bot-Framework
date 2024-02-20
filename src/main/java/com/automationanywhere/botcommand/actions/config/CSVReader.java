package com.automationanywhere.botcommand.actions.config;

import com.automationanywhere.botcommand.data.Value;
import com.automationanywhere.botcommand.data.impl.DictionaryValue;
import com.automationanywhere.botcommand.data.impl.StringValue;
import com.automationanywhere.botcommand.exception.BotCommandException;
import com.automationanywhere.botcommand.utilities.file.FileValidator;
import com.automationanywhere.commandsdk.annotations.*;
import com.automationanywhere.commandsdk.annotations.rules.*;
import com.automationanywhere.commandsdk.model.AllowedTarget;
import com.automationanywhere.commandsdk.model.AttributeType;
import com.automationanywhere.commandsdk.model.DataType;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Reader;
import java.nio.charset.Charset;
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
        allowed_agent_targets = AllowedTarget.HEADLESS,
        documentation_url = "",
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
        FileValidator fileValidator = new FileValidator(inputFilePath);
        String[] allowedExtensions = {"csv"};
        fileValidator.validateFile(allowedExtensions);
        boolean hasHeader = parsingMethod.equalsIgnoreCase(COLUMN_HEADER);
        CSVFormat csvFormat;
        if (hasHeader) {
            csvFormat = CSVFormat.Builder
                    .create()
                    .setHeader()
                    .setDelimiter(delimiter)
                    .setIgnoreHeaderCase(true)
                    .setIgnoreEmptyLines(true)
                    .build();
        } else {
            csvFormat = CSVFormat.Builder
                    .create()
                    .setIgnoreEmptyLines(true)
                    .setDelimiter(delimiter)
                    .build();
        }
        Charset charset = Charset.forName(charsetName);
        try (Reader reader = new BufferedReader(new FileReader(inputFilePath, charset));
             CSVParser csvParser = new CSVParser(reader, csvFormat)) {

            Map<String, Value> csvDictionary = new LinkedHashMap<>();

            for (CSVRecord csvRecord : csvParser) {
                String key;
                String value;
                if (hasHeader) {
                    key = csvRecord.get(keyColumnName);
                    value = csvRecord.get(valueColumnName);
                } else {
                    key = csvRecord.get(keyIndex.intValue());
                    value = csvRecord.get(valueIndex.intValue());
                }
                if (key == null || key.isEmpty())
                    continue;
                value = isTrimValues ? value.strip() : value;
                csvDictionary.put(key, new StringValue(value));
            }

            return new DictionaryValue(csvDictionary);

        } catch (Exception e) {
            throw new BotCommandException("Error occurred: " + e.getMessage());
        }
    }

}