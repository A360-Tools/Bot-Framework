# A360 Bot Framework Package

## Overview
Designed to streamline bot development, enhance logging, and facilitate comprehensive documentation within tasks for A360. Tailored for efficiency, consistency, and ease of use, this package addresses common challenges in bot development and maintenance, making it an indispensable tool for modern automation projects.

![main](https://github.com/A360-Tools/Bot-Framework/assets/82057278/8d29c730-b988-4721-b375-f937f623f158)

## Use Cases
- **Framework Templating**: Ideal for organizations looking to standardize bot development practices across multiple teams.
- **Detailed Logging**: Suited for complex workflows where detailed traceability and error tracking are crucial.
- **Visual Failure Replay**: Optionally attach a short MP4 of the seconds leading up to each WARN/ERROR entry, so post-mortems show exactly what the screen looked like at the moment of failure. See [Robust Logging](#robust-logging) and [Bundled FFmpeg](#bundled-ffmpeg) below.
- **Process Documentation**: Perfect for documenting automated processes within the bot, ensuring clarity and ease of maintenance.

## Key Features

### [Robust Logging](https://github.com/A360-Tools/Bot-Framework/blob/main/docs/logs/LogMessage.md)
- Global session logger for comprehensive logging across subtasks.
- Automatic capture of source task details, environment details for enhanced traceability.
- Visualization of complex/nested variables for efficient debugging and tracking.
- HTML format for logs to enable monitoring from various devices while minimizing the risk of unintended file locking.
- Log file rollover based on a configurable maximum number of entries per file (default 1000).
- Log file separation per level: send all log levels to a single file, or split INFO / WARN / ERROR into separate files.
- Screenshot capture alongside log messages for a complete logging experience.
- **Rolling screen-recording buffer**: opt-in continuous capture of the last N seconds (5-90, default 30) of bot activity. When a log entry fires at a configured level (any combination of INFO/WARN/ERROR), the buffer is finalized into a browser-playable MP4 placed next to the log. Choose between **Fast** (H.264, larger files, faster encoding, recommended) and **Compact** (AV1, smaller files, slower encoding) per session. On JVM crash or drain timeout, both the rolling buffer and any pending per-error clips are salvaged on the next bot startup so the HTML log's video links self-heal at the paths they already reference.

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

  ![image](https://github.com/A360-Tools/Bot-Framework/assets/82057278/c7646e32-4c73-4d54-a6e1-18ec92d5c37d)

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
- **Device Clean Directory**: Allows for the rule-based deletion of files or folders, with options to exclude specific items by regex pattern and to skip locked files instead of failing the bot.
- **Documentation About/Caution Sequence/Comment/Sequence**: Offers a range of documentation utilities from basic commenting to detailed sequence documentation with caution highlights and screenshots.
- **Log Message**: Provides session-based logging with options for detailed messages and screenshots.
- **Logs Start/Stop Session**: Manages the lifecycle of logging sessions, from initiation to termination. Always pair `Start Logger Session` with `Stop Logger Session` in a Finally block so logs flush and any pending video clips finalize.

## Bundled FFmpeg

Screen-recording uses an FFmpeg 6.0 binary (~12-15 MB) bundled at `src/main/resources/ffmpeg/ffmpeg.exe` and extracted on first use to `%LOCALAPPDATA%\A360-BotFramework\ffmpeg\<hash>\ffmpeg.exe` on each Bot Runner. The build enables only the libx264 (Fast mode), libaom-av1 (Compact mode), and ffvhuff (rolling buffer) encoders, plus the segment + concat + mp4 muxers, so the binary stays small. Because libx264 is GPL, the bundled ffmpeg is distributed under GPL-2.0 or later; full notice and build configuration are at `src/main/resources/ffmpeg/LICENSE-FFMPEG.txt` and `tools/ffmpeg-build/Dockerfile`. Source code for FFmpeg is at https://ffmpeg.org/download.html.

### Disk usage with screen recording on

| Setting | Cost |
|---|---|
| Stage-1 rolling buffer (per session, %LOCALAPPDATA%) | ~30-100 MB at default 30 s buffer / 5 fps / 1080p |
| Per-finalized clip (Fast / H.264) | ~5-15 MB MP4, encodes in ~0.5-1 s |
| Per-finalized clip (Compact / AV1) | ~1-3 MB MP4, encodes in ~5-15 s |
| Per-error poster PNG (in `<logDir>/screenshots/`) | ~50-200 KB |
| Stage-2 encode CPU | ~80-100% one core for the encode duration above |

Document this when planning bot deployments: enabling video on every INFO entry of a chatty bot will produce hundreds of MP4s. Stick to ERROR-only unless you have a specific need. Pick **Fast** when the bot runner has free CPU but disk is plentiful; pick **Compact** when long-term clip storage matters more than encode latency.

## Building the Project
You can build this project using Gradle with the following command:

```bash
gradle clean build :shadowJar
```

