package com.automationanywhere.botcommand.utilities.logger;

import com.automationanywhere.botcommand.data.Value;
import com.automationanywhere.botcommand.data.impl.DictionaryValue;
import com.automationanywhere.botcommand.data.impl.StringValue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DataConversion {
    public static Map<String, Value> getMergedDictionary(List<Value> list, Map<String, Value> existingMap) {
        if (list == null || list.isEmpty()) {
            return existingMap;
        }

        for (Value element : list) {
            if (!(element instanceof DictionaryValue)) continue; // Safety check

            Map<String, Value> customValuesMap = ((DictionaryValue) element).get();
            String key = customValuesMap.containsKey("NAME") ? ((StringValue) customValuesMap.get("NAME")).get() : "";
            Value value = customValuesMap.get("VALUE"); // getOrDefault is not needed as null is the default
            existingMap.put(key, value);
        }

        return existingMap;
    }

    public static Map<String, Value> getMergedDictionary(List<Value> list) {
        return getMergedDictionary(list, new LinkedHashMap<>());
    }
}
