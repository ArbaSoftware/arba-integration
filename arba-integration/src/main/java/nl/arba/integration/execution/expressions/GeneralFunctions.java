package nl.arba.integration.execution.expressions;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.arba.integration.execution.Context;
import nl.arba.integration.model.HttpResponse;
import nl.arba.integration.model.JsonArray;
import nl.arba.integration.model.JsonObject;
import nl.arba.integration.utils.JsonUtils;
import nl.arba.integration.utils.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Pattern;

public class GeneralFunctions {
    public static Object parseJson(Context context, byte[] jsonbytes) {
        try {
            Map data = JsonUtils.getMapper().readValue(jsonbytes, Map.class);
            return JsonObject.fromMap(data);
        }
        catch (Exception err) {
            try {
                Map<String,Object>[] array = JsonUtils.getMapper().readValue(jsonbytes, Map[].class);
                return JsonArray.fromMaps(array);
            }
            catch (Exception err2) {
                return null;
            }
        }
    }

    public static Object parseJson(Context context, HttpResponse response) throws Exception {
        byte[] jsonbytes = response.getContent();
        ObjectMapper mapper = JsonUtils.getMapper();
        try {
            Map data = mapper.readValue(jsonbytes, Map.class);
            return JsonObject.fromMap(data);
        }
        catch (Exception err) {
            try {
                Map<String,Object>[] array = mapper.readValue(jsonbytes, Map[].class);
                return JsonArray.fromMaps(array);
            }
            catch (Exception err2) {
                return null;
            }
        }
    }

    public static String translateJson(Context context, byte[] source, String stylesheet) throws Exception {
        Map<String, Object> allstylesheets = context.getJsonStylesheets();
        return JsonUtils.translate(new ByteArrayInputStream(source), allstylesheets, stylesheet);
    }

    public static String translateJson(Context context, HttpResponse source, String stylesheet) throws Exception {
        return translateJson(context, source.getContent(), stylesheet);
    }
}
