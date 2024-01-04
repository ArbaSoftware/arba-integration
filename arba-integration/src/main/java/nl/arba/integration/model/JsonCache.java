package nl.arba.integration.model;

import nl.arba.integration.model.JsonObject;

import java.util.HashMap;

public class JsonCache {
    private HashMap<String, JsonObject> cache = new HashMap<>();

    public void put(String key, JsonObject value) {
        cache.put(key, value);
    }

    public void put(String key, HttpResponse value) {
        try {
            cache.put(key, JsonObject.fromJson(new String(((HttpResponse) value).getContent())));
        }
        catch (Exception err) {
            err.printStackTrace();
        }
    }

    public JsonObject get(String key) {
        return cache.get(key);
    }

    public boolean has(String key) {
        return cache.containsKey(key);
    }
}
