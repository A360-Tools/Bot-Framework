# ExcelReader

## Overview

Read values from an Excel file and store them in a dictionary format. Excel application is not required.

![image](https://github.com/A360-Tools/Bot-Framework/assets/82057278/bd3c7888-f318-49e6-bb6d-a90191249d45)

## Features

- Parses Excel files (.xls, .xlsx) and outputs a dictionary with key-value pairs.
- Supports reading from specified sheet names within the Excel file.
- Offers options for parsing based on column indexes or headers.
- Provides functionality to handle password-protected Excel files.
- Allows trimming of values before storing in the resulting dictionary.

## Parameters

### `inputFilePath`

- **Type:** `String`
- **Description:** Path to the Excel file to be read.
- **Constraints:** Must be a non-empty string. Supported file extensions are `.xls` and `.xlsx`.

### `sheetName`

- **Type:** `String`
- **Description:** Name of the Excel sheet from which to read the data.
- **Constraints:** Must be a non-empty string.

### `isPasswordProtected`

- **Type:** `Boolean`
- **Description:** Indicates if the Excel file is password-protected.
- **Constraints:** `true` if the file is password-protected; otherwise, `false`.

### `filePassword`

- **Type:** `SecureString`
- **Description:** Password for the Excel file, required if `isPasswordProtected` is `true`.
- **Constraints:** Must be provided if the file is password-protected.

### `parsingMethod`

- **Type:** `String`
- **Description:** Specifies the method to parse the Excel file, either by column indexes or headers.
- **Constraints:** Must be either `"Index"` or `"Header"`. Default is `"Index"`.

### `keyIndex` (Conditional on `parsingMethod` being `"Index"`)

- **Type:** `Double`
- **Description:** The index of the column to use as keys in the dictionary.
- **Constraints:** Must be a non-negative integer. Indexing starts at 0.

### `valueIndex` (Conditional on `parsingMethod` being `"Index"`)

- **Type:** `Double`
- **Description:** The index of the column to use as values in the dictionary.
- **Constraints:** Must be a non-negative integer. Indexing starts at 0.

### `keyColumnName` (Conditional on `parsingMethod` being `"Header"`)

- **Type:** `String`
- **Description:** The header name of the column to use as keys in the dictionary.
- **Constraints:** Must be a non-empty string.

### `valueColumnName` (Conditional on `parsingMethod` being `"Header"`)

- **Type:** `String`
- **Description:** The header name of the column to use as values in the dictionary.
- **Constraints:** Must be a non-empty string.

### `isTrimValues`

- **Type:** `Boolean`
- **Description:** Whether to trim whitespace from the values in the resulting dictionary.
- **Constraints:** `true` to trim values; `false` otherwise. Default is `false`.

## Output

- **Type:** `DictionaryValue`
- **Description:** A dictionary containing key-value pairs read from the specified Excel sheet. Keys and values are
  stored as strings.

## Exceptions

- Throws `BotCommandException` if an error occurs during file reading, parsing, or if specified columns/headers are not
  found.
