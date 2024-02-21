# Stop Logger Session

## Overview

`Stop Logger Session` terminates a specified logger session, effectively stopping further logging activities.

![image](https://github.com/A360-Tools/Bot-Framework/assets/82057278/ebbb98f9-7df0-4859-a2b6-cda16880c3c5)

## Parameters

### Logger Session

- **Description:** Identifies the logger session to be stopped. This session should have been previously started and
  active.

## Exceptions

Throws `BotCommandException` if:

- The specified logger session is not found or is already closed.
- Any other error occurs during the attempt to stop the logger session.
