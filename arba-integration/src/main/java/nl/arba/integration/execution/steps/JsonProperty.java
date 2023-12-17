package nl.arba.integration.execution.steps;

import nl.arba.integration.execution.Context;
import nl.arba.integration.execution.expressions.InvalidExpressionException;
import nl.arba.integration.model.Collection;
import nl.arba.integration.model.HttpResponse;
import nl.arba.integration.model.JsonArray;
import nl.arba.integration.model.JsonObject;
import nl.arba.integration.utils.JsonUtils;
import nl.arba.integration.utils.StreamUtils;

import java.util.ArrayList;
import java.util.Map;

public class JsonProperty extends Step {
    private String jsonVariable;
    private String valueExpression;
    private String propertyName;

    public JsonProperty(nl.arba.integration.config.Step config) {
        super(config);
        jsonVariable = config.getSetting("name").toString();
        propertyName = config.getSetting("property").toString();
        valueExpression = config.getSetting("value").toString();
    }

    @Override
    public boolean execute(Context context) {
        try {
            JsonObject jsonObject = (JsonObject) context.getVariable(jsonVariable);
            Object newValue = context.evaluate(valueExpression);
            if (newValue instanceof String)
                jsonObject.setProperty(propertyName, (String) newValue);
            else if (newValue instanceof JsonArray)
                jsonObject.setProperty(propertyName, (JsonArray) newValue);
            else if (newValue instanceof Collection) {
                Collection coll = (Collection) newValue;
                if (!coll.getItems().stream().anyMatch(i -> !(i instanceof JsonObject))) {
                    JsonArray array = new JsonArray();
                    for (Object item : coll.getItems())
                        array.add((JsonObject) item);
                    jsonObject.setProperty(propertyName, array);
                }
            } else if (newValue instanceof HttpResponse) {
                HttpResponse response = (HttpResponse) newValue;
                if (response.getContentType().startsWith("text/json") || response.getContentType().startsWith("application/json")) {
                    String json = new String(response.getContent());
                    if (json.startsWith("[")) {
                        JsonArray array = new JsonArray();
                        try {
                            Map[] arrayItems = JsonUtils.getMapper().readValue(json, Map[].class);
                            for (Map item : arrayItems) {
                                array.add(JsonObject.fromMap(item));
                            }
                            jsonObject.setProperty(propertyName, array);
                        } catch (Exception err) {
                            err.printStackTrace();
                        }
                    } else {
                        try {
                            JsonObject object = JsonObject.fromJson(json);
                        } catch (Exception err) {
                            err.printStackTrace();
                        }
                    }
                }
            } else if (newValue instanceof Integer) {
                jsonObject.setProperty(propertyName, (Integer) newValue);
            }

            return true;
        }
        catch (InvalidExpressionException e) {
            return false;
        }
    }

    @Override
    public String[] getRequiredConfigurationParameters() {
        return new String[] {"name", "property", "value"};
    }
}
