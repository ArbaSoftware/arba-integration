package nl.arba.integration.execution.steps;

import nl.arba.integration.execution.Context;
import nl.arba.integration.model.Collection;
import nl.arba.integration.model.JsonObject;

import java.util.ArrayList;

public class AddToCollection extends Step {
    private String collectionProperty;
    private String itemProperty;

    public AddToCollection(nl.arba.integration.config.Step config) {
        super(config);
        collectionProperty = config.getSetting("collectionproperty").toString();
        itemProperty = config.getSetting("itemproperty").toString();
    }

    @Override
    public boolean execute(Context context) {
        Collection c = (Collection) context.getVariable(collectionProperty);
        Object item = (JsonObject) context.getVariable(itemProperty);
        c.addItem(item);
        return true;
    }

    @Override
    public String[] getRequiredConfigurationParameters() {
        return new String[] {"collectionproperty", "itemproperty"};
    }
}
