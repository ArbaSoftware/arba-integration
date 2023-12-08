package nl.arba.integration.execution.steps;

import nl.arba.integration.execution.Context;
import nl.arba.integration.model.Collection;
import nl.arba.integration.model.JsonArray;
import nl.arba.integration.model.JsonObject;

public class AddToJsonArray extends Step {
    private String arrayProperty;
    private String itemProperty;

    public AddToJsonArray(nl.arba.integration.config.Step config) {
        super(config);
        arrayProperty = config.getSetting("arrayproperty").toString();
        itemProperty = config.getSetting("itemproperty").toString();
    }

    @Override
    public boolean execute(Context context) {
        JsonArray a = (JsonArray) context.getVariable(arrayProperty);
        JsonObject item = (JsonObject) context.getVariable(itemProperty);
        a.add(item);
        return true;
    }

    @Override
    public String[] getRequiredConfigurationParameters() {
        return new String[] {"arrayproperty", "itemproperty"};
    }
}
