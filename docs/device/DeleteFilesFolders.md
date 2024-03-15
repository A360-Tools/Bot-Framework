# Clean Directory
## Important: Use version 3.0.3 or above for improved and fixed abilities
## Overview

`Clean Directory` removes files and/or folders within a specified directory based on a set of rules, including age
threshold and selection method.

![image](https://github.com/A360-Tools/Bot-Framework/assets/82057278/c7646e32-4c73-4d54-a6e1-18ec92d5c37d)

## Parameters

### Enter Base Folder Path

- **Description:** The directory within which files/folders will be scanned for deletion.

### Deletion Option

- **Options:** "Directories and Files" or "Files Only"
- **Description:** Specifies whether to delete both directories and files or files only.

### All Subdirectories Are Searched As Well

- **Type:** `Boolean`
- **Description:** Indicates whether the action should apply to all subdirectories.

### Enter Threshold Number

- **Type:** `Number`
- **Description:** Age threshold for deletion; files/folders older than this number will be deleted.

### Threshold Unit

- **Options:** "DAY", "HOUR", "MINUTE", "SECOND"
- **Description:** Unit of time for the age threshold.

### Ignore Specific Folder Paths

- **Type:** `Boolean`
- **Description:** Enables skipping specific folders based on a regex pattern.Path is compared against OS
  specific path and path separator.

### Folder Path Regex Pattern

- **Condition:** Required if ignoring specific folders.
- **Description:** Regex pattern to match folder paths that should be skipped.

### Ignore Specific File Paths

- **Type:** `Boolean`
- **Description:** Enables skipping specific files based on a regex pattern. Path is compared against OS
  specific path and path separator.

### File Path Regex Pattern

- **Condition:** Required if ignoring specific files.
- **Description:** Regex pattern to match file paths that should be skipped.

### If Certain Files/Folders Cannot Be Deleted

- **Options:** "Throw error" or "Ignore"
- **Description:** Determines behavior when some files/folders cannot be deleted.

## Output

No explicit output is returned by this command; it performs deletion actions based on the specified parameters.

## Exceptions

Throws `BotCommandException` if:

- The base folder path is invalid or inaccessible.
- Specified files/folders cannot be deleted and the option to throw an error is selected.
- Any unexpected error occurs during the deletion process.
