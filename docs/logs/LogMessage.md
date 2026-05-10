# Log Message

## Overview
`Log Message` records a message to the session log file and optionally captures a screenshot, enriching log diagnostics. If the parent logger session has screen recording enabled for this entry's log level, the rolling video buffer is also finalized into an MP4 placed next to the log, and a still image of the desktop is captured to serve as the HTML video preview thumbnail.

![image](https://github.com/A360-Tools/Bot-Framework/assets/82057278/8110493b-9bea-478c-9d78-cd864ee581c2)

## Parameters

### Logger Session
- **Description:** Specifies the logger session to use for logging the message.

### Select Log Type
- **Options:** "INFORMATION", "WARNING", "ERROR"
- **Description:** Determines the severity level of the log message.

### Enter Message to Log
- **Type:** `String`
- **Description:** The log message content.

### Capture Screenshot
- **Type:** `Boolean`
- **Default:** `false`
- **Description:** Whether to expose a screenshot file path in this action's return value. When the parent session has screen recording enabled for this entry's log level, a still is always captured behind the scenes as the HTML video preview thumbnail; that internal still is exposed as the returned `ScreenshotPath` only when this flag is checked. With both video on and this flag on, the same still is reused for both purposes (no double capture).

### Log Variable Values
- **Options:** "Yes" or "No"
- **Description:** Specifies if variable values should be logged alongside the message.

### Log Following Variables
- **Type:** `List<Value>`
- **Description:** A list of variables with their names and values to log, applicable if "Log variable values" is set to "Yes".

### Common Datatype Variables to Log
- **Type:** `Map<String, Value>`
- **Description:** A map of common datatype variables and their values to log, applicable if "Log variable values" is set to "Yes".

## Output

* **Type:** `File`
* **Assignment Variable:** `ScreenshotPath` (File, optional)
* **Description:** The path of the screenshot PNG captured for this log entry, populated only when "Capture Screenshot" is checked. Empty when the box is unchecked, even if the parent session is recording video and a video preview thumbnail was captured internally.

## Exceptions

Throws `BotCommandException` if:
- Invalid log level is specified.
- The log message is null or empty.
- There are issues capturing the screenshot (if enabled).
- Any other unexpected error occurs during the logging process.
