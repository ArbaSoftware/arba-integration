package nl.arba.integration.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class JsonUtils {
    private static ObjectMapper mapper;

    public static ObjectMapper getMapper() {
        if (mapper == null)
            mapper = new ObjectMapper();
        return mapper;
    }

    public static boolean isValidJson(String source) {
        try {
            getMapper().readValue(source, Map.class);
            return true;
        }
        catch (Exception err) {
            return false;
        }
    }

    public static String translate(InputStream source, Map<String,Object> allstylesheets, String stylesheetname) {
        try {
            Map <String, Object> jsonStylesheet = (Map) ((Map) allstylesheets.get("stylesheets")).get(stylesheetname);
            try {
                Map<String, Object> jsonSource = getMapper().readValue(source, Map.class);
                return getMapper().writeValueAsString(translate(jsonSource, jsonStylesheet));
            }
            catch (JsonProcessingException jpe) {
                source.reset();
                Map<String, Object>[] jsonSource = getMapper().readValue(source, Map[].class);
                Map[] results = new Map[jsonSource.length];
                for (int index = 0; index < results.length; index++) {
                    results[index] = translate(jsonSource[index], jsonStylesheet);
                }
                return getMapper().writeValueAsString(results);
            }
        }
        catch (Exception err) {
            return null;
        }
    }

    private static Map translate(Map<String, Object> source, Map<String,Object> stylesheet) {
        HashMap <String, Object> output = new HashMap<>();
        for (String key: stylesheet.keySet()) {
            Object value = stylesheet.get(key);
            if (value instanceof Map) {
                output.put(key, translate(source, (Map) value));
            }
            else if (value instanceof String) {
                String expression = (String) value;
                if (Pattern.matches("string(.*)", expression)) {
                    output.put(key, (String) eval(expression.substring(7, expression.lastIndexOf(")")), source));
                }
                else if (Pattern.matches(Pattern.quote("$.") + ".*", expression)) {
                    output.put(key, eval(expression, source));
                }
            }
            else  {
                output.put(key, value);
            }
        }
        return output;
    }

    private static Object eval(String expression, Map<String, Object> source) {
        if (expression.startsWith("$.")) {
            return source.get(expression.substring(2));
        }
        else if (Pattern.matches(PatternUtils.stringLiteral(), expression)) {
            return expression.substring(1, expression.lastIndexOf("'"));
        }
        else {
            return null;
        }
    }
}
