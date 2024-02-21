# Caution Steps

## Overview

`Caution Steps` is designed to highlight critical sections within a task, providing a means to document important
actions that require special attention and optionally include screenshots for clarity.

![image](https://github.com/A360-Tools/Bot-Framework/assets/82057278/235b9976-93c2-4001-9740-08c386ad6597)

## Parameters

### Title

- **Type:** `String`
- **Description:** The title for the caution sequence, summarizing the critical action or checkpoint.

### Documentation

- **Type:** HTML
- **Description:** Detailed documentation describing the actions within the caution sequence and why they require
  careful attention.

### Capture Image Options

For each image capture option:

- **Capture Image Checkbox:** Specifies whether an image should be captured for the particular step in the sequence.
- **Detail About Captured Image:** Provides a textual description or details regarding the context or content of the
  captured image.
- **Image:** The actual image captured, relevant to the step in the sequence. This is applicable only if the
  corresponding capture image checkbox is selected.

## Notes

- The command allows for the inclusion of up to three separate images, each with its own checkbox to enable/disable
  capture, a text area for details, and an image upload field.
- This command is used for documentation purposes within a bot and executes any enabled actions under this.
- It serves as a valuable tool for enhancing the readability and maintainability of bots, especially those involving
  complex or sensitive operations.
