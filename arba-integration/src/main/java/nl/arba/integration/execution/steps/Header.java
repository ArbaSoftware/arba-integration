package nl.arba.integration.execution.steps;

import nl.arba.integration.execution.Context;
import nl.arba.integration.model.HttpRequest;

import java.util.ArrayList;

public class Header extends Step {
    private String property;
    private String name;
    private String valueExpression;

    public Header(nl.arba.integration.config.Step config) {
        super(config);
        property = config.getSetting("property").toString();
        name = config.getSetting("name").toString();
        valueExpression = config.getSetting("value").toString();
    }
    @Override
    public boolean execute(Context context) {
        Object propertyValue = context.getVariable(property);
        if (propertyValue instanceof HttpRequest) {
            String headerValue = (String) context.evaluate(valueExpression);
            if (headerValue != null)
                ((HttpRequest) propertyValue).setHeader(name, headerValue);
            return true;
        }
        else
            return false;
    }

    @Override
    public String[] getRequiredConfigurationParameters() {
        return new String[] {"property", "name", "value"};
    }
}
