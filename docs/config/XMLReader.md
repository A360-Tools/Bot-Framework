# XMLReader

## Overview

`XMLReader` reads XML content, parses specified elements using XPath, and outputs a dictionary with customizable keys
and values.

## Parameters

### Input Method

- **Options:** "XML File" or "XML Text"
- **Description:** Choose whether to parse an XML file or text input.

### XML File Path

- **Type:** `String`
- **Condition:** Required if "XML File" is selected.
- **Description:** Path to the XML file.

### Character Set

- **Type:** `String`
- **Description:** Character set for reading the XML file, e.g., "UTF-8".

### XML Text Value

- **Type:** `String`
- **Condition:** Required if "XML Text" is selected.
- **Description:** XML content provided as text.

### XPath to Target Elements

- **Type:** `String`
- **Description:** XPath expression to select XML elements.

### Populate Dictionary Keys From

- **Options:** "Element's tag" or "Element's attribute"
- **Description:** Choose whether dictionary keys should come from element tags or attributes.

### Attribute Name for Keys

- **Type:** `String`
- **Condition:** Required if keys are from element's attribute.
- **Description:** Attribute name whose value is used as dictionary keys.

### Populate Dictionary Values From

- **Options:** "Element's text" or "Element's attribute value"
- **Description:** Choose whether dictionary values should come from element text or attribute values.

### Attribute Name for Values

- **Type:** `String`
- **Condition:** Required if values are from element's attribute.
- **Description:** Attribute name whose value is used as dictionary values.

### Trim Values

- **Type:** `Boolean`
- **Description:** Whether to trim whitespace from dictionary values.

## Output

- **Type:** `DictionaryValue`
- **Description:** A dictionary with keys and values extracted from the XML based on the specified parameters.