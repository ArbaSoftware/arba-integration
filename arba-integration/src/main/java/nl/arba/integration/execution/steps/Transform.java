package nl.arba.integration.execution.steps;

import nl.arba.integration.execution.Context;
import nl.arba.integration.model.Collection;
import nl.arba.integration.model.JsonArray;
import nl.arba.integration.model.JsonObject;
import nl.arba.integration.utils.JsonUtils;

import java.util.stream.Collectors;

public class Transform extends Step {
    private String inputProperty;
    private String format;
    private String targetProperty;

    public Transform(nl.arba.integration.config.Step config) {
        super(config);
        inputProperty = config.getSetting("inputproperty").toString();
        format = config.getSetting("format").toString();
        targetProperty = config.getSetting("targetproperty").toString();
    }

    @Override
    public boolean execute(Context context) {
        if (format.equals("json")) {
            Object input = context.getVariable(inputProperty);
            if (input instanceof Collection) {
                try {
                    Collection coll = (Collection) input;
                    if (!coll.getItems().stream().anyMatch(o -> !(o instanceof JsonObject))) {
                        //All jsonobjects
                        JsonArray array = new JsonArray();
                        for (Object item: coll.getItems()) {
                            JsonObject itemJson = (JsonObject) item;
                            array.add(itemJson);
                        }
                        context.setVariable(targetProperty, array);
                    }
                    else {
                        String json = JsonUtils.getMapper().writeValueAsString(coll.getItems());
                        System.out.println("Variabele nog niet gezet");
                    }
                }
                catch (Exception err) {
                    return false;
                }
            }
            return true;
        }
        else
            return false;
    }

    @Override
    public String[] getRequiredConfigurationParameters() {
        return new String[] {"inputproperty", "format", "targetproperty"};
    }
}
