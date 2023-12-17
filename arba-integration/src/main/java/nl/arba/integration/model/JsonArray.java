package nl.arba.integration.model;

import nl.arba.integration.utils.JsonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JsonArray implements ArrayValue {
    private ArrayList <JsonObject> items = new ArrayList<>();

    public static JsonArray fromJson(String json) throws Exception {
        Map<String, Object>[] items = JsonUtils.getMapper().readValue(json, Map[].class);
        return fromMaps(items);
    }

    public static JsonArray fromMaps(Map<String,Object>[] input) throws Exception {
        JsonArray result = new JsonArray();
        for (Map item : input) {
            result.items.add(JsonObject.fromMap(item));
        }
        return result;
    }

    public void add(JsonObject item) {
        items.add(item);
    }

    public List<JsonObject> getItems() {
        return items;
    }

    public JsonArray filter(String property, String operator, String value) {
        JsonArray array = new JsonArray();
        for (JsonObject item: items) {
            if (item.hasProperty(property) && item.getProperty(property) != null && item.getProperty(property).equals(value)) {
                array.add(item);
            }
            else {
                System.out.println(item.getProperty(property) + " != " + value);
            }
        }
        return array;
    }

    @Override
    public Object get(int index) {
        return items.get(index);
    }
}
