# CloseApplications

## Overview

`CloseApplications` sends close requests followed by terminate requests to a list of running applications, aiming to
gracefully shut down the applications before forcefully terminating them.

## Parameters

### List of Process Names to Close

- **Type:** `List<Value>`
- **Description:** Names of processes to be closed. Can include both the application's executable name (e.g., "
  excel.exe") and the process name (e.g., "excel").
- **Example Values:** "excel.exe", "chrome", "notepad"