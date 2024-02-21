# Log Message

## Overview
`Log Message` records a message to the session log file and optionally captures a screenshot, enriching log diagnostics.

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
- **Description:** Indicates whether to capture a screenshot with the log message.

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

No direct output is returned to the user. This action logs the specified message, variables, and optional screenshot to the configured log file.

## Exceptions

Throws `BotCommandException` if:
- Invalid log level is specified.
- The log message is null or empty.
- There are issues capturing the screenshot (if enabled).
- Any other unexpected error occurs during the logging process.
