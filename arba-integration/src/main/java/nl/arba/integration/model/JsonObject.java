package nl.arba.integration.model;

import nl.arba.integration.utils.JsonUtils;
import nl.arba.integration.utils.PatternUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class JsonObject {
    private HashMap<String, Object> properties = new HashMap<>();
    public void setProperty(String propertyname, String stringvalue) {
        properties.put(propertyname, stringvalue);
    }
    public void setProperty(String propertyname, JsonObject value) {
        properties.put(propertyname, value);
    }

    public void setProperty(String propertyname, JsonArray array) {
        properties.put(propertyname, array);
    }

    public void setProperty(String propertyname, Integer value) {properties.put(propertyname, value);}

    public void setProperty(String propertyname, Boolean value) {properties.put(propertyname, value);}

    public boolean hasProperty(String name) {
        return properties.containsKey(name);
    }

    public Object getProperty(String name) {
        return properties.get(name);
    }

    public static JsonObject fromJson(String json) throws Exception {
        return fromMap(JsonUtils.getMapper().readValue(json, Map.class));
    }

    public static JsonObject fromMap(Map <String, Object> input) {
        JsonObject result = new JsonObject();
        Iterator itProps = input.keySet().iterator();
        while (itProps.hasNext()) {
            String property = (String) itProps.next();
            Object propValue = input.get(property);
            if (propValue instanceof Map)
                result.setProperty(property, JsonObject.fromMap((Map<String,Object>) propValue));
            else if (propValue instanceof String)
                result.setProperty(property, (String) propValue);
            else if (propValue instanceof List) {
                List listValue = (List) propValue;
                JsonArray array = new JsonArray();
                for (Object value: listValue) {
                    array.add(JsonObject.fromMap((Map) value));
                }
                result.setProperty(property, array);
            }
            else if (propValue instanceof Integer) {
                result.setProperty(property, (Integer) propValue);
            }
            else if (propValue == null) {
                result.setProperty(property, (String) null);
            }
            else
                System.out.println("Unsupported json property value: " + propValue);
        }
        return result;
    }

    public JsonObject evaluateFilter(String filter) {
        String propertyname = filter.substring(0, filter.indexOf("["));
        Object propertyValue = getProperty(propertyname);
        if (propertyValue instanceof JsonArray) {
            JsonArray array = (JsonArray) propertyValue;
            JsonObject result = null;
            String filterExpression = filter.substring(filter.indexOf("[")+1);
            filterExpression = filterExpression.substring(0, filterExpression.indexOf("]"));
            String[] filterItems = filterExpression.split(Pattern.quote("="));
            for (JsonObject item: array.getItems()) {
                String filterProperty = filterItems[0];
                String filterValue = filterItems[1];
                if (Pattern.matches(PatternUtils.stringLiteral(), filterValue))
                    filterValue= filterValue.substring(1, filterValue.length()-1);
                if (item.hasProperty(filterProperty) && item.getProperty(filterProperty).equals(filterValue)) {
                    result = item;
                    break;
                }
            }
            return result;
        }
        return null;
    }

    public Map toMap() {
        HashMap<String,Object> map = new HashMap<>();
        for (String property: properties.keySet()) {
            Object propertyValue = properties.get(property);
            if (propertyValue instanceof JsonArray) {
                JsonArray array = (JsonArray) propertyValue;
                Map[] maps =new Map[array.getItems().size()];
                for (int index = 0; index < maps.length; index++)
                    maps[index] = array.getItems().get(index).toMap();
                map.put(property, maps);
            }
            else
                map.put(property, propertyValue);
        }
        return map;
    }

    public String toJson() {
        try {
            return JsonUtils.getMapper().writeValueAsString(toMap());
        }
        catch (Exception err) {
            return null;
        }
    }
}
