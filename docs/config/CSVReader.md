# CSVReader

## 1. Overview

Read values from a CSV file and save them into a dictionary.
![image](https://github.com/A360-Tools/Bot-Framework/assets/82057278/08014370-a4b5-4001-9736-2e77fcc1bb90)

### 1.1 Features

- Reads CSV files and converts them into a dictionary.
- Supports both column index and column header methods for parsing.
- Allows specifying character set for reading CSV files.
- Option to trim values in the resulting dictionary.
- Configurable delimiter support.

## 2. Parameters

### 2.1 `inputFilePath` (String)

- **Description:** The path to the CSV file.
- **Constraints:** Must be a non-empty string with a `.csv` file extension.

### 2.2 `parsingMethod` (String)

- **Description:** Method for parsing columns. Can be "Index" or "Header".
- **Constraints:** Must be non-empty. Default is "Index".

#### 2.2.1 `keyIndex` (Number) - Applicable if `parsingMethod` is "Index"

- **Description:** The index of the column to be used as keys in the dictionary.
- **Constraints:** Must be an integer greater than or equal to 0. Default is 0.

#### 2.2.2 `valueIndex` (Number) - Applicable if `parsingMethod` is "Index"

- **Description:** The index of the column to be used as values in the dictionary.
- **Constraints:** Must be an integer greater than or equal to 0. Default is 1.

#### 2.2.3 `keyColumnName` (String) - Applicable if `parsingMethod` is "Header"

- **Description:** The header name of the column to be used as keys in the dictionary.
- **Constraints:** Must be a non-empty string.

#### 2.2.4 `valueColumnName` (String) - Applicable if `parsingMethod` is "Header"

- **Description:** The header name of the column to be used as values in the dictionary.
- **Constraints:** Must be a non-empty string.

### 2.3 `charsetName` (String)

- **Description:** The character set to use when reading the CSV file.
- **Constraints:** Must be a valid charset name, e.g., "UTF-8".

### 2.4 `isTrimValues` (Boolean)

- **Description:** Whether to trim whitespace from values in the resulting dictionary.
- **Constraints:** Boolean value. Default is `false`.

### 2.5 `delimiter` (String)

- **Description:** The delimiter used in the CSV file.
- **Constraints:** Must be a non-empty string. Default is `,`.

## 3. Output

- **Type:** `DictionaryValue`
- **Description:** A dictionary containing key-value pairs read from the CSV file. Keys and values are stored as
  strings.

## 4. Exceptions

- Throws `BotCommandException` if an error occurs during file reading or parsing.
