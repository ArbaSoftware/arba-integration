package nl.arba.integration.execution.steps;

import nl.arba.integration.execution.Context;

import java.util.Arrays;

public class If extends Step {
    public If(nl.arba.integration.config.Step config) {
        super(config);
    }

    @Override
    public boolean execute(Context context) {
        try {
            Boolean result = (Boolean) context.evaluate((String) getConfiguration().getSetting("expression"));
            nl.arba.integration.config.Step[] steps = result ? getConfiguration().getSteps(): getConfiguration().getElseSteps();
            Step[] executeSteps = new Step[steps.length];
            for (int index = 0; index < executeSteps.length; index++) {
                try {
                    final String stepName = steps[index].getName();
                    String stepClass = Arrays.asList(context.getConfiguration().getStepClasses()).stream().filter(s -> s.toLowerCase().endsWith("." + stepName)).findFirst().get();
                    executeSteps[index] = (Step) Class.forName(stepClass).getConstructor(nl.arba.integration.config.Step.class).newInstance(steps[index]);
                }
                catch (Exception err) {}
            }
            for (int index = 0; index < executeSteps.length; index++) {
                if (!executeSteps[index].execute(context))
                    throw new Exception();
            }
            return true;
        }
        catch (Exception err) {
            return false;
        }
    }

    @Override
    public String[] getRequiredConfigurationParameters() {
        return new String[] {
                "expression"
        };
    }
}
