# CreateFolders

## Overview

`CreateFolders` facilitates the creation of directories based on specified paths, handling both relative and absolute
paths. It ensures the creation of any necessary parent directories and allows for optional overwriting of existing
folders.

![image](https://github.com/A360-Tools/Bot-Framework/assets/82057278/a8645e97-62fc-4ccb-8b1d-ea55577f1997)

## Parameters

### Creation Mode

- **Options:** "Relative Path" or "Absolute Path"
- **Description:** Specifies whether folder paths are relative to a given base path or are absolute.

### Base Folder Path

- **Condition:** Required for "Relative Path" mode.
- **Description:** The root folder path where directories will be created.

### Folder Names or Paths

- **Type:** `List<Value>`
- **Description:** List of folder names or paths to create. For relative paths, they are relative to the base folder
  path.

### Overwrite Existing Folders

- **Type:** `Boolean`
- **Description:** Determines whether to overwrite folders if they already exist.

### Append to Existing Dictionary

- **Type:** `Boolean`
- **Description:** If enabled, details of created folders are appended to an existing dictionary.

### Existing Config Dictionary

- **Condition:** Required if appending to an existing dictionary.
- **Description:** The dictionary to which the created folder details will be appended.

### Key Suffix

- **Description:** Suffix to add to the dictionary keys representing folder names.

### Key Prefix

- **Description:** Prefix to add to the dictionary keys representing folder names.

## Output

- **Type:** `DictionaryValue`
- **Description:** A dictionary containing keys as folder names (with optional prefix/suffix) and values as the
  corresponding folder paths.

## Exceptions

Throws `BotCommandException` if:

- Invalid paths are provided.
- There are permissions issues.
- Overwriting existing folders fails.
- Any other unexpected error occurs during folder creation.
