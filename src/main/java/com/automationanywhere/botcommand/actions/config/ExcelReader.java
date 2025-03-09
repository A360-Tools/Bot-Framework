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
import com.automationanywhere.core.security.SecureString;
import org.apache.poi.ss.usermodel.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.automationanywhere.commandsdk.model.AttributeType.CREDENTIAL;

/**
 * @author Sumit Kumar
 */
@BotCommand
@CommandPkg(label = "Read Excel",
        node_label = "file: {{inputFilePath}}, sheet {{sheetName}} with {{parsingMethod}} and save to" +
                "{{returnTo}}",
        description = "Read values from Excel file and save to dictionary",
        icon = "excel.svg", name = "config_read_excel",
        group_label = "Config",
        text_color = "#1f9c61",
        return_label = "Output: config dictionary", return_type = DataType.DICTIONARY, return_sub_type =
        DataType.STRING, return_name = "Config", return_Direct = true,
        documentation_url = "https://github.com/A360-Tools/Bot-Framework/blob/main/docs/config/ExcelReader.md",
//        allowed_agent_targets = AllowedTarget.HEADLESS,
        return_required = true
)
public class ExcelReader {
    private static final String COLUMN_INDEX = "INDEX";
    private static final String COLUMN_HEADER = "HEADER";

