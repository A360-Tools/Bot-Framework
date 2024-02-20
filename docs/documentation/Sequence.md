# Steps

## Overview

`Steps` organizes a sequence of commands within a bot, providing structured documentation and the option to include
screenshots for enhanced clarity and visual guidance.

## Parameters

### Title

- **Type:** `String`
- **Description:** The title for the sequence, summarizing the set of actions or steps involved.

### Documentation

- **Type:** HTML
- **Description:** Detailed documentation describing the actions being performed within the sequence.

### Capture Image Options

For each image capture option:

- **Capture Image Checkbox:** Indicates whether an image should be captured for a particular step within the sequence.
- **Detail About Captured Image:** Provides textual context or explanation for the captured image.
- **Image:** The actual image to be included, relevant to the specific step in the sequence. This field is active only
  if the corresponding capture image checkbox is selected.

## Notes

- The command supports up to three separate image capture options, each with its own checkbox to enable/disable capture,
  a textarea for image details, and an image upload field.
- This command is used for documentation purposes within a bot and executes enabled actions under this.
- It serves as a valuable tool for organizing and explaining complex sequences of actions, especially in scenarios where
  visual steps or configurations are involved.
