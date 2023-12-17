package nl.arba.integration.execution.steps;

import nl.arba.integration.execution.Context;
import nl.arba.integration.execution.expressions.InvalidExpressionException;

public class Log extends Step {
    private String expression;

    public Log(nl.arba.integration.config.Step config) {
        super(config);
        expression = config.getSetting("expression").toString();
    }

    @Override
    public boolean execute(Context context) {
        try {
            Object toLog = context.evaluate(expression);
            System.out.println(toLog);
            return true;
        }
        catch (InvalidExpressionException e) {
            return false;
        }
    }

    @Override
    public String[] getRequiredConfigurationParameters() {
        return new String[] {"expression"};
    }
}
