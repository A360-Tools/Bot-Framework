# Start Logger Session

## Overview

`Start Logger Session` initializes a new logging session, enabling the creation of log files. You can choose to log all levels (INFO, WARN, ERROR) to a single HTML file or configure separate HTML files for each level. The command supports log file rollover based on size.
![image](https://github.com/user-attachments/assets/cb497a74-d255-4e5a-9b8b-4e8e2933aa00)
![image](https://github.com/user-attachments/assets/0c8da229-737e-459d-95e5-fcd3b30a343d)

## Parameters

### Append option for different levels of log

* **Type:** `Select`
* **Options:**
  * `Same File`: Log INFO, WARN, and ERROR messages to a single file specified in "Log file path".
  * `Custom Configuration`: Log INFO, WARN, and ERROR messages to separate files specified below.
* **Default:** `Same File`
* **Description:** Determines whether all log levels are appended to the same file or if separate files are used for different log levels.

### Log file path

* **Condition:** Required if "Append option for different levels of log" is set to `Same File`.
* **Type:** `File`
* **Description:** The path to the log file where all logs (INFO, WARN, ERROR) will be stored.
* **Constraints:** Must be a local file path ending with the `.html` extension. Cannot be empty.

### INFO logs file path

* **Condition:** Required if "Append option for different levels of log" is set to `Custom Configuration`.
* **Type:** `File`
* **Description:** The path to the file where INFO level logs will be stored.
* **Constraints:** Must be a local file path ending with the `.html` extension. Cannot be empty.

### WARN logs file path

* **Condition:** Required if "Append option for different levels of log" is set to `Custom Configuration`.
* **Type:** `File`
* **Description:** The path to the file where WARN level logs will be stored.
* **Constraints:** Must be a local file path ending with the `.html` extension. Cannot be empty.

### ERROR logs file path

* **Condition:** Required if "Append option for different levels of log" is set to `Custom Configuration`.
* **Type:** `File`
* **Description:** The path to the file where ERROR level logs will be stored.
* **Constraints:** Must be a local file path ending with the `.html` extension. Cannot be empty.

### Rollover file size in MB

* **Type:** `Number`
* **Default:** `10`
* **Description:** The maximum size in megabytes (MB) before the log file is rolled over (archived and a new one started).
* **Constraints:** Must be a number greater than 0.

## Output

* **Type:** `Session`
* **Assignment Variable:** `Logger` (Session)
* **Description:** Returns a session object representing the initialized logger session. This session variable must be used in subsequent logging commands (e.g., `Write Log`, `End Logger Session`).

## Exceptions

Throws `BotCommandException` if:

* An invalid option is provided for "Append option for different levels of log".
* Required file paths are empty based on the selected "Append option".
* Provided file paths do not end with the `.html` extension.
* "Rollover file size in MB" is not greater than 0.
* Any other error occurs during logger session initialization (e.g., file access issues). The specific error message will be included.
