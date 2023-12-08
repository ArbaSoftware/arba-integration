package nl.arba.integration.validation.json;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

public class JsonSchema {
    private Map<String, Map> schemas;
    private JsonSchema(InputStream source) throws IOException {
        try {
            schemas = (Map<String,Map>)new ObjectMapper().readValue(source, Map.class).get("schemas");
        }
        catch (Exception err) {
            throw new IOException("Schema could not be readed");
        }
    }

    public static JsonSchema create(InputStream source) throws IOException {
        return new JsonSchema(source);
    }

    public String[] validate(String schema, InputStream json) {
        try {
            ArrayList<String> errormessages = new ArrayList<>();

            if (!schemas.containsKey(schema))
                errormessages.add("Invalid schema name");
            else {
                Map<String, Map> schematouse = schemas.get(schema);
                Map tovalidate = new ObjectMapper().readValue(json, Map.class);
                for (String property : schematouse.keySet()) {
                    Map propertySchema = schematouse.get(property);
                    if (propertySchema.containsKey("required") && propertySchema.get("required").equals(Boolean.TRUE)) {
                        if (!tovalidate.containsKey(property))
                            errormessages.add("Required property '" + property + "' missing");
                        if (tovalidate.containsKey(property)) {
                            String type = propertySchema.get("type").toString();
                            try {
                                validatePropertyValue(type, tovalidate.get(property));
                            }
                            catch (Exception err) {
                                errormessages.add("Invalid property value for property '" + property + "'");
                            }
                        }
                    }
                }
            }
            return errormessages.toArray(new String[0]);
        }
        catch (Exception err) {
            return new String[] {err.getMessage()};
        }
    }

    private void validatePropertyValue(String type, Object value) throws Exception {
        if (type.equals("string")) {
            if (!(value instanceof String))
                throw new Exception("Invalid property value");
        }
        else if (type.equals("integer")) {
            if (!(value instanceof Integer)) {
                throw new Exception("Invalid property value");
            }
        }
    }
}
