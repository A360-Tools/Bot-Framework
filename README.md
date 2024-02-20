# A360 Bot Framework Package

## Overview
Designed to streamline bot development, enhance logging, and facilitate comprehensive documentation within automated workflows for A360. Tailored for efficiency, consistency, and ease of use, this package addresses common challenges in bot deployment and maintenance, making it an indispensable tool for modern automation projects.

## Key Features

### Generate Framework Template
- Standardizes bot structures across teams.
- Ensures consistent logging practices for uniformity in deployment and maintenance.

### Robust Logging
- Global session logger for comprehensive logging across subtasks.
- Automatic capture of source task details and complex variables for enhanced traceability.
- Screenshot capture alongside log messages for a complete logging experience.

### Enhanced Documentation
- In-task documentation capabilities with screenshot incorporation.
- Error-prone activity highlights with detailed explanations and visual aids.
- Simplified process comprehension through design screenshots, aiding replication and understanding.

### Config Data Reading
- Reads configuration data from CSV, Excel, JSON, and XML formats into dictionaries.
- Excel-independent functionality for broader applicability and convenience.

### Auto Delete Logs
- Automated log deletion based on log age to optimize log management.
- Customizable log retention through pattern matching for directories and files, aligning with organizational policies.

### Close Process/Application
- Simplifies the termination of processes or applications by name, enhancing system resource management.

### Utility Commands
- **Config Read CSV/Excel/JSON/XML**: Streamlines the import of configuration data into dictionaries from various file formats.
- **Device Close Application**: Facilitates the closing of applications with close and terminate requests.
- **Device Create Folders**: Automates the creation of directories, ensuring the existence of necessary parent directories.
- **Device Delete Files/Folders**: Allows for the rule-based deletion of files or folders, with options to exclude specific items.
- **Documentation About/Caution Sequence/Comment/Sequence**: Offers a range of documentation utilities from basic commenting to detailed sequence documentation with caution highlights and screenshots.
- **Log Message**: Provides session-based logging with options for detailed messages and screenshots.
- **Logs Start/Stop Session**: Manages the lifecycle of logging sessions, from initiation to termination.

## Use Cases
- **Framework Templating**: Ideal for organizations looking to standardize bot development practices across multiple teams.
- **Detailed Logging**: Suited for complex workflows where detailed traceability and error tracking are crucial.
- **Process Documentation**: Perfect for documenting automated processes within the bot, ensuring clarity and ease of maintenance.
- **Configuration Management**: Simplifies the integration of external configuration data into bots, enhancing flexibility and adaptability.
- **Resource Management**: Streamlines the management of system resources by providing utilities for closing applications and managing filesystem resources.

## Building the Project
You can build this project using Gradle with the following command:

```bash
gradle clean build :shadowJar
```

