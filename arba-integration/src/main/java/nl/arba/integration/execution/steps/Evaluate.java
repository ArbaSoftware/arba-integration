package nl.arba.integration.execution.steps;

import nl.arba.integration.execution.Context;

public class Evaluate extends Step {
    public Evaluate(nl.arba.integration.config.Step config) {
        super(config);
    }

    @Override
    public boolean execute(Context context) {
        try {
            context.evaluate((String) getConfiguration().getSetting("expression"));
            return true;
        }
        catch (Exception err) {
            err.printStackTrace();
        }
        return false;
    }

    @Override
    public String[] getRequiredConfigurationParameters() {
        return new String[] {
                "expression"
        };
    }
}
