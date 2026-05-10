# Clean Directory

## Overview

`Clean Directory` removes files and/or folders within a specified directory based on a set of rules, including age
threshold, selection method, and skip patterns. The base folder itself is never deleted, only its contents.

![image](https://github.com/A360-Tools/Bot-Framework/assets/82057278/c7646e32-4c73-4d54-a6e1-18ec92d5c37d)

## Parameters

### Target folder to clean

- **Description:** Base folder path where cleanup will be performed. This folder itself will never be deleted, only its contents.

### What to delete

- **Options:** `"Both files and folders"` or `"Files only (keep folder structure)"`
- **Default:** `"Both files and folders"`
- **Description:** Whether to delete both files and folders, or files only while preserving the folder structure.

### Include all subfolders

- **Type:** `Boolean`
- **Default:** `true`
- **Description:** When checked, processes all nested folders. When unchecked, only processes immediate folder contents.

### Delete items older than

- **Type:** `Number`
- **Default:** `30`
- **Description:** Age threshold for deletion. Example: `7` (with Days selected) deletes items 7+ days old. Use `0` to delete all items regardless of age.
- **Constraints:** Non-negative integer.

### Time unit

- **Options:** `"Days"`, `"Hours"`, `"Minutes"`, `"Seconds"`
- **Default:** `"Days"`
- **Description:** Unit of time for the age threshold.

### Age based on

- **Options:** `"Creation date (when file was created)"` or `"Last modified date (when file was last changed)"`
- **Default:** `"Last modified date"`
- **Description:** Which timestamp to compare against the age threshold. For logs, prefer `"Last modified"` since they are continuously updated.

### Skip specific folders (preserve them)

- **Type:** `Boolean`
- **Default:** `false`
- **Description:** Enables skipping specific folders based on a regex pattern. Path is compared against the OS-specific absolute path.

### Folder pattern to skip (regex)

- **Condition:** Required when "Skip specific folders" is checked.
- **Description:** Regex pattern matched against the folder's full absolute path. Examples: `.*\\backup$` skips folders named `backup`; `.*\\(archive|important).*` skips folders containing `archive` or `important`.

### Skip specific files (preserve them)

- **Type:** `Boolean`
- **Default:** `false`
- **Description:** Enables skipping specific files based on a regex pattern. Path is compared against the OS-specific absolute path.

### File pattern to skip (regex)

- **Condition:** Required when "Skip specific files" is checked.
- **Description:** Regex pattern matched against the file's full absolute path. Examples: `.*\.log$` skips `.log` files; `.*\.(txt|csv)$` skips `.txt` and `.csv` files.

### When files cannot be deleted (locked/in-use)

- **Options:** `"Stop and throw error (fail the bot)"` or `"Continue silently (skip locked files)"`
- **Default:** `"Continue silently (skip locked files)"`
- **Description:** Behavior when some files cannot be deleted (typically because they are locked by another process). Choose `"Continue silently"` for log cleanup where files may be actively written; choose `"Stop and throw error"` when every targeted file must be deleted.

## Output

No explicit output is returned by this command; it performs deletion actions based on the specified parameters.

## Exceptions

Throws `BotCommandException` if:

- The base folder path does not exist or is inaccessible.
- A regex pattern under "Folder pattern to skip" or "File pattern to skip" is invalid.
- A file cannot be deleted **and** "When files cannot be deleted" is set to `"Stop and throw error"`. With the default `"Continue silently"`, locked-file failures are logged at warning level and skipped.
- Any unexpected error occurs during the deletion process.
