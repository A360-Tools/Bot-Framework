# Stop Logger Session

## Overview

`Stop Logger Session` terminates a specified logger session, effectively stopping further logging activities.

## Parameters

### Logger Session

- **Description:** Identifies the logger session to be stopped. This session should have been previously started and
  active.

## Exceptions

Throws `BotCommandException` if:

- The specified logger session is not found or is already closed.
- Any other error occurs during the attempt to stop the logger session.
