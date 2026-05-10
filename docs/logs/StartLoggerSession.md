# Start Logger Session

## Overview

`Start Logger Session` initializes a new logging session, enabling the creation of log files. You can choose to log all levels (INFO, WARN, ERROR) to a single HTML file or configure separate HTML files for each level. The command rolls over log files based on a configurable maximum number of entries per file, and optionally enables a rolling screen-recording buffer that finalizes a video clip whenever a log entry fires at a selected level.
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

### Maximum log entries per file (default 1000)

* **Type:** `Number`
* **Default:** `1000`
* **Description:** The maximum number of log entries written to a single log file before it is rolled over (archived and a new one started).
* **Constraints:** Must be a number greater than 0.

### Screen recording

* **Type:** `Select`
* **Options:**
  * `No video`: Disable screen recording. The session behaves as a logger only.
  * `Capture rolling video`: Continuously capture the desktop into a rolling buffer in the background. For each log entry at one of the selected levels below, the last N seconds are saved as an MP4 placed next to the log file. On JVM crash, the buffer at the moment of death is salvaged into a `crash-recording-<sessionId>.mp4` on the next bot startup. Windows-only; primary monitor only. If the recorder fails to start, the session degrades silently to the existing screenshot-only path.
* **Default:** `No video`

### Record video on INFO entries

* **Condition:** Required if "Screen recording" is set to `Capture rolling video`.
* **Type:** `Boolean`
* **Default:** `false`
* **Description:** When true, every INFO log entry in this session finalizes a video clip from the rolling buffer. Use sparingly: a chatty INFO bot will produce many MP4s.

### Record video on WARN entries

* **Condition:** Required if "Screen recording" is set to `Capture rolling video`.
* **Type:** `Boolean`
* **Default:** `false`
* **Description:** When true, every WARN log entry in this session finalizes a video clip from the rolling buffer.

### Record video on ERROR entries

* **Condition:** Required if "Screen recording" is set to `Capture rolling video`.
* **Type:** `Boolean`
* **Default:** `true`
* **Description:** When true, every ERROR log entry in this session finalizes a video clip from the rolling buffer. Recommended default for production bots.

### Video buffer seconds

* **Condition:** Required if "Screen recording" is set to `Capture rolling video`.
* **Type:** `Number`
* **Default:** `30`
* **Description:** Length in seconds of the rolling video buffer. When a log entry triggers a recording, the finalized clip contains roughly the last N seconds of bot activity ending at that entry's timestamp.
* **Constraints:** Integer between 5 and 300 inclusive.

## Output

* **Type:** `Session`
* **Assignment Variable:** `Logger` (Session)
* **Description:** Returns a session object representing the initialized logger session. This session variable must be used in subsequent logging commands (e.g., `Write Log`, `End Logger Session`).

## Exceptions

Throws `BotCommandException` if:

* An invalid option is provided for "Append option for different levels of log".
* Required file paths are empty based on the selected "Append option".
* Provided file paths do not end with the `.html` extension.
* "Maximum log entries per file" is not greater than 0.
* "Video buffer seconds" is outside the 5-300 range.
* Any other error occurs during logger session initialization (e.g., file access issues). The specific error message will be included.
