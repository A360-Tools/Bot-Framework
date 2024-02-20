# Start Logger Session

## Overview

`Start Logger Session` initializes a new logging session, enabling the creation of log files with optional separate
configurations for different log levels.

## Parameters

### Append Option for Different Levels of Log

- **Options:** "Same File", "Custom Configuration"
- **Description:** Determines whether all log levels are appended to the same file or if separate files are used for
  different log levels.

### Log File Path

- **Condition:** Required if "Same File" is selected.
- **Type:** `String`
- **Description:** The path to the log file where logs will be stored.

### INFO Logs File Path

- **Condition:** Required if "Custom Configuration" is selected for INFO level logs.
- **Type:** `String`
- **Description:** The path to the file where INFO level logs will be stored.

### WARN Logs File Path

- **Condition:** Required if "Custom Configuration" is selected for WARN level logs.
- **Type:** `String`
- **Description:** The path to the file where WARN level logs will be stored.

### ERROR Logs File Path

- **Condition:** Required if "Custom Configuration" is selected for ERROR level logs.
- **Type:** `String`
- **Description:** The path to the file where ERROR level logs will be stored.

### Screenshot Folder Path

- **Type:** `String`
- **Description:** The directory path where screenshots associated with log messages will be saved.

### Rollover File Size in MB

- **Type:** `Number`
- **Description:** The maximum size in megabytes before the log file is rolled over to a new file.

## Output

- **Type:** `SessionValue`
- **Description:** Returns a session object representing the initialized logger session.

## Exceptions

Throws `BotCommandException` if:

- Invalid file paths are provided.
- The screenshot folder path is invalid or empty.
- An unsupported log level and file option is specified.
- Any other error occurs during logger session initialization.