    @Execute
    public DictionaryValue action(
            @Idx(index = "1", type = AttributeType.FILE)
            @Pkg(label = "Enter Excel file path")
            @NotEmpty
            @FileExtension("xlsx,xls")
            String inputFilePath,

            @Idx(index = "2", type = AttributeType.TEXT)
            @Pkg(label = "Enter sheet name to read")
            @NotEmpty
            String sheetName,

            @Idx(index = "3", type = AttributeType.CHECKBOX)
            @Pkg(label = "File is password protected", description = "Select to supply password")
            Boolean isPasswordProtected,

            @Idx(index = "3.1", type = CREDENTIAL)
            @Pkg(label = "Password")
            @CredentialAllowPassword
            @NotEmpty
            SecureString filePassword,

            @Idx(index = "4", type = AttributeType.SELECT, options = {
                    @Idx.Option(index = "4.1", pkg = @Pkg(label = "Index", value = COLUMN_INDEX, node_label =
                            "Key in column index {{keyIndex}} , value in column index {{valueIndex}}")),
                    @Idx.Option(index = "4.2", pkg = @Pkg(label = "Header", value = COLUMN_HEADER, node_label =
                            "Key in header {{keyColumnName}} , value in header {{valueColumnName}}"))
            })
            @Pkg(label = "Parsing Method", default_value = COLUMN_INDEX, default_value_type = DataType.STRING)
            @SelectModes
            @NotEmpty
            String parsingMethod,

            @Idx(index = "4.1.1", type = AttributeType.NUMBER)
            @Pkg(label = "Key column index", default_value_type = DataType.NUMBER, default_value = "0",
                    description = "Index starts at 0. E.g. for column A, index = 0")
            @NotEmpty
            @GreaterThanEqualTo("0")
            @NumberInteger
            Double keyIndex,

            @Idx(index = "4.1.2", type = AttributeType.NUMBER)
            @Pkg(label = "Value column index", default_value_type = DataType.NUMBER, default_value = "1",
                    description = "Index starts at 0. E.g. for column B, index = 1")
            @NotEmpty
            @GreaterThanEqualTo("0")
            @NumberInteger
            Double valueIndex,

            @Idx(index = "4.2.1", type = AttributeType.TEXT)
            @Pkg(label = "Key column header")
            @NotEmpty
            String keyColumnName,

            @Idx(index = "4.2.2", type = AttributeType.TEXT)
            @Pkg(label = "Value column header")
            @NotEmpty
            String valueColumnName,

            @Idx(index = "5", type = AttributeType.BOOLEAN)
            @Pkg(label = "Trim values", default_value_type = DataType.BOOLEAN, default_value = "false", node_label =
                    "Trim values in resulting dictionary")
            @NotEmpty
            Boolean isTrimValues
    ) {
        FileInputStream inputStream = null;
        Workbook workbook = null;

        try {
            // Validate file exists and has correct extension
            FileValidator fileValidator = new FileValidator(inputFilePath);
            String[] allowedExtensions = {"xls", "xlsx"};
            fileValidator.validateFile(allowedExtensions);

            // Determine if we're using column headers
            boolean hasHeader = parsingMethod.equalsIgnoreCase(COLUMN_HEADER);

            // Get password if needed
            String filePasswordInsecureString = null;
            if (isPasswordProtected != null && isPasswordProtected) {
                filePasswordInsecureString = filePassword.getInsecureString();
            }

            // Create result dictionary
            Map<String, Value> excelDictionary = new LinkedHashMap<>();

            // Open file and workbook with explicit resource management
            File file = new File(inputFilePath);
            inputStream = new FileInputStream(file);

            // Create workbook with proper password handling
            if (filePasswordInsecureString != null && !filePasswordInsecureString.isEmpty()) {
                workbook = WorkbookFactory.create(inputStream, filePasswordInsecureString);
            } else {
                workbook = WorkbookFactory.create(inputStream);
            }

            // Get sheet by name
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new BotCommandException("Sheet with name '" + sheetName + "' not found");
            }

            // Initialize data formatter for cell value extraction
            DataFormatter dataFormatter = new DataFormatter();

            // Determine column indices
            int keyIdx;
            int valueIdx;

            if (hasHeader) {
                Row headerRow = sheet.getRow(0);
                if (headerRow == null) {
                    throw new BotCommandException("Header row not found in sheet '" + sheetName + "'");
                }
                keyIdx = findColumnIndex(headerRow, keyColumnName);
                valueIdx = findColumnIndex(headerRow, valueColumnName);
            } else {
                keyIdx = keyIndex.intValue();
                valueIdx = valueIndex.intValue();
            }

            // Process rows
            boolean headerSkipped = false;
            for (Row row : sheet) {
                if (row == null) {
                    continue;
                }

                // Skip header row if using column headers
                if (!headerSkipped && hasHeader) {
                    headerSkipped = true;
                    continue;
                }

                if (keyIdx >= 0 && valueIdx >= 0) {
                    // Get cells or create blank if missing
                    Cell keyCell = row.getCell(keyIdx, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    Cell valueCell = row.getCell(valueIdx, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);

                    // Extract key and verify it's not empty
                    String key = dataFormatter.formatCellValue(keyCell);
                    if (key == null || key.isEmpty()) {
                        continue;
                    }

                    // Extract and process value
                    String value = dataFormatter.formatCellValue(valueCell);
                    if (isTrimValues != null && isTrimValues) {
                        value = value.trim();
                    }

                    // Add to dictionary
                    excelDictionary.put(key, new StringValue(value));
                }
            }

            return new DictionaryValue(excelDictionary);

        } catch (Exception e) {
            throw new BotCommandException("Error reading Excel file: " + e.getMessage(), e);
        } finally {
            // Ensure resources are always closed properly
            closeWorkbook(workbook);
            closeInputStream(inputStream);
        }
    }

    private int findColumnIndex(Row headerRow, String columnName) {
        if (headerRow == null || columnName == null) {
            throw new BotCommandException("Invalid header row or column name");
        }

        int columnIndex = -1;
        DataFormatter dataFormatter = new DataFormatter();

        for (Cell cell : headerRow) {
            if (cell != null && columnName.equalsIgnoreCase(dataFormatter.formatCellValue(cell))) {
                columnIndex = cell.getColumnIndex();
                break;
            }
        }

        if (columnIndex == -1) {
            throw new BotCommandException("Column '" + columnName + "' not found in header row");
        }

        return columnIndex;
    }

    // Helper methods for resource cleanup
    private void closeWorkbook(Workbook workbook) {
        if (workbook != null) {
            try {
                workbook.close();
            } catch (IOException e) {
                // Log the error but don't throw, as we're in cleanup
                System.err.println("Error closing workbook: " + e.getMessage());
            }
        }
    }

    private void closeInputStream(FileInputStream inputStream) {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                // Log the error but don't throw, as we're in cleanup
                System.err.println("Error closing input stream: " + e.getMessage());
            }
        }
    }
}