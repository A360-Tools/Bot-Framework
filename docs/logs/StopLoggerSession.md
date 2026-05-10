# Stop Logger Session

## Overview

`Stop Logger Session` terminates a specified logger session, flushing any buffered log entries and finalizing any pending screen-recording clips before releasing session resources.

This action is **idempotent**: invoking it on a session that is already closed returns silently. That makes it safe to place inside a Finally block (the recommended pattern) even when the happy path also calls Stop, so a real exception is never masked by an "already closed" error.

![image](https://github.com/A360-Tools/Bot-Framework/assets/82057278/ebbb98f9-7df0-4859-a2b6-cda16880c3c5)

## Parameters

### Logger Session

- **Description:** The logger session to stop. This session should have been previously started by `Start Logger Session`.

## Exceptions

Does not throw on double-close. May propagate a `BotCommandException` only if an unexpected error occurs while flushing logs or closing the underlying recorder.
