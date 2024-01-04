package nl.arba.integration.execution.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.arba.integration.execution.Context;
import nl.arba.integration.model.HttpResponse;
import nl.arba.integration.model.JsonArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class ForEach extends Step {
    private String sourceName;
    private String itemVariable;
    private nl.arba.integration.config.Step[] steps;
    public ForEach(nl.arba.integration.config.Step config) {
        super(config);
        sourceName = config.getSetting("source").toString();
        itemVariable = config.getSetting("itemvariable").toString();
        steps = config.getSteps();
    }

    @Override
    public boolean execute(Context context) {
        Object sourceValue = context.getVariable(sourceName);
        ArrayList <Object> items = new ArrayList<>();
        if (sourceValue instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) sourceValue;
            if (response.getContentType() != null && (response.getContentType().startsWith("text/json") || response.getContentType().startsWith("application/json"))) {
                try {
                    Map[] array = new ObjectMapper().readValue(response.getContent(), Map[].class);
                    for (Map item: array) {
                        items.add(item);
                    }
                }
                catch (Exception err) {}
            }
        }
        else if (sourceValue instanceof JsonArray) {
            JsonArray array = (JsonArray) sourceValue;
            items.addAll(array.getItems());
        }
        Step[] executeSteps = new Step[steps.length];
        for (int index = 0; index < executeSteps.length; index++) {
            try {
                final String stepName = steps[index].getName();
                String stepClass = Arrays.asList(context.getConfiguration().getStepClasses()).stream().filter(s -> s.toLowerCase().endsWith("." + stepName)).findFirst().get();
                executeSteps[index] = (Step) Class.forName(stepClass).getConstructor(nl.arba.integration.config.Step.class).newInstance(steps[index]);
            }
            catch (Exception err) {}
        }
        for (Object item: items) {
            context.setVariable(itemVariable, item);
            boolean result = true;
            for (Step step: executeSteps) {
                if (!step.execute(context)) {
                    result = false;
                }
            }
            context.removeVariable(itemVariable);
        }
        return true;
    }

    @Override
    public String[] validate(nl.arba.integration.config.Step config) {
        ArrayList <String> errors = new ArrayList<>();
        for (String error: super.validate(config))
            errors.add(error);
        if (config.getSteps() == null || config.getSteps().length == 0)
            errors.add("Steps missing on step ForEach");
        return errors.toArray(new String[errors.size()]);
    }

    @Override
    public String[] getRequiredConfigurationParameters() {
        return new String[] {"itemvariable", "source"};
    }
}
