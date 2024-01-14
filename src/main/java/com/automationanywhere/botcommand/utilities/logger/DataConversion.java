package com.automationanywhere.botcommand.utilities.logger;

import com.automationanywhere.botcommand.data.Value;
import com.automationanywhere.botcommand.data.impl.DictionaryValue;
import com.automationanywhere.botcommand.data.impl.StringValue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DataConversion {
    public static Map<String, Value> getMergedDictionary(List<Value> list, Map<String, Value> map) {
        if (list != null && list.size() > 0) {

            for (Value element : list) {
                Map<String, Value> customValuesMap = ((DictionaryValue) element).get();
                String key = customValuesMap.containsKey("NAME") ? ((StringValue) customValuesMap.get("NAME")).get() : "";
                Value value = (customValuesMap.getOrDefault("VALUE", null) == null) ? null : (customValuesMap.get("VALUE"));
                map.put(key, value);
            }

        }

        return map;
    }

    public static Map<String, Value> getMergedDictionary(List<Value> list) {
        Map<String, Value> map = new LinkedHashMap<>();
        if (list != null && list.size() > 0) {

            for (Value element : list) {
                Map<String, Value> customValuesMap = ((DictionaryValue) element).get();
                String key = customValuesMap.containsKey("NAME") ? ((StringValue) customValuesMap.get("NAME")).get() : "";
                Value value = (customValuesMap.getOrDefault("VALUE", null) == null) ? null : (customValuesMap.get("VALUE"));
                map.put(key, value);
            }

        }

        return map;
    }
}
