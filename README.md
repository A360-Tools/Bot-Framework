# A360 Bot Framework Package

## Overview
Designed to streamline bot development, enhance logging, and facilitate comprehensive documentation within automated workflows for A360. Tailored for efficiency, consistency, and ease of use, this package addresses common challenges in bot deployment and maintenance, making it an indispensable tool for modern automation projects.

![main](https://github.com/A360-Tools/Bot-Framework/assets/82057278/8d29c730-b988-4721-b375-f937f623f158)

## Use Cases
- **Framework Templating**: Ideal for organizations looking to standardize bot development practices across multiple teams.
- **Detailed Logging**: Suited for complex workflows where detailed traceability and error tracking are crucial.
- **Process Documentation**: Perfect for documenting automated processes within the bot, ensuring clarity and ease of maintenance.

## Key Features

### [Robust Logging](https://github.com/A360-Tools/Bot-Framework/blob/main/docs/logs/LogMessage.md)
- Global session logger for comprehensive logging across subtasks.
- Automatic capture of source task details, environment details for enhanced traceability.
- Visualization of complex/nested variables for efficient debugging and tracking.
- HTML log format to allow log monitoring via any device and risk of accidental file locks.
- Log file rollover based on file size.
- Log file separation based on log level.
- Screenshot capture alongside log messages for a complete logging experience.
![Animation](https://github.com/A360-Tools/Bot-Framework/assets/82057278/6f8c9268-d411-4b62-93c2-74cca016e13e)

### [Config Data Reading](https://github.com/A360-Tools/Bot-Framework/tree/main/docs/config)
- Reads configuration data from CSV, Excel, JSON, and XML formats into dictionaries.
- Excel application -independent functionality for broader applicability and convenience.
  
  ![image](https://github.com/A360-Tools/Bot-Framework/assets/82057278/08014370-a4b5-4001-9736-2e77fcc1bb90)
  
  ![image](https://github.com/A360-Tools/Bot-Framework/assets/82057278/bd3c7888-f318-49e6-bb6d-a90191249d45)
  
  ![image](https://github.com/A360-Tools/Bot-Framework/assets/82057278/800d1bf7-7bda-4a1d-8f1a-6ea7821988bc)
  
  ![image](https://github.com/A360-Tools/Bot-Framework/assets/82057278/68a7dcdd-40d2-4f5f-a78a-21f4da117039)

### [Auto Delete Logs](https://github.com/A360-Tools/Bot-Framework/blob/main/docs/device/DeleteFilesFolders.md)
- Automated log deletion based on log age to optimize log management.
- Customizable log retention through pattern matching for directories and files, aligning with organizational policies.

  ![image](https://github.com/A360-Tools/Bot-Framework/assets/82057278/2606731b-cb4c-482a-9363-ef3b00158f7d)

### [Close Process/Application](https://github.com/A360-Tools/Bot-Framework/blob/main/docs/device/CloseApplications.md)
- Simplifies the termination of processes or applications by name, enhancing system resource management.

  ![image](https://github.com/A360-Tools/Bot-Framework/assets/82057278/144ef622-7068-4a86-8574-4585749628dc)

### [Generate Framework Template](https://github.com/A360-Tools/Bot-Framework/tree/main/docs)
- Standardizes bot structures across teams.
- Ensures consistent logging practices for uniformity in deployment and maintenance.

### [Enhanced Documentation](https://github.com/A360-Tools/Bot-Framework/tree/main/docs/documentation)
- In-task documentation capabilities with screenshot incorporation.
- Error-prone activity highlights with detailed explanations and visual aids.
- Simplified process comprehension through design screenshots, aiding replication and understanding.

  ![image](https://github.com/A360-Tools/Bot-Framework/assets/82057278/f4fc8d69-fddf-4d49-94d0-e68823dfee6e)

  ![image](https://github.com/A360-Tools/Bot-Framework/assets/82057278/235b9976-93c2-4001-9740-08c386ad6597)

  ![image](https://github.com/A360-Tools/Bot-Framework/assets/82057278/9d55028e-740a-433b-a1cb-af7e2d346402)

  ![image](https://github.com/A360-Tools/Bot-Framework/assets/82057278/133d4bf1-1113-42b4-8d7f-184e2e137b5a)


## Activity Summary
- **Config Read CSV/Excel/JSON/XML**: Streamlines the import of configuration data into dictionaries from various file formats.
- **Device Close Application**: Facilitates the closing of applications with close and terminate requests.
- **Device Create Folders**: Automates the creation of directories, ensuring the existence of necessary parent directories.
- **Device Delete Files/Folders**: Allows for the rule-based deletion of files or folders, with options to exclude specific items.
- **Documentation About/Caution Sequence/Comment/Sequence**: Offers a range of documentation utilities from basic commenting to detailed sequence documentation with caution highlights and screenshots.
- **Log Message**: Provides session-based logging with options for detailed messages and screenshots.
- **Logs Start/Stop Session**: Manages the lifecycle of logging sessions, from initiation to termination.

## Building the Project
You can build this project using Gradle with the following command:

```bash
gradle clean build :shadowJar
```

