# JSONReader

## Overview

Read data from a JSON source, which can be either a file or a text input, and convert this data into a dictionary.

![image](https://github.com/A360-Tools/Bot-Framework/assets/82057278/800d1bf7-7bda-4a1d-8f1a-6ea7821988bc)

## Features

- Supports reading JSON data from a file or direct text input.
- Capable of parsing the entire JSON structure or specific nodes based on the provided path segments.
- Offers functionality to trim whitespace from the resulting dictionary values.

## Parameters

### `inputMethod`

- **Type:** `String`
- **Description:** Specifies the source of the JSON data, either from a file or direct text input.
- **Options:**
    - `"File"`: The JSON data will be read from a file.
    - `"Text"`: The JSON data will be directly provided as text input.

### `jsonFilePath` (Conditional on `inputMethod` being `"File"`)

- **Type:** `String`
- **Description:** The file path to the JSON file.
- **Constraints:** Must be a non-empty string with a `.json` file extension.

### `charsetName` (Conditional on `inputMethod` being `"File"`)

- **Type:** `String`
- **Description:** The character set to use when reading the JSON file.
- **Default:** `"UTF-8"`

### `jsonText` (Conditional on `inputMethod` being `"Text"`)

- **Type:** `String`
- **Description:** The JSON content provided as text.
- **Constraints:** Must be a non-empty string containing valid JSON data.

### `parsingMethod`

- **Type:** `String`
- **Description:** Determines how the JSON data will be parsed.
- **Options:**
    - `"All Nodes"`: Parse the entire JSON structure.
    - `"Specific Node"`: Parse data from a specific node based on provided path segments.

### `jsonPathSegments` (Conditional on `parsingMethod` being `"Specific Node"`)

- **Type:** `List<Value>`
- **Description:** The path segments from the root to the specific node to be parsed.
- **Example:** For a path like `config.dev`, the list should be `["config", "dev"]`.

### `isTrimValues`

- **Type:** `Boolean`
- **Description:** Whether to trim whitespace from the dictionary values.
- **Default:** `false`

## Output

- **Type:** `DictionaryValue`
- **Description:** A dictionary containing key-value pairs extracted from the JSON data. Keys are constructed from the
  path to each value in the JSON structure, and values are stored as strings.
