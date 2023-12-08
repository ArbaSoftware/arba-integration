package nl.arba.integration.execution.steps;

import nl.arba.integration.execution.Context;

import java.util.ArrayList;

public class Property extends Step {
    private String name;
    private String valueExpression;

    public Property(nl.arba.integration.config.Step config) {
        super(config);
        name = config.getSetting("name").toString();
        valueExpression = config.getSetting("value").toString();
    }

    @Override
    public boolean execute(Context context) {
        if (name.equals("api.response"))
            context.setVariable(Context.API_RESPONSE, context.evaluate(valueExpression));
        else
            context.setVariable(name, context.evaluate(valueExpression));
        return true;
    }

    @Override
    public String[] getRequiredConfigurationParameters() {
        return new String[] {"name", "value"};
    }
}
